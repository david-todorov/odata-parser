package io.github.davidtodorov.odataparser.filter.semantic;

import io.github.davidtodorov.odataparser.filter.ast.FilterExpression;
import io.github.davidtodorov.odataparser.common.metadata.SourceSpan;

import java.util.Objects;

public class FilterSemanticException extends RuntimeException {

    private final SourceSpan sourceSpan;

    public FilterSemanticException(
            String message,
            FilterExpression filterExpression
    ) {
        this(
                message,
                requireExpression(filterExpression).sourceSpan(),
                null
        );
    }

    public FilterSemanticException(
            String message,
            FilterExpression filterExpression,
            Throwable cause
    ) {
        this(
                message,
                requireExpression(filterExpression).sourceSpan(),
                cause
        );
    }

    public FilterSemanticException(
            String message,
            SourceSpan sourceSpan
    ) {
        this(message, sourceSpan, null);
    }

    public FilterSemanticException(
            String message,
            SourceSpan sourceSpan,
            Throwable cause
    ) {
        super(
                createMessage(message, sourceSpan),
                cause
        );

        this.sourceSpan = Objects.requireNonNull(
                sourceSpan,
                "Source span cannot be null"
        );
    }

    public SourceSpan sourceSpan() {
        return sourceSpan;
    }

    public int position() {
        return sourceSpan.start();
    }

    private static String createMessage(
            String message,
            SourceSpan sourceSpan
    ) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException(
                    "Semantic error message cannot be null or blank"
            );
        }

        Objects.requireNonNull(
                sourceSpan,
                "Source span cannot be null"
        );

        if (sourceSpan.isUnknown()) {
            return message + " at an unknown source position.";
        }

        return message
                + " at source range ["
                + sourceSpan.start()
                + ", "
                + sourceSpan.end()
                + ").";
    }

    private static FilterExpression requireExpression(
            FilterExpression filterExpression
    ) {
        return Objects.requireNonNull(
                filterExpression,
                "Expression cannot be null"
        );
    }
}