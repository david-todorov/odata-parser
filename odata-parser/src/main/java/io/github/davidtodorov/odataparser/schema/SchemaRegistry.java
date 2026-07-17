package io.github.davidtodorov.odataparser.schema;

import java.util.*;

public final class SchemaRegistry {

    private final Map<String, EntitySchema> schemas;

    public SchemaRegistry(Collection<EntitySchema> schemas) {
        Objects.requireNonNull(
                schemas,
                "Schema collection cannot be null"
        );

        Map<String, EntitySchema> registeredSchemas =
                new LinkedHashMap<>();

        for (EntitySchema schema : schemas) {
            Objects.requireNonNull(
                    schema,
                    "Schema collection cannot contain null elements"
            );

            EntitySchema previous = registeredSchemas.put(
                    schema.name(),
                    schema
            );

            if (previous != null) {
                throw new IllegalArgumentException(
                        "Duplicate entity schema name: "
                                + schema.name()
                );
            }
        }

        this.schemas = Map.copyOf(registeredSchemas);
    }

    public static SchemaRegistry of(EntitySchema... schemas) {
        Objects.requireNonNull(
                schemas,
                "Schemas cannot be null"
        );

        return new SchemaRegistry(
                java.util.List.of(schemas)
        );
    }

    public Map<String, EntitySchema> schemas() {
        return schemas;
    }

    public Optional<EntitySchema> findSchema(String schemaName) {
        requireSchemaName(schemaName);

        return Optional.ofNullable(
                schemas.get(schemaName)
        );
    }

    public EntitySchema requireSchema(String schemaName) {
        return findSchema(schemaName)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown entity schema: " + schemaName
                ));
    }

    public boolean containsSchema(String schemaName) {
        requireSchemaName(schemaName);

        return schemas.containsKey(schemaName);
    }

    public int size() {
        return schemas.size();
    }

    private static void requireSchemaName(String schemaName) {
        Objects.requireNonNull(
                schemaName,
                "Schema name cannot be null"
        );

        if (schemaName.isBlank()) {
            throw new IllegalArgumentException(
                    "Schema name cannot be blank"
            );
        }
    }

    @Override
    public String toString() {
        return "SchemaRegistry[schemas="
                + schemas.keySet()
                + "]";
    }
}