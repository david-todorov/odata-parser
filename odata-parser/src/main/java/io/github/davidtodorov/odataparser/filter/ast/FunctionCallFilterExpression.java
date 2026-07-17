package io.github.davidtodorov.odataparser.filter.ast;

import io.github.davidtodorov.odataparser.filter.metadata.ExpressionMetadata;
import io.github.davidtodorov.odataparser.common.metadata.SourceSpan;

import java.util.List;
import java.util.Objects;

public record FunctionCallFilterExpression(
        String functionName,
        List<FilterExpression> arguments,
        ExpressionMetadata metadata
) implements FilterExpression {

    /**
     * Compatibility constructor.
     */
    public FunctionCallFilterExpression(
            String functionName,
            List<FilterExpression> arguments
    ) {
        this(
                functionName,
                arguments,
                ExpressionMetadata.unresolved(SourceSpan.unknown())
        );
    }

    public FunctionCallFilterExpression {
        Objects.requireNonNull(
                functionName,
                "Function name cannot be null"
        );

        Objects.requireNonNull(
                arguments,
                "Function arguments cannot be null"
        );

        Objects.requireNonNull(
                metadata,
                "Expression metadata cannot be null"
        );

        if (functionName.isBlank()) {
            throw new IllegalArgumentException(
                    "Function name cannot be blank"
            );
        }

        if (arguments.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "Function arguments cannot contain null elements"
            );
        }

        arguments = List.copyOf(arguments);
    }

    @Override
    public <R> R accept(FilterExpressionVisitor<R> visitor) {
        Objects.requireNonNull(
                visitor,
                "Expression visitor cannot be null"
        );

        return visitor.visitFunctionCallExpression(this);
    }

    @Override
    public List<FilterExpression> children() {
        return arguments;
    }
}