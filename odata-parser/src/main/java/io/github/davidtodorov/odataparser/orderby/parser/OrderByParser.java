package io.github.davidtodorov.odataparser.orderby.parser;

import io.github.davidtodorov.odataparser.common.metadata.SourceSpan;
import io.github.davidtodorov.odataparser.orderby.ast.OrderByDirection;
import io.github.davidtodorov.odataparser.orderby.ast.OrderByItem;
import io.github.davidtodorov.odataparser.orderby.ast.OrderByOption;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class OrderByParser {

    private final String input;
    private int position;

    public OrderByParser(String input) {
        this.input = Objects.requireNonNull(
                input,
                "Order-by input cannot be null"
        );

        this.position = 0;
    }

    public OrderByOption parse() {
        skipWhitespace();

        if (isAtEnd()) {
            throw error(
                    "Order-by option cannot be empty"
            );
        }

        int optionStart = position;

        List<OrderByItem> items =
                new ArrayList<>();

        while (true) {
            items.add(parseItem());

            skipWhitespace();

            if (isAtEnd()) {
                break;
            }

            if (!match(',')) {
                throw error(
                        "Expected ',' or end of input"
                );
            }

            skipWhitespace();

            if (isAtEnd()) {
                throw error(
                        "Expected another order-by item after ','"
                );
            }
        }

        int optionEnd =
                items.get(items.size() - 1)
                        .sourceSpan()
                        .end();

        return new OrderByOption(
                items,
                new SourceSpan(
                        optionStart,
                        optionEnd
                )
        );
    }

    private OrderByItem parseItem() {
        skipWhitespace();

        int itemStart = position;

        if (!isIdentifierStart(currentCharacter())) {
            throw error(
                    "Expected a property name"
            );
        }

        List<String> pathSegments =
                parsePropertyPath();

        int itemEnd = lastNonWhitespacePosition();

        skipWhitespace();

        OrderByDirection direction =
                OrderByDirection.ASCENDING;

        if (!isAtEnd()
                && currentCharacter() != ',') {

            if (!isIdentifierStart(currentCharacter())) {
                throw error(
                        "Expected 'asc', 'desc', ',' or end of input"
                );
            }

            int directionStart = position;
            String directionKeyword =
                    readIdentifier();

            try {
                direction =
                        OrderByDirection.fromKeyword(
                                directionKeyword
                        );
            } catch (IllegalArgumentException exception) {
                throw new OrderByParserException(
                        "Unsupported order-by direction '"
                                + directionKeyword
                                + "'. Expected 'asc' or 'desc'",
                        directionStart,
                        exception
                );
            }

            itemEnd = position;

            skipWhitespace();

            if (!isAtEnd()
                    && currentCharacter() != ',') {

                throw error(
                        "Expected ',' or end of input"
                );
            }
        }

        return new OrderByItem(
                pathSegments,
                direction,
                new SourceSpan(
                        itemStart,
                        itemEnd
                )
        );
    }

    private List<String> parsePropertyPath() {
        List<String> pathSegments =
                new ArrayList<>();

        pathSegments.add(
                readIdentifier()
        );

        while (true) {
            skipWhitespace();

            if (!match('/')) {
                break;
            }

            skipWhitespace();

            if (isAtEnd()
                    || !isIdentifierStart(
                    currentCharacter()
            )) {

                throw error(
                        "Expected a property name after '/'"
                );
            }

            pathSegments.add(
                    readIdentifier()
            );
        }

        return List.copyOf(pathSegments);
    }

    private String readIdentifier() {
        if (isAtEnd()
                || !isIdentifierStart(
                currentCharacter()
        )) {

            throw error(
                    "Expected an identifier"
            );
        }

        int start = position;

        advance();

        while (!isAtEnd()
                && isIdentifierPart(
                currentCharacter()
        )) {

            advance();
        }

        return input.substring(
                start,
                position
        );
    }

    private void skipWhitespace() {
        while (!isAtEnd()
                && Character.isWhitespace(
                currentCharacter()
        )) {

            advance();
        }
    }

    private boolean match(char expected) {
        if (isAtEnd()
                || currentCharacter() != expected) {

            return false;
        }

        advance();
        return true;
    }

    private void advance() {
        position++;
    }

    private boolean isAtEnd() {
        return position >= input.length();
    }

    private char currentCharacter() {
        if (isAtEnd()) {
            return '\0';
        }

        return input.charAt(position);
    }

    private boolean isIdentifierStart(char character) {
        return character == '_'
                || Character.isLetter(character);
    }

    private boolean isIdentifierPart(char character) {
        return character == '_'
                || Character.isLetterOrDigit(character);
    }

    private int lastNonWhitespacePosition() {
        int end = position;

        while (end > 0
                && Character.isWhitespace(
                input.charAt(end - 1)
        )) {

            end--;
        }

        return end;
    }

    private OrderByParserException error(
            String message
    ) {
        return new OrderByParserException(
                message,
                position
        );
    }
}