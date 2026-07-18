package io.github.davidtodorov.odataparser.meta.demo;

import io.github.davidtodorov.odataparser.demo.CaseModel;
import io.github.davidtodorov.odataparser.demo.DepartmentModel;
import io.github.davidtodorov.odataparser.demo.UserModel;
import io.github.davidtodorov.odataparser.meta.*;

public final class MetadataGraphDemo {

    private MetadataGraphDemo() {
    }

    public static void run() {
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

        printHeader("CONNECTED METADATA GRAPH");

        System.out.println("Registry size: " + registry.size());
        System.out.println("Registered names: " + registry.byName().keySet());
        System.out.println();

        printEntity(caseMetadata);
        printEntity(userMetadata);
        printEntity(departmentMetadata);

        verifyConnections(
                caseMetadata,
                userMetadata,
                departmentMetadata
        );
    }

    private static void printEntity(
            EntityMetadata<?> metadata
    ) {
        System.out.println(
                metadata.name()
                        + "Metadata"
                        + " | entity-type="
                        + metadata.entityType().getSimpleName()
                        + " | properties="
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
                                + primitive.javaType().getSimpleName()
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
                                + " | target="
                                + navigation.targetMetadata().name()
                                + " | target-type="
                                + navigation.javaType().getSimpleName()
                                + " | cardinality="
                                + navigation.cardinality()
                );
            }
        }

        System.out.println();
    }

    private static void verifyConnections(
            EntityMetadata<CaseModel> caseMetadata,
            EntityMetadata<UserModel> userMetadata,
            EntityMetadata<DepartmentModel> departmentMetadata
    ) {
        printHeader("CONNECTION CHECKS");

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

        printCheck(
                "Case.Owner references UserMetadata",
                owner.targetMetadata() == userMetadata
        );

        printCheck(
                "Case.Reviewer references the same UserMetadata",
                reviewer.targetMetadata() == userMetadata
        );

        printCheck(
                "Owner and Reviewer share one target instance",
                owner.targetMetadata()
                        == reviewer.targetMetadata()
        );

        printCheck(
                "Case.Department references DepartmentMetadata",
                caseDepartment.targetMetadata()
                        == departmentMetadata
        );

        printCheck(
                "User.Department references the same DepartmentMetadata",
                userDepartment.targetMetadata()
                        == departmentMetadata
        );

        System.out.println();
    }

    private static <O, T>
    NavigationPropertyMetadata<O, T> requireNavigation(
            EntityMetadata<O> ownerMetadata,
            String externalName,
            Class<T> targetType
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

        if (!targetType.equals(navigation.javaType())) {
            throw new IllegalStateException(
                    "Navigation property '"
                            + ownerMetadata.name()
                            + "."
                            + externalName
                            + "' targets '"
                            + navigation.javaType().getName()
                            + "' instead of expected type '"
                            + targetType.getName()
                            + "'"
            );
        }

        @SuppressWarnings("unchecked")
        NavigationPropertyMetadata<O, T> typedNavigation =
                (NavigationPropertyMetadata<O, T>) navigation;

        return typedNavigation;
    }

    private static void printCheck(
            String description,
            boolean successful
    ) {
        System.out.println(
                (successful ? "[PASS] " : "[FAIL] ")
                        + description
        );

        if (!successful) {
            throw new IllegalStateException(
                    "Metadata connection check failed: "
                            + description
            );
        }
    }

    private static void printHeader(
            String title
    ) {
        System.out.println("=".repeat(80));
        System.out.println(title);
        System.out.println("=".repeat(80));
    }
}