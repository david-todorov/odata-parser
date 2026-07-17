package io.github.davidtodorov.odataparser.orderby.ast;

public enum OrderByDirection {

    ASCENDING("asc"),
    DESCENDING("desc");

    private final String keyword;

    OrderByDirection(String keyword) {
        this.keyword = keyword;
    }

    public String keyword() {
        return keyword;
    }

    public static OrderByDirection fromKeyword(String keyword) {
        if (keyword == null) {
            throw new IllegalArgumentException(
                    "Order-by direction keyword cannot be null"
            );
        }

        return switch (keyword) {
            case "asc" -> ASCENDING;
            case "desc" -> DESCENDING;

            default -> throw new IllegalArgumentException(
                    "Unsupported order-by direction: '"
                            + keyword
                            + "'. Expected 'asc' or 'desc'"
            );
        };
    }

    public boolean isAscending() {
        return this == ASCENDING;
    }

    public boolean isDescending() {
        return this == DESCENDING;
    }
}