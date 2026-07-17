package io.github.davidtodorov.odataparser.expression.ast;

public enum BinaryOperator {
    // Logical operators
    AND("and"),
    OR("or"),

    // Comparison operators
    EQ("eq"),
    NE("ne"),
    GT("gt"),
    GE("ge"),
    LT("lt"),
    LE("le"),
    IN("in"),

    // Arithmetic operators
    ADD("add"),
    SUB("sub"),
    MUL("mul"),
    DIV("div"),
    MOD("mod");

    private final String keyword;

    BinaryOperator(String keyword) {
        this.keyword = keyword;
    }

    public String keyword() {
        return keyword;
    }
}
