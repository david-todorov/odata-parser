package io.github.davidtodorov.odataparser.search.parser;

import io.github.davidtodorov.odataparser.common.metadata.SourceSpan;
import io.github.davidtodorov.odataparser.search.ast.SearchBinaryExpression;
import io.github.davidtodorov.odataparser.search.ast.SearchBinaryOperator;
import io.github.davidtodorov.odataparser.search.ast.SearchExpression;
import io.github.davidtodorov.odataparser.search.ast.SearchNotExpression;
import io.github.davidtodorov.odataparser.search.ast.SearchPhraseExpression;
import io.github.davidtodorov.odataparser.search.ast.SearchTermExpression;
import io.github.davidtodorov.odataparser.search.lexer.SearchLexer;
import io.github.davidtodorov.odataparser.search.lexer.SearchToken;
import io.github.davidtodorov.odataparser.search.lexer.SearchTokenType;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class SearchParser {

    private final String input;
    private final List<SearchToken> tokens;

    private int currentIndex;

    public SearchParser(String input) {
        this.input = Objects.requireNonNull(
                input,
                "Search parser input cannot be null"
        );

        this.tokens = new SearchLexer(input).tokenize();
    }

    public SearchExpression parse() {
        currentIndex = 0;

        if (check(SearchTokenType.END_OF_INPUT)) {
            throw error(
                    currentToken(),
                    "Search expression cannot be empty"
            );
        }

        SearchExpression expression = parseOrExpression();

        if (!check(SearchTokenType.END_OF_INPUT)) {
            SearchToken token = currentToken();

            if (token.is(SearchTokenType.RIGHT_PARENTHESIS)) {
                throw error(
                        token,
                        "Unexpected closing parenthesis"
                );
            }

            throw error(
                    token,
                    "Unexpected token '" + token.lexeme() + "'"
            );
        }

        return expression;
    }

    private SearchExpression parseOrExpression() {
        SearchExpression expression = parseAndExpression();

        while (match(SearchTokenType.OR)) {
            SearchToken operatorToken = previousToken();

            requireExpressionAfterOperator(operatorToken);

            SearchExpression right = parseAndExpression();

            expression = new SearchBinaryExpression(
                    expression,
                    SearchBinaryOperator.OR,
                    right,
                    spanFrom(
                            expression,
                            right
                    )
            );
        }

        return expression;
    }

    private SearchExpression parseAndExpression() {
        SearchExpression expression = parseUnaryExpression();

        while (true) {
            if (match(SearchTokenType.AND)) {
                SearchToken operatorToken = previousToken();

                requireExpressionAfterOperator(operatorToken);

                SearchExpression right = parseUnaryExpression();

                expression = new SearchBinaryExpression(
                        expression,
                        SearchBinaryOperator.AND,
                        right,
                        spanFrom(
                                expression,
                                right
                        )
                );

                continue;
            }

            if (currentToken().startsExpression()) {
                validateImplicitAnd(expression);

                SearchExpression right = parseUnaryExpression();

                expression = new SearchBinaryExpression(
                        expression,
                        SearchBinaryOperator.AND,
                        right,
                        spanFrom(
                                expression,
                                right
                        )
                );

                continue;
            }

            break;
        }

        return expression;
    }

    private SearchExpression parseUnaryExpression() {
        if (match(SearchTokenType.NOT)) {
            SearchToken notToken = previousToken();

            requireExpressionAfterOperator(notToken);

            SearchExpression operand = parseUnaryExpression();

            return new SearchNotExpression(
                    operand,
                    new SourceSpan(
                            notToken.sourceSpan().start(),
                            operand.sourceSpan().end()
                    )
            );
        }

        return parsePrimaryExpression();
    }

    private SearchExpression parsePrimaryExpression() {
        if (match(SearchTokenType.TERM)) {
            SearchToken token = previousToken();

            rejectIncorrectlyCasedOperator(token);

            return new SearchTermExpression(
                    token.value(),
                    token.sourceSpan()
            );
        }

        if (match(SearchTokenType.PHRASE)) {
            SearchToken token = previousToken();

            return new SearchPhraseExpression(
                    token.value(),
                    token.lexeme(),
                    token.sourceSpan()
            );
        }

        if (match(SearchTokenType.LEFT_PARENTHESIS)) {
            return parseParenthesizedExpression(
                    previousToken()
            );
        }

        SearchToken token = currentToken();

        if (token.is(SearchTokenType.RIGHT_PARENTHESIS)) {
            throw error(
                    token,
                    "Unexpected closing parenthesis"
            );
        }

        if (token.is(SearchTokenType.AND)
                || token.is(SearchTokenType.OR)) {

            throw error(
                    token,
                    "Binary search operator '"
                            + token.lexeme()
                            + "' cannot begin an expression"
            );
        }

        if (token.is(SearchTokenType.END_OF_INPUT)) {
            throw error(
                    token,
                    "Expected a search expression"
            );
        }

        throw error(
                token,
                "Unexpected token '" + token.lexeme() + "'"
        );
    }

    private SearchExpression parseParenthesizedExpression(
            SearchToken openingParenthesis
    ) {
        if (check(SearchTokenType.RIGHT_PARENTHESIS)) {
            throw error(
                    currentToken(),
                    "Search parentheses cannot be empty"
            );
        }

        if (check(SearchTokenType.END_OF_INPUT)) {
            throw error(
                    currentToken(),
                    "Expected a search expression after opening parenthesis"
            );
        }

        SearchExpression expression = parseOrExpression();

        if (!match(SearchTokenType.RIGHT_PARENTHESIS)) {
            SearchToken token = currentToken();

            if (token.is(SearchTokenType.END_OF_INPUT)) {
                throw error(
                        token,
                        "Expected ')' to close the parenthesis opened at position "
                                + openingParenthesis.sourceSpan().start()
                );
            }

            throw error(
                    token,
                    "Expected ')' after parenthesized search expression"
            );
        }

        SearchToken closingParenthesis = previousToken();

        SourceSpan groupedSpan = new SourceSpan(
                openingParenthesis.sourceSpan().start(),
                closingParenthesis.sourceSpan().end()
        );

        return withSourceSpan(
                expression,
                groupedSpan
        );
    }

    private void requireExpressionAfterOperator(
            SearchToken operatorToken
    ) {
        SearchToken next = currentToken();

        if (next.startsExpression()) {
            return;
        }

        if (next.is(SearchTokenType.END_OF_INPUT)) {
            throw error(
                    next,
                    "Expected a search expression after "
                            + operatorToken.lexeme()
            );
        }

        if (next.is(SearchTokenType.RIGHT_PARENTHESIS)) {
            throw error(
                    next,
                    "Expected a search expression after "
                            + operatorToken.lexeme()
                            + " before closing parenthesis"
            );
        }

        throw error(
                next,
                "Expected a search expression after "
                        + operatorToken.lexeme()
                        + " but found '"
                        + next.lexeme()
                        + "'"
        );
    }

    private void validateImplicitAnd(
            SearchExpression left
    ) {
        SearchToken rightStart = currentToken();

        int leftEnd = left.sourceSpan().end();
        int rightStartPosition =
                rightStart.sourceSpan().start();

        if (!containsOnlyWhitespace(
                leftEnd,
                rightStartPosition
        )) {
            throw error(
                    rightStart,
                    "Expected whitespace or AND between search expressions"
            );
        }
    }

    private void rejectIncorrectlyCasedOperator(
            SearchToken token
    ) {
        String uppercase =
                token.value().toUpperCase(Locale.ROOT);

        String expectedOperator = switch (uppercase) {
            case "AND" -> "AND";
            case "OR" -> "OR";
            case "NOT" -> "NOT";
            default -> null;
        };

        if (expectedOperator != null) {
            throw error(
                    token,
                    "Search operator '"
                            + token.lexeme()
                            + "' must be written as '"
                            + expectedOperator
                            + "'"
            );
        }
    }

    private SearchExpression withSourceSpan(
            SearchExpression expression,
            SourceSpan sourceSpan
    ) {
        if (expression instanceof SearchTermExpression term) {
            return new SearchTermExpression(
                    term.term(),
                    sourceSpan
            );
        }

        if (expression instanceof SearchPhraseExpression phrase) {
            return new SearchPhraseExpression(
                    phrase.phrase(),
                    phrase.rawText(),
                    sourceSpan
            );
        }

        if (expression instanceof SearchNotExpression not) {
            return new SearchNotExpression(
                    not.operand(),
                    sourceSpan
            );
        }

        if (expression instanceof SearchBinaryExpression binary) {
            return new SearchBinaryExpression(
                    binary.left(),
                    binary.operator(),
                    binary.right(),
                    sourceSpan
            );
        }

        throw new IllegalStateException(
                "Unsupported search expression implementation: "
                        + expression.getClass().getName()
        );
    }


    private SourceSpan spanFrom(
            SearchExpression left,
            SearchExpression right
    ) {
        return new SourceSpan(
                left.sourceSpan().start(),
                right.sourceSpan().end()
        );
    }


    private boolean containsOnlyWhitespace(
            int start,
            int end
    ) {
        if (start >= end) {
            return false;
        }

        for (int index = start; index < end; index++) {
            if (!Character.isWhitespace(
                    input.charAt(index)
            )) {
                return false;
            }
        }

        return true;
    }

    private boolean match(
            SearchTokenType type
    ) {
        if (!check(type)) {
            return false;
        }

        advance();
        return true;
    }

    private boolean check(
            SearchTokenType type
    ) {
        return currentToken().is(type);
    }

    private SearchToken advance() {
        if (!check(SearchTokenType.END_OF_INPUT)) {
            currentIndex++;
        }

        return previousToken();
    }

    private SearchToken currentToken() {
        return tokens.get(currentIndex);
    }

    private SearchToken previousToken() {
        return tokens.get(currentIndex - 1);
    }

    private SearchParserException error(
            SearchToken token,
            String message
    ) {
        return new SearchParserException(
                message,
                token.sourceSpan().start()
        );
    }
}