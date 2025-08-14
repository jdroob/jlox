package com.craftinginterpreters.lox;

// Special exception class to signal a return statement
class Return extends RuntimeException {

    public final Object returnValue;

    public Return(Object retVal) {
        super(null, null, false, false);
        this.returnValue = retVal;
    }
}
