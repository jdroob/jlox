package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

public class LoxInstance {
    protected LoxClass klass;
    protected Map<String, Object> fields = new HashMap<>();
    LoxInstance(LoxClass klass) {
        this.klass = klass;
    }

    @Override
    public String toString() {
        return "<class instance: @" + Integer.toHexString(System.identityHashCode(this)) + ">";
    }

    public Object get(Token name) {
        Object field = getField(name);
        if (field != null) return field;

        Object method = getMethod(name);
        if (method != null) return method;

        throw new RuntimeError(name,
            "Undefined property " + name.lexeme + ".");
    }

    public Object set(Token name, Object rhsVal) {
        fields.put(name.lexeme, rhsVal);
        
        return rhsVal;
    }

    /* Helper methods */

    private Object getField(Token name) {
        // instance fields LUT
        if (fields.containsKey(name.lexeme)) {
            return fields.get(name.lexeme);
        }

        // class (static) fields LUT
        if (!(this instanceof LoxClass)) {
            if (klass.fields.containsKey(name.lexeme)) {
                return klass.fields.get(name.lexeme);
            }
        }

        return null;
    }

    private Object getMethod(Token name) {
        LoxFunction method = _getMethod(name);
        if (method != null) return method.bind(this);

        return null;
    }

    private LoxFunction _getMethod(Token name) {
        // methods LUT
        if (this instanceof LoxClass) {
            LoxFunction method = ((LoxClass)this).findMethod(name.lexeme);
            if (method != null) {
                if (method.isStatic) return method;
                throw new RuntimeError(name, 
                            "class object cannot access non-static method.");
            }
            return null;
        }
        return klass.findMethod(name.lexeme);
    }
}
