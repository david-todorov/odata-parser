package io.github.davidtodorov.odataparser.search.lexer;

public enum SearchTokenType {

    TERM,

    PHRASE,

    AND,

    OR,

    NOT,

    LEFT_PARENTHESIS,

    RIGHT_PARENTHESIS,

    END_OF_INPUT;

    public boolean isBinaryOperator() {
        return this == AND || this == OR;
    }

    public boolean startsExpression() {
        return switch (this) {
            case TERM,
                 PHRASE,
                 NOT,
                 LEFT_PARENTHESIS -> true;

            default -> false;
        };
    }

    public boolean endsExpression() {
        return switch (this) {
            case TERM,
                 PHRASE,
                 RIGHT_PARENTHESIS -> true;

            default -> false;
        };
    }
}