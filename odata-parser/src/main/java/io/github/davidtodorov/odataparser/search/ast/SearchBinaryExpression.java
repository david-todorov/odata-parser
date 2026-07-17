package io.github.davidtodorov.odataparser.search.ast;

import io.github.davidtodorov.odataparser.common.metadata.SourceSpan;

import java.util.List;
import java.util.Objects;

public record SearchBinaryExpression(
        SearchExpression left,
        SearchBinaryOperator operator,
        SearchExpression right,
        SourceSpan sourceSpan
) implements SearchExpression {

    public SearchBinaryExpression(
            SearchExpression left,
            SearchBinaryOperator operator,
            SearchExpression right
    ) {
        this(
                left,
                operator,
                right,
                SourceSpan.unknown()
        );
    }

    public SearchBinaryExpression {
        Objects.requireNonNull(
                left,
                "Left search expression cannot be null"
        );

        Objects.requireNonNull(
                operator,
                "Search binary operator cannot be null"
        );

        Objects.requireNonNull(
                right,
                "Right search expression cannot be null"
        );

        Objects.requireNonNull(
                sourceSpan,
                "Search binary expression source span cannot be null"
        );
    }

    @Override
    public List<SearchExpression> children() {
        return List.of(
                left,
                right
        );
    }

    @Override
    public <R> R accept(
            SearchExpressionVisitor<R> visitor
    ) {
        Objects.requireNonNull(
                visitor,
                "Search expression visitor cannot be null"
        );

        return visitor.visitBinaryExpression(this);
    }
}