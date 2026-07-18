package io.github.davidtodorov.odataparser.meta;

public sealed interface PropertyMetadata<O, V>
        permits PrimitivePropertyMetadata,
        NavigationPropertyMetadata {

    String externalName();

    String mappedName();

    Class<O> ownerType();

    Class<V> javaType();

    PropertyMetadataKind kind();

    default boolean isPrimitive() {
        return kind().isPrimitive();
    }

    default boolean isNavigation() {
        return kind().isNavigation();
    }
}
