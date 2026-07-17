package io.github.davidtodorov.odataparser.filter.lexer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class FilterLexer {

    private final String input;
    private int currentPosition;

    public FilterLexer(String input) {
        this.input = Objects.requireNonNull(
                input,
                "Filter input cannot be null"
        );

        this.currentPosition = 0;
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();

        while (!isAtEnd()) {
            skipWhitespace();

            if (isAtEnd()) {
                break;
            }

            tokens.add(readNextToken());
        }

        tokens.add(new Token(
                TokenType.END_OF_INPUT,
                "",
                currentPosition,
                currentPosition
        ));

        return List.copyOf(tokens);
    }

    private Token readNextToken() {
        char currentCharacter = currentCharacter();

        if (isIdentifierStart(currentCharacter)) {
            return readIdentifierOrKeyword();
        }

        if (Character.isDigit(currentCharacter)
                || isStartOfNegativeNumber()) {
            return readNumber();
        }

        if (currentCharacter == '\'') {
            return readString();
        }

        return readStructuralToken();
    }

    private Token readIdentifierOrKeyword() {
        int start = currentPosition;

        advance();

        while (!isAtEnd()
                && isIdentifierPart(currentCharacter())) {
            advance();
        }

        String lexeme = input.substring(start, currentPosition);
        TokenType type = determineIdentifierType(lexeme);

        return new Token(
                type,
                lexeme,
                start,
                currentPosition
        );
    }

    private TokenType determineIdentifierType(String lexeme) {
        return switch (lexeme) {
            // Logical operators
            case "and" -> TokenType.AND;
            case "or" -> TokenType.OR;
            case "not" -> TokenType.NOT;

            // Comparison operators
            case "eq" -> TokenType.EQ;
            case "ne" -> TokenType.NE;
            case "gt" -> TokenType.GT;
            case "ge" -> TokenType.GE;
            case "lt" -> TokenType.LT;
            case "le" -> TokenType.LE;
            case "in" -> TokenType.IN;

            // Arithmetic operators
            case "add" -> TokenType.ADD;
            case "sub" -> TokenType.SUB;
            case "mul" -> TokenType.MUL;
            case "div" -> TokenType.DIV;
            case "mod" -> TokenType.MOD;

            // Literal keywords
            case "true", "false" -> TokenType.BOOLEAN;
            case "null" -> TokenType.NULL;

            default -> TokenType.IDENTIFIER;
        };
    }

    private Token readNumber() {
        int start = currentPosition;

        if (currentCharacter() == '-') {
            advance();
        }

        while (!isAtEnd()
                && Character.isDigit(currentCharacter())) {
            advance();
        }

        TokenType type = TokenType.INTEGER;

        if (!isAtEnd()
                && currentCharacter() == '.'
                && hasNextDigit()) {

            type = TokenType.DECIMAL;
            advance();

            while (!isAtEnd()
                    && Character.isDigit(currentCharacter())) {
                advance();
            }
        }

        String lexeme = input.substring(start, currentPosition);

        return new Token(
                type,
                lexeme,
                start,
                currentPosition
        );
    }

    private Token readString() {
        int start = currentPosition;

        // Consume the opening apostrophe.
        advance();

        while (!isAtEnd()) {
            if (currentCharacter() != '\'') {
                advance();
                continue;
            }

            if (isEscapedApostrophe()) {
                // Two apostrophes inside a string represent one apostrophe.
                advance();
                advance();
                continue;
            }

            // Consume the closing apostrophe.
            advance();

            String lexeme = input.substring(start, currentPosition);

            return new Token(
                    TokenType.STRING,
                    lexeme,
                    start,
                    currentPosition
            );
        }

        throw new FilterLexerException(
                "Unterminated string literal",
                start
        );
    }

    private Token readStructuralToken() {
        int start = currentPosition;
        char character = currentCharacter();

        TokenType type = switch (character) {
            case '(' -> TokenType.LEFT_PAREN;
            case ')' -> TokenType.RIGHT_PAREN;
            case ',' -> TokenType.COMMA;
            case '/' -> TokenType.SLASH;

            default -> throw new FilterLexerException(
                    "Unexpected character '" + character + "'",
                    currentPosition
            );
        };

        advance();

        return new Token(
                type,
                String.valueOf(character),
                start,
                currentPosition
        );
    }

    private void skipWhitespace() {
        while (!isAtEnd()
                && Character.isWhitespace(currentCharacter())) {
            advance();
        }
    }

    private boolean isIdentifierStart(char character) {
        return Character.isLetter(character)
                || character == '_';
    }

    private boolean isIdentifierPart(char character) {
        return Character.isLetterOrDigit(character)
                || character == '_';
    }

    private boolean isStartOfNegativeNumber() {
        return currentCharacter() == '-'
                && currentPosition + 1 < input.length()
                && Character.isDigit(input.charAt(currentPosition + 1));
    }

    private boolean hasNextDigit() {
        return currentPosition + 1 < input.length()
                && Character.isDigit(input.charAt(currentPosition + 1));
    }

    private boolean isEscapedApostrophe() {
        return currentPosition + 1 < input.length()
                && input.charAt(currentPosition + 1) == '\'';
    }

    private char currentCharacter() {
        return input.charAt(currentPosition);
    }

    private void advance() {
        currentPosition++;
    }

    private boolean isAtEnd() {
        return currentPosition >= input.length();
    }
}
