package io.github.davidtodorov.odataparser.orderby.parser;

import io.github.davidtodorov.odataparser.orderby.ast.OrderByDirection;
import io.github.davidtodorov.odataparser.orderby.ast.OrderByItem;
import io.github.davidtodorov.odataparser.orderby.ast.OrderByOption;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderByParserTest {

    @Test
    void shouldParseSinglePropertyWithDefaultAscendingDirection() {
        OrderByOption option =
                parse("Title");

        assertAll(
                () -> assertEquals(
                        1,
                        option.size()
                ),
                () -> assertFalse(
                        option.isResolved()
                ),
                () -> assertSpan(
                        option,
                        0,
                        5
                )
        );

        assertItem(
                option.get(0),
                List.of("Title"),
                OrderByDirection.ASCENDING,
                0,
                5
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("explicitDirectionCases")
    void shouldParseExplicitDirection(
            String description,
            String input,
            OrderByDirection expectedDirection,
            int expectedEnd
    ) {
        OrderByOption option =
                parse(input);

        assertItem(
                option.get(0),
                List.of("Title"),
                expectedDirection,
                0,
                expectedEnd
        );

        assertSpan(
                option,
                0,
                expectedEnd
        );
    }

    @Test
    void shouldParseMultipleItemsInOriginalOrder() {
        String input =
                "Title, Owner/Username desc, Id asc";

        OrderByOption option =
                parse(input);

        assertAll(
                () -> assertEquals(
                        3,
                        option.size()
                ),
                () -> assertSpan(
                        option,
                        0,
                        input.length()
                )
        );

        assertItem(
                option.get(0),
                List.of("Title"),
                OrderByDirection.ASCENDING,
                0,
                5
        );

        assertItem(
                option.get(1),
                List.of(
                        "Owner",
                        "Username"
                ),
                OrderByDirection.DESCENDING,
                7,
                26
        );

        assertItem(
                option.get(2),
                List.of("Id"),
                OrderByDirection.ASCENDING,
                28,
                34
        );

        assertEquals(
                List.of(
                        "Title",
                        "Owner/Username",
                        "Id"
                ),
                option.items()
                        .stream()
                        .map(OrderByItem::externalPath)
                        .toList()
        );
    }

    @Test
    void shouldParseDeepNavigationPath() {
        OrderByOption option =
                parse(
                        "Owner/Department/Code desc"
                );

        assertItem(
                option.get(0),
                List.of(
                        "Owner",
                        "Department",
                        "Code"
                ),
                OrderByDirection.DESCENDING,
                0,
                26
        );
    }

    @Test
    void shouldAllowWhitespaceAroundPathSeparators() {
        String input =
                "  Owner / Department / Code   desc  ";

        OrderByOption option =
                parse(input);

        assertAll(
                () -> assertSpan(
                        option,
                        2,
                        34
                ),
                () -> assertEquals(
                        "Owner/Department/Code",
                        option.get(0).externalPath()
                )
        );

        assertItem(
                option.get(0),
                List.of(
                        "Owner",
                        "Department",
                        "Code"
                ),
                OrderByDirection.DESCENDING,
                2,
                34
        );
    }

    @Test
    void shouldIgnoreLeadingAndTrailingWhitespace() {
        String input =
                " \tTitle desc \r\n";

        OrderByOption option =
                parse(input);

        assertAll(
                () -> assertSpan(
                        option,
                        2,
                        12
                ),
                () -> assertSpan(
                        option.get(0),
                        2,
                        12
                )
        );
    }

    @ParameterizedTest(name = "whitespace variant: {0}")
    @ValueSource(strings = {
            " ",
            "\t",
            "\r",
            "\n",
            "\f",
            " \t\r\n\f "
    })
    void shouldRecognizeWhitespaceBetweenPropertyAndDirection(
            String whitespace
    ) {
        OrderByOption option =
                parse(
                        "Title"
                                + whitespace
                                + "desc"
                );

        assertEquals(
                OrderByDirection.DESCENDING,
                option.get(0).direction()
        );
    }

    @Test
    void shouldAllowWhitespaceAroundCommas() {
        OrderByOption option =
                parse(
                        "Title asc  ,\tAmount desc,\nId"
                );

        assertAll(
                () -> assertEquals(
                        3,
                        option.size()
                ),
                () -> assertEquals(
                        List.of(
                                OrderByDirection.ASCENDING,
                                OrderByDirection.DESCENDING,
                                OrderByDirection.ASCENDING
                        ),
                        option.items()
                                .stream()
                                .map(OrderByItem::direction)
                                .toList()
                )
        );
    }

    @Test
    void shouldParseIdentifiersContainingUnderscoresAndDigits() {
        OrderByOption option =
                parse(
                        "_owner2/Department_1/Code99 desc"
                );

        assertEquals(
                List.of(
                        "_owner2",
                        "Department_1",
                        "Code99"
                ),
                option.get(0).pathSegments()
        );
    }

    @Test
    void shouldPreserveIdentifierCase() {
        OrderByOption option =
                parse(
                        "Owner/DisplayName desc"
                );

        assertEquals(
                List.of(
                        "Owner",
                        "DisplayName"
                ),
                option.get(0).pathSegments()
        );
    }

    @Test
    void shouldDefaultEveryDirectionIndependently() {
        OrderByOption option =
                parse(
                        "Title, Amount desc, Priority"
                );

        assertEquals(
                List.of(
                        OrderByDirection.ASCENDING,
                        OrderByDirection.DESCENDING,
                        OrderByDirection.ASCENDING
                ),
                option.items()
                        .stream()
                        .map(OrderByItem::direction)
                        .toList()
        );
    }

    @Test
    void shouldExposeItemsThroughIterationInSourceOrder() {
        OrderByOption option =
                parse(
                        "Title, Amount desc, Id"
                );

        List<String> paths =
                new java.util.ArrayList<>();

        for (OrderByItem item : option) {
            paths.add(
                    item.externalPath()
            );
        }

        assertEquals(
                List.of(
                        "Title",
                        "Amount",
                        "Id"
                ),
                paths
        );
    }

    @Test
    void shouldKeepParsedItemsUnresolved() {
        OrderByOption option =
                parse(
                        "Title, Owner/Username desc"
                );

        assertAll(
                () -> assertFalse(
                        option.isResolved()
                ),
                () -> assertTrue(
                        option.items()
                                .stream()
                                .noneMatch(
                                        OrderByItem::isResolved
                                )
                ),
                () -> assertTrue(
                        option.items()
                                .stream()
                                .allMatch(
                                        item ->
                                                item.resolvedPath()
                                                        .isEmpty()
                                )
                ),
                () -> assertTrue(
                        option.items()
                                .stream()
                                .allMatch(
                                        item ->
                                                item.mappedPath()
                                                        .isEmpty()
                                )
                ),
                () -> assertTrue(
                        option.items()
                                .stream()
                                .allMatch(
                                        item ->
                                                item.expressionType()
                                                        .isEmpty()
                                )
                )
        );
    }

    @Test
    void shouldRejectNullInput() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new OrderByParser(null)
                );

        assertEquals(
                "Order-by input cannot be null",
                exception.getMessage()
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("malformedInputCases")
    void shouldRejectMalformedInputAtExpectedPosition(
            String description,
            String input,
            String expectedMessage,
            int expectedPosition
    ) {
        OrderByParserException exception =
                assertThrows(
                        OrderByParserException.class,
                        () -> parse(input)
                );

        assertAll(
                () -> assertEquals(
                        expectedMessage
                                + " at position "
                                + expectedPosition
                                + ".",
                        exception.getMessage()
                ),
                () -> assertEquals(
                        expectedPosition,
                        exception.position()
                )
        );
    }

    @ParameterizedTest(name = "unsupported direction: {0}")
    @MethodSource("unsupportedDirectionCases")
    void shouldRejectUnsupportedDirection(
            String direction,
            int expectedPosition
    ) {
        String input =
                "Title " + direction;

        OrderByParserException exception =
                assertThrows(
                        OrderByParserException.class,
                        () -> parse(input)
                );

        IllegalArgumentException cause =
                assertInstanceOf(
                        IllegalArgumentException.class,
                        exception.getCause()
                );

        assertAll(
                () -> assertEquals(
                        expectedPosition,
                        exception.position()
                ),
                () -> assertEquals(
                        "Unsupported order-by direction '"
                                + direction
                                + "'. Expected 'asc' or 'desc' "
                                + "at position "
                                + expectedPosition
                                + ".",
                        exception.getMessage()
                ),
                () -> assertEquals(
                        "Unsupported order-by direction: '"
                                + direction
                                + "'. Expected 'asc' or 'desc'",
                        cause.getMessage()
                )
        );
    }

    @Test
    void shouldRejectUnexpectedCharacterAfterProperty() {
        OrderByParserException exception =
                assertThrows(
                        OrderByParserException.class,
                        () -> parse(
                                "Title @"
                        )
                );

        assertAll(
                () -> assertEquals(
                        6,
                        exception.position()
                ),
                () -> assertEquals(
                        "Expected 'asc', 'desc', ',' or end of input "
                                + "at position 6.",
                        exception.getMessage()
                )
        );
    }

    @Test
    void shouldRejectUnexpectedTextAfterDirection() {
        OrderByParserException exception =
                assertThrows(
                        OrderByParserException.class,
                        () -> parse(
                                "Title desc extra"
                        )
                );

        assertAll(
                () -> assertEquals(
                        11,
                        exception.position()
                ),
                () -> assertEquals(
                        "Expected ',' or end of input at position 11.",
                        exception.getMessage()
                )
        );
    }

    @Test
    void shouldTreatDirectionKeywordsAsCaseSensitive() {
        OrderByParserException upperCase =
                assertThrows(
                        OrderByParserException.class,
                        () -> parse(
                                "Title DESC"
                        )
                );

        OrderByParserException mixedCase =
                assertThrows(
                        OrderByParserException.class,
                        () -> parse(
                                "Title Asc"
                        )
                );

        assertAll(
                () -> assertEquals(
                        6,
                        upperCase.position()
                ),
                () -> assertEquals(
                        6,
                        mixedCase.position()
                )
        );
    }

    private static OrderByOption parse(
            String input
    ) {
        return new OrderByParser(input).parse();
    }

    private static void assertItem(
            OrderByItem item,
            List<String> expectedPathSegments,
            OrderByDirection expectedDirection,
            int expectedStart,
            int expectedEnd
    ) {
        assertAll(
                () -> assertEquals(
                        expectedPathSegments,
                        item.pathSegments()
                ),
                () -> assertEquals(
                        String.join(
                                "/",
                                expectedPathSegments
                        ),
                        item.externalPath()
                ),
                () -> assertEquals(
                        expectedDirection,
                        item.direction()
                ),
                () -> assertFalse(
                        item.isResolved()
                ),
                () -> assertSpan(
                        item,
                        expectedStart,
                        expectedEnd
                )
        );
    }

    private static void assertSpan(
            OrderByOption option,
            int expectedStart,
            int expectedEnd
    ) {
        assertAll(
                () -> assertEquals(
                        expectedStart,
                        option.sourceSpan().start()
                ),
                () -> assertEquals(
                        expectedEnd,
                        option.sourceSpan().end()
                )
        );
    }

    private static void assertSpan(
            OrderByItem item,
            int expectedStart,
            int expectedEnd
    ) {
        assertAll(
                () -> assertEquals(
                        expectedStart,
                        item.sourceSpan().start()
                ),
                () -> assertEquals(
                        expectedEnd,
                        item.sourceSpan().end()
                )
        );
    }

    private static Stream<Arguments> explicitDirectionCases() {
        return Stream.of(
                Arguments.of(
                        "explicit ascending",
                        "Title asc",
                        OrderByDirection.ASCENDING,
                        9
                ),
                Arguments.of(
                        "explicit descending",
                        "Title desc",
                        OrderByDirection.DESCENDING,
                        10
                )
        );
    }

    private static Stream<Arguments> malformedInputCases() {
        return Stream.of(
                Arguments.of(
                        "empty input",
                        "",
                        "Order-by option cannot be empty",
                        0
                ),
                Arguments.of(
                        "space-only input",
                        "   ",
                        "Order-by option cannot be empty",
                        3
                ),
                Arguments.of(
                        "leading comma",
                        ",Title",
                        "Expected a property name",
                        0
                ),
                Arguments.of(
                        "trailing comma",
                        "Title,",
                        "Expected another order-by item after ','",
                        6
                ),
                Arguments.of(
                        "trailing comma and whitespace",
                        "Title,  ",
                        "Expected another order-by item after ','",
                        8
                ),
                Arguments.of(
                        "duplicate comma",
                        "Title,,Amount",
                        "Expected a property name",
                        6
                ),
                Arguments.of(
                        "slash before first property",
                        "/Owner",
                        "Expected a property name",
                        0
                ),
                Arguments.of(
                        "trailing slash",
                        "Owner/",
                        "Expected a property name after '/'",
                        6
                ),
                Arguments.of(
                        "trailing slash and whitespace",
                        "Owner/  ",
                        "Expected a property name after '/'",
                        8
                ),
                Arguments.of(
                        "duplicate slash",
                        "Owner//Username",
                        "Expected a property name after '/'",
                        6
                ),
                Arguments.of(
                        "comma after leading whitespace",
                        "  ,Title",
                        "Expected a property name",
                        2
                ),
                Arguments.of(
                        "number cannot start a property",
                        "1Title",
                        "Expected a property name",
                        0
                ),
                Arguments.of(
                        "unexpected opening parenthesis",
                        "(Title)",
                        "Expected a property name",
                        0
                ),
                Arguments.of(
                        "unexpected closing parenthesis",
                        "Title)",
                        "Expected 'asc', 'desc', ',' or end of input",
                        5
                ),
                Arguments.of(
                        "unexpected equals sign",
                        "Title=asc",
                        "Expected 'asc', 'desc', ',' or end of input",
                        5
                ),
                Arguments.of(
                        "unexpected text after explicit direction",
                        "Title asc desc",
                        "Expected ',' or end of input",
                        10
                ),
                Arguments.of(
                        "missing comma between items",
                        "Title asc Amount desc",
                        "Expected ',' or end of input",
                        10
                )
        );
    }

    private static Stream<Arguments> unsupportedDirectionCases() {
        return Stream.of(
                Arguments.of(
                        "ascending",
                        6
                ),
                Arguments.of(
                        "descending",
                        6
                ),
                Arguments.of(
                        "up",
                        6
                ),
                Arguments.of(
                        "down",
                        6
                ),
                Arguments.of(
                        "ASC",
                        6
                ),
                Arguments.of(
                        "Desc",
                        6
                )
        );
    }
}