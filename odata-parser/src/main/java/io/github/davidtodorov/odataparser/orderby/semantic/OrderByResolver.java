package io.github.davidtodorov.odataparser.orderby.semantic;

import io.github.davidtodorov.odataparser.orderby.ast.OrderByItem;
import io.github.davidtodorov.odataparser.orderby.ast.OrderByOption;
import io.github.davidtodorov.odataparser.schema.EntitySchema;
import io.github.davidtodorov.odataparser.schema.ResolvedPropertyPath;
import io.github.davidtodorov.odataparser.schema.SchemaRegistry;
import io.github.davidtodorov.odataparser.schema.resolver.PropertyPathResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class OrderByResolver {

    private final PropertyPathResolver propertyPathResolver;

    public OrderByResolver(EntitySchema rootSchema) {
        this(
                rootSchema,
                SchemaRegistry.of(rootSchema)
        );
    }

    public OrderByResolver(
            EntitySchema rootSchema,
            SchemaRegistry schemaRegistry
    ) {
        this.propertyPathResolver =
                new PropertyPathResolver(
                        Objects.requireNonNull(
                                rootSchema,
                                "Root entity schema cannot be null"
                        ),
                        Objects.requireNonNull(
                                schemaRegistry,
                                "Schema registry cannot be null"
                        )
                );
    }

    public OrderByOption resolve(OrderByOption option) {
        Objects.requireNonNull(
                option,
                "Order-by option cannot be null"
        );

        List<OrderByItem> resolvedItems =
                new ArrayList<>(option.size());

        for (OrderByItem item : option) {
            resolvedItems.add(
                    resolveItem(item)
            );
        }

        return option.withResolvedItems(
                resolvedItems
        );
    }

    private OrderByItem resolveItem(
            OrderByItem item
    ) {
        Objects.requireNonNull(
                item,
                "Order-by item cannot be null"
        );

        try {
            ResolvedPropertyPath resolvedPath =
                    propertyPathResolver.resolve(
                            item.pathSegments()
                    );

            return item.withResolvedPath(
                    resolvedPath
            );

        } catch (IllegalArgumentException exception) {
            throw new OrderBySemanticException(
                    exception.getMessage(),
                    item,
                    exception
            );
        }
    }
}