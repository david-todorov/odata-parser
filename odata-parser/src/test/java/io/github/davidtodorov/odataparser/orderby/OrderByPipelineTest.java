package io.github.davidtodorov.odataparser.orderby;

import io.github.davidtodorov.odataparser.common.type.ExpressionType;
import io.github.davidtodorov.odataparser.meta.MetadataRegistry;
import io.github.davidtodorov.odataparser.meta.NavigationJoinType;
import io.github.davidtodorov.odataparser.meta.demo.CaseMetadata;
import io.github.davidtodorov.odataparser.meta.demo.DepartmentMetadata;
import io.github.davidtodorov.odataparser.meta.demo.UserMetadata;
import io.github.davidtodorov.odataparser.meta.path.ResolvedMetadataPath;
import io.github.davidtodorov.odataparser.meta.path.ResolvedMetadataPathSegment;
import io.github.davidtodorov.odataparser.orderby.ast.OrderByDirection;
import io.github.davidtodorov.odataparser.orderby.ast.OrderByItem;
import io.github.davidtodorov.odataparser.orderby.ast.OrderByOption;
import io.github.davidtodorov.odataparser.orderby.parser.OrderByParser;
import io.github.davidtodorov.odataparser.orderby.parser.OrderByParserException;
import io.github.davidtodorov.odataparser.orderby.semantic.OrderByResolver;
import io.github.davidtodorov.odataparser.orderby.semantic.OrderBySemanticException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderByPipelineTest {

    private CaseMetadata caseMetadata;
    private UserMetadata userMetadata;
    private DepartmentMetadata departmentMetadata;
    private OrderByResolver resolver;

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
                new OrderByResolver(
                        caseMetadata
                );
    }

    @Test
    void shouldProcessCompleteRealisticOrderByPipeline() {
        String input =
                "Title, "
                        + "Owner/Username desc, "
                        + "Owner/Department/Code asc, "
                        + "Department/Budget desc, "
                        + "Id desc";

        OrderByOption resolved =
                parseAndResolve(input);

        assertAll(
                () -> assertEquals(
                        5,
                        resolved.size()
                ),
                () -> assertTrue(
                        resolved.isResolved()
                ),
                () -> assertEquals(
                        0,
                        resolved.sourceSpan().start()
                ),
                () -> assertEquals(
                        input.length(),
                        resolved.sourceSpan().end()
                ),
                () -> assertEquals(
                        List.of(
                                "Title",
                                "Owner/Username",
                                "Owner/Department/Code",
                                "Department/Budget",
                                "Id"
                        ),
                        resolved.items()
                                .stream()
                                .map(OrderByItem::externalPath)
                                .toList()
                ),
                () -> assertEquals(
                        List.of(
                                "title",
                                "owner/username",
                                "owner/department/code",
                                "department/budget",
                                "id"
                        ),
                        resolved.items()
                                .stream()
                                .map(
                                        item ->
                                                item.mappedPath()
                                                        .orElseThrow()
                                )
                                .toList()
                ),
                () -> assertEquals(
                        List.of(
                                OrderByDirection.ASCENDING,
                                OrderByDirection.DESCENDING,
                                OrderByDirection.ASCENDING,
                                OrderByDirection.DESCENDING,
                                OrderByDirection.DESCENDING
                        ),
                        resolved.items()
                                .stream()
                                .map(OrderByItem::direction)
                                .toList()
                )
        );
    }

    @Test
    void shouldResolveExpectedTypesAndJavaTypes() {
        OrderByOption resolved =
                parseAndResolve(
                        "Title, "
                                + "Priority desc, "
                                + "Amount asc, "
                                + "Deleted, "
                                + "Owner/Active desc"
                );

        assertAll(
                () -> assertEquals(
                        List.of(
                                ExpressionType.STRING,
                                ExpressionType.INTEGER,
                                ExpressionType.DECIMAL,
                                ExpressionType.BOOLEAN,
                                ExpressionType.BOOLEAN
                        ),
                        resolved.items()
                                .stream()
                                .map(
                                        item ->
                                                item.expressionType()
                                                        .orElseThrow()
                                )
                                .toList()
                ),
                () -> assertEquals(
                        List.of(
                                String.class,
                                Integer.class,
                                java.math.BigDecimal.class,
                                Boolean.class,
                                Boolean.class
                        ),
                        resolved.items()
                                .stream()
                                .map(
                                        item ->
                                                item.javaType()
                                                        .orElseThrow()
                                )
                                .toList()
                )
        );
    }

    @Test
    void shouldPreserveMetadataIdentityAcrossDeepNavigationPath() {
        OrderByItem item =
                parseAndResolve(
                        "Owner/Department/Code desc"
                ).get(0);

        ResolvedMetadataPath path =
                item.resolvedPath()
                        .orElseThrow();

        ResolvedMetadataPathSegment owner =
                path.segments().get(0);

        ResolvedMetadataPathSegment department =
                path.segments().get(1);

        ResolvedMetadataPathSegment code =
                path.segments().get(2);

        assertAll(
                () -> assertSame(
                        caseMetadata,
                        owner.declaringMetadata()
                ),
                () -> assertSame(
                        caseMetadata.requireProperty("Owner"),
                        owner.propertyMetadata()
                ),
                () -> assertSame(
                        userMetadata,
                        owner.targetMetadata().orElseThrow()
                ),
                () -> assertSame(
                        userMetadata,
                        department.declaringMetadata()
                ),
                () -> assertSame(
                        userMetadata.requireProperty("Department"),
                        department.propertyMetadata()
                ),
                () -> assertSame(
                        departmentMetadata,
                        department.targetMetadata().orElseThrow()
                ),
                () -> assertSame(
                        departmentMetadata,
                        code.declaringMetadata()
                ),
                () -> assertSame(
                        departmentMetadata.requireProperty("Code"),
                        code.propertyMetadata()
                )
        );
    }

    @Test
    void shouldPreserveNavigationJoinPolicies() {
        OrderByItem item =
                parseAndResolve(
                        "Owner/Department/Enabled asc"
                ).get(0);

        ResolvedMetadataPath path =
                item.resolvedPath()
                        .orElseThrow();

        ResolvedMetadataPathSegment owner =
                path.segments().get(0);

        ResolvedMetadataPathSegment department =
                path.segments().get(1);

        assertAll(
                () -> assertEquals(
                        NavigationJoinType.LEFT,
                        owner.defaultJoinType().orElseThrow()
                ),
                () -> assertTrue(
                        owner.isJoinTypeOverridable()
                ),
                () -> assertFalse(
                        owner.isJoinTypeFixed()
                ),
                () -> assertEquals(
                        NavigationJoinType.LEFT,
                        department.defaultJoinType().orElseThrow()
                ),
                () -> assertTrue(
                        department.isJoinTypeOverridable()
                ),
                () -> assertFalse(
                        department.isJoinTypeFixed()
                )
        );
    }

    @Test
    void shouldPreserveDefaultAndExplicitDirectionsIndependently() {
        OrderByOption resolved =
                parseAndResolve(
                        "Title, Amount desc, Priority asc, Id"
                );

        assertEquals(
                List.of(
                        OrderByDirection.ASCENDING,
                        OrderByDirection.DESCENDING,
                        OrderByDirection.ASCENDING,
                        OrderByDirection.ASCENDING
                ),
                resolved.items()
                        .stream()
                        .map(OrderByItem::direction)
                        .toList()
        );
    }

    @Test
    void shouldLeaveParsedOptionUnchangedAfterResolution() {
        OrderByOption parsed =
                new OrderByParser(
                        "Title, Owner/Username desc"
                ).parse();

        OrderByItem parsedTitle =
                parsed.get(0);

        OrderByItem parsedOwner =
                parsed.get(1);

        OrderByOption resolved =
                resolver.resolve(parsed);

        assertAll(
                () -> assertNotSame(
                        parsed,
                        resolved
                ),
                () -> assertNotSame(
                        parsedTitle,
                        resolved.get(0)
                ),
                () -> assertNotSame(
                        parsedOwner,
                        resolved.get(1)
                ),
                () -> assertFalse(
                        parsed.isResolved()
                ),
                () -> assertFalse(
                        parsedTitle.isResolved()
                ),
                () -> assertFalse(
                        parsedOwner.isResolved()
                ),
                () -> assertTrue(
                        resolved.isResolved()
                ),
                () -> assertTrue(
                        resolved.get(0).isResolved()
                ),
                () -> assertTrue(
                        resolved.get(1).isResolved()
                ),
                () -> assertEquals(
                        parsed.sourceSpan(),
                        resolved.sourceSpan()
                ),
                () -> assertEquals(
                        parsedTitle.sourceSpan(),
                        resolved.get(0).sourceSpan()
                ),
                () -> assertEquals(
                        parsedOwner.sourceSpan(),
                        resolved.get(1).sourceSpan()
                )
        );
    }

    @Test
    void shouldReportMalformedSyntaxAtParserLayer() {
        assertThrows(
                OrderByParserException.class,
                () -> new OrderByParser(
                        "Title desc extra"
                ).parse()
        );
    }

    @Test
    void shouldReportUnknownPropertyAtSemanticLayer() {
        OrderBySemanticException exception =
                assertThrows(
                        OrderBySemanticException.class,
                        () -> parseAndResolve(
                                "UnknownProperty desc"
                        )
                );

        assertAll(
                () -> assertTrue(
                        exception.getMessage()
                                .contains("UnknownProperty")
                ),
                () -> assertEquals(
                        0,
                        exception.position()
                ),
                () -> assertEquals(
                        0,
                        exception.sourceSpan().start()
                ),
                () -> assertEquals(
                        "UnknownProperty desc".length(),
                        exception.sourceSpan().end()
                ),
                () -> assertInstanceOf(
                        IllegalArgumentException.class,
                        exception.getCause()
                )
        );
    }

    @Test
    void shouldAttachSemanticFailureToSpecificOrderByItem() {
        String input =
                "Title asc, UnknownProperty desc, Id";

        int invalidItemStart =
                input.indexOf("UnknownProperty");

        int invalidItemEnd =
                input.indexOf(", Id");

        OrderBySemanticException exception =
                assertThrows(
                        OrderBySemanticException.class,
                        () -> parseAndResolve(input)
                );

        assertAll(
                () -> assertEquals(
                        invalidItemStart,
                        exception.position()
                ),
                () -> assertEquals(
                        invalidItemStart,
                        exception.sourceSpan().start()
                ),
                () -> assertEquals(
                        invalidItemEnd,
                        exception.sourceSpan().end()
                ),
                () -> assertTrue(
                        exception.getMessage()
                                .contains(
                                        "at source range ["
                                                + invalidItemStart
                                                + ", "
                                                + invalidItemEnd
                                                + ")"
                                )
                )
        );
    }

    @Test
    void shouldReportPrimitiveTraversalAtSemanticLayer() {
        OrderBySemanticException exception =
                assertThrows(
                        OrderBySemanticException.class,
                        () -> parseAndResolve(
                                "Title/Value desc"
                        )
                );

        assertAll(
                () -> assertTrue(
                        exception.getMessage()
                                .contains("Title")
                ),
                () -> assertInstanceOf(
                        IllegalArgumentException.class,
                        exception.getCause()
                )
        );
    }

    private OrderByOption parseAndResolve(
            String input
    ) {
        OrderByOption parsed =
                new OrderByParser(input).parse();

        return resolver.resolve(parsed);
    }
}