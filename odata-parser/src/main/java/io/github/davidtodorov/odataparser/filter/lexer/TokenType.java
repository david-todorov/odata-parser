package io.github.davidtodorov.odataparser.filter.lexer;

public enum TokenType {

    // Names such as Price, Address, City and contains
    IDENTIFIER,

    // Literal values
    STRING,
    INTEGER,
    DECIMAL,
    BOOLEAN,
    NULL,

    // Logical operators
    AND,
    OR,
    NOT,

    // Comparison operators
    EQ,
    NE,
    GT,
    GE,
    LT,
    LE,
    IN,

    // Arithmetic operators
    ADD,
    SUB,
    MUL,
    DIV,
    MOD,

    // Structural tokens
    LEFT_PAREN,
    RIGHT_PAREN,
    COMMA,
    SLASH,

    // Marks the end of the input
    END_OF_INPUT
}
