package io.github.davidtodorov.odataparser.meta;

import io.github.davidtodorov.odataparser.common.type.ExpressionType;

import java.util.Objects;

public record PrimitivePropertyMetadata<O, V>(
        String externalName,
        String mappedName,
        Class<O> ownerType,
        Class<V> javaType,
        ExpressionType expressionType
) implements PropertyMetadata<O, V> {

    public PrimitivePropertyMetadata {
        externalName = validateName(
                externalName,
                "Primitive property external name"
        );

        mappedName = validateName(
                mappedName,
                "Primitive property mapped name"
        );

        Objects.requireNonNull(
                ownerType,
                "Primitive property owner type cannot be null"
        );

        Objects.requireNonNull(
                javaType,
                "Primitive property Java type cannot be null"
        );

        Objects.requireNonNull(
                expressionType,
                "Primitive property expression type cannot be null"
        );

        if (expressionType == ExpressionType.UNKNOWN) {
            throw new IllegalArgumentException(
                    "Primitive property '"
                            + externalName
                            + "' cannot use the UNKNOWN expression type"
            );
        }

        if (ownerType == Void.class || ownerType == Void.TYPE) {
            throw new IllegalArgumentException(
                    "Primitive property owner type cannot be void"
            );
        }

        if (javaType == Void.class || javaType == Void.TYPE) {
            throw new IllegalArgumentException(
                    "Primitive property Java type cannot be void"
            );
        }
    }

    @Override
    public PropertyMetadataKind kind() {
        return PropertyMetadataKind.PRIMITIVE;
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

        if (value.chars().anyMatch(Character::isWhitespace)) {
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
                            + " must describe one property segment and cannot contain '/': '"
                            + value
                            + "'"
            );
        }

        return value;
    }
}
