package com.craftinginterpreters.lox;

// typedef enum {
//     ...
// } TokenType;

enum TokenType {
    // Single-character tokens
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE, LEFT_BRACK, RIGHT_BRACK,
    COMMA, DOT, MINUS, PLUS, SEMICOLON, SLASH, STAR, MODULO, QUESTION_MARK, COLON,
    BITWISE_AND, BITWISE_OR, BITWISE_NOT, BITWISE_XOR,

    // One or two character tokens
    BANG, BANG_EQUAL,
    EQUAL, EQUAL_EQUAL,
    GREATER, GREATER_EQUAL,
    LESS, LESS_EQUAL,
    BITSHIFT_LEFT, BITSHIFT_RIGHT,
    PLUS_PLUS, MINUS_MINUS,
    STAR_STAR,

    // Literals
    IDENTIFIER, STRING, NUMBER,

    // Keywords
    AND, BREAK, CLASS, ELSE, FALSE, FUN, FOR, IF, NIL, OR,
    PRINT, RETURN, SUPER, THIS, TRUE, VAR, WHILE, CONTINUE,

    EOF
}
