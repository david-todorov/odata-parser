package io.github.davidtodorov.odataparser.orderby.visitor;

import io.github.davidtodorov.odataparser.common.metadata.SourceSpan;
import io.github.davidtodorov.odataparser.orderby.ast.OrderByItem;
import io.github.davidtodorov.odataparser.orderby.ast.OrderByOption;
import io.github.davidtodorov.odataparser.schema.ResolvedPathSegment;
import io.github.davidtodorov.odataparser.schema.ResolvedPropertyPath;

import java.util.Objects;

public final class OrderByAstPrinter {

    private static final String INDENTATION = "  ";
    private static final String LINE_SEPARATOR =
            System.lineSeparator();

    public String print(OrderByOption option) {
        Objects.requireNonNull(
                option,
                "Order-by option cannot be null"
        );

        StringBuilder output = new StringBuilder();

        output.append("ORDER BY")
                .append(" | items=")
                .append(option.size())
                .append(" | resolution=")
                .append(
                        option.isResolved()
                                ? "RESOLVED"
                                : "UNRESOLVED"
                )
                .append(" | span=")
                .append(
                        formatSpan(option.sourceSpan())
                );

        for (int index = 0;
             index < option.size();
             index++) {

            output.append(LINE_SEPARATOR)
                    .append(
                            indent(
                                    printItem(
                                            option.get(index),
                                            index
                                    )
                            )
                    );
        }

        return output.toString();
    }

    private String printItem(
            OrderByItem item,
            int index
    ) {
        StringBuilder output = new StringBuilder();

        output.append("ITEM ")
                .append(index + 1)
                .append(" | direction=")
                .append(item.direction())
                .append(" | keyword=")
                .append(item.direction().keyword())
                .append(" | span=")
                .append(
                        formatSpan(item.sourceSpan())
                );

        output.append(LINE_SEPARATOR)
                .append(
                        indent(
                                "external-path="
                                        + item.externalPath()
                        )
                );

        output.append(LINE_SEPARATOR)
                .append(
                        indent(
                                "resolution="
                                        + (
                                        item.isResolved()
                                                ? "RESOLVED"
                                                : "UNRESOLVED"
                                )
                        )
                );

        if (item.resolvedPath().isPresent()) {
            appendResolvedPathDetails(
                    output,
                    item.resolvedPath().orElseThrow()
            );
        }

        return output.toString();
    }

    private void appendResolvedPathDetails(
            StringBuilder output,
            ResolvedPropertyPath resolvedPath
    ) {
        output.append(LINE_SEPARATOR)
                .append(
                        indent(
                                "mapped-path="
                                        + resolvedPath.mappedPath()
                        )
                );

        output.append(LINE_SEPARATOR)
                .append(
                        indent(
                                "type="
                                        + resolvedPath.expressionType()
                        )
                );

        output.append(LINE_SEPARATOR)
                .append(
                        indent(
                                "java-type="
                                        + resolvedPath
                                        .javaType()
                                        .getTypeName()
                        )
                );

        output.append(LINE_SEPARATOR)
                .append(
                        indent("segments:")
                );

        for (ResolvedPathSegment segment
                : resolvedPath.segments()) {

            output.append(LINE_SEPARATOR)
                    .append(
                            indent(
                                    indent(
                                            describeSegment(segment)
                                    )
                            )
                    );
        }
    }

    private String describeSegment(
            ResolvedPathSegment segment
    ) {
        StringBuilder description =
                new StringBuilder();

        description.append(
                        segment.externalName()
                )
                .append(" -> ")
                .append(
                        segment.mappedName()
                )
                .append(" | declared-in=")
                .append(
                        segment.declaringSchemaName()
                )
                .append(" | kind=")
                .append(
                        segment.kind()
                )
                .append(" | cardinality=")
                .append(
                        segment.cardinality()
                )
                .append(" | java-type=")
                .append(
                        segment.javaType().getTypeName()
                );

        segment.expressionType()
                .ifPresent(type ->
                        description.append(" | type=")
                                .append(type)
                );

        segment.targetSchemaName()
                .ifPresent(targetSchema ->
                        description
                                .append(" | target-schema=")
                                .append(targetSchema)
                );

        return description.toString();
    }

    private String formatSpan(
            SourceSpan sourceSpan
    ) {
        if (sourceSpan.isUnknown()) {
            return "UNKNOWN";
        }

        return "["
                + sourceSpan.start()
                + ", "
                + sourceSpan.end()
                + ")";
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