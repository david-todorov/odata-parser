package io.github.davidtodorov.odataparser.expression.ast;

import java.util.List;

public interface Expression {

    <R> R accept(ExpressionVisitor<R> visitor);

    default List<Expression> children() {
        return List.of();
    }
}
