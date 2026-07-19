package io.github.davidtodorov.odataparser.orderby.visitor;

import io.github.davidtodorov.odataparser.meta.MetadataRegistry;
import io.github.davidtodorov.odataparser.meta.demo.CaseMetadata;
import io.github.davidtodorov.odataparser.meta.demo.DepartmentMetadata;
import io.github.davidtodorov.odataparser.meta.demo.UserMetadata;
import io.github.davidtodorov.odataparser.orderby.ast.OrderByOption;
import io.github.davidtodorov.odataparser.orderby.parser.OrderByParser;
import io.github.davidtodorov.odataparser.orderby.semantic.OrderByResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderByAstPrinterTest {

    private OrderByResolver resolver;
    private OrderByAstPrinter printer;

    @BeforeEach
    void setUp() {
        DepartmentMetadata departmentMetadata =
                new DepartmentMetadata();

        UserMetadata userMetadata =
                new UserMetadata();

        CaseMetadata caseMetadata =
                new CaseMetadata();

        MetadataRegistry.of(
                caseMetadata,
                userMetadata,
                departmentMetadata
        );

        resolver =
                new OrderByResolver(
                        caseMetadata
                );

        printer =
                new OrderByAstPrinter();
    }

    @Test
    void shouldPrintUnresolvedSingleItem() {
        OrderByOption parsed =
                parse("Title");

        String output =
                printer.print(parsed);

        assertContains(
                output,
                "ORDER BY | items=1 | resolution=UNRESOLVED | span=[0, 5)",
                "ITEM 1 | direction=ASCENDING | keyword=asc | span=[0, 5)",
                "external-path=Title",
                "resolution=UNRESOLVED"
        );

        assertAll(
                () -> assertFalse(
                        output.contains("mapped-path=")
                ),
                () -> assertFalse(
                        output.contains("root-metadata=")
                ),
                () -> assertFalse(
                        output.contains("segments:")
                )
        );
    }

    @Test
    void shouldPrintMultipleUnresolvedItemsInSourceOrder() {
        String input =
                "Title, Owner/Username desc, Id asc";

        String output =
                printer.print(
                        parse(input)
                );

        assertContains(
                output,
                "ORDER BY | items=3 | resolution=UNRESOLVED | span=[0, 34)",
                "ITEM 1 | direction=ASCENDING | keyword=asc | span=[0, 5)",
                "external-path=Title",
                "ITEM 2 | direction=DESCENDING | keyword=desc | span=[7, 26)",
                "external-path=Owner/Username",
                "ITEM 3 | direction=ASCENDING | keyword=asc | span=[28, 34)",
                "external-path=Id"
        );

        assertInOrder(
                output,
                "external-path=Title",
                "external-path=Owner/Username",
                "external-path=Id"
        );
    }

    @Test
    void shouldPrintResolvedPrimitivePropertyDetails() {
        String output =
                printer.print(
                        parseAndResolve(
                                "Title desc"
                        )
                );

        assertContains(
                output,
                "ORDER BY | items=1 | resolution=RESOLVED | span=[0, 10)",
                "ITEM 1 | direction=DESCENDING | keyword=desc | span=[0, 10)",
                "external-path=Title",
                "resolution=RESOLVED",
                "mapped-path=title",
                "type=STRING",
                "java-type=java.lang.String",
                "root-metadata=Case",
                "segments:",
                "Title -> title",
                "declared-in=Case",
                "declaring-type=io.github.davidtodorov.odataparser.demo.CaseModel",
                "kind=PRIMITIVE"
        );
    }

    @Test
    void shouldPrintResolvedNavigationPathAndJoinPolicies() {
        String output =
                printer.print(
                        parseAndResolve(
                                "Owner/Department/Code asc"
                        )
                );

        assertContains(
                output,
                "ORDER BY | items=1 | resolution=RESOLVED | span=[0, 25)",
                "ITEM 1 | direction=ASCENDING | keyword=asc | span=[0, 25)",
                "external-path=Owner/Department/Code",
                "mapped-path=owner/department/code",
                "type=STRING",
                "java-type=java.lang.String",
                "root-metadata=Case",
                "Owner -> owner",
                "declared-in=Case",
                "kind=NAVIGATION",
                "cardinality=SINGLE",
                "target-metadata=User",
                "target-type=io.github.davidtodorov.odataparser.demo.UserModel",
                "default-join=LEFT",
                "join-policy=OVERRIDABLE",
                "Department -> department",
                "declared-in=User",
                "target-metadata=Department",
                "target-type=io.github.davidtodorov.odataparser.demo.DepartmentModel",
                "Code -> code",
                "declared-in=Department",
                "kind=PRIMITIVE"
        );

        assertInOrder(
                output,
                "Owner -> owner",
                "Department -> department",
                "Code -> code"
        );
    }

    @Test
    void shouldPrintResolvedMultipleItemsWithTheirOwnDirectionsAndTypes() {
        String output =
                printer.print(
                        parseAndResolve(
                                "Title, Amount desc, Priority asc, Deleted"
                        )
                );

        assertContains(
                output,
                "ORDER BY | items=4 | resolution=RESOLVED",
                "ITEM 1 | direction=ASCENDING | keyword=asc",
                "external-path=Title",
                "mapped-path=title",
                "type=STRING",
                "ITEM 2 | direction=DESCENDING | keyword=desc",
                "external-path=Amount",
                "mapped-path=amount",
                "type=DECIMAL",
                "ITEM 3 | direction=ASCENDING | keyword=asc",
                "external-path=Priority",
                "mapped-path=priority",
                "type=INTEGER",
                "ITEM 4 | direction=ASCENDING | keyword=asc",
                "external-path=Deleted",
                "mapped-path=deleted",
                "type=BOOLEAN"
        );

        assertInOrder(
                output,
                "external-path=Title",
                "external-path=Amount",
                "external-path=Priority",
                "external-path=Deleted"
        );
    }

    @Test
    void shouldPrintUnknownSourceSpans() {
        OrderByOption option =
                new OrderByOption(
                        java.util.List.of(
                                new io.github.davidtodorov.odataparser.orderby.ast.OrderByItem(
                                        java.util.List.of("Title"),
                                        io.github.davidtodorov.odataparser.orderby.ast.OrderByDirection.ASCENDING
                                )
                        )
                );

        String output =
                printer.print(option);

        assertContains(
                output,
                "ORDER BY | items=1 | resolution=UNRESOLVED | span=UNKNOWN",
                "ITEM 1 | direction=ASCENDING | keyword=asc | span=UNKNOWN"
        );
    }

    @Test
    void shouldProduceDeterministicOutputAcrossRepeatedCalls() {
        OrderByOption resolved =
                parseAndResolve(
                        "Title, Owner/Username desc"
                );

        String first =
                printer.print(resolved);

        String second =
                printer.print(resolved);

        assertEquals(
                first,
                second
        );
    }

    @Test
    void shouldNotLeakOutputBetweenPrintCalls() {
        String first =
                printer.print(
                        parseAndResolve(
                                "Title desc"
                        )
                );

        String second =
                printer.print(
                        parseAndResolve(
                                "Deleted asc"
                        )
                );

        assertAll(
                () -> assertTrue(
                        first.contains(
                                "external-path=Title"
                        )
                ),
                () -> assertFalse(
                        first.contains(
                                "external-path=Deleted"
                        )
                ),
                () -> assertTrue(
                        second.contains(
                                "external-path=Deleted"
                        )
                ),
                () -> assertFalse(
                        second.contains(
                                "external-path=Title"
                        )
                )
        );
    }

    @Test
    void shouldRejectNullOption() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> printer.print(null)
                );

        assertEquals(
                "Order-by option cannot be null",
                exception.getMessage()
        );
    }

    private OrderByOption parseAndResolve(
            String input
    ) {
        return resolver.resolve(
                parse(input)
        );
    }

    private static OrderByOption parse(
            String input
    ) {
        return new OrderByParser(input).parse();
    }

    private static void assertContains(
            String output,
            String... expectedFragments
    ) {
        for (String expectedFragment : expectedFragments) {
            assertTrue(
                    output.contains(expectedFragment),
                    () -> "Expected output to contain:"
                            + System.lineSeparator()
                            + expectedFragment
                            + System.lineSeparator()
                            + System.lineSeparator()
                            + "Actual output:"
                            + System.lineSeparator()
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
                            + System.lineSeparator()
                            + expectedFragment
                            + System.lineSeparator()
                            + System.lineSeparator()
                            + "Actual output:"
                            + System.lineSeparator()
                            + output
            );

            assertTrue(
                    currentPosition > previousPosition,
                    () -> "Expected fragment to appear after the previous fragment:"
                            + System.lineSeparator()
                            + expectedFragment
                            + System.lineSeparator()
                            + System.lineSeparator()
                            + "Actual output:"
                            + System.lineSeparator()
                            + output
            );

            previousPosition =
                    currentPosition;
        }
    }
}