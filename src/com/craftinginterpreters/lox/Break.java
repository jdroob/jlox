package com.craftinginterpreters.lox;

// Special exception class to signal a break statement
class Break extends RuntimeException {
    // Using a singleton pattern to avoid creating multiple instances
    private static final Break instance = new Break();

    private Break() {
        // Private constructor to enforce singleton pattern
    }

    static Break instance() {
        return instance;
    }
}
