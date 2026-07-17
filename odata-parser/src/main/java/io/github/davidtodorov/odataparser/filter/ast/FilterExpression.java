package io.github.davidtodorov.odataparser.filter.ast;

import io.github.davidtodorov.odataparser.filter.metadata.ExpressionMetadata;
import io.github.davidtodorov.odataparser.common.metadata.SourceSpan;
import io.github.davidtodorov.odataparser.common.type.ExpressionType;

import java.util.List;

public interface FilterExpression {


    ExpressionMetadata metadata();


    default SourceSpan sourceSpan() {
        return metadata().sourceSpan();
    }


    default ExpressionType expressionType() {
        return metadata().expressionType();
    }


    <R> R accept(FilterExpressionVisitor<R> visitor);


    default List<FilterExpression> children() {
        return List.of();
    }
}
