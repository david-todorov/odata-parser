package io.github.davidtodorov.odataparser.filter.parser;

import io.github.davidtodorov.odataparser.common.type.ExpressionType;
import io.github.davidtodorov.odataparser.filter.ast.BinaryFilterExpression;
import io.github.davidtodorov.odataparser.filter.ast.BinaryOperator;
import io.github.davidtodorov.odataparser.filter.ast.FilterExpression;
import io.github.davidtodorov.odataparser.filter.ast.FunctionCallFilterExpression;
import io.github.davidtodorov.odataparser.filter.ast.ListFilterExpression;
import io.github.davidtodorov.odataparser.filter.ast.LiteralFilterExpression;
import io.github.davidtodorov.odataparser.filter.ast.LiteralType;
import io.github.davidtodorov.odataparser.filter.ast.PropertyFilterExpression;
import io.github.davidtodorov.odataparser.filter.ast.UnaryFilterExpression;
import io.github.davidtodorov.odataparser.filter.ast.UnaryOperator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilterParserTest {

    @Test
    void shouldParseSinglePropertyExpression() {
        FilterExpression expression =
                parse("Title");

        PropertyFilterExpression property =
                assertProperty(
                        expression,
                        "Title"
                );

        assertAll(
                () -> assertEquals(
                        "Title",
                        property.path()
                ),
                () -> assertEquals(
                        ExpressionType.UNKNOWN,
                        property.expressionType()
                ),
                () -> assertFalse(
                        property.isResolved()
                ),
                () -> assertTrue(
                        property.resolvedPath().isEmpty()
                ),
                () -> assertSpan(
                        property,
                        0,
                        5
                )
        );
    }

    @Test
    void shouldParseDeepNavigationPropertyPath() {
        String input =
                "Owner/Department/Code";

        PropertyFilterExpression property =
                assertProperty(
                        parse(input),
                        "Owner",
                        "Department",
                        "Code"
                );

        assertAll(
                () -> assertEquals(
                        input,
                        property.path()
                ),
                () -> assertEquals(
                        List.of(
                                "Owner",
                                "Department",
                                "Code"
                        ),
                        property.pathSegments()
                ),
                () -> assertFalse(
                        property.isResolved()
                ),
                () -> assertSpan(
                        property,
                        0,
                        input.length()
                )
        );
    }

    @Test
    void shouldPreservePropertySourceSpanWithOuterWhitespace() {
        String input =
                "  Owner/Department/Code  ";

        PropertyFilterExpression property =
                assertProperty(
                        parse(input),
                        "Owner",
                        "Department",
                        "Code"
                );

        assertSpan(
                property,
                2,
                23
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("literalCases")
    void shouldParseLiteralExpression(
            String description,
            String input,
            LiteralType expectedLiteralType,
            String expectedValueText,
            String expectedRawText
    ) {
        LiteralFilterExpression literal =
                assertLiteral(
                        parse(input),
                        expectedLiteralType
                );

        assertEquals(
                expectedRawText,
                literal.rawText()
        );

        if (expectedValueText == null) {
            assertNull(
                    literal.value()
            );
        } else {
            assertEquals(
                    expectedValueText,
                    literal.value().toString()
            );
        }

        assertSpan(
                literal,
                0,
                input.length()
        );
    }

    @Test
    void shouldDecodeEscapedApostropheInStringValue() {
        LiteralFilterExpression literal =
                assertLiteral(
                        parse("'O''Brien'"),
                        LiteralType.STRING
                );

        assertAll(
                () -> assertEquals(
                        "'O''Brien'",
                        literal.rawText()
                ),
                () -> assertEquals(
                        "O'Brien",
                        literal.value()
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("binaryOperatorCases")
    void shouldParseBinaryOperator(
            String description,
            String input,
            BinaryOperator expectedOperator
    ) {
        BinaryFilterExpression binary =
                assertBinary(
                        parse(input),
                        expectedOperator
                );

        assertSpan(
                binary,
                0,
                input.length()
        );
    }

    @Test
    void shouldParseSimpleComparisonStructure() {
        BinaryFilterExpression comparison =
                assertBinary(
                        parse("Amount gt 100"),
                        BinaryOperator.GT
                );

        assertProperty(
                comparison.left(),
                "Amount"
        );

        LiteralFilterExpression right =
                assertLiteral(
                        comparison.right(),
                        LiteralType.INTEGER
                );

        assertAll(
                () -> assertEquals(
                        "100",
                        right.rawText()
                ),
                () -> assertEquals(
                        "100",
                        right.value().toString()
                ),
                () -> assertEquals(
                        ExpressionType.BOOLEAN,
                        comparison.expressionType()
                ),
                () -> assertSpan(
                        comparison,
                        0,
                        13
                )
        );
    }

    @Test
    void shouldParseEqualityWithNullLiteral() {
        BinaryFilterExpression comparison =
                assertBinary(
                        parse("Owner eq null"),
                        BinaryOperator.EQ
                );

        assertProperty(
                comparison.left(),
                "Owner"
        );

        LiteralFilterExpression nullLiteral =
                assertLiteral(
                        comparison.right(),
                        LiteralType.NULL
                );

        assertNull(
                nullLiteral.value()
        );
    }

    @Test
    void shouldGiveMultiplicationHigherPrecedenceThanAddition() {
        BinaryFilterExpression addition =
                assertBinary(
                        parse("A add B mul C"),
                        BinaryOperator.ADD
                );

        assertProperty(
                addition.left(),
                "A"
        );

        BinaryFilterExpression multiplication =
                assertBinary(
                        addition.right(),
                        BinaryOperator.MUL
                );

        assertProperty(
                multiplication.left(),
                "B"
        );

        assertProperty(
                multiplication.right(),
                "C"
        );
    }

    @Test
    void shouldGiveDivisionHigherPrecedenceThanSubtraction() {
        BinaryFilterExpression subtraction =
                assertBinary(
                        parse("A sub B div C"),
                        BinaryOperator.SUB
                );

        assertProperty(
                subtraction.left(),
                "A"
        );

        BinaryFilterExpression division =
                assertBinary(
                        subtraction.right(),
                        BinaryOperator.DIV
                );

        assertProperty(
                division.left(),
                "B"
        );

        assertProperty(
                division.right(),
                "C"
        );
    }

    @Test
    void shouldGiveModuloHigherPrecedenceThanAddition() {
        BinaryFilterExpression addition =
                assertBinary(
                        parse("A add B mod C"),
                        BinaryOperator.ADD
                );

        assertProperty(
                addition.left(),
                "A"
        );

        assertBinary(
                addition.right(),
                BinaryOperator.MOD
        );
    }

    @Test
    void shouldGiveArithmeticHigherPrecedenceThanComparison() {
        BinaryFilterExpression comparison =
                assertBinary(
                        parse("Amount add Tax gt Limit"),
                        BinaryOperator.GT
                );

        BinaryFilterExpression addition =
                assertBinary(
                        comparison.left(),
                        BinaryOperator.ADD
                );

        assertProperty(
                addition.left(),
                "Amount"
        );

        assertProperty(
                addition.right(),
                "Tax"
        );

        assertProperty(
                comparison.right(),
                "Limit"
        );
    }

    @Test
    void shouldGiveComparisonHigherPrecedenceThanLogicalAnd() {
        BinaryFilterExpression andExpression =
                assertBinary(
                        parse(
                                "Amount gt 100 and "
                                        + "Deleted eq false"
                        ),
                        BinaryOperator.AND
                );

        assertBinary(
                andExpression.left(),
                BinaryOperator.GT
        );

        assertBinary(
                andExpression.right(),
                BinaryOperator.EQ
        );
    }

    @Test
    void shouldGiveAndHigherPrecedenceThanOr() {
        BinaryFilterExpression orExpression =
                assertBinary(
                        parse("A or B and C"),
                        BinaryOperator.OR
                );

        assertProperty(
                orExpression.left(),
                "A"
        );

        BinaryFilterExpression andExpression =
                assertBinary(
                        orExpression.right(),
                        BinaryOperator.AND
                );

        assertProperty(
                andExpression.left(),
                "B"
        );

        assertProperty(
                andExpression.right(),
                "C"
        );
    }

    @Test
    void shouldParseAndBeforeFollowingOr() {
        BinaryFilterExpression orExpression =
                assertBinary(
                        parse("A and B or C"),
                        BinaryOperator.OR
                );

        assertBinary(
                orExpression.left(),
                BinaryOperator.AND
        );

        assertProperty(
                orExpression.right(),
                "C"
        );
    }

    @Test
    void shouldAllowParenthesesToOverrideArithmeticPrecedence() {
        BinaryFilterExpression multiplication =
                assertBinary(
                        parse("(A add B) mul C"),
                        BinaryOperator.MUL
                );

        BinaryFilterExpression addition =
                assertBinary(
                        multiplication.left(),
                        BinaryOperator.ADD
                );

        assertProperty(
                addition.left(),
                "A"
        );

        assertProperty(
                addition.right(),
                "B"
        );

        assertProperty(
                multiplication.right(),
                "C"
        );
    }

    @Test
    void shouldAllowParenthesesToOverrideLogicalPrecedence() {
        BinaryFilterExpression andExpression =
                assertBinary(
                        parse("(A or B) and C"),
                        BinaryOperator.AND
                );

        assertBinary(
                andExpression.left(),
                BinaryOperator.OR
        );

        assertProperty(
                andExpression.right(),
                "C"
        );
    }

    @Test
    void shouldRemoveRedundantGroupingFromAstStructure() {
        PropertyFilterExpression property =
                assertProperty(
                        parse("(((Title)))"),
                        "Title"
                );

        assertEquals(
                "Title",
                property.path()
        );
    }

    @Test
    void shouldParseAdditionAndSubtractionAsLeftAssociative() {
        BinaryFilterExpression outerSubtraction =
                assertBinary(
                        parse("A sub B sub C"),
                        BinaryOperator.SUB
                );

        BinaryFilterExpression innerSubtraction =
                assertBinary(
                        outerSubtraction.left(),
                        BinaryOperator.SUB
                );

        assertProperty(
                innerSubtraction.left(),
                "A"
        );

        assertProperty(
                innerSubtraction.right(),
                "B"
        );

        assertProperty(
                outerSubtraction.right(),
                "C"
        );
    }

    @Test
    void shouldParseMultiplicationAndDivisionAsLeftAssociative() {
        BinaryFilterExpression multiplication =
                assertBinary(
                        parse("A div B mul C"),
                        BinaryOperator.MUL
                );

        BinaryFilterExpression division =
                assertBinary(
                        multiplication.left(),
                        BinaryOperator.DIV
                );

        assertProperty(
                division.left(),
                "A"
        );

        assertProperty(
                division.right(),
                "B"
        );

        assertProperty(
                multiplication.right(),
                "C"
        );
    }

    @Test
    void shouldParseLogicalAndAsLeftAssociative() {
        BinaryFilterExpression outerAnd =
                assertBinary(
                        parse("A and B and C"),
                        BinaryOperator.AND
                );

        BinaryFilterExpression innerAnd =
                assertBinary(
                        outerAnd.left(),
                        BinaryOperator.AND
                );

        assertProperty(
                innerAnd.left(),
                "A"
        );

        assertProperty(
                innerAnd.right(),
                "B"
        );

        assertProperty(
                outerAnd.right(),
                "C"
        );
    }

    @Test
    void shouldParseLogicalOrAsLeftAssociative() {
        BinaryFilterExpression outerOr =
                assertBinary(
                        parse("A or B or C"),
                        BinaryOperator.OR
                );

        BinaryFilterExpression innerOr =
                assertBinary(
                        outerOr.left(),
                        BinaryOperator.OR
                );

        assertProperty(
                innerOr.left(),
                "A"
        );

        assertProperty(
                innerOr.right(),
                "B"
        );

        assertProperty(
                outerOr.right(),
                "C"
        );
    }

    @Test
    void shouldParseUnaryNotExpression() {
        UnaryFilterExpression unary =
                assertInstanceOf(
                        UnaryFilterExpression.class,
                        parse("not Deleted")
                );

        assertAll(
                () -> assertEquals(
                        UnaryOperator.NOT,
                        unary.operator()
                ),
                () -> assertEquals(
                        ExpressionType.BOOLEAN,
                        unary.expressionType()
                ),
                () -> assertSpan(
                        unary,
                        0,
                        11
                )
        );

        assertProperty(
                unary.operand(),
                "Deleted"
        );
    }

    @Test
    void shouldParseRepeatedUnaryNotOperators() {
        UnaryFilterExpression outer =
                assertInstanceOf(
                        UnaryFilterExpression.class,
                        parse("not not Deleted")
                );

        assertEquals(
                UnaryOperator.NOT,
                outer.operator()
        );

        UnaryFilterExpression inner =
                assertInstanceOf(
                        UnaryFilterExpression.class,
                        outer.operand()
                );

        assertEquals(
                UnaryOperator.NOT,
                inner.operator()
        );

        assertProperty(
                inner.operand(),
                "Deleted"
        );
    }

    @Test
    void shouldParseFunctionCallWithTwoArguments() {
        String input =
                "contains(Title, 'urgent')";

        FunctionCallFilterExpression function =
                assertFunction(
                        parse(input),
                        "contains",
                        2
                );

        assertProperty(
                function.arguments().get(0),
                "Title"
        );

        LiteralFilterExpression argument =
                assertLiteral(
                        function.arguments().get(1),
                        LiteralType.STRING
                );

        assertAll(
                () -> assertEquals(
                        "urgent",
                        argument.value()
                ),
                () -> assertEquals(
                        ExpressionType.UNKNOWN,
                        function.expressionType()
                ),
                () -> assertSpan(
                        function,
                        0,
                        input.length()
                )
        );
    }

    @Test
    void shouldParseFunctionCallUsingNavigationPath() {
        FunctionCallFilterExpression function =
                assertFunction(
                        parse(
                                "contains("
                                        + "Owner/Department/Code,"
                                        + "'ENG'"
                                        + ")"
                        ),
                        "contains",
                        2
                );

        assertProperty(
                function.arguments().get(0),
                "Owner",
                "Department",
                "Code"
        );

        assertLiteral(
                function.arguments().get(1),
                LiteralType.STRING
        );
    }

    @Test
    void shouldParseNestedFunctionCalls() {
        FunctionCallFilterExpression contains =
                assertFunction(
                        parse(
                                "contains("
                                        + "tolower(Title),"
                                        + "'urgent'"
                                        + ")"
                        ),
                        "contains",
                        2
                );

        FunctionCallFilterExpression toLower =
                assertFunction(
                        contains.arguments().get(0),
                        "tolower",
                        1
                );

        assertProperty(
                toLower.arguments().getFirst(),
                "Title"
        );

        assertLiteral(
                contains.arguments().get(1),
                LiteralType.STRING
        );
    }

    @Test
    void shouldParseFunctionWithExpressionArguments() {
        FunctionCallFilterExpression function =
                assertFunction(
                        parse(
                                "custom("
                                        + "Amount add 10,"
                                        + "Deleted eq false"
                                        + ")"
                        ),
                        "custom",
                        2
                );

        assertBinary(
                function.arguments().get(0),
                BinaryOperator.ADD
        );

        assertBinary(
                function.arguments().get(1),
                BinaryOperator.EQ
        );
    }

    @Test
    void shouldPreserveFunctionNameCase() {
        FunctionCallFilterExpression function =
                assertFunction(
                        parse("CustomFunction(Title)"),
                        "CustomFunction",
                        1
                );

        assertEquals(
                "CustomFunction",
                function.functionName()
        );
    }

    @Test
    void shouldParseInExpressionWithLiteralList() {
        BinaryFilterExpression inExpression =
                assertBinary(
                        parse(
                                "Priority in (1, 2, 3)"
                        ),
                        BinaryOperator.IN
                );

        assertProperty(
                inExpression.left(),
                "Priority"
        );

        ListFilterExpression list =
                assertInstanceOf(
                        ListFilterExpression.class,
                        inExpression.right()
                );

        assertEquals(
                3,
                list.elements().size()
        );

        assertLiteral(
                list.elements().get(0),
                LiteralType.INTEGER
        );

        assertLiteral(
                list.elements().get(1),
                LiteralType.INTEGER
        );

        assertLiteral(
                list.elements().get(2),
                LiteralType.INTEGER
        );
    }

    @Test
    void shouldParseInExpressionWithStringList() {
        BinaryFilterExpression inExpression =
                assertBinary(
                        parse(
                                "Status in "
                                        + "('OPEN', 'CLOSED', 'PENDING')"
                        ),
                        BinaryOperator.IN
                );

        ListFilterExpression list =
                assertInstanceOf(
                        ListFilterExpression.class,
                        inExpression.right()
                );

        assertEquals(
                List.of(
                        "OPEN",
                        "CLOSED",
                        "PENDING"
                ),
                list.elements()
                        .stream()
                        .map(
                                element ->
                                        assertInstanceOf(
                                                LiteralFilterExpression.class,
                                                element
                                        )
                        )
                        .map(LiteralFilterExpression::value)
                        .toList()
        );
    }

    @Test
    void shouldParseSingletonInList() {
        BinaryFilterExpression inExpression =
                assertBinary(
                        parse("Priority in (1)"),
                        BinaryOperator.IN
                );

        ListFilterExpression list =
                assertInstanceOf(
                        ListFilterExpression.class,
                        inExpression.right()
                );

        assertEquals(
                1,
                list.elements().size()
        );
    }

    @Test
    void shouldParseExpressionsInsideInList() {
        BinaryFilterExpression inExpression =
                assertBinary(
                        parse(
                                "Priority in "
                                        + "(1, OtherPriority, 2 add 3)"
                        ),
                        BinaryOperator.IN
                );

        ListFilterExpression list =
                assertInstanceOf(
                        ListFilterExpression.class,
                        inExpression.right()
                );

        assertEquals(
                3,
                list.elements().size()
        );

        assertLiteral(
                list.elements().get(0),
                LiteralType.INTEGER
        );

        assertProperty(
                list.elements().get(1),
                "OtherPriority"
        );

        assertBinary(
                list.elements().get(2),
                BinaryOperator.ADD
        );
    }

    @Test
    void shouldParseRealisticCompleteExpression() {
        String input =
                "contains(Title, 'urgent') "
                        + "and Amount gt 100.50 "
                        + "and Deleted eq false "
                        + "and Owner/Department/Code eq 'ENG'";

        BinaryFilterExpression root =
                assertBinary(
                        parse(input),
                        BinaryOperator.AND
                );

        assertEquals(
                ExpressionType.BOOLEAN,
                root.expressionType()
        );

        assertSpan(
                root,
                0,
                input.length()
        );

        BinaryFilterExpression left =
                assertBinary(
                        root.left(),
                        BinaryOperator.AND
                );

        assertBinary(
                root.right(),
                BinaryOperator.EQ
        );

        assertBinary(
                left.left(),
                BinaryOperator.AND
        );

        assertBinary(
                left.right(),
                BinaryOperator.EQ
        );
    }

    @Test
    void shouldIgnoreWhitespaceAroundExpression() {
        BinaryFilterExpression expression =
                assertBinary(
                        parse(
                                " \t\r\n "
                                        + "Title eq 'urgent'"
                                        + " \t\r\n "
                        ),
                        BinaryOperator.EQ
                );

        assertProperty(
                expression.left(),
                "Title"
        );

        assertLiteral(
                expression.right(),
                LiteralType.STRING
        );
    }

    @Test
    void shouldProduceEquivalentAstShapesForEquivalentWhitespace() {
        FilterExpression compact =
                parse(
                        "contains(Title,'urgent')"
                                + "and Amount gt 10"
                );

        FilterExpression spaced =
                parse(
                        "contains ( Title , 'urgent' ) "
                                + "and Amount gt 10"
                );

        assertEquivalentShape(
                compact,
                spaced
        );
    }

    @Test
    void shouldCreateIndependentAstForEachParserInstance() {
        FilterExpression first =
                parse("Title eq 'urgent'");

        FilterExpression second =
                parse("Title eq 'urgent'");

        assertEquivalentShape(
                first,
                second
        );
    }

    @ParameterizedTest(name = "reject malformed filter: {0}")
    @ValueSource(strings = {
            "",
            " ",
            "\t\r\n",

            "and",
            "or",
            "eq",
            "gt",
            "in",
            "add",
            "mul",

            ")",
            ",",
            "/",

            "not",
            "Title eq",
            "Title ne",
            "Amount gt",
            "Amount add",
            "A and",
            "A or",

            "(",
            "()",
            "(Title",
            "Title)",
            "(Title eq 'x'",
            "Title eq 'x')",

            "Owner/",
            "/Owner",
            "Owner//Username",

            "contains(",
            "contains(Title",
            "contains(Title,",
            "contains(Title,)",
            "contains(,Title)",
            "contains(Title 'urgent')",
            "contains(Title, 'urgent'",

            "Priority in",
            "Priority in 1",
            "Priority in ()",
            "Priority in (",
            "Priority in (1",
            "Priority in (1,)",
            "Priority in (,1)",
            "Priority in (1 2)",
            "Priority in (1,,2)",

            "Title eq 'urgent' Amount",
            "Title eq 'urgent' true",
            "Title Title",
            "1 2",
            "'a' 'b'"
    })
    void shouldRejectMalformedFilter(
            String input
    ) {
        assertThrows(
                FilterParserException.class,
                () -> parse(input)
        );
    }

    @ParameterizedTest(name = "reject incomplete operator expression: {0}")
    @MethodSource("incompleteOperatorCases")
    void shouldRejectExpressionWithoutRightOperand(
            String input
    ) {
        FilterParserException exception =
                assertThrows(
                        FilterParserException.class,
                        () -> parse(input)
                );

        assertTrue(
                exception.getMessage() != null
                        && !exception.getMessage().isBlank()
        );
    }

    @ParameterizedTest(name = "operator keywords are case-sensitive: {0}")
    @ValueSource(strings = {
            "Title EQ 'urgent'",
            "Title Eq 'urgent'",
            "Amount GT 10",
            "Deleted AND Closed",
            "Deleted OR Closed",
            "NOT Deleted",
            "Amount ADD 10"
    })
    void shouldRejectIncorrectlyCasedOperatorKeyword(
            String input
    ) {
        assertThrows(
                FilterParserException.class,
                () -> parse(input)
        );
    }

    @Test
    void shouldRejectUnexpectedClosingParenthesisAtRoot() {
        FilterParserException exception =
                assertThrows(
                        FilterParserException.class,
                        () -> parse(
                                "Title eq 'urgent')"
                        )
                );

        assertTrue(
                exception.getMessage() != null
                        && !exception.getMessage().isBlank()
        );
    }

    @Test
    void shouldRejectMissingFunctionArgumentSeparator() {
        assertThrows(
                FilterParserException.class,
                () -> parse(
                        "contains(Title 'urgent')"
                )
        );
    }

    @Test
    void shouldRejectTrailingFunctionArgumentSeparator() {
        assertThrows(
                FilterParserException.class,
                () -> parse(
                        "contains(Title, 'urgent',)"
                )
        );
    }

    @Test
    void shouldRejectTrailingInListSeparator() {
        assertThrows(
                FilterParserException.class,
                () -> parse(
                        "Priority in (1, 2,)"
                )
        );
    }

    private static FilterExpression parse(
            String input
    ) {
        return new FilterParser(input).parse();
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
            FilterExpression expression,
            UnaryOperator expectedOperator
    ) {
        UnaryFilterExpression unary =
                assertInstanceOf(
                        UnaryFilterExpression.class,
                        expression
                );

        assertEquals(
                expectedOperator,
                unary.operator()
        );

        return unary;
    }

    private static PropertyFilterExpression assertProperty(
            FilterExpression expression,
            String... expectedSegments
    ) {
        PropertyFilterExpression property =
                assertInstanceOf(
                        PropertyFilterExpression.class,
                        expression
                );

        assertEquals(
                List.of(expectedSegments),
                property.pathSegments()
        );

        return property;
    }

    private static LiteralFilterExpression assertLiteral(
            FilterExpression expression,
            LiteralType expectedType
    ) {
        LiteralFilterExpression literal =
                assertInstanceOf(
                        LiteralFilterExpression.class,
                        expression
                );

        assertEquals(
                expectedType,
                literal.literalType()
        );

        return literal;
    }

    private static FunctionCallFilterExpression assertFunction(
            FilterExpression expression,
            String expectedName,
            int expectedArgumentCount
    ) {
        FunctionCallFilterExpression function =
                assertInstanceOf(
                        FunctionCallFilterExpression.class,
                        expression
                );

        assertAll(
                () -> assertEquals(
                        expectedName,
                        function.functionName()
                ),
                () -> assertEquals(
                        expectedArgumentCount,
                        function.arguments().size()
                )
        );

        return function;
    }

    private static void assertSpan(
            FilterExpression expression,
            int expectedStart,
            int expectedEnd
    ) {
        assertAll(
                () -> assertEquals(
                        expectedStart,
                        expression.sourceSpan().start()
                ),
                () -> assertEquals(
                        expectedEnd,
                        expression.sourceSpan().end()
                )
        );
    }

    private static void assertEquivalentShape(
            FilterExpression first,
            FilterExpression second
    ) {
        assertEquals(
                first.getClass(),
                second.getClass()
        );

        if (first
                instanceof BinaryFilterExpression firstBinary) {

            BinaryFilterExpression secondBinary =
                    assertInstanceOf(
                            BinaryFilterExpression.class,
                            second
                    );

            assertEquals(
                    firstBinary.operator(),
                    secondBinary.operator()
            );

            assertEquivalentShape(
                    firstBinary.left(),
                    secondBinary.left()
            );

            assertEquivalentShape(
                    firstBinary.right(),
                    secondBinary.right()
            );

            return;
        }

        if (first
                instanceof UnaryFilterExpression firstUnary) {

            UnaryFilterExpression secondUnary =
                    assertInstanceOf(
                            UnaryFilterExpression.class,
                            second
                    );

            assertEquals(
                    firstUnary.operator(),
                    secondUnary.operator()
            );

            assertEquivalentShape(
                    firstUnary.operand(),
                    secondUnary.operand()
            );

            return;
        }

        if (first
                instanceof PropertyFilterExpression firstProperty) {

            PropertyFilterExpression secondProperty =
                    assertInstanceOf(
                            PropertyFilterExpression.class,
                            second
                    );

            assertEquals(
                    firstProperty.pathSegments(),
                    secondProperty.pathSegments()
            );

            return;
        }

        if (first
                instanceof LiteralFilterExpression firstLiteral) {

            LiteralFilterExpression secondLiteral =
                    assertInstanceOf(
                            LiteralFilterExpression.class,
                            second
                    );

            assertAll(
                    () -> assertEquals(
                            firstLiteral.literalType(),
                            secondLiteral.literalType()
                    ),
                    () -> assertEquals(
                            firstLiteral.value(),
                            secondLiteral.value()
                    ),
                    () -> assertEquals(
                            firstLiteral.rawText(),
                            secondLiteral.rawText()
                    )
            );

            return;
        }

        if (first
                instanceof FunctionCallFilterExpression firstFunction) {

            FunctionCallFilterExpression secondFunction =
                    assertInstanceOf(
                            FunctionCallFilterExpression.class,
                            second
                    );

            assertAll(
                    () -> assertEquals(
                            firstFunction.functionName(),
                            secondFunction.functionName()
                    ),
                    () -> assertEquals(
                            firstFunction.arguments().size(),
                            secondFunction.arguments().size()
                    )
            );

            for (int index = 0;
                 index < firstFunction.arguments().size();
                 index++) {

                assertEquivalentShape(
                        firstFunction.arguments().get(index),
                        secondFunction.arguments().get(index)
                );
            }

            return;
        }

        if (first
                instanceof ListFilterExpression firstList) {

            ListFilterExpression secondList =
                    assertInstanceOf(
                            ListFilterExpression.class,
                            second
                    );

            assertEquals(
                    firstList.elements().size(),
                    secondList.elements().size()
            );

            for (int index = 0;
                 index < firstList.elements().size();
                 index++) {

                assertEquivalentShape(
                        firstList.elements().get(index),
                        secondList.elements().get(index)
                );
            }
        }
    }

    private static Stream<Arguments> literalCases() {
        return Stream.of(
                Arguments.of(
                        "string literal",
                        "'urgent'",
                        LiteralType.STRING,
                        "urgent",
                        "'urgent'"
                ),
                Arguments.of(
                        "empty string literal",
                        "''",
                        LiteralType.STRING,
                        "",
                        "''"
                ),
                Arguments.of(
                        "string with escaped apostrophe",
                        "'O''Brien'",
                        LiteralType.STRING,
                        "O'Brien",
                        "'O''Brien'"
                ),
                Arguments.of(
                        "positive integer",
                        "123",
                        LiteralType.INTEGER,
                        "123",
                        "123"
                ),
                Arguments.of(
                        "zero integer",
                        "0",
                        LiteralType.INTEGER,
                        "0",
                        "0"
                ),
                Arguments.of(
                        "negative integer",
                        "-123",
                        LiteralType.INTEGER,
                        "-123",
                        "-123"
                ),
                Arguments.of(
                        "positive decimal",
                        "123.50",
                        LiteralType.DECIMAL,
                        "123.50",
                        "123.50"
                ),
                Arguments.of(
                        "negative decimal",
                        "-123.50",
                        LiteralType.DECIMAL,
                        "-123.50",
                        "-123.50"
                ),
                Arguments.of(
                        "boolean true",
                        "true",
                        LiteralType.BOOLEAN,
                        "true",
                        "true"
                ),
                Arguments.of(
                        "boolean false",
                        "false",
                        LiteralType.BOOLEAN,
                        "false",
                        "false"
                ),
                Arguments.of(
                        "null literal",
                        "null",
                        LiteralType.NULL,
                        null,
                        "null"
                )
        );
    }

    private static Stream<Arguments> binaryOperatorCases() {
        return Stream.of(
                Arguments.of(
                        "logical and",
                        "A and B",
                        BinaryOperator.AND
                ),
                Arguments.of(
                        "logical or",
                        "A or B",
                        BinaryOperator.OR
                ),

                Arguments.of(
                        "equality",
                        "A eq B",
                        BinaryOperator.EQ
                ),
                Arguments.of(
                        "inequality",
                        "A ne B",
                        BinaryOperator.NE
                ),
                Arguments.of(
                        "greater than",
                        "A gt B",
                        BinaryOperator.GT
                ),
                Arguments.of(
                        "greater than or equal",
                        "A ge B",
                        BinaryOperator.GE
                ),
                Arguments.of(
                        "less than",
                        "A lt B",
                        BinaryOperator.LT
                ),
                Arguments.of(
                        "less than or equal",
                        "A le B",
                        BinaryOperator.LE
                ),
                Arguments.of(
                        "membership",
                        "A in (1)",
                        BinaryOperator.IN
                ),

                Arguments.of(
                        "addition",
                        "A add B",
                        BinaryOperator.ADD
                ),
                Arguments.of(
                        "subtraction",
                        "A sub B",
                        BinaryOperator.SUB
                ),
                Arguments.of(
                        "multiplication",
                        "A mul B",
                        BinaryOperator.MUL
                ),
                Arguments.of(
                        "division",
                        "A div B",
                        BinaryOperator.DIV
                ),
                Arguments.of(
                        "modulo",
                        "A mod B",
                        BinaryOperator.MOD
                )
        );
    }

    private static Stream<String> incompleteOperatorCases() {
        return Stream.of(
                "A and",
                "A or",
                "A eq",
                "A ne",
                "A gt",
                "A ge",
                "A lt",
                "A le",
                "A in",
                "A add",
                "A sub",
                "A mul",
                "A div",
                "A mod"
        );
    }

    @Test
    void shouldParseFunctionCallWithoutArguments() {
        FunctionCallFilterExpression function =
                assertFunction(
                        parse("contains()"),
                        "contains",
                        0
                );

        assertAll(
                () -> assertTrue(
                        function.arguments().isEmpty()
                ),
                () -> assertEquals(
                        ExpressionType.UNKNOWN,
                        function.expressionType()
                ),
                () -> assertSpan(
                        function,
                        0,
                        10
                )
        );
    }
}