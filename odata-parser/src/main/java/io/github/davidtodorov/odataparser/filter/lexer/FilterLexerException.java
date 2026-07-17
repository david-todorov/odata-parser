package io.github.davidtodorov.odataparser.filter.lexer;

public class FilterLexerException extends RuntimeException {

    private final int position;

    public FilterLexerException(String message, int position) {
        super(createMessage(message, position));

        if (position < 0) {
            throw new IllegalArgumentException(
                    "Error position cannot be negative"
            );
        }

        this.position = position;
    }

    public int position() {
        return position;
    }

    private static String createMessage(String message, int position) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException(
                    "Error message cannot be null or blank"
            );
        }

        if (position < 0) {
            throw new IllegalArgumentException(
                    "Error position cannot be negative"
            );
        }

        return message + " at position " + position;
    }
}
