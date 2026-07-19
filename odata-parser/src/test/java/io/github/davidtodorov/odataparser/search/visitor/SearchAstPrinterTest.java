package io.github.davidtodorov.odataparser.search.visitor;

import io.github.davidtodorov.odataparser.common.metadata.SourceSpan;
import io.github.davidtodorov.odataparser.search.ast.SearchBinaryExpression;
import io.github.davidtodorov.odataparser.search.ast.SearchBinaryOperator;
import io.github.davidtodorov.odataparser.search.ast.SearchExpression;
import io.github.davidtodorov.odataparser.search.ast.SearchNotExpression;
import io.github.davidtodorov.odataparser.search.ast.SearchPhraseExpression;
import io.github.davidtodorov.odataparser.search.ast.SearchTermExpression;
import io.github.davidtodorov.odataparser.search.parser.SearchParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchAstPrinterTest {

    private static final String LINE_SEPARATOR =
            System.lineSeparator();

    private final SearchAstPrinter printer =
            new SearchAstPrinter();

    @Test
    void shouldPrintTermWithKnownSourceSpan() {
        SearchExpression expression =
                new SearchTermExpression(
                        "urgent",
                        new SourceSpan(
                                2,
                                8
                        )
                );

        assertEquals(
                "TERM | value=\"urgent\" | span=[2, 8)",
                printer.print(expression)
        );
    }

    @Test
    void shouldPrintTermWithUnknownSourceSpan() {
        SearchExpression expression =
                new SearchTermExpression(
                        "urgent"
                );

        assertEquals(
                "TERM | value=\"urgent\" | span=unknown",
                printer.print(expression)
        );
    }

    @Test
    void shouldEscapeSpecialCharactersInPrintedTermValue() {
        SearchExpression expression =
                new SearchTermExpression(
                        "path\\\"value"
                );

        assertEquals(
                "TERM | value=\"path\\\\\\\"value\" | span=unknown",
                printer.print(expression)
        );
    }

    @Test
    void shouldPrintPhraseWithDecodedValueAndRawText() {
        SearchExpression expression =
                new SearchPhraseExpression(
                        "say \"hello\" at C:\\temp",
                        "\"say \\\"hello\\\" at C:\\\\temp\"",
                        new SourceSpan(
                                0,
                                27
                        )
                );

        assertEquals(
                "PHRASE"
                        + " | value=\"say \\\"hello\\\" at C:\\\\temp\""
                        + " | raw-text=\"say \\\"hello\\\" at C:\\\\temp\""
                        + " | span=[0, 27)",
                printer.print(expression)
        );
    }

    @Test
    void shouldDisplayControlCharactersInPhraseRawText() {
        SearchExpression expression =
                new SearchPhraseExpression(
                        "line one\nline two\tvalue",
                        "\"line one\nline two\tvalue\"",
                        SourceSpan.unknown()
                );

        String output =
                printer.print(expression);

        assertAll(
                () -> assertTrue(
                        output.contains(
                                "value=\"line one\\nline two\\tvalue\""
                        )
                ),
                () -> assertTrue(
                        output.contains(
                                "raw-text=\"line one\\nline two\\tvalue\""
                        )
                ),
                () -> assertTrue(
                        output.endsWith(
                                "span=unknown"
                        )
                )
        );
    }

    @Test
    void shouldPrintNotExpressionWithOperandIndentation() {
        SearchExpression expression =
                new SearchNotExpression(
                        new SearchTermExpression(
                                "archived",
                                new SourceSpan(
                                        4,
                                        12
                                )
                        ),
                        new SourceSpan(
                                0,
                                12
                        )
                );

        String expected =
                String.join(
                        LINE_SEPARATOR,
                        "NOT | span=[0, 12)",
                        "  OPERAND:",
                        "    TERM | value=\"archived\" | span=[4, 12)"
                );

        assertEquals(
                expected,
                printer.print(expression)
        );
    }

    @Test
    void shouldPrintBinaryExpressionWithBothChildren() {
        SearchExpression expression =
                new SearchBinaryExpression(
                        new SearchTermExpression(
                                "urgent",
                                new SourceSpan(
                                        0,
                                        6
                                )
                        ),
                        SearchBinaryOperator.AND,
                        new SearchTermExpression(
                                "delayed",
                                new SourceSpan(
                                        11,
                                        18
                                )
                        ),
                        new SourceSpan(
                                0,
                                18
                        )
                );

        String expected =
                String.join(
                        LINE_SEPARATOR,
                        "BINARY | operator=AND | keyword=AND | span=[0, 18)",
                        "  LEFT:",
                        "    TERM | value=\"urgent\" | span=[0, 6)",
                        "  RIGHT:",
                        "    TERM | value=\"delayed\" | span=[11, 18)"
                );

        assertEquals(
                expected,
                printer.print(expression)
        );
    }

    @Test
    void shouldPrintNestedExpressionInTreeOrder() {
        String input =
                "(urgent OR \"database failure\") "
                        + "AND NOT archived";

        String output =
                printer.print(
                        new SearchParser(input).parse()
                );

        assertContains(
                output,
                "BINARY | operator=AND | keyword=AND | span=[0, 47)",
                "BINARY | operator=OR | keyword=OR | span=[0, 30)",
                "TERM | value=\"urgent\" | span=[1, 7)",
                "PHRASE | value=\"database failure\""
                        + " | raw-text=\"database failure\""
                        + " | span=[11, 29)",
                "NOT | span=[35, 47)",
                "TERM | value=\"archived\" | span=[39, 47)"
        );

        assertInOrder(
                output,
                "BINARY | operator=AND",
                "BINARY | operator=OR",
                "TERM | value=\"urgent\"",
                "PHRASE | value=\"database failure\"",
                "NOT | span=[35, 47)",
                "TERM | value=\"archived\""
        );
    }

    @Test
    void shouldIndentNestedBinaryAndNotChildrenCorrectly() {
        String output =
                printer.print(
                        new SearchParser(
                                "(urgent OR delayed) AND NOT archived"
                        ).parse()
                );

        assertContains(
                output,
                LINE_SEPARATOR + "  LEFT:"
                        + LINE_SEPARATOR + "    BINARY | operator=OR",
                LINE_SEPARATOR + "      LEFT:"
                        + LINE_SEPARATOR + "        TERM | value=\"urgent\"",
                LINE_SEPARATOR + "      RIGHT:"
                        + LINE_SEPARATOR + "        TERM | value=\"delayed\"",
                LINE_SEPARATOR + "  RIGHT:"
                        + LINE_SEPARATOR + "    NOT",
                LINE_SEPARATOR + "      OPERAND:"
                        + LINE_SEPARATOR + "        TERM | value=\"archived\""
        );
    }

    @Test
    void shouldProduceDeterministicOutputAcrossRepeatedCalls() {
        SearchExpression expression =
                new SearchParser(
                        "urgent OR delayed AND NOT archived"
                ).parse();

        String first =
                printer.print(expression);

        String second =
                printer.print(expression);

        assertEquals(
                first,
                second
        );
    }

    @Test
    void shouldNotLeakOutputBetweenPrintCalls() {
        String first =
                printer.print(
                        new SearchParser(
                                "urgent AND delayed"
                        ).parse()
                );

        String second =
                printer.print(
                        new SearchParser(
                                "NOT archived"
                        ).parse()
                );

        assertAll(
                () -> assertTrue(
                        first.contains(
                                "value=\"urgent\""
                        )
                ),
                () -> assertTrue(
                        first.contains(
                                "value=\"delayed\""
                        )
                ),
                () -> assertFalse(
                        first.contains(
                                "value=\"archived\""
                        )
                ),
                () -> assertTrue(
                        second.contains(
                                "value=\"archived\""
                        )
                ),
                () -> assertFalse(
                        second.contains(
                                "value=\"urgent\""
                        )
                ),
                () -> assertFalse(
                        second.contains(
                                "value=\"delayed\""
                        )
                )
        );
    }

    @Test
    void shouldRejectNullExpressionInPrintMethod() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> printer.print(null)
                );

        assertEquals(
                "Search expression cannot be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullTermExpressionInVisitorMethod() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> printer.visitTermExpression(null)
                );

        assertEquals(
                "Search term expression cannot be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullPhraseExpressionInVisitorMethod() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> printer.visitPhraseExpression(null)
                );

        assertEquals(
                "Search phrase expression cannot be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullNotExpressionInVisitorMethod() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> printer.visitNotExpression(null)
                );

        assertEquals(
                "Search NOT expression cannot be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullBinaryExpressionInVisitorMethod() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> printer.visitBinaryExpression(null)
                );

        assertEquals(
                "Search binary expression cannot be null",
                exception.getMessage()
        );
    }

    private static void assertContains(
            String output,
            String... expectedFragments
    ) {
        for (String expectedFragment : expectedFragments) {
            assertTrue(
                    output.contains(expectedFragment),
                    () -> "Expected output to contain:"
                            + LINE_SEPARATOR
                            + expectedFragment
                            + LINE_SEPARATOR
                            + LINE_SEPARATOR
                            + "Actual output:"
                            + LINE_SEPARATOR
                            + output
            );
        }
    }

    private static void assertInOrder(
            String output,
            String... expectedFragments
    ) {
        int previousPosition = -1;

        for (String expectedFragment : expectedFragments) {
            int currentPosition =
                    output.indexOf(
                            expectedFragment
                    );

            assertTrue(
                    currentPosition >= 0,
                    () -> "Expected output to contain:"
                            + LINE_SEPARATOR
                            + expectedFragment
                            + LINE_SEPARATOR
                            + LINE_SEPARATOR
                            + "Actual output:"
                            + LINE_SEPARATOR
                            + output
            );

            assertTrue(
                    currentPosition > previousPosition,
                    () -> "Expected fragment to appear after the previous fragment:"
                            + LINE_SEPARATOR
                            + expectedFragment
                            + LINE_SEPARATOR
                            + LINE_SEPARATOR
                            + "Actual output:"
                            + LINE_SEPARATOR
                            + output
            );

            previousPosition =
                    currentPosition;
        }
    }
}