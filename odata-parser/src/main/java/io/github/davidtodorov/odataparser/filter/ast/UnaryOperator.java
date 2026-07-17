package io.github.davidtodorov.odataparser.filter.ast;

public enum UnaryOperator {
    NOT("not");

    private final String keyword;

    UnaryOperator(String keyword) {
        this.keyword = keyword;
    }

    public String keyword() {
        return keyword;
    }
}
