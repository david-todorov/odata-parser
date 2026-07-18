package io.github.davidtodorov.odataparser.meta;

import java.util.Objects;
import java.util.Optional;

public interface MetadataContext {

    <E> Optional<EntityMetadata<E>> find(
            Class<E> entityType
    );

    default <E> EntityMetadata<E> require(
            Class<E> entityType
    ) {
        Objects.requireNonNull(
                entityType,
                "Entity type cannot be null"
        );

        return find(entityType)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "No entity metadata is registered for Java type '"
                                        + entityType.getName()
                                        + "'"
                        )
                );
    }

    Optional<EntityMetadata<?>> find(
            String metadataName
    );

    default EntityMetadata<?> require(
            String metadataName
    ) {
        Objects.requireNonNull(
                metadataName,
                "Entity metadata name cannot be null"
        );

        if (metadataName.isBlank()) {
            throw new IllegalArgumentException(
                    "Entity metadata name cannot be blank"
            );
        }

        return find(metadataName)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "No entity metadata is registered with name '"
                                        + metadataName
                                        + "'"
                        )
                );
    }
}