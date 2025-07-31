package com.craftinginterpreters.lox;

import java.util.List;
import java.util.Map;

public class LoxClass extends LoxInstance implements LoxCallable {
    private final String name;
    private final Map<String, LoxFunction> methods;
    public final LoxClass superClass;
    private Integer arity;

    // Lox class definition
    LoxClass(String name, LoxClass superClass, Map<String, LoxFunction> methods) {
        super(null);
        this.name = name;
        this.superClass = superClass;
        this.methods = methods;
    }

    @Override
    public int arity() { 
        LoxFunction initMethod = findMethod("init");
        if (initMethod == null) {
            return 0;
            // System.out.println("DEBUG: setting " + this.toString() + " arity to " + this.arity);
        }
        return initMethod.arity();
     }

    @Override
    public Object call(Interpreter interpreter, List<Object> args) {
        try {
            LoxInstance instance = instantiate();
            LoxFunction initMethod = findMethod("init");
            if (initMethod != null) {   // run constructor
                findMethod("init").bind(instance).call(interpreter, args);
            }
            return instance;
        } catch (Return r) {
            return r.returnValue;
        }
    }

    public LoxFunction findMethod(String name) {
        if (methods.containsKey(name)) {
            return methods.get(name);
        }
        if (superClass != null) {
            return superClass.findMethod(name);
        }
        return null;
    }
    
    private LoxInstance instantiate() {
        return new LoxInstance(this);
    }

    @Override
    public String toString() {
        return "<class: " + name + ">";
    }

    public Boolean hasSuper() {
        return this.superClass != null;
    }
}
