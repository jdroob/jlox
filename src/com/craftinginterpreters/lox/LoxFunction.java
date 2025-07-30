package com.craftinginterpreters.lox;

import java.util.List;

public class LoxFunction implements LoxCallable {
    private final Stmt.FunctionDef funcDef;
    private final Environment closure;
    private Boolean isInitializer;
    public Boolean isGetter;
    public Boolean isStatic;

    // Ordinary function definition
    LoxFunction(Stmt.FunctionDef funcDef, Environment closure, Boolean isInit, Boolean isStatic, Boolean isGetter) {
        this.funcDef = funcDef;
        this.closure = closure;
        this.isInitializer = isInit;
        this.isGetter = isGetter;
        this.isStatic = isStatic;
    }
    
    // Anonymous function definition
    LoxFunction(List<Token> params, Stmt body, Environment closure, Boolean isInit) {
        this.funcDef = new Stmt.FunctionDef(new Token(TokenType.IDENTIFIER, "anon", null, 0), params, body, false, false);
        this.closure = closure;
        this.isInitializer = isInit;
        this.isGetter = false;
        this.isStatic = false;
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
            if (isInitializer) return closure.getAt(new Token(TokenType.IDENTIFIER, "this", null, 0), 0);
            return r.returnValue;
        }
        if (isInitializer) return closure.getAt(new Token(TokenType.IDENTIFIER, "this", null, 0), 0);
        return null;
    }

    @Override
    public String toString() {
        return "<fn: " + funcDef.name.lexeme + ">";
    }

    public LoxFunction bind(LoxInstance instance) {
        Environment environment = new Environment(closure);
        environment.define("this", instance);
        return new LoxFunction(funcDef, environment, isInitializer, isStatic, isGetter);
    }

    // public LoxFunction bind(LoxClass klass) {
    //     Environment environment = new Environment(closure);
    //     environment.define("this", klass);
    //     return new LoxFunction(funcDef, environment, isInitializer);
    // }

}