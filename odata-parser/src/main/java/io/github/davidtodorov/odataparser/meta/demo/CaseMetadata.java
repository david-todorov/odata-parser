package io.github.davidtodorov.odataparser.meta.demo;

import io.github.davidtodorov.odataparser.common.type.ExpressionType;
import io.github.davidtodorov.odataparser.demo.CaseModel;
import io.github.davidtodorov.odataparser.demo.DepartmentModel;
import io.github.davidtodorov.odataparser.demo.UserModel;
import io.github.davidtodorov.odataparser.meta.AbstractEntityMetadata;
import io.github.davidtodorov.odataparser.meta.EntityMetadata;
import io.github.davidtodorov.odataparser.meta.MetadataContext;
import io.github.davidtodorov.odataparser.meta.NavigationJoinPolicy;

import java.math.BigDecimal;

public final class CaseMetadata
        extends AbstractEntityMetadata<CaseModel> {

    public CaseMetadata() {
        super(
                "Case",
                CaseModel.class
        );
    }

    @Override
    protected void configure(
            MetadataContext context
    ) {
        EntityMetadata<UserModel> userMetadata =
                context.require(UserModel.class);

        EntityMetadata<DepartmentModel> departmentMetadata =
                context.require(DepartmentModel.class);

        primitive(
                "Id",
                "id",
                Integer.class,
                ExpressionType.INTEGER
        );

        primitive(
                "Reference",
                "reference",
                String.class,
                ExpressionType.STRING
        );

        primitive(
                "Title",
                "title",
                String.class,
                ExpressionType.STRING
        );

        primitive(
                "Amount",
                "amount",
                BigDecimal.class,
                ExpressionType.DECIMAL
        );

        primitive(
                "Priority",
                "priority",
                Integer.class,
                ExpressionType.INTEGER
        );

        primitive(
                "Deleted",
                "deleted",
                Boolean.class,
                ExpressionType.BOOLEAN
        );

        primitive(
                "Closed",
                "closed",
                Boolean.class,
                ExpressionType.BOOLEAN
        );

        singleNavigation(
                "Owner",
                "owner",
                userMetadata,
                NavigationJoinPolicy.left()
        );

        singleNavigation(
                "Reviewer",
                "reviewer",
                userMetadata,
                NavigationJoinPolicy.left()
        );

        singleNavigation(
                "Department",
                "department",
                departmentMetadata,
                NavigationJoinPolicy.left()
        );
    }
}