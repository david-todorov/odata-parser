package io.github.davidtodorov.odataparser.schema;

import io.github.davidtodorov.odataparser.common.type.ExpressionType;

import java.util.Objects;
import java.util.Optional;

public final class PropertyDefinition {

    private final String externalName;
    private final String mappedName;
    private final PropertyKind kind;
    private final Cardinality cardinality;
    private final ExpressionType expressionType;
    private final Class<?> javaType;
    private final String targetSchemaName;

    private PropertyDefinition(
            String externalName,
            String mappedName,
            PropertyKind kind,
            Cardinality cardinality,
            ExpressionType expressionType,
            Class<?> javaType,
            String targetSchemaName
    ) {
        this.externalName = requireText(
                externalName,
                "External property name"
        );

        this.mappedName = requireText(
                mappedName,
                "Mapped property name"
        );

        this.kind = Objects.requireNonNull(
                kind,
                "Property kind cannot be null"
        );

        this.cardinality = Objects.requireNonNull(
                cardinality,
                "Property cardinality cannot be null"
        );

        this.javaType = Objects.requireNonNull(
                javaType,
                "Java type cannot be null"
        );

        this.expressionType = expressionType;
        this.targetSchemaName = targetSchemaName;

        validate();
    }

    public static PropertyDefinition primitive(
            String externalName,
            String mappedName,
            ExpressionType expressionType,
            Class<?> javaType
    ) {
        Objects.requireNonNull(
                expressionType,
                "Primitive expression type cannot be null"
        );

        return new PropertyDefinition(
                externalName,
                mappedName,
                PropertyKind.PRIMITIVE,
                Cardinality.SINGLE,
                expressionType,
                javaType,
                null
        );
    }

    public static PropertyDefinition navigation(
            String externalName,
            String mappedName,
            String targetSchemaName,
            Class<?> targetJavaType
    ) {
        return new PropertyDefinition(
                externalName,
                mappedName,
                PropertyKind.NAVIGATION,
                Cardinality.SINGLE,
                null,
                targetJavaType,
                requireText(
                        targetSchemaName,
                        "Target schema name"
                )
        );
    }

    public static PropertyDefinition navigationCollection(
            String externalName,
            String mappedName,
            String targetSchemaName,
            Class<?> targetJavaType
    ) {
        return new PropertyDefinition(
                externalName,
                mappedName,
                PropertyKind.NAVIGATION,
                Cardinality.COLLECTION,
                null,
                targetJavaType,
                requireText(
                        targetSchemaName,
                        "Target schema name"
                )
        );
    }

    public String externalName() {
        return externalName;
    }

    public String mappedName() {
        return mappedName;
    }

    public PropertyKind kind() {
        return kind;
    }

    public Cardinality cardinality() {
        return cardinality;
    }

    public Optional<ExpressionType> expressionType() {
        return Optional.ofNullable(expressionType);
    }

    public Class<?> javaType() {
        return javaType;
    }

    public Optional<String> targetSchemaName() {
        return Optional.ofNullable(targetSchemaName);
    }

    public boolean isPrimitive() {
        return kind == PropertyKind.PRIMITIVE;
    }

    public boolean isNavigation() {
        return kind == PropertyKind.NAVIGATION;
    }

    public boolean isCollection() {
        return cardinality == Cardinality.COLLECTION;
    }

    private void validate() {
        if (isPrimitive()) {
            validatePrimitiveProperty();
        } else {
            validateNavigationProperty();
        }
    }

    private void validatePrimitiveProperty() {
        if (cardinality != Cardinality.SINGLE) {
            throw new IllegalArgumentException(
                    "Primitive collection properties are not supported"
            );
        }

        if (expressionType == null) {
            throw new IllegalArgumentException(
                    "Primitive properties require an expression type"
            );
        }

        if (expressionType == ExpressionType.UNKNOWN
                || expressionType == ExpressionType.COLLECTION
                || expressionType == ExpressionType.NULL) {
            throw new IllegalArgumentException(
                    "Invalid primitive expression type: "
                            + expressionType
            );
        }

        if (targetSchemaName != null) {
            throw new IllegalArgumentException(
                    "Primitive properties cannot have a target schema"
            );
        }
    }

    private void validateNavigationProperty() {
        if (expressionType != null) {
            throw new IllegalArgumentException(
                    "Navigation properties cannot have a scalar expression type"
            );
        }

        if (targetSchemaName == null || targetSchemaName.isBlank()) {
            throw new IllegalArgumentException(
                    "Navigation properties require a target schema name"
            );
        }
    }

    private static String requireText(
            String value,
            String fieldName
    ) {
        Objects.requireNonNull(
                value,
                fieldName + " cannot be null"
        );

        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " cannot be blank"
            );
        }

        return value;
    }

    @Override
    public String toString() {
        return "PropertyDefinition[" +
                "externalName='" + externalName + '\'' +
                ", mappedName='" + mappedName + '\'' +
                ", kind=" + kind +
                ", cardinality=" + cardinality +
                ", expressionType=" + expressionType +
                ", javaType=" + javaType.getSimpleName() +
                ", targetSchemaName='" + targetSchemaName + '\'' +
                ']';
    }
}