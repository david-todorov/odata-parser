package io.github.davidtodorov.odataparser.filter.lexer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FilterTokenTest {

    @Test
    void shouldCreateTokenWithExpectedValues() {
        FilterToken token =
                new FilterToken(
                        FilterTokenType.IDENTIFIER,
                        "Title",
                        4,
                        9
                );

        assertAll(
                () -> assertEquals(
                        FilterTokenType.IDENTIFIER,
                        token.type()
                ),
                () -> assertEquals(
                        "Title",
                        token.lexeme()
                ),
                () -> assertEquals(
                        4,
                        token.start()
                ),
                () -> assertEquals(
                        9,
                        token.end()
                ),
                () -> assertEquals(
                        5,
                        token.length()
                )
        );
    }

    @Test
    void shouldCalculateTokenLengthFromSourceRange() {
        FilterToken token =
                new FilterToken(
                        FilterTokenType.STRING,
                        "'urgent'",
                        10,
                        18
                );

        assertEquals(
                8,
                token.length()
        );
    }

    @Test
    void shouldSupportRecordValueEquality() {
        FilterToken first =
                new FilterToken(
                        FilterTokenType.INTEGER,
                        "123",
                        0,
                        3
                );

        FilterToken second =
                new FilterToken(
                        FilterTokenType.INTEGER,
                        "123",
                        0,
                        3
                );

        assertAll(
                () -> assertEquals(first, second),
                () -> assertEquals(
                        first.hashCode(),
                        second.hashCode()
                )
        );
    }

    @Test
    void shouldNotConsiderDifferentTokensEqual() {
        FilterToken first =
                new FilterToken(
                        FilterTokenType.INTEGER,
                        "123",
                        0,
                        3
                );

        FilterToken second =
                new FilterToken(
                        FilterTokenType.DECIMAL,
                        "123",
                        0,
                        3
                );

        assertNotEquals(
                first,
                second
        );
    }

    @Test
    void shouldRejectNullTokenType() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new FilterToken(
                                null,
                                "Title",
                                0,
                                5
                        )
                );

        assertEquals(
                "Token type cannot be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullLexeme() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new FilterToken(
                                FilterTokenType.IDENTIFIER,
                                null,
                                0,
                                5
                        )
                );

        assertEquals(
                "Token lexeme cannot be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNegativeStartPosition() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new FilterToken(
                                FilterTokenType.IDENTIFIER,
                                "Title",
                                -1,
                                5
                        )
                );

        assertEquals(
                "Token start position cannot be negative",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectEndPositionBeforeStartPosition() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new FilterToken(
                                FilterTokenType.IDENTIFIER,
                                "Title",
                                6,
                                5
                        )
                );

        assertEquals(
                "Token end position cannot be before its start position",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectEmptyLexemeForNormalToken() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new FilterToken(
                                FilterTokenType.IDENTIFIER,
                                "",
                                0,
                                0
                        )
                );

        assertEquals(
                "Only the end-of-input token may have an empty lexeme",
                exception.getMessage()
        );
    }

    @Test
    void shouldCreateEndOfInputTokenWithEmptyRange() {
        FilterToken token =
                new FilterToken(
                        FilterTokenType.END_OF_INPUT,
                        "",
                        15,
                        15
                );

        assertAll(
                () -> assertEquals(
                        FilterTokenType.END_OF_INPUT,
                        token.type()
                ),
                () -> assertEquals(
                        "",
                        token.lexeme()
                ),
                () -> assertEquals(
                        15,
                        token.start()
                ),
                () -> assertEquals(
                        15,
                        token.end()
                ),
                () -> assertEquals(
                        0,
                        token.length()
                )
        );
    }

    @Test
    void shouldRejectEndOfInputTokenWithNonEmptyRange() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new FilterToken(
                                FilterTokenType.END_OF_INPUT,
                                "",
                                15,
                                16
                        )
                );

        assertEquals(
                "The end-of-input token must have an empty source range",
                exception.getMessage()
        );
    }
}