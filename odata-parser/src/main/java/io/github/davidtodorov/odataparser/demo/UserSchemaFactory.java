package io.github.davidtodorov.odataparser.demo;

import io.github.davidtodorov.odataparser.common.type.ExpressionType;
import io.github.davidtodorov.odataparser.schema.EntitySchema;
import io.github.davidtodorov.odataparser.schema.PropertyDefinition;

import java.util.LinkedHashMap;
import java.util.Map;

public final class UserSchemaFactory {

    private UserSchemaFactory() {
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
                "Username",
                PropertyDefinition.primitive(
                        "Username",
                        "username",
                        ExpressionType.STRING,
                        String.class
                )
        );

        properties.put(
                "DisplayName",
                PropertyDefinition.primitive(
                        "DisplayName",
                        "displayName",
                        ExpressionType.STRING,
                        String.class
                )
        );

        properties.put(
                "Email",
                PropertyDefinition.primitive(
                        "Email",
                        "email",
                        ExpressionType.STRING,
                        String.class
                )
        );

        properties.put(
                "Active",
                PropertyDefinition.primitive(
                        "Active",
                        "active",
                        ExpressionType.BOOLEAN,
                        Boolean.class
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
                "User",
                UserModel.class,
                properties
        );
    }
}