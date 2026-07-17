package io.github.davidtodorov.odataparser;

import io.github.davidtodorov.odataparser.demo.CaseSchemaFactory;
import io.github.davidtodorov.odataparser.demo.DepartmentSchemaFactory;
import io.github.davidtodorov.odataparser.demo.UserSchemaFactory;
import io.github.davidtodorov.odataparser.filter.ast.FilterExpression;
import io.github.davidtodorov.odataparser.filter.parser.FilterParser;
import io.github.davidtodorov.odataparser.filter.semantic.FilterExpressionResolver;
import io.github.davidtodorov.odataparser.filter.visitor.FilterAstPrinter;
import io.github.davidtodorov.odataparser.orderby.ast.OrderByOption;
import io.github.davidtodorov.odataparser.orderby.parser.OrderByParser;
import io.github.davidtodorov.odataparser.orderby.semantic.OrderByResolver;
import io.github.davidtodorov.odataparser.orderby.visitor.OrderByAstPrinter;
import io.github.davidtodorov.odataparser.schema.EntitySchema;
import io.github.davidtodorov.odataparser.schema.SchemaRegistry;
import io.github.davidtodorov.odataparser.search.ast.SearchExpression;
import io.github.davidtodorov.odataparser.search.lexer.SearchLexer;
import io.github.davidtodorov.odataparser.search.lexer.SearchToken;
import io.github.davidtodorov.odataparser.search.parser.SearchParser;
import io.github.davidtodorov.odataparser.search.visitor.SearchAstPrinter;

import java.util.List;

public final class Main {

    private static final int OUTPUT_WIDTH = 88;

    private static int passedChecks;
    private static int failedChecks;

    private Main() {
    }

    public static void main(String[] args) {
        EntitySchema caseSchema =
                CaseSchemaFactory.create();

        EntitySchema userSchema =
                UserSchemaFactory.create();

        EntitySchema departmentSchema =
                DepartmentSchemaFactory.create();

        SchemaRegistry registry =
                SchemaRegistry.of(
                        caseSchema,
                        userSchema,
                        departmentSchema
                );

        printApplicationHeader();

        runSuccessfulDemo(
                "$filter valid expression",
                () -> demonstrateFilter(
                        caseSchema,
                        registry
                )
        );

        runSuccessfulDemo(
                "$orderby valid expression",
                () -> demonstrateOrderBy(
                        caseSchema,
                        registry
                )
        );

        runSuccessfulDemo(
                "$search valid expression",
                Main::demonstrateSearch
        );

        demonstrateExpectedFailures(
                caseSchema,
                registry
        );

        printFinalSummary();
    }

    // -------------------------------------------------------------------------
    // $filter
    // -------------------------------------------------------------------------

    private static void demonstrateFilter(
            EntitySchema caseSchema,
            SchemaRegistry registry
    ) {
        String filterText =
                "contains(Title, 'urgent') "
                        + "and Amount gt 100.50 "
                        + "and Deleted eq false "
                        + "and Owner/Active eq true "
                        + "and Owner/Department/Code eq 'ENG' "
                        + "and Reviewer/Email eq 'reviewer@example.com' "
                        + "and Department/Enabled eq true";

        printInput(
                "$filter",
                filterText
        );

        FilterExpression parsedFilterExpression =
                new FilterParser(filterText).parse();

        FilterAstPrinter printer =
                new FilterAstPrinter();

        printOutputBlock(
                "UNRESOLVED FILTER AST",
                printer.print(parsedFilterExpression)
        );

        FilterExpression resolvedFilterExpression =
                new FilterExpressionResolver(
                        caseSchema,
                        registry
                ).resolve(parsedFilterExpression);

        printOutputBlock(
                "RESOLVED FILTER AST",
                printer.print(resolvedFilterExpression)
        );
    }

    // -------------------------------------------------------------------------
    // $orderby
    // -------------------------------------------------------------------------

