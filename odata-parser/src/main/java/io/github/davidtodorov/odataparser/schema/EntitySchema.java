package io.github.davidtodorov.odataparser.schema;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class EntitySchema {

    private final String name;
    private final Class<?> javaType;
    private final Map<String, PropertyDefinition> properties;

    public EntitySchema(
            String name,
            Class<?> javaType,
            Map<String, PropertyDefinition> properties
    ) {
        this.name = requireText(
                name,
                "Entity schema name"
        );

        this.javaType = Objects.requireNonNull(
                javaType,
                "Entity Java type cannot be null"
        );

        Objects.requireNonNull(
                properties,
                "Entity properties cannot be null"
        );

        this.properties = validateAndCopyProperties(properties);
    }
    public String name() {
        return name;
    }

    public Class<?> javaType() {
        return javaType;
    }

    public Map<String, PropertyDefinition> properties() {
        return properties;
    }

    public Optional<PropertyDefinition> findProperty(
            String externalName
    ) {
        Objects.requireNonNull(
                externalName,
                "External property name cannot be null"
        );

        return Optional.ofNullable(
                properties.get(externalName)
        );
    }

    public PropertyDefinition requireProperty(
            String externalName
    ) {
        return findProperty(externalName)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown property '"
                                + externalName
                                + "' in entity schema '"
                                + name
                                + "'"
                ));
    }

    public boolean hasProperty(String externalName) {
        Objects.requireNonNull(
                externalName,
                "External property name cannot be null"
        );

        return properties.containsKey(externalName);
    }

    private Map<String, PropertyDefinition> validateAndCopyProperties(
            Map<String, PropertyDefinition> source
    ) {
        Map<String, PropertyDefinition> validated =
                new LinkedHashMap<>();

        for (Map.Entry<String, PropertyDefinition> entry
                : source.entrySet()) {

            String key = requireText(
                    entry.getKey(),
                    "Property map key"
            );

            PropertyDefinition property =
                    Objects.requireNonNull(
                            entry.getValue(),
                            "Property definition cannot be null"
                    );

            if (!key.equals(property.externalName())) {
                throw new IllegalArgumentException(
                        "Property map key '"
                                + key
                                + "' does not match property external name '"
                                + property.externalName()
                                + "'"
                );
            }

            if (validated.put(key, property) != null) {
                throw new IllegalArgumentException(
                        "Duplicate property definition: " + key
                );
            }
        }

        return Map.copyOf(validated);
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
        return "EntitySchema[" +
                "name='" + name + '\'' +
                ", javaType=" + javaType.getSimpleName() +
                ", properties=" + properties.keySet() +
                ']';
    }
}