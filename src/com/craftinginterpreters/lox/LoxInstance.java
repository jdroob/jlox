package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

public class LoxInstance {
    private final LoxClass klass;
    private final Map<String, Object> fields = new HashMap<>();
    LoxInstance(LoxClass klass) {
        this.klass = klass;
    }

    @Override
    public String toString() {
        return "<class instance: @" + Integer.toHexString(System.identityHashCode(this)) + ">";
    }

    public Object get(Token name) {
        if (fields.containsKey(name.lexeme)) {
            return fields.get(name.lexeme);
        }

        LoxFunction method = klass.findMethod(name.lexeme);
        if (method != null) return method;
        
        throw new RuntimeError(name,
            "Undefined property " + name.lexeme + ".");
    }

    public Object set(Token name, Object rhsVal) {
        fields.put(name.lexeme, rhsVal);
        
        return rhsVal;
    }
}
