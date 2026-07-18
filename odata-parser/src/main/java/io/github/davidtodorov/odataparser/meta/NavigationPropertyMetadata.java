package io.github.davidtodorov.odataparser.meta;

import java.util.Objects;

public record NavigationPropertyMetadata<O, T>(
        String externalName,
        String mappedName,
        Class<O> ownerType,
        EntityMetadata<T> targetMetadata,
        NavigationCardinality cardinality,
        NavigationJoinPolicy joinPolicy
) implements PropertyMetadata<O, T> {

    public NavigationPropertyMetadata {
        externalName = validateName(
                externalName,
                "Navigation property external name"
        );

        mappedName = validateName(
                mappedName,
                "Navigation property mapped name"
        );

        Objects.requireNonNull(
                ownerType,
                "Navigation property owner type cannot be null"
        );

        Objects.requireNonNull(
                targetMetadata,
                "Navigation property target metadata cannot be null"
        );

        Objects.requireNonNull(
                cardinality,
                "Navigation property cardinality cannot be null"
        );

        Objects.requireNonNull(
                joinPolicy,
                "Navigation property join policy cannot be null"
        );

        if (ownerType == Void.class
                || ownerType == Void.TYPE) {

            throw new IllegalArgumentException(
                    "Navigation property owner type cannot be void"
            );
        }

        Class<T> targetType =
                Objects.requireNonNull(
                        targetMetadata.entityType(),
                        "Navigation target metadata entity type cannot be null"
                );

        if (targetType == Void.class
                || targetType == Void.TYPE) {

            throw new IllegalArgumentException(
                    "Navigation target entity type cannot be void"
            );
        }

        String targetName =
                Objects.requireNonNull(
                        targetMetadata.name(),
                        "Navigation target metadata name cannot be null"
                );

        if (targetName.isBlank()) {
            throw new IllegalArgumentException(
                    "Navigation target metadata name cannot be blank"
            );
        }
    }

    @Override
    public Class<T> javaType() {
        return targetMetadata.entityType();
    }

    @Override
    public PropertyMetadataKind kind() {
        return PropertyMetadataKind.NAVIGATION;
    }

    public String targetMetadataName() {
        return targetMetadata.name();
    }

    public NavigationJoinType defaultJoinType() {
        return joinPolicy.defaultJoinType();
    }

    public boolean isJoinTypeOverridable() {
        return joinPolicy.overridable();
    }

    public boolean isJoinTypeFixed() {
        return !joinPolicy.overridable();
    }

    public boolean isSingle() {
        return cardinality.isSingle();
    }

    public boolean isCollection() {
        return cardinality.isCollection();
    }

    private static String validateName(
            String value,
            String description
    ) {
        Objects.requireNonNull(
                value,
                description + " cannot be null"
        );

        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    description + " cannot be blank"
            );
        }

        if (!value.equals(value.trim())) {
            throw new IllegalArgumentException(
                    description
                            + " cannot contain leading or trailing whitespace: '"
                            + value
                            + "'"
            );
        }

        if (value.chars()
                .anyMatch(Character::isWhitespace)) {

            throw new IllegalArgumentException(
                    description
                            + " cannot contain whitespace: '"
                            + value
                            + "'"
            );
        }

        if (value.indexOf('/') >= 0) {
            throw new IllegalArgumentException(
                    description
                            + " must describe one property segment "
                            + "and cannot contain '/': '"
                            + value
                            + "'"
            );
        }

        return value;
    }
}