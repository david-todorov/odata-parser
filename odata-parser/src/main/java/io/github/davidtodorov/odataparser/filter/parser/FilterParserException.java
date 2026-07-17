package io.github.davidtodorov.odataparser.filter.parser;

import io.github.davidtodorov.odataparser.filter.lexer.FilterToken;
import io.github.davidtodorov.odataparser.filter.lexer.FilterTokenType;

import java.util.Objects;

public class FilterParserException extends RuntimeException {

    private final FilterToken filterToken;

    public FilterParserException(String message, FilterToken filterToken) {
        this(message, filterToken, null);
    }

    public FilterParserException(
            String message,
            FilterToken filterToken,
            Throwable cause
    ) {
        super(createMessage(message, filterToken), cause);
        this.filterToken = filterToken;
    }

    public FilterToken token() {
        return filterToken;
    }

    public int position() {
        return filterToken.start();
    }

    private static String createMessage(
            String message,
            FilterToken filterToken
    ) {
        Objects.requireNonNull(
                filterToken,
                "Error token cannot be null"
        );

        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException(
                    "Error message cannot be null or blank"
            );
        }

        String found = filterToken.type() == FilterTokenType.END_OF_INPUT
                ? "end of input"
                : "'" + filterToken.lexeme() + "' (" + filterToken.type() + ")";

        return message
                + " at position "
                + filterToken.start()
                + ". Found "
                + found
                + ".";
    }
}
