package io.github.davidtodorov.odataparser.orderby.ast;

import io.github.davidtodorov.odataparser.common.metadata.SourceSpan;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public record OrderByOption(
        List<OrderByItem> items,
        SourceSpan sourceSpan
) implements Iterable<OrderByItem> {

    public OrderByOption(List<OrderByItem> items) {
        this(
                items,
                SourceSpan.unknown()
        );
    }

    public OrderByOption {
        Objects.requireNonNull(
                items,
                "Order-by items cannot be null"
        );

        Objects.requireNonNull(
                sourceSpan,
                "Order-by source span cannot be null"
        );

        if (items.isEmpty()) {
            throw new IllegalArgumentException(
                    "An order-by option must contain at least one item"
            );
        }

        if (items.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "Order-by items cannot contain null elements"
            );
        }

        items = List.copyOf(items);
    }

    public int size() {
        return items.size();
    }

    public OrderByItem get(int index) {
        return items.get(index);
    }

    public boolean isResolved() {
        return items.stream()
                .allMatch(OrderByItem::isResolved);
    }

    public OrderByOption withResolvedItems(
            List<OrderByItem> resolvedItems
    ) {
        Objects.requireNonNull(
                resolvedItems,
                "Resolved order-by items cannot be null"
        );

        if (resolvedItems.size() != items.size()) {
            throw new IllegalArgumentException(
                    "Resolved order-by item count does not match "
                            + "the original item count"
            );
        }

        for (int index = 0;
             index < items.size();
             index++) {

            OrderByItem original =
                    items.get(index);

            OrderByItem resolved =
                    Objects.requireNonNull(
                            resolvedItems.get(index),
                            "Resolved order-by item at index "
                                    + index
                                    + " cannot be null"
                    );

            if (!original.pathSegments()
                    .equals(resolved.pathSegments())) {

                throw new IllegalArgumentException(
                        "Resolved item at index "
                                + index
                                + " changed the property path from '"
                                + original.externalPath()
                                + "' to '"
                                + resolved.externalPath()
                                + "'"
                );
            }

            if (original.direction()
                    != resolved.direction()) {

                throw new IllegalArgumentException(
                        "Resolved item at index "
                                + index
                                + " changed the direction of property '"
                                + original.externalPath()
                                + "'"
                );
            }

            if (!resolved.isResolved()) {
                throw new IllegalArgumentException(
                        "Order-by item at index "
                                + index
                                + " is still unresolved"
                );
            }
        }

        return new OrderByOption(
                resolvedItems,
                sourceSpan
        );
    }

    @Override
    public Iterator<OrderByItem> iterator() {
        return items.iterator();
    }
}