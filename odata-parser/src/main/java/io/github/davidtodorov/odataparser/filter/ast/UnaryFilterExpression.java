package io.github.davidtodorov.odataparser.filter.ast;

import io.github.davidtodorov.odataparser.filter.metadata.ExpressionMetadata;
import io.github.davidtodorov.odataparser.common.type.ExpressionType;

import java.util.List;
import java.util.Objects;

public record UnaryFilterExpression(
        UnaryOperator operator,
        FilterExpression operand,
        ExpressionMetadata metadata
) implements FilterExpression {

    public UnaryFilterExpression(
            UnaryOperator operator,
            FilterExpression operand
    ) {
        this(
                operator,
                operand,
                createInitialMetadata(operator, operand)
        );
    }

    public UnaryFilterExpression {
        Objects.requireNonNull(
                operator,
                "Unary operator cannot be null"
        );

        Objects.requireNonNull(
                operand,
                "Unary operand cannot be null"
        );

        Objects.requireNonNull(
                metadata,
                "Expression metadata cannot be null"
        );
    }

    @Override
    public <R> R accept(FilterExpressionVisitor<R> visitor) {
        Objects.requireNonNull(
                visitor,
                "Expression visitor cannot be null"
        );

        return visitor.visitUnaryExpression(this);
    }

    @Override
    public List<FilterExpression> children() {
        return List.of(operand);
    }

    private static ExpressionMetadata createInitialMetadata(
            UnaryOperator operator,
            FilterExpression operand
    ) {
        Objects.requireNonNull(operator, "Unary operator cannot be null");
        Objects.requireNonNull(operand, "Unary operand cannot be null");

        return new ExpressionMetadata(
                operand.sourceSpan(),
                ExpressionType.BOOLEAN
        );
    }
}