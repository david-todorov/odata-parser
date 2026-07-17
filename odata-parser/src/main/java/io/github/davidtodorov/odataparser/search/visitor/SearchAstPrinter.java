package io.github.davidtodorov.odataparser.search.visitor;

import io.github.davidtodorov.odataparser.common.metadata.SourceSpan;
import io.github.davidtodorov.odataparser.search.ast.SearchBinaryExpression;
import io.github.davidtodorov.odataparser.search.ast.SearchExpression;
import io.github.davidtodorov.odataparser.search.ast.SearchExpressionVisitor;
import io.github.davidtodorov.odataparser.search.ast.SearchNotExpression;
import io.github.davidtodorov.odataparser.search.ast.SearchPhraseExpression;
import io.github.davidtodorov.odataparser.search.ast.SearchTermExpression;

import java.util.Objects;

public final class SearchAstPrinter
        implements SearchExpressionVisitor<String> {

    private static final String INDENTATION = "  ";
    private static final String LINE_SEPARATOR =
            System.lineSeparator();

    public String print(
            SearchExpression expression
    ) {
        Objects.requireNonNull(
                expression,
                "Search expression cannot be null"
        );

        return expression.accept(this);
    }

    @Override
    public String visitTermExpression(
            SearchTermExpression expression
    ) {
        Objects.requireNonNull(
                expression,
                "Search term expression cannot be null"
        );

        return "TERM"
                + " | value="
                + quote(expression.term())
                + " | span="
                + formatSpan(expression.sourceSpan());
    }

    @Override
    public String visitPhraseExpression(
            SearchPhraseExpression expression
    ) {
        Objects.requireNonNull(
                expression,
                "Search phrase expression cannot be null"
        );

        return "PHRASE"
                + " | value="
                + quote(expression.phrase())
                + " | raw-text="
                + displayRawText(expression.rawText())
                + " | span="
                + formatSpan(expression.sourceSpan());
    }

    @Override
    public String visitNotExpression(
            SearchNotExpression expression
    ) {
        Objects.requireNonNull(
                expression,
                "Search NOT expression cannot be null"
        );

        StringBuilder builder = new StringBuilder();

        builder.append("NOT")
                .append(" | span=")
                .append(formatSpan(expression.sourceSpan()));

        appendChild(
                builder,
                "OPERAND",
                expression.operand()
        );

        return builder.toString();
    }

    @Override
    public String visitBinaryExpression(
            SearchBinaryExpression expression
    ) {
        Objects.requireNonNull(
                expression,
                "Search binary expression cannot be null"
        );

        StringBuilder builder = new StringBuilder();

        builder.append("BINARY")
                .append(" | operator=")
                .append(expression.operator())
                .append(" | keyword=")
                .append(expression.operator().keyword())
                .append(" | span=")
                .append(formatSpan(expression.sourceSpan()));

        appendChild(
                builder,
                "LEFT",
                expression.left()
        );

        appendChild(
                builder,
                "RIGHT",
                expression.right()
        );

        return builder.toString();
    }

    private void appendChild(
            StringBuilder builder,
            String label,
            SearchExpression child
    ) {
        builder.append(LINE_SEPARATOR)
                .append(INDENTATION)
                .append(label)
                .append(":")
                .append(LINE_SEPARATOR)
                .append(
                        indent(
                                child.accept(this),
                                2
                        )
                );
    }

    private String indent(
            String text,
            int levels
    ) {
        String indentation =
                INDENTATION.repeat(levels);

        return indentation
                + text.replace(
                LINE_SEPARATOR,
                LINE_SEPARATOR + indentation
        );
    }

    private String formatSpan(
            SourceSpan sourceSpan
    ) {
        if (sourceSpan.isUnknown()) {
            return "unknown";
        }

        return "["
                + sourceSpan.start()
                + ", "
                + sourceSpan.end()
                + ")";
    }

    private String quote(
            String value
    ) {
        return "\""
                + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t")
                + "\"";
    }


    private String displayRawText(
            String rawText
    ) {
        return rawText
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }
}