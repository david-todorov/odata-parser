package io.github.davidtodorov.odataparser.filter.ast;

import io.github.davidtodorov.odataparser.filter.metadata.ExpressionMetadata;
import io.github.davidtodorov.odataparser.common.metadata.SourceSpan;
import io.github.davidtodorov.odataparser.common.type.ExpressionType;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

public record LiteralFilterExpression(
        LiteralType literalType,
        Object value,
        String rawText,
        ExpressionMetadata metadata
) implements FilterExpression {


    public LiteralFilterExpression(
            LiteralType literalType,
            Object value,
            String rawText
    ) {
        this(
                literalType,
                value,
                rawText,
                new ExpressionMetadata(
                        SourceSpan.unknown(),
                        initialExpressionType(literalType)
                )
        );
    }

    public LiteralFilterExpression {
        Objects.requireNonNull(
                literalType,
                "Literal type cannot be null"
        );

        Objects.requireNonNull(
                rawText,
                "Raw literal text cannot be null"
        );

        Objects.requireNonNull(
                metadata,
                "Expression metadata cannot be null"
        );

        if (rawText.isBlank()) {
            throw new IllegalArgumentException(
                    "Raw literal text cannot be blank"
            );
        }

        if (!hasExpectedValueType(literalType, value)) {
            throw new IllegalArgumentException(
                    "Value does not match literal type " + literalType
            );
        }
    }

    @Override
    public <R> R accept(FilterExpressionVisitor<R> visitor) {
        Objects.requireNonNull(
                visitor,
                "Expression visitor cannot be null"
        );

        return visitor.visitLiteralExpression(this);
    }

    private static boolean hasExpectedValueType(
            LiteralType literalType,
            Object value
    ) {
        return switch (literalType) {
            case STRING -> value instanceof String;
            case INTEGER -> value instanceof BigInteger;
            case DECIMAL -> value instanceof BigDecimal;
            case BOOLEAN -> value instanceof Boolean;
            case NULL -> value == null;
        };
    }

    private static ExpressionType initialExpressionType(
            LiteralType literalType
    ) {
        Objects.requireNonNull(
                literalType,
                "Literal type cannot be null"
        );

        return switch (literalType) {
            case STRING -> ExpressionType.STRING;
            case INTEGER -> ExpressionType.INTEGER;
            case DECIMAL -> ExpressionType.DECIMAL;
            case BOOLEAN -> ExpressionType.BOOLEAN;
            case NULL -> ExpressionType.NULL;
        };
    }
}