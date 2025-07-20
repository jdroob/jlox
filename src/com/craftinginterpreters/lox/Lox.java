package com.craftinginterpreters.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {
    private static enum LOG_LEVEL {
        DEBUG,
        WARNING,
        ERROR
    }
    static boolean hadError = false;
    // static boolean hadRunFileError = false;
    // static boolean runningFile = false;
    static boolean hadRuntimeError = false;
    private static Scanner scanner = null;
    private static Parser parser = null;
    private static final Interpreter interpreter = new Interpreter();
    private static final Resolver resolver = new Resolver(interpreter);

    // Entry point
    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("usage: jlox <script>");
            System.exit(64);
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    private static void runFile(String path) throws IOException {
        /**
         * In C:
         * int fd = read(path, O_RDONLY);
         * OR
         * FILE *fh = fread(path, "rb");
         */
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        // runningFile = true;
        // System.out.println("Reading " + path);
        run(new String(bytes, Charset.defaultCharset()));

        // Indicate an error in exit code
        if (hadError) System.exit(65);
        if (hadRuntimeError) System.exit(70);
        return;
    }

    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in); // stdin
        BufferedReader reader = new BufferedReader(input);  // fread(stdin);

        for (;;) {
            System.out.print("> ");
            String line = reader.readLine();
            if (line == null) break;
            if (!line.endsWith(";") && !line.endsWith("}"))
                evalExpr(line);
            else
                run(line);
            hadError = false;
        }
        return;
    }

    private static void evalExpr(String source) throws IOException {
        // Scan baby :)
        scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();

        // FOR DEBUG
        // System.out.println("\nPRINTING TOKENS:\n");
        // for (Token token : tokens) {
        //     System.out.println(token);
        // }
        
        // Parse & interpret baby :)
        parser = new Parser(tokens);

        while (!parser.doneParsing()) {
            Expr expression = parser.parseExpression();
            if (hadError) return;
            // FOR DEBUG
            // System.out.println("~~~ EXPRESSION ~~~");
            // System.out.println(new TreePrinter().print(expression));
            // System.out.println(new AstPrinter().print(expression));
            // System.out.println("~~~~~~~~~~~~~~~~~~");
            // System.out.println("~~~ RESULT ~~~");
            resolver.resolveExpression(expression);
            interpreter.interpretExpression(expression);
            // System.out.println("~~~~~~~~~~~~~~~~~~");
        }
    }

    private static void run(String source) throws IOException {
        // Scan baby :)
        scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();

        // FOR DEBUG
        // System.out.println("\nPRINTING TOKENS:\n");
        // for (Token token : tokens) {
        //     System.out.println(token);
        // }
        
        // Parse & interpret baby :)
        parser = new Parser(tokens);
        // FOR DEBUG
        // while (!parser.doneParsing()) {
            //     Expr expression = parser.parseExpression();
            //     if (hadError) return;
        //     System.out.println("~~~ EXPRESSION ~~~");
        //     System.out.println(new TreePrinter().print(expression));
        //     System.out.println(new AstPrinter().print(expression));
        //     System.out.println("~~~~~~~~~~~~~~~~~~");
        //     System.out.println("~~~ RESULT ~~~");
        //     interpreter.interpretExpression(expression);
        //     System.out.println("~~~~~~~~~~~~~~~~~~");
        // }
        
        List<Stmt> statements = parser.parse();
        if (hadError) return;
        resolver.resolve(statements);
        if (hadError) return;
        // TODO: figure out why anyUnused isn't working
        // String unused = resolver.anyUnused();
        interpreter.interpret(statements);

    }

    static void error(int line, String msg) {
        report(line, "", msg, LOG_LEVEL.ERROR);
    }

    private static void report(int line, String where, String msg, LOG_LEVEL level) {
        String levelString = level == LOG_LEVEL.WARNING ? " Warning" : level == LOG_LEVEL.ERROR ? " Error" : " Debug error";
        System.err.println(
            "[line " + line + "]" + levelString + where + ": " + msg);
        if (level == LOG_LEVEL.ERROR) hadError = true;
    }

    static void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message, LOG_LEVEL.ERROR);
        } else {
            report(token.line, " at '" + token.lexeme + "'", message, LOG_LEVEL.ERROR);
        }
    }

    static void warning(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, "at end", message, LOG_LEVEL.WARNING);
        } else {
            report(token.line, " at '" + token.lexeme + "'", message, LOG_LEVEL.WARNING);
        }
    }

    static void runtimeError(RuntimeError error) {
        System.err.println(error.getMessage() +
            "\n[line " + error.token.line + "]");
        hadRuntimeError = true;
    }
}
