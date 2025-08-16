package com.craftinginterpreters.lox;

import java.util.List;
import java.util.Map;

public class LoxClass extends LoxInstance implements LoxCallable {
    private final String name;
    private final Map<String, LoxFunction> methods;
    public final List<LoxClass> superClasses;
    private Integer arity;

    // Lox class definition
    LoxClass(String name, List<LoxClass> superClasses, Map<String, LoxFunction> methods) {
        super(null);
        this.name = name;
        this.superClasses = superClasses;
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
        if (!superClasses.isEmpty()) {
            for (LoxClass superClass : superClasses) {
                LoxFunction method = superClass.findMethod(name);
                if (method != null) return method;
            }
        }
        return null;
    }
    
    public LoxInstance instantiate() {
        return new LoxInstance(this);
    }

    @Override
    public String toString() {
        return "<class: " + name + ">";
    }

    public Boolean hasSuper() {
        return !this.superClasses.isEmpty();
    }
}
