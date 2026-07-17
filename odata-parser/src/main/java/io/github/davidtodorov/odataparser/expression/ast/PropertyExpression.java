package io.github.davidtodorov.odataparser.expression.ast;

import java.util.List;
import java.util.Objects;

public record PropertyExpression(List<String> pathSegments) implements Expression{
    public PropertyExpression {
        Objects.requireNonNull(pathSegments, "Path segments cannot be null");

        if (pathSegments.isEmpty()) {
            throw new IllegalArgumentException(
                    "A property expression must contain at least one path segment"
            );
        }

        if (pathSegments.stream().anyMatch(
                segment -> segment == null || segment.isBlank()
        )) {
            throw new IllegalArgumentException(
                    "Property path segments cannot be null or blank"
            );
        }

        pathSegments = List.copyOf(pathSegments);
    }

    public String path() {
        return String.join("/", pathSegments);
    }

    @Override
    public <R> R accept(ExpressionVisitor<R> visitor) {
        Objects.requireNonNull(visitor, "Expression visitor cannot be null");
        return visitor.visitPropertyExpression(this);
    }
}
