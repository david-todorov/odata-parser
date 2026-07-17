package io.github.davidtodorov.odataparser.filter.parser;

import io.github.davidtodorov.odataparser.expression.ast.*;
import io.github.davidtodorov.odataparser.filter.lexer.FilterLexer;
import io.github.davidtodorov.odataparser.filter.lexer.Token;
import io.github.davidtodorov.odataparser.filter.lexer.TokenType;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class FilterParser {

    private final List<Token> tokens;
    private int currentIndex;

    /**
     * Convenience constructor that tokenizes the input before parsing.
     *
     * The input must contain only the filter expression, without "$filter=".
     */
    public FilterParser(String input) {
        this(new FilterLexer(input).tokenize());
    }

    /**
     * Creates a parser from an existing token sequence.
     */
    public FilterParser(List<Token> tokens) {
        Objects.requireNonNull(
                tokens,
                "Token list cannot be null"
        );

        if (tokens.isEmpty()) {
            throw new IllegalArgumentException(
                    "Token list cannot be empty"
            );
        }

        if (tokens.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "Token list cannot contain null elements"
            );
        }

        validateEndOfInputToken(tokens);

        this.tokens = List.copyOf(tokens);
        this.currentIndex = 0;
    }

    /**
     * Parses the complete token sequence and returns the AST root.
     */
    public Expression parse() {
        currentIndex = 0;

        Expression expression = parseExpression();

        if (!check(TokenType.END_OF_INPUT)) {
            throw error(
                    "Unexpected token after complete filter expression"
            );
        }

        return expression;
    }

    /**
     * Entry point for the expression grammar.
     */
    private Expression parseExpression() {
        return parseOrExpression();
    }

    /**
     * orExpression:
     *     andExpression ("or" andExpression)*
     */
    private Expression parseOrExpression() {
        Expression expression = parseAndExpression();

        while (match(TokenType.OR)) {
            Expression right = parseAndExpression();

            expression = new BinaryExpression(
                    expression,
                    BinaryOperator.OR,
                    right
            );
        }

        return expression;
    }

    /**
     * andExpression:
     *     comparisonExpression ("and" comparisonExpression)*
     */
    private Expression parseAndExpression() {
        Expression expression = parseComparisonExpression();

        while (match(TokenType.AND)) {
            Expression right = parseComparisonExpression();

            expression = new BinaryExpression(
                    expression,
                    BinaryOperator.AND,
                    right
            );
        }

        return expression;
    }

    /**
     * comparisonExpression:
     *     additiveExpression
     *     (comparisonOperator comparisonRightOperand)?
     *
     * Comparison operators are deliberately non-chainable.
     */
    private Expression parseComparisonExpression() {
        Expression left = parseAdditiveExpression();

        if (!match(
                TokenType.EQ,
                TokenType.NE,
                TokenType.GT,
                TokenType.GE,
                TokenType.LT,
                TokenType.LE,
                TokenType.IN
        )) {
            return left;
        }

        Token operatorToken = previous();

        Expression right;

        if (operatorToken.type() == TokenType.IN) {
            right = parseInList();
        } else {
            right = parseAdditiveExpression();
        }

        Expression comparison = new BinaryExpression(
                left,
                toBinaryOperator(operatorToken),
                right
        );

        if (isComparisonOperator(currentToken().type())) {
            throw error(
                    "Chained comparison operators are not supported"
            );
        }

        return comparison;
    }

    /**
     * additiveExpression:
     *     multiplicativeExpression
     *     (("add" | "sub") multiplicativeExpression)*
     */
    private Expression parseAdditiveExpression() {
        Expression expression = parseMultiplicativeExpression();

        while (match(TokenType.ADD, TokenType.SUB)) {
            Token operatorToken = previous();
            Expression right = parseMultiplicativeExpression();

            expression = new BinaryExpression(
                    expression,
                    toBinaryOperator(operatorToken),
                    right
            );
        }

        return expression;
    }

    /**
     * multiplicativeExpression:
     *     unaryExpression
     *     (("mul" | "div" | "mod") unaryExpression)*
     */
    private Expression parseMultiplicativeExpression() {
        Expression expression = parseUnaryExpression();

        while (match(
                TokenType.MUL,
                TokenType.DIV,
                TokenType.MOD
        )) {
            Token operatorToken = previous();
            Expression right = parseUnaryExpression();

            expression = new BinaryExpression(
                    expression,
                    toBinaryOperator(operatorToken),
                    right
            );
        }

        return expression;
    }

    /**
     * unaryExpression:
     *     "not" unaryExpression
     *     | primaryExpression
     */
    private Expression parseUnaryExpression() {
        if (match(TokenType.NOT)) {
            return new UnaryExpression(
                    UnaryOperator.NOT,
                    parseUnaryExpression()
            );
        }

        return parsePrimaryExpression();
    }

    /**
     * primaryExpression:
     *     literal
     *     | propertyPath
     *     | functionCall
     *     | "(" expression ")"
     */
    private Expression parsePrimaryExpression() {
        if (match(
                TokenType.STRING,
                TokenType.INTEGER,
                TokenType.DECIMAL,
                TokenType.BOOLEAN,
                TokenType.NULL
        )) {
            return createLiteralExpression(previous());
        }

        if (match(TokenType.IDENTIFIER)) {
            return parseIdentifierExpression(previous());
        }

        if (match(TokenType.LEFT_PAREN)) {
            Expression groupedExpression = parseExpression();

            consume(
                    TokenType.RIGHT_PAREN,
                    "Expected ')' after grouped expression"
            );

            return groupedExpression;
        }

        throw error("Expected an expression");
    }

    /**
     * An identifier may begin either:
     *
     * - a function call: contains(...)
     * - a property: Price
     * - a property path: Address/City
     */
    private Expression parseIdentifierExpression(
            Token firstIdentifier
    ) {
        if (match(TokenType.LEFT_PAREN)) {
            return parseFunctionCall(firstIdentifier.lexeme());
        }

        List<String> pathSegments = new ArrayList<>();
        pathSegments.add(firstIdentifier.lexeme());

        while (match(TokenType.SLASH)) {
            Token segment = consume(
                    TokenType.IDENTIFIER,
                    "Expected a property name after '/'"
            );

            pathSegments.add(segment.lexeme());
        }

        return new PropertyExpression(pathSegments);
    }

    /**
     * Parses a function after its opening parenthesis
     * has already been consumed.
     */
    private Expression parseFunctionCall(String functionName) {
        List<Expression> arguments = new ArrayList<>();

        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                arguments.add(parseExpression());
            } while (match(TokenType.COMMA));
        }

        consume(
                TokenType.RIGHT_PAREN,
                "Expected ')' after function arguments"
        );

        return new FunctionCallExpression(
                functionName,
                arguments
        );
    }

    /**
     * Parses the right side of the "in" operator.
     *
     * Example:
     * ('Ulm', 'Berlin')
     */
    private Expression parseInList() {
        consume(
                TokenType.LEFT_PAREN,
                "Expected '(' after 'in'"
        );

        if (check(TokenType.RIGHT_PAREN)) {
            throw error(
                    "The 'in' operator requires at least one list element"
            );
        }

        List<Expression> elements = new ArrayList<>();

        do {
            elements.add(parseExpression());
        } while (match(TokenType.COMMA));

        consume(
                TokenType.RIGHT_PAREN,
                "Expected ')' after 'in' list"
        );

        return new ListExpression(elements);
    }

    private LiteralExpression createLiteralExpression(Token token) {
        try {
            return switch (token.type()) {
                case STRING -> new LiteralExpression(
                        LiteralType.STRING,
                        decodeString(token.lexeme()),
                        token.lexeme()
                );

                case INTEGER -> new LiteralExpression(
                        LiteralType.INTEGER,
                        new BigInteger(token.lexeme()),
                        token.lexeme()
                );

                case DECIMAL -> new LiteralExpression(
                        LiteralType.DECIMAL,
                        new BigDecimal(token.lexeme()),
                        token.lexeme()
                );

                case BOOLEAN -> new LiteralExpression(
                        LiteralType.BOOLEAN,
                        Boolean.parseBoolean(token.lexeme()),
                        token.lexeme()
                );

                case NULL -> new LiteralExpression(
                        LiteralType.NULL,
                        null,
                        token.lexeme()
                );

                default -> throw new IllegalStateException(
                        "Token is not a literal: " + token.type()
                );
            };
        } catch (NumberFormatException exception) {
            throw new FilterParserException(
                    "Invalid numeric literal",
                    token,
                    exception
            );
        }
    }

    /**
     * Converts:
     *
     * 'O''Brien'
     *
     * into:
     *
     * O'Brien
     */
    private String decodeString(String lexeme) {
        String content = lexeme.substring(
                1,
                lexeme.length() - 1
        );

        return content.replace("''", "'");
    }

    private BinaryOperator toBinaryOperator(Token token) {
        return switch (token.type()) {
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
                            + token.type()
            );
        };
    }

    private boolean isComparisonOperator(TokenType type) {
        return switch (type) {
            case EQ, NE, GT, GE, LT, LE, IN -> true;
            default -> false;
        };
    }

    /**
     * Returns true and consumes the current token when its type
     * matches one of the provided types.
     */
    private boolean match(TokenType... tokenTypes) {
        for (TokenType tokenType : tokenTypes) {
            if (check(tokenType)) {
                advance();
                return true;
            }
        }

        return false;
    }

    /**
     * Requires a specific token type and consumes it.
     */
    private Token consume(
            TokenType expectedType,
            String errorMessage
    ) {
        if (check(expectedType)) {
            return advance();
        }

        throw error(errorMessage);
    }

    private boolean check(TokenType tokenType) {
        return currentToken().type() == tokenType;
    }

    /**
     * Consumes and returns the current token.
     *
     * The end-of-input token is never consumed.
     */
    private Token advance() {
        Token token = currentToken();

        if (!isAtEnd()) {
            currentIndex++;
        }

        return token;
    }

    private boolean isAtEnd() {
        return currentToken().type()
                == TokenType.END_OF_INPUT;
    }

    private Token currentToken() {
        return tokens.get(currentIndex);
    }

    private Token previous() {
        return tokens.get(currentIndex - 1);
    }

    private FilterParserException error(String message) {
        return new FilterParserException(
                message,
                currentToken()
        );
    }

    private static void validateEndOfInputToken(
            List<Token> tokens
    ) {
        Token lastToken = tokens.get(tokens.size() - 1);

        if (lastToken.type() != TokenType.END_OF_INPUT) {
            throw new IllegalArgumentException(
                    "Token sequence must end with END_OF_INPUT"
            );
        }

        for (int index = 0; index < tokens.size() - 1; index++) {
            if (tokens.get(index).type()
                    == TokenType.END_OF_INPUT) {
                throw new IllegalArgumentException(
                        "END_OF_INPUT may appear only as the last token"
                );
            }
        }
    }
}