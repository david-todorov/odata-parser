package io.github.davidtodorov.odataparser.orderby.parser;

public final class OrderByParserException extends RuntimeException {

    private final int position;

    public OrderByParserException(
            String message,
            int position
    ) {
        this(
                message,
                position,
                null
        );
    }

    public OrderByParserException(
            String message,
            int position,
            Throwable cause
    ) {
        super(
                formatMessage(
                        message,
                        position
                ),
                cause
        );

        this.position = position;
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
                    "Order-by parser error message cannot be null or blank"
            );
        }

        if (position < 0) {
            throw new IllegalArgumentException(
                    "Order-by parser error position cannot be negative"
            );
        }

        return message
                + " at position "
                + position
                + ".";
    }
}