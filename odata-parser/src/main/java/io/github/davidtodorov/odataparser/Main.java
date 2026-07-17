package io.github.davidtodorov.odataparser;

import io.github.davidtodorov.odataparser.expression.ast.*;
import io.github.davidtodorov.odataparser.expression.visitor.AstPrinter;
import io.github.davidtodorov.odataparser.filter.parser.FilterParser;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        String filter = "(Price add Tax mul 2 gt 100) or (Active eq true)";

        FilterParser parser = new FilterParser(filter);
        Expression root = parser.parse();

        AstPrinter printer = new AstPrinter();

        System.out.println(printer.print(root));
    }

}

