package io.github.davidtodorov.odataparser.demo;

import io.github.davidtodorov.odataparser.common.type.ExpressionType;
import io.github.davidtodorov.odataparser.schema.EntitySchema;
import io.github.davidtodorov.odataparser.schema.PropertyDefinition;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public final class CaseSchemaFactory {

    private CaseSchemaFactory() {
    }

    public static EntitySchema create() {
        Map<String, PropertyDefinition> properties =
                new LinkedHashMap<>();

        properties.put(
                "Id",
                PropertyDefinition.primitive(
                        "Id",
                        "id",
                        ExpressionType.INTEGER,
                        Integer.class
                )
        );

        properties.put(
                "Reference",
                PropertyDefinition.primitive(
                        "Reference",
                        "reference",
                        ExpressionType.STRING,
                        String.class
                )
        );

        properties.put(
                "Title",
                PropertyDefinition.primitive(
                        "Title",
                        "title",
                        ExpressionType.STRING,
                        String.class
                )
        );

        properties.put(
                "Amount",
                PropertyDefinition.primitive(
                        "Amount",
                        "amount",
                        ExpressionType.DECIMAL,
                        BigDecimal.class
                )
        );

        properties.put(
                "Priority",
                PropertyDefinition.primitive(
                        "Priority",
                        "priority",
                        ExpressionType.INTEGER,
                        Integer.class
                )
        );

        properties.put(
                "Deleted",
                PropertyDefinition.primitive(
                        "Deleted",
                        "deleted",
                        ExpressionType.BOOLEAN,
                        Boolean.class
                )
        );

        properties.put(
                "Closed",
                PropertyDefinition.primitive(
                        "Closed",
                        "closed",
                        ExpressionType.BOOLEAN,
                        Boolean.class
                )
        );

        properties.put(
                "Owner",
                PropertyDefinition.navigation(
                        "Owner",
                        "owner",
                        "User",
                        UserModel.class
                )
        );

        properties.put(
                "Reviewer",
                PropertyDefinition.navigation(
                        "Reviewer",
                        "reviewer",
                        "User",
                        UserModel.class
                )
        );

        properties.put(
                "Department",
                PropertyDefinition.navigation(
                        "Department",
                        "department",
                        "Department",
                        DepartmentModel.class
                )
        );

        return new EntitySchema(
                "Case",
                CaseModel.class,
                properties
        );
    }
}