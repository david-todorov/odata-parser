package io.github.davidtodorov.odataparser.filter.ast;

public interface FilterExpressionVisitor<R> {

    R visitBinaryExpression(BinaryFilterExpression expression);

    R visitUnaryExpression(UnaryFilterExpression expression);

    R visitLiteralExpression(LiteralFilterExpression expression);

    R visitPropertyExpression(PropertyFilterExpression expression);

    R visitFunctionCallExpression(FunctionCallFilterExpression expression);

    R visitListExpression(ListFilterExpression expression);
}
