package io.github.davidtodorov.odataparser.filter.semantic;

import io.github.davidtodorov.odataparser.common.type.ExpressionType;
import io.github.davidtodorov.odataparser.filter.ast.BinaryFilterExpression;
import io.github.davidtodorov.odataparser.filter.ast.FilterExpression;
import io.github.davidtodorov.odataparser.filter.ast.FilterExpressionVisitor;
import io.github.davidtodorov.odataparser.filter.ast.FunctionCallFilterExpression;
import io.github.davidtodorov.odataparser.filter.ast.ListFilterExpression;
import io.github.davidtodorov.odataparser.filter.ast.LiteralFilterExpression;
import io.github.davidtodorov.odataparser.filter.ast.LiteralType;
import io.github.davidtodorov.odataparser.filter.ast.PropertyFilterExpression;
import io.github.davidtodorov.odataparser.filter.ast.UnaryFilterExpression;
import io.github.davidtodorov.odataparser.filter.metadata.ExpressionMetadata;
import io.github.davidtodorov.odataparser.meta.EntityMetadata;
import io.github.davidtodorov.odataparser.meta.path.ResolvedMetadataPath;
import io.github.davidtodorov.odataparser.meta.resolver.MetadataPropertyPathResolver;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class FilterExpressionResolver
        implements FilterExpressionVisitor<FilterExpression> {

    private final MetadataPropertyPathResolver propertyPathResolver;

    public FilterExpressionResolver(
            EntityMetadata<?> rootMetadata
    ) {
        this.propertyPathResolver =
                new MetadataPropertyPathResolver(
                        Objects.requireNonNull(
                                rootMetadata,
                                "Root entity metadata cannot be null"
                        )
                );
    }

    public FilterExpression resolve(
            FilterExpression root
    ) {
        Objects.requireNonNull(
                root,
                "Root expression cannot be null"
        );

        FilterExpression resolved =
                root.accept(this);

        if (resolved.expressionType()
                != ExpressionType.BOOLEAN) {

            throw new FilterSemanticException(
                    "The complete filter expression must return BOOLEAN, "
                            + "but returned "
                            + resolved.expressionType(),
                    resolved
            );
        }

        return resolved;
    }

    @Override
    public FilterExpression visitPropertyExpression(
            PropertyFilterExpression expression
    ) {
        try {
            ResolvedMetadataPath resolvedPath =
                    propertyPathResolver.resolve(
                            expression.pathSegments()
                    );

            return expression.withResolvedPath(
                    resolvedPath
            );
        } catch (IllegalArgumentException exception) {
            throw new FilterSemanticException(
                    exception.getMessage(),
                    expression,
                    exception
            );
        }
    }

    @Override
    public FilterExpression visitLiteralExpression(
            LiteralFilterExpression expression
    ) {
        ExpressionType expressionType =
                expressionTypeOf(
                        expression.literalType()
                );

        ExpressionMetadata metadata =
                new ExpressionMetadata(
                        expression.sourceSpan(),
                        expressionType
                );

        return new LiteralFilterExpression(
                expression.literalType(),
                expression.value(),
                expression.rawText(),
                metadata
        );
    }

    @Override
    public FilterExpression visitUnaryExpression(
            UnaryFilterExpression expression
    ) {
        FilterExpression resolvedOperand =
                expression.operand().accept(this);

        requireType(
                resolvedOperand,
                ExpressionType.BOOLEAN,
                "Operator '"
                        + expression.operator().keyword()
                        + "' requires a BOOLEAN operand"
        );

        ExpressionMetadata metadata =
                new ExpressionMetadata(
                        expression.sourceSpan(),
                        ExpressionType.BOOLEAN
                );

        return new UnaryFilterExpression(
                expression.operator(),
                resolvedOperand,
                metadata
        );
    }

    @Override
    public FilterExpression visitBinaryExpression(
            BinaryFilterExpression expression
    ) {
        FilterExpression resolvedLeft =
                expression.left().accept(this);

        FilterExpression resolvedRight =
                expression.right().accept(this);

        ExpressionType resultType =
                switch (expression.operator()) {
                    case AND, OR ->
                            resolveLogicalExpression(
                                    expression,
                                    resolvedLeft,
                                    resolvedRight
                            );

                    case ADD, SUB, MUL, DIV, MOD ->
                            resolveArithmeticExpression(
                                    expression,
                                    resolvedLeft,
                                    resolvedRight
                            );

                    case EQ, NE ->
                            resolveEqualityComparison(
                                    expression,
                                    resolvedLeft,
                                    resolvedRight
                            );

                    case GT, GE, LT, LE ->
                            resolveOrderedComparison(
                                    expression,
                                    resolvedLeft,
                                    resolvedRight
                            );

                    case IN ->
                            resolveInExpression(
                                    expression,
                                    resolvedLeft,
                                    resolvedRight
                            );
                };

        ExpressionMetadata metadata =
                new ExpressionMetadata(
                        expression.sourceSpan(),
                        resultType
                );

        return new BinaryFilterExpression(
                resolvedLeft,
                expression.operator(),
                resolvedRight,
                metadata
        );
    }

    @Override
    public FilterExpression visitListExpression(
            ListFilterExpression expression
    ) {
        List<FilterExpression> resolvedElements =
                expression.elements()
                        .stream()
                        .map(element -> element.accept(this))
                        .toList();

        ExpressionMetadata metadata =
                new ExpressionMetadata(
                        expression.sourceSpan(),
                        ExpressionType.COLLECTION
                );

        return new ListFilterExpression(
                resolvedElements,
                metadata
        );
    }

    @Override
    public FilterExpression visitFunctionCallExpression(
            FunctionCallFilterExpression expression
    ) {
        List<FilterExpression> resolvedArguments =
                expression.arguments()
                        .stream()
                        .map(argument -> argument.accept(this))
                        .toList();

        ExpressionType returnType =
                resolveFunctionType(
                        expression,
                        resolvedArguments
                );

        ExpressionMetadata metadata =
                new ExpressionMetadata(
                        expression.sourceSpan(),
                        returnType
                );

        return new FunctionCallFilterExpression(
                expression.functionName(),
                resolvedArguments,
                metadata
        );
    }

    private ExpressionType resolveLogicalExpression(
            BinaryFilterExpression original,
            FilterExpression left,
            FilterExpression right
    ) {
        requireType(
                left,
                ExpressionType.BOOLEAN,
                "Left operand of '"
                        + original.operator().keyword()
                        + "' must be BOOLEAN"
        );

        requireType(
                right,
                ExpressionType.BOOLEAN,
                "Right operand of '"
                        + original.operator().keyword()
                        + "' must be BOOLEAN"
        );

        return ExpressionType.BOOLEAN;
    }

    private ExpressionType resolveArithmeticExpression(
            BinaryFilterExpression original,
            FilterExpression left,
            FilterExpression right
    ) {
        if (!isNumeric(left.expressionType())) {
            throw new FilterSemanticException(
                    "Left operand of arithmetic operator '"
                            + original.operator().keyword()
                            + "' must be numeric, but was "
                            + left.expressionType(),
                    left
            );
        }

        if (!isNumeric(right.expressionType())) {
            throw new FilterSemanticException(
                    "Right operand of arithmetic operator '"
                            + original.operator().keyword()
                            + "' must be numeric, but was "
                            + right.expressionType(),
                    right
            );
        }

        return promoteNumericType(
                left.expressionType(),
                right.expressionType()
        );
    }

    private ExpressionType resolveEqualityComparison(
            BinaryFilterExpression original,
            FilterExpression left,
            FilterExpression right
    ) {
        if (!areEqualityCompatible(
                left.expressionType(),
                right.expressionType()
        )) {
            throw new FilterSemanticException(
                    "Operator '"
                            + original.operator().keyword()
                            + "' cannot compare "
                            + left.expressionType()
                            + " with "
                            + right.expressionType(),
                    original
            );
        }

        return ExpressionType.BOOLEAN;
    }

    private ExpressionType resolveOrderedComparison(
            BinaryFilterExpression original,
            FilterExpression left,
            FilterExpression right
    ) {
        if (!areOrderCompatible(
                left.expressionType(),
                right.expressionType()
        )) {
            throw new FilterSemanticException(
                    "Operator '"
                            + original.operator().keyword()
                            + "' cannot order-compare "
                            + left.expressionType()
                            + " with "
                            + right.expressionType(),
                    original
            );
        }

        return ExpressionType.BOOLEAN;
    }

    private ExpressionType resolveInExpression(
            BinaryFilterExpression original,
            FilterExpression left,
            FilterExpression right
    ) {
        if (!(right
                instanceof ListFilterExpression listExpression)) {

            throw new FilterSemanticException(
                    "The right operand of 'in' must be a list",
                    right
            );
        }

        for (FilterExpression element
                : listExpression.elements()) {

            if (!areEqualityCompatible(
                    left.expressionType(),
                    element.expressionType()
            )) {
                throw new FilterSemanticException(
                        "List element of type "
                                + element.expressionType()
                                + " is not compatible with left operand type "
                                + left.expressionType(),
                        element
                );
            }
        }

        return ExpressionType.BOOLEAN;
    }

    private ExpressionType resolveFunctionType(
            FunctionCallFilterExpression function,
            List<FilterExpression> arguments
    ) {
        String normalizedName =
                function.functionName()
                        .toLowerCase(Locale.ROOT);

        return switch (normalizedName) {
            case "contains",
                 "startswith",
                 "endswith" -> {

                requireArgumentCount(
                        function,
                        arguments,
                        2
                );

                requireStringArgument(
                        function,
                        arguments,
                        0
                );

                requireStringArgument(
                        function,
                        arguments,
                        1
                );

                yield ExpressionType.BOOLEAN;
            }

            case "tolower",
                 "toupper",
                 "trim" -> {

                requireArgumentCount(
                        function,
                        arguments,
                        1
                );

                requireStringArgument(
                        function,
                        arguments,
                        0
                );

                yield ExpressionType.STRING;
            }

            case "length" -> {
                requireArgumentCount(
                        function,
                        arguments,
                        1
                );

                requireStringArgument(
                        function,
                        arguments,
                        0
                );

                yield ExpressionType.INTEGER;
            }

            default -> throw new FilterSemanticException(
                    "Unsupported function '"
                            + function.functionName()
                            + "'",
                    function
            );
        };
    }

    private void requireArgumentCount(
            FunctionCallFilterExpression function,
            List<FilterExpression> arguments,
            int expectedCount
    ) {
        if (arguments.size() != expectedCount) {
            throw new FilterSemanticException(
                    "Function '"
                            + function.functionName()
                            + "' requires "
                            + expectedCount
                            + " argument(s), but received "
                            + arguments.size(),
                    function
            );
        }
    }

    private void requireStringArgument(
            FunctionCallFilterExpression function,
            List<FilterExpression> arguments,
            int argumentIndex
    ) {
        FilterExpression argument =
                arguments.get(argumentIndex);

        if (argument.expressionType()
                != ExpressionType.STRING) {

            throw new FilterSemanticException(
                    "Argument "
                            + (argumentIndex + 1)
                            + " of function '"
                            + function.functionName()
                            + "' must be STRING, but was "
                            + argument.expressionType(),
                    argument
            );
        }
    }

    private void requireType(
            FilterExpression expression,
            ExpressionType requiredType,
            String message
    ) {
        if (expression.expressionType()
                != requiredType) {

            throw new FilterSemanticException(
                    message
                            + ", but was "
                            + expression.expressionType(),
                    expression
            );
        }
    }

    private boolean areEqualityCompatible(
            ExpressionType left,
            ExpressionType right
    ) {
        if (left == ExpressionType.NULL
                || right == ExpressionType.NULL) {

            return left != ExpressionType.COLLECTION
                    && right != ExpressionType.COLLECTION;
        }

        if (left == right) {
            return left != ExpressionType.COLLECTION
                    && left != ExpressionType.UNKNOWN;
        }

        return isNumeric(left)
                && isNumeric(right);
    }

    private boolean areOrderCompatible(
            ExpressionType left,
            ExpressionType right
    ) {
        if (isNumeric(left)
                && isNumeric(right)) {

            return true;
        }

        if (left != right) {
            return false;
        }

        return switch (left) {
            case STRING,
                 DATE,
                 DATE_TIME_OFFSET -> true;

            default -> false;
        };
    }

    private boolean isNumeric(
            ExpressionType type
    ) {
        return switch (type) {
            case INTEGER,
                 DECIMAL,
                 FLOATING_POINT -> true;

            default -> false;
        };
    }

    private ExpressionType promoteNumericType(
            ExpressionType left,
            ExpressionType right
    ) {
        if (left == ExpressionType.FLOATING_POINT
                || right == ExpressionType.FLOATING_POINT) {

            return ExpressionType.FLOATING_POINT;
        }

        if (left == ExpressionType.DECIMAL
                || right == ExpressionType.DECIMAL) {

            return ExpressionType.DECIMAL;
        }

        return ExpressionType.INTEGER;
    }

    private ExpressionType expressionTypeOf(
            LiteralType literalType
    ) {
        return switch (literalType) {
            case STRING ->
                    ExpressionType.STRING;

            case INTEGER ->
                    ExpressionType.INTEGER;

            case DECIMAL ->
                    ExpressionType.DECIMAL;

            case BOOLEAN ->
                    ExpressionType.BOOLEAN;

            case NULL ->
                    ExpressionType.NULL;
        };
    }
}