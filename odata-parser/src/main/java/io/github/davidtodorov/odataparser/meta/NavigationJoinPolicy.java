package io.github.davidtodorov.odataparser.meta;

import java.util.Objects;

public record NavigationJoinPolicy(
        NavigationJoinType defaultJoinType,
        boolean overridable
) {

    public NavigationJoinPolicy {
        Objects.requireNonNull(
                defaultJoinType,
                "Default navigation join type cannot be null"
        );
    }

    public static NavigationJoinPolicy inner() {
        return new NavigationJoinPolicy(
                NavigationJoinType.INNER,
                true
        );
    }

    public static NavigationJoinPolicy left() {
        return new NavigationJoinPolicy(
                NavigationJoinType.LEFT,
                true
        );
    }

    public static NavigationJoinPolicy fixedInner() {
        return new NavigationJoinPolicy(
                NavigationJoinType.INNER,
                false
        );
    }

    public static NavigationJoinPolicy fixedLeft() {
        return new NavigationJoinPolicy(
                NavigationJoinType.LEFT,
                false
        );
    }

    public static NavigationJoinPolicy fixed(
            NavigationJoinType joinType
    ) {
        return new NavigationJoinPolicy(
                joinType,
                false
        );
    }
}