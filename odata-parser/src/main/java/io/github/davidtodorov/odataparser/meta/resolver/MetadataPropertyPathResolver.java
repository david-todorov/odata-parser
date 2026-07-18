package io.github.davidtodorov.odataparser.meta.resolver;

import io.github.davidtodorov.odataparser.common.type.ExpressionType;
import io.github.davidtodorov.odataparser.meta.EntityMetadata;
import io.github.davidtodorov.odataparser.meta.NavigationPropertyMetadata;
import io.github.davidtodorov.odataparser.meta.PrimitivePropertyMetadata;
import io.github.davidtodorov.odataparser.meta.PropertyMetadata;
import io.github.davidtodorov.odataparser.meta.path.ResolvedMetadataPath;
import io.github.davidtodorov.odataparser.meta.path.ResolvedMetadataPathSegment;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class MetadataPropertyPathResolver {

    private final EntityMetadata<?> rootMetadata;

    public MetadataPropertyPathResolver(
            EntityMetadata<?> rootMetadata
    ) {
        this.rootMetadata = Objects.requireNonNull(
                rootMetadata,
                "Root entity metadata cannot be null"
        );
    }

    public ResolvedMetadataPath resolve(
            List<String> pathSegments
    ) {
        List<String> validatedSegments =
                validateAndCopy(pathSegments);

        String externalPath =
                String.join("/", validatedSegments);

        EntityMetadata<?> currentMetadata =
                rootMetadata;

        List<ResolvedMetadataPathSegment> resolvedSegments =
                new ArrayList<>(validatedSegments.size());

        for (int index = 0;
             index < validatedSegments.size();
             index++) {

            String segmentName =
                    validatedSegments.get(index);

            boolean isLastSegment =
                    index == validatedSegments.size() - 1;

            PropertyMetadata<?, ?> property =
                    currentMetadata
                            .findProperty(segmentName)
                            .orElse(null);

            if (property == null) {
                throw new IllegalArgumentException(
                        "Unknown property '"
                                + segmentName
                                + "' in metadata '"
                                + currentMetadata.name()
                                + "' while resolving path '"
                                + externalPath
                                + "'"
                );
            }

            resolvedSegments.add(
                    new ResolvedMetadataPathSegment(
                            currentMetadata,
                            property
                    )
            );

            if (isLastSegment) {
                validateLeafProperty(
                        property,
                        currentMetadata,
                        externalPath
                );
            } else {
                currentMetadata = resolveNextMetadata(
                        property,
                        currentMetadata,
                        externalPath
                );
            }
        }

        return new ResolvedMetadataPath(
                resolvedSegments
        );
    }

    private EntityMetadata<?> resolveNextMetadata(
            PropertyMetadata<?, ?> property,
            EntityMetadata<?> declaringMetadata,
            String externalPath
    ) {
        if (!(property
                instanceof NavigationPropertyMetadata<?, ?>
                navigation)) {

            throw new IllegalArgumentException(
                    "Property '"
                            + property.externalName()
                            + "' in metadata '"
                            + declaringMetadata.name()
                            + "' is primitive, but path '"
                            + externalPath
                            + "' continues after it"
            );
        }

        if (navigation.isCollection()) {
            throw new IllegalArgumentException(
                    "Collection navigation property '"
                            + navigation.externalName()
                            + "' in metadata '"
                            + declaringMetadata.name()
                            + "' cannot be traversed as a scalar path: '"
                            + externalPath
                            + "'"
            );
        }

        EntityMetadata<?> targetMetadata =
                navigation.targetMetadata();

        if (!navigation.javaType()
                .equals(targetMetadata.entityType())) {

            throw new IllegalStateException(
                    "Navigation property '"
                            + navigation.externalName()
                            + "' in metadata '"
                            + declaringMetadata.name()
                            + "' exposes target type '"
                            + navigation.javaType().getName()
                            + "', but its target metadata describes '"
                            + targetMetadata.entityType().getName()
                            + "'"
            );
        }

        return targetMetadata;
    }

    private void validateLeafProperty(
            PropertyMetadata<?, ?> property,
            EntityMetadata<?> declaringMetadata,
            String externalPath
    ) {
        if (!(property
                instanceof PrimitivePropertyMetadata<?, ?>
                primitive)) {

            throw new IllegalArgumentException(
                    "The final segment '"
                            + property.externalName()
                            + "' in path '"
                            + externalPath
                            + "' must be primitive, but it is navigation "
                            + "declared by metadata '"
                            + declaringMetadata.name()
                            + "'"
            );
        }

        if (primitive.expressionType()
                == ExpressionType.UNKNOWN) {

            throw new IllegalArgumentException(
                    "Primitive property '"
                            + primitive.externalName()
                            + "' in metadata '"
                            + declaringMetadata.name()
                            + "' has no concrete expression type"
            );
        }
    }

    private List<String> validateAndCopy(
            List<String> pathSegments
    ) {
        Objects.requireNonNull(
                pathSegments,
                "Property path segments cannot be null"
        );

        if (pathSegments.isEmpty()) {
            throw new IllegalArgumentException(
                    "A property path must contain at least one segment"
            );
        }

        for (int index = 0;
             index < pathSegments.size();
             index++) {

            String segment =
                    pathSegments.get(index);

            if (segment == null || segment.isBlank()) {
                throw new IllegalArgumentException(
                        "Property path segment at index "
                                + index
                                + " cannot be null or blank"
                );
            }

            if (!segment.equals(segment.trim())) {
                throw new IllegalArgumentException(
                        "Property path segment at index "
                                + index
                                + " cannot contain leading or trailing whitespace: '"
                                + segment
                                + "'"
                );
            }

            if (segment.chars()
                    .anyMatch(Character::isWhitespace)) {

                throw new IllegalArgumentException(
                        "Property path segment at index "
                                + index
                                + " cannot contain whitespace: '"
                                + segment
                                + "'"
                );
            }

            if (segment.indexOf('/') >= 0) {
                throw new IllegalArgumentException(
                        "Property path segment at index "
                                + index
                                + " cannot contain '/': '"
                                + segment
                                + "'"
                );
            }
        }

        return List.copyOf(pathSegments);
    }
}