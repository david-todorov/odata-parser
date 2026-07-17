package io.github.davidtodorov.odataparser.expression.ast;

public interface ExpressionVisitor<R> {

    R visitBinaryExpression(BinaryExpression expression);

    R visitUnaryExpression(UnaryExpression expression);

    R visitLiteralExpression(LiteralExpression expression);

    R visitPropertyExpression(PropertyExpression expression);

    R visitFunctionCallExpression(FunctionCallExpression expression);

    R visitListExpression(ListExpression expression);
}
