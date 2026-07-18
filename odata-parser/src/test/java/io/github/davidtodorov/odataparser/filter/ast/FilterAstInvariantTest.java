package io.github.davidtodorov.odataparser.filter.ast;

import io.github.davidtodorov.odataparser.common.metadata.SourceSpan;
import io.github.davidtodorov.odataparser.common.type.ExpressionType;
import io.github.davidtodorov.odataparser.filter.metadata.ExpressionMetadata;
import io.github.davidtodorov.odataparser.meta.MetadataRegistry;
import io.github.davidtodorov.odataparser.meta.demo.CaseMetadata;
import io.github.davidtodorov.odataparser.meta.demo.DepartmentMetadata;
import io.github.davidtodorov.odataparser.meta.demo.UserMetadata;
import io.github.davidtodorov.odataparser.meta.path.ResolvedMetadataPath;
import io.github.davidtodorov.odataparser.meta.resolver.MetadataPropertyPathResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilterAstInvariantTest {

    private MetadataPropertyPathResolver propertyPathResolver;

    @BeforeEach
    void setUp() {
        DepartmentMetadata departmentMetadata =
                new DepartmentMetadata();

        UserMetadata userMetadata =
                new UserMetadata();

        CaseMetadata caseMetadata =
                new CaseMetadata();

        MetadataRegistry.of(
                caseMetadata,
                userMetadata,
                departmentMetadata
        );

        propertyPathResolver =
                new MetadataPropertyPathResolver(
                        caseMetadata
                );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("booleanBinaryOperatorCases")
    void shouldInitializeBooleanBinaryOperatorWithBooleanType(
            String description,
            BinaryOperator operator
    ) {
        FilterExpression left =
                property(
                        "Left",
                        2,
                        6
                );

        FilterExpression right =
                property(
                        "Right",
                        10,
                        15
                );

        BinaryFilterExpression expression =
                new BinaryFilterExpression(
                        left,
                        operator,
                        right
                );

        assertAll(
                () -> assertEquals(
                        ExpressionType.BOOLEAN,
                        expression.expressionType()
                ),
                () -> assertEquals(
                        new SourceSpan(2, 15),
                        expression.sourceSpan()
                ),
                () -> assertSame(
                        left,
                        expression.left()
                ),
                () -> assertSame(
                        right,
                        expression.right()
                ),
                () -> assertEquals(
                        operator,
                        expression.operator()
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("arithmeticBinaryOperatorCases")
    void shouldInitializeArithmeticBinaryOperatorWithUnknownType(
            String description,
            BinaryOperator operator
    ) {
        BinaryFilterExpression expression =
                new BinaryFilterExpression(
                        integerLiteral(
                                1,
                                "1",
                                0,
                                1
                        ),
                        operator,
                        integerLiteral(
                                2,
                                "2",
                                4,
                                5
                        )
                );

        assertAll(
                () -> assertEquals(
                        ExpressionType.UNKNOWN,
                        expression.expressionType()
                ),
                () -> assertEquals(
                        new SourceSpan(0, 5),
                        expression.sourceSpan()
                )
        );
    }

    @Test
    void shouldPreserveExplicitBinaryMetadata() {
        FilterExpression left =
                property(
                        "Amount",
                        0,
                        6
                );

        FilterExpression right =
                decimalLiteral(
                        "10.50",
                        10,
                        15
                );

        ExpressionMetadata metadata =
                metadata(
                        0,
                        15,
                        ExpressionType.DECIMAL
                );

        BinaryFilterExpression expression =
                new BinaryFilterExpression(
                        left,
                        BinaryOperator.ADD,
                        right,
                        metadata
                );

        assertAll(
                () -> assertSame(
                        metadata,
                        expression.metadata()
                ),
                () -> assertEquals(
                        ExpressionType.DECIMAL,
                        expression.expressionType()
                ),
                () -> assertEquals(
                        new SourceSpan(0, 15),
                        expression.sourceSpan()
                )
        );
    }

    @Test
    void shouldExposeBinaryChildrenInOperandOrder() {
        FilterExpression left =
                property(
                        "Deleted",
                        0,
                        7
                );

        FilterExpression right =
                property(
                        "Closed",
                        12,
                        18
                );

        BinaryFilterExpression expression =
                new BinaryFilterExpression(
                        left,
                        BinaryOperator.AND,
                        right
                );

        assertAll(
                () -> assertEquals(
                        List.of(left, right),
                        expression.children()
                ),
                () -> assertSame(
                        left,
                        expression.children().getFirst()
                ),
                () -> assertSame(
                        right,
                        expression.children().getLast()
                ),
                () -> assertThrows(
                        UnsupportedOperationException.class,
                        () -> expression.children().add(
                                booleanLiteral(
                                        true,
                                        20,
                                        24
                                )
                        )
                )
        );
    }

    @Test
    void shouldRejectNullBinaryLeftOperand() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new BinaryFilterExpression(
                                null,
                                BinaryOperator.EQ,
                                stringLiteral(
                                        "test",
                                        "'test'",
                                        3,
                                        9
                                ),
                                metadata(
                                        0,
                                        9,
                                        ExpressionType.BOOLEAN
                                )
                        )
                );

        assertEquals(
                "Left expression cannot be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullBinaryOperator() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new BinaryFilterExpression(
                                property(
                                        "Title",
                                        0,
                                        5
                                ),
                                null,
                                stringLiteral(
                                        "test",
                                        "'test'",
                                        9,
                                        15
                                ),
                                metadata(
                                        0,
                                        15,
                                        ExpressionType.BOOLEAN
                                )
                        )
                );

        assertEquals(
                "Binary operator cannot be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullBinaryRightOperand() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new BinaryFilterExpression(
                                property(
                                        "Title",
                                        0,
                                        5
                                ),
                                BinaryOperator.EQ,
                                null,
                                metadata(
                                        0,
                                        8,
                                        ExpressionType.BOOLEAN
                                )
                        )
                );

        assertEquals(
                "Right expression cannot be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullBinaryMetadata() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new BinaryFilterExpression(
                                property(
                                        "Title",
                                        0,
                                        5
                                ),
                                BinaryOperator.EQ,
                                stringLiteral(
                                        "test",
                                        "'test'",
                                        9,
                                        15
                                ),
                                null
                        )
                );

        assertEquals(
                "Expression metadata cannot be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldInitializeUnaryExpressionAsBoolean() {
        FilterExpression operand =
                property(
                        "Deleted",
                        4,
                        11
                );

        UnaryFilterExpression expression =
                new UnaryFilterExpression(
                        UnaryOperator.NOT,
                        operand
                );

        assertAll(
                () -> assertEquals(
                        UnaryOperator.NOT,
                        expression.operator()
                ),
                () -> assertSame(
                        operand,
                        expression.operand()
                ),
                () -> assertEquals(
                        ExpressionType.BOOLEAN,
                        expression.expressionType()
                ),
                () -> assertEquals(
                        operand.sourceSpan(),
                        expression.sourceSpan()
                ),
                () -> assertEquals(
                        List.of(operand),
                        expression.children()
                )
        );
    }

    @Test
    void shouldPreserveExplicitUnaryMetadata() {
        FilterExpression operand =
                property(
                        "Deleted",
                        4,
                        11
                );

        ExpressionMetadata metadata =
                metadata(
                        0,
                        11,
                        ExpressionType.BOOLEAN
                );

        UnaryFilterExpression expression =
                new UnaryFilterExpression(
                        UnaryOperator.NOT,
                        operand,
                        metadata
                );

        assertSame(
                metadata,
                expression.metadata()
        );
    }

    @Test
    void shouldReturnImmutableUnaryChildren() {
        UnaryFilterExpression expression =
                new UnaryFilterExpression(
                        UnaryOperator.NOT,
                        property(
                                "Deleted",
                                4,
                                11
                        )
                );

        assertThrows(
                UnsupportedOperationException.class,
                () -> expression.children().clear()
        );
    }

    @Test
    void shouldRejectNullUnaryOperator() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new UnaryFilterExpression(
                                null,
                                property(
                                        "Deleted",
                                        4,
                                        11
                                ),
                                metadata(
                                        0,
                                        11,
                                        ExpressionType.BOOLEAN
                                )
                        )
                );

        assertEquals(
                "Unary operator cannot be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullUnaryOperand() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new UnaryFilterExpression(
                                UnaryOperator.NOT,
                                null,
                                metadata(
                                        0,
                                        3,
                                        ExpressionType.BOOLEAN
                                )
                        )
                );

        assertEquals(
                "Unary operand cannot be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullUnaryMetadata() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new UnaryFilterExpression(
                                UnaryOperator.NOT,
                                property(
                                        "Deleted",
                                        4,
                                        11
                                ),
                                null
                        )
                );

        assertEquals(
                "Expression metadata cannot be null",
                exception.getMessage()
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validLiteralCases")
    void shouldCreateLiteralWithExpectedType(
            String description,
            LiteralType literalType,
            Object value,
            String rawText,
            ExpressionType expectedExpressionType
    ) {
        LiteralFilterExpression literal =
                new LiteralFilterExpression(
                        literalType,
                        value,
                        rawText
                );

        assertAll(
                () -> assertEquals(
                        literalType,
                        literal.literalType()
                ),
                () -> assertEquals(
                        value,
                        literal.value()
                ),
                () -> assertEquals(
                        rawText,
                        literal.rawText()
                ),
                () -> assertEquals(
                        expectedExpressionType,
                        literal.expressionType()
                ),
                () -> assertTrue(
                        literal.sourceSpan().isUnknown()
                ),
                () -> assertTrue(
                        literal.children().isEmpty()
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidLiteralValueCases")
    void shouldRejectLiteralValueWithIncorrectJavaType(
            String description,
            LiteralType literalType,
            Object value,
            String rawText
    ) {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new LiteralFilterExpression(
                                literalType,
                                value,
                                rawText
                        )
                );

        assertEquals(
                "Value does not match literal type "
                        + literalType,
                exception.getMessage()
        );
    }

    @Test
    void shouldPreserveExplicitLiteralMetadata() {
        ExpressionMetadata metadata =
                metadata(
                        10,
                        18,
                        ExpressionType.STRING
                );

        LiteralFilterExpression literal =
                new LiteralFilterExpression(
                        LiteralType.STRING,
                        "urgent",
                        "'urgent'",
                        metadata
                );

        assertAll(
                () -> assertSame(
                        metadata,
                        literal.metadata()
                ),
                () -> assertEquals(
                        new SourceSpan(10, 18),
                        literal.sourceSpan()
                ),
                () -> assertEquals(
                        ExpressionType.STRING,
                        literal.expressionType()
                )
        );
    }

    @Test
    void shouldRejectNullLiteralType() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new LiteralFilterExpression(
                                null,
                                "test",
                                "'test'",
                                metadata(
                                        0,
                                        6,
                                        ExpressionType.STRING
                                )
                        )
                );

        assertEquals(
                "Literal type cannot be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullRawLiteralText() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new LiteralFilterExpression(
                                LiteralType.STRING,
                                "test",
                                null,
                                metadata(
                                        0,
                                        6,
                                        ExpressionType.STRING
                                )
                        )
                );

        assertEquals(
                "Raw literal text cannot be null",
                exception.getMessage()
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            " ",
            "\t",
            "\r\n"
    })
    void shouldRejectBlankRawLiteralText(
            String rawText
    ) {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new LiteralFilterExpression(
                                LiteralType.STRING,
                                "test",
                                rawText,
                                metadata(
                                        0,
                                        1,
                                        ExpressionType.STRING
                                )
                        )
                );

        assertEquals(
                "Raw literal text cannot be blank",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullLiteralMetadata() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new LiteralFilterExpression(
                                LiteralType.STRING,
                                "test",
                                "'test'",
                                null
                        )
                );

        assertEquals(
                "Expression metadata cannot be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldReturnImmutableEmptyLiteralChildren() {
        LiteralFilterExpression literal =
                stringLiteral(
                        "urgent",
                        "'urgent'",
                        0,
                        8
                );

        assertAll(
                () -> assertTrue(
                        literal.children().isEmpty()
                ),
                () -> assertThrows(
                        UnsupportedOperationException.class,
                        () -> literal.children().add(
                                property(
                                        "Title",
                                        0,
                                        5
                                )
                        )
                )
        );
    }

    @Test
    void shouldCreateUnresolvedPropertyExpression() {
        PropertyFilterExpression property =
                new PropertyFilterExpression(
                        List.of(
                                "Owner",
                                "Department",
                                "Code"
                        )
                );

        assertAll(
                () -> assertEquals(
                        List.of(
                                "Owner",
                                "Department",
                                "Code"
                        ),
                        property.pathSegments()
                ),
                () -> assertEquals(
                        "Owner/Department/Code",
                        property.path()
                ),
                () -> assertFalse(
                        property.isResolved()
                ),
                () -> assertTrue(
                        property.resolvedPath().isEmpty()
                ),
                () -> assertEquals(
                        ExpressionType.UNKNOWN,
                        property.expressionType()
                ),
                () -> assertTrue(
                        property.sourceSpan().isUnknown()
                ),
                () -> assertTrue(
                        property.children().isEmpty()
                )
        );
    }

    @Test
    void shouldPreserveExplicitUnresolvedPropertyMetadata() {
        ExpressionMetadata metadata =
                metadata(
                        2,
                        23,
                        ExpressionType.UNKNOWN
                );

        PropertyFilterExpression property =
                new PropertyFilterExpression(
                        List.of(
                                "Owner",
                                "Department",
                                "Code"
                        ),
                        metadata
                );

        assertAll(
                () -> assertSame(
                        metadata,
                        property.metadata()
                ),
                () -> assertEquals(
                        new SourceSpan(2, 23),
                        property.sourceSpan()
                ),
                () -> assertFalse(
                        property.isResolved()
                )
        );
    }

    @Test
    void shouldDefensivelyCopyPropertyPathSegments() {
        List<String> source =
                new ArrayList<>(
                        List.of(
                                "Owner",
                                "Username"
                        )
                );

        PropertyFilterExpression property =
                new PropertyFilterExpression(
                        source
                );

        source.set(
                0,
                "Reviewer"
        );

        assertAll(
                () -> assertEquals(
                        List.of(
                                "Owner",
                                "Username"
                        ),
                        property.pathSegments()
                ),
                () -> assertEquals(
                        "Owner/Username",
                        property.path()
                ),
                () -> assertThrows(
                        UnsupportedOperationException.class,
                        () -> property.pathSegments().add(
                                "Other"
                        )
                )
        );
    }

    @Test
    void shouldRejectNullPropertyPathSegments() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new PropertyFilterExpression(
                                null,
                                Optional.empty(),
                                unresolvedMetadata(
                                        0,
                                        0
                                )
                        )
                );

        assertEquals(
                "Path segments cannot be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectEmptyPropertyPath() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new PropertyFilterExpression(
                                List.of()
                        )
                );

        assertEquals(
                "A property expression must contain at least one path segment",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectPropertyPathContainingNullSegment() {
        List<String> segments =
                new ArrayList<>();

        segments.add("Owner");
        segments.add(null);
        segments.add("Username");

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new PropertyFilterExpression(
                                segments
                        )
                );

        assertEquals(
                "Property path segments cannot be null or blank",
                exception.getMessage()
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            " ",
            "\t",
            "\r\n"
    })
    void shouldRejectPropertyPathContainingBlankSegment(
            String blankSegment
    ) {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new PropertyFilterExpression(
                                List.of(
                                        "Owner",
                                        blankSegment,
                                        "Username"
                                )
                        )
                );

        assertEquals(
                "Property path segments cannot be null or blank",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullResolvedPathOptional() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new PropertyFilterExpression(
                                List.of("Title"),
                                null,
                                unresolvedMetadata(
                                        0,
                                        5
                                )
                        )
                );

        assertEquals(
                "Resolved metadata path cannot be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullPropertyMetadata() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new PropertyFilterExpression(
                                List.of("Title"),
                                Optional.empty(),
                                null
                        )
                );

        assertEquals(
                "Expression metadata cannot be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldResolvePropertyWithoutMutatingOriginalNode() {
        PropertyFilterExpression unresolved =
                new PropertyFilterExpression(
                        List.of("Title"),
                        unresolvedMetadata(
                                2,
                                7
                        )
                );

        ResolvedMetadataPath resolvedPath =
                propertyPathResolver.resolve(
                        List.of("Title")
                );

        PropertyFilterExpression resolved =
                unresolved.withResolvedPath(
                        resolvedPath
                );

        assertAll(
                () -> assertNotSame(
                        unresolved,
                        resolved
                ),
                () -> assertFalse(
                        unresolved.isResolved()
                ),
                () -> assertEquals(
                        ExpressionType.UNKNOWN,
                        unresolved.expressionType()
                ),
                () -> assertTrue(
                        resolved.isResolved()
                ),
                () -> assertSame(
                        resolvedPath,
                        resolved.resolvedPath().orElseThrow()
                ),
                () -> assertEquals(
                        ExpressionType.STRING,
                        resolved.expressionType()
                ),
                () -> assertEquals(
                        unresolved.pathSegments(),
                        resolved.pathSegments()
                ),
                () -> assertEquals(
                        unresolved.sourceSpan(),
                        resolved.sourceSpan()
                )
        );
    }

    @Test
    void shouldCreatePropertyWithMatchingResolvedPathAndType() {
        ResolvedMetadataPath resolvedPath =
                propertyPathResolver.resolve(
                        List.of(
                                "Owner",
                                "Department",
                                "Code"
                        )
                );

        ExpressionMetadata metadata =
                metadata(
                        0,
                        21,
                        ExpressionType.STRING
                );

        PropertyFilterExpression property =
                new PropertyFilterExpression(
                        List.of(
                                "Owner",
                                "Department",
                                "Code"
                        ),
                        Optional.of(resolvedPath),
                        metadata
                );

        assertAll(
                () -> assertTrue(
                        property.isResolved()
                ),
                () -> assertSame(
                        resolvedPath,
                        property.resolvedPath().orElseThrow()
                ),
                () -> assertEquals(
                        ExpressionType.STRING,
                        property.expressionType()
                ),
                () -> assertEquals(
                        "Owner/Department/Code",
                        property.path()
                )
        );
    }

    @Test
    void shouldRejectResolvedPathThatDoesNotMatchPropertyPath() {
        ResolvedMetadataPath titlePath =
                propertyPathResolver.resolve(
                        List.of("Title")
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new PropertyFilterExpression(
                                List.of("Reference"),
                                Optional.of(titlePath),
                                metadata(
                                        0,
                                        9,
                                        ExpressionType.STRING
                                )
                        )
                );

        assertTrue(
                exception.getMessage().contains(
                        "Resolved path does not match the property expression"
                )
        );
    }

    @Test
    void shouldRejectResolvedPathWithDifferentExpressionType() {
        ResolvedMetadataPath titlePath =
                propertyPathResolver.resolve(
                        List.of("Title")
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new PropertyFilterExpression(
                                List.of("Title"),
                                Optional.of(titlePath),
                                metadata(
                                        0,
                                        5,
                                        ExpressionType.BOOLEAN
                                )
                        )
                );

        assertEquals(
                "Property expression type BOOLEAN "
                        + "does not match resolved path type STRING",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullPathPassedToWithResolvedPath() {
        PropertyFilterExpression property =
                new PropertyFilterExpression(
                        List.of("Title")
                );

        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> property.withResolvedPath(null)
                );

        assertEquals(
                "Resolved metadata path cannot be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldReturnImmutableEmptyPropertyChildren() {
        PropertyFilterExpression property =
                new PropertyFilterExpression(
                        List.of("Title")
                );

        assertAll(
                () -> assertTrue(
                        property.children().isEmpty()
                ),
                () -> assertThrows(
                        UnsupportedOperationException.class,
                        () -> property.children().add(
                                stringLiteral(
                                        "test",
                                        "'test'",
                                        0,
                                        6
                                )
                        )
                )
        );
    }

    @Test
    void shouldAllowFunctionCallWithoutArguments() {
        FunctionCallFilterExpression function =
                new FunctionCallFilterExpression(
                        "currentFunction",
                        List.of()
                );

        assertAll(
                () -> assertEquals(
                        "currentFunction",
                        function.functionName()
                ),
                () -> assertTrue(
                        function.arguments().isEmpty()
                ),
                () -> assertTrue(
                        function.children().isEmpty()
                ),
                () -> assertEquals(
                        ExpressionType.UNKNOWN,
                        function.expressionType()
                ),
                () -> assertTrue(
                        function.sourceSpan().isUnknown()
                )
        );
    }

    @Test
    void shouldDefensivelyCopyFunctionArguments() {
        FilterExpression title =
                property(
                        "Title",
                        9,
                        14
                );

        FilterExpression search =
                stringLiteral(
                        "urgent",
                        "'urgent'",
                        16,
                        24
                );

        List<FilterExpression> source =
                new ArrayList<>(
                        List.of(
                                title,
                                search
                        )
                );

        FunctionCallFilterExpression function =
                new FunctionCallFilterExpression(
                        "contains",
                        source
                );

        source.clear();

        assertAll(
                () -> assertEquals(
                        List.of(
                                title,
                                search
                        ),
                        function.arguments()
                ),
                () -> assertSame(
                        function.arguments(),
                        function.children()
                ),
                () -> assertThrows(
                        UnsupportedOperationException.class,
                        () -> function.arguments().add(
                                booleanLiteral(
                                        true,
                                        25,
                                        29
                                )
                        )
                ),
                () -> assertThrows(
                        UnsupportedOperationException.class,
                        () -> function.children().clear()
                )
        );
    }

    @Test
    void shouldPreserveExplicitFunctionMetadata() {
        ExpressionMetadata metadata =
                metadata(
                        0,
                        26,
                        ExpressionType.BOOLEAN
                );

        FunctionCallFilterExpression function =
                new FunctionCallFilterExpression(
                        "contains",
                        List.of(
                                property(
                                        "Title",
                                        9,
                                        14
                                ),
                                stringLiteral(
                                        "urgent",
                                        "'urgent'",
                                        16,
                                        24
                                )
                        ),
                        metadata
                );

        assertAll(
                () -> assertSame(
                        metadata,
                        function.metadata()
                ),
                () -> assertEquals(
                        ExpressionType.BOOLEAN,
                        function.expressionType()
                ),
                () -> assertEquals(
                        new SourceSpan(0, 26),
                        function.sourceSpan()
                )
        );
    }

    @Test
    void shouldRejectNullFunctionName() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new FunctionCallFilterExpression(
                                null,
                                List.of(),
                                unresolvedMetadata(
                                        0,
                                        0
                                )
                        )
                );

        assertEquals(
                "Function name cannot be null",
                exception.getMessage()
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            " ",
            "\t",
            "\r\n"
    })
    void shouldRejectBlankFunctionName(
            String functionName
    ) {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new FunctionCallFilterExpression(
                                functionName,
                                List.of()
                        )
                );

        assertEquals(
                "Function name cannot be blank",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullFunctionArguments() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new FunctionCallFilterExpression(
                                "contains",
                                null,
                                unresolvedMetadata(
                                        0,
                                        0
                                )
                        )
                );

        assertEquals(
                "Function arguments cannot be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullFunctionArgumentElement() {
        List<FilterExpression> arguments =
                new ArrayList<>();

        arguments.add(
                property(
                        "Title",
                        9,
                        14
                )
        );

        arguments.add(null);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new FunctionCallFilterExpression(
                                "contains",
                                arguments
                        )
                );

        assertEquals(
                "Function arguments cannot contain null elements",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullFunctionMetadata() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new FunctionCallFilterExpression(
                                "contains",
                                List.of(),
                                null
                        )
                );

        assertEquals(
                "Expression metadata cannot be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldInitializeListExpressionAsCollection() {
        FilterExpression first =
                integerLiteral(
                        1,
                        "1",
                        0,
                        1
                );

        FilterExpression second =
                integerLiteral(
                        2,
                        "2",
                        3,
                        4
                );

        ListFilterExpression list =
                new ListFilterExpression(
                        List.of(
                                first,
                                second
                        )
                );

        assertAll(
                () -> assertEquals(
                        ExpressionType.COLLECTION,
                        list.expressionType()
                ),
                () -> assertTrue(
                        list.sourceSpan().isUnknown()
                ),
                () -> assertEquals(
                        List.of(
                                first,
                                second
                        ),
                        list.elements()
                ),
                () -> assertSame(
                        list.elements(),
                        list.children()
                )
        );
    }

    @Test
    void shouldDefensivelyCopyListElements() {
        FilterExpression first =
                integerLiteral(
                        1,
                        "1",
                        0,
                        1
                );

        FilterExpression second =
                integerLiteral(
                        2,
                        "2",
                        3,
                        4
                );

        List<FilterExpression> source =
                new ArrayList<>(
                        List.of(
                                first,
                                second
                        )
                );

        ListFilterExpression list =
                new ListFilterExpression(
                        source
                );

        source.clear();

        assertAll(
                () -> assertEquals(
                        List.of(
                                first,
                                second
                        ),
                        list.elements()
                ),
                () -> assertThrows(
                        UnsupportedOperationException.class,
                        () -> list.elements().clear()
                ),
                () -> assertThrows(
                        UnsupportedOperationException.class,
                        () -> list.children().add(
                                first
                        )
                )
        );
    }

    @Test
    void shouldPreserveExplicitListMetadata() {
        ExpressionMetadata metadata =
                metadata(
                        12,
                        21,
                        ExpressionType.COLLECTION
                );

        ListFilterExpression list =
                new ListFilterExpression(
                        List.of(
                                integerLiteral(
                                        1,
                                        "1",
                                        13,
                                        14
                                )
                        ),
                        metadata
                );

        assertAll(
                () -> assertSame(
                        metadata,
                        list.metadata()
                ),
                () -> assertEquals(
                        ExpressionType.COLLECTION,
                        list.expressionType()
                ),
                () -> assertEquals(
                        new SourceSpan(12, 21),
                        list.sourceSpan()
                )
        );
    }

    @Test
    void shouldRejectNullListElements() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new ListFilterExpression(
                                null,
                                metadata(
                                        0,
                                        2,
                                        ExpressionType.COLLECTION
                                )
                        )
                );

        assertEquals(
                "List elements cannot be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectEmptyListExpression() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new ListFilterExpression(
                                List.of()
                        )
                );

        assertEquals(
                "A list expression must contain at least one element",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullListElement() {
        List<FilterExpression> elements =
                new ArrayList<>();

        elements.add(
                integerLiteral(
                        1,
                        "1",
                        0,
                        1
                )
        );

        elements.add(null);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new ListFilterExpression(
                                elements
                        )
                );

        assertEquals(
                "List elements cannot contain null expressions",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullListMetadata() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new ListFilterExpression(
                                List.of(
                                        integerLiteral(
                                                1,
                                                "1",
                                                0,
                                                1
                                        )
                                ),
                                null
                        )
                );

        assertEquals(
                "Expression metadata cannot be null",
                exception.getMessage()
        );
    }

    @ParameterizedTest(name = "{0} uses keyword {2}")
    @MethodSource("binaryKeywordCases")
    void shouldExposeBinaryOperatorKeyword(
            String description,
            BinaryOperator operator,
            String expectedKeyword
    ) {
        assertEquals(
                expectedKeyword,
                operator.keyword()
        );
    }

    @Test
    void shouldExposeUnaryOperatorKeyword() {
        assertEquals(
                "not",
                UnaryOperator.NOT.keyword()
        );
    }

    @Test
    void shouldDispatchEveryExpressionToCorrectVisitorMethod() {
        RecordingVisitor visitor =
                new RecordingVisitor();

        FilterExpression literal =
                stringLiteral(
                        "urgent",
                        "'urgent'",
                        0,
                        8
                );

        FilterExpression property =
                property(
                        "Title",
                        0,
                        5
                );

        FilterExpression unary =
                new UnaryFilterExpression(
                        UnaryOperator.NOT,
                        booleanLiteral(
                                true,
                                4,
                                8
                        )
                );

        FilterExpression binary =
                new BinaryFilterExpression(
                        booleanLiteral(
                                true,
                                0,
                                4
                        ),
                        BinaryOperator.AND,
                        booleanLiteral(
                                false,
                                9,
                                14
                        )
                );

        FilterExpression function =
                new FunctionCallFilterExpression(
                        "contains",
                        List.of(
                                property,
                                literal
                        )
                );

        FilterExpression list =
                new ListFilterExpression(
                        List.of(
                                literal
                        )
                );

        assertAll(
                () -> assertEquals(
                        "literal",
                        literal.accept(visitor)
                ),
                () -> assertEquals(
                        "property",
                        property.accept(visitor)
                ),
                () -> assertEquals(
                        "unary",
                        unary.accept(visitor)
                ),
                () -> assertEquals(
                        "binary",
                        binary.accept(visitor)
                ),
                () -> assertEquals(
                        "function",
                        function.accept(visitor)
                ),
                () -> assertEquals(
                        "list",
                        list.accept(visitor)
                )
        );
    }

    @Test
    void shouldRejectNullVisitorForEveryExpressionType() {
        List<FilterExpression> expressions =
                List.of(
                        property(
                                "Title",
                                0,
                                5
                        ),
                        stringLiteral(
                                "urgent",
                                "'urgent'",
                                0,
                                8
                        ),
                        new UnaryFilterExpression(
                                UnaryOperator.NOT,
                                booleanLiteral(
                                        true,
                                        0,
                                        4
                                )
                        ),
                        new BinaryFilterExpression(
                                booleanLiteral(
                                        true,
                                        0,
                                        4
                                ),
                                BinaryOperator.AND,
                                booleanLiteral(
                                        false,
                                        9,
                                        14
                                )
                        ),
                        new FunctionCallFilterExpression(
                                "contains",
                                List.of()
                        ),
                        new ListFilterExpression(
                                List.of(
                                        integerLiteral(
                                                1,
                                                "1",
                                                0,
                                                1
                                        )
                                )
                        )
                );

        for (FilterExpression expression : expressions) {
            NullPointerException exception =
                    assertThrows(
                            NullPointerException.class,
                            () -> expression.accept(null)
                    );

            assertEquals(
                    "Expression visitor cannot be null",
                    exception.getMessage()
            );
        }
    }

    private static PropertyFilterExpression property(
            String path,
            int start,
            int end
    ) {
        return new PropertyFilterExpression(
                List.of(path),
                unresolvedMetadata(
                        start,
                        end
                )
        );
    }

    private static LiteralFilterExpression stringLiteral(
            String value,
            String rawText,
            int start,
            int end
    ) {
        return new LiteralFilterExpression(
                LiteralType.STRING,
                value,
                rawText,
                metadata(
                        start,
                        end,
                        ExpressionType.STRING
                )
        );
    }

    private static LiteralFilterExpression integerLiteral(
            long value,
            String rawText,
            int start,
            int end
    ) {
        return new LiteralFilterExpression(
                LiteralType.INTEGER,
                BigInteger.valueOf(value),
                rawText,
                metadata(
                        start,
                        end,
                        ExpressionType.INTEGER
                )
        );
    }

    private static LiteralFilterExpression decimalLiteral(
            String rawText,
            int start,
            int end
    ) {
        return new LiteralFilterExpression(
                LiteralType.DECIMAL,
                new BigDecimal(rawText),
                rawText,
                metadata(
                        start,
                        end,
                        ExpressionType.DECIMAL
                )
        );
    }

    private static LiteralFilterExpression booleanLiteral(
            boolean value,
            int start,
            int end
    ) {
        return new LiteralFilterExpression(
                LiteralType.BOOLEAN,
                value,
                Boolean.toString(value),
                metadata(
                        start,
                        end,
                        ExpressionType.BOOLEAN
                )
        );
    }

    private static ExpressionMetadata unresolvedMetadata(
            int start,
            int end
    ) {
        return metadata(
                start,
                end,
                ExpressionType.UNKNOWN
        );
    }

    private static ExpressionMetadata metadata(
            int start,
            int end,
            ExpressionType expressionType
    ) {
        return new ExpressionMetadata(
                new SourceSpan(
                        start,
                        end
                ),
                expressionType
        );
    }

    private static Stream<Arguments>
    booleanBinaryOperatorCases() {
        return Stream.of(
                Arguments.of(
                        "logical AND",
                        BinaryOperator.AND
                ),
                Arguments.of(
                        "logical OR",
                        BinaryOperator.OR
                ),
                Arguments.of(
                        "equality",
                        BinaryOperator.EQ
                ),
                Arguments.of(
                        "inequality",
                        BinaryOperator.NE
                ),
                Arguments.of(
                        "greater than",
                        BinaryOperator.GT
                ),
                Arguments.of(
                        "greater than or equal",
                        BinaryOperator.GE
                ),
                Arguments.of(
                        "less than",
                        BinaryOperator.LT
                ),
                Arguments.of(
                        "less than or equal",
                        BinaryOperator.LE
                ),
                Arguments.of(
                        "membership",
                        BinaryOperator.IN
                )
        );
    }

    private static Stream<Arguments>
    arithmeticBinaryOperatorCases() {
        return Stream.of(
                Arguments.of(
                        "addition",
                        BinaryOperator.ADD
                ),
                Arguments.of(
                        "subtraction",
                        BinaryOperator.SUB
                ),
                Arguments.of(
                        "multiplication",
                        BinaryOperator.MUL
                ),
                Arguments.of(
                        "division",
                        BinaryOperator.DIV
                ),
                Arguments.of(
                        "modulo",
                        BinaryOperator.MOD
                )
        );
    }

    private static Stream<Arguments> validLiteralCases() {
        return Stream.of(
                Arguments.of(
                        "string literal",
                        LiteralType.STRING,
                        "urgent",
                        "'urgent'",
                        ExpressionType.STRING
                ),
                Arguments.of(
                        "integer literal",
                        LiteralType.INTEGER,
                        new BigInteger("123"),
                        "123",
                        ExpressionType.INTEGER
                ),
                Arguments.of(
                        "decimal literal",
                        LiteralType.DECIMAL,
                        new BigDecimal("123.50"),
                        "123.50",
                        ExpressionType.DECIMAL
                ),
                Arguments.of(
                        "boolean literal",
                        LiteralType.BOOLEAN,
                        true,
                        "true",
                        ExpressionType.BOOLEAN
                ),
                Arguments.of(
                        "null literal",
                        LiteralType.NULL,
                        null,
                        "null",
                        ExpressionType.NULL
                )
        );
    }

    private static Stream<Arguments>
    invalidLiteralValueCases() {
        return Stream.of(
                Arguments.of(
                        "string literal with integer value",
                        LiteralType.STRING,
                        BigInteger.ONE,
                        "'1'"
                ),
                Arguments.of(
                        "string literal with null value",
                        LiteralType.STRING,
                        null,
                        "null"
                ),
                Arguments.of(
                        "integer literal with Java Integer value",
                        LiteralType.INTEGER,
                        1,
                        "1"
                ),
                Arguments.of(
                        "integer literal with decimal value",
                        LiteralType.INTEGER,
                        BigDecimal.ONE,
                        "1"
                ),
                Arguments.of(
                        "decimal literal with integer value",
                        LiteralType.DECIMAL,
                        BigInteger.ONE,
                        "1"
                ),
                Arguments.of(
                        "boolean literal with string value",
                        LiteralType.BOOLEAN,
                        "true",
                        "true"
                ),
                Arguments.of(
                        "null literal with non-null value",
                        LiteralType.NULL,
                        "null",
                        "null"
                )
        );
    }

    private static Stream<Arguments> binaryKeywordCases() {
        return Stream.of(
                Arguments.of(
                        "logical AND",
                        BinaryOperator.AND,
                        "and"
                ),
                Arguments.of(
                        "logical OR",
                        BinaryOperator.OR,
                        "or"
                ),
                Arguments.of(
                        "equality",
                        BinaryOperator.EQ,
                        "eq"
                ),
                Arguments.of(
                        "inequality",
                        BinaryOperator.NE,
                        "ne"
                ),
                Arguments.of(
                        "greater than",
                        BinaryOperator.GT,
                        "gt"
                ),
                Arguments.of(
                        "greater than or equal",
                        BinaryOperator.GE,
                        "ge"
                ),
                Arguments.of(
                        "less than",
                        BinaryOperator.LT,
                        "lt"
                ),
                Arguments.of(
                        "less than or equal",
                        BinaryOperator.LE,
                        "le"
                ),
                Arguments.of(
                        "membership",
                        BinaryOperator.IN,
                        "in"
                ),
                Arguments.of(
                        "addition",
                        BinaryOperator.ADD,
                        "add"
                ),
                Arguments.of(
                        "subtraction",
                        BinaryOperator.SUB,
                        "sub"
                ),
                Arguments.of(
                        "multiplication",
                        BinaryOperator.MUL,
                        "mul"
                ),
                Arguments.of(
                        "division",
                        BinaryOperator.DIV,
                        "div"
                ),
                Arguments.of(
                        "modulo",
                        BinaryOperator.MOD,
                        "mod"
                )
        );
    }

    private static final class RecordingVisitor
            implements FilterExpressionVisitor<String> {

        @Override
        public String visitBinaryExpression(
                BinaryFilterExpression expression
        ) {
            return "binary";
        }

        @Override
        public String visitUnaryExpression(
                UnaryFilterExpression expression
        ) {
            return "unary";
        }

        @Override
        public String visitLiteralExpression(
                LiteralFilterExpression expression
        ) {
            return "literal";
        }

        @Override
        public String visitPropertyExpression(
                PropertyFilterExpression expression
        ) {
            return "property";
        }

        @Override
        public String visitFunctionCallExpression(
                FunctionCallFilterExpression expression
        ) {
            return "function";
        }

        @Override
        public String visitListExpression(
                ListFilterExpression expression
        ) {
            return "list";
        }
    }
}