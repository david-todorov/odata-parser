package io.github.davidtodorov.odataparser;

import io.github.davidtodorov.odataparser.demo.CaseModel;
import io.github.davidtodorov.odataparser.demo.DepartmentModel;
import io.github.davidtodorov.odataparser.demo.UserModel;
import io.github.davidtodorov.odataparser.filter.ast.FilterExpression;
import io.github.davidtodorov.odataparser.filter.parser.FilterParser;
import io.github.davidtodorov.odataparser.filter.semantic.FilterExpressionResolver;
import io.github.davidtodorov.odataparser.filter.visitor.FilterAstPrinter;
import io.github.davidtodorov.odataparser.meta.*;
import io.github.davidtodorov.odataparser.meta.demo.CaseMetadata;
import io.github.davidtodorov.odataparser.meta.demo.DepartmentMetadata;
import io.github.davidtodorov.odataparser.meta.demo.UserMetadata;
import io.github.davidtodorov.odataparser.orderby.ast.OrderByItem;
import io.github.davidtodorov.odataparser.orderby.ast.OrderByOption;
import io.github.davidtodorov.odataparser.orderby.parser.OrderByParser;
import io.github.davidtodorov.odataparser.orderby.semantic.OrderByResolver;
import io.github.davidtodorov.odataparser.orderby.visitor.OrderByAstPrinter;
import io.github.davidtodorov.odataparser.search.ast.SearchExpression;
import io.github.davidtodorov.odataparser.search.lexer.SearchLexer;
import io.github.davidtodorov.odataparser.search.lexer.SearchToken;
import io.github.davidtodorov.odataparser.search.parser.SearchParser;
import io.github.davidtodorov.odataparser.search.visitor.SearchAstPrinter;

import java.util.List;

public final class Main {

    private static final int OUTPUT_WIDTH = 100;

    private static int passedChecks;
    private static int failedChecks;

    private Main() {
    }

    public static void main(String[] args) {
        DepartmentMetadata departmentMetadata =
                new DepartmentMetadata();

        UserMetadata userMetadata =
                new UserMetadata();

        CaseMetadata caseMetadata =
                new CaseMetadata();

        MetadataRegistry registry =
                MetadataRegistry.of(
                        caseMetadata,
                        userMetadata,
                        departmentMetadata
                );

        printApplicationHeader();

        runSuccessfulCheck(
                "Connected metadata graph",
                () -> demonstrateMetadataGraph(
                        registry,
                        caseMetadata,
                        userMetadata,
                        departmentMetadata
                )
        );

        runSuccessfulCheck(
                "$filter with connected metadata",
                () -> demonstrateFilter(caseMetadata)
        );

        runSuccessfulCheck(
                "$orderby with connected metadata",
                () -> demonstrateOrderBy(caseMetadata)
        );

        runSuccessfulCheck(
                "$search parsing",
                Main::demonstrateSearch
        );

        demonstrateExpectedFailures(caseMetadata);

        printFinalSummary();
    }

    private static void demonstrateMetadataGraph(
            MetadataRegistry registry,
            EntityMetadata<CaseModel> caseMetadata,
            EntityMetadata<UserModel> userMetadata,
            EntityMetadata<DepartmentModel> departmentMetadata
    ) {
        printInputHeader("METADATA REGISTRY");

        System.out.println("Registered metadata count: " + registry.size());
        System.out.println("Registered names: " + registry.byName().keySet());
        System.out.println();

        for (EntityMetadata<?> metadata : registry.all()) {
            printEntityMetadata(metadata);
        }

        NavigationPropertyMetadata<CaseModel, UserModel> owner =
                requireNavigation(
                        caseMetadata,
                        "Owner",
                        UserModel.class
                );

        NavigationPropertyMetadata<CaseModel, UserModel> reviewer =
                requireNavigation(
                        caseMetadata,
                        "Reviewer",
                        UserModel.class
                );

        NavigationPropertyMetadata<CaseModel, DepartmentModel>
                caseDepartment =
                requireNavigation(
                        caseMetadata,
                        "Department",
                        DepartmentModel.class
                );

        NavigationPropertyMetadata<UserModel, DepartmentModel>
                userDepartment =
                requireNavigation(
                        userMetadata,
                        "Department",
                        DepartmentModel.class
                );

        printSmallHeader("DIRECT CONNECTION CHECKS");

        verify(
                "Case.Owner directly references UserMetadata",
                owner.targetMetadata() == userMetadata
        );

        verify(
                "Case.Reviewer directly references UserMetadata",
                reviewer.targetMetadata() == userMetadata
        );

        verify(
                "Owner and Reviewer reuse the same metadata instance",
                owner.targetMetadata()
                        == reviewer.targetMetadata()
        );

        verify(
                "Case.Department directly references DepartmentMetadata",
                caseDepartment.targetMetadata()
                        == departmentMetadata
        );

        verify(
                "User.Department reuses the same DepartmentMetadata",
                userDepartment.targetMetadata()
                        == departmentMetadata
        );


        printSmallHeader("JOIN POLICY CHECKS");

        verify(
                "Case.Owner defaults to LEFT join",
                owner.defaultJoinType()
                        == NavigationJoinType.LEFT
        );

        verify(
                "Case.Owner join type is overridable",
                owner.isJoinTypeOverridable()
        );

        verify(
                "Case.Reviewer defaults to LEFT join",
                reviewer.defaultJoinType()
                        == NavigationJoinType.LEFT
        );

        verify(
                "Case.Department defaults to LEFT join",
                caseDepartment.defaultJoinType()
                        == NavigationJoinType.LEFT
        );

        verify(
                "User.Department defaults to LEFT join",
                userDepartment.defaultJoinType()
                        == NavigationJoinType.LEFT
        );
    }

