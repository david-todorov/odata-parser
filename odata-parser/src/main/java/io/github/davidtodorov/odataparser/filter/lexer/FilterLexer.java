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

    public List<FilterToken> tokenize() {
        List<FilterToken> filterTokens = new ArrayList<>();

        while (!isAtEnd()) {
            skipWhitespace();

            if (isAtEnd()) {
                break;
            }

            filterTokens.add(readNextToken());
        }

        filterTokens.add(new FilterToken(
                FilterTokenType.END_OF_INPUT,
                "",
                currentPosition,
                currentPosition
        ));

        return List.copyOf(filterTokens);
    }

    private FilterToken readNextToken() {
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

    private FilterToken readIdentifierOrKeyword() {
        int start = currentPosition;

        advance();

        while (!isAtEnd()
                && isIdentifierPart(currentCharacter())) {
            advance();
        }

        String lexeme = input.substring(start, currentPosition);
        FilterTokenType type = determineIdentifierType(lexeme);

        return new FilterToken(
                type,
                lexeme,
                start,
                currentPosition
        );
    }

    private FilterTokenType determineIdentifierType(String lexeme) {
        return switch (lexeme) {
            // Logical operators
            case "and" -> FilterTokenType.AND;
            case "or" -> FilterTokenType.OR;
            case "not" -> FilterTokenType.NOT;

            // Comparison operators
            case "eq" -> FilterTokenType.EQ;
            case "ne" -> FilterTokenType.NE;
            case "gt" -> FilterTokenType.GT;
            case "ge" -> FilterTokenType.GE;
            case "lt" -> FilterTokenType.LT;
            case "le" -> FilterTokenType.LE;
            case "in" -> FilterTokenType.IN;

            // Arithmetic operators
            case "add" -> FilterTokenType.ADD;
            case "sub" -> FilterTokenType.SUB;
            case "mul" -> FilterTokenType.MUL;
            case "div" -> FilterTokenType.DIV;
            case "mod" -> FilterTokenType.MOD;

            // Literal keywords
            case "true", "false" -> FilterTokenType.BOOLEAN;
            case "null" -> FilterTokenType.NULL;

            default -> FilterTokenType.IDENTIFIER;
        };
    }

    private FilterToken readNumber() {
        int start = currentPosition;

        if (currentCharacter() == '-') {
            advance();
        }

        while (!isAtEnd()
                && Character.isDigit(currentCharacter())) {
            advance();
        }

        FilterTokenType type = FilterTokenType.INTEGER;

        if (!isAtEnd()
                && currentCharacter() == '.'
                && hasNextDigit()) {

            type = FilterTokenType.DECIMAL;
            advance();

            while (!isAtEnd()
                    && Character.isDigit(currentCharacter())) {
                advance();
            }
        }

        String lexeme = input.substring(start, currentPosition);

        return new FilterToken(
                type,
                lexeme,
                start,
                currentPosition
        );
    }

    private FilterToken readString() {
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

            return new FilterToken(
                    FilterTokenType.STRING,
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

    private FilterToken readStructuralToken() {
        int start = currentPosition;
        char character = currentCharacter();

        FilterTokenType type = switch (character) {
            case '(' -> FilterTokenType.LEFT_PAREN;
            case ')' -> FilterTokenType.RIGHT_PAREN;
            case ',' -> FilterTokenType.COMMA;
            case '/' -> FilterTokenType.SLASH;

            default -> throw new FilterLexerException(
                    "Unexpected character '" + character + "'",
                    currentPosition
            );
        };

        advance();

        return new FilterToken(
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
