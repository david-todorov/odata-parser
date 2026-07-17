package io.github.davidtodorov.odataparser.filter.ast;

import io.github.davidtodorov.odataparser.filter.metadata.ExpressionMetadata;
import io.github.davidtodorov.odataparser.common.metadata.SourceSpan;
import io.github.davidtodorov.odataparser.common.type.ExpressionType;

import java.util.List;
import java.util.Objects;

public record BinaryFilterExpression(
        FilterExpression left,
        BinaryOperator operator,
        FilterExpression right,
        ExpressionMetadata metadata
) implements FilterExpression {

    /**
     * Compatibility constructor.
     */
    public BinaryFilterExpression(
            FilterExpression left,
            BinaryOperator operator,
            FilterExpression right
    ) {
        this(
                left,
                operator,
                right,
                createInitialMetadata(left, operator, right)
        );
    }

    public BinaryFilterExpression {
        Objects.requireNonNull(
                left,
                "Left expression cannot be null"
        );

        Objects.requireNonNull(
                operator,
                "Binary operator cannot be null"
        );

        Objects.requireNonNull(
                right,
                "Right expression cannot be null"
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

        return visitor.visitBinaryExpression(this);
    }

    @Override
    public List<FilterExpression> children() {
        return List.of(left, right);
    }

    private static ExpressionMetadata createInitialMetadata(
            FilterExpression left,
            BinaryOperator operator,
            FilterExpression right
    ) {
        Objects.requireNonNull(left, "Left expression cannot be null");
        Objects.requireNonNull(operator, "Binary operator cannot be null");
        Objects.requireNonNull(right, "Right expression cannot be null");

        ExpressionType initialType = switch (operator) {
            case AND, OR,
                 EQ, NE, GT, GE, LT, LE, IN ->
                    ExpressionType.BOOLEAN;

            case ADD, SUB, MUL, DIV, MOD ->
                    ExpressionType.UNKNOWN;
        };

        SourceSpan span = left.sourceSpan().cover(right.sourceSpan());

        return new ExpressionMetadata(span, initialType);
    }
}