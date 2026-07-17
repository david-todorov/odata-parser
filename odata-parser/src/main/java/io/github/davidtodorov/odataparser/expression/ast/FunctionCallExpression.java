package io.github.davidtodorov.odataparser.expression.ast;

import java.util.List;
import java.util.Objects;

public record FunctionCallExpression(
        String functionName,
        List<Expression> arguments
) implements Expression {

    public FunctionCallExpression {
        Objects.requireNonNull(
                functionName,
                "Function name cannot be null"
        );

        Objects.requireNonNull(
                arguments,
                "Function arguments cannot be null"
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
    public List<Expression> children() {
        return arguments;
    }

    @Override
    public <R> R accept(ExpressionVisitor<R> visitor) {
        Objects.requireNonNull(visitor, "Expression visitor cannot be null");
        return visitor.visitFunctionCallExpression(this);
    }
}
