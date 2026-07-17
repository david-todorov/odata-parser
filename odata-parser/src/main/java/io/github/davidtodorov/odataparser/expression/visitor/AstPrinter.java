package io.github.davidtodorov.odataparser.expression.visitor;

import io.github.davidtodorov.odataparser.expression.ast.*;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class AstPrinter implements ExpressionVisitor<String> {

    private static final String INDENTATION = "  ";
    private static final String LINE_SEPARATOR = System.lineSeparator();

    public String print(Expression root) {
        Objects.requireNonNull(
                root,
                "Root expression cannot be null"
        );

        return root.accept(this);
    }

    @Override
    public String visitBinaryExpression(BinaryExpression expression) {
        return "BINARY " + expression.operator().keyword()
                + LINE_SEPARATOR
                + printChildren(expression.children());
    }

    @Override
    public String visitUnaryExpression(UnaryExpression expression) {
        return "UNARY " + expression.operator().keyword()
                + LINE_SEPARATOR
                + printChildren(expression.children());
    }

    @Override
    public String visitLiteralExpression(LiteralExpression expression) {
        return "LITERAL "
                + expression.literalType()
                + " "
                + expression.rawText();
    }

    @Override
    public String visitPropertyExpression(PropertyExpression expression) {
        return "PROPERTY " + expression.path();
    }

    @Override
    public String visitFunctionCallExpression(
            FunctionCallExpression expression
    ) {
        String header = "FUNCTION " + expression.functionName();

        if (expression.arguments().isEmpty()) {
            return header;
        }

        return header
                + LINE_SEPARATOR
                + printChildren(expression.children());
    }

    @Override
    public String visitListExpression(ListExpression expression) {
        return "LIST"
                + LINE_SEPARATOR
                + printChildren(expression.children());
    }

    private String printChildren(List<Expression> children) {
        return children.stream()
                .map(child -> child.accept(this))
                .map(this::indent)
                .collect(Collectors.joining(LINE_SEPARATOR));
    }

    private String indent(String subtree) {
        return subtree.lines()
                .map(line -> INDENTATION + line)
                .collect(Collectors.joining(LINE_SEPARATOR));
    }

}
