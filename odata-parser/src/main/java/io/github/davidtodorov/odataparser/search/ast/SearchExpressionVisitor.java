package io.github.davidtodorov.odataparser.search.ast;

public interface SearchExpressionVisitor<R> {

    R visitTermExpression(
            SearchTermExpression expression
    );

    R visitPhraseExpression(
            SearchPhraseExpression expression
    );

    R visitNotExpression(
            SearchNotExpression expression
    );

    R visitBinaryExpression(
            SearchBinaryExpression expression
    );
}
