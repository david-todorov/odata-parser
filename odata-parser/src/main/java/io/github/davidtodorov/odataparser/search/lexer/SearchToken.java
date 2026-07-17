package io.github.davidtodorov.odataparser.search.lexer;

import io.github.davidtodorov.odataparser.common.metadata.SourceSpan;

import java.util.Objects;

public record SearchToken(
        SearchTokenType type,
        String lexeme,
        String value,
        SourceSpan sourceSpan
) {

    public SearchToken {
        Objects.requireNonNull(
                type,
                "Search token type cannot be null"
        );

        Objects.requireNonNull(
                lexeme,
                "Search token lexeme cannot be null"
        );

        Objects.requireNonNull(
                value,
                "Search token value cannot be null"
        );

        Objects.requireNonNull(
                sourceSpan,
                "Search token source span cannot be null"
        );

        if (type == SearchTokenType.END_OF_INPUT) {
            if (!lexeme.isEmpty() || !value.isEmpty()) {
                throw new IllegalArgumentException(
                        "The end-of-input token must have an empty "
                                + "lexeme and value"
                );
            }
        } else {
            if (lexeme.isEmpty()) {
                throw new IllegalArgumentException(
                        "Search token lexeme cannot be empty for token type "
                                + type
                );
            }

            if (type == SearchTokenType.PHRASE) {
                validatePhraseToken(
                        lexeme,
                        value
                );
            }
        }
    }

    public SearchToken(
            SearchTokenType type,
            String lexeme,
            SourceSpan sourceSpan
    ) {
        this(
                type,
                lexeme,
                lexeme,
                sourceSpan
        );
    }

    public static SearchToken endOfInput(
            int position
    ) {
        if (position < 0) {
            throw new IllegalArgumentException(
                    "End-of-input position cannot be negative"
            );
        }

        return new SearchToken(
                SearchTokenType.END_OF_INPUT,
                "",
                "",
                new SourceSpan(
                        position,
                        position
                )
        );
    }

    public boolean is(
            SearchTokenType expectedType
    ) {
        return type == Objects.requireNonNull(
                expectedType,
                "Expected search token type cannot be null"
        );
    }

    public boolean startsExpression() {
        return type.startsExpression();
    }

    public boolean endsExpression() {
        return type.endsExpression();
    }

    public boolean isEndOfInput() {
        return type == SearchTokenType.END_OF_INPUT;
    }

    private static void validatePhraseToken(
            String lexeme,
            String value
    ) {
        if (lexeme.length() < 2
                || lexeme.charAt(0) != '"'
                || lexeme.charAt(lexeme.length() - 1) != '"') {

            throw new IllegalArgumentException(
                    "A search phrase token must be enclosed "
                            + "in double quotes"
            );
        }

        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    "A search phrase token cannot have a blank value"
            );
        }
    }
}