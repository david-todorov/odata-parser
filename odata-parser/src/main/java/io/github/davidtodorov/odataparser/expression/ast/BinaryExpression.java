package io.github.davidtodorov.odataparser.expression.ast;

import java.util.List;
import java.util.Objects;

public record BinaryExpression(
        Expression left,
        BinaryOperator operator,
        Expression right
) implements Expression {

    public BinaryExpression {
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
    }

    @Override
    public <R> R accept(ExpressionVisitor<R> visitor) {
        Objects.requireNonNull(
                visitor,
                "Expression visitor cannot be null"
        );

        return visitor.visitBinaryExpression(this);
    }

    @Override
    public List<Expression> children() {
        return List.of(left, right);
    }
}
