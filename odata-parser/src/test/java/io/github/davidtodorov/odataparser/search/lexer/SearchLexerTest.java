package io.github.davidtodorov.odataparser.search.lexer;

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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchLexerTest {

    @Test
    void shouldRejectNullInput() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new SearchLexer(null)
                );

        assertEquals(
                "Search input cannot be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldTokenizeEmptyInput() {
        List<SearchToken> tokens =
                tokenize("");

        assertAll(
                () -> assertEquals(
                        1,
                        tokens.size()
                ),
                () -> assertToken(
                        tokens.getFirst(),
                        SearchTokenType.END_OF_INPUT,
                        "",
                        "",
                        0,
                        0
                )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            " ",
            "\t",
            "\r",
            "\n",
            "\f",
            " \t\r\n\f "
    })
    void shouldIgnoreWhitespace(
            String input
    ) {
        List<SearchToken> tokens =
                tokenize(input);

        assertAll(
                () -> assertEquals(
                        1,
                        tokens.size()
                ),
                () -> assertToken(
                        tokens.getFirst(),
                        SearchTokenType.END_OF_INPUT,
                        "",
                        "",
                        input.length(),
                        input.length()
                )
        );
    }

    @Test
    void shouldTokenizeSingleTerm() {
        List<SearchToken> tokens =
                tokenize("urgent");

        assertAll(
                () -> assertEquals(
                        2,
                        tokens.size()
                ),
                () -> assertToken(
                        tokens.get(0),
                        SearchTokenType.TERM,
                        "urgent",
                        "urgent",
                        0,
                        6
                ),
                () -> assertToken(
                        tokens.get(1),
                        SearchTokenType.END_OF_INPUT,
                        "",
                        "",
                        6,
                        6
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("operatorCases")
    void shouldRecognizeUppercaseOperators(
            String description,
            String input,
            SearchTokenType expectedType
    ) {
        List<SearchToken> tokens =
                tokenize(input);

        assertToken(
                tokens.getFirst(),
                expectedType,
                input,
                input,
                0,
                input.length()
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
    void shouldTreatIncorrectlyCasedOperatorsAsTerms(
            String input
    ) {
        SearchToken token =
                tokenize(input).getFirst();

        assertToken(
                token,
                SearchTokenType.TERM,
                input,
                input,
                0,
                input.length()
        );
    }

    @Test
    void shouldTokenizeCompleteExpression() {
        String input =
                "urgent AND \"database failure\" OR NOT archived";

        List<SearchToken> tokens =
                tokenize(input);

        assertAll(
                () -> assertEquals(
                        7,
                        tokens.size()
                ),
                () -> assertToken(
                        tokens.get(0),
                        SearchTokenType.TERM,
                        "urgent",
                        "urgent",
                        0,
                        6
                ),
                () -> assertToken(
                        tokens.get(1),
                        SearchTokenType.AND,
                        "AND",
                        "AND",
                        7,
                        10
                ),
                () -> assertToken(
                        tokens.get(2),
                        SearchTokenType.PHRASE,
                        "\"database failure\"",
                        "database failure",
                        11,
                        29
                ),
                () -> assertToken(
                        tokens.get(3),
                        SearchTokenType.OR,
                        "OR",
                        "OR",
                        30,
                        32
                ),
                () -> assertToken(
                        tokens.get(4),
                        SearchTokenType.NOT,
                        "NOT",
                        "NOT",
                        33,
                        36
                ),
                () -> assertToken(
                        tokens.get(5),
                        SearchTokenType.TERM,
                        "archived",
                        "archived",
                        37,
                        45
                ),
                () -> assertToken(
                        tokens.get(6),
                        SearchTokenType.END_OF_INPUT,
                        "",
                        "",
                        45,
                        45
                )
        );
    }

    @Test
    void shouldTokenizeParentheses() {
        String input =
                "(urgent OR delayed) AND NOT archived";

        List<SearchToken> tokens =
                tokenize(input);

        assertAll(
                () -> assertToken(
                        tokens.get(0),
                        SearchTokenType.LEFT_PARENTHESIS,
                        "(",
                        "(",
                        0,
                        1
                ),
                () -> assertToken(
                        tokens.get(1),
                        SearchTokenType.TERM,
                        "urgent",
                        "urgent",
                        1,
                        7
                ),
                () -> assertToken(
                        tokens.get(2),
                        SearchTokenType.OR,
                        "OR",
                        "OR",
                        8,
                        10
                ),
                () -> assertToken(
                        tokens.get(3),
                        SearchTokenType.TERM,
                        "delayed",
                        "delayed",
                        11,
                        18
                ),
                () -> assertToken(
                        tokens.get(4),
                        SearchTokenType.RIGHT_PARENTHESIS,
                        ")",
                        ")",
                        18,
                        19
                ),
                () -> assertToken(
                        tokens.get(5),
                        SearchTokenType.AND,
                        "AND",
                        "AND",
                        20,
                        23
                ),
                () -> assertToken(
                        tokens.get(6),
                        SearchTokenType.NOT,
                        "NOT",
                        "NOT",
                        24,
                        27
                ),
                () -> assertToken(
                        tokens.get(7),
                        SearchTokenType.TERM,
                        "archived",
                        "archived",
                        28,
                        36
                ),
                () -> assertToken(
                        tokens.get(8),
                        SearchTokenType.END_OF_INPUT,
                        "",
                        "",
                        36,
                        36
                )
        );
    }

    @Test
    void shouldTokenizeAdjacentParenthesesWithoutWhitespace() {
        List<SearchToken> tokens =
                tokenize("(urgent)");

        assertEquals(
                List.of(
                        SearchTokenType.LEFT_PARENTHESIS,
                        SearchTokenType.TERM,
                        SearchTokenType.RIGHT_PARENTHESIS,
                        SearchTokenType.END_OF_INPUT
                ),
                tokens.stream()
                        .map(SearchToken::type)
                        .toList()
        );
    }

    @Test
    void shouldTokenizeAdjacentTermAndPhraseSeparately() {
        List<SearchToken> tokens =
                tokenize("urgent\"database failure\"");

        assertAll(
                () -> assertEquals(
                        List.of(
                                SearchTokenType.TERM,
                                SearchTokenType.PHRASE,
                                SearchTokenType.END_OF_INPUT
                        ),
                        tokens.stream()
                                .map(SearchToken::type)
                                .toList()
                ),
                () -> assertEquals(
                        "urgent",
                        tokens.get(0).value()
                ),
                () -> assertEquals(
                        "database failure",
                        tokens.get(1).value()
                )
        );
    }

    @Test
    void shouldTokenizeQuotedPhrase() {
        String input =
                "\"database failure\"";

        SearchToken token =
                tokenize(input).getFirst();

        assertToken(
                token,
                SearchTokenType.PHRASE,
                input,
                "database failure",
                0,
                input.length()
        );
    }

    @Test
    void shouldDecodeEscapedQuotationMarksInPhrase() {
        String input =
                "\"say \\\"hello\\\"\"";

        SearchToken token =
                tokenize(input).getFirst();

        assertToken(
                token,
                SearchTokenType.PHRASE,
                input,
                "say \"hello\"",
                0,
                input.length()
        );
    }

    @Test
    void shouldDecodeEscapedBackslashInPhrase() {
        String input =
                "\"C:\\\\temp\"";

        SearchToken token =
                tokenize(input).getFirst();

        assertToken(
                token,
                SearchTokenType.PHRASE,
                input,
                "C:\\temp",
                0,
                input.length()
        );
    }

    @Test
    void shouldPreserveWhitespaceInsidePhraseValue() {
        String input =
                "\"database   failure\"";

        SearchToken token =
                tokenize(input).getFirst();

        assertEquals(
                "database   failure",
                token.value()
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "case-123",
            "owner/name",
            "email@example.com",
            "price:100",
            "file.txt",
            "C++",
            "one,two",
            "question?"
    })
    void shouldAllowSupportedPunctuationInsideUnquotedTerms(
            String input
    ) {
        SearchToken token =
                tokenize(input).getFirst();

        assertToken(
                token,
                SearchTokenType.TERM,
                input,
                input,
                0,
                input.length()
        );
    }

    @Test
    void shouldPreserveUnicodeTerms() {
        String input =
                "Überprüfung";

        SearchToken token =
                tokenize(input).getFirst();

        assertEquals(
                input,
                token.value()
        );
    }

    @Test
    void shouldReturnImmutableTokenList() {
        List<SearchToken> tokens =
                tokenize("urgent");

        assertThrows(
                UnsupportedOperationException.class,
                () -> tokens.add(
                        SearchToken.endOfInput(6)
                )
        );
    }

    @Test
    void shouldResetPositionForRepeatedTokenization() {
        SearchLexer lexer =
                new SearchLexer(
                        "urgent AND delayed"
                );

        List<SearchToken> first =
                lexer.tokenize();

        List<SearchToken> second =
                lexer.tokenize();

        assertAll(
                () -> assertEquals(
                        first,
                        second
                ),
                () -> assertFalse(
                        first == second
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("unsupportedUnquotedCharacterCases")
    void shouldRejectUnsupportedUnquotedCharacter(
            String description,
            String input,
            char unsupportedCharacter,
            int expectedPosition
    ) {
        SearchLexerException exception =
                assertThrows(
                        SearchLexerException.class,
                        () -> tokenize(input)
                );

        assertAll(
                () -> assertEquals(
                        expectedPosition,
                        exception.position()
                ),
                () -> assertEquals(
                        "Unsupported character '"
                                + unsupportedCharacter
                                + "' in unquoted search term "
                                + "at position "
                                + expectedPosition
                                + ".",
                        exception.getMessage()
                )
        );
    }

    @Test
    void shouldRejectIsoControlCharacterInsideTerm() {
        String input =
                "urgent\u0000delayed";

        SearchLexerException exception =
                assertThrows(
                        SearchLexerException.class,
                        () -> tokenize(input)
                );

        assertAll(
                () -> assertEquals(
                        6,
                        exception.position()
                ),
                () -> assertTrue(
                        exception.getMessage().contains(
                                "Unsupported character"
                        )
                )
        );
    }

    @Test
    void shouldRejectUnterminatedPhrase() {
        SearchLexerException exception =
                assertThrows(
                        SearchLexerException.class,
                        () -> tokenize(
                                "urgent \"database failure"
                        )
                );

        assertAll(
                () -> assertEquals(
                        7,
                        exception.position()
                ),
                () -> assertEquals(
                        "Unterminated search phrase at position 7.",
                        exception.getMessage()
                )
        );
    }

    @Test
    void shouldRejectUnterminatedEscapeSequence() {
        String input =
                "\"abc\\";

        SearchLexerException exception =
                assertThrows(
                        SearchLexerException.class,
                        () -> tokenize(input)
                );

        assertAll(
                () -> assertEquals(
                        4,
                        exception.position()
                ),
                () -> assertEquals(
                        "Unterminated escape sequence in search phrase "
                                + "at position 4.",
                        exception.getMessage()
                )
        );
    }

    @ParameterizedTest
    @ValueSource(chars = {
            'n',
            't',
            'r',
            '/',
            'x'
    })
    void shouldRejectUnsupportedPhraseEscapeSequence(
            char escapedCharacter
    ) {
        String input =
                "\"bad\\"
                        + escapedCharacter
                        + "\"";

        SearchLexerException exception =
                assertThrows(
                        SearchLexerException.class,
                        () -> tokenize(input)
                );

        assertAll(
                () -> assertEquals(
                        4,
                        exception.position()
                ),
                () -> assertEquals(
                        "Unsupported search phrase escape sequence '\\"
                                + escapedCharacter
                                + "' at position 4.",
                        exception.getMessage()
                )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "\"\"",
            "\" \"",
            "\"\t\"",
            "\"\r\n\""
    })
    void shouldRejectEmptyOrBlankPhrase(
            String input
    ) {
        SearchLexerException exception =
                assertThrows(
                        SearchLexerException.class,
                        () -> tokenize(input)
                );

        assertAll(
                () -> assertEquals(
                        0,
                        exception.position()
                ),
                () -> assertEquals(
                        "Search phrase cannot be empty or blank "
                                + "at position 0.",
                        exception.getMessage()
                )
        );
    }

    private static List<SearchToken> tokenize(
            String input
    ) {
        return new SearchLexer(input).tokenize();
    }

    private static void assertToken(
            SearchToken token,
            SearchTokenType expectedType,
            String expectedLexeme,
            String expectedValue,
            int expectedStart,
            int expectedEnd
    ) {
        assertAll(
                () -> assertEquals(
                        expectedType,
                        token.type()
                ),
                () -> assertEquals(
                        expectedLexeme,
                        token.lexeme()
                ),
                () -> assertEquals(
                        expectedValue,
                        token.value()
                ),
                () -> assertEquals(
                        expectedStart,
                        token.sourceSpan().start()
                ),
                () -> assertEquals(
                        expectedEnd,
                        token.sourceSpan().end()
                )
        );
    }

    private static Stream<Arguments> operatorCases() {
        return Stream.of(
                Arguments.of(
                        "AND operator",
                        "AND",
                        SearchTokenType.AND
                ),
                Arguments.of(
                        "OR operator",
                        "OR",
                        SearchTokenType.OR
                ),
                Arguments.of(
                        "NOT operator",
                        "NOT",
                        SearchTokenType.NOT
                )
        );
    }

    private static Stream<Arguments>
    unsupportedUnquotedCharacterCases() {
        return Stream.of(
                Arguments.of(
                        "ampersand",
                        "urgent&delayed",
                        '&',
                        6
                ),
                Arguments.of(
                        "vertical bar",
                        "urgent|delayed",
                        '|',
                        6
                ),
                Arguments.of(
                        "exclamation mark",
                        "!urgent",
                        '!',
                        0
                ),
                Arguments.of(
                        "backslash",
                        "urgent\\delayed",
                        '\\',
                        6
                )
        );
    }
}