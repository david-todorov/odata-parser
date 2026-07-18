package io.github.davidtodorov.odataparser.meta;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public interface EntityMetadata<E> {

    String name();

    Class<E> entityType();

    Map<String, PropertyMetadata<E, ?>> properties();

    default Optional<PropertyMetadata<E, ?>> findProperty(
            String externalName
    ) {
        Objects.requireNonNull(
                externalName,
                "Property external name cannot be null"
        );

        if (externalName.isBlank()) {
            throw new IllegalArgumentException(
                    "Property external name cannot be blank"
            );
        }

        return Optional.ofNullable(
                properties().get(externalName)
        );
    }

    default PropertyMetadata<E, ?> requireProperty(
            String externalName
    ) {
        return findProperty(externalName)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Unknown property '"
                                        + externalName
                                        + "' in entity metadata '"
                                        + name()
                                        + "'"
                        )
                );
    }

    default boolean hasProperty(
            String externalName
    ) {
        return findProperty(externalName).isPresent();
    }

    default int propertyCount() {
        return properties().size();
    }
}
