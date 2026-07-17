package io.github.davidtodorov.odataparser.orderby.ast;

import io.github.davidtodorov.odataparser.common.metadata.SourceSpan;
import io.github.davidtodorov.odataparser.common.type.ExpressionType;
import io.github.davidtodorov.odataparser.schema.ResolvedPropertyPath;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record OrderByItem(
        List<String> pathSegments,
        OrderByDirection direction,
        Optional<ResolvedPropertyPath> resolvedPath,
        SourceSpan sourceSpan
) {

    public OrderByItem(
            List<String> pathSegments,
            OrderByDirection direction,
            SourceSpan sourceSpan
    ) {
        this(
                pathSegments,
                direction,
                Optional.empty(),
                sourceSpan
        );
    }

    public OrderByItem(
            List<String> pathSegments,
            OrderByDirection direction
    ) {
        this(
                pathSegments,
                direction,
                Optional.empty(),
                SourceSpan.unknown()
        );
    }

    public OrderByItem {
        Objects.requireNonNull(
                pathSegments,
                "Order-by path segments cannot be null"
        );

        Objects.requireNonNull(
                direction,
                "Order-by direction cannot be null"
        );

        Objects.requireNonNull(
                resolvedPath,
                "Resolved order-by path cannot be null"
        );

        Objects.requireNonNull(
                sourceSpan,
                "Order-by source span cannot be null"
        );

        if (pathSegments.isEmpty()) {
            throw new IllegalArgumentException(
                    "An order-by item must contain at least one path segment"
            );
        }

        for (int index = 0;
             index < pathSegments.size();
             index++) {

            String segment = pathSegments.get(index);

            if (segment == null || segment.isBlank()) {
                throw new IllegalArgumentException(
                        "Order-by path segment at index "
                                + index
                                + " cannot be null or blank"
                );
            }
        }

        pathSegments = List.copyOf(pathSegments);

        List<String> finalPathSegments = pathSegments;
        resolvedPath.ifPresent(path ->
                validateResolvedPath(
                        finalPathSegments,
                        path
                )
        );
    }

    public String externalPath() {
        return String.join("/", pathSegments);
    }

    public boolean isResolved() {
        return resolvedPath.isPresent();
    }

    public Optional<String> mappedPath() {
        return resolvedPath.map(
                ResolvedPropertyPath::mappedPath
        );
    }

    public Optional<ExpressionType> expressionType() {
        return resolvedPath.map(
                ResolvedPropertyPath::expressionType
        );
    }

    public Optional<Class<?>> javaType() {
        return resolvedPath.map(
                ResolvedPropertyPath::javaType
        );
    }

    public OrderByItem withResolvedPath(
            ResolvedPropertyPath resolvedPropertyPath
    ) {
        Objects.requireNonNull(
                resolvedPropertyPath,
                "Resolved property path cannot be null"
        );

        return new OrderByItem(
                pathSegments,
                direction,
                Optional.of(resolvedPropertyPath),
                sourceSpan
        );
    }

    private static void validateResolvedPath(
            List<String> pathSegments,
            ResolvedPropertyPath resolvedPath
    ) {
        if (!pathSegments.equals(
                resolvedPath.externalSegments()
        )) {
            throw new IllegalArgumentException(
                    "Resolved property path does not match the order-by item. "
                            + "Order-by path: '"
                            + String.join("/", pathSegments)
                            + "', resolved path: '"
                            + resolvedPath.externalPath()
                            + "'"
            );
        }
    }
}