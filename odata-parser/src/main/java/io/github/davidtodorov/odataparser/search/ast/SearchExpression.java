package io.github.davidtodorov.odataparser.search.ast;

import io.github.davidtodorov.odataparser.common.metadata.SourceSpan;

import java.util.List;

public interface SearchExpression {

    SourceSpan sourceSpan();

    <R> R accept(
            SearchExpressionVisitor<R> visitor
    );

    default List<SearchExpression> children() {
        return List.of();
    }

    default boolean isLeaf() {
        return children().isEmpty();
    }
}