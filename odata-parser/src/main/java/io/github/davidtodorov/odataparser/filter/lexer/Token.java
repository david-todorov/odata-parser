package io.github.davidtodorov.odataparser.filter.lexer;

import java.util.Objects;

public record Token(
        TokenType type,
        String lexeme,
        int start,
        int end
) {

    public Token {
        Objects.requireNonNull(
                type,
                "Token type cannot be null"
        );

        Objects.requireNonNull(
                lexeme,
                "Token lexeme cannot be null"
        );

        if (start < 0) {
            throw new IllegalArgumentException(
                    "Token start position cannot be negative"
            );
        }

        if (end < start) {
            throw new IllegalArgumentException(
                    "Token end position cannot be before its start position"
            );
        }

        if (type != TokenType.END_OF_INPUT && lexeme.isEmpty()) {
            throw new IllegalArgumentException(
                    "Only the end-of-input token may have an empty lexeme"
            );
        }

        if (type == TokenType.END_OF_INPUT && start != end) {
            throw new IllegalArgumentException(
                    "The end-of-input token must have an empty source range"
            );
        }
    }

    public int length() {
        return end - start;
    }
}
