package io.github.davidtodorov.odataparser.orderby.visitor;

import io.github.davidtodorov.odataparser.common.metadata.SourceSpan;
import io.github.davidtodorov.odataparser.meta.path.ResolvedMetadataPath;
import io.github.davidtodorov.odataparser.meta.path.ResolvedMetadataPathSegment;
import io.github.davidtodorov.odataparser.orderby.ast.OrderByItem;
import io.github.davidtodorov.odataparser.orderby.ast.OrderByOption;

import java.util.Objects;

public final class OrderByAstPrinter {

    private static final String INDENTATION = "  ";
    private static final String LINE_SEPARATOR =
            System.lineSeparator();

    public String print(
            OrderByOption option
    ) {
        Objects.requireNonNull(
                option,
                "Order-by option cannot be null"
        );

        StringBuilder output =
                new StringBuilder();

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
                        formatSpan(
                                option.sourceSpan()
                        )
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
        StringBuilder output =
                new StringBuilder();

        output.append("ITEM ")
                .append(index + 1)
                .append(" | direction=")
                .append(item.direction())
                .append(" | keyword=")
                .append(item.direction().keyword())
                .append(" | span=")
                .append(
                        formatSpan(
                                item.sourceSpan()
                        )
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

        item.resolvedPath()
                .ifPresent(
                        resolvedPath ->
                                appendResolvedPathDetails(
                                        output,
                                        resolvedPath
                                )
                );

        return output.toString();
    }

    private void appendResolvedPathDetails(
            StringBuilder output,
            ResolvedMetadataPath resolvedPath
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
                        indent(
                                "root-metadata="
                                        + resolvedPath
                                        .rootMetadata()
                                        .name()
                        )
                );

        output.append(LINE_SEPARATOR)
                .append(
                        indent("segments:")
                );

        for (ResolvedMetadataPathSegment segment
                : resolvedPath.segments()) {

            output.append(LINE_SEPARATOR)
                    .append(
                            indent(
                                    indent(
                                            describeSegment(
                                                    segment
                                            )
                                    )
                            )
                    );
        }
    }

    private String describeSegment(
            ResolvedMetadataPathSegment segment
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
                        segment.declaringMetadataName()
                )
                .append(" | declaring-type=")
                .append(
                        segment.declaringEntityType()
                                .getTypeName()
                )
                .append(" | kind=")
                .append(
                        segment.kind()
                )
                .append(" | java-type=")
                .append(
                        segment.javaType()
                                .getTypeName()
                );

        segment.cardinality()
                .ifPresent(
                        cardinality ->
                                description
                                        .append(" | cardinality=")
                                        .append(cardinality)
                );

        segment.expressionType()
                .ifPresent(
                        expressionType ->
                                description
                                        .append(" | type=")
                                        .append(expressionType)
                );

        segment.targetMetadata()
                .ifPresent(
                        targetMetadata ->
                                description
                                        .append(" | target-metadata=")
                                        .append(
                                                targetMetadata.name()
                                        )
                                        .append(" | target-type=")
                                        .append(
                                                targetMetadata
                                                        .entityType()
                                                        .getTypeName()
                                        )
                );

        segment.joinPolicy()
                .ifPresent(
                        joinPolicy ->
                                description
                                        .append(" | default-join=")
                                        .append(
                                                joinPolicy.defaultJoinType()
                                        )
                                        .append(" | join-policy=")
                                        .append(
                                                joinPolicy.overridable()
                                                        ? "OVERRIDABLE"
                                                        : "FIXED"
                                        )
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

    private String indent(
            String text
    ) {
        return text.lines()
                .map(
                        line ->
                                INDENTATION + line
                )
                .reduce(
                        (left, right) ->
                                left
                                        + LINE_SEPARATOR
                                        + right
                )
                .orElse("");
    }
}