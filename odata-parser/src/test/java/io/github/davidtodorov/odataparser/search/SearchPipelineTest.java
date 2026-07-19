package io.github.davidtodorov.odataparser.search;

import io.github.davidtodorov.odataparser.search.ast.SearchBinaryExpression;
import io.github.davidtodorov.odataparser.search.ast.SearchBinaryOperator;
import io.github.davidtodorov.odataparser.search.ast.SearchExpression;
import io.github.davidtodorov.odataparser.search.ast.SearchNotExpression;
import io.github.davidtodorov.odataparser.search.ast.SearchPhraseExpression;
import io.github.davidtodorov.odataparser.search.ast.SearchTermExpression;
import io.github.davidtodorov.odataparser.search.lexer.SearchLexer;
import io.github.davidtodorov.odataparser.search.lexer.SearchLexerException;
import io.github.davidtodorov.odataparser.search.lexer.SearchToken;
import io.github.davidtodorov.odataparser.search.lexer.SearchTokenType;
import io.github.davidtodorov.odataparser.search.parser.SearchParser;
import io.github.davidtodorov.odataparser.search.parser.SearchParserException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SearchPipelineTest {

    @Test
    void shouldTokenizeAndParseRealisticSearchExpression() {
        String input =
                "(urgent OR \"database failure\") "
                        + "AND NOT archived delayed";

        List<SearchToken> tokens =
                new SearchLexer(input).tokenize();

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
                        List.of(
                                SearchTokenType.LEFT_PARENTHESIS,
                                SearchTokenType.TERM,
                                SearchTokenType.OR,
                                SearchTokenType.PHRASE,
                                SearchTokenType.RIGHT_PARENTHESIS,
                                SearchTokenType.AND,
                                SearchTokenType.NOT,
                                SearchTokenType.TERM,
                                SearchTokenType.TERM,
                                SearchTokenType.END_OF_INPUT
                        ),
                        tokens.stream()
                                .map(SearchToken::type)
                                .toList()
                ),
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
                () -> assertEquals(
                        "\"database failure\"",
                        phrase.rawText()
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
    void shouldPreserveRawAndDecodedEscapedPhraseAcrossPipeline() {
        String input =
                "\"say \\\"hello\\\" at C:\\\\temp\" AND urgent";

        List<SearchToken> tokens =
                new SearchLexer(input).tokenize();

        SearchBinaryExpression binary =
                assertInstanceOf(
                        SearchBinaryExpression.class,
                        parse(input)
                );

        SearchToken phraseToken =
                tokens.getFirst();

        SearchPhraseExpression phrase =
                assertInstanceOf(
                        SearchPhraseExpression.class,
                        binary.left()
                );

        assertAll(
                () -> assertEquals(
                        SearchTokenType.PHRASE,
                        phraseToken.type()
                ),
                () -> assertEquals(
                        "\"say \\\"hello\\\" at C:\\\\temp\"",
                        phraseToken.lexeme()
                ),
                () -> assertEquals(
                        "say \"hello\" at C:\\temp",
                        phraseToken.value()
                ),
                () -> assertEquals(
                        phraseToken.value(),
                        phrase.phrase()
                ),
                () -> assertEquals(
                        phraseToken.lexeme(),
                        phrase.rawText()
                ),
                () -> assertEquals(
                        SearchBinaryOperator.AND,
                        binary.operator()
                ),
                () -> assertTerm(
                        binary.right(),
                        "urgent"
                ),
                () -> assertSpan(
                        binary,
                        0,
                        input.length()
                )
        );
    }

    @Test
    void shouldApplyAndPrecedenceAcrossExplicitAndImplicitOperators() {
        SearchBinaryExpression root =
                assertInstanceOf(
                        SearchBinaryExpression.class,
                        parse(
                                "urgent OR delayed archived AND active"
                        )
                );

        SearchBinaryExpression right =
                assertInstanceOf(
                        SearchBinaryExpression.class,
                        root.right()
                );

        SearchBinaryExpression firstAnd =
                assertInstanceOf(
                        SearchBinaryExpression.class,
                        right.left()
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
                () -> assertEquals(
                        SearchBinaryOperator.AND,
                        firstAnd.operator()
                ),
                () -> assertTerm(
                        root.left(),
                        "urgent"
                ),
                () -> assertTerm(
                        firstAnd.left(),
                        "delayed"
                ),
                () -> assertTerm(
                        firstAnd.right(),
                        "archived"
                ),
                () -> assertTerm(
                        right.right(),
                        "active"
                )
        );
    }

    @Test
    void shouldPreserveMeaningfulSourceSpanWithOuterWhitespace() {
        String input =
                "  \turgent AND delayed \r\n";

        SearchBinaryExpression expression =
                assertInstanceOf(
                        SearchBinaryExpression.class,
                        parse(input)
                );

        assertAll(
                () -> assertSpan(
                        expression,
                        input.indexOf("urgent"),
                        input.indexOf("delayed")
                                + "delayed".length()
                ),
                () -> assertSpan(
                        expression.left(),
                        input.indexOf("urgent"),
                        input.indexOf("urgent")
                                + "urgent".length()
                ),
                () -> assertSpan(
                        expression.right(),
                        input.indexOf("delayed"),
                        input.indexOf("delayed")
                                + "delayed".length()
                )
        );
    }

    @Test
    void shouldSupportUnicodeAndSupportedPunctuationEndToEnd() {
        String input =
                "Überprüfung AND email@example.com";

        SearchBinaryExpression expression =
                assertInstanceOf(
                        SearchBinaryExpression.class,
                        parse(input)
                );

        assertAll(
                () -> assertEquals(
                        SearchBinaryOperator.AND,
                        expression.operator()
                ),
                () -> assertTerm(
                        expression.left(),
                        "Überprüfung"
                ),
                () -> assertTerm(
                        expression.right(),
                        "email@example.com"
                ),
                () -> assertSpan(
                        expression,
                        0,
                        input.length()
                )
        );
    }

    @Test
    void shouldProduceEquivalentTreesFromIndependentPipelineRuns() {
        String input =
                "(urgent OR delayed) AND NOT archived";

        SearchExpression first =
                parse(input);

        SearchExpression second =
                parse(input);

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

    @Test
    void shouldSurfaceUnsupportedCharacterAsLexerFailure() {
        String input =
                "urgent & delayed";

        SearchLexerException exception =
                assertThrows(
                        SearchLexerException.class,
                        () -> parse(input)
                );

        assertAll(
                () -> assertEquals(
                        7,
                        exception.position()
                ),
                () -> assertEquals(
                        "Unsupported character '&' in unquoted search term "
                                + "at position 7.",
                        exception.getMessage()
                )
        );
    }

    @Test
    void shouldSurfaceUnterminatedPhraseAsLexerFailure() {
        String input =
                "urgent AND \"database failure";

        SearchLexerException exception =
                assertThrows(
                        SearchLexerException.class,
                        () -> parse(input)
                );

        assertAll(
                () -> assertEquals(
                        11,
                        exception.position()
                ),
                () -> assertEquals(
                        "Unterminated search phrase at position 11.",
                        exception.getMessage()
                )
        );
    }

    @Test
    void shouldSurfaceMalformedExpressionAsParserFailure() {
        String input =
                "(urgent OR delayed";

        SearchParserException exception =
                assertThrows(
                        SearchParserException.class,
                        () -> parse(input)
                );

        assertAll(
                () -> assertEquals(
                        input.length(),
                        exception.position()
                ),
                () -> assertEquals(
                        "Expected ')' to close the parenthesis opened "
                                + "at position 0 at position "
                                + input.length()
                                + ".",
                        exception.getMessage()
                )
        );
    }

    @Test
    void shouldSurfaceIncorrectOperatorCaseAsParserFailure() {
        String input =
                "urgent and delayed";

        SearchParserException exception =
                assertThrows(
                        SearchParserException.class,
                        () -> parse(input)
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
}