package io.github.davidtodorov.odataparser.filter.ast;

import io.github.davidtodorov.odataparser.filter.metadata.ExpressionMetadata;
import io.github.davidtodorov.odataparser.common.metadata.SourceSpan;
import io.github.davidtodorov.odataparser.common.type.ExpressionType;

import java.util.List;
import java.util.Objects;

public record ListFilterExpression(
        List<FilterExpression> elements,
        ExpressionMetadata metadata
) implements FilterExpression {


    public ListFilterExpression(List<FilterExpression> elements) {
        this(
                elements,
                new ExpressionMetadata(
                        SourceSpan.unknown(),
                        ExpressionType.COLLECTION
                )
        );
    }

    public ListFilterExpression {
        Objects.requireNonNull(
                elements,
                "List elements cannot be null"
        );

        Objects.requireNonNull(
                metadata,
                "Expression metadata cannot be null"
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
    public <R> R accept(FilterExpressionVisitor<R> visitor) {
        Objects.requireNonNull(
                visitor,
                "Expression visitor cannot be null"
        );

        return visitor.visitListExpression(this);
    }

    @Override
    public List<FilterExpression> children() {
        return elements;
    }
}