package io.github.davidtodorov.odataparser.filter.visitor;

import io.github.davidtodorov.odataparser.filter.ast.FilterExpression;
import io.github.davidtodorov.odataparser.filter.parser.FilterParser;
import io.github.davidtodorov.odataparser.filter.semantic.FilterExpressionResolver;
import io.github.davidtodorov.odataparser.meta.MetadataRegistry;
import io.github.davidtodorov.odataparser.meta.demo.CaseMetadata;
import io.github.davidtodorov.odataparser.meta.demo.DepartmentMetadata;
import io.github.davidtodorov.odataparser.meta.demo.UserMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilterAstPrinterTest {

    private CaseMetadata caseMetadata;
    private FilterExpressionResolver resolver;
    private FilterAstPrinter printer;

    @BeforeEach
    void setUp() {
        DepartmentMetadata departmentMetadata =
                new DepartmentMetadata();

        UserMetadata userMetadata =
                new UserMetadata();

        caseMetadata =
                new CaseMetadata();

        MetadataRegistry.of(
                caseMetadata,
                userMetadata,
                departmentMetadata
        );

        resolver =
                new FilterExpressionResolver(
                        caseMetadata
                );

        printer =
                new FilterAstPrinter();
    }

    @Test
    void shouldPrintUnresolvedExpressionStructure() {
        String input =
                "contains(Title, 'urgent') "
                        + "and Amount gt 100.50";

        FilterExpression parsed =
                new FilterParser(input).parse();

        String output =
                printer.print(parsed);

        assertContains(
                output,
                "BINARY and | type=BOOLEAN | span=[0, 46)",
                "FUNCTION contains | type=UNKNOWN | span=[0, 25)",
                "argument-count=2",
                "PROPERTY Title | type=UNKNOWN | span=[9, 14)",
                "resolution=UNRESOLVED",
                "LITERAL STRING 'urgent' | type=STRING | span=[16, 24)",
                "value=\"urgent\"",
                "BINARY gt | type=BOOLEAN | span=[30, 46)",
                "PROPERTY Amount | type=UNKNOWN | span=[30, 36)",
                "LITERAL DECIMAL 100.50 | type=DECIMAL | span=[40, 46)",
                "value=100.50"
        );
    }

    @Test
    void shouldPrintResolvedPrimitivePropertyMetadata() {
        FilterExpression resolved =
                parseAndResolve(
                        "Title eq 'urgent'"
                );

        String output =
                printer.print(resolved);

        assertContains(
                output,
                "BINARY eq | type=BOOLEAN | span=[0, 17)",
                "PROPERTY Title | type=STRING | span=[0, 5)",
                "resolution=RESOLVED",
                "external-path=Title",
                "mapped-path=title",
                "java-type=java.lang.String",
                "root-metadata=Case",
                "segments:",
                "Title -> title",
                "declared-in=Case",
                "kind=PRIMITIVE",
                "type=STRING",
                "LITERAL STRING 'urgent' | type=STRING | span=[9, 17)",
                "value=\"urgent\""
        );
    }

    @Test
    void shouldPrintResolvedNavigationMetadataAndJoinPolicy() {
        FilterExpression resolved =
                parseAndResolve(
                        "Owner/Department/Code eq 'ENG'"
                );

        String output =
                printer.print(resolved);

        assertContains(
                output,
                "PROPERTY Owner/Department/Code | type=STRING",
                "resolution=RESOLVED",
                "external-path=Owner/Department/Code",
                "mapped-path=owner/department/code",
                "java-type=java.lang.String",
                "root-metadata=Case",
                "Owner -> owner",
                "declared-in=Case",
                "kind=NAVIGATION",
                "cardinality=SINGLE",
                "target-metadata=User",
                "default-join=LEFT",
                "join-policy=OVERRIDABLE",
                "Department -> department",
                "declared-in=User",
                "target-metadata=Department",
                "Code -> code",
                "declared-in=Department",
                "kind=PRIMITIVE",
                "type=STRING"
        );
    }

    @Test
    void shouldPrintFunctionAfterSemanticResolution() {
        FilterExpression resolved =
                parseAndResolve(
                        "contains(Title, 'O''Brien')"
                );

        String output =
                printer.print(resolved);

        assertContains(
                output,
                "FUNCTION contains | type=BOOLEAN",
                "argument-count=2",
                "PROPERTY Title | type=STRING",
                "resolution=RESOLVED",
                "LITERAL STRING 'O''Brien' | type=STRING",
                "value=\"O'Brien\""
        );
    }

    @Test
    void shouldPrintUnaryAndListExpressions() {
        String unaryOutput =
                printer.print(
                        parseAndResolve(
                                "not Deleted"
                        )
                );

        String listOutput =
                printer.print(
                        parseAndResolve(
                                "Priority in (1, 2, 3)"
                        )
                );

        assertAll(
                () -> assertContains(
                        unaryOutput,
                        "not",
                        "Deleted",
                        "BOOLEAN",
                        "resolution=RESOLVED"
                ),
                () -> assertContains(
                        listOutput,
                        "BINARY in",
                        "Priority",
                        "1",
                        "2",
                        "3",
                        "COLLECTION",
                        "resolution=RESOLVED"
                )
        );
    }

    @Test
    void shouldPrintEveryLiteralValueClearly() {
        String output =
                printer.print(
                        parseAndResolve(
                                "Title eq null "
                                        + "or Deleted eq true "
                                        + "or Amount eq 12.50 "
                                        + "or Priority eq -2"
                        )
                );

        assertContains(
                output,
                "LITERAL NULL null | type=NULL",
                "value=null",
                "LITERAL BOOLEAN true | type=BOOLEAN",
                "value=true",
                "LITERAL DECIMAL 12.50 | type=DECIMAL",
                "value=12.50",
                "LITERAL INTEGER -2 | type=INTEGER",
                "value=-2"
        );
    }

    @Test
    void shouldProduceDeterministicOutputAcrossRepeatedCalls() {
        FilterExpression resolved =
                parseAndResolve(
                        "Deleted and Closed"
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
    void shouldNotLeakOutputBetweenDifferentPrintCalls() {
        String first =
                printer.print(
                        parseAndResolve(
                                "Title eq 'urgent'"
                        )
                );

        String second =
                printer.print(
                        parseAndResolve(
                                "Deleted eq true"
                        )
                );

        assertAll(
                () -> assertTrue(
                        first.contains("Title")
                ),
                () -> assertFalse(
                        first.contains("Deleted")
                ),
                () -> assertTrue(
                        second.contains("Deleted")
                ),
                () -> assertFalse(
                        second.contains("Title")
                )
        );
    }

    @Test
    void shouldRejectNullRootExpression() {
        assertThrows(
                NullPointerException.class,
                () -> printer.print(null)
        );
    }

    private FilterExpression parseAndResolve(
            String input
    ) {
        FilterExpression parsed =
                new FilterParser(input).parse();

        return resolver.resolve(parsed);
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
}