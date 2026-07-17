package io.github.davidtodorov.odataparser.expression.ast;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

public record LiteralExpression(
        LiteralType literalType,
        Object value,
        String rawText
) implements Expression {

    public LiteralExpression {
        Objects.requireNonNull(
                literalType,
                "Literal type cannot be null"
        );

        Objects.requireNonNull(
                rawText,
                "Raw literal text cannot be null"
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

    @Override
    public <R> R accept(ExpressionVisitor<R> visitor) {
        Objects.requireNonNull(visitor, "Expression visitor cannot be null");
        return visitor.visitLiteralExpression(this);
    }
}
