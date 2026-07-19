package io.github.davidtodorov.odataparser.orderby.ast;

import io.github.davidtodorov.odataparser.common.metadata.SourceSpan;
import io.github.davidtodorov.odataparser.common.type.ExpressionType;
import io.github.davidtodorov.odataparser.meta.MetadataRegistry;
import io.github.davidtodorov.odataparser.meta.demo.CaseMetadata;
import io.github.davidtodorov.odataparser.meta.demo.DepartmentMetadata;
import io.github.davidtodorov.odataparser.meta.demo.UserMetadata;
import io.github.davidtodorov.odataparser.meta.path.ResolvedMetadataPath;
import io.github.davidtodorov.odataparser.meta.resolver.MetadataPropertyPathResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderByAstInvariantTest {

    private MetadataPropertyPathResolver propertyPathResolver;

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

        propertyPathResolver =
                new MetadataPropertyPathResolver(
                        caseMetadata
                );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("directionCases")
    void shouldExposeDirectionProperties(
            String description,
            OrderByDirection direction,
            String expectedKeyword,
            boolean expectedAscending,
            boolean expectedDescending
    ) {
        assertAll(
                () -> assertEquals(
                        expectedKeyword,
                        direction.keyword()
                ),
                () -> assertEquals(
                        expectedAscending,
                        direction.isAscending()
                ),
                () -> assertEquals(
                        expectedDescending,
                        direction.isDescending()
                ),
                () -> assertSame(
                        direction,
                        OrderByDirection.fromKeyword(
                                expectedKeyword
                        )
                )
        );
    }

    @Test
    void shouldRejectNullDirectionKeyword() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> OrderByDirection.fromKeyword(null)
                );

        assertEquals(
                "Order-by direction keyword cannot be null",
                exception.getMessage()
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "ASC",
            "Desc",
            "ascending",
            "descending",
            "up",
            "down"
    })
    void shouldRejectUnsupportedDirectionKeyword(
            String keyword
    ) {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> OrderByDirection.fromKeyword(
                                keyword
                        )
                );

        assertEquals(
                "Unsupported order-by direction: '"
                        + keyword
                        + "'. Expected 'asc' or 'desc'",
                exception.getMessage()
        );
    }

    @Test
    void shouldCreateUnresolvedItemWithUnknownSourceSpan() {
        OrderByItem item =
                new OrderByItem(
                        List.of("Title"),
                        OrderByDirection.ASCENDING
                );

        assertAll(
                () -> assertEquals(
                        List.of("Title"),
                        item.pathSegments()
                ),
                () -> assertEquals(
                        "Title",
                        item.externalPath()
                ),
                () -> assertEquals(
                        OrderByDirection.ASCENDING,
                        item.direction()
                ),
                () -> assertFalse(
                        item.isResolved()
                ),
                () -> assertTrue(
                        item.resolvedPath().isEmpty()
                ),
                () -> assertTrue(
                        item.mappedPath().isEmpty()
                ),
                () -> assertTrue(
                        item.expressionType().isEmpty()
                ),
                () -> assertTrue(
                        item.javaType().isEmpty()
                ),
                () -> assertTrue(
                        item.sourceSpan().isUnknown()
                )
        );
    }

    @Test
    void shouldPreserveExplicitItemSourceSpan() {
        SourceSpan sourceSpan =
                new SourceSpan(
                        2,
                        24
                );

        OrderByItem item =
                new OrderByItem(
                        List.of(
                                "Owner",
                                "Username"
                        ),
                        OrderByDirection.DESCENDING,
                        sourceSpan
                );

        assertAll(
                () -> assertSame(
                        sourceSpan,
                        item.sourceSpan()
                ),
                () -> assertEquals(
                        "Owner/Username",
                        item.externalPath()
                ),
                () -> assertEquals(
                        OrderByDirection.DESCENDING,
                        item.direction()
                )
        );
    }

    @Test
    void shouldDefensivelyCopyItemPathSegments() {
        List<String> source =
                new ArrayList<>(
                        List.of(
                                "Owner",
                                "Department",
                                "Code"
                        )
                );

        OrderByItem item =
                new OrderByItem(
                        source,
                        OrderByDirection.ASCENDING
                );

        source.set(
                0,
                "Reviewer"
        );

        assertAll(
                () -> assertEquals(
                        List.of(
                                "Owner",
                                "Department",
                                "Code"
                        ),
                        item.pathSegments()
                ),
                () -> assertEquals(
                        "Owner/Department/Code",
                        item.externalPath()
                ),
                () -> assertThrows(
                        UnsupportedOperationException.class,
                        () -> item.pathSegments().add(
                                "Other"
                        )
                )
        );
    }

    @Test
    void shouldRejectNullItemPathSegments() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new OrderByItem(
                                null,
                                OrderByDirection.ASCENDING,
                                Optional.empty(),
                                SourceSpan.unknown()
                        )
                );

        assertEquals(
                "Order-by path segments cannot be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectEmptyItemPath() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new OrderByItem(
                                List.of(),
                                OrderByDirection.ASCENDING
                        )
                );

        assertEquals(
                "An order-by item must contain at least one path segment",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectItemPathContainingNullSegment() {
        List<String> segments =
                new ArrayList<>();

        segments.add("Owner");
        segments.add(null);
        segments.add("Username");

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new OrderByItem(
                                segments,
                                OrderByDirection.ASCENDING
                        )
                );

        assertEquals(
                "Order-by path segment at index 1 cannot be null or blank",
                exception.getMessage()
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            " ",
            "\t",
            "\r\n"
    })
    void shouldRejectItemPathContainingBlankSegment(
            String blankSegment
    ) {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new OrderByItem(
                                List.of(
                                        "Owner",
                                        blankSegment,
                                        "Username"
                                ),
                                OrderByDirection.ASCENDING
                        )
                );

        assertEquals(
                "Order-by path segment at index 1 cannot be null or blank",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullItemDirection() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new OrderByItem(
                                List.of("Title"),
                                null,
                                Optional.empty(),
                                SourceSpan.unknown()
                        )
                );

        assertEquals(
                "Order-by direction cannot be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullResolvedPathOptional() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new OrderByItem(
                                List.of("Title"),
                                OrderByDirection.ASCENDING,
                                null,
                                SourceSpan.unknown()
                        )
                );

        assertEquals(
                "Resolved order-by metadata path cannot be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullItemSourceSpan() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new OrderByItem(
                                List.of("Title"),
                                OrderByDirection.ASCENDING,
                                Optional.empty(),
                                null
                        )
                );

        assertEquals(
                "Order-by source span cannot be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldResolveItemWithoutMutatingOriginal() {
        SourceSpan sourceSpan =
                new SourceSpan(
                        3,
                        13
                );

        OrderByItem unresolved =
                new OrderByItem(
                        List.of("Title"),
                        OrderByDirection.DESCENDING,
                        sourceSpan
                );

        ResolvedMetadataPath resolvedPath =
                propertyPathResolver.resolve(
                        List.of("Title")
                );

        OrderByItem resolved =
                unresolved.withResolvedPath(
                        resolvedPath
                );

        assertAll(
                () -> assertNotSame(
                        unresolved,
                        resolved
                ),
                () -> assertFalse(
                        unresolved.isResolved()
                ),
                () -> assertTrue(
                        resolved.isResolved()
                ),
                () -> assertSame(
                        resolvedPath,
                        resolved.resolvedPath().orElseThrow()
                ),
                () -> assertEquals(
                        "title",
                        resolved.mappedPath().orElseThrow()
                ),
                () -> assertEquals(
                        ExpressionType.STRING,
                        resolved.expressionType().orElseThrow()
                ),
                () -> assertEquals(
                        String.class,
                        resolved.javaType().orElseThrow()
                ),
                () -> assertEquals(
                        unresolved.pathSegments(),
                        resolved.pathSegments()
                ),
                () -> assertEquals(
                        unresolved.direction(),
                        resolved.direction()
                ),
                () -> assertSame(
                        sourceSpan,
                        resolved.sourceSpan()
                )
        );
    }

    @Test
    void shouldRejectNullPathPassedToWithResolvedPath() {
        OrderByItem item =
                new OrderByItem(
                        List.of("Title"),
                        OrderByDirection.ASCENDING
                );

        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> item.withResolvedPath(null)
                );

        assertEquals(
                "Resolved metadata path cannot be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectResolvedPathThatDoesNotMatchItemPath() {
        ResolvedMetadataPath titlePath =
                propertyPathResolver.resolve(
                        List.of("Title")
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new OrderByItem(
                                List.of("Reference"),
                                OrderByDirection.ASCENDING,
                                Optional.of(titlePath),
                                SourceSpan.unknown()
                        )
                );

        assertEquals(
                "Resolved metadata path does not match the order-by item. "
                        + "Order-by path: 'Reference', "
                        + "resolved path: 'Title'",
                exception.getMessage()
        );
    }

    @Test
    void shouldCreateOptionWithUnknownSourceSpan() {
        OrderByItem title =
                new OrderByItem(
                        List.of("Title"),
                        OrderByDirection.ASCENDING
                );

        OrderByItem amount =
                new OrderByItem(
                        List.of("Amount"),
                        OrderByDirection.DESCENDING
                );

        OrderByOption option =
                new OrderByOption(
                        List.of(
                                title,
                                amount
                        )
                );

        assertAll(
                () -> assertEquals(
                        2,
                        option.size()
                ),
                () -> assertSame(
                        title,
                        option.get(0)
                ),
                () -> assertSame(
                        amount,
                        option.get(1)
                ),
                () -> assertFalse(
                        option.isResolved()
                ),
                () -> assertTrue(
                        option.sourceSpan().isUnknown()
                )
        );
    }

    @Test
    void shouldPreserveExplicitOptionSourceSpan() {
        SourceSpan sourceSpan =
                new SourceSpan(
                        2,
                        27
                );

        OrderByOption option =
                new OrderByOption(
                        List.of(
                                new OrderByItem(
                                        List.of("Title"),
                                        OrderByDirection.ASCENDING
                                )
                        ),
                        sourceSpan
                );

        assertSame(
                sourceSpan,
                option.sourceSpan()
        );
    }

    @Test
    void shouldDefensivelyCopyOptionItems() {
        OrderByItem title =
                new OrderByItem(
                        List.of("Title"),
                        OrderByDirection.ASCENDING
                );

        List<OrderByItem> source =
                new ArrayList<>(
                        List.of(title)
                );

        OrderByOption option =
                new OrderByOption(source);

        source.clear();

        assertAll(
                () -> assertEquals(
                        1,
                        option.size()
                ),
                () -> assertSame(
                        title,
                        option.get(0)
                ),
                () -> assertThrows(
                        UnsupportedOperationException.class,
                        () -> option.items().clear()
                )
        );
    }

    @Test
    void shouldIterateOptionItemsInOriginalOrder() {
        OrderByItem title =
                new OrderByItem(
                        List.of("Title"),
                        OrderByDirection.ASCENDING
                );

        OrderByItem amount =
                new OrderByItem(
                        List.of("Amount"),
                        OrderByDirection.DESCENDING
                );

        OrderByOption option =
                new OrderByOption(
                        List.of(
                                title,
                                amount
                        )
                );

        Iterator<OrderByItem> iterator =
                option.iterator();

        assertAll(
                () -> assertTrue(
                        iterator.hasNext()
                ),
                () -> assertSame(
                        title,
                        iterator.next()
                ),
                () -> assertTrue(
                        iterator.hasNext()
                ),
                () -> assertSame(
                        amount,
                        iterator.next()
                ),
                () -> assertFalse(
                        iterator.hasNext()
                ),
                () -> assertThrows(
                        UnsupportedOperationException.class,
                        iterator::remove
                )
        );
    }

    @Test
    void shouldRejectNullOptionItems() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new OrderByOption(
                                null,
                                SourceSpan.unknown()
                        )
                );

        assertEquals(
                "Order-by items cannot be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectEmptyOption() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new OrderByOption(
                                List.of()
                        )
                );

        assertEquals(
                "An order-by option must contain at least one item",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectOptionContainingNullItem() {
        List<OrderByItem> items =
                new ArrayList<>();

        items.add(
                new OrderByItem(
                        List.of("Title"),
                        OrderByDirection.ASCENDING
                )
        );

        items.add(null);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new OrderByOption(items)
                );

        assertEquals(
                "Order-by items cannot contain null elements",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullOptionSourceSpan() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new OrderByOption(
                                List.of(
                                        new OrderByItem(
                                                List.of("Title"),
                                                OrderByDirection.ASCENDING
                                        )
                                ),
                                null
                        )
                );

        assertEquals(
                "Order-by source span cannot be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldCreateResolvedOptionWithoutMutatingOriginal() {
        SourceSpan sourceSpan =
                new SourceSpan(
                        0,
                        25
                );

        OrderByItem title =
                new OrderByItem(
                        List.of("Title"),
                        OrderByDirection.ASCENDING,
                        new SourceSpan(
                                0,
                                5
                        )
                );

        OrderByItem ownerUsername =
                new OrderByItem(
                        List.of(
                                "Owner",
                                "Username"
                        ),
                        OrderByDirection.DESCENDING,
                        new SourceSpan(
                                7,
                                25
                        )
                );

        OrderByOption unresolved =
                new OrderByOption(
                        List.of(
                                title,
                                ownerUsername
                        ),
                        sourceSpan
                );

        OrderByItem resolvedTitle =
                title.withResolvedPath(
                        propertyPathResolver.resolve(
                                title.pathSegments()
                        )
                );

        OrderByItem resolvedOwnerUsername =
                ownerUsername.withResolvedPath(
                        propertyPathResolver.resolve(
                                ownerUsername.pathSegments()
                        )
                );

        OrderByOption resolved =
                unresolved.withResolvedItems(
                        List.of(
                                resolvedTitle,
                                resolvedOwnerUsername
                        )
                );

        assertAll(
                () -> assertNotSame(
                        unresolved,
                        resolved
                ),
                () -> assertFalse(
                        unresolved.isResolved()
                ),
                () -> assertTrue(
                        resolved.isResolved()
                ),
                () -> assertSame(
                        sourceSpan,
                        resolved.sourceSpan()
                ),
                () -> assertSame(
                        resolvedTitle,
                        resolved.get(0)
                ),
                () -> assertSame(
                        resolvedOwnerUsername,
                        resolved.get(1)
                )
        );
    }

    @Test
    void shouldRejectNullResolvedItemsList() {
        OrderByOption option =
                unresolvedOption();

        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> option.withResolvedItems(null)
                );

        assertEquals(
                "Resolved order-by items cannot be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectDifferentResolvedItemCount() {
        OrderByOption option =
                unresolvedOption();

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> option.withResolvedItems(
                                List.of(
                                        resolvedItem(
                                                "Title",
                                                OrderByDirection.ASCENDING
                                        )
                                )
                        )
                );

        assertEquals(
                "Resolved order-by item count does not match "
                        + "the original item count",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullResolvedItem() {
        OrderByOption option =
                unresolvedOption();

        List<OrderByItem> resolvedItems =
                new ArrayList<>();

        resolvedItems.add(
                resolvedItem(
                        "Title",
                        OrderByDirection.ASCENDING
                )
        );

        resolvedItems.add(null);

        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> option.withResolvedItems(
                                resolvedItems
                        )
                );

        assertEquals(
                "Resolved order-by item at index 1 cannot be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectResolvedItemWithChangedPath() {
        OrderByOption option =
                unresolvedOption();

        OrderByItem changedPath =
                resolvedItem(
                        "Reference",
                        OrderByDirection.ASCENDING
                );

        OrderByItem amount =
                resolvedItem(
                        "Amount",
                        OrderByDirection.DESCENDING
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> option.withResolvedItems(
                                List.of(
                                        changedPath,
                                        amount
                                )
                        )
                );

        assertEquals(
                "Resolved item at index 0 changed the property path "
                        + "from 'Title' to 'Reference'",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectResolvedItemWithChangedDirection() {
        OrderByOption option =
                unresolvedOption();

        OrderByItem title =
                resolvedItem(
                        "Title",
                        OrderByDirection.DESCENDING
                );

        OrderByItem amount =
                resolvedItem(
                        "Amount",
                        OrderByDirection.DESCENDING
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> option.withResolvedItems(
                                List.of(
                                        title,
                                        amount
                                )
                        )
                );

        assertEquals(
                "Resolved item at index 0 changed the direction "
                        + "of property 'Title'",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectItemThatIsStillUnresolved() {
        OrderByOption option =
                unresolvedOption();

        OrderByItem unresolvedTitle =
                new OrderByItem(
                        List.of("Title"),
                        OrderByDirection.ASCENDING
                );

        OrderByItem resolvedAmount =
                resolvedItem(
                        "Amount",
                        OrderByDirection.DESCENDING
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> option.withResolvedItems(
                                List.of(
                                        unresolvedTitle,
                                        resolvedAmount
                                )
                        )
                );

        assertEquals(
                "Order-by item at index 0 is still unresolved",
                exception.getMessage()
        );
    }

    private OrderByOption unresolvedOption() {
        return new OrderByOption(
                List.of(
                        new OrderByItem(
                                List.of("Title"),
                                OrderByDirection.ASCENDING
                        ),
                        new OrderByItem(
                                List.of("Amount"),
                                OrderByDirection.DESCENDING
                        )
                )
        );
    }

    private OrderByItem resolvedItem(
            String externalPath,
            OrderByDirection direction
    ) {
        List<String> pathSegments =
                List.of(
                        externalPath.split("/")
                );

        return new OrderByItem(
                pathSegments,
                direction
        ).withResolvedPath(
                propertyPathResolver.resolve(
                        pathSegments
                )
        );
    }

    private static Stream<Arguments> directionCases() {
        return Stream.of(
                Arguments.of(
                        "ascending direction",
                        OrderByDirection.ASCENDING,
                        "asc",
                        true,
                        false
                ),
                Arguments.of(
                        "descending direction",
                        OrderByDirection.DESCENDING,
                        "desc",
                        false,
                        true
                )
        );
    }
}