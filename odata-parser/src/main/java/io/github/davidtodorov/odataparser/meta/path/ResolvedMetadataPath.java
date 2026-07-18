package io.github.davidtodorov.odataparser.meta.path;

import io.github.davidtodorov.odataparser.common.type.ExpressionType;
import io.github.davidtodorov.odataparser.meta.EntityMetadata;

import java.util.List;
import java.util.Objects;

public record ResolvedMetadataPath(
        List<ResolvedMetadataPathSegment> segments
) {

    public ResolvedMetadataPath {
        Objects.requireNonNull(
                segments,
                "Resolved metadata path segments cannot be null"
        );

        if (segments.isEmpty()) {
            throw new IllegalArgumentException(
                    "A resolved metadata path must contain at least one segment"
            );
        }

        if (segments.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "A resolved metadata path cannot contain null segments"
            );
        }

        segments = List.copyOf(segments);

        validateSegments(segments);
    }

    public ResolvedMetadataPathSegment rootSegment() {
        return segments.getFirst();
    }

    public ResolvedMetadataPathSegment leaf() {
        return segments.getLast();
    }

    public EntityMetadata<?> rootMetadata() {
        return rootSegment().declaringMetadata();
    }

    public String externalPath() {
        return String.join(
                "/",
                externalSegments()
        );
    }

    public String mappedPath() {
        return String.join(
                "/",
                mappedSegments()
        );
    }

    public List<String> externalSegments() {
        return segments.stream()
                .map(ResolvedMetadataPathSegment::externalName)
                .toList();
    }

    public List<String> mappedSegments() {
        return segments.stream()
                .map(ResolvedMetadataPathSegment::mappedName)
                .toList();
    }

    public ExpressionType expressionType() {
        return leaf()
                .expressionType()
                .orElseThrow(
                        () -> new IllegalStateException(
                                "Resolved metadata path leaf has no expression type"
                        )
                );
    }

    public Class<?> javaType() {
        return leaf().javaType();
    }

    public boolean containsNavigation() {
        return segments.stream()
                .anyMatch(
                        ResolvedMetadataPathSegment::isNavigation
                );
    }

    public int size() {
        return segments.size();
    }

    private static void validateSegments(
            List<ResolvedMetadataPathSegment> segments
    ) {
        int lastIndex = segments.size() - 1;

        for (int index = 0; index < segments.size(); index++) {
            ResolvedMetadataPathSegment current =
                    segments.get(index);

            if (index == lastIndex) {
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
            ResolvedMetadataPathSegment leaf
    ) {
        if (!leaf.isPrimitive()) {
            throw new IllegalArgumentException(
                    "The final path segment must be primitive, but '"
                            + leaf.externalName()
                            + "' is navigation"
            );
        }

        if (leaf.expressionType().isEmpty()) {
            throw new IllegalArgumentException(
                    "The final primitive property '"
                            + leaf.externalName()
                            + "' must define an expression type"
            );
        }
    }

    private static void validateIntermediateSegment(
            ResolvedMetadataPathSegment current,
            ResolvedMetadataPathSegment next
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

        EntityMetadata<?> expectedTarget =
                current.targetMetadata()
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "Navigation property '"
                                                + current.externalName()
                                                + "' has no target metadata"
                                )
                        );

        EntityMetadata<?> actualDeclaringMetadata =
                next.declaringMetadata();

        if (expectedTarget != actualDeclaringMetadata) {
            throw new IllegalArgumentException(
                    "Navigation property '"
                            + current.externalName()
                            + "' targets metadata '"
                            + expectedTarget.name()
                            + "', but the next segment is declared by metadata '"
                            + actualDeclaringMetadata.name()
                            + "'"
            );
        }
    }
}