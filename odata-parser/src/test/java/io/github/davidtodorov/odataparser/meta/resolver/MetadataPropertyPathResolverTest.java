package io.github.davidtodorov.odataparser.meta.resolver;

import io.github.davidtodorov.odataparser.common.type.ExpressionType;
import io.github.davidtodorov.odataparser.demo.CaseModel;
import io.github.davidtodorov.odataparser.demo.DepartmentModel;
import io.github.davidtodorov.odataparser.demo.UserModel;
import io.github.davidtodorov.odataparser.meta.EntityMetadata;
import io.github.davidtodorov.odataparser.meta.MetadataRegistry;
import io.github.davidtodorov.odataparser.meta.NavigationCardinality;
import io.github.davidtodorov.odataparser.meta.NavigationJoinType;
import io.github.davidtodorov.odataparser.meta.PropertyMetadataKind;
import io.github.davidtodorov.odataparser.meta.demo.CaseMetadata;
import io.github.davidtodorov.odataparser.meta.demo.DepartmentMetadata;
import io.github.davidtodorov.odataparser.meta.demo.UserMetadata;
import io.github.davidtodorov.odataparser.meta.path.ResolvedMetadataPath;
import io.github.davidtodorov.odataparser.meta.path.ResolvedMetadataPathSegment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetadataPropertyPathResolverTest {

    private CaseMetadata caseMetadata;
    private UserMetadata userMetadata;
    private DepartmentMetadata departmentMetadata;
    private MetadataPropertyPathResolver resolver;

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
                new MetadataPropertyPathResolver(
                        caseMetadata
                );
    }

    @Test
    void shouldResolveDirectPrimitiveProperty() {
        ResolvedMetadataPath path =
                resolver.resolve(
                        List.of("Title")
                );

        ResolvedMetadataPathSegment title =
                path.leaf();

        assertAll(
                () -> assertEquals(
                        "Title",
                        path.externalPath()
                ),
                () -> assertEquals(
                        "title",
                        path.mappedPath()
                ),
                () -> assertEquals(
                        List.of("Title"),
                        path.externalSegments()
                ),
                () -> assertEquals(
                        List.of("title"),
                        path.mappedSegments()
                ),
                () -> assertEquals(
                        1,
                        path.size()
                ),
                () -> assertFalse(
                        path.containsNavigation()
                ),
                () -> assertSame(
                        caseMetadata,
                        path.rootMetadata()
                ),
                () -> assertSame(
                        path.rootSegment(),
                        path.leaf()
                ),
                () -> assertEquals(
                        ExpressionType.STRING,
                        path.expressionType()
                ),
                () -> assertEquals(
                        String.class,
                        path.javaType()
                ),
                () -> assertSame(
                        caseMetadata,
                        title.declaringMetadata()
                ),
                () -> assertSame(
                        caseMetadata.requireProperty("Title"),
                        title.propertyMetadata()
                ),
                () -> assertEquals(
                        "Case",
                        title.declaringMetadataName()
                ),
                () -> assertEquals(
                        CaseModel.class,
                        title.declaringEntityType()
                ),
                () -> assertEquals(
                        PropertyMetadataKind.PRIMITIVE,
                        title.kind()
                ),
                () -> assertTrue(
                        title.isPrimitive()
                ),
                () -> assertFalse(
                        title.isNavigation()
                ),
                () -> assertEquals(
                        ExpressionType.STRING,
                        title.expressionType().orElseThrow()
                ),
                () -> assertTrue(
                        title.cardinality().isEmpty()
                ),
                () -> assertTrue(
                        title.targetMetadata().isEmpty()
                ),
                () -> assertTrue(
                        title.joinPolicy().isEmpty()
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("directPrimitiveCases")
    void shouldResolveEveryDirectPrimitiveType(
            String description,
            String externalName,
            String mappedName,
            ExpressionType expressionType,
            Class<?> javaType
    ) {
        ResolvedMetadataPath path =
                resolver.resolve(
                        List.of(externalName)
                );

        assertAll(
                () -> assertEquals(
                        externalName,
                        path.externalPath()
                ),
                () -> assertEquals(
                        mappedName,
                        path.mappedPath()
                ),
                () -> assertEquals(
                        expressionType,
                        path.expressionType()
                ),
                () -> assertEquals(
                        javaType,
                        path.javaType()
                ),
                () -> assertSame(
                        caseMetadata.requireProperty(externalName),
                        path.leaf().propertyMetadata()
                )
        );
    }

    @Test
    void shouldResolveSingleNavigationPath() {
        ResolvedMetadataPath path =
                resolver.resolve(
                        List.of(
                                "Owner",
                                "Username"
                        )
                );

        ResolvedMetadataPathSegment owner =
                path.segments().get(0);

        ResolvedMetadataPathSegment username =
                path.segments().get(1);

        assertAll(
                () -> assertEquals(
                        "Owner/Username",
                        path.externalPath()
                ),
                () -> assertEquals(
                        "owner/username",
                        path.mappedPath()
                ),
                () -> assertEquals(
                        2,
                        path.size()
                ),
                () -> assertTrue(
                        path.containsNavigation()
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
                        caseMetadata,
                        owner.declaringMetadata()
                ),
                () -> assertSame(
                        caseMetadata.requireProperty("Owner"),
                        owner.propertyMetadata()
                ),
                () -> assertEquals(
                        PropertyMetadataKind.NAVIGATION,
                        owner.kind()
                ),
                () -> assertTrue(
                        owner.isNavigation()
                ),
                () -> assertFalse(
                        owner.isPrimitive()
                ),
                () -> assertTrue(
                        owner.isSingle()
                ),
                () -> assertFalse(
                        owner.isCollection()
                ),
                () -> assertEquals(
                        NavigationCardinality.SINGLE,
                        owner.cardinality().orElseThrow()
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
                )
        );

        assertAll(
                () -> assertSame(
                        userMetadata,
                        username.declaringMetadata()
                ),
                () -> assertSame(
                        userMetadata.requireProperty("Username"),
                        username.propertyMetadata()
                ),
                () -> assertTrue(
                        username.isPrimitive()
                ),
                () -> assertEquals(
                        ExpressionType.STRING,
                        username.expressionType().orElseThrow()
                )
        );
    }

    @Test
    void shouldResolveDeepNavigationPath() {
        ResolvedMetadataPath path =
                resolver.resolve(
                        List.of(
                                "Owner",
                                "Department",
                                "Code"
                        )
                );

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
                        3,
                        path.size()
                ),
                () -> assertEquals(
                        ExpressionType.STRING,
                        path.expressionType()
                )
        );

        assertAll(
                () -> assertSame(
                        caseMetadata,
                        owner.declaringMetadata()
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
    void shouldResolveDirectDepartmentNavigationPath() {
        ResolvedMetadataPath path =
                resolver.resolve(
                        List.of(
                                "Department",
                                "Budget"
                        )
                );

        assertAll(
                () -> assertEquals(
                        "Department/Budget",
                        path.externalPath()
                ),
                () -> assertEquals(
                        "department/budget",
                        path.mappedPath()
                ),
                () -> assertEquals(
                        ExpressionType.DECIMAL,
                        path.expressionType()
                ),
                () -> assertEquals(
                        BigDecimal.class,
                        path.javaType()
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
    void shouldResolveReviewerThroughSameUserMetadataInstance() {
        ResolvedMetadataPath ownerPath =
                resolver.resolve(
                        List.of(
                                "Owner",
                                "Email"
                        )
                );

        ResolvedMetadataPath reviewerPath =
                resolver.resolve(
                        List.of(
                                "Reviewer",
                                "Email"
                        )
                );

        assertAll(
                () -> assertSame(
                        userMetadata,
                        ownerPath.rootSegment()
                                .targetMetadata()
                                .orElseThrow()
                ),
                () -> assertSame(
                        userMetadata,
                        reviewerPath.rootSegment()
                                .targetMetadata()
                                .orElseThrow()
                ),
                () -> assertSame(
                        ownerPath.leaf().propertyMetadata(),
                        reviewerPath.leaf().propertyMetadata()
                ),
                () -> assertNotSame(
                        ownerPath,
                        reviewerPath
                )
        );
    }

    @Test
    void shouldResolveUsingUserMetadataAsRoot() {
        MetadataPropertyPathResolver userResolver =
                new MetadataPropertyPathResolver(
                        userMetadata
                );

        ResolvedMetadataPath path =
                userResolver.resolve(
                        List.of(
                                "Department",
                                "Enabled"
                        )
                );

        assertAll(
                () -> assertSame(
                        userMetadata,
                        path.rootMetadata()
                ),
                () -> assertEquals(
                        "Department/Enabled",
                        path.externalPath()
                ),
                () -> assertEquals(
                        "department/enabled",
                        path.mappedPath()
                ),
                () -> assertEquals(
                        ExpressionType.BOOLEAN,
                        path.expressionType()
                ),
                () -> assertSame(
                        departmentMetadata,
                        path.rootSegment()
                                .targetMetadata()
                                .orElseThrow()
                )
        );
    }

    @Test
    void shouldResolveUsingDepartmentMetadataAsRoot() {
        MetadataPropertyPathResolver departmentResolver =
                new MetadataPropertyPathResolver(
                        departmentMetadata
                );

        ResolvedMetadataPath path =
                departmentResolver.resolve(
                        List.of("Code")
                );

        assertAll(
                () -> assertSame(
                        departmentMetadata,
                        path.rootMetadata()
                ),
                () -> assertEquals(
                        "Code",
                        path.externalPath()
                ),
                () -> assertEquals(
                        "code",
                        path.mappedPath()
                ),
                () -> assertEquals(
                        ExpressionType.STRING,
                        path.expressionType()
                )
        );
    }

    @Test
    void shouldCreateIndependentPathObjectsWhileReusingMetadataDescriptors() {
        ResolvedMetadataPath first =
                resolver.resolve(
                        List.of(
                                "Owner",
                                "Department",
                                "Code"
                        )
                );

        ResolvedMetadataPath second =
                resolver.resolve(
                        List.of(
                                "Owner",
                                "Department",
                                "Code"
                        )
                );

        assertAll(
                () -> assertNotSame(
                        first,
                        second
                ),
                () -> assertEquals(
                        first,
                        second
                ),
                () -> assertSame(
                        first.rootMetadata(),
                        second.rootMetadata()
                ),
                () -> assertSame(
                        first.rootSegment().propertyMetadata(),
                        second.rootSegment().propertyMetadata()
                ),
                () -> assertSame(
                        first.leaf().propertyMetadata(),
                        second.leaf().propertyMetadata()
                )
        );
    }

    @Test
    void shouldNotDependOnLaterMutationOfInputList() {
        List<String> input =
                new ArrayList<>(
                        List.of(
                                "Owner",
                                "Username"
                        )
                );

        ResolvedMetadataPath path =
                resolver.resolve(input);

        input.set(
                0,
                "Reviewer"
        );

        assertAll(
                () -> assertEquals(
                        "Owner/Username",
                        path.externalPath()
                ),
                () -> assertEquals(
                        List.of(
                                "Owner",
                                "Username"
                        ),
                        path.externalSegments()
                )
        );
    }

    @Test
    void shouldExposeImmutableResolvedPathLists() {
        ResolvedMetadataPath path =
                resolver.resolve(
                        List.of(
                                "Owner",
                                "Department",
                                "Code"
                        )
                );

        assertAll(
                () -> assertThrows(
                        UnsupportedOperationException.class,
                        () -> path.segments().clear()
                ),
                () -> assertThrows(
                        UnsupportedOperationException.class,
                        () -> path.externalSegments().add(
                                "Other"
                        )
                ),
                () -> assertThrows(
                        UnsupportedOperationException.class,
                        () -> path.mappedSegments().add(
                                "other"
                        )
                )
        );
    }

    @Test
    void shouldRejectUnknownRootProperty() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> resolver.resolve(
                                List.of("UnknownProperty")
                        )
                );

        assertAll(
                () -> assertTrue(
                        exception.getMessage().contains(
                                "UnknownProperty"
                        )
                ),
                () -> assertTrue(
                        exception.getMessage().contains(
                                "Case"
                        )
                )
        );
    }

    @Test
    void shouldRejectUnknownNestedProperty() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> resolver.resolve(
                                List.of(
                                        "Owner",
                                        "UnknownProperty"
                                )
                        )
                );

        assertAll(
                () -> assertTrue(
                        exception.getMessage().contains(
                                "UnknownProperty"
                        )
                ),
                () -> assertTrue(
                        exception.getMessage().contains(
                                "User"
                        )
                ),
                () -> assertTrue(
                        exception.getMessage().contains(
                                "Owner/UnknownProperty"
                        )
                )
        );
    }

    @Test
    void shouldRejectTraversalThroughPrimitiveProperty() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> resolver.resolve(
                                List.of(
                                        "Title",
                                        "Value"
                                )
                        )
                );

        assertAll(
                () -> assertTrue(
                        exception.getMessage().contains(
                                "Title"
                        )
                ),
                () -> assertTrue(
                        exception.getMessage().contains(
                                "Title/Value"
                        )
                )
        );
    }

    @Test
    void shouldRejectNavigationPropertyAsFinalSegment() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> resolver.resolve(
                                List.of("Owner")
                        )
                );

        assertTrue(
                exception.getMessage().contains(
                        "Owner"
                )
        );
    }

    @Test
    void shouldTreatExternalPropertyNamesAsCaseSensitive() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> resolver.resolve(
                                List.of("title")
                        )
                );

        assertTrue(
                exception.getMessage().contains(
                        "title"
                )
        );
    }

    @Test
    void shouldRejectEmptyPath() {
        assertThrows(
                IllegalArgumentException.class,
                () -> resolver.resolve(
                        List.of()
                )
        );
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