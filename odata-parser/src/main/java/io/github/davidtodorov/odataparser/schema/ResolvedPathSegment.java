package io.github.davidtodorov.odataparser.schema;

import io.github.davidtodorov.odataparser.common.type.ExpressionType;

import java.util.Objects;
import java.util.Optional;

public record ResolvedPathSegment(
        String declaringSchemaName,
        PropertyDefinition definition
) {

    public ResolvedPathSegment {
        Objects.requireNonNull(
                declaringSchemaName,
                "Declaring schema name cannot be null"
        );

        Objects.requireNonNull(
                definition,
                "Property definition cannot be null"
        );

        if (declaringSchemaName.isBlank()) {
            throw new IllegalArgumentException(
                    "Declaring schema name cannot be blank"
            );
        }
    }

    public String externalName() {
        return definition.externalName();
    }

    public String mappedName() {
        return definition.mappedName();
    }

    public PropertyKind kind() {
        return definition.kind();
    }

    public Cardinality cardinality() {
        return definition.cardinality();
    }

    public Optional<ExpressionType> expressionType() {
        return definition.expressionType();
    }

    public Class<?> javaType() {
        return definition.javaType();
    }

    public Optional<String> targetSchemaName() {
        return definition.targetSchemaName();
    }

    public boolean isPrimitive() {
        return definition.isPrimitive();
    }

    public boolean isNavigation() {
        return definition.isNavigation();
    }

    public boolean isCollection() {
        return definition.isCollection();
    }
}
