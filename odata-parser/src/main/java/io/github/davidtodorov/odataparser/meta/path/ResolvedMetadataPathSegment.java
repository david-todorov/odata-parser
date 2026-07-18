package io.github.davidtodorov.odataparser.meta.path;

import io.github.davidtodorov.odataparser.common.type.ExpressionType;
import io.github.davidtodorov.odataparser.meta.EntityMetadata;
import io.github.davidtodorov.odataparser.meta.NavigationCardinality;
import io.github.davidtodorov.odataparser.meta.NavigationJoinPolicy;
import io.github.davidtodorov.odataparser.meta.NavigationJoinType;
import io.github.davidtodorov.odataparser.meta.NavigationPropertyMetadata;
import io.github.davidtodorov.odataparser.meta.PrimitivePropertyMetadata;
import io.github.davidtodorov.odataparser.meta.PropertyMetadata;
import io.github.davidtodorov.odataparser.meta.PropertyMetadataKind;

import java.util.Objects;
import java.util.Optional;

public record ResolvedMetadataPathSegment(
        EntityMetadata<?> declaringMetadata,
        PropertyMetadata<?, ?> propertyMetadata
) {

    public ResolvedMetadataPathSegment {
        Objects.requireNonNull(
                declaringMetadata,
                "Declaring entity metadata cannot be null"
        );

        Objects.requireNonNull(
                propertyMetadata,
                "Property metadata cannot be null"
        );

        if (!declaringMetadata
                .entityType()
                .equals(propertyMetadata.ownerType())) {

            throw new IllegalArgumentException(
                    "Property '"
                            + propertyMetadata.externalName()
                            + "' declares owner type '"
                            + propertyMetadata.ownerType().getName()
                            + "', but the declaring metadata '"
                            + declaringMetadata.name()
                            + "' describes '"
                            + declaringMetadata.entityType().getName()
                            + "'"
            );
        }
    }

    public String declaringMetadataName() {
        return declaringMetadata.name();
    }

    public Class<?> declaringEntityType() {
        return declaringMetadata.entityType();
    }

    public String externalName() {
        return propertyMetadata.externalName();
    }

    public String mappedName() {
        return propertyMetadata.mappedName();
    }

    public PropertyMetadataKind kind() {
        return propertyMetadata.kind();
    }

    public Class<?> javaType() {
        return propertyMetadata.javaType();
    }

    public Optional<ExpressionType> expressionType() {
        return primitiveMetadata()
                .map(
                        PrimitivePropertyMetadata::expressionType
                );
    }

    public Optional<NavigationCardinality> cardinality() {
        return navigationMetadata()
                .map(
                        NavigationPropertyMetadata::cardinality
                );
    }

    public Optional<EntityMetadata<?>> targetMetadata() {
        return navigationMetadata()
                .map(
                        NavigationPropertyMetadata::targetMetadata
                );
    }

    public Optional<NavigationJoinPolicy> joinPolicy() {
        return navigationMetadata()
                .map(
                        NavigationPropertyMetadata::joinPolicy
                );
    }

    public Optional<NavigationJoinType> defaultJoinType() {
        return navigationMetadata()
                .map(
                        NavigationPropertyMetadata::defaultJoinType
                );
    }

    public Optional<PrimitivePropertyMetadata<?, ?>>
    primitiveMetadata() {
        if (propertyMetadata
                instanceof PrimitivePropertyMetadata<?, ?> primitive) {

            return Optional.of(primitive);
        }

        return Optional.empty();
    }

    public Optional<NavigationPropertyMetadata<?, ?>>
    navigationMetadata() {
        if (propertyMetadata
                instanceof NavigationPropertyMetadata<?, ?> navigation) {

            return Optional.of(navigation);
        }

        return Optional.empty();
    }

    public boolean isPrimitive() {
        return propertyMetadata.isPrimitive();
    }

    public boolean isNavigation() {
        return propertyMetadata.isNavigation();
    }

    public boolean isSingle() {
        return navigationMetadata()
                .map(
                        NavigationPropertyMetadata::isSingle
                )
                .orElse(false);
    }

    public boolean isCollection() {
        return navigationMetadata()
                .map(
                        NavigationPropertyMetadata::isCollection
                )
                .orElse(false);
    }

    public boolean isJoinTypeOverridable() {
        return navigationMetadata()
                .map(
                        NavigationPropertyMetadata::isJoinTypeOverridable
                )
                .orElse(false);
    }

    public boolean isJoinTypeFixed() {
        return navigationMetadata()
                .map(
                        NavigationPropertyMetadata::isJoinTypeFixed
                )
                .orElse(false);
    }
}