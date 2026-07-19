package io.github.davidtodorov.odataparser.search.lexer;

import io.github.davidtodorov.odataparser.common.metadata.SourceSpan;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchTokenTest {

    @Test
    void shouldCreateTermTokenWithLexemeAsValue() {
        SourceSpan sourceSpan =
                new SourceSpan(
                        2,
                        8
                );

        SearchToken token =
                new SearchToken(
                        SearchTokenType.TERM,
                        "urgent",
                        sourceSpan
                );

        assertAll(
                () -> assertEquals(
                        SearchTokenType.TERM,
                        token.type()
                ),
                () -> assertEquals(
                        "urgent",
                        token.lexeme()
                ),
                () -> assertEquals(
                        "urgent",
                        token.value()
                ),
                () -> assertSame(
                        sourceSpan,
                        token.sourceSpan()
                ),
                () -> assertTrue(
                        token.startsExpression()
                ),
                () -> assertTrue(
                        token.endsExpression()
                ),
                () -> assertFalse(
                        token.isEndOfInput()
                )
        );
    }

    @Test
    void shouldCreatePhraseTokenWithDecodedValue() {
        SourceSpan sourceSpan =
                new SourceSpan(
                        0,
                        18
                );

        SearchToken token =
                new SearchToken(
                        SearchTokenType.PHRASE,
                        "\"database failure\"",
                        "database failure",
                        sourceSpan
                );

        assertAll(
                () -> assertEquals(
                        SearchTokenType.PHRASE,
                        token.type()
                ),
                () -> assertEquals(
                        "\"database failure\"",
                        token.lexeme()
                ),
                () -> assertEquals(
                        "database failure",
                        token.value()
                ),
                () -> assertSame(
                        sourceSpan,
                        token.sourceSpan()
                ),
                () -> assertTrue(
                        token.startsExpression()
                ),
                () -> assertTrue(
                        token.endsExpression()
                )
        );
    }

    @Test
    void shouldCreateEndOfInputToken() {
        SearchToken token =
                SearchToken.endOfInput(12);

        assertAll(
                () -> assertEquals(
                        SearchTokenType.END_OF_INPUT,
                        token.type()
                ),
                () -> assertEquals(
                        "",
                        token.lexeme()
                ),
                () -> assertEquals(
                        "",
                        token.value()
                ),
                () -> assertEquals(
                        12,
                        token.sourceSpan().start()
                ),
                () -> assertEquals(
                        12,
                        token.sourceSpan().end()
                ),
                () -> assertTrue(
                        token.isEndOfInput()
                ),
                () -> assertFalse(
                        token.startsExpression()
                ),
                () -> assertFalse(
                        token.endsExpression()
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tokenTypeBehaviorCases")
    void shouldExposeTokenTypeBehavior(
            String description,
            SearchTokenType type,
            boolean expectedBinaryOperator,
            boolean expectedStartsExpression,
            boolean expectedEndsExpression
    ) {
        assertAll(
                () -> assertEquals(
                        expectedBinaryOperator,
                        type.isBinaryOperator()
                ),
                () -> assertEquals(
                        expectedStartsExpression,
                        type.startsExpression()
                ),
                () -> assertEquals(
                        expectedEndsExpression,
                        type.endsExpression()
                )
        );
    }

    @ParameterizedTest
    @EnumSource(SearchTokenType.class)
    void shouldMatchItsOwnTokenType(
            SearchTokenType type
    ) {
        SearchToken token =
                tokenFor(type);

        assertTrue(
                token.is(type)
        );
    }

    @Test
    void shouldNotMatchDifferentTokenType() {
        SearchToken token =
                new SearchToken(
                        SearchTokenType.TERM,
                        "urgent",
                        new SourceSpan(
                                0,
                                6
                        )
                );

        assertFalse(
                token.is(SearchTokenType.PHRASE)
        );
    }

    @Test
    void shouldRejectNullExpectedTypeInIsMethod() {
        SearchToken token =
                new SearchToken(
                        SearchTokenType.TERM,
                        "urgent",
                        new SourceSpan(
                                0,
                                6
                        )
                );

        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> token.is(null)
                );

        assertEquals(
                "Expected search token type cannot be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullTokenType() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new SearchToken(
                                null,
                                "urgent",
                                "urgent",
                                new SourceSpan(
                                        0,
                                        6
                                )
                        )
                );

        assertEquals(
                "Search token type cannot be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullLexeme() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new SearchToken(
                                SearchTokenType.TERM,
                                null,
                                "urgent",
                                new SourceSpan(
                                        0,
                                        6
                                )
                        )
                );

        assertEquals(
                "Search token lexeme cannot be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullValue() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new SearchToken(
                                SearchTokenType.TERM,
                                "urgent",
                                null,
                                new SourceSpan(
                                        0,
                                        6
                                )
                        )
                );

        assertEquals(
                "Search token value cannot be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullSourceSpan() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new SearchToken(
                                SearchTokenType.TERM,
                                "urgent",
                                "urgent",
                                null
                        )
                );

        assertEquals(
                "Search token source span cannot be null",
                exception.getMessage()
        );
    }

    @ParameterizedTest
    @EnumSource(
            value = SearchTokenType.class,
            names = "END_OF_INPUT",
            mode = EnumSource.Mode.EXCLUDE
    )
    void shouldRejectEmptyLexemeForNonEndOfInputToken(
            SearchTokenType type
    ) {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new SearchToken(
                                type,
                                "",
                                "",
                                new SourceSpan(
                                        0,
                                        0
                                )
                        )
                );

        assertEquals(
                "Search token lexeme cannot be empty for token type "
                        + type,
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNonEmptyEndOfInputLexeme() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new SearchToken(
                                SearchTokenType.END_OF_INPUT,
                                "x",
                                "",
                                new SourceSpan(
                                        1,
                                        1
                                )
                        )
                );

        assertEquals(
                "The end-of-input token must have an empty lexeme and value",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNonEmptyEndOfInputValue() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new SearchToken(
                                SearchTokenType.END_OF_INPUT,
                                "",
                                "x",
                                new SourceSpan(
                                        1,
                                        1
                                )
                        )
                );

        assertEquals(
                "The end-of-input token must have an empty lexeme and value",
                exception.getMessage()
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "phrase",
            "\"phrase",
            "phrase\"",
            "'phrase'",
            "\""
    })
    void shouldRejectPhraseLexemeWithoutDoubleQuotePair(
            String lexeme
    ) {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new SearchToken(
                                SearchTokenType.PHRASE,
                                lexeme,
                                "phrase",
                                new SourceSpan(
                                        0,
                                        Math.max(
                                                1,
                                                lexeme.length()
                                        )
                                )
                        )
                );

        assertEquals(
                "A search phrase token must be enclosed in double quotes",
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
    void shouldRejectBlankPhraseValue(
            String value
    ) {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new SearchToken(
                                SearchTokenType.PHRASE,
                                "\"phrase\"",
                                value,
                                new SourceSpan(
                                        0,
                                        8
                                )
                        )
                );

        assertEquals(
                "A search phrase token cannot have a blank value",
                exception.getMessage()
        );
    }

    @Test
    void shouldAllowPhraseValueDifferentFromRawLexeme() {
        SearchToken token =
                new SearchToken(
                        SearchTokenType.PHRASE,
                        "\"quoted \\\"value\\\"\"",
                        "quoted \"value\"",
                        new SourceSpan(
                                0,
                                18
                        )
                );

        assertAll(
                () -> assertEquals(
                        "\"quoted \\\"value\\\"\"",
                        token.lexeme()
                ),
                () -> assertEquals(
                        "quoted \"value\"",
                        token.value()
                )
        );
    }

    @Test
    void shouldRejectNegativeEndOfInputPosition() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> SearchToken.endOfInput(-1)
                );

        assertEquals(
                "End-of-input position cannot be negative",
                exception.getMessage()
        );
    }

    private static SearchToken tokenFor(
            SearchTokenType type
    ) {
        return switch (type) {
            case TERM ->
                    new SearchToken(
                            type,
                            "urgent",
                            new SourceSpan(
                                    0,
                                    6
                            )
                    );

            case PHRASE ->
                    new SearchToken(
                            type,
                            "\"database failure\"",
                            "database failure",
                            new SourceSpan(
                                    0,
                                    18
                            )
                    );

            case AND ->
                    new SearchToken(
                            type,
                            "AND",
                            new SourceSpan(
                                    0,
                                    3
                            )
                    );

            case OR ->
                    new SearchToken(
                            type,
                            "OR",
                            new SourceSpan(
                                    0,
                                    2
                            )
                    );

            case NOT ->
                    new SearchToken(
                            type,
                            "NOT",
                            new SourceSpan(
                                    0,
                                    3
                            )
                    );

            case LEFT_PARENTHESIS ->
                    new SearchToken(
                            type,
                            "(",
                            new SourceSpan(
                                    0,
                                    1
                            )
                    );

            case RIGHT_PARENTHESIS ->
                    new SearchToken(
                            type,
                            ")",
                            new SourceSpan(
                                    0,
                                    1
                            )
                    );

            case END_OF_INPUT ->
                    SearchToken.endOfInput(0);
        };
    }

    private static Stream<Arguments> tokenTypeBehaviorCases() {
        return Stream.of(
                Arguments.of(
                        "term",
                        SearchTokenType.TERM,
                        false,
                        true,
                        true
                ),
                Arguments.of(
                        "phrase",
                        SearchTokenType.PHRASE,
                        false,
                        true,
                        true
                ),
                Arguments.of(
                        "AND",
                        SearchTokenType.AND,
                        true,
                        false,
                        false
                ),
                Arguments.of(
                        "OR",
                        SearchTokenType.OR,
                        true,
                        false,
                        false
                ),
                Arguments.of(
                        "NOT",
                        SearchTokenType.NOT,
                        false,
                        true,
                        false
                ),
                Arguments.of(
                        "left parenthesis",
                        SearchTokenType.LEFT_PARENTHESIS,
                        false,
                        true,
                        false
                ),
                Arguments.of(
                        "right parenthesis",
                        SearchTokenType.RIGHT_PARENTHESIS,
                        false,
                        false,
                        true
                ),
                Arguments.of(
                        "end of input",
                        SearchTokenType.END_OF_INPUT,
                        false,
                        false,
                        false
                )
        );
    }
}