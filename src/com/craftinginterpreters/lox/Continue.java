package com.craftinginterpreters.lox;

// Special exception class to signal a continue statement
class Continue extends RuntimeException {
    // Using a singleton pattern to avoid creating multiple instances
    private static final Continue instance = new Continue();

    private Continue() {
        // Private constructor to enforce singleton pattern
    }

    static Continue instance() {
        return instance;
    }
}