    private static void printEntityMetadata(
            EntityMetadata<?> metadata
    ) {
        System.out.println(
                metadata.name()
                        + " | entity-type="
                        + metadata.entityType().getTypeName()
                        + " | property-count="
                        + metadata.propertyCount()
        );

        for (PropertyMetadata<?, ?> property
                : metadata.properties().values()) {

            if (property
                    instanceof PrimitivePropertyMetadata<?, ?> primitive) {

                System.out.println(
                        "  PRIMITIVE "
                                + primitive.externalName()
                                + " -> "
                                + primitive.mappedName()
                                + " | java-type="
                                + primitive.javaType().getTypeName()
                                + " | expression-type="
                                + primitive.expressionType()
                );

                continue;
            }

            if (property
                    instanceof NavigationPropertyMetadata<?, ?> navigation) {

                System.out.println(
                        "  NAVIGATION "
                                + navigation.externalName()
                                + " -> "
                                + navigation.mappedName()
                                + " | cardinality="
                                + navigation.cardinality()
                                + " | target="
                                + navigation.targetMetadata().name()
                                + " | target-type="
                                + navigation.javaType().getTypeName()
                                + " | default-join="
                                + navigation.defaultJoinType()
                                + " | join-policy="
                                + (
                                navigation.isJoinTypeOverridable()
                                        ? "OVERRIDABLE"
                                        : "FIXED"
                        )
                );
            }
        }

        System.out.println();
    }

    private static void demonstrateFilter(
            EntityMetadata<CaseModel> caseMetadata
    ) {
        String filterText =
                "contains(Title, 'urgent') "
                        + "and Amount gt 100.50 "
                        + "and Deleted eq false "
                        + "and Owner/Active eq true "
                        + "and Owner/Department/Code eq 'ENG' "
                        + "and Reviewer/Email eq 'reviewer@example.com' "
                        + "and Department/Enabled eq true";

        printOptionInput(
                "$filter",
                filterText
        );

        FilterExpression parsedExpression =
                new FilterParser(filterText).parse();

        FilterAstPrinter printer =
                new FilterAstPrinter();

        printOutputBlock(
                "UNRESOLVED FILTER AST",
                printer.print(parsedExpression)
        );

        FilterExpression resolvedExpression =
                new FilterExpressionResolver(
                        caseMetadata
                ).resolve(parsedExpression);

        printOutputBlock(
                "RESOLVED FILTER AST USING CONNECTED METADATA",
                printer.print(resolvedExpression)
        );
    }

    private static void demonstrateOrderBy(
            EntityMetadata<CaseModel> caseMetadata
    ) {
        String orderByText =
                "Title, "
                        + "Owner/Username desc, "
                        + "Owner/Department/Code asc, "
                        + "Id desc";

        printOptionInput(
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
                        caseMetadata
                ).resolve(parsedOption);

        printOutputBlock(
                "RESOLVED ORDER-BY AST USING CONNECTED METADATA",
                printer.print(resolvedOption)
        );

        printSmallHeader("RESOLVED ORDER-BY ITEMS");

        for (OrderByItem item : resolvedOption) {
            System.out.println(
                    item.externalPath()
                            + " -> "
                            + item.mappedPath().orElseThrow()
                            + " | direction="
                            + item.direction()
                            + " | type="
                            + item.expressionType().orElseThrow()
            );
        }
    }

