package io.github.davidtodorov.odataparser.orderby.semantic;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderByResolverTest {

    private CaseMetadata caseMetadata;
    private UserMetadata userMetadata;
    private DepartmentMetadata departmentMetadata;
    private OrderByResolver resolver;

    @BeforeEach
    void setUp() {
        departmentMetadata = new DepartmentMetadata();
        userMetadata = new UserMetadata();
        caseMetadata = new CaseMetadata();

        MetadataRegistry.of(
                caseMetadata,
                userMetadata,
                departmentMetadata
        );

        resolver = new OrderByResolver(caseMetadata);
    }

    @Test
    void shouldRejectNullRootMetadata() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new OrderByResolver(null)
                );

        assertEquals(
                "Root entity metadata cannot be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullOrderByOption() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> resolver.resolve(null)
                );

        assertEquals(
                "Order-by option cannot be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldResolveDirectPrimitiveProperty() {
        OrderByOption resolved = resolve("Title desc");
        OrderByItem item = resolved.get(0);
        ResolvedMetadataPath path =
                item.resolvedPath().orElseThrow();

        assertAll(
                () -> assertTrue(resolved.isResolved()),
                () -> assertTrue(item.isResolved()),
                () -> assertEquals(
                        OrderByDirection.DESCENDING,
                        item.direction()
                ),
                () -> assertEquals(
                        "Title",
                        item.externalPath()
                ),
                () -> assertEquals(
                        "title",
                        item.mappedPath().orElseThrow()
                ),
                () -> assertEquals(
                        ExpressionType.STRING,
                        item.expressionType().orElseThrow()
                ),
                () -> assertEquals(
                        String.class,
                        item.javaType().orElseThrow()
                ),
                () -> assertEquals(
                        "Title",
                        path.externalPath()
                ),
                () -> assertEquals(
                        "title",
                        path.mappedPath()
                ),
                () -> assertFalse(
                        path.containsNavigation()
                ),
                () -> assertSame(
                        caseMetadata,
                        path.rootMetadata()
                ),
                () -> assertSame(
                        caseMetadata.requireProperty("Title"),
                        path.leaf().propertyMetadata()
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("directPrimitiveCases")
    void shouldResolveEveryDirectPrimitiveProperty(
            String description,
            String externalPath,
            String mappedPath,
            ExpressionType expressionType,
            Class<?> javaType
    ) {
        OrderByItem item =
                resolve(externalPath).get(0);

        assertAll(
                () -> assertEquals(
                        mappedPath,
                        item.mappedPath().orElseThrow()
                ),
                () -> assertEquals(
                        expressionType,
                        item.expressionType().orElseThrow()
                ),
                () -> assertEquals(
                        javaType,
                        item.javaType().orElseThrow()
                ),
                () -> assertSame(
                        caseMetadata.requireProperty(externalPath),
                        item.resolvedPath()
                                .orElseThrow()
                                .leaf()
                                .propertyMetadata()
                )
        );
    }

    @Test
    void shouldResolveSingleNavigationPath() {
        OrderByItem item =
                resolve("Owner/Username desc").get(0);

        ResolvedMetadataPath path =
                item.resolvedPath().orElseThrow();

        ResolvedMetadataPathSegment owner =
                path.segments().get(0);

        ResolvedMetadataPathSegment username =
                path.segments().get(1);

        assertAll(
                () -> assertEquals(
                        "Owner/Username",
                        item.externalPath()
                ),
                () -> assertEquals(
                        "owner/username",
                        item.mappedPath().orElseThrow()
                ),
                () -> assertEquals(
                        ExpressionType.STRING,
                        item.expressionType().orElseThrow()
                ),
                () -> assertEquals(
                        String.class,
                        item.javaType().orElseThrow()
                ),
                () -> assertEquals(2, path.size()),
                () -> assertTrue(path.containsNavigation())
        );

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
                () -> assertSame(
                        userMetadata,
                        username.declaringMetadata()
                ),
                () -> assertSame(
                        userMetadata.requireProperty("Username"),
                        username.propertyMetadata()
                )
        );
    }

    @Test
    void shouldResolveDeepNavigationPath() {
        OrderByItem item =
                resolve(
                        "Owner/Department/Code asc"
                ).get(0);

        ResolvedMetadataPath path =
                item.resolvedPath().orElseThrow();

        ResolvedMetadataPathSegment owner =
                path.segments().get(0);

        ResolvedMetadataPathSegment department =
                path.segments().get(1);

        ResolvedMetadataPathSegment code =
                path.segments().get(2);

        assertAll(
                () -> assertEquals(
                        "Owner/Department/Code",
                        path.externalPath()
                ),
                () -> assertEquals(
                        "owner/department/code",
                        path.mappedPath()
                ),
                () -> assertEquals(
                        List.of(
                                "Owner",
                                "Department",
                                "Code"
                        ),
                        path.externalSegments()
                ),
                () -> assertEquals(
                        List.of(
                                "owner",
                                "department",
                                "code"
                        ),
                        path.mappedSegments()
                ),
                () -> assertEquals(
                        ExpressionType.STRING,
                        path.expressionType()
                ),
                () -> assertEquals(
                        String.class,
                        path.javaType()
                )
        );

        assertAll(
                () -> assertSame(
                        userMetadata,
                        owner.targetMetadata().orElseThrow()
                ),
                () -> assertSame(
                        userMetadata,
                        department.declaringMetadata()
                ),
                () -> assertSame(
                        departmentMetadata,
                        department.targetMetadata().orElseThrow()
                ),
                () -> assertEquals(
                        NavigationJoinType.LEFT,
                        department.defaultJoinType().orElseThrow()
                ),
                () -> assertTrue(
                        department.isJoinTypeOverridable()
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
    void shouldResolveDirectDepartmentNavigationPath() {
        OrderByItem item =
                resolve("Department/Budget desc").get(0);

        ResolvedMetadataPath path =
                item.resolvedPath().orElseThrow();

        assertAll(
                () -> assertEquals(
                        "department/budget",
                        item.mappedPath().orElseThrow()
                ),
                () -> assertEquals(
                        ExpressionType.DECIMAL,
                        item.expressionType().orElseThrow()
                ),
                () -> assertEquals(
                        BigDecimal.class,
                        item.javaType().orElseThrow()
                ),
                () -> assertSame(
                        departmentMetadata,
                        path.rootSegment()
                                .targetMetadata()
                                .orElseThrow()
                ),
                () -> assertSame(
                        departmentMetadata.requireProperty("Budget"),
                        path.leaf().propertyMetadata()
                )
        );
    }

    @Test
    void shouldResolveMultipleItemsWithoutChangingOrderOrDirection() {
        String input =
                "Title, "
                        + "Owner/Username desc, "
                        + "Owner/Department/Code asc, "
                        + "Id desc";

        OrderByOption resolved = resolve(input);

        assertAll(
                () -> assertEquals(4, resolved.size()),
                () -> assertTrue(resolved.isResolved()),
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
                                "Id"
                        ),
                        resolved.items()
                                .stream()
                                .map(OrderByItem::externalPath)
                                .toList()
                ),
                () -> assertEquals(
                        List.of(
                                OrderByDirection.ASCENDING,
                                OrderByDirection.DESCENDING,
                                OrderByDirection.ASCENDING,
                                OrderByDirection.DESCENDING
                        ),
                        resolved.items()
                                .stream()
                                .map(OrderByItem::direction)
                                .toList()
                ),
                () -> assertEquals(
                        List.of(
                                "title",
                                "owner/username",
                                "owner/department/code",
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
                )
        );
    }

    @Test
    void shouldPreserveOptionAndItemSourceSpans() {
        OrderByOption parsed =
                parse(
                        "  Title desc, Owner/Username asc  "
                );

        OrderByOption resolved =
                resolver.resolve(parsed);

        assertAll(
                () -> assertEquals(
                        parsed.sourceSpan(),
                        resolved.sourceSpan()
                ),
                () -> assertEquals(
                        parsed.get(0).sourceSpan(),
                        resolved.get(0).sourceSpan()
                ),
                () -> assertEquals(
                        parsed.get(1).sourceSpan(),
                        resolved.get(1).sourceSpan()
                )
        );
    }

    @Test
    void shouldReturnNewResolvedObjectsWithoutMutatingParsedOption() {
        OrderByOption parsed =
                parse(
                        "Title, Owner/Username desc"
                );

        OrderByOption resolved =
                resolver.resolve(parsed);

        assertAll(
                () -> assertNotSame(parsed, resolved),
                () -> assertNotSame(
                        parsed.get(0),
                        resolved.get(0)
                ),
                () -> assertNotSame(
                        parsed.get(1),
                        resolved.get(1)
                ),
                () -> assertFalse(parsed.isResolved()),
                () -> assertFalse(
                        parsed.get(0).isResolved()
                ),
                () -> assertFalse(
                        parsed.get(1).isResolved()
                ),
                () -> assertTrue(resolved.isResolved()),
                () -> assertTrue(
                        resolved.get(0).isResolved()
                ),
                () -> assertTrue(
                        resolved.get(1).isResolved()
                )
        );
    }

    @Test
    void shouldReuseExactMetadataDescriptorsAcrossDifferentNavigationRoots() {
        OrderByOption resolved =
                resolve(
                        "Owner/Email, Reviewer/Email desc"
                );

        ResolvedMetadataPath ownerEmail =
                resolved.get(0)
                        .resolvedPath()
                        .orElseThrow();

        ResolvedMetadataPath reviewerEmail =
                resolved.get(1)
                        .resolvedPath()
                        .orElseThrow();

        assertAll(
                () -> assertSame(
                        userMetadata,
                        ownerEmail.rootSegment()
                                .targetMetadata()
                                .orElseThrow()
                ),
                () -> assertSame(
                        userMetadata,
                        reviewerEmail.rootSegment()
                                .targetMetadata()
                                .orElseThrow()
                ),
                () -> assertSame(
                        ownerEmail.leaf().propertyMetadata(),
                        reviewerEmail.leaf().propertyMetadata()
                )
        );
    }

    @Test
    void shouldAllowResolvingAnAlreadyResolvedOptionAgain() {
        OrderByOption first =
                resolve(
                        "Title, Owner/Username desc"
                );

        OrderByOption second =
                resolver.resolve(first);

        assertAll(
                () -> assertTrue(first.isResolved()),
                () -> assertTrue(second.isResolved()),
                () -> assertNotSame(first, second),
                () -> assertNotSame(
                        first.get(0),
                        second.get(0)
                ),
                () -> assertEquals(
                        first.get(0).externalPath(),
                        second.get(0).externalPath()
                ),
                () -> assertEquals(
                        first.get(0).direction(),
                        second.get(0).direction()
                ),
                () -> assertEquals(
                        first.get(0).mappedPath(),
                        second.get(0).mappedPath()
                )
        );
    }

    @Test
    void shouldRejectUnknownRootProperty() {
        OrderBySemanticException exception =
                assertThrows(
                        OrderBySemanticException.class,
                        () -> resolve(
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
                        exception.sourceSpan().start()
                ),
                () -> assertEquals(
                        20,
                        exception.sourceSpan().end()
                ),
                () -> assertEquals(
                        0,
                        exception.position()
                ),
                () -> assertInstanceOf(
                        IllegalArgumentException.class,
                        exception.getCause()
                )
        );
    }

    @Test
    void shouldRejectUnknownNestedProperty() {
        OrderBySemanticException exception =
                assertThrows(
                        OrderBySemanticException.class,
                        () -> resolve(
                                "Owner/Unknown desc"
                        )
                );

        assertAll(
                () -> assertTrue(
                        exception.getMessage()
                                .contains("Unknown")
                ),
                () -> assertTrue(
                        exception.getMessage()
                                .contains("User")
                ),
                () -> assertEquals(
                        0,
                        exception.sourceSpan().start()
                ),
                () -> assertEquals(
                        18,
                        exception.sourceSpan().end()
                ),
                () -> assertInstanceOf(
                        IllegalArgumentException.class,
                        exception.getCause()
                )
        );
    }

    @Test
    void shouldRejectTraversalThroughPrimitiveProperty() {
        OrderBySemanticException exception =
                assertThrows(
                        OrderBySemanticException.class,
                        () -> resolve(
                                "Title/Value desc"
                        )
                );

        assertAll(
                () -> assertTrue(
                        exception.getMessage()
                                .contains("Title")
                ),
                () -> assertEquals(
                        0,
                        exception.sourceSpan().start()
                ),
                () -> assertEquals(
                        16,
                        exception.sourceSpan().end()
                ),
                () -> assertInstanceOf(
                        IllegalArgumentException.class,
                        exception.getCause()
                )
        );
    }

    @Test
    void shouldRejectNavigationPropertyAsFinalSegment() {
        OrderBySemanticException exception =
                assertThrows(
                        OrderBySemanticException.class,
                        () -> resolve(
                                "Owner asc"
                        )
                );

        assertAll(
                () -> assertTrue(
                        exception.getMessage()
                                .contains("Owner")
                ),
                () -> assertEquals(
                        0,
                        exception.sourceSpan().start()
                ),
                () -> assertEquals(
                        9,
                        exception.sourceSpan().end()
                ),
                () -> assertInstanceOf(
                        IllegalArgumentException.class,
                        exception.getCause()
                )
        );
    }

    @Test
    void shouldAttachFailureToSpecificInvalidItem() {
        OrderBySemanticException exception =
                assertThrows(
                        OrderBySemanticException.class,
                        () -> resolve(
                                "Title asc, UnknownProperty desc, Id"
                        )
                );

        assertAll(
                () -> assertEquals(
                        11,
                        exception.sourceSpan().start()
                ),
                () -> assertEquals(
                        31,
                        exception.sourceSpan().end()
                ),
                () -> assertEquals(
                        11,
                        exception.position()
                ),
                () -> assertTrue(
                        exception.getMessage()
                                .contains(
                                        "at source range [11, 31)"
                                )
                )
        );
    }

    private OrderByOption resolve(
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

    private static Stream<Arguments> directPrimitiveCases() {
        return Stream.of(
                Arguments.of(
                        "integer identifier",
                        "Id",
                        "id",
                        ExpressionType.INTEGER,
                        Integer.class
                ),
                Arguments.of(
                        "string reference",
                        "Reference",
                        "reference",
                        ExpressionType.STRING,
                        String.class
                ),
                Arguments.of(
                        "string title",
                        "Title",
                        "title",
                        ExpressionType.STRING,
                        String.class
                ),
                Arguments.of(
                        "decimal amount",
                        "Amount",
                        "amount",
                        ExpressionType.DECIMAL,
                        BigDecimal.class
                ),
                Arguments.of(
                        "integer priority",
                        "Priority",
                        "priority",
                        ExpressionType.INTEGER,
                        Integer.class
                ),
                Arguments.of(
                        "boolean deleted",
                        "Deleted",
                        "deleted",
                        ExpressionType.BOOLEAN,
                        Boolean.class
                ),
                Arguments.of(
                        "boolean closed",
                        "Closed",
                        "closed",
                        ExpressionType.BOOLEAN,
                        Boolean.class
                )
        );
    }
}