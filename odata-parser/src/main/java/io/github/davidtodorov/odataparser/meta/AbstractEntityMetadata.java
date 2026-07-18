package io.github.davidtodorov.odataparser.meta;

import io.github.davidtodorov.odataparser.common.type.ExpressionType;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public abstract class AbstractEntityMetadata<E>
        implements EntityMetadata<E> {

    private enum LifecycleState {
        NEW,
        CONFIGURING,
        INITIALIZED,
        FAILED
    }

    private final String name;
    private final Class<E> entityType;

    private final Map<String, PropertyMetadata<E, ?>>
            propertiesBeingConfigured;

    private Map<String, PropertyMetadata<E, ?>> properties;

    private LifecycleState lifecycleState;

    protected AbstractEntityMetadata(
            String name,
            Class<E> entityType
    ) {
        this.name = validateName(
                name,
                "Entity metadata name"
        );

        this.entityType = Objects.requireNonNull(
                entityType,
                "Entity metadata Java type cannot be null"
        );

        if (entityType.isPrimitive()
                || entityType == Void.class) {

            throw new IllegalArgumentException(
                    "Entity metadata Java type must be a non-primitive class: "
                            + entityType.getTypeName()
            );
        }

        this.propertiesBeingConfigured =
                new LinkedHashMap<>();

        this.properties = Map.of();
        this.lifecycleState = LifecycleState.NEW;
    }

    protected abstract void configure(
            MetadataContext context
    );

    @Override
    public final String name() {
        return name;
    }

    @Override
    public final Class<E> entityType() {
        return entityType;
    }

    @Override
    public final Map<String, PropertyMetadata<E, ?>> properties() {
        ensureInitialized();
        return properties;
    }

    protected final <V> PrimitivePropertyMetadata<E, V> primitive(
            String externalName,
            String mappedName,
            Class<V> javaType,
            ExpressionType expressionType
    ) {
        PrimitivePropertyMetadata<E, V> property =
                new PrimitivePropertyMetadata<>(
                        externalName,
                        mappedName,
                        entityType,
                        javaType,
                        expressionType
                );

        registerProperty(property);

        return property;
    }

    protected final <T> NavigationPropertyMetadata<E, T> navigation(
            String externalName,
            String mappedName,
            EntityMetadata<T> targetMetadata,
            NavigationCardinality cardinality,
            NavigationJoinPolicy joinPolicy
    ) {
        NavigationPropertyMetadata<E, T> property =
                new NavigationPropertyMetadata<>(
                        externalName,
                        mappedName,
                        entityType,
                        targetMetadata,
                        cardinality,
                        joinPolicy
                );

        registerProperty(property);

        return property;
    }

    protected final <T> NavigationPropertyMetadata<E, T> singleNavigation(
            String externalName,
            String mappedName,
            EntityMetadata<T> targetMetadata,
            NavigationJoinPolicy joinPolicy
    ) {
        return navigation(
                externalName,
                mappedName,
                targetMetadata,
                NavigationCardinality.SINGLE,
                joinPolicy
        );
    }

    protected final <T> NavigationPropertyMetadata<E, T> collectionNavigation(
            String externalName,
            String mappedName,
            EntityMetadata<T> targetMetadata,
            NavigationJoinPolicy joinPolicy
    ) {
        return navigation(
                externalName,
                mappedName,
                targetMetadata,
                NavigationCardinality.COLLECTION,
                joinPolicy
        );
    }

    final void initialize(
            MetadataContext context
    ) {
        Objects.requireNonNull(
                context,
                "Metadata context cannot be null"
        );

        if (lifecycleState != LifecycleState.NEW) {
            throw new IllegalStateException(
                    "Entity metadata '"
                            + name
                            + "' cannot be initialized from state "
                            + lifecycleState
            );
        }

        lifecycleState =
                LifecycleState.CONFIGURING;

        try {
            configure(context);

            properties =
                    Collections.unmodifiableMap(
                            new LinkedHashMap<>(
                                    propertiesBeingConfigured
                            )
                    );

            propertiesBeingConfigured.clear();

            lifecycleState =
                    LifecycleState.INITIALIZED;
        } catch (RuntimeException | Error exception) {
            propertiesBeingConfigured.clear();
            properties = Map.of();

            lifecycleState =
                    LifecycleState.FAILED;

            throw exception;
        }
    }

    final boolean isInitialized() {
        return lifecycleState
                == LifecycleState.INITIALIZED;
    }

    private void registerProperty(
            PropertyMetadata<E, ?> property
    ) {
        Objects.requireNonNull(
                property,
                "Property metadata cannot be null"
        );

        ensureConfiguring();

        if (!entityType.equals(
                property.ownerType()
        )) {
            throw new IllegalArgumentException(
                    "Property '"
                            + property.externalName()
                            + "' declares owner type '"
                            + property.ownerType().getName()
                            + "', but entity metadata '"
                            + name
                            + "' describes '"
                            + entityType.getName()
                            + "'"
            );
        }

        PropertyMetadata<E, ?> existing =
                propertiesBeingConfigured.putIfAbsent(
                        property.externalName(),
                        property
                );

        if (existing != null) {
            throw new IllegalArgumentException(
                    "Duplicate property external name '"
                            + property.externalName()
                            + "' in entity metadata '"
                            + name
                            + "'"
            );
        }
    }

    private void ensureConfiguring() {
        if (lifecycleState
                != LifecycleState.CONFIGURING) {

            throw new IllegalStateException(
                    "Properties can only be registered while entity metadata '"
                            + name
                            + "' is being configured"
            );
        }
    }

    private void ensureInitialized() {
        if (lifecycleState
                != LifecycleState.INITIALIZED) {

            throw new IllegalStateException(
                    "Entity metadata '"
                            + name
                            + "' is not initialized. Current state: "
                            + lifecycleState
            );
        }
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

        if (value.chars()
                .anyMatch(Character::isWhitespace)) {

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
                            + " cannot contain '/': '"
                            + value
                            + "'"
            );
        }

        return value;
    }

    @Override
    public final String toString() {
        return "EntityMetadata("
                + "name="
                + name
                + ", entityType="
                + entityType.getName()
                + ", state="
                + lifecycleState
                + ")";
    }
}