package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

import com.craftinginterpreters.lox.Stmt.Block;
import static com.craftinginterpreters.lox.TokenType.*;

/**
 * LOX GRAMMAR RULES:
 * ------------------
 * program      -> declaration* EOF ;
 * declaration  -> funDef | varDecl | classDecl | statement ;
 * classDecl    -> "class" IDENTIFIER ( "extends" IDENTIFIER ("," IDENTIFIER)* )? "{" (function | staticMethod | getterMethod)* "}" ;
 * funDef       -> "fun" function ;
 * staticMethod -> "class" function ;
 * getterMethod -> IDENTIFIER blockStmt ;
 * function     -> IDENTIFIER "(" parameters? ")" blockStmt ;
 * parameters   -> IDENTIFIER ( "," IDENTIFIER )* ;
 * varDecl      -> "var" IDENTIFIER ( "=" expression )? ";" ;
 * statement    -> printStmt | branchStmt | iterStmt | exprStmt | breakStmt | blockStmt | returnStmt ;
 * printStmt    -> "print" expression ";" ;
 * branchStmt   -> ifStmt | ifElseStmt ;
 * blockStmt    -> "{" declaration* "}" ;
 * iterStmt     -> whileStmt | forStmt ;
 * whileStmt    -> "while" "(" expression ")" statement ;
 * forStmt      -> "for" "(" (varStmt | exprStmt)? ";" expression? ";" expression? ")" statement ;
 * ifStmt       -> "if" "(" expression ")" statement ("else" statement)? ;
 * exprStmt     -> expression ";" ;
 * breakStmt    -> "break" ";" ;
 * continueStmt -> "continue" ";" ;
 * returnStmt   -> "return" expression ";" ;
 *
 * expression   -> comma ;
 * comma        -> assign ("," assign)* ;
 * assign       -> (call ".")? IDENTIFIER "=" assign | ternary ;
 * ternary      -> logical_or ("?" ternary ":" ternary)? ;
 * logical_or   -> equality ("or" equality)* ;
 * logical_and  -> equality ("and" equality)* ;
 * equality     -> bitwise ( ("==" | "!=") bitwise )* ;
 * bitwise      -> comparison ( ("&" | "|" | "^") comparison)* ;
 * comparison   -> bitshift ( (">" | "<" | ">=" | "<=") bitshift )* ;
 * bitshift     -> term ( ("<<" | ">>") term)* ;
 * term         -> factor ( ("+" | "-") factor )* ;
 * factor       -> unary ( ("*" | "/" | "%") unary)* ;
 * unary        -> ("!" | "-" | "~") unary | exp ;
 * exp          -> prefix ( "**" prefix )* ;
 * prefix       -> ("++" | "--") IDENTIFIER | index ;
 * index        -> postfix ("[" expression (":" expression)? "]")* ;
 * postfix      -> IDENTIFIER ("++" | "--") | call ;
 * call         -> primary ( "(" arguments? ")" | IDENTIFIER "." )* ;
 * arguments    -> expression ( "," expression )* ;
 * primary      -> NUMBER | STRING | "true" | "false" | "nil" | "this" | "(" expression ")" | IDENTIFIER | 
 *                 anonymous | list | error ;
 * list         -> "[" ( expression ( "," expression )*)? "]";
 * anonymous    -> "fun (" parameters? ")" blockStmt ;G
 * error        -> ( ("==" | "!=" | ">" | "<" | ">=" | "<=" | "+" | "-" | "*" | "/") expression ) ;
 */

public class Parser {
    private static class ParseError extends RuntimeException{}
    private final List<Token> tokens;
    private int curr = 0;

