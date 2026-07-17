package io.github.davidtodorov.odataparser.filter.visitor;

import io.github.davidtodorov.odataparser.filter.ast.BinaryFilterExpression;
import io.github.davidtodorov.odataparser.filter.ast.FilterExpression;
import io.github.davidtodorov.odataparser.filter.ast.FilterExpressionVisitor;
import io.github.davidtodorov.odataparser.filter.ast.FunctionCallFilterExpression;
import io.github.davidtodorov.odataparser.filter.ast.ListFilterExpression;
import io.github.davidtodorov.odataparser.filter.ast.LiteralFilterExpression;
import io.github.davidtodorov.odataparser.filter.ast.PropertyFilterExpression;
import io.github.davidtodorov.odataparser.filter.ast.UnaryFilterExpression;
import io.github.davidtodorov.odataparser.common.metadata.SourceSpan;
import io.github.davidtodorov.odataparser.schema.ResolvedPathSegment;
import io.github.davidtodorov.odataparser.schema.ResolvedPropertyPath;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class FilterAstPrinter implements FilterExpressionVisitor<String> {

    private static final String INDENTATION = "  ";
    private static final String LINE_SEPARATOR =
            System.lineSeparator();

    public String print(FilterExpression root) {
        Objects.requireNonNull(
                root,
                "Root expression cannot be null"
        );

        return root.accept(this);
    }

    @Override
    public String visitBinaryExpression(
            BinaryFilterExpression expression
    ) {
        String title =
                "BINARY " + expression.operator().keyword();

        return renderNode(
                title,
                expression,
                List.of(),
                expression.children()
        );
    }

    @Override
    public String visitUnaryExpression(
            UnaryFilterExpression expression
    ) {
        String title =
                "UNARY " + expression.operator().keyword();

        return renderNode(
                title,
                expression,
                List.of(),
                expression.children()
        );
    }

    @Override
    public String visitLiteralExpression(
            LiteralFilterExpression expression
    ) {
        String title =
                "LITERAL "
                        + expression.literalType()
                        + " "
                        + expression.rawText();

        List<String> details = List.of(
                "value=" + formatValue(expression.value())
        );

        return renderNode(
                title,
                expression,
                details,
                List.of()
        );
    }

    @Override
    public String visitPropertyExpression(
            PropertyFilterExpression expression
    ) {
        String title =
                "PROPERTY " + expression.path();

        return renderNode(
                title,
                expression,
                createPropertyDetails(expression),
                List.of()
        );
    }

    @Override
    public String visitFunctionCallExpression(
            FunctionCallFilterExpression expression
    ) {
        String title =
                "FUNCTION " + expression.functionName();

        List<String> details = List.of(
                "argument-count="
                        + expression.arguments().size()
        );

        return renderNode(
                title,
                expression,
                details,
                expression.children()
        );
    }

    @Override
    public String visitListExpression(
            ListFilterExpression expression
    ) {
        List<String> details = List.of(
                "element-count="
                        + expression.elements().size()
        );

        return renderNode(
                "LIST",
                expression,
                details,
                expression.children()
        );
    }

    private String renderNode(
            String title,
            FilterExpression filterExpression,
            List<String> details,
            List<FilterExpression> children
    ) {
        StringBuilder output = new StringBuilder();

        output.append(title)
                .append(" | type=")
                .append(filterExpression.expressionType())
                .append(" | span=")
                .append(formatSpan(filterExpression.sourceSpan()));

        for (String detail : details) {
            output.append(LINE_SEPARATOR)
                    .append(indent(detail));
        }

        for (FilterExpression child : children) {
            output.append(LINE_SEPARATOR)
                    .append(indent(child.accept(this)));
        }

        return output.toString();
    }

    private List<String> createPropertyDetails(
            PropertyFilterExpression expression
    ) {
        List<String> details = new ArrayList<>();

        Optional<ResolvedPropertyPath> resolvedPath =
                expression.resolvedPath();

        if (resolvedPath.isEmpty()) {
            details.add("resolution=UNRESOLVED");
            return List.copyOf(details);
        }

        ResolvedPropertyPath path = resolvedPath.get();

        details.add("resolution=RESOLVED");
        details.add("external-path=" + path.externalPath());
        details.add("mapped-path=" + path.mappedPath());
        details.add(
                "java-type=" + path.javaType().getTypeName()
        );

        details.add("segments:");

        for (ResolvedPathSegment segment : path.segments()) {
            details.add(
                    INDENTATION + describeSegment(segment)
            );
        }

        return List.copyOf(details);
    }

    private String describeSegment(
            ResolvedPathSegment segment
    ) {
        StringBuilder description = new StringBuilder();

        description.append(segment.externalName())
                .append(" -> ")
                .append(segment.mappedName())
                .append(" | declared-in=")
                .append(segment.declaringSchemaName())
                .append(" | kind=")
                .append(segment.kind())
                .append(" | cardinality=")
                .append(segment.cardinality())
                .append(" | java-type=")
                .append(segment.javaType().getTypeName());

        segment.expressionType().ifPresent(type ->
                description.append(" | type=")
                        .append(type)
        );

        segment.targetSchemaName().ifPresent(target ->
                description.append(" | target-schema=")
                        .append(target)
        );

        return description.toString();
    }

    private String formatSpan(SourceSpan sourceSpan) {
        if (sourceSpan.isUnknown()) {
            return "UNKNOWN";
        }

        return "["
                + sourceSpan.start()
                + ", "
                + sourceSpan.end()
                + ")";
    }

    private String formatValue(Object value) {
        if (value == null) {
            return "null";
        }

        if (value instanceof String stringValue) {
            return "\"" + stringValue + "\"";
        }

        return value.toString();
    }

    private String indent(String text) {
        return text.lines()
                .map(line -> INDENTATION + line)
                .reduce(
                        (left, right) ->
                                left
                                        + LINE_SEPARATOR
                                        + right
                )
                .orElse("");
    }
}