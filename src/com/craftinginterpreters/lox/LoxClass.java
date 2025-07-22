package com.craftinginterpreters.lox;

import java.util.List;
import java.util.Map;

public class LoxClass implements LoxCallable {
    private final String name;
    private final Map<String, LoxFunction> methods;
    private final Environment enclosing;
    private Integer arity;

    // Lox class definition
    LoxClass(String name, Map<String, LoxFunction> methods, Environment enclosing) {
        this.name = name;
        this.methods = methods;
        this.enclosing = enclosing;
        this.arity = 0;
    }
    
    // Anonymous function definition
    // LoxClass(List<Token> params, Stmt body, Environment closure) {
    //     this.funcDef = new Stmt.FunctionDef(new Token(TokenType.IDENTIFIER, "anon", null, 0), params, body);
    //     this.closure = closure;
    // }

    @Override
    public int arity() { return arity; }

    @Override
    public Object call(Interpreter interpreter, List<Object> args) {
        Environment env = new Environment(this.enclosing);

        try {
            // int N = this.arity();
            // for (int i = 0; i < N; i++) {
            //     env.define(funcDef.params.get(i).lexeme, args.get(i));
            // }
            // for (LoxFunction method : this.methods.values()) {
            //     if (method.name.lexeme.toLowerCase().equals(this.name.toLowerCase())) {
            //         interpreter.executeBlockStmt((Stmt.Block)method.body, env, true);
            //         break;
            //     }
            // }
            if (methods.containsKey(name)) {
                for (LoxFunction method : methods.values()) {
                    if (method.arity() == args.size()) {
                        method.call(interpreter, args);
                    }
                }
            }
            return instantiate();
        } catch (Return r) {
            return r.returnValue;
        }
    }

    public LoxFunction findMethod(String name) {
        if (methods.containsKey(name)) {
            return methods.get(name);
        }
        return null;
    }
    
    private Object instantiate() {
        return new LoxInstance(this);
    }

    @Override
    public String toString() {
        return "<class: " + name + ">";
    }

}
