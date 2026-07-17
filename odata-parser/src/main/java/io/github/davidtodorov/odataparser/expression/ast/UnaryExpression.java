package io.github.davidtodorov.odataparser.expression.ast;

import java.util.List;
import java.util.Objects;

public record UnaryExpression(
        UnaryOperator operator,
        Expression operand
) implements Expression {

    public UnaryExpression {
        Objects.requireNonNull(
                operator,
                "Unary operator cannot be null"
        );

        Objects.requireNonNull(
                operand,
                "Unary operand cannot be null"
        );
    }

    @Override
    public List<Expression> children() {
        return List.of(operand);
    }

    @Override
    public <R> R accept(ExpressionVisitor<R> visitor) {
        Objects.requireNonNull(visitor, "Expression visitor cannot be null");
        return visitor.visitUnaryExpression(this);
    }
}
