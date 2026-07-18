package io.github.davidtodorov.odataparser.meta;

public enum PropertyMetadataKind {

    PRIMITIVE,

    NAVIGATION;

    public boolean isPrimitive() {
        return this == PRIMITIVE;
    }

    public boolean isNavigation() {
        return this == NAVIGATION;
    }
}
