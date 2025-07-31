package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.craftinginterpreters.lox.TokenType.*;

public class Scanner {
   private final String source;
   private final ArrayList<Token> tokens = new ArrayList<>();
   private int line = 1;
   private int start = 0;
   private int curr = 0;

private static final HashMap<String, TokenType> keywords = new HashMap<>();

static {
    keywords.put("and", AND);
    keywords.put("break", BREAK);
    keywords.put("class", CLASS);
    keywords.put("else", ELSE);
    keywords.put("extends", EXTENDS);
    keywords.put("false", FALSE);
    keywords.put("for", FOR);
    keywords.put("fun", FUN);
    keywords.put("if", IF);
    keywords.put("nil", NIL);
    keywords.put("or", OR);
    keywords.put("print", PRINT);
    keywords.put("return", RETURN);
    keywords.put("super", SUPER);
    keywords.put("this", THIS);
    keywords.put("true", TRUE);
    keywords.put("var", VAR);
    keywords.put("while", WHILE);
    keywords.put("continue", CONTINUE);
}

Scanner(String source) {
        this.source = source;
    }

    private Boolean isAtEnd() {
        return curr >= source.length();
    }

    private char peekNext() {
        if (isAtEnd()) return '\0';
        if (curr + 1 >= source.length()) return '\0';
        return source.charAt(curr + 1);
    }

    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(curr);
    }

    private char advance() {
        return source.charAt(curr++);
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, curr);
        tokens.add(new Token(type, text, literal, line));
    }

    private Boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(curr) != expected) return false;
        curr++;
        return true;
    }

    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++;
            advance();
        }
        if (isAtEnd()) {
            Lox.error(line, "Unterminated string");
        }
        advance();  // bump curr past closing quote
        String literal = source.substring(start + 1, curr - 1);
        addToken(STRING, literal);
    }

    private Boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private Boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || 
               (c >= 'A' && c <= 'Z') ||
               c == '_';
    }

    private Boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private void number() {
        while (!isAtEnd() && isDigit(peek())) advance();

        if (isAtEnd()) {
            String literal;
            // System.err.println(source.length());
            // System.out.println(source.charAt(start));
            // if (start == source.length() - 1) {
            //     literal = source.substring(start, curr);
            // } else {
            //     literal = source.substring(start + 1, curr - 1);
            // }
            literal = source.substring(start, curr);
            addToken(NUMBER, Double.parseDouble(literal));
            return;
        }

        if (peek() == '.') {
            advance();
            while (!isAtEnd() && isDigit(peek())) advance();
        }

        Double literal = Double.parseDouble(source.substring(start, curr));
        addToken(NUMBER, literal);
    }

    private void keywordOrIdentifier() {
        while (!isAtEnd() && isAlphaNumeric(peek())) advance();
        String rawText = source.substring(start, curr);
        TokenType type = keywords.get(rawText);
        if (type == null) type = IDENTIFIER;
        addToken(type, rawText);
    }

    private void skipMultiLineComment() {
        while (!isAtEnd()) {
            if (peek() == '/' && peekNext() == '*') {   // nested multi-liner
                advance(); // Consume '/'
                advance(); // Consume '*'
                skipMultiLineComment();
            }
            if (peek() == '\n') line++;
            if (peek() == '*' && peekNext() == '/') {
                advance(); // Consume '*'
                advance(); // Consume '/'
                return;
            }
            advance();
        }
        // Error handling for unterminated comment
        Lox.error(line, "Unterminated multi-line comment");
    }

    private void scanToken() {
        if (isAtEnd()) {
            addToken(EOF);
            return;
        }

        char c = advance();

        switch (c) {
            case '%': addToken(MODULO); break;
            case ';': addToken(SEMICOLON); break;
            case '.': addToken(DOT); break;
            case ',': addToken(COMMA); break;
            case '(': addToken(LEFT_PAREN); break;
            case ')': addToken(RIGHT_PAREN); break;
            case '[': addToken(LEFT_BRACK); break;
            case ']': addToken(RIGHT_BRACK); break;
            case '{': addToken(LEFT_BRACE); break;
            case '}': addToken(RIGHT_BRACE); break;
            case '?': addToken(QUESTION_MARK); break;
            case ':': addToken(COLON); break;
            case '&': addToken(BITWISE_AND); break;
            case '|': addToken(BITWISE_OR); break;
            case '~': addToken(BITWISE_NOT); break;
            case '^': addToken(BITWISE_XOR); break;
            case '+': 
                addToken(match('+') ? PLUS_PLUS : PLUS);
                break;
            case '-':
                addToken(match('-') ? MINUS_MINUS : MINUS);
                break;
            case '*':
                addToken(match('*') ? STAR_STAR : STAR);
                break;
            case '=':
                addToken(match('=') ? EQUAL_EQUAL : EQUAL);
                break;
            case '!':
                addToken(match('=') ? BANG_EQUAL : BANG);
                break;
            case '<':
                addToken(match('=') ? LESS_EQUAL : match('<') ? BITSHIFT_LEFT : LESS);
                break;
            case '>':
                addToken(match('=') ? GREATER_EQUAL : match('>') ? BITSHIFT_RIGHT : GREATER);
                break;
            case ' ':
            case '\t':
            case '\r':
                break;
            case '\n':
                line++;
                break;
            case '/':
                if (match('/')) {   // Single-line comment
                    while (!isAtEnd() && peek() != '\n') advance();
                } else if (match('*')) {    // Multi-line comment
                    skipMultiLineComment();
                } else {
                    addToken(SLASH);
                }
                break;
            case '"':
                string();
                break;
            default:
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    keywordOrIdentifier();
                } else {
                    Lox.error(line, "Unexpected character " + c);
                }
                break;
        }
    }

    List<Token> scanTokens() {
        // System.out.println(source.length());
        if (source.length() == 0) { // empty source
            tokens.add(new Token(EOF, "", null, line));
        } else {
            while (!isAtEnd()) {
                start = curr;
                scanToken();
            }
            // For handling comment-only source or token list missing EOF token
            if (tokens.size() == 0 || tokens.get(tokens.size() - 1).type != EOF)
                tokens.add(new Token(EOF, "", null, line));
        }
        return tokens;
    }

}
