package io.github.davidtodorov.odataparser.filter.parser;

import io.github.davidtodorov.odataparser.filter.ast.BinaryFilterExpression;
import io.github.davidtodorov.odataparser.filter.ast.BinaryOperator;
import io.github.davidtodorov.odataparser.filter.ast.FilterExpression;
import io.github.davidtodorov.odataparser.filter.ast.FunctionCallFilterExpression;
import io.github.davidtodorov.odataparser.filter.ast.ListFilterExpression;
import io.github.davidtodorov.odataparser.filter.ast.LiteralFilterExpression;
import io.github.davidtodorov.odataparser.filter.ast.LiteralType;
import io.github.davidtodorov.odataparser.filter.ast.PropertyFilterExpression;
import io.github.davidtodorov.odataparser.filter.ast.UnaryFilterExpression;
import io.github.davidtodorov.odataparser.filter.ast.UnaryOperator;
import io.github.davidtodorov.odataparser.filter.metadata.ExpressionMetadata;
import io.github.davidtodorov.odataparser.common.metadata.SourceSpan;
import io.github.davidtodorov.odataparser.common.type.ExpressionType;
import io.github.davidtodorov.odataparser.filter.lexer.FilterLexer;
import io.github.davidtodorov.odataparser.filter.lexer.FilterToken;
import io.github.davidtodorov.odataparser.filter.lexer.FilterTokenType;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class FilterParser {

    private final List<FilterToken> filterTokens;
    private int currentIndex;

    public FilterParser(String input) {
        this(new FilterLexer(input).tokenize());
    }

    public FilterParser(List<FilterToken> filterTokens) {
        Objects.requireNonNull(
                filterTokens,
                "Token list cannot be null"
        );

        if (filterTokens.isEmpty()) {
            throw new IllegalArgumentException(
                    "Token list cannot be empty"
            );
        }

        if (filterTokens.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "Token list cannot contain null elements"
            );
        }

        validateEndOfInputToken(filterTokens);

        this.filterTokens = List.copyOf(filterTokens);
        this.currentIndex = 0;
    }

    public FilterExpression parse() {
        currentIndex = 0;

        FilterExpression filterExpression = parseExpression();

        if (!check(FilterTokenType.END_OF_INPUT)) {
            throw error(
                    "Unexpected token after complete filter expression"
            );
        }

        return filterExpression;
    }

    private FilterExpression parseExpression() {
        return parseOrExpression();
    }

    private FilterExpression parseOrExpression() {
        FilterExpression filterExpression = parseAndExpression();

        while (match(FilterTokenType.OR)) {
            FilterExpression right = parseAndExpression();

            filterExpression = createBinaryExpression(
                    filterExpression,
                    BinaryOperator.OR,
                    right
            );
        }

        return filterExpression;
    }

    private FilterExpression parseAndExpression() {
        FilterExpression filterExpression = parseComparisonExpression();

        while (match(FilterTokenType.AND)) {
            FilterExpression right = parseComparisonExpression();

            filterExpression = createBinaryExpression(
                    filterExpression,
                    BinaryOperator.AND,
                    right
            );
        }

        return filterExpression;
    }

    private FilterExpression parseComparisonExpression() {
        FilterExpression left = parseAdditiveExpression();

        if (!match(
                FilterTokenType.EQ,
                FilterTokenType.NE,
                FilterTokenType.GT,
                FilterTokenType.GE,
                FilterTokenType.LT,
                FilterTokenType.LE,
                FilterTokenType.IN
        )) {
            return left;
        }

        FilterToken operatorFilterToken = previous();

        FilterExpression right;

        if (operatorFilterToken.type() == FilterTokenType.IN) {
            right = parseInList();
        } else {
            right = parseAdditiveExpression();
        }

        FilterExpression comparison = createBinaryExpression(
                left,
                toBinaryOperator(operatorFilterToken),
                right
        );

        if (isComparisonOperator(currentToken().type())) {
            throw error(
                    "Chained comparison operators are not supported"
            );
        }

        return comparison;
    }

    private FilterExpression parseAdditiveExpression() {
        FilterExpression filterExpression = parseMultiplicativeExpression();

        while (match(FilterTokenType.ADD, FilterTokenType.SUB)) {
            FilterToken operatorFilterToken = previous();
            FilterExpression right = parseMultiplicativeExpression();

            filterExpression = createBinaryExpression(
                    filterExpression,
                    toBinaryOperator(operatorFilterToken),
                    right
            );
        }

        return filterExpression;
    }

    private FilterExpression parseMultiplicativeExpression() {
        FilterExpression filterExpression = parseUnaryExpression();

        while (match(
                FilterTokenType.MUL,
                FilterTokenType.DIV,
                FilterTokenType.MOD
        )) {
            FilterToken operatorFilterToken = previous();
            FilterExpression right = parseUnaryExpression();

            filterExpression = createBinaryExpression(
                    filterExpression,
                    toBinaryOperator(operatorFilterToken),
                    right
            );
        }

        return filterExpression;
    }

    private FilterExpression parseUnaryExpression() {
        if (match(FilterTokenType.NOT)) {
            FilterToken operatorFilterToken = previous();
            FilterExpression operand = parseUnaryExpression();

            SourceSpan sourceSpan = new SourceSpan(
                    operatorFilterToken.start(),
                    operand.sourceSpan().end()
            );

            ExpressionMetadata metadata = new ExpressionMetadata(
                    sourceSpan,
                    ExpressionType.BOOLEAN
            );

            return new UnaryFilterExpression(
                    UnaryOperator.NOT,
                    operand,
                    metadata
            );
        }

        return parsePrimaryExpression();
    }

    private FilterExpression parsePrimaryExpression() {
        if (match(
                FilterTokenType.STRING,
                FilterTokenType.INTEGER,
                FilterTokenType.DECIMAL,
                FilterTokenType.BOOLEAN,
                FilterTokenType.NULL
        )) {
            return createLiteralExpression(previous());
        }

        if (match(FilterTokenType.IDENTIFIER)) {
            return parseIdentifierExpression(previous());
        }

        if (match(FilterTokenType.LEFT_PAREN)) {
            FilterToken openingParenthesis = previous();

            FilterExpression groupedFilterExpression = parseExpression();

            FilterToken closingParenthesis = consume(
                    FilterTokenType.RIGHT_PAREN,
                    "Expected ')' after grouped expression"
            );

            SourceSpan groupedSpan = new SourceSpan(
                    openingParenthesis.start(),
                    closingParenthesis.end()
            );

            return withSourceSpan(
                    groupedFilterExpression,
                    groupedSpan
            );
        }

        throw error("Expected an expression");
    }

    private FilterExpression parseIdentifierExpression(
            FilterToken firstIdentifier
    ) {
        if (match(FilterTokenType.LEFT_PAREN)) {
            return parseFunctionCall(firstIdentifier);
        }

        List<String> pathSegments = new ArrayList<>();
        pathSegments.add(firstIdentifier.lexeme());

        int pathEnd = firstIdentifier.end();

        while (match(FilterTokenType.SLASH)) {
            FilterToken pathSegment = consume(
                    FilterTokenType.IDENTIFIER,
                    "Expected a property name after '/'"
            );

            pathSegments.add(pathSegment.lexeme());
            pathEnd = pathSegment.end();
        }

        SourceSpan sourceSpan = new SourceSpan(
                firstIdentifier.start(),
                pathEnd
        );

        return new PropertyFilterExpression(
                pathSegments,
                ExpressionMetadata.unresolved(sourceSpan)
        );
    }

    private FilterExpression parseFunctionCall(
            FilterToken functionNameFilterToken
    ) {
        List<FilterExpression> arguments = new ArrayList<>();

        if (!check(FilterTokenType.RIGHT_PAREN)) {
            do {
                arguments.add(parseExpression());
            } while (match(FilterTokenType.COMMA));
        }

        FilterToken closingParenthesis = consume(
                FilterTokenType.RIGHT_PAREN,
                "Expected ')' after function arguments"
        );

        SourceSpan sourceSpan = new SourceSpan(
                functionNameFilterToken.start(),
                closingParenthesis.end()
        );

        return new FunctionCallFilterExpression(
                functionNameFilterToken.lexeme(),
                arguments,
                ExpressionMetadata.unresolved(sourceSpan)
        );
    }

    private FilterExpression parseInList() {
        FilterToken openingParenthesis = consume(
                FilterTokenType.LEFT_PAREN,
                "Expected '(' after 'in'"
        );

        if (check(FilterTokenType.RIGHT_PAREN)) {
            throw error(
                    "The 'in' operator requires at least one list element"
            );
        }

        List<FilterExpression> elements = new ArrayList<>();

        do {
            elements.add(parseExpression());
        } while (match(FilterTokenType.COMMA));

        FilterToken closingParenthesis = consume(
                FilterTokenType.RIGHT_PAREN,
                "Expected ')' after 'in' list"
        );

        SourceSpan sourceSpan = new SourceSpan(
                openingParenthesis.start(),
                closingParenthesis.end()
        );

        ExpressionMetadata metadata = new ExpressionMetadata(
                sourceSpan,
                ExpressionType.COLLECTION
        );

        return new ListFilterExpression(
                elements,
                metadata
        );
    }

    private BinaryFilterExpression createBinaryExpression(
            FilterExpression left,
            BinaryOperator operator,
            FilterExpression right
    ) {
        SourceSpan sourceSpan = left.sourceSpan()
                .cover(right.sourceSpan());

        ExpressionType expressionType = switch (operator) {
            case AND, OR,
                 EQ, NE, GT, GE, LT, LE, IN ->
                    ExpressionType.BOOLEAN;

            case ADD, SUB, MUL, DIV, MOD ->
                    ExpressionType.UNKNOWN;
        };

        ExpressionMetadata metadata = new ExpressionMetadata(
                sourceSpan,
                expressionType
        );

        return new BinaryFilterExpression(
                left,
                operator,
                right,
                metadata
        );
    }

    private LiteralFilterExpression createLiteralExpression(
            FilterToken filterToken
    ) {
        SourceSpan sourceSpan = new SourceSpan(
                filterToken.start(),
                filterToken.end()
        );

        try {
            return switch (filterToken.type()) {
                case STRING -> new LiteralFilterExpression(
                        LiteralType.STRING,
                        decodeString(filterToken.lexeme()),
                        filterToken.lexeme(),
                        new ExpressionMetadata(
                                sourceSpan,
                                ExpressionType.STRING
                        )
                );

                case INTEGER -> new LiteralFilterExpression(
                        LiteralType.INTEGER,
                        new BigInteger(filterToken.lexeme()),
                        filterToken.lexeme(),
                        new ExpressionMetadata(
                                sourceSpan,
                                ExpressionType.INTEGER
                        )
                );

                case DECIMAL -> new LiteralFilterExpression(
                        LiteralType.DECIMAL,
                        new BigDecimal(filterToken.lexeme()),
                        filterToken.lexeme(),
                        new ExpressionMetadata(
                                sourceSpan,
                                ExpressionType.DECIMAL
                        )
                );

                case BOOLEAN -> new LiteralFilterExpression(
                        LiteralType.BOOLEAN,
                        Boolean.parseBoolean(filterToken.lexeme()),
                        filterToken.lexeme(),
                        new ExpressionMetadata(
                                sourceSpan,
                                ExpressionType.BOOLEAN
                        )
                );

                case NULL -> new LiteralFilterExpression(
                        LiteralType.NULL,
                        null,
                        filterToken.lexeme(),
                        new ExpressionMetadata(
                                sourceSpan,
                                ExpressionType.NULL
                        )
                );

                default -> throw new IllegalStateException(
                        "Token is not a literal: " + filterToken.type()
                );
            };
        } catch (NumberFormatException exception) {
            throw new FilterParserException(
                    "Invalid numeric literal",
                    filterToken,
                    exception
            );
        }
    }

    private FilterExpression withSourceSpan(
            FilterExpression filterExpression,
            SourceSpan sourceSpan
    ) {
        ExpressionMetadata metadata = new ExpressionMetadata(
                sourceSpan,
                filterExpression.expressionType()
        );

        if (filterExpression instanceof BinaryFilterExpression binary) {
            return new BinaryFilterExpression(
                    binary.left(),
                    binary.operator(),
                    binary.right(),
                    metadata
            );
        }

        if (filterExpression instanceof UnaryFilterExpression unary) {
            return new UnaryFilterExpression(
                    unary.operator(),
                    unary.operand(),
                    metadata
            );
        }

        if (filterExpression instanceof LiteralFilterExpression literal) {
            return new LiteralFilterExpression(
                    literal.literalType(),
                    literal.value(),
                    literal.rawText(),
                    metadata
            );
        }

        if (filterExpression instanceof PropertyFilterExpression property) {
            return new PropertyFilterExpression(
                    property.pathSegments(),
                    property.resolvedPath(),
                    metadata
            );
        }

        if (filterExpression instanceof FunctionCallFilterExpression function) {
            return new FunctionCallFilterExpression(
                    function.functionName(),
                    function.arguments(),
                    metadata
            );
        }

        if (filterExpression instanceof ListFilterExpression list) {
            return new ListFilterExpression(
                    list.elements(),
                    metadata
            );
        }

        throw new IllegalStateException(
                "Unsupported expression type: "
                        + filterExpression.getClass().getName()
        );
    }

    private String decodeString(String lexeme) {
        String content = lexeme.substring(
                1,
                lexeme.length() - 1
        );

        return content.replace("''", "'");
    }

    private BinaryOperator toBinaryOperator(FilterToken filterToken) {
        return switch (filterToken.type()) {
            case AND -> BinaryOperator.AND;
            case OR -> BinaryOperator.OR;

            case EQ -> BinaryOperator.EQ;
            case NE -> BinaryOperator.NE;
            case GT -> BinaryOperator.GT;
            case GE -> BinaryOperator.GE;
            case LT -> BinaryOperator.LT;
            case LE -> BinaryOperator.LE;
            case IN -> BinaryOperator.IN;

            case ADD -> BinaryOperator.ADD;
            case SUB -> BinaryOperator.SUB;
            case MUL -> BinaryOperator.MUL;
            case DIV -> BinaryOperator.DIV;
            case MOD -> BinaryOperator.MOD;

            default -> throw new IllegalArgumentException(
                    "Token is not a binary operator: "
                            + filterToken.type()
            );
        };
    }

    private boolean isComparisonOperator(FilterTokenType type) {
        return switch (type) {
            case EQ, NE, GT, GE, LT, LE, IN -> true;
            default -> false;
        };
    }

    private boolean match(FilterTokenType... filterTokenTypes) {
        for (FilterTokenType filterTokenType : filterTokenTypes) {
            if (check(filterTokenType)) {
                advance();
                return true;
            }
        }

        return false;
    }

    private FilterToken consume(
            FilterTokenType expectedType,
            String errorMessage
    ) {
        if (check(expectedType)) {
            return advance();
        }

        throw error(errorMessage);
    }

    private boolean check(FilterTokenType filterTokenType) {
        return currentToken().type() == filterTokenType;
    }

    private FilterToken advance() {
        FilterToken filterToken = currentToken();

        if (!isAtEnd()) {
            currentIndex++;
        }

        return filterToken;
    }

    private boolean isAtEnd() {
        return currentToken().type()
                == FilterTokenType.END_OF_INPUT;
    }

    private FilterToken currentToken() {
        return filterTokens.get(currentIndex);
    }

    private FilterToken previous() {
        return filterTokens.get(currentIndex - 1);
    }

    private FilterParserException error(String message) {
        return new FilterParserException(
                message,
                currentToken()
        );
    }

    private static void validateEndOfInputToken(
            List<FilterToken> filterTokens
    ) {
        FilterToken lastFilterToken = filterTokens.get(filterTokens.size() - 1);

        if (lastFilterToken.type() != FilterTokenType.END_OF_INPUT) {
            throw new IllegalArgumentException(
                    "Token sequence must end with END_OF_INPUT"
            );
        }

        for (int index = 0; index < filterTokens.size() - 1; index++) {
            if (filterTokens.get(index).type()
                    == FilterTokenType.END_OF_INPUT) {
                throw new IllegalArgumentException(
                        "END_OF_INPUT may appear only as the last token"
                );
            }
        }
    }
}