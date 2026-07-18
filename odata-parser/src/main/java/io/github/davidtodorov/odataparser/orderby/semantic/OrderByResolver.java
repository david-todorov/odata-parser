package io.github.davidtodorov.odataparser.orderby.semantic;

import io.github.davidtodorov.odataparser.meta.EntityMetadata;
import io.github.davidtodorov.odataparser.meta.path.ResolvedMetadataPath;
import io.github.davidtodorov.odataparser.meta.resolver.MetadataPropertyPathResolver;
import io.github.davidtodorov.odataparser.orderby.ast.OrderByItem;
import io.github.davidtodorov.odataparser.orderby.ast.OrderByOption;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class OrderByResolver {

    private final MetadataPropertyPathResolver propertyPathResolver;

    public OrderByResolver(
            EntityMetadata<?> rootMetadata
    ) {
        this.propertyPathResolver =
                new MetadataPropertyPathResolver(
                        Objects.requireNonNull(
                                rootMetadata,
                                "Root entity metadata cannot be null"
                        )
                );
    }

    public OrderByOption resolve(
            OrderByOption option
    ) {
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
            ResolvedMetadataPath resolvedPath =
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