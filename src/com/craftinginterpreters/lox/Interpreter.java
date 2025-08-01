package com.craftinginterpreters.lox;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Interpreter implements Expr.ExprVisitor<Object>, Stmt.StmtVisitor<Void> {
    final Environment globals = new Environment();  // always refers to inner-most scope
    private Environment env = globals;      // env can change
    private HashMap<Expr, Integer> locals = new HashMap<>();

    public Interpreter() {
        /**\
         * What?
         *  Define a 'clock' function to be used in benchmarking
         *  e.g. 
         *      var start = clock();
         *      run_expensive_fn();
         *      var finish = clock();
         *      var TAT = finish - start;
         * 
         * How?
         *  Add a 'clock' name to the global namespace.
         *  Associate 'clock' with an anonymous Java class
         *  that implements the LoxCallable interface.
         *  This anaonymous class sets arity to 0 (since clock() takes 0 args).
         *  call is implemented via Java's currentTimeMillis system method.
         */
        globals.define("clock", new LoxCallable() {
            @Override
            public int arity() { return 0; }

            @Override
            public Object call(Interpreter interpreter, List<Object> args) {
                return (double)System.currentTimeMillis() / 1000.0;
            }

            @Override
            public String toString() {
                return "<native fn: clock>";
            }
        });

        globals.define("input", new LoxCallable() {
            @Override
            public int arity() { return 1; }

            @Override
            public Object call(Interpreter interpreter, List<Object> args) {
                StringBuilder sb = new StringBuilder();
                try {
                    // Print user-provided message
                    String msg = (String)args.get(0);
                    System.out.print(msg);

                    // Read all available input from stdin until EOF or newline
                    int ch;
                    while ((ch = System.in.read()) != -1 && (char)ch != '\n') {
                        sb.append((char) ch);
                    }
                } catch (java.io.IOException e) {
                    throw new RuntimeException("Error reading from stdin", e);
                }
                return sb.toString();
            }

            @Override
            public String toString() {
                return "<native fn: input>";
            }
        });

        globals.define("print", new LoxCallable() {
            @Override
            public int arity() { return 1; }

            @Override
            public Object call(Interpreter interpreter, List<Object> args) {
                try {
                    // User-provided input to print to stdout
                    String rawInput = (String)args.get(0);
                    System.out.println(rawInput);
                } catch (NumberFormatException e) {
                    throw new RuntimeError(null, "Cannot convert input to number: '" + args.get(0) + "'");
                }
                return null;
            }

            @Override
            public String toString() {
                return "<native fn: print>";
            }
        });

        globals.define("num", new LoxCallable() {
            @Override
            public int arity() { return 1; }

            @Override
            public Object call(Interpreter interpreter, List<Object> args) {
                double inputAsNum;
                // User-provided input
                Object arg = args.get(0);
                try {
                    if (arg instanceof String) {
                        String rawInput = (String)args.get(0);
                        inputAsNum = Double.parseDouble(rawInput);
                    } else if (arg instanceof Number) {
                        inputAsNum = ((Number) arg).doubleValue();
                    } else {
                        throw new RuntimeError(null, "Invalid input type: '" + arg + "'");
                    }
                } catch (NumberFormatException e) {
                    throw new RuntimeError(null, "Cannot convert input to number: '" + args.get(0) + "'");
                }
                return inputAsNum;
            }

            @Override
            public String toString() {
                return "<native fn: num>";
            }
        });

        // TODO: Add native functions to implement file I/O
    }

    // For DEBUG
    public void interpretExpression(Expr expr) {
        try {
            Object value = evaluate(expr);
            if (value instanceof String || value instanceof Character) {
                value = "'" + (String)value + "'";
            }
            System.out.println(stringify(value));
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }
    
    public void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                try {
                    execute(statement);
                } catch (Break | Continue | Return e) {  // Can only get here if break or continue used outside of for / while
                    // dummy token for error reporting
                    TokenType type = e instanceof Break ? TokenType.BREAK : e instanceof Continue ? TokenType.CONTINUE : TokenType.RETURN;
                    String lexeme = type == TokenType.BREAK ? "break" : type == TokenType.CONTINUE ? "continue" : "return";
                    Token dummyToken = new Token(type, lexeme, null, 0);
                    
                    Lox.runtimeError(
                        new RuntimeError(
                            dummyToken, 
                            e instanceof Break ? 
                            "break statement outside of loop." :
                            e instanceof Continue ?
                            "continue statement outside of loop." : 
                            "return statement outside of function."
                        )
                    );
                    break;
                }
            }
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    public void resolve(Expr expr, Integer depth) {
        if (!locals.containsKey(expr)) locals.put(expr, depth);
    }

    private Object lookUpVariable(Token name, Expr expr) {
        Integer hops = locals.get(expr);
        if (hops == null) {
            return globals.get(name);
        } else {
            return env.getAt(name, hops);
        }
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression exprStmt) {
        evaluate(exprStmt.expression);

        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print printStmt) {
        Object value = evaluate(printStmt.expression);
        System.out.println(stringify(value));

        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If ifStmt) {
        if (getTruthiness(evaluate(ifStmt.condition))) {
            execute(ifStmt.thenStmt); 
        } else if (ifStmt.elseStmt != null) {
            execute(ifStmt.elseStmt); 
        }

        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While whileStmt) {
        try {
            while (getTruthiness(evaluate(whileStmt.condition))) {
                try {
                    execute(whileStmt.body);
                } catch (Continue ignored) {
                    // When continue is encountered, we just skip to the next iteration
                    // by breaking out of the inner try and continuing the outer loop
                    continue;
                }
            }
        } catch (Break ignored) {
            // When break is encountered, we just exit the loop completely
        }
        
        return null;
    }

    @Override
    public Void visitForStmt(Stmt.For forStmt) {
        if (forStmt.initialization != null) {
            execute(forStmt.initialization);
        }

        try {
            if (forStmt.condition != null) {
                // Execute body, execute update, repeat while condition is true
                while (getTruthiness(evaluate(forStmt.condition))) {
                    try {
                        execute(forStmt.body);
                    } catch (Continue ignored) {
                        // When continue is encountered, skip to the update step
                    }
                    
                    if (forStmt.update != null) {
                        execute(forStmt.update);
                    }
                }
            } else {
                // Infinite loop case (no condition)
                while (true) {
                    try {
                        execute(forStmt.body);
                    } catch (Continue ignored) {
                        // When continue is encountered, skip to the update step
                    }
                    
                    if (forStmt.update != null) {
                        execute(forStmt.update);
                    }
                }
            }
        } catch (Break ignored) {
            // When break is encountered, we just exit the loop completely
        }

        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var varStmt) {
        /* 
         * This function handles variable declarations.
         * Variables with initializers are fully defined and initialized.
         * Variables without initializers are declared but marked as uninitialized.
         * Attempting to use an uninitialized variable will cause a runtime error.
         */
        Object value = null;

        /***
         * DESIGN NOTE:
         * 
         * IN C, the program:
         * 
         * int main(void) {
         *  int x = 1;
         *  {
         *      int x = x + 2;  // declares x FIRST. THEN evaluates initializer expression.
         *  }
         * }
         * 
         * Personally, I believe it makes more sense to evaluate the initializer first, then declare
         * the new identifier... but if we want to align with C users' expectations, we'd need
         * to swap the evaluation and the definition below.
         * 
         * NOTE: clang++ does the "evaluate then define" method like Lox :)
         * 
         * UPDATE: Lox no longer allows cases like the above. Specifically, it does not allow
         *         a var statement's initializer to refer to the variable being defined.
         *         Even if the user's intent was to initialize a shadowing var with the global version,
         *         there are better ways to do this and we believe code like this is usually a mistake.
         * 
        */
        if (varStmt.initializer != null) {
            value = evaluate(varStmt.initializer);
        }
        env.define(varStmt.name.lexeme, value);

        return null;
    }

    @Override
    public Void visitBlockStmt(Stmt.Block block) {
        return executeBlockStmt(block, new Environment(this.env));
    }

    public Void executeBlockStmt(Stmt.Block block, Environment env) {
        Environment enclosing = this.env;    // Set parent to current environment
        try {
            this.env = env;   // set current environment to newly alloc'd Environment (more "innner" scope)
            for (Stmt stmt : block.statements) {
                execute(stmt);
            }
        } catch (Break | Continue e) {
            // Re-throw break and continue exceptions so they propagate up to the enclosing loop
            this.env = enclosing;    // Make sure we restore the environment
            throw e;
        } finally {
            // restore env even if exception is thrown
            this.env = enclosing;    // Reset env to previous scope
        }
        return null;
    }
    
    // Overloaded version for function bodies
    public Void executeBlockStmt(Stmt.Block block, Environment env, boolean inFunction) {
        Environment enclosing = this.env;    // Set parent to current environment
        try {
            this.env = env;   // set current environment to newly alloc'd Environment (more "innner" scope)
            for (Stmt stmt : block.statements) {
                execute(stmt);
            }
        } catch (Break | Continue e) {
            this.env = enclosing;    // Make sure we restore the environment
            
            if (inFunction) {
                // In a function context, break/continue are errors
                TokenType type = e instanceof Break ? TokenType.BREAK : TokenType.CONTINUE;
                String lexeme = type == TokenType.BREAK ? "break" : "continue";
                Token dummyToken = new Token(type, lexeme, null, 0);
                
                throw new RuntimeError(
                    dummyToken, e instanceof Break ? 
                    "break statement outside of loop." : 
                    "continue statement outside of loop."
                );

            } else {
                // Re-throw for loops to handle
                throw e;
            }
        } finally {
            // restore env even if exception is thrown
            this.env = enclosing;    // Reset env to previous scope
        }
        return null;
    }

    @Override
    public Void visitBreakStmt(Stmt.Break breakStmt) {
        throw Break.instance();
    }

    @Override 
    public Void visitContinueStmt(Stmt.Continue continueStmt) {
        throw Continue.instance();
    }

    @Override
    public Void visitFunctionDefStmt(Stmt.FunctionDef funcDef) {
        if (env.contains(funcDef.name)) {
            env.update(funcDef.name, new LoxFunction(funcDef, this.env, false, false, false));
        } else {
            env.define(funcDef.name.lexeme, new LoxFunction(funcDef, this.env, false, false, false));
        }

        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return retStmt) {
        Object retValue = null;
        if (retStmt.value != null) retValue = evaluate(retStmt.value);

        throw new Return(retValue);
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        // Logical
        if (expr.operator.type == TokenType.AND) 
            return (boolean)(getTruthiness(evaluate(expr.left))) && 
                   (boolean)(getTruthiness(evaluate(expr.right)));
        if (expr.operator.type == TokenType.OR) 
            return (boolean)(getTruthiness(evaluate(expr.left))) || 
                   (boolean)(getTruthiness(evaluate(expr.right)));
        
        // Declare here so short-cirtuiting works correctly
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            // Arithmetic
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double)left + (double)right;
                }
                if (left instanceof String && right instanceof String) {
                    return (String)left + (String)right;
                }
                if (left instanceof String && right instanceof Double) {
                    String sright = canonicalizeNum(right.toString());
                    return (String)left + sright;
                }
                if (left instanceof Double && right instanceof String) {
                    String sleft = canonicalizeNum(left.toString());
                    return sleft + (String)right;
                }
                if (left instanceof String && right instanceof Integer) {
                    String sright = ((Integer)right).toString();
                    return (String)left + sright;
                }
                if (left instanceof Integer && right instanceof String) {
                    String sleft = ((Integer)left).toString();
                    return sleft + (String)right;
                }
                if (left instanceof Integer && right instanceof Double) {
                    return ((Integer)left).doubleValue() + (double)right;
                }
                if (left instanceof Double && right instanceof Integer) {
                    return (double)left + ((Integer)right).doubleValue();
                }
                if (left instanceof Integer && right instanceof Integer) {
                    return ((Integer)left).doubleValue() + ((Integer)right).doubleValue();
                }
                throw new RuntimeError(expr.operator, expr.operator + 
                    " operator only supports number and/or string types.");
            case MINUS: 
                checkNumberOperands(expr.operator, left, right);
                return (double)left - (double)right;
            case STAR: 
                checkNumberOperands(expr.operator, left, right);
                return (double)left * (double)right;
            case STAR_STAR: 
                checkNumberOperands(expr.operator, left, right);
                return Math.pow((double)left, (double)right);
            case SLASH: 
                checkNumberOperands(expr.operator, left, right);
                checkDivisor(expr.operator, right);
                return (double)left / (double)right;
            case MODULO: 
                checkNumberOperands(expr.operator, left, right);
                return (double)left % (double)right;

            // Bitwise
            case BITSHIFT_LEFT:
            case BITSHIFT_RIGHT:
            case BITWISE_AND:
            case BITWISE_OR:
            case BITWISE_XOR:
                checkBitwiseOperands(expr.operator, left, right);
                int ileft = (left instanceof Double) ? ((Double)left).intValue() : (int)left;
                int iright = (right instanceof Double) ? ((Double)right).intValue() : (int)right;
                if (expr.operator.type == TokenType.BITWISE_AND) return ileft & iright;
                if (expr.operator.type == TokenType.BITWISE_OR) return ileft | iright;
                if (expr.operator.type == TokenType.BITWISE_XOR) return ileft ^ iright;
                if (expr.operator.type == TokenType.BITSHIFT_LEFT) return ileft << iright;
                if (expr.operator.type == TokenType.BITSHIFT_RIGHT) return ileft >> iright;

            // Comparison
            case EQUAL_EQUAL: 
                checkTruthyOperands(expr.operator, left, right);
                return isEqual(left, right);
            case BANG_EQUAL: 
                checkTruthyOperands(expr.operator, left, right);
                return !isEqual(left, right);
            case GREATER: 
                checkNumberOperands(expr.operator, left, right);
                return (double)left > (double)right;
            case LESS: 
                checkNumberOperands(expr.operator, left, right);
                return (double)left < (double)right;
            case GREATER_EQUAL: 
                checkNumberOperands(expr.operator, left, right);
                return (double)left >= (double)right;
            case LESS_EQUAL: 
                checkNumberOperands(expr.operator, left, right);
                return (double)left <= (double)right;

            // Comma
            case COMMA: return right;
        }

        return null;
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);
        switch(expr.operator.type) {
            case BANG: 
                checkTruthyOperand(expr.operator, right);
                return !getTruthiness(right);
            case MINUS: 
                checkNumberOperand(expr.operator, right);
                return -(double)right;
            case BITWISE_NOT: 
                // checkNumberOperand(expr.operator, right);
                checkBitwiseOperand(expr.operator, right);
                return ~(((Double)right).intValue());
            // Prefix
            case PLUS_PLUS:
            case MINUS_MINUS:
                
            default: return null;
        }
    }

    @Override
    public Object visitPostfixExpr(Expr.Postfix postfix) {
        // Get value assoc'd with identifier
        Object origVal = lookUpVariable(postfix.name, postfix);

        // Confirm value is a number
        checkNumberOperand(postfix.operator, origVal);

        // Update value in storage location
        Integer hops = locals.get(postfix);
        if (hops == null) {     // TODO: Clean up this if-else
            if (postfix.operator.type == TokenType.PLUS_PLUS) {
                globals.update(postfix.name, ((Double)origVal) + 1);
            } else {
                globals.update(postfix.name, ((Double)origVal) - 1);
            }
        } else {
            if (postfix.operator.type == TokenType.PLUS_PLUS) {
                env.assignAt(postfix.name, hops, ((Double)origVal) + 1);
            } else {
                env.assignAt(postfix.name, hops, ((Double)origVal) - 1);
            }
        }

        // Return original value
        return origVal;
    }

    @Override
    public Object visitPrefixExpr(Expr.Prefix prefix) {
        // Get value assoc'd with identifier
        Object origVal = lookUpVariable(prefix.name, prefix);

        // Confirm value is a number
        checkNumberOperand(prefix.operator, origVal);

        // Update value in storage location
        double newVal;
        if (prefix.operator.type == TokenType.PLUS_PLUS) {
            newVal = ((Double)origVal) + 1;
        } else {
            newVal = ((Double)origVal) - 1;
        }
        Integer hops = locals.get(prefix);
        if (hops == null) {
            globals.update(prefix.name, newVal);
        } else {
            env.assignAt(prefix.name, hops, newVal);  
        } 

        // Return updated value
        return newVal;
    }

    @Override
    public Object visitTernaryExpr(Expr.Ternary expr) {
        Object condVal = evaluate(expr.cond);
        checkTruthyOperand(expr.operator, condVal);

        return getTruthiness(condVal) ? evaluate(expr.trueBranch) : evaluate(expr.falseBranch);
    }

    @Override
    public Object visitVariableExpr(Expr.Variable var) {
        return lookUpVariable(var.name, var);
    }

    @Override
    public Object visitAssignExpr(Expr.Assign assignment) {
        Object rhsVal = evaluate(assignment.rhs);
        // env.update(assignment.name, rhsVal);

        Integer hops = locals.get(assignment);
        if (hops == null) {
            globals.update(assignment.name, rhsVal);
        } else {
            env.assignAt(assignment.name, hops, rhsVal);
        }

        return rhsVal;
    }

    @Override
    public Void visitClassStmt(Stmt.Class classStmt) {
        Object superClass = null;
        if (classStmt.superClass != null) {
            superClass = evaluate(classStmt.superClass);
            if (!(superClass instanceof LoxClass)) {
                Lox.error(classStmt.superClass.name,
                          "Superclass must be a class.");
            }
        }
        env.define(classStmt.name.lexeme, null);
        Map<String, LoxFunction> methods = new HashMap<>();
        for (Stmt.FunctionDef method : classStmt.methods) {
            LoxFunction function = new LoxFunction(
                                                    method, 
                                                    this.env, 
                                                    method.name.lexeme.equals("init"), 
                                                    method.isStaticMethod, 
                                                    method.isGetterMethod
                                                   );
            methods.put(method.name.lexeme, function);
        }
        LoxClass klass = new LoxClass(classStmt.name.lexeme, (LoxClass)superClass, methods);
        env.update(classStmt.name, klass);
        return null;
    }

    @Override
    public Object visitIndexExpr(Expr.Index idx) {
        Object object = evaluate(idx.object);
        Object idxExpr = evaluate(idx.idxExpr);
        Object idxExpr2 = idx.idxExpr2 != null ? evaluate(idx.idxExpr2) : null;

        if (object instanceof String) {
            String str = (String)object;
            int start = toIntIndex(idxExpr, idx.lbrack, str.length());
            int end = idxExpr2 != null ? toIntIndex(idxExpr2, idx.lbrack, str.length()) : -1;

            if (end != -1) {
                if (start > end) {
                    throw new RuntimeError(idx.lbrack, "Start index cannot be greater than end index.");
                }
                return str.substring(start, end);
            } else {
                return Character.toString(str.charAt(start));
            }
        }

        // TODO: Add support for arrays when those are impl'd in Lox

        throw new RuntimeError(idx.lbrack, "Can only index strings (and arrays in the future).");
    }

    @Override
    public Object visitCallExpr(Expr.Call call) {
        Object callee = evaluate(call.callee);
        List<Object> args = new ArrayList<>();

        for (Expr arg : call.args) {
            args.add(evaluate(arg));
        }

        if (!(callee instanceof LoxCallable)) {
            throw new RuntimeError(call.paren,
                "Can only call functions or classes."
            );
        }

        LoxCallable callable = (LoxCallable)callee;
        if (args.size() != callable.arity()) {
            throw new RuntimeError(call.paren, "Expected " +
                callable.arity() + " arguments but received " +
                args.size() + " arguments.");
        }
        
        return callable.call(this, args);
    }

    @Override
    public Object visitAnonymousExpr(Expr.Anonymous anon) {
        return new LoxFunction(anon.params, anon.body, this.env, false);
    }

    @Override
    public Object visitGetExpr(Expr.Get getExpr) {
        Object object = evaluate(getExpr.object);
        if (object instanceof LoxInstance) {
            Object value = ((LoxInstance)object).get(getExpr.name);
            if (value instanceof LoxFunction) {
                LoxFunction method = (LoxFunction)value;
                if (method.isGetter) {
                    return method.call(this, new ArrayList<>());
                }
            }
            return value;
        }
        
        if (object instanceof LoxClass) {
            return ((LoxClass)object).get(getExpr.name);
        }
        
        throw new RuntimeError(getExpr.name,
            "Only instances have properties.");
    }

    @Override
    public Object visitSetExpr(Expr.Set setExpr) {
        Object object = evaluate(setExpr.object);
        if (object instanceof LoxInstance) {
            Object rhsVal = evaluate(setExpr.rhs);
            ((LoxInstance)object).set(setExpr.name, rhsVal);
            return rhsVal;
        }
        
        throw new RuntimeError(setExpr.name,
            "Properties can only be set for instances.");
    }

    @Override
    public Object visitThisExpr(Expr.This thisExpr) {
        return lookUpVariable(thisExpr.keyword, thisExpr);
    }

    @Override
    public Object visitSuperExpr(Expr.Super superExpr) {
        return lookUpVariable(superExpr.keyword, superExpr);
    }

    //==================
    // Helper methods
    //==================
    
    private Object evaluate(Expr expression) {
        return expression.accept(this);
    }

    private Void execute(Stmt statement) {
        // if (statement == null) return null;
        statement.accept(this);
        return null;
    }

    private int toIntIndex(Object idxVal, Token lbrack, int length) {
        int idx;
        if (idxVal instanceof Integer) {
            idx = (Integer) idxVal;
        } else if (idxVal instanceof Double) {
            idx = ((Double) idxVal).intValue();
        } else {
            throw new RuntimeError(lbrack, "String indices must be integers.");
        }
        if (idx < 0) {
            idx = (length + idx) % length;
            // throw new RuntimeError(lbrack, "String index out of bounds.");
        } else if (idx > length) {
            idx = length;
        }
        return idx;
    }

    private boolean getTruthiness(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean)object;
        if (object instanceof Double) return (double)object != 0;
        if (object instanceof String) return !((String)object).isEmpty();

        return true;
    }

    private boolean isTruthy(Object object) {
        return object instanceof Boolean      || 
               object instanceof String       || 
               object instanceof Double       ||
               object instanceof LoxFunction  ||
               object instanceof LoxClass     ||
               object instanceof LoxInstance  ||
               object == null;
    }
    
    private boolean isEqual(Object left, Object right) {
        if (left instanceof LoxFunction ||
            left instanceof LoxInstance ||
            left instanceof LoxClass) {
                left = System.identityHashCode(left);
                right = System.identityHashCode(right);
            }
        if (left == null && right == null) return true;
        if (left == null) return false;
        if (right == null) return false;

        return left.equals(right);
    }

    private void checkTruthyOperand(Token operator, Object object) {
        if (object instanceof Boolean || object instanceof Double  || object instanceof String) return;

        throw new RuntimeError(operator, "Operand must be a truthy type.");
    }

    private void checkTruthyOperands(Token operator, Object left, Object right) {
        if ((left instanceof Boolean && right instanceof Boolean)         ||
            (left instanceof Double && right instanceof Double)           ||
            (left instanceof String && right instanceof String)           || 
            (isTruthy(left) && right == null)                             ||
            (left == null && isTruthy(right))                             ||
            (left instanceof LoxInstance && right instanceof LoxInstance) ||
            (left instanceof LoxFunction && right instanceof LoxFunction) ||
            (left instanceof LoxClass && right instanceof LoxClass))
            return;

        throw new RuntimeError(operator, "Operands must be matching truthy types.");
    }

    private void checkNumberOperand(Token operator, Object object) {
        if (object instanceof Double) return;

        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;

        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    private void checkBitwiseOperands(Token operator, Object left, Object right) {
        if ((left instanceof Double || left instanceof Integer) &&
            (right instanceof Double || right instanceof Integer)) return;

        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    private void checkBitwiseOperand(Token operator, Object right) {
        if (right instanceof Double || right instanceof Integer) return;

        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private void checkDivisor(Token operator, Object divisor) {
        if ((Double)divisor == 0) {
            throw new RuntimeError(operator, "Division by 0 not allowed.");
        }
    }

    private String stringify(Object object) {
        if (object == null) return "nil";

        if (object instanceof Double) {
            String text = object.toString();
            return canonicalizeNum(text);
        }

        if (object instanceof String || object instanceof Character) {
            return object.toString();
        }

        return object.toString();
    }

    private String canonicalizeNum(String text) {
        if (text.endsWith(".0")) {
            text = text.substring(0, text.length() - 2);    // chop off ".0"
        }

        return text;
    }

    public static void main(String[] args) {
        /*
         * (1+2) * (4-2)
        */
        Expr expression = new Expr.Binary(
            new Expr.Grouping(
                new Expr.Binary(
                    new Expr.Literal(1.0),
                    new Token(TokenType.PLUS, "+", null, 1),
                    new Expr.Literal(2.0)
                )
            ),
            new Token(TokenType.STAR, "*", null, 1),
            new Expr.Grouping(
                new Expr.Binary(
                    new Expr.Literal(4.0),
                    new Token(TokenType.MINUS, "-", null, 1),
                    new Expr.Literal(2.0)
                )
            )
        );

        /*
         * (0/0) == (0/0)
        */
        Expr nanEquality = new Expr.Binary(
            new Expr.Binary(
                new Expr.Literal(0.0),
                new Token(TokenType.SLASH, "/", null, 1),
                new Expr.Literal(0.0)
            ),
            new Token(TokenType.EQUAL_EQUAL, "==", null, 1),
            new Expr.Binary(
                new Expr.Literal(0.0),
                new Token(TokenType.SLASH, "/", null, 1),
                new Expr.Literal(0.0)
            )
        );
        /*
         * 1 + 2, 3
         */
        Expr commaExpr = new Expr.Binary(
            new Expr.Binary(
                new Expr.Literal(1.0),
                new Token(TokenType.PLUS, "+", null, 1),
                new Expr.Literal(41.0)
            ),
            new Token(TokenType.COMMA, ",", null, 1),
            new Expr.Literal(69.0)
        );
        /*
         * !5
         */
        Expr unaryExpr = new Expr.Unary(
            new Token(TokenType.BANG, "!", null, 1), 
            new Expr.Literal("")
        );
        System.out.println(new Interpreter().evaluate(nanEquality));
        System.out.println(new Interpreter().evaluate(expression));
        System.out.println(new Interpreter().evaluate(commaExpr));
        System.out.println(new Interpreter().evaluate(unaryExpr));
    }
}
