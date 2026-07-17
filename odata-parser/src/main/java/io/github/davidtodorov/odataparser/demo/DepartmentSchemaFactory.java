package io.github.davidtodorov.odataparser.demo;

import io.github.davidtodorov.odataparser.common.type.ExpressionType;
import io.github.davidtodorov.odataparser.schema.EntitySchema;
import io.github.davidtodorov.odataparser.schema.PropertyDefinition;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DepartmentSchemaFactory {

    private DepartmentSchemaFactory() {
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
                "Name",
                PropertyDefinition.primitive(
                        "Name",
                        "name",
                        ExpressionType.STRING,
                        String.class
                )
        );

        properties.put(
                "Code",
                PropertyDefinition.primitive(
                        "Code",
                        "code",
                        ExpressionType.STRING,
                        String.class
                )
        );

        properties.put(
                "Enabled",
                PropertyDefinition.primitive(
                        "Enabled",
                        "enabled",
                        ExpressionType.BOOLEAN,
                        Boolean.class
                )
        );

        properties.put(
                "Budget",
                PropertyDefinition.primitive(
                        "Budget",
                        "budget",
                        ExpressionType.DECIMAL,
                        BigDecimal.class
                )
        );

        return new EntitySchema(
                "Department",
                DepartmentModel.class,
                properties
        );
    }
}