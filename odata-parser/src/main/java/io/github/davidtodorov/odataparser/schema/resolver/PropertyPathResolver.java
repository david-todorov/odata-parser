package io.github.davidtodorov.odataparser.schema.resolver;

import io.github.davidtodorov.odataparser.schema.EntitySchema;
import io.github.davidtodorov.odataparser.schema.PropertyDefinition;
import io.github.davidtodorov.odataparser.schema.ResolvedPathSegment;
import io.github.davidtodorov.odataparser.schema.ResolvedPropertyPath;
import io.github.davidtodorov.odataparser.schema.SchemaRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class PropertyPathResolver {

    private final EntitySchema rootSchema;
    private final SchemaRegistry schemaRegistry;

    public PropertyPathResolver(
            EntitySchema rootSchema,
            SchemaRegistry schemaRegistry
    ) {
        this.rootSchema = Objects.requireNonNull(
                rootSchema,
                "Root entity schema cannot be null"
        );

        this.schemaRegistry = Objects.requireNonNull(
                schemaRegistry,
                "Schema registry cannot be null"
        );
    }

    public ResolvedPropertyPath resolve(
            List<String> pathSegments
    ) {
        List<String> validatedSegments =
                validateAndCopy(pathSegments);

        String externalPath =
                String.join("/", validatedSegments);

        EntitySchema currentSchema =
                rootSchema;

        List<ResolvedPathSegment> resolvedSegments =
                new ArrayList<>(validatedSegments.size());

        for (int index = 0;
             index < validatedSegments.size();
             index++) {

            String segmentName =
                    validatedSegments.get(index);

            boolean isLastSegment =
                    index == validatedSegments.size() - 1;

            PropertyDefinition definition =
                    currentSchema
                            .findProperty(segmentName)
                            .orElse(null);

            if (definition == null) {
                throw new IllegalArgumentException(
                        "Unknown property '"
                                + segmentName
                                + "' in schema '"
                                + currentSchema.name()
                                + "' while resolving path '"
                                + externalPath
                                + "'"
                );
            }

            resolvedSegments.add(
                    new ResolvedPathSegment(
                            currentSchema.name(),
                            definition
                    )
            );

            if (isLastSegment) {
                validateLeafProperty(
                        definition,
                        currentSchema,
                        externalPath
                );

                continue;
            }

            currentSchema = resolveNextSchema(
                    definition,
                    currentSchema,
                    externalPath
            );
        }

        return new ResolvedPropertyPath(
                resolvedSegments
        );
    }

    private EntitySchema resolveNextSchema(
            PropertyDefinition definition,
            EntitySchema declaringSchema,
            String externalPath
    ) {
        if (!definition.isNavigation()) {
            throw new IllegalArgumentException(
                    "Property '"
                            + definition.externalName()
                            + "' in schema '"
                            + declaringSchema.name()
                            + "' is primitive, but path '"
                            + externalPath
                            + "' continues after it"
            );
        }

        if (definition.isCollection()) {
            throw new IllegalArgumentException(
                    "Collection navigation property '"
                            + definition.externalName()
                            + "' in schema '"
                            + declaringSchema.name()
                            + "' cannot be traversed as a scalar path: '"
                            + externalPath
                            + "'"
            );
        }

        String targetSchemaName =
                definition.targetSchemaName()
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Navigation property '"
                                                + definition.externalName()
                                                + "' in schema '"
                                                + declaringSchema.name()
                                                + "' has no target schema"
                                )
                        );

        EntitySchema targetSchema =
                schemaRegistry
                        .findSchema(targetSchemaName)
                        .orElse(null);

        if (targetSchema == null) {
            throw new IllegalArgumentException(
                    "Navigation property '"
                            + definition.externalName()
                            + "' in schema '"
                            + declaringSchema.name()
                            + "' targets unregistered schema '"
                            + targetSchemaName
                            + "' while resolving path '"
                            + externalPath
                            + "'"
            );
        }

        return targetSchema;
    }

    private void validateLeafProperty(
            PropertyDefinition definition,
            EntitySchema declaringSchema,
            String externalPath
    ) {
        if (!definition.isPrimitive()) {
            throw new IllegalArgumentException(
                    "The final segment '"
                            + definition.externalName()
                            + "' in path '"
                            + externalPath
                            + "' must be primitive, but it is navigation "
                            + "declared by schema '"
                            + declaringSchema.name()
                            + "'"
            );
        }

        if (definition.isCollection()) {
            throw new IllegalArgumentException(
                    "The final property '"
                            + definition.externalName()
                            + "' in path '"
                            + externalPath
                            + "' cannot be a collection"
            );
        }

        if (definition.expressionType().isEmpty()) {
            throw new IllegalArgumentException(
                    "Primitive property '"
                            + definition.externalName()
                            + "' in schema '"
                            + declaringSchema.name()
                            + "' has no expression type"
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
        }

        return List.copyOf(pathSegments);
    }
}