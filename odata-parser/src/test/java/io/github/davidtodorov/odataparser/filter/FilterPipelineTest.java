package io.github.davidtodorov.odataparser.filter;

import io.github.davidtodorov.odataparser.common.type.ExpressionType;
import io.github.davidtodorov.odataparser.filter.ast.BinaryFilterExpression;
import io.github.davidtodorov.odataparser.filter.ast.BinaryOperator;
import io.github.davidtodorov.odataparser.filter.ast.FilterExpression;
import io.github.davidtodorov.odataparser.filter.ast.FunctionCallFilterExpression;
import io.github.davidtodorov.odataparser.filter.ast.ListFilterExpression;
import io.github.davidtodorov.odataparser.filter.ast.LiteralFilterExpression;
import io.github.davidtodorov.odataparser.filter.ast.PropertyFilterExpression;
import io.github.davidtodorov.odataparser.filter.ast.UnaryFilterExpression;
import io.github.davidtodorov.odataparser.filter.lexer.FilterLexer;
import io.github.davidtodorov.odataparser.filter.lexer.FilterLexerException;
import io.github.davidtodorov.odataparser.filter.parser.FilterParser;
import io.github.davidtodorov.odataparser.filter.parser.FilterParserException;
import io.github.davidtodorov.odataparser.filter.semantic.FilterExpressionResolver;
import io.github.davidtodorov.odataparser.filter.semantic.FilterSemanticException;
import io.github.davidtodorov.odataparser.meta.MetadataRegistry;
import io.github.davidtodorov.odataparser.meta.NavigationJoinType;
import io.github.davidtodorov.odataparser.meta.demo.CaseMetadata;
import io.github.davidtodorov.odataparser.meta.demo.DepartmentMetadata;
import io.github.davidtodorov.odataparser.meta.demo.UserMetadata;
import io.github.davidtodorov.odataparser.meta.path.ResolvedMetadataPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilterPipelineTest {

    private CaseMetadata caseMetadata;
    private UserMetadata userMetadata;
    private DepartmentMetadata departmentMetadata;
    private FilterExpressionResolver resolver;

    @BeforeEach
    void setUp() {
        departmentMetadata =
                new DepartmentMetadata();

        userMetadata =
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
    }

    @Test
    void shouldProcessCompleteRealisticFilterPipeline() {
        String input =
                "contains(Title, 'urgent') "
                        + "and Amount gt 100.50 "
                        + "and Deleted eq false "
                        + "and Owner/Active eq true "
                        + "and Owner/Department/Code eq 'ENG' "
                        + "and Reviewer/Email eq 'reviewer@example.com' "
                        + "and Department/Enabled eq true";

        FilterExpression resolved =
                parseAndResolve(input);

        assertAll(
                () -> assertEquals(
                        ExpressionType.BOOLEAN,
                        resolved.expressionType()
                ),
                () -> assertEquals(
                        0,
                        resolved.sourceSpan().start()
                ),
                () -> assertEquals(
                        input.length(),
                        resolved.sourceSpan().end()
                )
        );

        assertFullyResolved(resolved);
    }

    @Test
    void shouldResolveExpectedExternalAndMappedPropertyPaths() {
        FilterExpression resolved =
                parseAndResolve(
                        "Title eq 'urgent' "
                                + "and Owner/Active eq true "
                                + "and Owner/Department/Code eq 'ENG' "
                                + "and Department/Enabled eq true"
                );

        Map<String, ResolvedMetadataPath> paths =
                collectProperties(resolved)
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        PropertyFilterExpression::path,
                                        property ->
                                                property.resolvedPath()
                                                        .orElseThrow()
                                )
                        );

        assertAll(
                () -> assertEquals(
                        "title",
                        paths.get("Title").mappedPath()
                ),
                () -> assertEquals(
                        "owner/active",
                        paths.get("Owner/Active").mappedPath()
                ),
                () -> assertEquals(
                        "owner/department/code",
                        paths.get(
                                "Owner/Department/Code"
                        ).mappedPath()
                ),
                () -> assertEquals(
                        "department/enabled",
                        paths.get(
                                "Department/Enabled"
                        ).mappedPath()
                )
        );
    }

    @Test
    void shouldPreserveMetadataIdentityAndJoinPolicyAcrossDeepPath() {
        FilterExpression resolved =
                parseAndResolve(
                        "Owner/Department/Enabled eq true"
                );

        PropertyFilterExpression property =
                collectProperties(resolved)
                        .getFirst();

        ResolvedMetadataPath path =
                property.resolvedPath()
                        .orElseThrow();

        assertAll(
                () -> assertSame(
                        caseMetadata,
                        path.segments()
                                .get(0)
                                .declaringMetadata()
                ),
                () -> assertSame(
                        caseMetadata.requireProperty("Owner"),
                        path.segments()
                                .get(0)
                                .propertyMetadata()
                ),
                () -> assertSame(
                        userMetadata,
                        path.segments()
                                .get(0)
                                .targetMetadata()
                                .orElseThrow()
                ),
                () -> assertEquals(
                        NavigationJoinType.LEFT,
                        path.segments()
                                .get(0)
                                .defaultJoinType()
                                .orElseThrow()
                ),
                () -> assertTrue(
                        path.segments()
                                .get(0)
                                .isJoinTypeOverridable()
                ),
                () -> assertSame(
                        departmentMetadata,
                        path.segments()
                                .get(1)
                                .targetMetadata()
                                .orElseThrow()
                ),
                () -> assertSame(
                        departmentMetadata.requireProperty(
                                "Enabled"
                        ),
                        path.leaf().propertyMetadata()
                )
        );
    }

    @Test
    void shouldResolveNestedFunctionsThroughCompletePipeline() {
        FunctionCallFilterExpression contains =
                assertInstanceOf(
                        FunctionCallFilterExpression.class,
                        parseAndResolve(
                                "contains("
                                        + "tolower(trim(Title)),"
                                        + "'urgent'"
                                        + ")"
                        )
                );

        FunctionCallFilterExpression toLower =
                assertInstanceOf(
                        FunctionCallFilterExpression.class,
                        contains.arguments().getFirst()
                );

        FunctionCallFilterExpression trim =
                assertInstanceOf(
                        FunctionCallFilterExpression.class,
                        toLower.arguments().getFirst()
                );

        PropertyFilterExpression title =
                assertInstanceOf(
                        PropertyFilterExpression.class,
                        trim.arguments().getFirst()
                );

        assertAll(
                () -> assertEquals(
                        ExpressionType.BOOLEAN,
                        contains.expressionType()
                ),
                () -> assertEquals(
                        ExpressionType.STRING,
                        toLower.expressionType()
                ),
                () -> assertEquals(
                        ExpressionType.STRING,
                        trim.expressionType()
                ),
                () -> assertEquals(
                        ExpressionType.STRING,
                        title.expressionType()
                ),
                () -> assertEquals(
                        "title",
                        title.resolvedPath()
                                .orElseThrow()
                                .mappedPath()
                )
        );
    }

    @Test
    void shouldPromoteNumericTypesThroughCompletePipeline() {
        BinaryFilterExpression comparison =
                assertInstanceOf(
                        BinaryFilterExpression.class,
                        parseAndResolve(
                                "Priority add 2.50 eq Amount"
                        )
                );

        BinaryFilterExpression addition =
                assertInstanceOf(
                        BinaryFilterExpression.class,
                        comparison.left()
                );

        assertAll(
                () -> assertEquals(
                        BinaryOperator.EQ,
                        comparison.operator()
                ),
                () -> assertEquals(
                        ExpressionType.BOOLEAN,
                        comparison.expressionType()
                ),
                () -> assertEquals(
                        BinaryOperator.ADD,
                        addition.operator()
                ),
                () -> assertEquals(
                        ExpressionType.DECIMAL,
                        addition.expressionType()
                )
        );
    }

    @Test
    void shouldResolveEveryInListElement() {
        BinaryFilterExpression inExpression =
                assertInstanceOf(
                        BinaryFilterExpression.class,
                        parseAndResolve(
                                "Amount in (1, 2.50, null)"
                        )
                );

        ListFilterExpression list =
                assertInstanceOf(
                        ListFilterExpression.class,
                        inExpression.right()
                );

        assertAll(
                () -> assertEquals(
                        BinaryOperator.IN,
                        inExpression.operator()
                ),
                () -> assertEquals(
                        ExpressionType.BOOLEAN,
                        inExpression.expressionType()
                ),
                () -> assertEquals(
                        ExpressionType.COLLECTION,
                        list.expressionType()
                ),
                () -> assertEquals(
                        List.of(
                                ExpressionType.INTEGER,
                                ExpressionType.DECIMAL,
                                ExpressionType.NULL
                        ),
                        list.elements()
                                .stream()
                                .map(
                                        FilterExpression::expressionType
                                )
                                .toList()
                )
        );
    }

    @Test
    void shouldDecodeEscapedApostropheAcrossLexerAndParser() {
        BinaryFilterExpression comparison =
                assertInstanceOf(
                        BinaryFilterExpression.class,
                        parseAndResolve(
                                "Title eq 'O''Brien'"
                        )
                );

        LiteralFilterExpression literal =
                assertInstanceOf(
                        LiteralFilterExpression.class,
                        comparison.right()
                );

        assertAll(
                () -> assertEquals(
                        "O'Brien",
                        literal.value()
                ),
                () -> assertEquals(
                        "'O''Brien'",
                        literal.rawText()
                ),
                () -> assertEquals(
                        ExpressionType.STRING,
                        literal.expressionType()
                )
        );
    }

    @Test
    void shouldLeaveParsedTreeUnchangedAfterResolution() {
        FilterExpression parsed =
                new FilterParser(
                        "Title eq 'urgent' "
                                + "and Owner/Active eq true"
                ).parse();

        List<PropertyFilterExpression> parsedProperties =
                collectProperties(parsed);

        FilterExpression resolved =
                resolver.resolve(parsed);

        List<PropertyFilterExpression> resolvedProperties =
                collectProperties(resolved);

        assertAll(
                () -> assertNotSame(
                        parsed,
                        resolved
                ),
                () -> assertTrue(
                        parsedProperties
                                .stream()
                                .noneMatch(
                                        PropertyFilterExpression::isResolved
                                )
                ),
                () -> assertTrue(
                        resolvedProperties
                                .stream()
                                .allMatch(
                                        PropertyFilterExpression::isResolved
                                )
                ),
                () -> assertTrue(
                        parsedProperties
                                .stream()
                                .allMatch(
                                        property ->
                                                property.expressionType()
                                                        == ExpressionType.UNKNOWN
                                )
                ),
                () -> assertFalse(
                        resolvedProperties.isEmpty()
                )
        );

        for (int index = 0;
             index < parsedProperties.size();
             index++) {

            assertNotSame(
                    parsedProperties.get(index),
                    resolvedProperties.get(index)
            );
        }
    }

    @Test
    void shouldReportLexicalFailureAtLexerLayer() {
        assertThrows(
                FilterLexerException.class,
                () -> new FilterLexer(
                        "Title = 'urgent'"
                ).tokenize()
        );
    }

    @Test
    void shouldReportGrammarFailureAtParserLayer() {
        assertThrows(
                FilterParserException.class,
                () -> new FilterParser(
                        "Title eq"
                ).parse()
        );
    }

    @Test
    void shouldReportUnknownPropertyAtSemanticLayer() {
        assertThrows(
                FilterSemanticException.class,
                () -> parseAndResolve(
                        "UnknownProperty eq 'urgent'"
                )
        );
    }

    @Test
    void shouldReportTypeMismatchAtSemanticLayer() {
        FilterSemanticException exception =
                assertThrows(
                        FilterSemanticException.class,
                        () -> parseAndResolve(
                                "Title gt 10"
                        )
                );

        assertTrue(
                exception.getMessage().contains(
                        "cannot order-compare STRING with INTEGER"
                )
        );
    }

    private FilterExpression parseAndResolve(
            String input
    ) {
        FilterExpression parsed =
                new FilterParser(input).parse();

        return resolver.resolve(parsed);
    }

    private static List<PropertyFilterExpression>
    collectProperties(
            FilterExpression root
    ) {
        List<PropertyFilterExpression> properties =
                new ArrayList<>();

        collectProperties(
                root,
                properties
        );

        return List.copyOf(properties);
    }

    private static void collectProperties(
            FilterExpression expression,
            List<PropertyFilterExpression> properties
    ) {
        if (expression
                instanceof PropertyFilterExpression property) {

            properties.add(property);
            return;
        }

        for (FilterExpression child
                : expression.children()) {

            collectProperties(
                    child,
                    properties
            );
        }
    }

    private static void assertFullyResolved(
            FilterExpression expression
    ) {
        assertTrue(
                expression.expressionType()
                        != ExpressionType.UNKNOWN
        );

        if (expression
                instanceof PropertyFilterExpression property) {

            assertTrue(
                    property.isResolved()
            );

            assertTrue(
                    property.resolvedPath().isPresent()
            );
        }

        for (FilterExpression child
                : expression.children()) {

            assertFullyResolved(child);
        }
    }
}