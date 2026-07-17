package io.github.davidtodorov.odataparser.filter.metadata;

import io.github.davidtodorov.odataparser.common.metadata.SourceSpan;
import io.github.davidtodorov.odataparser.common.type.ExpressionType;

import java.util.Objects;

public record ExpressionMetadata(
        SourceSpan sourceSpan,
        ExpressionType expressionType
) {

    public ExpressionMetadata {
        Objects.requireNonNull(
                sourceSpan,
                "Source span cannot be null"
        );

        Objects.requireNonNull(
                expressionType,
                "Expression type cannot be null"
        );
    }

    public static ExpressionMetadata unresolved(SourceSpan sourceSpan) {
        return new ExpressionMetadata(
                sourceSpan,
                ExpressionType.UNKNOWN
        );
    }

    public ExpressionMetadata withType(ExpressionType expressionType) {
        return new ExpressionMetadata(
                sourceSpan,
                expressionType
        );
    }
}
