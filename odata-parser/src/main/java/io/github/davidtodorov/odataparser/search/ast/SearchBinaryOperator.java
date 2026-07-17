package io.github.davidtodorov.odataparser.search.ast;

public enum SearchBinaryOperator {

    AND("AND"),
    OR("OR");

    private final String keyword;

    SearchBinaryOperator(String keyword) {
        this.keyword = keyword;
    }

    public String keyword() {
        return keyword;
    }

    public static SearchBinaryOperator fromKeyword(
            String keyword
    ) {
        if (keyword == null) {
            throw new IllegalArgumentException(
                    "Search operator keyword cannot be null"
            );
        }

        return switch (keyword) {
            case "AND" -> AND;
            case "OR" -> OR;

            default -> throw new IllegalArgumentException(
                    "Unsupported search binary operator '"
                            + keyword
                            + "'. Expected 'AND' or 'OR'"
            );
        };
    }
}
