package io.github.davidtodorov.odataparser.search.ast;

import io.github.davidtodorov.odataparser.common.metadata.SourceSpan;

import java.util.List;
import java.util.Objects;

public record SearchNotExpression(
        SearchExpression operand,
        SourceSpan sourceSpan
) implements SearchExpression {

    public SearchNotExpression(
            SearchExpression operand
    ) {
        this(
                operand,
                SourceSpan.unknown()
        );
    }

    public SearchNotExpression {
        Objects.requireNonNull(
                operand,
                "Search NOT operand cannot be null"
        );

        Objects.requireNonNull(
                sourceSpan,
                "Search NOT source span cannot be null"
        );
    }

    @Override
    public List<SearchExpression> children() {
        return List.of(operand);
    }

    @Override
    public <R> R accept(
            SearchExpressionVisitor<R> visitor
    ) {
        Objects.requireNonNull(
                visitor,
                "Search expression visitor cannot be null"
        );

        return visitor.visitNotExpression(this);
    }
}