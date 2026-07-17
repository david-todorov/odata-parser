package io.github.davidtodorov.odataparser.expression.ast;

import java.util.List;
import java.util.Objects;

public record ListExpression(
        List<Expression> elements
) implements Expression {

    public ListExpression {
        Objects.requireNonNull(
                elements,
                "List elements cannot be null"
        );

        if (elements.isEmpty()) {
            throw new IllegalArgumentException(
                    "A list expression must contain at least one element"
            );
        }

        if (elements.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "List elements cannot contain null expressions"
            );
        }

        elements = List.copyOf(elements);
    }

    @Override
    public List<Expression> children() {
        return elements;
    }

    @Override
    public <R> R accept(ExpressionVisitor<R> visitor) {
        Objects.requireNonNull(visitor, "Expression visitor cannot be null");
        return visitor.visitListExpression(this);
    }
}
