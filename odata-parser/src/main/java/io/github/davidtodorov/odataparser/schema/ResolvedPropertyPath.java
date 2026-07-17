package io.github.davidtodorov.odataparser.schema;

import io.github.davidtodorov.odataparser.common.type.ExpressionType;

import java.util.List;
import java.util.Objects;

public record ResolvedPropertyPath(
        List<ResolvedPathSegment> segments
) {

    public ResolvedPropertyPath {
        Objects.requireNonNull(
                segments,
                "Resolved path segments cannot be null"
        );

        if (segments.isEmpty()) {
            throw new IllegalArgumentException(
                    "A resolved property path must contain at least one segment"
            );
        }

        if (segments.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "Resolved property path cannot contain null segments"
            );
        }

        segments = List.copyOf(segments);

        validateSegments(segments);
    }

    public ResolvedPathSegment leaf() {
        return segments.getLast();
    }

    public String externalPath() {
        return String.join("/", externalSegments());
    }

    public String mappedPath() {
        return String.join("/", mappedSegments());
    }

    public List<String> externalSegments() {
        return segments.stream()
                .map(ResolvedPathSegment::externalName)
                .toList();
    }

    public List<String> mappedSegments() {
        return segments.stream()
                .map(ResolvedPathSegment::mappedName)
                .toList();
    }

    public ExpressionType expressionType() {
        return leaf()
                .expressionType()
                .orElseThrow(() -> new IllegalStateException(
                        "Resolved property path leaf has no expression type"
                ));
    }

    public Class<?> javaType() {
        return leaf().javaType();
    }

    public boolean containsNavigation() {
        return segments.stream()
                .anyMatch(ResolvedPathSegment::isNavigation);
    }

    private static void validateSegments(
            List<ResolvedPathSegment> segments
    ) {
        int lastIndex = segments.size() - 1;

        for (int index = 0; index < segments.size(); index++) {
            ResolvedPathSegment current = segments.get(index);
            boolean isLeaf = index == lastIndex;

            if (isLeaf) {
                validateLeaf(current);
            } else {
                validateIntermediateSegment(
                        current,
                        segments.get(index + 1)
                );
            }
        }
    }

    private static void validateLeaf(
            ResolvedPathSegment leaf
    ) {
        if (!leaf.isPrimitive()) {
            throw new IllegalArgumentException(
                    "The final path segment must be primitive, but '"
                            + leaf.externalName()
                            + "' is navigation"
            );
        }

        if (leaf.isCollection()) {
            throw new IllegalArgumentException(
                    "The final primitive property cannot be a collection"
            );
        }

        if (leaf.expressionType().isEmpty()) {
            throw new IllegalArgumentException(
                    "The final primitive property must define an expression type"
            );
        }
    }

    private static void validateIntermediateSegment(
            ResolvedPathSegment current,
            ResolvedPathSegment next
    ) {
        if (!current.isNavigation()) {
            throw new IllegalArgumentException(
                    "Path continues after non-navigation property '"
                            + current.externalName()
                            + "'"
            );
        }

        if (current.isCollection()) {
            throw new IllegalArgumentException(
                    "Collection navigation property '"
                            + current.externalName()
                            + "' cannot be traversed as a scalar path"
            );
        }

        String expectedTargetSchema = current
                .targetSchemaName()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Navigation property '"
                                + current.externalName()
                                + "' has no target schema"
                ));

        if (!expectedTargetSchema.equals(next.declaringSchemaName())) {
            throw new IllegalArgumentException(
                    "Navigation property '"
                            + current.externalName()
                            + "' targets schema '"
                            + expectedTargetSchema
                            + "', but the next segment is declared by schema '"
                            + next.declaringSchemaName()
                            + "'"
            );
        }
    }
}
