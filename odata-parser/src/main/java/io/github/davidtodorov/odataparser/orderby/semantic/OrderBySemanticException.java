package io.github.davidtodorov.odataparser.orderby.semantic;

import io.github.davidtodorov.odataparser.common.metadata.SourceSpan;
import io.github.davidtodorov.odataparser.orderby.ast.OrderByItem;

import java.util.Objects;

public final class OrderBySemanticException
        extends RuntimeException {

    private final SourceSpan sourceSpan;

    public OrderBySemanticException(
            String message,
            OrderByItem item
    ) {
        this(
                message,
                requireItem(item).sourceSpan(),
                null
        );
    }

    public OrderBySemanticException(
            String message,
            OrderByItem item,
            Throwable cause
    ) {
        this(
                message,
                requireItem(item).sourceSpan(),
                cause
        );
    }

    public OrderBySemanticException(
            String message,
            SourceSpan sourceSpan
    ) {
        this(
                message,
                sourceSpan,
                null
        );
    }

    public OrderBySemanticException(
            String message,
            SourceSpan sourceSpan,
            Throwable cause
    ) {
        super(
                formatMessage(
                        message,
                        sourceSpan
                ),
                cause
        );

        this.sourceSpan = Objects.requireNonNull(
                sourceSpan,
                "Order-by semantic error source span cannot be null"
        );
    }

    public SourceSpan sourceSpan() {
        return sourceSpan;
    }

    public int position() {
        return sourceSpan.start();
    }

    private static String formatMessage(
            String message,
            SourceSpan sourceSpan
    ) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException(
                    "Order-by semantic error message cannot be null or blank"
            );
        }

        Objects.requireNonNull(
                sourceSpan,
                "Order-by semantic error source span cannot be null"
        );

        if (sourceSpan.isUnknown()) {
            return message
                    + " at an unknown source position.";
        }

        return message
                + " at source range ["
                + sourceSpan.start()
                + ", "
                + sourceSpan.end()
                + ").";
    }

    private static OrderByItem requireItem(
            OrderByItem item
    ) {
        return Objects.requireNonNull(
                item,
                "Order-by item cannot be null"
        );
    }
}