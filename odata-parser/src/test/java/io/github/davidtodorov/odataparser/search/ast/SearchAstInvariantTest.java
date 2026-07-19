package io.github.davidtodorov.odataparser.search.ast;

import io.github.davidtodorov.odataparser.common.metadata.SourceSpan;
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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchAstInvariantTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("binaryOperatorCases")
    void shouldExposeBinaryOperatorProperties(
            String description,
            SearchBinaryOperator operator,
            String expectedKeyword
    ) {
        assertAll(
                () -> assertEquals(
                        expectedKeyword,
                        operator.keyword()
                ),
                () -> assertSame(
                        operator,
                        SearchBinaryOperator.fromKeyword(
                                expectedKeyword
                        )
                )
        );
    }

    @Test
    void shouldRejectNullBinaryOperatorKeyword() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> SearchBinaryOperator.fromKeyword(null)
                );

        assertEquals(
                "Search operator keyword cannot be null",
                exception.getMessage()
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "and",
            "or",
            "And",
            "Or",
            "NOT",
            "XOR"
    })
    void shouldRejectUnsupportedBinaryOperatorKeyword(
            String keyword
    ) {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> SearchBinaryOperator.fromKeyword(
                                keyword
                        )
                );

        assertEquals(
                "Unsupported search binary operator '"
                        + keyword
                        + "'. Expected 'AND' or 'OR'",
                exception.getMessage()
        );
    }

    @Test
    void shouldCreateTermWithUnknownSourceSpan() {
        SearchTermExpression term =
                new SearchTermExpression(
                        "urgent"
                );

        assertAll(
                () -> assertEquals(
                        "urgent",
                        term.term()
                ),
                () -> assertTrue(
                        term.sourceSpan().isUnknown()
                ),
                () -> assertTrue(
                        term.children().isEmpty()
                ),
                () -> assertTrue(
                        term.isLeaf()
                )
        );
    }

    @Test
    void shouldPreserveExplicitTermSourceSpan() {
        SourceSpan sourceSpan =
                new SourceSpan(
                        3,
                        9
                );

        SearchTermExpression term =
                new SearchTermExpression(
                        "urgent",
                        sourceSpan
                );

        assertSame(
                sourceSpan,
                term.sourceSpan()
        );
    }

    @Test
    void shouldAllowPunctuationInsideTerm() {
        SearchTermExpression term =
                new SearchTermExpression(
                        "email@example.com"
                );

        assertEquals(
                "email@example.com",
                term.term()
        );
    }

    @Test
    void shouldRejectNullTerm() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new SearchTermExpression(
                                null,
                                SourceSpan.unknown()
                        )
                );

        assertEquals(
                "Search term cannot be null",
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
    void shouldRejectBlankTerm(
            String term
    ) {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new SearchTermExpression(
                                term
                        )
                );

        assertEquals(
                "Search term cannot be blank",
                exception.getMessage()
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "database failure",
            "urgent\tdelayed",
            "urgent\ndelayed",
            "urgent\rdelayed"
    })
    void shouldRejectWhitespaceInsideUnquotedTerm(
            String term
    ) {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new SearchTermExpression(
                                term
                        )
                );

        assertEquals(
                "An unquoted search term cannot contain whitespace: '"
                        + term
                        + "'",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullTermSourceSpan() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new SearchTermExpression(
                                "urgent",
                                null
                        )
                );

        assertEquals(
                "Search term source span cannot be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldCreatePhraseWithGeneratedRawTextAndUnknownSpan() {
        SearchPhraseExpression phrase =
                new SearchPhraseExpression(
                        "database failure"
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
                () -> assertTrue(
                        phrase.sourceSpan().isUnknown()
                ),
                () -> assertTrue(
                        phrase.children().isEmpty()
                ),
                () -> assertTrue(
                        phrase.isLeaf()
                )
        );
    }

    @Test
    void shouldCreatePhraseWithGeneratedRawTextAndExplicitSpan() {
        SourceSpan sourceSpan =
                new SourceSpan(
                        4,
                        22
                );

        SearchPhraseExpression phrase =
                new SearchPhraseExpression(
                        "database failure",
                        sourceSpan
                );

        assertAll(
                () -> assertEquals(
                        "\"database failure\"",
                        phrase.rawText()
                ),
                () -> assertSame(
                        sourceSpan,
                        phrase.sourceSpan()
                )
        );
    }

    @Test
    void shouldPreserveExplicitPhraseRawText() {
        SourceSpan sourceSpan =
                new SourceSpan(
                        0,
                        15
                );

        SearchPhraseExpression phrase =
                new SearchPhraseExpression(
                        "say \"hello\"",
                        "\"say \\\"hello\\\"\"",
                        sourceSpan
                );

        assertAll(
                () -> assertEquals(
                        "say \"hello\"",
                        phrase.phrase()
                ),
                () -> assertEquals(
                        "\"say \\\"hello\\\"\"",
                        phrase.rawText()
                ),
                () -> assertSame(
                        sourceSpan,
                        phrase.sourceSpan()
                )
        );
    }

    @Test
    void shouldRejectNullPhrase() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new SearchPhraseExpression(
                                null,
                                "\"phrase\"",
                                SourceSpan.unknown()
                        )
                );

        assertEquals(
                "Search phrase cannot be null",
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
    void shouldRejectBlankPhrase(
            String phrase
    ) {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new SearchPhraseExpression(
                                phrase,
                                "\"phrase\"",
                                SourceSpan.unknown()
                        )
                );

        assertEquals(
                "Search phrase cannot be blank",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullPhraseRawText() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new SearchPhraseExpression(
                                "phrase",
                                null,
                                SourceSpan.unknown()
                        )
                );

        assertEquals(
                "Search phrase raw text cannot be null",
                exception.getMessage()
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "phrase",
            "\"phrase",
            "phrase\"",
            "'phrase'",
            "\""
    })
    void shouldRejectPhraseRawTextWithoutDoubleQuotePair(
            String rawText
    ) {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new SearchPhraseExpression(
                                "phrase",
                                rawText,
                                SourceSpan.unknown()
                        )
                );

        assertEquals(
                "Search phrase raw text must be enclosed in double quotes",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullPhraseSourceSpan() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new SearchPhraseExpression(
                                "phrase",
                                "\"phrase\"",
                                null
                        )
                );

        assertEquals(
                "Search phrase source span cannot be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldCreateNotExpressionWithUnknownSpan() {
        SearchTermExpression operand =
                new SearchTermExpression(
                        "archived"
                );

        SearchNotExpression expression =
                new SearchNotExpression(
                        operand
                );

        assertAll(
                () -> assertSame(
                        operand,
                        expression.operand()
                ),
                () -> assertTrue(
                        expression.sourceSpan().isUnknown()
                ),
                () -> assertEquals(
                        List.of(operand),
                        expression.children()
                ),
                () -> assertFalse(
                        expression.isLeaf()
                ),
                () -> assertThrows(
                        UnsupportedOperationException.class,
                        () -> expression.children().clear()
                )
        );
    }

    @Test
    void shouldPreserveExplicitNotSourceSpan() {
        SearchExpression operand =
                new SearchTermExpression(
                        "archived",
                        new SourceSpan(
                                4,
                                12
                        )
                );

        SourceSpan sourceSpan =
                new SourceSpan(
                        0,
                        12
                );

        SearchNotExpression expression =
                new SearchNotExpression(
                        operand,
                        sourceSpan
                );

        assertSame(
                sourceSpan,
                expression.sourceSpan()
        );
    }

    @Test
    void shouldRejectNullNotOperand() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new SearchNotExpression(
                                null,
                                SourceSpan.unknown()
                        )
                );

        assertEquals(
                "Search NOT operand cannot be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullNotSourceSpan() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new SearchNotExpression(
                                new SearchTermExpression(
                                        "archived"
                                ),
                                null
                        )
                );

        assertEquals(
                "Search NOT source span cannot be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldCreateBinaryExpressionWithUnknownSpan() {
        SearchExpression left =
                new SearchTermExpression(
                        "urgent"
                );

        SearchExpression right =
                new SearchTermExpression(
                        "delayed"
                );

        SearchBinaryExpression expression =
                new SearchBinaryExpression(
                        left,
                        SearchBinaryOperator.AND,
                        right
                );

        assertAll(
                () -> assertSame(
                        left,
                        expression.left()
                ),
                () -> assertEquals(
                        SearchBinaryOperator.AND,
                        expression.operator()
                ),
                () -> assertSame(
                        right,
                        expression.right()
                ),
                () -> assertTrue(
                        expression.sourceSpan().isUnknown()
                ),
                () -> assertEquals(
                        List.of(left, right),
                        expression.children()
                ),
                () -> assertFalse(
                        expression.isLeaf()
                ),
                () -> assertThrows(
                        UnsupportedOperationException.class,
                        () -> expression.children().add(
                                new SearchTermExpression(
                                        "other"
                                )
                        )
                )
        );
    }

    @Test
    void shouldPreserveExplicitBinarySourceSpan() {
        SearchExpression left =
                new SearchTermExpression(
                        "urgent",
                        new SourceSpan(
                                0,
                                6
                        )
                );

        SearchExpression right =
                new SearchTermExpression(
                        "delayed",
                        new SourceSpan(
                                11,
                                18
                        )
                );

        SourceSpan sourceSpan =
                new SourceSpan(
                        0,
                        18
                );

        SearchBinaryExpression expression =
                new SearchBinaryExpression(
                        left,
                        SearchBinaryOperator.OR,
                        right,
                        sourceSpan
                );

        assertSame(
                sourceSpan,
                expression.sourceSpan()
        );
    }

    @Test
    void shouldExposeBinaryChildrenInOperandOrder() {
        SearchExpression left =
                new SearchTermExpression(
                        "urgent"
                );

        SearchExpression right =
                new SearchPhraseExpression(
                        "database failure"
                );

        SearchBinaryExpression expression =
                new SearchBinaryExpression(
                        left,
                        SearchBinaryOperator.OR,
                        right
                );

        assertAll(
                () -> assertSame(
                        left,
                        expression.children().getFirst()
                ),
                () -> assertSame(
                        right,
                        expression.children().getLast()
                )
        );
    }

    @Test
    void shouldRejectNullBinaryLeftOperand() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new SearchBinaryExpression(
                                null,
                                SearchBinaryOperator.AND,
                                new SearchTermExpression(
                                        "delayed"
                                ),
                                SourceSpan.unknown()
                        )
                );

        assertEquals(
                "Left search expression cannot be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullBinaryOperator() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new SearchBinaryExpression(
                                new SearchTermExpression(
                                        "urgent"
                                ),
                                null,
                                new SearchTermExpression(
                                        "delayed"
                                ),
                                SourceSpan.unknown()
                        )
                );

        assertEquals(
                "Search binary operator cannot be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullBinaryRightOperand() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new SearchBinaryExpression(
                                new SearchTermExpression(
                                        "urgent"
                                ),
                                SearchBinaryOperator.AND,
                                null,
                                SourceSpan.unknown()
                        )
                );

        assertEquals(
                "Right search expression cannot be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullBinarySourceSpan() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new SearchBinaryExpression(
                                new SearchTermExpression(
                                        "urgent"
                                ),
                                SearchBinaryOperator.AND,
                                new SearchTermExpression(
                                        "delayed"
                                ),
                                null
                        )
                );

        assertEquals(
                "Search binary expression source span cannot be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldDispatchEveryExpressionToCorrectVisitorMethod() {
        RecordingVisitor visitor =
                new RecordingVisitor();

        SearchExpression term =
                new SearchTermExpression(
                        "urgent"
                );

        SearchExpression phrase =
                new SearchPhraseExpression(
                        "database failure"
                );

        SearchExpression not =
                new SearchNotExpression(
                        term
                );

        SearchExpression binary =
                new SearchBinaryExpression(
                        term,
                        SearchBinaryOperator.AND,
                        phrase
                );

        assertAll(
                () -> assertEquals(
                        "term",
                        term.accept(visitor)
                ),
                () -> assertEquals(
                        "phrase",
                        phrase.accept(visitor)
                ),
                () -> assertEquals(
                        "not",
                        not.accept(visitor)
                ),
                () -> assertEquals(
                        "binary",
                        binary.accept(visitor)
                )
        );
    }

    @Test
    void shouldRejectNullVisitorForEveryExpressionType() {
        List<SearchExpression> expressions =
                List.of(
                        new SearchTermExpression(
                                "urgent"
                        ),
                        new SearchPhraseExpression(
                                "database failure"
                        ),
                        new SearchNotExpression(
                                new SearchTermExpression(
                                        "archived"
                                )
                        ),
                        new SearchBinaryExpression(
                                new SearchTermExpression(
                                        "urgent"
                                ),
                                SearchBinaryOperator.OR,
                                new SearchTermExpression(
                                        "delayed"
                                )
                        )
                );

        for (SearchExpression expression : expressions) {
            NullPointerException exception =
                    assertThrows(
                            NullPointerException.class,
                            () -> expression.accept(null)
                    );

            assertEquals(
                    "Search expression visitor cannot be null",
                    exception.getMessage()
            );
        }
    }

    private static Stream<Arguments> binaryOperatorCases() {
        return Stream.of(
                Arguments.of(
                        "AND operator",
                        SearchBinaryOperator.AND,
                        "AND"
                ),
                Arguments.of(
                        "OR operator",
                        SearchBinaryOperator.OR,
                        "OR"
                )
        );
    }

    private static final class RecordingVisitor
            implements SearchExpressionVisitor<String> {

        @Override
        public String visitTermExpression(
                SearchTermExpression expression
        ) {
            return "term";
        }

        @Override
        public String visitPhraseExpression(
                SearchPhraseExpression expression
        ) {
            return "phrase";
        }

        @Override
        public String visitNotExpression(
                SearchNotExpression expression
        ) {
            return "not";
        }

        @Override
        public String visitBinaryExpression(
                SearchBinaryExpression expression
        ) {
            return "binary";
        }
    }
}