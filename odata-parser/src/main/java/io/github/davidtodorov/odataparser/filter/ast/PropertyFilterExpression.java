package io.github.davidtodorov.odataparser.filter.ast;

import io.github.davidtodorov.odataparser.common.metadata.SourceSpan;
import io.github.davidtodorov.odataparser.filter.metadata.ExpressionMetadata;
import io.github.davidtodorov.odataparser.meta.path.ResolvedMetadataPath;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record PropertyFilterExpression(
        List<String> pathSegments,
        Optional<ResolvedMetadataPath> resolvedPath,
        ExpressionMetadata metadata
) implements FilterExpression {

    public PropertyFilterExpression(
            List<String> pathSegments
    ) {
        this(
                pathSegments,
                Optional.empty(),
                ExpressionMetadata.unresolved(
                        SourceSpan.unknown()
                )
        );
    }

    public PropertyFilterExpression(
            List<String> pathSegments,
            ExpressionMetadata metadata
    ) {
        this(
                pathSegments,
                Optional.empty(),
                metadata
        );
    }

    public PropertyFilterExpression {
        Objects.requireNonNull(
                pathSegments,
                "Path segments cannot be null"
        );

        Objects.requireNonNull(
                resolvedPath,
                "Resolved metadata path cannot be null"
        );

        Objects.requireNonNull(
                metadata,
                "Expression metadata cannot be null"
        );

        if (pathSegments.isEmpty()) {
            throw new IllegalArgumentException(
                    "A property expression must contain at least one path segment"
            );
        }

        if (pathSegments.stream().anyMatch(
                segment -> segment == null
                        || segment.isBlank()
        )) {
            throw new IllegalArgumentException(
                    "Property path segments cannot be null or blank"
            );
        }

        pathSegments = List.copyOf(pathSegments);

        List<String> validatedPathSegments =
                pathSegments;

        resolvedPath.ifPresent(
                path -> validateResolvedPath(
                        validatedPathSegments,
                        path,
                        metadata
                )
        );
    }

    public String path() {
        return String.join(
                "/",
                pathSegments
        );
    }

    public boolean isResolved() {
        return resolvedPath.isPresent();
    }

    public PropertyFilterExpression withResolvedPath(
            ResolvedMetadataPath resolvedMetadataPath
    ) {
        Objects.requireNonNull(
                resolvedMetadataPath,
                "Resolved metadata path cannot be null"
        );

        return new PropertyFilterExpression(
                pathSegments,
                Optional.of(
                        resolvedMetadataPath
                ),
                metadata.withType(
                        resolvedMetadataPath.expressionType()
                )
        );
    }

    @Override
    public <R> R accept(
            FilterExpressionVisitor<R> visitor
    ) {
        Objects.requireNonNull(
                visitor,
                "Expression visitor cannot be null"
        );

        return visitor.visitPropertyExpression(this);
    }

    private static void validateResolvedPath(
            List<String> pathSegments,
            ResolvedMetadataPath resolvedPath,
            ExpressionMetadata metadata
    ) {
        if (!pathSegments.equals(
                resolvedPath.externalSegments()
        )) {
            throw new IllegalArgumentException(
                    "Resolved path does not match the property expression. "
                            + "Expression path: "
                            + String.join(
                            "/",
                            pathSegments
                    )
                            + ", resolved path: "
                            + resolvedPath.externalPath()
            );
        }

        if (metadata.expressionType()
                != resolvedPath.expressionType()) {

            throw new IllegalArgumentException(
                    "Property expression type "
                            + metadata.expressionType()
                            + " does not match resolved path type "
                            + resolvedPath.expressionType()
            );
        }
    }
}