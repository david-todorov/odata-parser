package io.github.davidtodorov.odataparser.meta.demo;

import io.github.davidtodorov.odataparser.common.type.ExpressionType;
import io.github.davidtodorov.odataparser.demo.DepartmentModel;
import io.github.davidtodorov.odataparser.meta.AbstractEntityMetadata;
import io.github.davidtodorov.odataparser.meta.MetadataContext;

import java.math.BigDecimal;

public final class DepartmentMetadata
        extends AbstractEntityMetadata<DepartmentModel> {

    public DepartmentMetadata() {
        super(
                "Department",
                DepartmentModel.class
        );
    }

    @Override
    protected void configure(
            MetadataContext context
    ) {
        primitive(
                "Id",
                "id",
                Integer.class,
                ExpressionType.INTEGER
        );

        primitive(
                "Name",
                "name",
                String.class,
                ExpressionType.STRING
        );

        primitive(
                "Code",
                "code",
                String.class,
                ExpressionType.STRING
        );

        primitive(
                "Enabled",
                "enabled",
                Boolean.class,
                ExpressionType.BOOLEAN
        );

        primitive(
                "Budget",
                "budget",
                BigDecimal.class,
                ExpressionType.DECIMAL
        );
    }
}
