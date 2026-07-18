package io.github.davidtodorov.odataparser.meta;

import java.util.*;

public final class MetadataRegistry
        implements MetadataContext {

    private final List<AbstractEntityMetadata<?>> metadataDescriptors;

    private final Map<String, EntityMetadata<?>> metadataByName;
    private final Map<Class<?>, EntityMetadata<?>> metadataByType;

    private MetadataRegistry(
            Collection<? extends AbstractEntityMetadata<?>> descriptors
    ) {
        Objects.requireNonNull(
                descriptors,
                "Entity metadata collection cannot be null"
        );

        if (descriptors.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one entity metadata descriptor is required"
            );
        }

        List<AbstractEntityMetadata<?>> orderedDescriptors =
                new ArrayList<>();

        Map<String, EntityMetadata<?>> byName =
                new LinkedHashMap<>();

        Map<Class<?>, EntityMetadata<?>> byType =
                new LinkedHashMap<>();

        for (AbstractEntityMetadata<?> descriptor : descriptors) {
            registerDescriptor(
                    descriptor,
                    orderedDescriptors,
                    byName,
                    byType
            );
        }

        this.metadataDescriptors =
                List.copyOf(orderedDescriptors);

        this.metadataByName =
                Collections.unmodifiableMap(
                        new LinkedHashMap<>(byName)
                );

        this.metadataByType =
                Collections.unmodifiableMap(
                        new LinkedHashMap<>(byType)
                );

        initializeDescriptors();
        validateConnectedGraph();
    }

    public static MetadataRegistry of(
            AbstractEntityMetadata<?>... descriptors
    ) {
        Objects.requireNonNull(
                descriptors,
                "Entity metadata descriptor array cannot be null"
        );

        return new MetadataRegistry(
                List.of(descriptors)
        );
    }

    public static MetadataRegistry of(
            Collection<? extends AbstractEntityMetadata<?>> descriptors
    ) {
        return new MetadataRegistry(descriptors);
    }

    @Override
    public <E> Optional<EntityMetadata<E>> find(
            Class<E> entityType
    ) {
        Objects.requireNonNull(
                entityType,
                "Entity type cannot be null"
        );

        EntityMetadata<?> metadata =
                metadataByType.get(entityType);

        if (metadata == null) {
            return Optional.empty();
        }

        @SuppressWarnings("unchecked")
        EntityMetadata<E> typedMetadata =
                (EntityMetadata<E>) metadata;

        return Optional.of(typedMetadata);
    }

    @Override
    public Optional<EntityMetadata<?>> find(
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

        return Optional.ofNullable(
                metadataByName.get(metadataName)
        );
    }

    public List<EntityMetadata<?>> all() {
        return List.copyOf(metadataDescriptors);
    }

    public Map<String, EntityMetadata<?>> byName() {
        return metadataByName;
    }

    public Map<Class<?>, EntityMetadata<?>> byType() {
        return metadataByType;
    }

    public boolean contains(
            Class<?> entityType
    ) {
        Objects.requireNonNull(
                entityType,
                "Entity type cannot be null"
        );

        return metadataByType.containsKey(entityType);
    }

    public boolean contains(
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

        return metadataByName.containsKey(metadataName);
    }

    public int size() {
        return metadataDescriptors.size();
    }

    private static void registerDescriptor(
            AbstractEntityMetadata<?> descriptor,
            List<AbstractEntityMetadata<?>> orderedDescriptors,
            Map<String, EntityMetadata<?>> byName,
            Map<Class<?>, EntityMetadata<?>> byType
    ) {
        Objects.requireNonNull(
                descriptor,
                "Entity metadata descriptor cannot be null"
        );

        EntityMetadata<?> existingByName =
                byName.putIfAbsent(
                        descriptor.name(),
                        descriptor
                );

        if (existingByName != null) {
            throw new IllegalArgumentException(
                    "Duplicate entity metadata name '"
                            + descriptor.name()
                            + "' used by Java types '"
                            + existingByName.entityType().getName()
                            + "' and '"
                            + descriptor.entityType().getName()
                            + "'"
            );
        }

        EntityMetadata<?> existingByType =
                byType.putIfAbsent(
                        descriptor.entityType(),
                        descriptor
                );

        if (existingByType != null) {
            /*
             * Remove the name inserted above so the temporary maps remain
             * internally consistent before the constructor fails.
             */
            byName.remove(
                    descriptor.name(),
                    descriptor
            );

            throw new IllegalArgumentException(
                    "Duplicate entity metadata for Java type '"
                            + descriptor.entityType().getName()
                            + "'. Existing metadata name is '"
                            + existingByType.name()
                            + "' and duplicate metadata name is '"
                            + descriptor.name()
                            + "'"
            );
        }

        orderedDescriptors.add(descriptor);
    }

    private void initializeDescriptors() {
        for (AbstractEntityMetadata<?> descriptor
                : metadataDescriptors) {

            descriptor.initialize(this);
        }
    }

    private void validateConnectedGraph() {
        for (AbstractEntityMetadata<?> descriptor
                : metadataDescriptors) {

            validateDescriptorProperties(descriptor);
        }
    }

    private void validateDescriptorProperties(
            EntityMetadata<?> descriptor
    ) {
        for (PropertyMetadata<?, ?> property
                : descriptor.properties().values()) {

            validatePropertyOwner(
                    descriptor,
                    property
            );

            if (property
                    instanceof NavigationPropertyMetadata<?, ?>
                    navigation) {

                validateNavigationTarget(
                        descriptor,
                        navigation
                );
            }
        }
    }

    private void validatePropertyOwner(
            EntityMetadata<?> descriptor,
            PropertyMetadata<?, ?> property
    ) {
        if (!descriptor.entityType()
                .equals(property.ownerType())) {

            throw new IllegalStateException(
                    "Property '"
                            + property.externalName()
                            + "' in metadata '"
                            + descriptor.name()
                            + "' declares owner type '"
                            + property.ownerType().getName()
                            + "' instead of '"
                            + descriptor.entityType().getName()
                            + "'"
            );
        }
    }

    private void validateNavigationTarget(
            EntityMetadata<?> sourceMetadata,
            NavigationPropertyMetadata<?, ?> navigation
    ) {
        EntityMetadata<?> target =
                navigation.targetMetadata();

        EntityMetadata<?> registeredByName =
                metadataByName.get(target.name());

        EntityMetadata<?> registeredByType =
                metadataByType.get(target.entityType());

        if (registeredByName == null
                || registeredByType == null) {

            throw new IllegalStateException(
                    "Navigation property '"
                            + sourceMetadata.name()
                            + "."
                            + navigation.externalName()
                            + "' targets unregistered metadata '"
                            + target.name()
                            + "' for Java type '"
                            + target.entityType().getName()
                            + "'"
            );
        }

        if (registeredByName != target
                || registeredByType != target) {

            throw new IllegalStateException(
                    "Navigation property '"
                            + sourceMetadata.name()
                            + "."
                            + navigation.externalName()
                            + "' does not reference the registered metadata "
                            + "instance for target '"
                            + target.name()
                            + "'"
            );
        }

        if (!navigation.javaType()
                .equals(target.entityType())) {

            throw new IllegalStateException(
                    "Navigation property '"
                            + sourceMetadata.name()
                            + "."
                            + navigation.externalName()
                            + "' exposes Java type '"
                            + navigation.javaType().getName()
                            + "', but its target metadata describes '"
                            + target.entityType().getName()
                            + "'"
            );
        }
    }

    @Override
    public String toString() {
        return "MetadataRegistry("
                + "size="
                + size()
                + ", names="
                + metadataByName.keySet()
                + ")";
    }
}