    private static void demonstrateOrderBy(
            EntitySchema caseSchema,
            SchemaRegistry registry
    ) {
        String orderByText =
                "Title, "
                        + "Owner/Username desc, "
                        + "Owner/Department/Code asc, "
                        + "Id desc";

        printInput(
                "$orderby",
                orderByText
        );

        OrderByOption parsedOption =
                new OrderByParser(orderByText).parse();

        OrderByAstPrinter printer =
                new OrderByAstPrinter();

        printOutputBlock(
                "UNRESOLVED ORDER-BY AST",
                printer.print(parsedOption)
        );

        OrderByOption resolvedOption =
                new OrderByResolver(
                        caseSchema,
                        registry
                ).resolve(parsedOption);

        printOutputBlock(
                "RESOLVED ORDER-BY AST",
                printer.print(resolvedOption)
        );
    }

    // -------------------------------------------------------------------------
    // $search
    // -------------------------------------------------------------------------

    private static void demonstrateSearch() {
        String searchText =
                "urgent AND "
                        + "(\"payment problem\" OR invoice) "
                        + "NOT archived";

        printInput(
                "$search",
                searchText
        );

        List<SearchToken> tokens =
                new SearchLexer(searchText).tokenize();

        printSearchTokens(tokens);

        SearchExpression expression =
                new SearchParser(searchText).parse();

        SearchAstPrinter printer =
                new SearchAstPrinter();

        printOutputBlock(
                "SEARCH AST",
                printer.print(expression)
        );
    }

