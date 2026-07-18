package io.github.davidtodorov.odataparser.filter.semantic;

import io.github.davidtodorov.odataparser.common.type.ExpressionType;
import io.github.davidtodorov.odataparser.filter.ast.BinaryFilterExpression;
import io.github.davidtodorov.odataparser.filter.ast.BinaryOperator;
import io.github.davidtodorov.odataparser.filter.ast.FilterExpression;
import io.github.davidtodorov.odataparser.filter.ast.FunctionCallFilterExpression;
import io.github.davidtodorov.odataparser.filter.ast.ListFilterExpression;
import io.github.davidtodorov.odataparser.filter.ast.LiteralFilterExpression;
import io.github.davidtodorov.odataparser.filter.ast.PropertyFilterExpression;
import io.github.davidtodorov.odataparser.filter.ast.UnaryFilterExpression;
import io.github.davidtodorov.odataparser.filter.parser.FilterParser;
import io.github.davidtodorov.odataparser.meta.MetadataRegistry;
import io.github.davidtodorov.odataparser.meta.NavigationJoinType;
import io.github.davidtodorov.odataparser.meta.demo.CaseMetadata;
import io.github.davidtodorov.odataparser.meta.demo.DepartmentMetadata;
import io.github.davidtodorov.odataparser.meta.demo.UserMetadata;
import io.github.davidtodorov.odataparser.meta.path.ResolvedMetadataPath;
import io.github.davidtodorov.odataparser.meta.path.ResolvedMetadataPathSegment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilterExpressionResolverTest {

    private CaseMetadata caseMetadata;
    private UserMetadata userMetadata;
    private DepartmentMetadata departmentMetadata;
    private FilterExpressionResolver resolver;

    @BeforeEach
    void setUp() {
        departmentMetadata =
                new DepartmentMetadata();

        userMetadata =
                new UserMetadata();

        caseMetadata =
                new CaseMetadata();

        MetadataRegistry.of(
                caseMetadata,
                userMetadata,
                departmentMetadata
        );

        resolver =
                new FilterExpressionResolver(
                        caseMetadata
                );
    }

    @Test
    void shouldRejectNullRootMetadata() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new FilterExpressionResolver(null)
                );

        assertEquals(
                "Root entity metadata cannot be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullRootExpression() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> resolver.resolve(null)
                );

        assertEquals(
                "Root expression cannot be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldResolveDirectBooleanProperty() {
        PropertyFilterExpression property =
                assertProperty(
                        resolve("Deleted")
                );

        ResolvedMetadataPath path =
                property.resolvedPath()
                        .orElseThrow();

        assertAll(
                () -> assertTrue(
                        property.isResolved()
                ),
                () -> assertEquals(
                        ExpressionType.BOOLEAN,
                        property.expressionType()
                ),
                () -> assertEquals(
                        "Deleted",
                        property.path()
                ),
                () -> assertEquals(
                        "Deleted",
                        path.externalPath()
                ),
                () -> assertEquals(
                        "deleted",
                        path.mappedPath()
                ),
                () -> assertEquals(
                        List.of("Deleted"),
                        path.externalSegments()
                ),
                () -> assertEquals(
                        List.of("deleted"),
                        path.mappedSegments()
                ),
                () -> assertEquals(
                        ExpressionType.BOOLEAN,
                        path.expressionType()
                ),
                () -> assertEquals(
                        Boolean.class,
                        path.javaType()
                ),
                () -> assertEquals(
                        1,
                        path.size()
                ),
                () -> assertFalse(
                        path.containsNavigation()
                ),
                () -> assertSame(
                        caseMetadata,
                        path.rootMetadata()
                ),
                () -> assertSame(
                        caseMetadata.requireProperty("Deleted"),
                        path.leaf().propertyMetadata()
                )
        );
    }

    @Test
    void shouldResolveDirectStringPropertyInsideComparison() {
        BinaryFilterExpression comparison =
                assertBinary(
                        resolve("Title eq 'urgent'"),
                        BinaryOperator.EQ
                );

        PropertyFilterExpression property =
                assertProperty(
                        comparison.left()
                );

        ResolvedMetadataPath path =
                property.resolvedPath()
                        .orElseThrow();

        assertAll(
                () -> assertEquals(
                        ExpressionType.BOOLEAN,
                        comparison.expressionType()
                ),
                () -> assertEquals(
                        ExpressionType.STRING,
                        property.expressionType()
                ),
                () -> assertEquals(
                        "Title",
                        path.externalPath()
                ),
                () -> assertEquals(
                        "title",
                        path.mappedPath()
                ),
                () -> assertEquals(
                        String.class,
                        path.javaType()
                ),
                () -> assertSame(
                        caseMetadata.requireProperty("Title"),
                        path.leaf().propertyMetadata()
                )
        );
    }

    @Test
    void shouldResolveDeepNavigationPropertyPath() {
        PropertyFilterExpression property =
                assertProperty(
                        resolve(
                                "Owner/Department/Enabled"
                        )
                );

        ResolvedMetadataPath path =
                property.resolvedPath()
                        .orElseThrow();

        assertAll(
                () -> assertEquals(
                        ExpressionType.BOOLEAN,
                        property.expressionType()
                ),
                () -> assertEquals(
                        "Owner/Department/Enabled",
                        path.externalPath()
                ),
                () -> assertEquals(
                        "owner/department/enabled",
                        path.mappedPath()
                ),
                () -> assertEquals(
                        List.of(
                                "Owner",
                                "Department",
                                "Enabled"
                        ),
                        path.externalSegments()
                ),
                () -> assertEquals(
                        List.of(
                                "owner",
                                "department",
                                "enabled"
                        ),
                        path.mappedSegments()
                ),
                () -> assertEquals(
                        3,
                        path.size()
                ),
                () -> assertTrue(
                        path.containsNavigation()
                ),
                () -> assertEquals(
                        Boolean.class,
                        path.javaType()
                )
        );

        ResolvedMetadataPathSegment owner =
                path.segments().get(0);

        ResolvedMetadataPathSegment department =
                path.segments().get(1);

        ResolvedMetadataPathSegment enabled =
                path.segments().get(2);

        assertAll(
                () -> assertSame(
                        caseMetadata,
                        owner.declaringMetadata()
                ),
                () -> assertSame(
                        caseMetadata.requireProperty("Owner"),
                        owner.propertyMetadata()
                ),
                () -> assertTrue(
                        owner.isNavigation()
                ),
                () -> assertTrue(
                        owner.isSingle()
                ),
                () -> assertFalse(
                        owner.isCollection()
                ),
                () -> assertSame(
                        userMetadata,
                        owner.targetMetadata().orElseThrow()
                ),
                () -> assertEquals(
                        NavigationJoinType.LEFT,
                        owner.defaultJoinType().orElseThrow()
                ),
                () -> assertTrue(
                        owner.isJoinTypeOverridable()
                ),
                () -> assertFalse(
                        owner.isJoinTypeFixed()
                )
        );

        assertAll(
                () -> assertSame(
                        userMetadata,
                        department.declaringMetadata()
                ),
                () -> assertSame(
                        userMetadata.requireProperty("Department"),
                        department.propertyMetadata()
                ),
                () -> assertTrue(
                        department.isNavigation()
                ),
                () -> assertSame(
                        departmentMetadata,
                        department.targetMetadata().orElseThrow()
                ),
                () -> assertEquals(
                        NavigationJoinType.LEFT,
                        department.defaultJoinType().orElseThrow()
                ),
                () -> assertTrue(
                        department.isJoinTypeOverridable()
                )
        );

        assertAll(
                () -> assertSame(
                        departmentMetadata,
                        enabled.declaringMetadata()
                ),
                () -> assertSame(
                        departmentMetadata.requireProperty("Enabled"),
                        enabled.propertyMetadata()
                ),
                () -> assertTrue(
                        enabled.isPrimitive()
                ),
                () -> assertFalse(
                        enabled.isNavigation()
                ),
                () -> assertEquals(
                        ExpressionType.BOOLEAN,
                        enabled.expressionType().orElseThrow()
                ),
                () -> assertTrue(
                        enabled.targetMetadata().isEmpty()
                ),
                () -> assertTrue(
                        enabled.joinPolicy().isEmpty()
                )
        );
    }

    @Test
    void shouldReuseExactMetadataInstancesAcrossDifferentPaths() {
        BinaryFilterExpression expression =
                assertBinary(
                        resolve(
                                "Owner/Department/Enabled "
                                        + "and Department/Enabled"
                        ),
                        BinaryOperator.AND
                );

        PropertyFilterExpression ownerDepartmentEnabled =
                assertProperty(
                        expression.left()
                );

        PropertyFilterExpression departmentEnabled =
                assertProperty(
                        expression.right()
                );

        ResolvedMetadataPath firstPath =
                ownerDepartmentEnabled
                        .resolvedPath()
                        .orElseThrow();

        ResolvedMetadataPath secondPath =
                departmentEnabled
                        .resolvedPath()
                        .orElseThrow();

        assertSame(
                departmentMetadata,
                firstPath.segments()
                        .get(1)
                        .targetMetadata()
                        .orElseThrow()
        );

        assertSame(
                departmentMetadata,
                secondPath.segments()
                        .getFirst()
                        .targetMetadata()
                        .orElseThrow()
        );

        assertSame(
                firstPath.leaf().propertyMetadata(),
                secondPath.leaf().propertyMetadata()
        );
    }

    @Test
    void shouldReturnNewResolvedTreeWithoutMutatingParsedTree() {
        FilterExpression parsed =
                parse(
                        "Title eq 'urgent' "
                                + "and Deleted eq false"
                );

        BinaryFilterExpression parsedRoot =
                assertBinary(
                        parsed,
                        BinaryOperator.AND
                );

        BinaryFilterExpression parsedLeftComparison =
                assertBinary(
                        parsedRoot.left(),
                        BinaryOperator.EQ
                );

        PropertyFilterExpression parsedTitle =
                assertProperty(
                        parsedLeftComparison.left()
                );

        assertFalse(
                parsedTitle.isResolved()
        );

        FilterExpression resolved =
                resolver.resolve(parsed);

        BinaryFilterExpression resolvedRoot =
                assertBinary(
                        resolved,
                        BinaryOperator.AND
                );

        BinaryFilterExpression resolvedLeftComparison =
                assertBinary(
                        resolvedRoot.left(),
                        BinaryOperator.EQ
                );

        PropertyFilterExpression resolvedTitle =
                assertProperty(
                        resolvedLeftComparison.left()
                );

        assertAll(
                () -> assertNotSame(
                        parsed,
                        resolved
                ),
                () -> assertNotSame(
                        parsedRoot,
                        resolvedRoot
                ),
                () -> assertNotSame(
                        parsedTitle,
                        resolvedTitle
                ),
                () -> assertFalse(
                        parsedTitle.isResolved()
                ),
                () -> assertTrue(
                        resolvedTitle.isResolved()
                ),
                () -> assertEquals(
                        ExpressionType.STRING,
                        resolvedTitle.expressionType()
                )
        );
    }

    @Test
    void shouldPreserveSourceSpansWhileRebuildingTree() {
        FilterExpression parsed =
                parse(
                        "  contains(Title, 'urgent') "
                                + "and Amount gt 10.50  "
                );

        FilterExpression resolved =
                resolver.resolve(parsed);

        assertSourceSpansPreserved(
                parsed,
                resolved
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("literalResolutionCases")
    void shouldResolveLiteralType(
            String description,
            String input,
            ExpressionType expectedType
    ) {
        BinaryFilterExpression comparison =
                assertBinary(
                        resolve(input),
                        BinaryOperator.EQ
                );

        LiteralFilterExpression literal =
                assertLiteral(
                        comparison.right()
                );

        assertEquals(
                expectedType,
                literal.expressionType()
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validComparisonCases")
    void shouldResolveCompatibleComparison(
            String description,
            String input,
            BinaryOperator expectedOperator
    ) {
        BinaryFilterExpression comparison =
                assertBinary(
                        resolve(input),
                        expectedOperator
                );

        assertEquals(
                ExpressionType.BOOLEAN,
                comparison.expressionType()
        );

        assertFullyResolved(
                comparison
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("integerArithmeticCases")
    void shouldResolveIntegerArithmeticExpression(
            String description,
            String input,
            BinaryOperator expectedArithmeticOperator
    ) {
        BinaryFilterExpression comparison =
                assertBinary(
                        resolve(input),
                        BinaryOperator.EQ
                );

        BinaryFilterExpression arithmetic =
                assertBinary(
                        comparison.left(),
                        expectedArithmeticOperator
                );

        assertAll(
                () -> assertEquals(
                        ExpressionType.INTEGER,
                        arithmetic.expressionType()
                ),
                () -> assertEquals(
                        ExpressionType.BOOLEAN,
                        comparison.expressionType()
                )
        );
    }

    @Test
    void shouldPromoteIntegerAndDecimalArithmeticToDecimal() {
        BinaryFilterExpression comparison =
                assertBinary(
                        resolve(
                                "Priority add 2.50 eq 3.50"
                        ),
                        BinaryOperator.EQ
                );

        BinaryFilterExpression addition =
                assertBinary(
                        comparison.left(),
                        BinaryOperator.ADD
                );

        assertEquals(
                ExpressionType.DECIMAL,
                addition.expressionType()
        );
    }

    @Test
    void shouldKeepDecimalArithmeticAsDecimal() {
        BinaryFilterExpression comparison =
                assertBinary(
                        resolve(
                                "Amount mul 2 eq 10"
                        ),
                        BinaryOperator.EQ
                );

        BinaryFilterExpression multiplication =
                assertBinary(
                        comparison.left(),
                        BinaryOperator.MUL
                );

        assertEquals(
                ExpressionType.DECIMAL,
                multiplication.expressionType()
        );
    }

    @Test
    void shouldResolveLogicalAndExpression() {
        BinaryFilterExpression expression =
                assertBinary(
                        resolve(
                                "Deleted and Closed"
                        ),
                        BinaryOperator.AND
                );

        assertAll(
                () -> assertEquals(
                        ExpressionType.BOOLEAN,
                        expression.expressionType()
                ),
                () -> assertEquals(
                        ExpressionType.BOOLEAN,
                        expression.left().expressionType()
                ),
                () -> assertEquals(
                        ExpressionType.BOOLEAN,
                        expression.right().expressionType()
                )
        );
    }

    @Test
    void shouldResolveLogicalOrExpression() {
        BinaryFilterExpression expression =
                assertBinary(
                        resolve(
                                "Deleted or Closed"
                        ),
                        BinaryOperator.OR
                );

        assertEquals(
                ExpressionType.BOOLEAN,
                expression.expressionType()
        );
    }

    @Test
    void shouldResolveUnaryNotExpression() {
        UnaryFilterExpression expression =
                assertUnary(
                        resolve(
                                "not Deleted"
                        )
                );

        assertAll(
                () -> assertEquals(
                        ExpressionType.BOOLEAN,
                        expression.expressionType()
                ),
                () -> assertEquals(
                        ExpressionType.BOOLEAN,
                        expression.operand().expressionType()
                ),
                () -> assertTrue(
                        assertProperty(
                                expression.operand()
                        ).isResolved()
                )
        );
    }

    @Test
    void shouldResolveRepeatedUnaryNotExpressions() {
        UnaryFilterExpression outer =
                assertUnary(
                        resolve(
                                "not not Deleted"
                        )
                );

        UnaryFilterExpression inner =
                assertUnary(
                        outer.operand()
                );

        assertAll(
                () -> assertEquals(
                        ExpressionType.BOOLEAN,
                        outer.expressionType()
                ),
                () -> assertEquals(
                        ExpressionType.BOOLEAN,
                        inner.expressionType()
                ),
                () -> assertEquals(
                        ExpressionType.BOOLEAN,
                        inner.operand().expressionType()
                )
        );
    }

    @Test
    void shouldResolveIntegerInList() {
        BinaryFilterExpression expression =
                assertBinary(
                        resolve(
                                "Priority in (1, 2, 3)"
                        ),
                        BinaryOperator.IN
                );

        ListFilterExpression list =
                assertList(
                        expression.right()
                );

        assertAll(
                () -> assertEquals(
                        ExpressionType.BOOLEAN,
                        expression.expressionType()
                ),
                () -> assertEquals(
                        ExpressionType.COLLECTION,
                        list.expressionType()
                ),
                () -> assertEquals(
                        3,
                        list.elements().size()
                )
        );

        for (FilterExpression element
                : list.elements()) {

            assertEquals(
                    ExpressionType.INTEGER,
                    element.expressionType()
            );
        }
    }

    @Test
    void shouldAllowNumericPromotionInsideInList() {
        BinaryFilterExpression expression =
                assertBinary(
                        resolve(
                                "Amount in (1, 2.50, null)"
                        ),
                        BinaryOperator.IN
                );

        ListFilterExpression list =
                assertList(
                        expression.right()
                );

        assertEquals(
                List.of(
                        ExpressionType.INTEGER,
                        ExpressionType.DECIMAL,
                        ExpressionType.NULL
                ),
                list.elements()
                        .stream()
                        .map(
                                FilterExpression::expressionType
                        )
                        .toList()
        );
    }

    @Test
    void shouldAllowNullInsideStringInList() {
        BinaryFilterExpression expression =
                assertBinary(
                        resolve(
                                "Title in ('OPEN', null, 'CLOSED')"
                        ),
                        BinaryOperator.IN
                );

        ListFilterExpression list =
                assertList(
                        expression.right()
                );

        assertEquals(
                List.of(
                        ExpressionType.STRING,
                        ExpressionType.NULL,
                        ExpressionType.STRING
                ),
                list.elements()
                        .stream()
                        .map(
                                FilterExpression::expressionType
                        )
                        .toList()
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("booleanFunctionCases")
    void shouldResolveBooleanStringFunction(
            String description,
            String input,
            String expectedFunctionName
    ) {
        FunctionCallFilterExpression function =
                assertFunction(
                        resolve(input)
                );

        assertAll(
                () -> assertEquals(
                        expectedFunctionName,
                        function.functionName()
                ),
                () -> assertEquals(
                        ExpressionType.BOOLEAN,
                        function.expressionType()
                ),
                () -> assertEquals(
                        2,
                        function.arguments().size()
                ),
                () -> assertEquals(
                        ExpressionType.STRING,
                        function.arguments()
                                .get(0)
                                .expressionType()
                ),
                () -> assertEquals(
                        ExpressionType.STRING,
                        function.arguments()
                                .get(1)
                                .expressionType()
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("stringFunctionCases")
    void shouldResolveStringReturningFunction(
            String description,
            String input,
            String expectedFunctionName
    ) {
        BinaryFilterExpression comparison =
                assertBinary(
                        resolve(input),
                        BinaryOperator.EQ
                );

        FunctionCallFilterExpression function =
                assertFunction(
                        comparison.left()
                );

        assertAll(
                () -> assertEquals(
                        expectedFunctionName,
                        function.functionName()
                ),
                () -> assertEquals(
                        ExpressionType.STRING,
                        function.expressionType()
                ),
                () -> assertEquals(
                        1,
                        function.arguments().size()
                ),
                () -> assertEquals(
                        ExpressionType.STRING,
                        function.arguments()
                                .getFirst()
                                .expressionType()
                )
        );
    }

    @Test
    void shouldResolveLengthFunctionAsInteger() {
        BinaryFilterExpression comparison =
                assertBinary(
                        resolve(
                                "length(Title) gt 3"
                        ),
                        BinaryOperator.GT
                );

        FunctionCallFilterExpression length =
                assertFunction(
                        comparison.left()
                );

        assertAll(
                () -> assertEquals(
                        "length",
                        length.functionName()
                ),
                () -> assertEquals(
                        ExpressionType.INTEGER,
                        length.expressionType()
                ),
                () -> assertEquals(
                        ExpressionType.STRING,
                        length.arguments()
                                .getFirst()
                                .expressionType()
                ),
                () -> assertEquals(
                        ExpressionType.BOOLEAN,
                        comparison.expressionType()
                )
        );
    }

    @Test
    void shouldResolveNestedFunctionsFromInsideOut() {
        FunctionCallFilterExpression contains =
                assertFunction(
                        resolve(
                                "contains("
                                        + "tolower(trim(Title)),"
                                        + "'urgent'"
                                        + ")"
                        )
                );

        FunctionCallFilterExpression toLower =
                assertFunction(
                        contains.arguments().getFirst()
                );

        FunctionCallFilterExpression trim =
                assertFunction(
                        toLower.arguments().getFirst()
                );

        PropertyFilterExpression title =
                assertProperty(
                        trim.arguments().getFirst()
                );

        assertAll(
                () -> assertEquals(
                        ExpressionType.BOOLEAN,
                        contains.expressionType()
                ),
                () -> assertEquals(
                        ExpressionType.STRING,
                        toLower.expressionType()
                ),
                () -> assertEquals(
                        ExpressionType.STRING,
                        trim.expressionType()
                ),
                () -> assertEquals(
                        ExpressionType.STRING,
                        title.expressionType()
                ),
                () -> assertTrue(
                        title.isResolved()
                )
        );
    }

    @Test
    void shouldResolveFunctionNamesCaseInsensitively() {
        FunctionCallFilterExpression function =
                assertFunction(
                        resolve(
                                "CONTAINS(Title, 'urgent')"
                        )
                );

        assertAll(
                () -> assertEquals(
                        "CONTAINS",
                        function.functionName()
                ),
                () -> assertEquals(
                        ExpressionType.BOOLEAN,
                        function.expressionType()
                )
        );
    }

    @Test
    void shouldResolveCompleteRealisticExpression() {
        String input =
                "contains(Title, 'urgent') "
                        + "and Amount gt 100.50 "
                        + "and Deleted eq false "
                        + "and Owner/Active eq true "
                        + "and Owner/Department/Code eq 'ENG' "
                        + "and Reviewer/Email eq 'reviewer@example.com' "
                        + "and Department/Enabled eq true";

        FilterExpression resolved =
                resolve(input);

        assertAll(
                () -> assertEquals(
                        ExpressionType.BOOLEAN,
                        resolved.expressionType()
                ),
                () -> assertEquals(
                        0,
                        resolved.sourceSpan().start()
                ),
                () -> assertEquals(
                        input.length(),
                        resolved.sourceSpan().end()
                )
        );

        assertFullyResolved(
                resolved
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("nonBooleanRootCases")
    void shouldRejectNonBooleanRootExpression(
            String description,
            String input,
            ExpressionType expectedType
    ) {
        FilterSemanticException exception =
                assertThrows(
                        FilterSemanticException.class,
                        () -> resolve(input)
                );

        assertAll(
                () -> assertEquals(
                        "The complete filter expression must return BOOLEAN, "
                                + "but returned "
                                + expectedType
                                + " at source range [0, "
                                + input.length()
                                + ").",
                        exception.getMessage()
                ),
                () -> assertEquals(
                        0,
                        exception.position()
                ),
                () -> assertEquals(
                        0,
                        exception.sourceSpan().start()
                ),
                () -> assertEquals(
                        input.length(),
                        exception.sourceSpan().end()
                ),
                () -> assertNull(
                        exception.getCause()
                )
        );
    }

    @Test
    void shouldRejectStringAsLeftLogicalOperand() {
        assertSemanticFailure(
                "Title and Deleted",
                "Left operand of 'and' must be BOOLEAN, but was STRING",
                0,
                5
        );
    }

    @Test
    void shouldRejectStringAsRightLogicalOperand() {
        assertSemanticFailure(
                "Deleted and Title",
                "Right operand of 'and' must be BOOLEAN, but was STRING",
                12,
                17
        );
    }

    @Test
    void shouldRejectNonBooleanUnaryOperand() {
        assertSemanticFailure(
                "not Title",
                "Operator 'not' requires a BOOLEAN operand, but was STRING",
                4,
                9
        );
    }

    @Test
    void shouldRejectNonNumericLeftArithmeticOperand() {
        assertSemanticFailure(
                "Title add 1 eq 2",
                "Left operand of arithmetic operator 'add' "
                        + "must be numeric, but was STRING",
                0,
                5
        );
    }

    @Test
    void shouldRejectNonNumericRightArithmeticOperand() {
        assertSemanticFailure(
                "Priority add Title eq 1",
                "Right operand of arithmetic operator 'add' "
                        + "must be numeric, but was STRING",
                13,
                18
        );
    }

    @Test
    void shouldRejectIncompatibleEqualityComparison() {
        FilterSemanticException exception =
                assertSemanticFailure(
                        "Title eq 1",
                        "Operator 'eq' cannot compare STRING with INTEGER",
                        0,
                        10
                );

        assertNull(
                exception.getCause()
        );
    }

    @Test
    void shouldRejectBooleanOrderedComparison() {
        assertSemanticFailure(
                "Deleted gt Closed",
                "Operator 'gt' cannot order-compare "
                        + "BOOLEAN with BOOLEAN",
                0,
                17
        );
    }

    @Test
    void shouldRejectOrderedComparisonWithNull() {
        assertSemanticFailure(
                "Title gt null",
                "Operator 'gt' cannot order-compare "
                        + "STRING with NULL",
                0,
                13
        );
    }

    @Test
    void shouldRejectIncompatibleInListElement() {
        assertSemanticFailure(
                "Priority in (1, 'two', 3)",
                "List element of type STRING is not compatible "
                        + "with left operand type INTEGER",
                16,
                21
        );
    }

    @Test
    void shouldRejectUnsupportedFunction() {
        assertSemanticFailure(
                "unknown(Title)",
                "Unsupported function 'unknown'",
                0,
                14
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidFunctionArgumentCountCases")
    void shouldRejectInvalidFunctionArgumentCount(
            String description,
            String input,
            String expectedMessage
    ) {
        assertSemanticFailure(
                input,
                expectedMessage,
                0,
                input.length()
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidStringArgumentCases")
    void shouldRejectNonStringFunctionArgument(
            String description,
            String input,
            String expectedMessage,
            int expectedStart,
            int expectedEnd
    ) {
        assertSemanticFailure(
                input,
                expectedMessage,
                expectedStart,
                expectedEnd
        );
    }

    @Test
    void shouldRejectUnknownRootProperty() {
        FilterSemanticException exception =
                assertThrows(
                        FilterSemanticException.class,
                        () -> resolve(
                                "UnknownProperty eq 'test'"
                        )
                );

        assertAll(
                () -> assertTrue(
                        exception.getMessage()
                                .contains("UnknownProperty")
                ),
                () -> assertEquals(
                        0,
                        exception.sourceSpan().start()
                ),
                () -> assertEquals(
                        15,
                        exception.sourceSpan().end()
                ),
                () -> assertInstanceOf(
                        IllegalArgumentException.class,
                        exception.getCause()
                )
        );
    }

    @Test
    void shouldRejectUnknownNestedProperty() {
        FilterSemanticException exception =
                assertThrows(
                        FilterSemanticException.class,
                        () -> resolve(
                                "Owner/Unknown eq 'test'"
                        )
                );

        assertAll(
                () -> assertTrue(
                        exception.getMessage()
                                .contains("Unknown")
                ),
                () -> assertEquals(
                        0,
                        exception.sourceSpan().start()
                ),
                () -> assertEquals(
                        13,
                        exception.sourceSpan().end()
                ),
                () -> assertInstanceOf(
                        IllegalArgumentException.class,
                        exception.getCause()
                )
        );
    }

    @Test
    void shouldRejectTraversalThroughPrimitiveProperty() {
        FilterSemanticException exception =
                assertThrows(
                        FilterSemanticException.class,
                        () -> resolve(
                                "Title/Value eq 'test'"
                        )
                );

        assertAll(
                () -> assertTrue(
                        exception.getMessage()
                                .contains("Title")
                ),
                () -> assertEquals(
                        0,
                        exception.sourceSpan().start()
                ),
                () -> assertEquals(
                        11,
                        exception.sourceSpan().end()
                ),
                () -> assertInstanceOf(
                        IllegalArgumentException.class,
                        exception.getCause()
                )
        );
    }

    @Test
    void shouldRejectNavigationPropertyAsFinalScalarSegment() {
        FilterSemanticException exception =
                assertThrows(
                        FilterSemanticException.class,
                        () -> resolve(
                                "Owner eq null"
                        )
                );

        assertAll(
                () -> assertTrue(
                        exception.getMessage()
                                .contains("Owner")
                ),
                () -> assertEquals(
                        0,
                        exception.sourceSpan().start()
                ),
                () -> assertEquals(
                        5,
                        exception.sourceSpan().end()
                ),
                () -> assertInstanceOf(
                        IllegalArgumentException.class,
                        exception.getCause()
                )
        );
    }

    private FilterExpression resolve(
            String input
    ) {
        return resolver.resolve(
                parse(input)
        );
    }

    private static FilterExpression parse(
            String input
    ) {
        return new FilterParser(input).parse();
    }

    private FilterSemanticException assertSemanticFailure(
            String input,
            String expectedMessagePart,
            int expectedStart,
            int expectedEnd
    ) {
        FilterSemanticException exception =
                assertThrows(
                        FilterSemanticException.class,
                        () -> resolve(input)
                );

        assertAll(
                () -> assertTrue(
                        exception.getMessage()
                                .contains(expectedMessagePart),
                        () -> "Expected message to contain: "
                                + expectedMessagePart
                                + System.lineSeparator()
                                + "Actual message: "
                                + exception.getMessage()
                ),
                () -> assertEquals(
                        expectedStart,
                        exception.sourceSpan().start()
                ),
                () -> assertEquals(
                        expectedEnd,
                        exception.sourceSpan().end()
                ),
                () -> assertEquals(
                        expectedStart,
                        exception.position()
                )
        );

        return exception;
    }

    private static BinaryFilterExpression assertBinary(
            FilterExpression expression,
            BinaryOperator expectedOperator
    ) {
        BinaryFilterExpression binary =
                assertInstanceOf(
                        BinaryFilterExpression.class,
                        expression
                );

        assertEquals(
                expectedOperator,
                binary.operator()
        );

        return binary;
    }

    private static UnaryFilterExpression assertUnary(
            FilterExpression expression
    ) {
        return assertInstanceOf(
                UnaryFilterExpression.class,
                expression
        );
    }

    private static PropertyFilterExpression assertProperty(
            FilterExpression expression
    ) {
        return assertInstanceOf(
                PropertyFilterExpression.class,
                expression
        );
    }

    private static LiteralFilterExpression assertLiteral(
            FilterExpression expression
    ) {
        return assertInstanceOf(
                LiteralFilterExpression.class,
                expression
        );
    }

    private static FunctionCallFilterExpression assertFunction(
            FilterExpression expression
    ) {
        return assertInstanceOf(
                FunctionCallFilterExpression.class,
                expression
        );
    }

    private static ListFilterExpression assertList(
            FilterExpression expression
    ) {
        return assertInstanceOf(
                ListFilterExpression.class,
                expression
        );
    }

    private static void assertFullyResolved(
            FilterExpression expression
    ) {
        assertNotEquals(
                ExpressionType.UNKNOWN,
                expression.expressionType()
        );

        if (expression
                instanceof PropertyFilterExpression property) {

            assertTrue(
                    property.isResolved()
            );

            assertTrue(
                    property.resolvedPath().isPresent()
            );

            return;
        }

        if (expression
                instanceof BinaryFilterExpression binary) {

            assertFullyResolved(
                    binary.left()
            );

            assertFullyResolved(
                    binary.right()
            );

            return;
        }

        if (expression
                instanceof UnaryFilterExpression unary) {

            assertFullyResolved(
                    unary.operand()
            );

            return;
        }

        if (expression
                instanceof FunctionCallFilterExpression function) {

            for (FilterExpression argument
                    : function.arguments()) {

                assertFullyResolved(
                        argument
                );
            }

            return;
        }

        if (expression
                instanceof ListFilterExpression list) {

            for (FilterExpression element
                    : list.elements()) {

                assertFullyResolved(
                        element
                );
            }
        }
    }

    private static void assertSourceSpansPreserved(
            FilterExpression unresolved,
            FilterExpression resolved
    ) {
        assertAll(
                () -> assertEquals(
                        unresolved.getClass(),
                        resolved.getClass()
                ),
                () -> assertEquals(
                        unresolved.sourceSpan(),
                        resolved.sourceSpan()
                )
        );

        if (unresolved
                instanceof BinaryFilterExpression unresolvedBinary) {

            BinaryFilterExpression resolvedBinary =
                    assertInstanceOf(
                            BinaryFilterExpression.class,
                            resolved
                    );

            assertSourceSpansPreserved(
                    unresolvedBinary.left(),
                    resolvedBinary.left()
            );

            assertSourceSpansPreserved(
                    unresolvedBinary.right(),
                    resolvedBinary.right()
            );

            return;
        }

        if (unresolved
                instanceof UnaryFilterExpression unresolvedUnary) {

            UnaryFilterExpression resolvedUnary =
                    assertInstanceOf(
                            UnaryFilterExpression.class,
                            resolved
                    );

            assertSourceSpansPreserved(
                    unresolvedUnary.operand(),
                    resolvedUnary.operand()
            );

            return;
        }

        if (unresolved
                instanceof FunctionCallFilterExpression unresolvedFunction) {

            FunctionCallFilterExpression resolvedFunction =
                    assertInstanceOf(
                            FunctionCallFilterExpression.class,
                            resolved
                    );

            assertEquals(
                    unresolvedFunction.arguments().size(),
                    resolvedFunction.arguments().size()
            );

            for (int index = 0;
                 index < unresolvedFunction.arguments().size();
                 index++) {

                assertSourceSpansPreserved(
                        unresolvedFunction.arguments().get(index),
                        resolvedFunction.arguments().get(index)
                );
            }

            return;
        }

        if (unresolved
                instanceof ListFilterExpression unresolvedList) {

            ListFilterExpression resolvedList =
                    assertInstanceOf(
                            ListFilterExpression.class,
                            resolved
                    );

            assertEquals(
                    unresolvedList.elements().size(),
                    resolvedList.elements().size()
            );

            for (int index = 0;
                 index < unresolvedList.elements().size();
                 index++) {

                assertSourceSpansPreserved(
                        unresolvedList.elements().get(index),
                        resolvedList.elements().get(index)
                );
            }
        }
    }

    private static Stream<Arguments> literalResolutionCases() {
        return Stream.of(
                Arguments.of(
                        "string literal",
                        "Title eq 'urgent'",
                        ExpressionType.STRING
                ),
                Arguments.of(
                        "integer literal",
                        "Priority eq 123",
                        ExpressionType.INTEGER
                ),
                Arguments.of(
                        "decimal literal",
                        "Amount eq 123.50",
                        ExpressionType.DECIMAL
                ),
                Arguments.of(
                        "boolean literal",
                        "Deleted eq true",
                        ExpressionType.BOOLEAN
                ),
                Arguments.of(
                        "null literal",
                        "Title eq null",
                        ExpressionType.NULL
                )
        );
    }

    private static Stream<Arguments> validComparisonCases() {
        return Stream.of(
                Arguments.of(
                        "string equality",
                        "Title eq 'urgent'",
                        BinaryOperator.EQ
                ),
                Arguments.of(
                        "string inequality",
                        "Title ne 'urgent'",
                        BinaryOperator.NE
                ),
                Arguments.of(
                        "integer greater than",
                        "Priority gt 1",
                        BinaryOperator.GT
                ),
                Arguments.of(
                        "integer greater than or equal",
                        "Priority ge 1",
                        BinaryOperator.GE
                ),
                Arguments.of(
                        "integer less than",
                        "Priority lt 1",
                        BinaryOperator.LT
                ),
                Arguments.of(
                        "integer less than or equal",
                        "Priority le 1",
                        BinaryOperator.LE
                ),
                Arguments.of(
                        "string ordered comparison",
                        "Title gt 'A'",
                        BinaryOperator.GT
                ),
                Arguments.of(
                        "integer and decimal equality",
                        "Priority eq 1.0",
                        BinaryOperator.EQ
                ),
                Arguments.of(
                        "decimal and integer equality",
                        "Amount eq 1",
                        BinaryOperator.EQ
                ),
                Arguments.of(
                        "property and null equality",
                        "Title eq null",
                        BinaryOperator.EQ
                ),
                Arguments.of(
                        "null and null equality",
                        "null eq null",
                        BinaryOperator.EQ
                )
        );
    }

    private static Stream<Arguments> integerArithmeticCases() {
        return Stream.of(
                Arguments.of(
                        "integer addition",
                        "Priority add 2 eq 3",
                        BinaryOperator.ADD
                ),
                Arguments.of(
                        "integer subtraction",
                        "Priority sub 2 eq 3",
                        BinaryOperator.SUB
                ),
                Arguments.of(
                        "integer multiplication",
                        "Priority mul 2 eq 3",
                        BinaryOperator.MUL
                ),
                Arguments.of(
                        "integer division",
                        "Priority div 2 eq 3",
                        BinaryOperator.DIV
                ),
                Arguments.of(
                        "integer modulo",
                        "Priority mod 2 eq 3",
                        BinaryOperator.MOD
                )
        );
    }

    private static Stream<Arguments> booleanFunctionCases() {
        return Stream.of(
                Arguments.of(
                        "contains",
                        "contains(Title, 'urgent')",
                        "contains"
                ),
                Arguments.of(
                        "starts with",
                        "startswith(Title, 'urgent')",
                        "startswith"
                ),
                Arguments.of(
                        "ends with",
                        "endswith(Title, 'urgent')",
                        "endswith"
                )
        );
    }

    private static Stream<Arguments> stringFunctionCases() {
        return Stream.of(
                Arguments.of(
                        "lowercase",
                        "tolower(Title) eq 'urgent'",
                        "tolower"
                ),
                Arguments.of(
                        "uppercase",
                        "toupper(Title) eq 'URGENT'",
                        "toupper"
                ),
                Arguments.of(
                        "trim",
                        "trim(Title) eq 'urgent'",
                        "trim"
                )
        );
    }

    private static Stream<Arguments>
    invalidFunctionArgumentCountCases() {
        return Stream.of(
                Arguments.of(
                        "contains with no arguments",
                        "contains()",
                        "Function 'contains' requires 2 argument(s), "
                                + "but received 0"
                ),
                Arguments.of(
                        "contains with one argument",
                        "contains(Title)",
                        "Function 'contains' requires 2 argument(s), "
                                + "but received 1"
                ),
                Arguments.of(
                        "contains with three arguments",
                        "contains(Title,'x','y')",
                        "Function 'contains' requires 2 argument(s), "
                                + "but received 3"
                ),
                Arguments.of(
                        "length with no arguments",
                        "length()",
                        "Function 'length' requires 1 argument(s), "
                                + "but received 0"
                ),
                Arguments.of(
                        "length with two arguments",
                        "length(Title,Title)",
                        "Function 'length' requires 1 argument(s), "
                                + "but received 2"
                )
        );
    }

    private static Stream<Arguments>
    invalidStringArgumentCases() {
        return Stream.of(
                Arguments.of(
                        "contains with numeric first argument",
                        "contains(Priority,'1')",
                        "Argument 1 of function 'contains' "
                                + "must be STRING, but was INTEGER",
                        9,
                        17
                ),
                Arguments.of(
                        "contains with numeric second argument",
                        "contains(Title,1)",
                        "Argument 2 of function 'contains' "
                                + "must be STRING, but was INTEGER",
                        15,
                        16
                ),
                Arguments.of(
                        "tolower with integer argument",
                        "tolower(Priority) eq 'x'",
                        "Argument 1 of function 'tolower' "
                                + "must be STRING, but was INTEGER",
                        8,
                        16
                ),
                Arguments.of(
                        "length with boolean argument",
                        "length(Deleted) gt 1",
                        "Argument 1 of function 'length' "
                                + "must be STRING, but was BOOLEAN",
                        7,
                        14
                )
        );
    }

    private static Stream<Arguments> nonBooleanRootCases() {
        return Stream.of(
                Arguments.of(
                        "string property",
                        "Title",
                        ExpressionType.STRING
                ),
                Arguments.of(
                        "integer property",
                        "Priority",
                        ExpressionType.INTEGER
                ),
                Arguments.of(
                        "decimal property",
                        "Amount",
                        ExpressionType.DECIMAL
                ),
                Arguments.of(
                        "string literal",
                        "'urgent'",
                        ExpressionType.STRING
                ),
                Arguments.of(
                        "integer arithmetic",
                        "Priority add 1",
                        ExpressionType.INTEGER
                ),
                Arguments.of(
                        "string function",
                        "tolower(Title)",
                        ExpressionType.STRING
                ),
                Arguments.of(
                        "integer function",
                        "length(Title)",
                        ExpressionType.INTEGER
                )
        );
    }
}