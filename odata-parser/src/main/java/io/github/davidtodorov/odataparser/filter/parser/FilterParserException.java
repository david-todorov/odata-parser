package io.github.davidtodorov.odataparser.filter.parser;

import io.github.davidtodorov.odataparser.filter.lexer.Token;
import io.github.davidtodorov.odataparser.filter.lexer.TokenType;

import java.util.Objects;

public class FilterParserException extends RuntimeException {

    private final Token token;

    public FilterParserException(String message, Token token) {
        this(message, token, null);
    }

    public FilterParserException(
            String message,
            Token token,
            Throwable cause
    ) {
        super(createMessage(message, token), cause);
        this.token = token;
    }

    public Token token() {
        return token;
    }

    public int position() {
        return token.start();
    }

    private static String createMessage(
            String message,
            Token token
    ) {
        Objects.requireNonNull(
                token,
                "Error token cannot be null"
        );

        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException(
                    "Error message cannot be null or blank"
            );
        }

        String found = token.type() == TokenType.END_OF_INPUT
                ? "end of input"
                : "'" + token.lexeme() + "' (" + token.type() + ")";

        return message
                + " at position "
                + token.start()
                + ". Found "
                + found
                + ".";
    }
}
