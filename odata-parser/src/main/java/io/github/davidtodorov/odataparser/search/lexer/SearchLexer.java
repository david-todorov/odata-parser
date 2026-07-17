package io.github.davidtodorov.odataparser.search.lexer;

import io.github.davidtodorov.odataparser.common.metadata.SourceSpan;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class SearchLexer {

    private final String input;
    private int position;

    public SearchLexer(String input) {
        this.input = Objects.requireNonNull(
                input,
                "Search input cannot be null"
        );
    }

    public List<SearchToken> tokenize() {
        position = 0;

        List<SearchToken> tokens = new ArrayList<>();

        while (true) {
            skipWhitespace();

            if (isAtEnd()) {
                tokens.add(
                        SearchToken.endOfInput(position)
                );

                return List.copyOf(tokens);
            }

            SearchToken token = switch (peek()) {
                case '(' -> readSingleCharacterToken(
                        SearchTokenType.LEFT_PARENTHESIS
                );

                case ')' -> readSingleCharacterToken(
                        SearchTokenType.RIGHT_PARENTHESIS
                );

                case '"' -> readPhrase();

                default -> readTermOrOperator();
            };

            tokens.add(token);
        }
    }

    private SearchToken readSingleCharacterToken(
            SearchTokenType type
    ) {
        int start = position;

        char character = advance();
        String lexeme = String.valueOf(character);

        return new SearchToken(
                type,
                lexeme,
                new SourceSpan(
                        start,
                        position
                )
        );
    }


    private SearchToken readPhrase() {
        int start = position;

        // Consume the opening quotation mark.
        advance();

        StringBuilder value = new StringBuilder();
        boolean terminated = false;

        while (!isAtEnd()) {
            char current = advance();

            if (current == '"') {
                terminated = true;
                break;
            }

            if (current == '\\') {
                readEscapedPhraseCharacter(
                        value,
                        position - 1
                );
            } else {
                value.append(current);
            }
        }

        if (!terminated) {
            throw new SearchLexerException(
                    "Unterminated search phrase",
                    start
            );
        }

        if (value.toString().isBlank()) {
            throw new SearchLexerException(
                    "Search phrase cannot be empty or blank",
                    start
            );
        }

        String lexeme = input.substring(
                start,
                position
        );

        return new SearchToken(
                SearchTokenType.PHRASE,
                lexeme,
                value.toString(),
                new SourceSpan(
                        start,
                        position
                )
        );
    }

    private void readEscapedPhraseCharacter(
            StringBuilder value,
            int escapePosition
    ) {
        if (isAtEnd()) {
            throw new SearchLexerException(
                    "Unterminated escape sequence in search phrase",
                    escapePosition
            );
        }

        char escapedCharacter = advance();

        switch (escapedCharacter) {
            case '"' -> value.append('"');
            case '\\' -> value.append('\\');

            default -> throw new SearchLexerException(
                    "Unsupported search phrase escape sequence '\\"
                            + escapedCharacter
                            + "'",
                    escapePosition
            );
        }
    }

    private SearchToken readTermOrOperator() {
        int start = position;

        while (!isAtEnd()
                && !isTermDelimiter(peek())) {

            char current = peek();

            if (isUnsupportedUnquotedCharacter(current)) {
                throw new SearchLexerException(
                        "Unsupported character '"
                                + current
                                + "' in unquoted search term",
                        position
                );
            }

            advance();
        }

        if (start == position) {
            throw new SearchLexerException(
                    "Unexpected character '"
                            + peek()
                            + "'",
                    position
            );
        }

        String lexeme = input.substring(
                start,
                position
        );

        SearchTokenType type = classifyTerm(lexeme);

        return new SearchToken(
                type,
                lexeme,
                new SourceSpan(
                        start,
                        position
                )
        );
    }

    private SearchTokenType classifyTerm(
            String lexeme
    ) {
        return switch (lexeme) {
            case "AND" -> SearchTokenType.AND;
            case "OR" -> SearchTokenType.OR;
            case "NOT" -> SearchTokenType.NOT;
            default -> SearchTokenType.TERM;
        };
    }

    private boolean isTermDelimiter(
            char character
    ) {
        return Character.isWhitespace(character)
                || character == '('
                || character == ')'
                || character == '"';
    }

    private boolean isUnsupportedUnquotedCharacter(
            char character
    ) {
        return character == '&'
                || character == '|'
                || character == '!'
                || character == '\\'
                || Character.isISOControl(character);
    }

    private void skipWhitespace() {
        while (!isAtEnd()
                && Character.isWhitespace(peek())) {

            advance();
        }
    }

    private char peek() {
        return input.charAt(position);
    }

    private char advance() {
        return input.charAt(position++);
    }

    private boolean isAtEnd() {
        return position >= input.length();
    }
}