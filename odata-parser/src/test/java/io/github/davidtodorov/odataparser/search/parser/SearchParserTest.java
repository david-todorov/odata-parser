package io.github.davidtodorov.odataparser.search.parser;

import io.github.davidtodorov.odataparser.search.ast.SearchBinaryExpression;
import io.github.davidtodorov.odataparser.search.ast.SearchBinaryOperator;
import io.github.davidtodorov.odataparser.search.ast.SearchExpression;
import io.github.davidtodorov.odataparser.search.ast.SearchNotExpression;
import io.github.davidtodorov.odataparser.search.ast.SearchPhraseExpression;
import io.github.davidtodorov.odataparser.search.ast.SearchTermExpression;
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
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchParserTest {

    @Test
    void shouldRejectNullInput() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new SearchParser(null)
                );

        assertEquals(
                "Search parser input cannot be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldParseSingleTerm() {
        SearchTermExpression term =
                assertInstanceOf(
                        SearchTermExpression.class,
                        parse("urgent")
                );

        assertAll(
                () -> assertEquals(
                        "urgent",
                        term.term()
                ),
                () -> assertSpan(
                        term,
                        0,
                        6
                ),
                () -> assertTrue(
                        term.isLeaf()
                ),
                () -> assertTrue(
                        term.children().isEmpty()
                )
        );
    }

    @Test
    void shouldParseQuotedPhrase() {
        SearchPhraseExpression phrase =
                assertInstanceOf(
                        SearchPhraseExpression.class,
                        parse(
                                "\"database failure\""
                        )
                );

        assertAll(
                () -> assertEquals(
                        "database failure",
                        phrase.phrase()
                ),
                () -> assertEquals(
                        "\"database failure\"",
                        phrase.rawText()
                ),
                () -> assertSpan(
                        phrase,
                        0,
                        18
                ),
                () -> assertTrue(
                        phrase.isLeaf()
                )
        );
    }

    @Test
    void shouldPreserveDecodedAndRawEscapedPhraseValues() {
        String input =
                "\"say \\\"hello\\\" at C:\\\\temp\"";

        SearchPhraseExpression phrase =
                assertInstanceOf(
                        SearchPhraseExpression.class,
                        parse(input)
                );

        assertAll(
                () -> assertEquals(
                        "say \"hello\" at C:\\temp",
                        phrase.phrase()
                ),
                () -> assertEquals(
                        input,
                        phrase.rawText()
                ),
                () -> assertSpan(
                        phrase,
                        0,
                        input.length()
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("binaryOperatorCases")
    void shouldParseExplicitBinaryExpression(
            String description,
            String input,
            SearchBinaryOperator expectedOperator,
            String expectedLeft,
            String expectedRight
    ) {
        SearchBinaryExpression binary =
                assertInstanceOf(
                        SearchBinaryExpression.class,
                        parse(input)
                );

        SearchTermExpression left =
                assertInstanceOf(
                        SearchTermExpression.class,
                        binary.left()
                );

        SearchTermExpression right =
                assertInstanceOf(
                        SearchTermExpression.class,
                        binary.right()
                );

        assertAll(
                () -> assertEquals(
                        expectedOperator,
                        binary.operator()
                ),
                () -> assertEquals(
                        expectedLeft,
                        left.term()
                ),
                () -> assertEquals(
                        expectedRight,
                        right.term()
                ),
                () -> assertSpan(
                        binary,
                        0,
                        input.length()
                ),
                () -> assertFalse(
                        binary.isLeaf()
                ),
                () -> assertEquals(
                        List.of(left, right),
                        binary.children()
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("implicitAndCases")
    void shouldParseImplicitAnd(
            String description,
            String input
    ) {
        SearchBinaryExpression binary =
                assertInstanceOf(
                        SearchBinaryExpression.class,
                        parse(input)
                );

        assertAll(
                () -> assertEquals(
                        SearchBinaryOperator.AND,
                        binary.operator()
                ),
                () -> assertSpan(
                        binary,
                        0,
                        input.length()
                )
        );
    }

    @Test
    void shouldApplyAndBeforeOr() {
        SearchBinaryExpression root =
                assertInstanceOf(
                        SearchBinaryExpression.class,
                        parse(
                                "urgent OR delayed AND archived"
                        )
                );

        SearchTermExpression left =
                assertInstanceOf(
                        SearchTermExpression.class,
                        root.left()
                );

        SearchBinaryExpression right =
                assertInstanceOf(
                        SearchBinaryExpression.class,
                        root.right()
                );

        assertAll(
                () -> assertEquals(
                        SearchBinaryOperator.OR,
                        root.operator()
                ),
                () -> assertEquals(
                        "urgent",
                        left.term()
                ),
                () -> assertEquals(
                        SearchBinaryOperator.AND,
                        right.operator()
                ),
                () -> assertTerm(
                        right.left(),
                        "delayed"
                ),
                () -> assertTerm(
                        right.right(),
                        "archived"
                ),
                () -> assertSpan(
                        root,
                        0,
                        30
                )
        );
    }

    @Test
    void shouldApplyImplicitAndBeforeOr() {
        SearchBinaryExpression root =
                assertInstanceOf(
                        SearchBinaryExpression.class,
                        parse(
                                "urgent OR delayed archived"
                        )
                );

        SearchBinaryExpression right =
                assertInstanceOf(
                        SearchBinaryExpression.class,
                        root.right()
                );

        assertAll(
                () -> assertEquals(
                        SearchBinaryOperator.OR,
                        root.operator()
                ),
                () -> assertEquals(
                        SearchBinaryOperator.AND,
                        right.operator()
                ),
                () -> assertTerm(
                        right.left(),
                        "delayed"
                ),
                () -> assertTerm(
                        right.right(),
                        "archived"
                )
        );
    }

    @Test
    void shouldAssociateAndExpressionsFromLeftToRight() {
        SearchBinaryExpression root =
                assertInstanceOf(
                        SearchBinaryExpression.class,
                        parse(
                                "urgent AND delayed AND archived"
                        )
                );

        SearchBinaryExpression left =
                assertInstanceOf(
                        SearchBinaryExpression.class,
                        root.left()
                );

        assertAll(
                () -> assertEquals(
                        SearchBinaryOperator.AND,
                        root.operator()
                ),
                () -> assertEquals(
                        SearchBinaryOperator.AND,
                        left.operator()
                ),
                () -> assertTerm(
                        left.left(),
                        "urgent"
                ),
                () -> assertTerm(
                        left.right(),
                        "delayed"
                ),
                () -> assertTerm(
                        root.right(),
                        "archived"
                )
        );
    }

    @Test
    void shouldAssociateOrExpressionsFromLeftToRight() {
        SearchBinaryExpression root =
                assertInstanceOf(
                        SearchBinaryExpression.class,
                        parse(
                                "urgent OR delayed OR archived"
                        )
                );

        SearchBinaryExpression left =
                assertInstanceOf(
                        SearchBinaryExpression.class,
                        root.left()
                );

        assertAll(
                () -> assertEquals(
                        SearchBinaryOperator.OR,
                        root.operator()
                ),
                () -> assertEquals(
                        SearchBinaryOperator.OR,
                        left.operator()
                ),
                () -> assertTerm(
                        left.left(),
                        "urgent"
                ),
                () -> assertTerm(
                        left.right(),
                        "delayed"
                ),
                () -> assertTerm(
                        root.right(),
                        "archived"
                )
        );
    }

    @Test
    void shouldParseNotExpression() {
        SearchNotExpression not =
                assertInstanceOf(
                        SearchNotExpression.class,
                        parse("NOT urgent")
                );

        assertAll(
                () -> assertTerm(
                        not.operand(),
                        "urgent"
                ),
                () -> assertSpan(
                        not,
                        0,
                        10
                ),
                () -> assertEquals(
                        List.of(not.operand()),
                        not.children()
                ),
                () -> assertFalse(
                        not.isLeaf()
                )
        );
    }

    @Test
    void shouldAssociateRepeatedNotFromRightToLeft() {
        SearchNotExpression outer =
                assertInstanceOf(
                        SearchNotExpression.class,
                        parse("NOT NOT urgent")
                );

        SearchNotExpression inner =
                assertInstanceOf(
                        SearchNotExpression.class,
                        outer.operand()
                );

        assertAll(
                () -> assertTerm(
                        inner.operand(),
                        "urgent"
                ),
                () -> assertSpan(
                        outer,
                        0,
                        14
                ),
                () -> assertSpan(
                        inner,
                        4,
                        14
                )
        );
    }

    @Test
    void shouldApplyNotBeforeAnd() {
        SearchBinaryExpression root =
                assertInstanceOf(
                        SearchBinaryExpression.class,
                        parse(
                                "NOT urgent AND delayed"
                        )
                );

        SearchNotExpression left =
                assertInstanceOf(
                        SearchNotExpression.class,
                        root.left()
                );

        assertAll(
                () -> assertEquals(
                        SearchBinaryOperator.AND,
                        root.operator()
                ),
                () -> assertTerm(
                        left.operand(),
                        "urgent"
                ),
                () -> assertTerm(
                        root.right(),
                        "delayed"
                )
        );
    }

    @Test
    void shouldAllowNotImmediatelyBeforeParenthesis() {
        SearchNotExpression not =
                assertInstanceOf(
                        SearchNotExpression.class,
                        parse(
                                "NOT(urgent OR delayed)"
                        )
                );

        SearchBinaryExpression operand =
                assertInstanceOf(
                        SearchBinaryExpression.class,
                        not.operand()
                );

        assertAll(
                () -> assertEquals(
                        SearchBinaryOperator.OR,
                        operand.operator()
                ),
                () -> assertSpan(
                        operand,
                        3,
                        22
                ),
                () -> assertSpan(
                        not,
                        0,
                        22
                )
        );
    }

    @Test
    void shouldAllowExplicitOperatorImmediatelyBeforeParenthesis() {
        SearchBinaryExpression root =
                assertInstanceOf(
                        SearchBinaryExpression.class,
                        parse(
                                "urgent AND(delayed OR archived)"
                        )
                );

        SearchBinaryExpression right =
                assertInstanceOf(
                        SearchBinaryExpression.class,
                        root.right()
                );

        assertAll(
                () -> assertEquals(
                        SearchBinaryOperator.AND,
                        root.operator()
                ),
                () -> assertEquals(
                        SearchBinaryOperator.OR,
                        right.operator()
                ),
                () -> assertTerm(
                        right.left(),
                        "delayed"
                ),
                () -> assertTerm(
                        right.right(),
                        "archived"
                )
        );
    }

    @Test
    void shouldUseParenthesesToOverridePrecedence() {
        SearchBinaryExpression root =
                assertInstanceOf(
                        SearchBinaryExpression.class,
                        parse(
                                "(urgent OR delayed) AND archived"
                        )
                );

        SearchBinaryExpression left =
                assertInstanceOf(
                        SearchBinaryExpression.class,
                        root.left()
                );

        assertAll(
                () -> assertEquals(
                        SearchBinaryOperator.AND,
                        root.operator()
                ),
                () -> assertEquals(
                        SearchBinaryOperator.OR,
                        left.operator()
                ),
                () -> assertSpan(
                        left,
                        0,
                        19
                ),
                () -> assertSpan(
                        root,
                        0,
                        32
                )
        );
    }

    @Test
    void shouldIncludeParenthesesInSingleTermSpan() {
        SearchTermExpression term =
                assertInstanceOf(
                        SearchTermExpression.class,
                        parse("(urgent)")
                );

        assertAll(
                () -> assertEquals(
                        "urgent",
                        term.term()
                ),
                () -> assertSpan(
                        term,
                        0,
                        8
                )
        );
    }

    @Test
    void shouldIncludeAllNestedParenthesesInRootSpan() {
        SearchTermExpression term =
                assertInstanceOf(
                        SearchTermExpression.class,
                        parse("((urgent))")
                );

        assertSpan(
                term,
                0,
                10
        );
    }

    @Test
    void shouldPreserveChildSpansInsideParenthesizedExpression() {
        SearchBinaryExpression binary =
                assertInstanceOf(
                        SearchBinaryExpression.class,
                        parse(
                                "(urgent OR delayed)"
                        )
                );

        assertAll(
                () -> assertSpan(
                        binary,
                        0,
                        19
                ),
                () -> assertSpan(
                        binary.left(),
                        1,
                        7
                ),
                () -> assertSpan(
                        binary.right(),
                        11,
                        18
                )
        );
    }

    @Test
    void shouldParseRealisticNestedExpression() {
        String input =
                "(urgent OR \"database failure\") "
                        + "AND NOT archived delayed";

        SearchBinaryExpression root =
                assertInstanceOf(
                        SearchBinaryExpression.class,
                        parse(input)
                );

        SearchBinaryExpression precedingAnd =
                assertInstanceOf(
                        SearchBinaryExpression.class,
                        root.left()
                );

        SearchBinaryExpression groupedOr =
                assertInstanceOf(
                        SearchBinaryExpression.class,
                        precedingAnd.left()
                );

        SearchNotExpression notArchived =
                assertInstanceOf(
                        SearchNotExpression.class,
                        precedingAnd.right()
                );

        SearchPhraseExpression phrase =
                assertInstanceOf(
                        SearchPhraseExpression.class,
                        groupedOr.right()
                );

        assertAll(
                () -> assertEquals(
                        SearchBinaryOperator.AND,
                        root.operator()
                ),
                () -> assertEquals(
                        SearchBinaryOperator.AND,
                        precedingAnd.operator()
                ),
                () -> assertEquals(
                        SearchBinaryOperator.OR,
                        groupedOr.operator()
                ),
                () -> assertTerm(
                        groupedOr.left(),
                        "urgent"
                ),
                () -> assertEquals(
                        "database failure",
                        phrase.phrase()
                ),
                () -> assertTerm(
                        notArchived.operand(),
                        "archived"
                ),
                () -> assertTerm(
                        root.right(),
                        "delayed"
                ),
                () -> assertSpan(
                        groupedOr,
                        0,
                        30
                ),
                () -> assertSpan(
                        precedingAnd,
                        0,
                        47
                ),
                () -> assertSpan(
                        root,
                        0,
                        input.length()
                )
        );
    }

    @Test
    void shouldProduceEquivalentTreeAcrossRepeatedParseCalls() {
        SearchParser parser =
                new SearchParser(
                        "urgent AND NOT archived"
                );

        SearchExpression first =
                parser.parse();

        SearchExpression second =
                parser.parse();

        assertAll(
                () -> assertEquals(
                        first,
                        second
                ),
                () -> assertNotSame(
                        first,
                        second
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("parserErrorCases")
    void shouldRejectMalformedExpression(
            String description,
            String input,
            String expectedMessage,
            int expectedPosition
    ) {
        SearchParserException exception =
                assertThrows(
                        SearchParserException.class,
                        () -> parse(input)
                );

        assertAll(
                () -> assertEquals(
                        expectedPosition,
                        exception.position()
                ),
                () -> assertEquals(
                        expectedMessage
                                + " at position "
                                + expectedPosition
                                + ".",
                        exception.getMessage()
                )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "and",
            "And",
            "aNd",
            "or",
            "Or",
            "not",
            "Not"
    })
    void shouldRejectIncorrectlyCasedOperatorAtExpressionStart(
            String operator
    ) {
        String expected =
                operator.toUpperCase(
                        java.util.Locale.ROOT
                );

        SearchParserException exception =
                assertThrows(
                        SearchParserException.class,
                        () -> parse(
                                operator + " urgent"
                        )
                );

        assertAll(
                () -> assertEquals(
                        0,
                        exception.position()
                ),
                () -> assertEquals(
                        "Search operator '"
                                + operator
                                + "' must be written as '"
                                + expected
                                + "' at position 0.",
                        exception.getMessage()
                )
        );
    }

    @Test
    void shouldRejectIncorrectlyCasedOperatorInMiddle() {
        SearchParserException exception =
                assertThrows(
                        SearchParserException.class,
                        () -> parse(
                                "urgent and delayed"
                        )
                );

        assertAll(
                () -> assertEquals(
                        7,
                        exception.position()
                ),
                () -> assertEquals(
                        "Search operator 'and' must be written as 'AND' "
                                + "at position 7.",
                        exception.getMessage()
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("adjacentExpressionCases")
    void shouldRejectImplicitAndWithoutWhitespace(
            String description,
            String input,
            int expectedPosition
    ) {
        SearchParserException exception =
                assertThrows(
                        SearchParserException.class,
                        () -> parse(input)
                );

        assertAll(
                () -> assertEquals(
                        expectedPosition,
                        exception.position()
                ),
                () -> assertEquals(
                        "Expected whitespace or AND between search expressions "
                                + "at position "
                                + expectedPosition
                                + ".",
                        exception.getMessage()
                )
        );
    }

    private static SearchExpression parse(
            String input
    ) {
        return new SearchParser(input).parse();
    }

    private static void assertTerm(
            SearchExpression expression,
            String expectedTerm
    ) {
        SearchTermExpression term =
                assertInstanceOf(
                        SearchTermExpression.class,
                        expression
                );

        assertEquals(
                expectedTerm,
                term.term()
        );
    }

    private static void assertSpan(
            SearchExpression expression,
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

    private static Stream<Arguments> binaryOperatorCases() {
        return Stream.of(
                Arguments.of(
                        "explicit AND",
                        "urgent AND delayed",
                        SearchBinaryOperator.AND,
                        "urgent",
                        "delayed"
                ),
                Arguments.of(
                        "explicit OR",
                        "urgent OR delayed",
                        SearchBinaryOperator.OR,
                        "urgent",
                        "delayed"
                )
        );
    }

    private static Stream<Arguments> implicitAndCases() {
        return Stream.of(
                Arguments.of(
                        "term followed by term",
                        "urgent delayed"
                ),
                Arguments.of(
                        "term followed by phrase",
                        "urgent \"database failure\""
                ),
                Arguments.of(
                        "phrase followed by term",
                        "\"database failure\" urgent"
                ),
                Arguments.of(
                        "term followed by parenthesized expression",
                        "urgent (delayed OR archived)"
                ),
                Arguments.of(
                        "parenthesized expression followed by term",
                        "(urgent OR delayed) archived"
                ),
                Arguments.of(
                        "NOT expression followed by term",
                        "NOT urgent delayed"
                ),
                Arguments.of(
                        "tab-separated expressions",
                        "urgent\tdelayed"
                ),
                Arguments.of(
                        "newline-separated expressions",
                        "urgent\ndelayed"
                )
        );
    }

    private static Stream<Arguments> parserErrorCases() {
        return Stream.of(
                Arguments.of(
                        "empty input",
                        "",
                        "Search expression cannot be empty",
                        0
                ),
                Arguments.of(
                        "blank input",
                        "   ",
                        "Search expression cannot be empty",
                        3
                ),
                Arguments.of(
                        "AND at beginning",
                        "AND urgent",
                        "Binary search operator 'AND' cannot begin an expression",
                        0
                ),
                Arguments.of(
                        "OR at beginning",
                        "OR urgent",
                        "Binary search operator 'OR' cannot begin an expression",
                        0
                ),
                Arguments.of(
                        "trailing AND",
                        "urgent AND",
                        "Expected a search expression after AND",
                        10
                ),
                Arguments.of(
                        "trailing OR",
                        "urgent OR",
                        "Expected a search expression after OR",
                        9
                ),
                Arguments.of(
                        "standalone NOT",
                        "NOT",
                        "Expected a search expression after NOT",
                        3
                ),
                Arguments.of(
                        "operator before closing parenthesis",
                        "urgent AND )",
                        "Expected a search expression after AND before closing parenthesis",
                        11
                ),
                Arguments.of(
                        "empty parentheses",
                        "()",
                        "Search parentheses cannot be empty",
                        1
                ),
                Arguments.of(
                        "opening parenthesis only",
                        "(",
                        "Expected a search expression after opening parenthesis",
                        1
                ),
                Arguments.of(
                        "missing closing parenthesis",
                        "(urgent",
                        "Expected ')' to close the parenthesis opened at position 0",
                        7
                ),
                Arguments.of(
                        "unexpected closing parenthesis at end",
                        "urgent)",
                        "Unexpected closing parenthesis",
                        6
                ),
                Arguments.of(
                        "unexpected closing parenthesis at beginning",
                        ")urgent",
                        "Unexpected closing parenthesis",
                        0
                ),
                Arguments.of(
                        "AND followed by OR",
                        "urgent AND OR delayed",
                        "Expected a search expression after AND but found 'OR'",
                        11
                ),
                Arguments.of(
                        "OR followed by AND",
                        "urgent OR AND delayed",
                        "Expected a search expression after OR but found 'AND'",
                        10
                ),
                Arguments.of(
                        "NOT followed by OR",
                        "NOT OR urgent",
                        "Expected a search expression after NOT but found 'OR'",
                        4
                )
        );
    }

    private static Stream<Arguments> adjacentExpressionCases() {
        return Stream.of(
                Arguments.of(
                        "term followed immediately by phrase",
                        "urgent\"failure\"",
                        6
                ),
                Arguments.of(
                        "group followed immediately by group",
                        "(urgent)(delayed)",
                        8
                ),
                Arguments.of(
                        "phrase followed immediately by phrase",
                        "\"urgent\"\"delayed\"",
                        8
                ),
                Arguments.of(
                        "term followed immediately by group",
                        "urgent(delayed)",
                        6
                )
        );
    }
}