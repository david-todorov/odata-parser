package io.github.davidtodorov.odataparser.filter.lexer;

import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class FilterLexerTest {

    @Test
    void shouldTokenizeEmptyInputAsEndOfInput() {
        List<FilterToken> tokens =
                new FilterLexer("").tokenize();

        assertEquals(
                List.of(
                        new FilterToken(
                                FilterTokenType.END_OF_INPUT,
                                "",
                                0,
                                0
                        )
                ),
                tokens
        );
    }

    @Test
    void shouldIgnoreWhitespaceOnlyInput() {
        String input = " \t\r\n ";

        List<FilterToken> tokens =
                new FilterLexer(input).tokenize();

        assertEquals(
                List.of(
                        new FilterToken(
                                FilterTokenType.END_OF_INPUT,
                                "",
                                input.length(),
                                input.length()
                        )
                ),
                tokens
        );
    }

    @Test
    void shouldTokenizeIdentifier() {
        List<FilterToken> tokens =
                new FilterLexer("Title").tokenize();

        assertEquals(
                List.of(
                        new FilterToken(
                                FilterTokenType.IDENTIFIER,
                                "Title",
                                0,
                                5
                        ),
                        new FilterToken(
                                FilterTokenType.END_OF_INPUT,
                                "",
                                5,
                                5
                        )
                ),
                tokens
        );
    }

    @Test
    void shouldTokenizeIdentifierContainingLettersDigitsAndUnderscores() {
        List<FilterToken> tokens =
                new FilterLexer("_property123").tokenize();

        assertEquals(
                FilterTokenType.IDENTIFIER,
                tokens.getFirst().type()
        );

        assertEquals(
                "_property123",
                tokens.getFirst().lexeme()
        );
    }

    @Test
    void shouldTokenizeSimpleComparison() {
        List<FilterToken> tokens =
                new FilterLexer(
                        "Amount gt 100"
                ).tokenize();

        assertEquals(
                List.of(
                        FilterTokenType.IDENTIFIER,
                        FilterTokenType.GT,
                        FilterTokenType.INTEGER,
                        FilterTokenType.END_OF_INPUT
                ),
                tokenTypes(tokens)
        );

        assertAll(
                () -> assertEquals(
                        "Amount",
                        tokens.get(0).lexeme()
                ),
                () -> assertEquals(
                        "gt",
                        tokens.get(1).lexeme()
                ),
                () -> assertEquals(
                        "100",
                        tokens.get(2).lexeme()
                )
        );
    }

    @Test
    void shouldPreserveSourcePositionsWhileSkippingWhitespace() {
        List<FilterToken> tokens =
                new FilterLexer(
                        "  Title   eq   'urgent'  "
                ).tokenize();

        FilterToken title = tokens.get(0);
        FilterToken equality = tokens.get(1);
        FilterToken string = tokens.get(2);
        FilterToken endOfInput = tokens.get(3);

        assertAll(
                () -> assertEquals(
                        new FilterToken(
                                FilterTokenType.IDENTIFIER,
                                "Title",
                                2,
                                7
                        ),
                        title
                ),
                () -> assertEquals(
                        new FilterToken(
                                FilterTokenType.EQ,
                                "eq",
                                10,
                                12
                        ),
                        equality
                ),
                () -> assertEquals(
                        new FilterToken(
                                FilterTokenType.STRING,
                                "'urgent'",
                                15,
                                23
                        ),
                        string
                ),
                () -> assertEquals(
                        new FilterToken(
                                FilterTokenType.END_OF_INPUT,
                                "",
                                25,
                                25
                        ),
                        endOfInput
                )
        );
    }

    @Test
    void shouldReturnImmutableTokenList() {
        List<FilterToken> tokens =
                new FilterLexer("Title").tokenize();

        assertThrows(
                UnsupportedOperationException.class,
                () -> tokens.add(
                        new FilterToken(
                                FilterTokenType.IDENTIFIER,
                                "Other",
                                0,
                                5
                        )
                )
        );
    }

    private static List<FilterTokenType> tokenTypes(
            List<FilterToken> tokens
    ) {
        return tokens.stream()
                .map(FilterToken::type)
                .toList();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("reservedKeywordCases")
    void shouldTokenizeReservedKeyword(
            String keyword,
            FilterTokenType expectedType
    ) {
        List<FilterToken> tokens =
                new FilterLexer(keyword).tokenize();

        assertEquals(
                List.of(
                        new FilterToken(
                                expectedType,
                                keyword,
                                0,
                                keyword.length()
                        ),
                        new FilterToken(
                                FilterTokenType.END_OF_INPUT,
                                "",
                                keyword.length(),
                                keyword.length()
                        )
                ),
                tokens
        );
    }

    @ParameterizedTest(name = "{0} must remain an identifier")
    @MethodSource("nonKeywordCases")
    void shouldTreatNonKeywordTextAsIdentifier(
            String text
    ) {
        List<FilterToken> tokens =
                new FilterLexer(text).tokenize();

        assertEquals(
                FilterTokenType.IDENTIFIER,
                tokens.getFirst().type()
        );

        assertEquals(
                text,
                tokens.getFirst().lexeme()
        );
    }

    private static Stream<Arguments> reservedKeywordCases() {
        return Stream.of(
                Arguments.of(
                        Named.of("logical AND", "and"),
                        FilterTokenType.AND
                ),
                Arguments.of(
                        Named.of("logical OR", "or"),
                        FilterTokenType.OR
                ),
                Arguments.of(
                        Named.of("logical NOT", "not"),
                        FilterTokenType.NOT
                ),

                Arguments.of(
                        Named.of("equality", "eq"),
                        FilterTokenType.EQ
                ),
                Arguments.of(
                        Named.of("inequality", "ne"),
                        FilterTokenType.NE
                ),
                Arguments.of(
                        Named.of("greater than", "gt"),
                        FilterTokenType.GT
                ),
                Arguments.of(
                        Named.of("greater than or equal", "ge"),
                        FilterTokenType.GE
                ),
                Arguments.of(
                        Named.of("less than", "lt"),
                        FilterTokenType.LT
                ),
                Arguments.of(
                        Named.of("less than or equal", "le"),
                        FilterTokenType.LE
                ),
                Arguments.of(
                        Named.of("membership", "in"),
                        FilterTokenType.IN
                ),

                Arguments.of(
                        Named.of("addition", "add"),
                        FilterTokenType.ADD
                ),
                Arguments.of(
                        Named.of("subtraction", "sub"),
                        FilterTokenType.SUB
                ),
                Arguments.of(
                        Named.of("multiplication", "mul"),
                        FilterTokenType.MUL
                ),
                Arguments.of(
                        Named.of("division", "div"),
                        FilterTokenType.DIV
                ),
                Arguments.of(
                        Named.of("modulo", "mod"),
                        FilterTokenType.MOD
                ),

                Arguments.of(
                        Named.of("boolean true", "true"),
                        FilterTokenType.BOOLEAN
                ),
                Arguments.of(
                        Named.of("boolean false", "false"),
                        FilterTokenType.BOOLEAN
                ),
                Arguments.of(
                        Named.of("null literal", "null"),
                        FilterTokenType.NULL
                )
        );
    }

    private static Stream<String> nonKeywordCases() {
        return Stream.of(
                "AND",
                "And",
                "EQ",
                "True",
                "NULL",
                "contains",
                "startswith",
                "Title",
                "andValue",
                "eqProperty"
        );
    }

    @ParameterizedTest(name = "{0} should produce {1}")
    @MethodSource("validNumericLiteralCases")
    void shouldTokenizeNumericLiteral(
            String input,
            FilterTokenType expectedType
    ) {
        List<FilterToken> tokens =
                new FilterLexer(input).tokenize();

        assertEquals(
                List.of(
                        new FilterToken(
                                expectedType,
                                input,
                                0,
                                input.length()
                        ),
                        new FilterToken(
                                FilterTokenType.END_OF_INPUT,
                                "",
                                input.length(),
                                input.length()
                        )
                ),
                tokens
        );
    }

    @Test
    void shouldPreserveNumericLiteralSourcePosition() {
        String input = "  -12.50  ";

        List<FilterToken> tokens =
                new FilterLexer(input).tokenize();

        assertEquals(
                List.of(
                        new FilterToken(
                                FilterTokenType.DECIMAL,
                                "-12.50",
                                2,
                                8
                        ),
                        new FilterToken(
                                FilterTokenType.END_OF_INPUT,
                                "",
                                10,
                                10
                        )
                ),
                tokens
        );
    }

    @ParameterizedTest(name = "{0} should be rejected")
    @MethodSource("malformedNumericLiteralCases")
    void shouldRejectMalformedNumericLiteral(
            String input
    ) {
        assertThrows(
                FilterLexerException.class,
                () -> new FilterLexer(input).tokenize()
        );
    }

    private static Stream<Arguments> validNumericLiteralCases() {
        return Stream.of(
                Arguments.of(
                        "0",
                        FilterTokenType.INTEGER
                ),
                Arguments.of(
                        "7",
                        FilterTokenType.INTEGER
                ),
                Arguments.of(
                        "1234567890",
                        FilterTokenType.INTEGER
                ),
                Arguments.of(
                        "-1",
                        FilterTokenType.INTEGER
                ),
                Arguments.of(
                        "-999",
                        FilterTokenType.INTEGER
                ),
                Arguments.of(
                        "0.0",
                        FilterTokenType.DECIMAL
                ),
                Arguments.of(
                        "123.45",
                        FilterTokenType.DECIMAL
                ),
                Arguments.of(
                        "-0.50",
                        FilterTokenType.DECIMAL
                ),
                Arguments.of(
                        "-999.001",
                        FilterTokenType.DECIMAL
                )
        );
    }

    private static Stream<String> malformedNumericLiteralCases() {
        return Stream.of(
                ".5",
                "1.",
                "-",
                "--1",
                "+1",
                "1..2",
                "1.2.3",
                "-1."
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validStringLiteralCases")
    void shouldTokenizeStringLiteral(
            String description,
            String input
    ) {
        List<FilterToken> tokens =
                new FilterLexer(input).tokenize();

        assertEquals(
                List.of(
                        new FilterToken(
                                FilterTokenType.STRING,
                                input,
                                0,
                                input.length()
                        ),
                        new FilterToken(
                                FilterTokenType.END_OF_INPUT,
                                "",
                                input.length(),
                                input.length()
                        )
                ),
                tokens
        );
    }

    @Test
    void shouldPreserveStringLiteralSourcePosition() {
        String input = "  'urgent case'  ";

        List<FilterToken> tokens =
                new FilterLexer(input).tokenize();

        assertEquals(
                List.of(
                        new FilterToken(
                                FilterTokenType.STRING,
                                "'urgent case'",
                                2,
                                15
                        ),
                        new FilterToken(
                                FilterTokenType.END_OF_INPUT,
                                "",
                                17,
                                17
                        )
                ),
                tokens
        );
    }

    @Test
    void shouldKeepEscapedApostrophesInsideStringToken() {
        String input = "'O''Brien'";

        List<FilterToken> tokens =
                new FilterLexer(input).tokenize();

        FilterToken stringToken =
                tokens.getFirst();

        assertAll(
                () -> assertEquals(
                        FilterTokenType.STRING,
                        stringToken.type()
                ),
                () -> assertEquals(
                        "'O''Brien'",
                        stringToken.lexeme()
                ),
                () -> assertEquals(
                        0,
                        stringToken.start()
                ),
                () -> assertEquals(
                        10,
                        stringToken.end()
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("unterminatedStringLiteralCases")
    void shouldRejectUnterminatedStringLiteral(
            String description,
            String input,
            int expectedPosition
    ) {
        FilterLexerException exception =
                assertThrows(
                        FilterLexerException.class,
                        () -> new FilterLexer(input).tokenize()
                );

        assertAll(
                () -> assertEquals(
                        expectedPosition,
                        exception.position()
                ),
                () -> assertEquals(
                        "Unterminated string literal at position "
                                + expectedPosition,
                        exception.getMessage()
                )
        );
    }

    private static Stream<Arguments> validStringLiteralCases() {
        return Stream.of(
                Arguments.of(
                        "empty string",
                        "''"
                ),
                Arguments.of(
                        "simple string",
                        "'urgent'"
                ),
                Arguments.of(
                        "string containing spaces",
                        "'urgent payment case'"
                ),
                Arguments.of(
                        "string containing numbers",
                        "'Case 123'"
                ),
                Arguments.of(
                        "string containing operator text",
                        "'and or eq true null'"
                ),
                Arguments.of(
                        "string containing one escaped apostrophe",
                        "''''"
                ),
                Arguments.of(
                        "string containing an escaped apostrophe",
                        "'O''Brien'"
                ),
                Arguments.of(
                        "string containing multiple escaped apostrophes",
                        "'David''s user''s case'"
                )
        );
    }

    private static Stream<Arguments> unterminatedStringLiteralCases() {
        return Stream.of(
                Arguments.of(
                        "opening apostrophe only",
                        "'",
                        0
                ),
                Arguments.of(
                        "ordinary unterminated string",
                        "'urgent",
                        0
                ),
                Arguments.of(
                        "escaped apostrophe without final closing apostrophe",
                        "'O''Brien",
                        0
                ),
                Arguments.of(
                        "escaped apostrophe pair at end",
                        "'urgent''",
                        0
                ),
                Arguments.of(
                        "unterminated string after whitespace",
                        "  'urgent",
                        2
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("structuralTokenCases")
    void shouldTokenizeStructuralToken(
            String description,
            String input,
            FilterTokenType expectedType
    ) {
        List<FilterToken> tokens =
                new FilterLexer(input).tokenize();

        assertEquals(
                List.of(
                        new FilterToken(
                                expectedType,
                                input,
                                0,
                                1
                        ),
                        new FilterToken(
                                FilterTokenType.END_OF_INPUT,
                                "",
                                1,
                                1
                        )
                ),
                tokens
        );
    }

    @Test
    void shouldTokenizeAllStructuralTokensTogether() {
        String input = "(),/";

        List<FilterToken> tokens =
                new FilterLexer(input).tokenize();

        assertEquals(
                List.of(
                        new FilterToken(
                                FilterTokenType.LEFT_PAREN,
                                "(",
                                0,
                                1
                        ),
                        new FilterToken(
                                FilterTokenType.RIGHT_PAREN,
                                ")",
                                1,
                                2
                        ),
                        new FilterToken(
                                FilterTokenType.COMMA,
                                ",",
                                2,
                                3
                        ),
                        new FilterToken(
                                FilterTokenType.SLASH,
                                "/",
                                3,
                                4
                        ),
                        new FilterToken(
                                FilterTokenType.END_OF_INPUT,
                                "",
                                4,
                                4
                        )
                ),
                tokens
        );
    }

    @Test
    void shouldTokenizeNestedParentheses() {
        String input = "((Title))";

        List<FilterToken> tokens =
                new FilterLexer(input).tokenize();

        assertEquals(
                List.of(
                        FilterTokenType.LEFT_PAREN,
                        FilterTokenType.LEFT_PAREN,
                        FilterTokenType.IDENTIFIER,
                        FilterTokenType.RIGHT_PAREN,
                        FilterTokenType.RIGHT_PAREN,
                        FilterTokenType.END_OF_INPUT
                ),
                tokenTypes(tokens)
        );

        assertAll(
                () -> assertEquals(
                        "(",
                        tokens.get(0).lexeme()
                ),
                () -> assertEquals(
                        "(",
                        tokens.get(1).lexeme()
                ),
                () -> assertEquals(
                        "Title",
                        tokens.get(2).lexeme()
                ),
                () -> assertEquals(
                        ")",
                        tokens.get(3).lexeme()
                ),
                () -> assertEquals(
                        ")",
                        tokens.get(4).lexeme()
                )
        );
    }

    @Test
    void shouldTokenizeNavigationPropertyPath() {
        String input =
                "Owner/Department/Code";

        List<FilterToken> tokens =
                new FilterLexer(input).tokenize();

        assertEquals(
                List.of(
                        new FilterToken(
                                FilterTokenType.IDENTIFIER,
                                "Owner",
                                0,
                                5
                        ),
                        new FilterToken(
                                FilterTokenType.SLASH,
                                "/",
                                5,
                                6
                        ),
                        new FilterToken(
                                FilterTokenType.IDENTIFIER,
                                "Department",
                                6,
                                16
                        ),
                        new FilterToken(
                                FilterTokenType.SLASH,
                                "/",
                                16,
                                17
                        ),
                        new FilterToken(
                                FilterTokenType.IDENTIFIER,
                                "Code",
                                17,
                                21
                        ),
                        new FilterToken(
                                FilterTokenType.END_OF_INPUT,
                                "",
                                21,
                                21
                        )
                ),
                tokens
        );
    }

    @Test
    void shouldTokenizeFunctionCallWithPropertyPathAndArgument() {
        String input =
                "contains(Owner/Department/Code,'ENG')";

        List<FilterToken> tokens =
                new FilterLexer(input).tokenize();

        assertEquals(
                List.of(
                        new FilterToken(
                                FilterTokenType.IDENTIFIER,
                                "contains",
                                0,
                                8
                        ),
                        new FilterToken(
                                FilterTokenType.LEFT_PAREN,
                                "(",
                                8,
                                9
                        ),
                        new FilterToken(
                                FilterTokenType.IDENTIFIER,
                                "Owner",
                                9,
                                14
                        ),
                        new FilterToken(
                                FilterTokenType.SLASH,
                                "/",
                                14,
                                15
                        ),
                        new FilterToken(
                                FilterTokenType.IDENTIFIER,
                                "Department",
                                15,
                                25
                        ),
                        new FilterToken(
                                FilterTokenType.SLASH,
                                "/",
                                25,
                                26
                        ),
                        new FilterToken(
                                FilterTokenType.IDENTIFIER,
                                "Code",
                                26,
                                30
                        ),
                        new FilterToken(
                                FilterTokenType.COMMA,
                                ",",
                                30,
                                31
                        ),
                        new FilterToken(
                                FilterTokenType.STRING,
                                "'ENG'",
                                31,
                                36
                        ),
                        new FilterToken(
                                FilterTokenType.RIGHT_PAREN,
                                ")",
                                36,
                                37
                        ),
                        new FilterToken(
                                FilterTokenType.END_OF_INPUT,
                                "",
                                37,
                                37
                        )
                ),
                tokens
        );
    }

    @Test
    void shouldTokenizeCommaSeparatedInList() {
        String input =
                "Priority in (1,2,3)";

        List<FilterToken> tokens =
                new FilterLexer(input).tokenize();

        assertEquals(
                List.of(
                        FilterTokenType.IDENTIFIER,
                        FilterTokenType.IN,
                        FilterTokenType.LEFT_PAREN,
                        FilterTokenType.INTEGER,
                        FilterTokenType.COMMA,
                        FilterTokenType.INTEGER,
                        FilterTokenType.COMMA,
                        FilterTokenType.INTEGER,
                        FilterTokenType.RIGHT_PAREN,
                        FilterTokenType.END_OF_INPUT
                ),
                tokenTypes(tokens)
        );

        assertAll(
                () -> assertEquals(
                        "1",
                        tokens.get(3).lexeme()
                ),
                () -> assertEquals(
                        "2",
                        tokens.get(5).lexeme()
                ),
                () -> assertEquals(
                        "3",
                        tokens.get(7).lexeme()
                )
        );
    }

    private static Stream<Arguments> structuralTokenCases() {
        return Stream.of(
                Arguments.of(
                        "left parenthesis",
                        "(",
                        FilterTokenType.LEFT_PAREN
                ),
                Arguments.of(
                        "right parenthesis",
                        ")",
                        FilterTokenType.RIGHT_PAREN
                ),
                Arguments.of(
                        "comma",
                        ",",
                        FilterTokenType.COMMA
                ),
                Arguments.of(
                        "property-path slash",
                        "/",
                        FilterTokenType.SLASH
                )
        );
    }

    @Test
    void shouldRejectNullInput() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new FilterLexer(null)
                );

        assertEquals(
                "Filter input cannot be null",
                exception.getMessage()
        );
    }

    @ParameterizedTest(name = "should reject {1} at position {2}")
    @MethodSource("unexpectedCharacterCases")
    void shouldRejectUnexpectedCharacter(
            String input,
            String expectedCharacter,
            int expectedPosition
    ) {
        FilterLexerException exception =
                assertThrows(
                        FilterLexerException.class,
                        () -> new FilterLexer(input).tokenize()
                );

        assertAll(
                () -> assertTrue(
                        exception.getMessage().contains(
                                "Unexpected character '"
                                        + expectedCharacter
                                        + "'"
                        )
                ),
                () -> assertTrue(
                        exception.getMessage().contains(
                                String.valueOf(expectedPosition)
                        )
                )
        );
    }

    @Test
    void shouldStopAtFirstUnexpectedCharacter() {
        String input =
                "Title eq 'valid' @ Amount gt 10";

        FilterLexerException exception =
                assertThrows(
                        FilterLexerException.class,
                        () -> new FilterLexer(input).tokenize()
                );

        assertAll(
                () -> assertTrue(
                        exception.getMessage().contains(
                                "Unexpected character '@'"
                        )
                ),
                () -> assertTrue(
                        exception.getMessage().contains("17")
                )
        );
    }

    private static Stream<Arguments> unexpectedCharacterCases() {
        return Stream.of(
                Arguments.of(
                        "@",
                        "@",
                        0
                ),
                Arguments.of(
                        "$",
                        "$",
                        0
                ),
                Arguments.of(
                        "Title = 'urgent'",
                        "=",
                        6
                ),
                Arguments.of(
                        "Amount > 100",
                        ">",
                        7
                ),
                Arguments.of(
                        "Amount < 100",
                        "<",
                        7
                ),
                Arguments.of(
                        "Price + 10",
                        "+",
                        6
                ),
                Arguments.of(
                        "Title eq \"urgent\"",
                        "\"",
                        9
                ),
                Arguments.of(
                        "A&B",
                        "&",
                        1
                ),
                Arguments.of(
                        "Owner\\Username",
                        "\\",
                        5
                ),
                Arguments.of(
                        "  @",
                        "@",
                        2
                )
        );
    }

    @Test
    void shouldTokenizeCompleteRealisticFilterExpression() {
        String input =
                "contains(Owner/Department/Code,'O''Brien') "
                        + "and Amount ge -12.50 "
                        + "or Deleted eq false";

        List<FilterToken> tokens =
                new FilterLexer(input).tokenize();

        assertEquals(
                List.of(
                        FilterTokenType.IDENTIFIER,
                        FilterTokenType.LEFT_PAREN,
                        FilterTokenType.IDENTIFIER,
                        FilterTokenType.SLASH,
                        FilterTokenType.IDENTIFIER,
                        FilterTokenType.SLASH,
                        FilterTokenType.IDENTIFIER,
                        FilterTokenType.COMMA,
                        FilterTokenType.STRING,
                        FilterTokenType.RIGHT_PAREN,
                        FilterTokenType.AND,
                        FilterTokenType.IDENTIFIER,
                        FilterTokenType.GE,
                        FilterTokenType.DECIMAL,
                        FilterTokenType.OR,
                        FilterTokenType.IDENTIFIER,
                        FilterTokenType.EQ,
                        FilterTokenType.BOOLEAN,
                        FilterTokenType.END_OF_INPUT
                ),
                tokenTypes(tokens)
        );

        assertEquals(
                List.of(
                        "contains",
                        "(",
                        "Owner",
                        "/",
                        "Department",
                        "/",
                        "Code",
                        ",",
                        "'O''Brien'",
                        ")",
                        "and",
                        "Amount",
                        "ge",
                        "-12.50",
                        "or",
                        "Deleted",
                        "eq",
                        "false",
                        ""
                ),
                tokenLexemes(tokens)
        );
    }

    @Test
    void shouldTokenizeAdjacentTokensWithoutWhitespace() {
        String input =
                "not(Deleted)and(Amount gt -1)";

        List<FilterToken> tokens =
                new FilterLexer(input).tokenize();

        assertEquals(
                List.of(
                        FilterTokenType.NOT,
                        FilterTokenType.LEFT_PAREN,
                        FilterTokenType.IDENTIFIER,
                        FilterTokenType.RIGHT_PAREN,
                        FilterTokenType.AND,
                        FilterTokenType.LEFT_PAREN,
                        FilterTokenType.IDENTIFIER,
                        FilterTokenType.GT,
                        FilterTokenType.INTEGER,
                        FilterTokenType.RIGHT_PAREN,
                        FilterTokenType.END_OF_INPUT
                ),
                tokenTypes(tokens)
        );
    }

    @Test
    void shouldTokenizeLiteralKeywordsNextToStructuralTokens() {
        String input =
                "(true,false,null)";

        List<FilterToken> tokens =
                new FilterLexer(input).tokenize();

        assertEquals(
                List.of(
                        FilterTokenType.LEFT_PAREN,
                        FilterTokenType.BOOLEAN,
                        FilterTokenType.COMMA,
                        FilterTokenType.BOOLEAN,
                        FilterTokenType.COMMA,
                        FilterTokenType.NULL,
                        FilterTokenType.RIGHT_PAREN,
                        FilterTokenType.END_OF_INPUT
                ),
                tokenTypes(tokens)
        );

        assertEquals(
                List.of(
                        "(",
                        "true",
                        ",",
                        "false",
                        ",",
                        "null",
                        ")",
                        ""
                ),
                tokenLexemes(tokens)
        );
    }

    @Test
    void shouldTokenizeNegativeNumbersInsideList() {
        String input =
                "Priority in (-1,-2.50,0,3.1415)";

        List<FilterToken> tokens =
                new FilterLexer(input).tokenize();

        assertEquals(
                List.of(
                        FilterTokenType.IDENTIFIER,
                        FilterTokenType.IN,
                        FilterTokenType.LEFT_PAREN,
                        FilterTokenType.INTEGER,
                        FilterTokenType.COMMA,
                        FilterTokenType.DECIMAL,
                        FilterTokenType.COMMA,
                        FilterTokenType.INTEGER,
                        FilterTokenType.COMMA,
                        FilterTokenType.DECIMAL,
                        FilterTokenType.RIGHT_PAREN,
                        FilterTokenType.END_OF_INPUT
                ),
                tokenTypes(tokens)
        );

        assertEquals(
                List.of(
                        "Priority",
                        "in",
                        "(",
                        "-1",
                        ",",
                        "-2.50",
                        ",",
                        "0",
                        ",",
                        "3.1415",
                        ")",
                        ""
                ),
                tokenLexemes(tokens)
        );
    }

    @Test
    void shouldTokenizeEmptyStringInsideFunctionCall() {
        String input =
                "contains(Title,'')";

        List<FilterToken> tokens =
                new FilterLexer(input).tokenize();

        assertEquals(
                List.of(
                        FilterTokenType.IDENTIFIER,
                        FilterTokenType.LEFT_PAREN,
                        FilterTokenType.IDENTIFIER,
                        FilterTokenType.COMMA,
                        FilterTokenType.STRING,
                        FilterTokenType.RIGHT_PAREN,
                        FilterTokenType.END_OF_INPUT
                ),
                tokenTypes(tokens)
        );

        assertEquals(
                "''",
                tokens.get(4).lexeme()
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
    void shouldRecognizeJavaWhitespaceBetweenTokens(
            String whitespace
    ) {
        String input =
                "Title"
                        + whitespace
                        + "eq"
                        + whitespace
                        + "'urgent'";

        List<FilterToken> tokens =
                new FilterLexer(input).tokenize();

        assertEquals(
                List.of(
                        FilterTokenType.IDENTIFIER,
                        FilterTokenType.EQ,
                        FilterTokenType.STRING,
                        FilterTokenType.END_OF_INPUT
                ),
                tokenTypes(tokens)
        );
    }

    @ParameterizedTest(name = "large numeric literal {0}")
    @MethodSource("largeNumericLiteralCases")
    void shouldPreserveLargeNumericLiteralWithoutParsingIt(
            String input,
            FilterTokenType expectedType
    ) {
        List<FilterToken> tokens =
                new FilterLexer(input).tokenize();

        assertAll(
                () -> assertEquals(
                        expectedType,
                        tokens.getFirst().type()
                ),
                () -> assertEquals(
                        input,
                        tokens.getFirst().lexeme()
                ),
                () -> assertEquals(
                        input.length(),
                        tokens.getFirst().length()
                )
        );
    }

    @Test
    void shouldPreserveValidSourceRangeForEveryToken() {
        String input =
                "  Owner/Department/Code eq 'ENG' "
                        + "and Amount gt -100.50  ";

        List<FilterToken> tokens =
                new FilterLexer(input).tokenize();

        int previousEnd = 0;

        for (FilterToken token : tokens) {
            assertTrue(
                    token.start() >= previousEnd,
                    "Token ranges must be ordered"
            );

            assertTrue(
                    token.end() <= input.length(),
                    "Token range must not exceed the input"
            );

            if (token.type()
                    != FilterTokenType.END_OF_INPUT) {

                assertEquals(
                        token.lexeme(),
                        input.substring(
                                token.start(),
                                token.end()
                        )
                );
            }

            previousEnd = token.end();
        }

        FilterToken endOfInput =
                tokens.getLast();

        assertAll(
                () -> assertEquals(
                        FilterTokenType.END_OF_INPUT,
                        endOfInput.type()
                ),
                () -> assertEquals(
                        input.length(),
                        endOfInput.start()
                ),
                () -> assertEquals(
                        input.length(),
                        endOfInput.end()
                )
        );
    }

    @Test
    void shouldAlwaysAppendExactlyOneEndOfInputToken() {
        List<FilterToken> tokens =
                new FilterLexer(
                        "Title eq 'urgent'"
                ).tokenize();

        long endOfInputCount =
                tokens.stream()
                        .filter(
                                token ->
                                        token.type()
                                                == FilterTokenType.END_OF_INPUT
                        )
                        .count();

        assertAll(
                () -> assertEquals(
                        1,
                        endOfInputCount
                ),
                () -> assertEquals(
                        FilterTokenType.END_OF_INPUT,
                        tokens.getLast().type()
                )
        );
    }

    @Test
    void shouldProduceSameResultWhenTokenizedMoreThanOnce() {
        FilterLexer lexer =
                new FilterLexer(
                        "Title eq 'urgent'"
                );

        List<FilterToken> firstResult =
                lexer.tokenize();

        List<FilterToken> secondResult =
                lexer.tokenize();

        assertEquals(
                firstResult,
                secondResult
        );
    }

    private static Stream<Arguments> largeNumericLiteralCases() {
        return Stream.of(
                Arguments.of(
                        "999999999999999999999999999999999999999999",
                        FilterTokenType.INTEGER
                ),
                Arguments.of(
                        "-999999999999999999999999999999999999999999",
                        FilterTokenType.INTEGER
                ),
                Arguments.of(
                        "999999999999999999999999999999.12345678901234567890",
                        FilterTokenType.DECIMAL
                ),
                Arguments.of(
                        "-999999999999999999999999999999.12345678901234567890",
                        FilterTokenType.DECIMAL
                )
        );
    }

    private static List<String> tokenLexemes(
            List<FilterToken> tokens
    ) {
        return tokens.stream()
                .map(FilterToken::lexeme)
                .toList();
    }
}