    private static void demonstrateSearch() {
        String searchText =
                "urgent AND "
                        + "(\"payment problem\" OR invoice) "
                        + "NOT archived";

        printOptionInput(
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

    private static void demonstrateExpectedFailures(
            EntityMetadata<CaseModel> caseMetadata
    ) {
        printMainHeader("EXPECTED FAILURE CHECKS");

        expectFailure(
                "$filter rejects an unknown metadata property",
                () -> {
                    FilterExpression expression =
                            new FilterParser(
                                    "UnknownProperty eq 'test'"
                            ).parse();

                    new FilterExpressionResolver(
                            caseMetadata
                    ).resolve(expression);
                }
        );

        expectFailure(
                "$filter rejects traversal through a primitive property",
                () -> {
                    FilterExpression expression =
                            new FilterParser(
                                    "Title/Value eq 'test'"
                            ).parse();

                    new FilterExpressionResolver(
                            caseMetadata
                    ).resolve(expression);
                }
        );

        expectFailure(
                "$filter rejects a navigation property as the final scalar value",
                () -> {
                    FilterExpression expression =
                            new FilterParser(
                                    "Owner eq null"
                            ).parse();

                    new FilterExpressionResolver(
                            caseMetadata
                    ).resolve(expression);
                }
        );

        expectFailure(
                "$orderby rejects an unknown metadata property",
                () -> {
                    OrderByOption option =
                            new OrderByParser(
                                    "UnknownProperty desc"
                            ).parse();

                    new OrderByResolver(
                            caseMetadata
                    ).resolve(option);
                }
        );

        expectFailure(
                "$orderby rejects a navigation property as the final segment",
                () -> {
                    OrderByOption option =
                            new OrderByParser(
                                    "Owner asc"
                            ).parse();

                    new OrderByResolver(
                            caseMetadata
                    ).resolve(option);
                }
        );

        expectFailure(
                "$search rejects an incomplete OR expression",
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

    private static <O, T>
    NavigationPropertyMetadata<O, T> requireNavigation(
            EntityMetadata<O> ownerMetadata,
            String externalName,
            Class<T> expectedTargetType
    ) {
        PropertyMetadata<O, ?> property =
                ownerMetadata.requireProperty(externalName);

        if (!(property
                instanceof NavigationPropertyMetadata<?, ?> navigation)) {

            throw new IllegalStateException(
                    "Property '"
                            + ownerMetadata.name()
                            + "."
                            + externalName
                            + "' is not a navigation property"
            );
        }

        if (!expectedTargetType.equals(
                navigation.javaType()
        )) {
            throw new IllegalStateException(
                    "Navigation property '"
                            + ownerMetadata.name()
                            + "."
                            + externalName
                            + "' targets Java type '"
                            + navigation.javaType().getName()
                            + "' instead of expected type '"
                            + expectedTargetType.getName()
                            + "'"
            );
        }

        @SuppressWarnings("unchecked")
        NavigationPropertyMetadata<O, T> typedNavigation =
                (NavigationPropertyMetadata<O, T>) navigation;

        return typedNavigation;
    }

    private static void runSuccessfulCheck(
            String description,
            Runnable operation
    ) {
        printMainHeader(description);

        try {
            operation.run();

            passedChecks++;

            System.out.println();
            System.out.println(
                    "[PASS] "
                            + description
                            + " completed successfully."
            );
        } catch (RuntimeException exception) {
            failedChecks++;

            System.out.println();
            System.out.println(
                    "[FAIL] "
                            + description
            );

            System.out.println(
                    "Exception: "
                            + exception.getClass().getName()
            );

            System.out.println(
                    "Message: "
                            + exception.getMessage()
            );

            exception.printStackTrace(System.out);
        }
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
                    "[PASS] Input was rejected as expected."
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

    private static void verify(
            String description,
            boolean condition
    ) {
        if (!condition) {
            throw new IllegalStateException(
                    "Metadata graph check failed: "
                            + description
            );
        }

        System.out.println(
                "[PASS] " + description
        );
    }

    private static void printApplicationHeader() {
        System.out.println();
        System.out.println("=".repeat(OUTPUT_WIDTH));
        System.out.println(
                centerText(
                        "ODATA PARSER — CONNECTED METADATA SYSTEM"
                )
        );
        System.out.println("=".repeat(OUTPUT_WIDTH));
    }

    private static void printMainHeader(
            String title
    ) {
        System.out.println();
        System.out.println();
        System.out.println("=".repeat(OUTPUT_WIDTH));
        System.out.println(
                centerText(
                        title.toUpperCase()
                )
        );
        System.out.println("=".repeat(OUTPUT_WIDTH));
    }

    private static void printInputHeader(
            String title
    ) {
        printSmallHeader(title);
    }

    private static void printOptionInput(
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
                centerText("TEST SUMMARY")
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
                    "[SUCCESS] The connected metadata system works correctly."
            );
        } else {
            System.out.println(
                    "[WARNING] One or more checks failed."
            );
        }

        System.out.println("=".repeat(OUTPUT_WIDTH));
    }
}