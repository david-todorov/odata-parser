package io.github.davidtodorov.odataparser.search.parser;

public final class SearchParserException extends RuntimeException {

    private final int position;

    public SearchParserException(
            String message,
            int position
    ) {
        super(formatMessage(message, position));
        this.position = validatePosition(position);
    }

    public SearchParserException(
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
                    "Search parser error message cannot be null or blank"
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
                    "Search parser error position cannot be negative"
            );
        }

        return position;
    }
}