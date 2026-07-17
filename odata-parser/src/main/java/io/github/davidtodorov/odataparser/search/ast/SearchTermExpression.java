package io.github.davidtodorov.odataparser.search.ast;

import io.github.davidtodorov.odataparser.common.metadata.SourceSpan;

import java.util.Objects;

public record SearchTermExpression(
        String term,
        SourceSpan sourceSpan
) implements SearchExpression {

    public SearchTermExpression(String term) {
        this(
                term,
                SourceSpan.unknown()
        );
    }

    public SearchTermExpression {
        Objects.requireNonNull(
                term,
                "Search term cannot be null"
        );

        Objects.requireNonNull(
                sourceSpan,
                "Search term source span cannot be null"
        );

        if (term.isBlank()) {
            throw new IllegalArgumentException(
                    "Search term cannot be blank"
            );
        }

        if (term.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(
                    "An unquoted search term cannot contain whitespace: '"
                            + term
                            + "'"
            );
        }
    }

    @Override
    public <R> R accept(
            SearchExpressionVisitor<R> visitor
    ) {
        Objects.requireNonNull(
                visitor,
                "Search expression visitor cannot be null"
        );

        return visitor.visitTermExpression(this);
    }
}