    private static void printSearchTokens(
            List<SearchToken> tokens
    ) {
        printSmallHeader("SEARCH TOKENS");

        System.out.printf(
                "%-20s | %-24s | %-24s | %s%n",
                "TYPE",
                "LEXEME",
                "VALUE",
                "SOURCE SPAN"
        );

        System.out.println("-".repeat(OUTPUT_WIDTH));

        for (SearchToken token : tokens) {
            System.out.printf(
                    "%-20s | %-24s | %-24s | [%d, %d)%n",
                    token.type(),
                    displayTokenText(token.lexeme()),
                    displayTokenText(token.value()),
                    token.sourceSpan().start(),
                    token.sourceSpan().end()
            );
        }

        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Expected failure checks
    // -------------------------------------------------------------------------

    private static void demonstrateExpectedFailures(
            EntitySchema caseSchema,
            SchemaRegistry registry
    ) {
        printMainHeader("EXPECTED FAILURE CHECKS");

        expectFailure(
                "$filter rejects unknown properties",
                () -> {
                    FilterExpression filterExpression =
                            new FilterParser(
                                    "UnknownProperty eq 'test'"
                            ).parse();

                    new FilterExpressionResolver(
                            caseSchema,
                            registry
                    ).resolve(filterExpression);
                }
        );

        expectFailure(
                "$orderby rejects functions",
                () -> new OrderByParser(
                        "tolower(Title) asc"
                ).parse()
        );

        expectFailure(
                "$orderby rejects unknown properties",
                () -> {
                    OrderByOption option =
                            new OrderByParser(
                                    "UnknownProperty desc"
                            ).parse();

                    new OrderByResolver(
                            caseSchema,
                            registry
                    ).resolve(option);
                }
        );

        expectFailure(
                "$search rejects incomplete OR expressions",
                () -> new SearchParser(
                        "urgent OR"
                ).parse()
        );

        expectFailure(
                "$search rejects empty parentheses",
                () -> new SearchParser(
                        "urgent AND ()"
                ).parse()
        );

        expectFailure(
                "$search rejects lowercase operators",
                () -> new SearchParser(
                        "urgent and invoice"
                ).parse()
        );
    }

    private static void expectFailure(
            String description,
            Runnable operation
    ) {
        System.out.println();
        System.out.println("CHECK: " + description);

        try {
            operation.run();

            failedChecks++;

            System.out.println(
                    "[FAIL] No exception was thrown."
            );
        } catch (RuntimeException exception) {
            passedChecks++;

            System.out.println(
                    "[PASS] Expression was rejected as expected."
            );

            System.out.println(
                    "       Exception: "
                            + exception.getClass().getSimpleName()
            );

            System.out.println(
                    "       Message:   "
                            + exception.getMessage()
            );
        }
    }

    // -------------------------------------------------------------------------
    // Demo execution
    // -------------------------------------------------------------------------

    private static void runSuccessfulDemo(
            String description,
            Runnable demonstration
    ) {
        printMainHeader(description);

        try {
            demonstration.run();

            passedChecks++;

            System.out.println(
                    "[PASS] "
                            + description
                            + " completed successfully."
            );
        } catch (RuntimeException exception) {
            failedChecks++;

            System.out.println(
                    "[FAIL] "
                            + description
                            + " failed."
            );

            System.out.println(
                    "Exception: "
                            + exception.getClass().getName()
            );

            System.out.println(
                    "Message: "
                            + exception.getMessage()
            );

            System.out.println();
            exception.printStackTrace(System.out);
        }
    }

    // -------------------------------------------------------------------------
    // Visual formatting
    // -------------------------------------------------------------------------

    private static void printApplicationHeader() {
        System.out.println();
        System.out.println("=".repeat(OUTPUT_WIDTH));
        System.out.println(
                centerText(
                        "ODATA QUERY-OPTION PARSER PLAYGROUND"
                )
        );
        System.out.println("=".repeat(OUTPUT_WIDTH));
        System.out.println();
        System.out.println(
                "Visual demonstrations for $filter, $orderby and $search"
        );
    }

    private static void printMainHeader(
            String title
    ) {
        System.out.println();
        System.out.println();
        System.out.println("=".repeat(OUTPUT_WIDTH));
        System.out.println(
                centerText(title.toUpperCase())
        );
        System.out.println("=".repeat(OUTPUT_WIDTH));
    }

    private static void printInput(
            String optionName,
            String value
    ) {
        printSmallHeader("INPUT");

        System.out.println(
                optionName + "=" + value
        );

        System.out.println();
    }

    private static void printOutputBlock(
            String title,
            String content
    ) {
        printSmallHeader(title);
        System.out.println(content);
        System.out.println();
    }

    private static void printSmallHeader(
            String title
    ) {
        System.out.println();
        System.out.println("-".repeat(OUTPUT_WIDTH));
        System.out.println(title);
        System.out.println("-".repeat(OUTPUT_WIDTH));
    }

    private static String centerText(
            String text
    ) {
        if (text.length() >= OUTPUT_WIDTH) {
            return text;
        }

        int leftPadding =
                (OUTPUT_WIDTH - text.length()) / 2;

        return " ".repeat(leftPadding) + text;
    }

    private static String displayTokenText(
            String value
    ) {
        if (value.isEmpty()) {
            return "<empty>";
        }

        return "\""
                + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t")
                + "\"";
    }

    private static void printFinalSummary() {
        int totalChecks =
                passedChecks + failedChecks;

        System.out.println();
        System.out.println();
        System.out.println("=".repeat(OUTPUT_WIDTH));
        System.out.println(
                centerText("DEMO SUMMARY")
        );
        System.out.println("=".repeat(OUTPUT_WIDTH));

        System.out.println(
                "Total checks : " + totalChecks
        );

        System.out.println(
                "Passed       : " + passedChecks
        );

        System.out.println(
                "Failed       : " + failedChecks
        );

        System.out.println();

        if (failedChecks == 0) {
            System.out.println(
                    "[SUCCESS] All visual checks completed successfully."
            );
        } else {
            System.out.println(
                    "[WARNING] One or more visual checks failed."
            );
        }

        System.out.println("=".repeat(OUTPUT_WIDTH));
    }
}