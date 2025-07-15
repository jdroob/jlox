package com.craftinginterpreters.lox;

import java.util.List;

public class LoxFunction implements LoxCallable {
    private final Stmt.FunctionDef funcDef;
    private final Environment closure;

    // Ordinary function definition
    LoxFunction(Stmt.FunctionDef funcDef, Environment closure) {
        this.funcDef = funcDef;
        this.closure = closure;
    }
    
    // Anonymous function definition
    LoxFunction(List<Token> params, Stmt body, Environment closure) {
        this.funcDef = new Stmt.FunctionDef(new Token(TokenType.IDENTIFIER, "anon", null, 0), params, body);
        this.closure = closure;
    }

    @Override
    public int arity() { return funcDef.params.size(); }

    @Override
    public Object call(Interpreter interpreter, List<Object> args) {
        Environment env = new Environment(this.closure);

        try {
            int N = this.arity();
            for (int i = 0; i < N; i++) {
                env.define(funcDef.params.get(i).lexeme, args.get(i));
            }
            interpreter.executeBlockStmt((Stmt.Block)funcDef.body, env, true);
        } catch (Return r) {
            return r.returnValue;
        }
        return null;
    }

    @Override
    public String toString() {
        return "<fn: " + funcDef.name.lexeme + ">";
    }

}