    //============================
    // Parser Interface Functions
    //============================
    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    // FOR DEBUG ONLY
    Expr parseExpression() {
        try {
            return expression();
        } catch (ParseError error) {
            return null;
        }
    }

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<Stmt>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }

    public boolean doneParsing() {
        return isAtEnd();
    }

    //=========================
    // Statements
    //=========================
    private Stmt declaration() {
        // declaration  -> funDef | varDecl | classDecl | statement ;
        try {
            if (match(VAR)) {
                return varDeclaration();
            } else if (peek().type == FUN && peekNext().type == IDENTIFIER) {
                match(FUN);
                return functionDef("function");   
            } else if (match(CLASS)) {
                return classDecl();
            }
            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt varDeclaration() {
        // varDecl -> "var" IDENTIFIER ( "=" expression )? ";" ;
        Token name = consume(IDENTIFIER, "Expect a variable name.");

        Expr initializer = null;
        if (match(EQUAL)) {
            initializer = expression();
        }

        consume(SEMICOLON, "Expect ';' after variable declaration.");
        
        return new Stmt.Var(name, initializer);
    }

    private Stmt classDecl() {
        // classDecl -> "class" IDENTIFIER ( "extends" IDENTIFIER ("," IDENTIFIER)* )? "{" ( function | staticMethod | getterMethod )* "}" ;
        Token name = consume(IDENTIFIER, "Expect a class name.");
        List<Expr.Variable> superClasses = new ArrayList<>();
        if (match(EXTENDS)) {
            Token className = consume(IDENTIFIER, "Expect a superclass name.");
            superClasses.add(new Expr.Variable(className));
            if (!check(LEFT_BRACE)) {
                while (match(COMMA)) {
                    if (superClasses.size() >= 255) {
                        error(peek(), "Cannot have greater than 255 super classes");
                    }
                    className = consume(IDENTIFIER, "Expect a superclass name."); 
                    superClasses.add(new Expr.Variable(className));
                }
            }
        }
        consume(LEFT_BRACE, "Expected a '{'.");

        List<Stmt.FunctionDef> methods = new ArrayList<>();
        String kind = "method";
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            // staticMethod -> "class" function ;
            // function -> IDENTIFIER "(" parameters? ")" blockStmt ;
            if (match(CLASS)) kind = "static_method";
            methods.add((Stmt.FunctionDef)functionDef(kind));
            kind = "method";
        }

        consume(RIGHT_BRACE, "Expected a '}'");
        return new Stmt.Class(name, superClasses, methods);
    }

    private Stmt functionDef(String kind) {
        // funDef   -> "fun" function ;
        // function ->  IDENTIFIER "(" parameters? ")" blockStmt ;
        // getterMethod -> IDENTIFIER blockStmt ;
        Boolean isStaticMethod = kind.equals("static_method");
        Boolean isGetterMethod = false;
        Token name = consume(IDENTIFIER, "Expect a " + kind + " name.");
        List<Token> params = new ArrayList<>();
        if (match(LEFT_PAREN)) {
            // consume(LEFT_PAREN, "Expected a '(' after " + kind + " name.");
            if (!check(RIGHT_PAREN)) {
                do {
                    if (params.size() >= 255) {
                        error(peek(), "Cannot have greater than 255 parameters in " + kind);
                    }
                    params.add(consume(IDENTIFIER, "Expect a parameter name."));
                } while (match(COMMA));
            }
            consume(RIGHT_PAREN, "Expected a ')'.");
        } else {
            isGetterMethod = true;
        }
        consume(LEFT_BRACE, "Expected a '{'.");
        
        Stmt body = blockStmt();

        return new Stmt.FunctionDef(name, params, body, isStaticMethod, isGetterMethod);
    }

    private Stmt statement() {
        // statement -> printStmt | branchStmt | iterStmt | exprStmt | blockStmt ;
        if (match(PRINT)) return printStmt();
        if (match(IF)) return branchStmt();
        if (match(WHILE)) return whileStmt();
        if (match(FOR)) return forStmt();
        if (match(BREAK)) return breakStmt();
        if (match(CONTINUE)) return continueStmt();
        if (match(RETURN)) return returnStmt();
        if (match(LEFT_BRACE)) return blockStmt();

        return exprStmt();
    }

    private Stmt printStmt() {
        // printStmt -> "print" expression ";" ;
        Expr value = expression();
        consume(SEMICOLON, "expected a ';' after value");

        return new Stmt.Print(value);
    }

    private Stmt exprStmt() {
        // exprStmt -> expression ";" ;
        Expr expression = expression();
        consume(SEMICOLON, "expected a ';' after expresssion");

        return new Stmt.Expression(expression);
    }

    private Stmt branchStmt() {
        // branchStmt   -> ifStmt | ifElseStmt ;
        // ifStmt       -> "if" "(" expression ")" statement ;
        // ifElseStmt   -> "if" "(" expression ")" statement "else" statement ;
        consume(LEFT_PAREN, "expected a '(' after 'if'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "expected a ')' after condition.");
        // consume(LEFT_BRACE, "expected a '{'");
        Stmt thenStmt = statement();
        Stmt elseStmt = null;
        if (match(ELSE)) {
            // consume(LEFT_BRACE, "expected a '{'");
            elseStmt = statement();
        }

        return new Stmt.If(condition, thenStmt, elseStmt);
    }

    private Stmt whileStmt() {
        // iterStmt -> "while" "(" expression ")" statement ;
        consume(LEFT_PAREN, "expected a '(' after 'if'");
        Expr expression = expression();
        consume(RIGHT_PAREN, "expected a ')' after condition");
        Stmt body = statement();

        return new Stmt.While(expression, body);
    }

    private Stmt forStmt() {
        // forStmt -> "for" "(" (varStmt | exprStmt)? ";" expression? ";" expression? ")" statement ;
        consume(LEFT_PAREN, "expected a '(' after 'if'");

        // (varStmt | exprStmt)? ";"
        Stmt initialization;
        if (match(VAR)) {
            initialization = varDeclaration();
        } else if (match(SEMICOLON)) {
            initialization = null;
        } else {
            initialization = exprStmt();
        }

        // expression? ";"
        Expr condition;
        if (match(SEMICOLON)) {
            condition = null;
        } else {
            condition = expression();
            consume(SEMICOLON, "Expect ';' in for statement.");
        }

        // exprStmt?
        Stmt update;
        if (match(RIGHT_PAREN)) {
            update = null;
        } else {
            // No semicolon here
            Expr expr = expression();
            update = new Stmt.Expression(expr);
            consume(RIGHT_PAREN, "expected a ')' after condition");
        }
        
        // blockStmt
        Stmt body = statement();

        /// DESUGARING ('for' can be expressed as while)

            /**
             * Reason for not desugaring:
             *      Right now, we're desugaring by pushing the update to the end of the
             *      while body. This is usually fine... EXCEPT our current 'continue' implementation
             *      then skips the update statement (since in a while loop, there's no requirement to perform an update step).
             * 
             *      TLDR; We'd be (i) checking the condition, (ii) executing the body, (iii) if we hit a continue, we skip rest of body and check condition
             *             *** this presents and issue if we're counting on an update step being executed.
             *      
             *      In the meantime, to avoid hackiness we're keeping forStmt and whileStmt as 2 separate nodes in AST.
             */
            
//        if (update != null) {
//            body = new Stmt.Block(
//                Arrays.asList(
//                    body,
//                    update
//                )
//            );
//        }
//
//        if (condition == null) condition = new Expr.Literal(true);
//        body = new Stmt.While(condition, body);
//
//        if (initialization != null) {
//            body = new Stmt.Block(
//                Arrays.asList(
//                    initialization,
//                    body
//                )
//            );
//        }
//        
//        return body;
        return new Stmt.For(initialization, condition, update, body);
    }

    private Block blockStmt() {
        // blockStmt -> "{" declaration* "}" ;
        List<Stmt> statements = new ArrayList<Stmt>();

        while (!isAtEnd() && !check(RIGHT_BRACE)) {
            statements.add(declaration());
        }

        consume(RIGHT_BRACE, "expecting a '}' after block.");

        return new Stmt.Block(statements);
    }

    private Stmt breakStmt() {
        // break -> "break" ";" ;
        Token keyword = previous();
        consume(SEMICOLON, "Expect ';' after break.");
        return new Stmt.Break(keyword);
    }

    private Stmt continueStmt() {
        // continue -> "continue" ";" ;
        Token keyword = previous();
        consume(SEMICOLON, "Expect a ';' after continue.");
        return new Stmt.Continue(keyword);
    }

    private Stmt returnStmt() {
        // return -> "return" expression ";" ;
        Token keyword = previous();
        Expr value = null;

        if (!check(SEMICOLON)) { 
            value = expression();
        }
        consume(SEMICOLON, "Expect a ';'.");
        return new Stmt.Return(keyword, value);
    }

    //=========================
    // Expressions
    //=========================
    private Expr expression() {
        // expression -> comma ;
        return comma();
    }

    private Expr comma() {
        // comma -> assign ("," assign)* ;
        Expr expr = assign();
        while (match(COMMA)) {
            Token operator = previous();
            Expr right = assign();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr assign() {
        // assign -> IDENTIFIER "=" assign | ternary ;

        // if (match(IDENTIFIER)) {
        //     Token name = previous();
        //     if (match(EQUAL)) {
        //         Expr rhs = expression();
        //         return new Expr.Assign(name, rhs);
        //     } else {
        //         // Not an assignment, so rewind to before the identifier and parse as ternary
        //         rewind();
        //         return ternary();
        //     }
        // }

        // Using below impl to account for thing.next_thing = value;
        Expr expr = ternary();

        if (match(EQUAL)) {
            Token equals = previous();
            Expr rhs = assign();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, rhs);
            } else if (expr instanceof Expr.Get) {
                Expr.Get get = (Expr.Get)expr;
                return new Expr.Set(get.object, get.name, rhs);
            }

            error(equals, "Invalid assignment target.");
        }

        return expr;
    }

    private Expr ternary() {
        // ternary -> logical_or ("?" ternary ":" ternary)? ;
        Expr expr = logical_or();
        if (match(QUESTION_MARK)) {
            Token operator = previous();
            Expr trueExpr = ternary();
            consume(COLON, "expected a ':' in ternary expression");
            Expr falseExpr = ternary();
            expr = new Expr.Ternary(expr, operator, trueExpr, falseExpr);
        }

        return expr;
    }

    private Expr logical_or() {
        // logical -> logical_and ("or" logical_and)* ;
        Expr expr = logical_and();
        while (match(OR)) {
            Token operator = previous();
            Expr right = logical_and();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr logical_and() {
        // logical -> equality ("and" equality)* ;
        Expr expr = equality();
        while (match(AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr equality() {
        // equality -> bitwise ( ("==" | "!=") bitwise )* ;
        Expr expr = bitwise();
        while (match(EQUAL_EQUAL, BANG_EQUAL)) {
            Token operator = previous();
            Expr right = bitwise();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr bitwise() {
        // bitwise -> comparison ( ("&" | "|" | "^") comparison)* ;
        Expr expr = comparison();
        while (match(BITWISE_AND, BITWISE_OR, BITWISE_XOR)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr comparison() {
        // comparison -> bitshift ( (">" | "<" | ">=" | "<=") bitshift )* ;
        Expr expr = bitshift();
        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = bitshift();
            expr =  new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr bitshift() {
        // bitshift -> term ( ("<<" | ">>") term)* ;
        Expr expr = term();
        while (match(BITSHIFT_LEFT, BITSHIFT_RIGHT)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr term() {
        // term -> factor ( ("+" | "-") factor )* ;
        Expr expr = factor();
        while (match(PLUS, MINUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr factor() {
        // factor -> unary ( ("*" | "/" | "%") unary )* ;
        Expr expr = unary();
        while (match(STAR, SLASH, MODULO)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr unary() {
        // unary -> ("!" | "-" | "~") unary | exp ;
        if (match(BANG, MINUS, BITWISE_NOT)) {
            Token operator = previous();
            Expr expr = unary();
            return new Expr.Unary(operator, expr);
        }

        return exp();
    }

    private Expr exp() {
        // exp -> prefix ( "**" unary )* ;
        Expr expr = prefix();
        if (match(STAR_STAR)) {
            Token operator = previous();
            Expr exponent = unary();
            return new Expr.Binary(expr, operator, exponent);
        }

        return expr;
    }

    private Expr prefix() {
        // prefix -> ("++" | "--") IDENTIFIER | index ;
        if (match(MINUS_MINUS, PLUS_PLUS)) {
            Token operator = previous();
            Expr expr = index();

            if (!(expr instanceof Expr.Variable)) {
                error(operator, "Invalid prefix expression.");
            }
            
            Token name = ((Expr.Variable)expr).name;
            return new Expr.Prefix(operator, name);
        }

        return index();
    }

    private Expr index() {
        // index -> postfix ("[" expression "]")* ;
        // index -> postfix ("[" expression (":" expression)? "]")* ;
        Expr object = postfix();
        while (match(LEFT_BRACK)) {
            Token lbrack = previous();
            Expr idxExpr = expression();
            Expr idxExpr2 = null;
            if (match(COLON)) idxExpr2 = expression();
            consume(RIGHT_BRACK, "Expected a ']'.");
            return new Expr.Index(lbrack, object, idxExpr, idxExpr2);
        }

        return object;
    }

    private Expr postfix() {
        // postfix -> IDENTIFIER ("++" | "--") | call ;
        Expr expr = call();

        if (match(MINUS_MINUS, PLUS_PLUS)) {
            Token operator = previous();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Postfix(name, operator);
            }

            error(operator, "Invalid postfix expression.");
        }

        return expr;
        
    }

    private Expr call() {
        // call -> primary ( "(" arguments? ")" | "." IDENTIFIER )* ;
        Expr expr = primary();
        while (true) {
            if (match(LEFT_PAREN)) {
                expr = finishCall(expr);
            } else if (match(DOT)) {
                Token identifier = consume(IDENTIFIER, "Expected an identifier.");
                expr = new Expr.Get(expr, identifier);
            } else {
                break;
            }
        }

        return expr;
    }

    private Expr finishCall(Expr callee) {
        // arguments -> assign ( "," assign )* ;


        // List<Expr> args = new ArrayList<>();
        // if (match(RIGHT_PAREN)) {
        //     Token paren = previous();
        //     return new Expr.Call(callee, paren args);
        // } else {
        //     do {
        //         Expr arg = assign();
        //         args.add(arg);
        //     } while (match(COMMA));
        //     Token paren = consume(RIGHT_PAREN, "Expecting a ')'.");

        //     return new Expr.Call(callee, paren args);
        // }

        List<Expr> args = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (args.size() >= 255) {
                    error(peek(), "Cannot have 255 or more arguments.");
                }
                // HACK - avoid comma() ambiguity
                args.add(assign());
            } while (match(COMMA));
        }

        Token paren = consume(RIGHT_PAREN, "Expecting a ')'.");

        return new Expr.Call(callee, paren, args);
    }

    private Expr primary() {
        // primary -> NUMBER | STRING | "true" | "false" | "nil" | "this" | "super" "." IDENTIFIER | "(" expression ")" | IDENTIFIER | error ;
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);
        if (match(NUMBER, STRING)) return new Expr.Literal(previous().literal);
        if (match(IDENTIFIER)) return new Expr.Variable(previous());
        if (match(FUN)) return anonymousFun();
        if (match(THIS)) return new Expr.This(previous());
        if (match(SUPER)) {
            Token keyword = previous();
            consume(DOT, "Expected a '.' after super keyword.");
            Token property = consume(IDENTIFIER, "Expected an identifier.");
            return new Expr.Super(keyword, property);
        }

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expected a ')' after expression.");
            return new Expr.Grouping(expr);
        }

        if (match(LEFT_BRACK)) {
            return list();
        }

        return error();
    }

    private Expr list() {
        // list -> "[" ( expression ( "," expression )*)? "]";
        List<Expr> listExpr = new ArrayList<>();
        Expr element = null;
        do {
            if (listExpr.size() >= Integer.MAX_VALUE) {
                error(peek(), "List is way to big...\nIOW, you exceeded the size limit buddy");
            }
            if (check(RIGHT_BRACK)) break;
            element = assign(); // HACK - avoid comma ambiguity
            listExpr.add(element);
        } while (match(COMMA));
        consume(RIGHT_BRACK, "Expected a ']'.");

        return new Expr.ListExpr(listExpr);
    }

    private Expr anonymousFun() {
        // anonymous -> "fun (" parameters? ")" blockStmt 
        consume(LEFT_PAREN, "Expected a '(' after function name.");
        List<Token> params = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (params.size() >= 255) {
                    error(peek(), "Cannot have greater than 255 parameters in function");
                }
                params.add(consume(IDENTIFIER, "Expect a parameter name."));
            } while (match(COMMA));
        }
        consume(RIGHT_PAREN, "Expected a ')'.");
        consume(LEFT_BRACE, "Expected a ')'.");

        Stmt body = blockStmt();

        return new Expr.Anonymous(params, body);
    }

    private Expr error() {
        // error -> ( ("==" | "!=" | ">" | "<" | ">=" | "<=" | "+" | "-" | "*" | "/") expression ) ;
        if (match(EQUAL_EQUAL, BANG_EQUAL, GREATER,
                 LESS, GREATER_EQUAL, LESS_EQUAL,
                 PLUS, MINUS, STAR, SLASH)) {
            Token operator = previous();
            Expr right = expression();

            throw error(operator, "Missing LHS operand");
        }

        throw error(peek(), "Expect expression.");
    }

    /* Helper Methods */
    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    private boolean isAtStart() {
        return curr == 0;
    }    

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private Token peek() {
        return tokens.get(curr);
    }
    
    private Token peekNext() {
        if (tokens.get(curr).type == EOF || tokens.get(curr + 1).type == EOF)
            return null;
        return tokens.get(curr + 1);
    }

    private Token previous() {
        return tokens.get(curr - 1);
    }

    private Token advance() {
        if (!isAtEnd()) curr++;
        return previous();
    }

    private void rewind() {
        curr--;
    }

    private Token consume(TokenType type, String errmsg) {
        if (check(type)) return advance();

        throw error(peek(), errmsg);
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return;

            switch (peek().type) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }

            advance();
        }
    }
}
