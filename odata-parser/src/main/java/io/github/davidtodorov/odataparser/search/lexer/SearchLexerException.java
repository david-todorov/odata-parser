package io.github.davidtodorov.odataparser.search.lexer;

public final class SearchLexerException extends RuntimeException {

    private final int position;

    public SearchLexerException(
            String message,
            int position
    ) {
        super(formatMessage(message, position));
        this.position = validatePosition(position);
    }

    public SearchLexerException(
            String message,
            int position,
            Throwable cause
    ) {
        super(
                formatMessage(message, position),
                cause
        );

        this.position = validatePosition(position);
    }

    public int position() {
        return position;
    }

    private static String formatMessage(
            String message,
            int position
    ) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException(
                    "Search lexer error message cannot be null or blank"
            );
        }

        validatePosition(position);

        return message + " at position " + position + ".";
    }

    private static int validatePosition(
            int position
    ) {
        if (position < 0) {
            throw new IllegalArgumentException(
                    "Search lexer error position cannot be negative"
            );
        }

        return position;
    }
}
