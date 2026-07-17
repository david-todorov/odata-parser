package io.github.davidtodorov.odataparser.search.ast;

import io.github.davidtodorov.odataparser.common.metadata.SourceSpan;

import java.util.Objects;

public record SearchPhraseExpression(
        String phrase,
        String rawText,
        SourceSpan sourceSpan
) implements SearchExpression {


    public SearchPhraseExpression(String phrase) {
        this(
                phrase,
                "\"" + Objects.requireNonNull(
                        phrase,
                        "Search phrase cannot be null"
                ) + "\"",
                SourceSpan.unknown()
        );
    }


    public SearchPhraseExpression(
            String phrase,
            SourceSpan sourceSpan
    ) {
        this(
                phrase,
                "\"" + Objects.requireNonNull(
                        phrase,
                        "Search phrase cannot be null"
                ) + "\"",
                sourceSpan
        );
    }


    public SearchPhraseExpression {
        Objects.requireNonNull(
                phrase,
                "Search phrase cannot be null"
        );

        Objects.requireNonNull(
                rawText,
                "Search phrase raw text cannot be null"
        );

        Objects.requireNonNull(
                sourceSpan,
                "Search phrase source span cannot be null"
        );

        if (phrase.isBlank()) {
            throw new IllegalArgumentException(
                    "Search phrase cannot be blank"
            );
        }

        if (rawText.length() < 2
                || rawText.charAt(0) != '"'
                || rawText.charAt(rawText.length() - 1) != '"') {

            throw new IllegalArgumentException(
                    "Search phrase raw text must be enclosed in double quotes"
            );
        }
    }

    @Override
    public <R> R accept(
            SearchExpressionVisitor<R> visitor
    ) {
        Objects.requireNonNull(
                visitor,
                "Search expression visitor cannot be null"
        );

        return visitor.visitPhraseExpression(this);
    }
}