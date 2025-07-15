package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

public class Environment {
    /**
     * Note: using String instead of Token for key b/c
     *       Tokens are used (in this case) to represent
     *       identifiers at specific locations in the program
     *       whereas the lexeme String is the name of the identifier
     *       each of these hypothetical Tokens refer to.
     *       Programmatically speaking, using a raw string ensures
     *       that all tokens refer to the same map key.
    */
    
    Environment() {
        this.enclosing = null;
    }

    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    private final Map<String, Map.Entry<Boolean, Object>> values = new HashMap<>();
    final Environment enclosing;

    /**
     * Define a variable in the current environment.
     * @param name The name of the variable
     * @param value The initial value (nil for uninitialized variables)
     */
    public void define(String name, Object value) {
        boolean is_initialized = value != null;
        values.put(name, new java.util.AbstractMap.SimpleEntry<>(is_initialized, value));
    }

    public Object get(Token name) {
        if (values.containsKey(name.lexeme)) {
            if (!values.get(name.lexeme).getKey()) {    // Check is_initialized field
                throw new RuntimeError(name, "Uninitialized variable: " 
                                + name.lexeme + ".");
            }
            return values.get(name.lexeme).getValue();
        }

        if (enclosing != null) {
            return enclosing.get(name); // recursively search outer scopes for name
        }

        throw new RuntimeError(name, "Undefined variable: " 
                                + name.lexeme + ".");
    }

    public Object getAt(Token name, Integer hops) {
        return ancestor(hops).get(name);
    }

    private Environment ancestor(Integer hops) {
        Environment curr = this;
        while (hops > 0) {
            curr = curr.enclosing;
            hops--;
        }
        return curr;
    }

    public void assignAt(Token name, Integer hops, Object value) {
        ancestor(hops).update(name, value);
    }

    public Object update(Token name, Object value) {
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, new java.util.AbstractMap.SimpleEntry<>(true, value));
            return null;
        }

        if (enclosing != null) {
            return enclosing.update(name, value);
        }

        throw new RuntimeError(name, "Undefined variable: " 
                                + name.lexeme + ".");
    }

    public boolean contains(Token name) {
        if (values.containsKey(name.lexeme)) {
            return true;
        }
        
        if (enclosing != null) {
            return enclosing.contains(name);
        }

        return false;
    }

    public Map<String, Map.Entry<Boolean, Object>> getEnv() {
        return values;
    }
}
