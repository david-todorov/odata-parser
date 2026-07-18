package io.github.davidtodorov.odataparser.meta;

public enum NavigationCardinality {
    SINGLE,
    COLLECTION;

    public boolean isSingle() {
        return this == SINGLE;
    }

    public boolean isCollection() {
        return this == COLLECTION;
    }
}
