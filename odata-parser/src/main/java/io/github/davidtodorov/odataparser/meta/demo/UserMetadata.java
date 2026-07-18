package io.github.davidtodorov.odataparser.meta.demo;

import io.github.davidtodorov.odataparser.common.type.ExpressionType;
import io.github.davidtodorov.odataparser.demo.DepartmentModel;
import io.github.davidtodorov.odataparser.demo.UserModel;
import io.github.davidtodorov.odataparser.meta.AbstractEntityMetadata;
import io.github.davidtodorov.odataparser.meta.EntityMetadata;
import io.github.davidtodorov.odataparser.meta.MetadataContext;
import io.github.davidtodorov.odataparser.meta.NavigationJoinPolicy;

public final class UserMetadata
        extends AbstractEntityMetadata<UserModel> {

    public UserMetadata() {
        super(
                "User",
                UserModel.class
        );
    }

    @Override
    protected void configure(
            MetadataContext context
    ) {
        EntityMetadata<DepartmentModel> departmentMetadata =
                context.require(DepartmentModel.class);

        primitive(
                "Id",
                "id",
                Integer.class,
                ExpressionType.INTEGER
        );

        primitive(
                "Username",
                "username",
                String.class,
                ExpressionType.STRING
        );

        primitive(
                "DisplayName",
                "displayName",
                String.class,
                ExpressionType.STRING
        );

        primitive(
                "Email",
                "email",
                String.class,
                ExpressionType.STRING
        );

        primitive(
                "Active",
                "active",
                Boolean.class,
                ExpressionType.BOOLEAN
        );

        singleNavigation(
                "Department",
                "department",
                departmentMetadata,
                NavigationJoinPolicy.left()
        );
    }
}