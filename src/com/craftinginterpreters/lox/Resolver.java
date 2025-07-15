package com.craftinginterpreters.lox;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class Resolver implements Expr.ExprVisitor<Void>, Stmt.StmtVisitor<Void> {
    private enum FunctionType {
        NONE,
        FUNCTION
    }
    private enum LoopStatus {
        NONE,
        LOOP
    }
    private enum ObjectType {
        UNINITIALIZED,
        VARIABLE,
        FUNCTION
    }
    private class ResolverInfo {
        public Boolean isDefined;
        public Boolean isUsed;
        public ObjectType objectType;
        public Token name;
        public Integer index;

        ResolverInfo() {
            this.isDefined = false;
            this.isUsed = false;
            this.objectType = ObjectType.UNINITIALIZED;
            this.name = null;
            // this.index = 0;  // TODO: implement array-based Environments
        }

        ResolverInfo(Boolean isDefined, Boolean isUsed, ObjectType objectType, Token name) {
            this.isDefined = isDefined;
            this.isUsed = isUsed;
            this.objectType = objectType;
            this.name = name;
            // this.index = 0;
        }
    }
    private final Interpreter interpreter;
    private final Stack<Map<String, ResolverInfo>> scopes;
    private final Map<String, ResolverInfo> globals;
    FunctionType currentFunction = FunctionType.NONE;
    LoopStatus loopStatus = LoopStatus.NONE;

    Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
        this.scopes = new Stack<Map<String, ResolverInfo>>();
        this.globals = new HashMap<String, ResolverInfo>();
    }

    public void resolveExpression(Expr expression) {
        this.resolve(expression);
    }

    public void resolve(List<Stmt> statements) {
        for (Stmt stmt : statements) {
            resolve(stmt);
        }
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        beginScope();
        resolve(stmt.statements);
        endScope();

        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        declare(stmt.name, ObjectType.VARIABLE);
        if (stmt.initializer != null) {
            resolve(stmt.initializer);
        }
        define(stmt.name);

        return null;
    }

    @Override
    public Void visitVariableExpr(Expr.Variable expr) {
        // TODO: come back to this... globals line probs needs to go
        if (!scopes.isEmpty() &&
            // !globals.containsKey(expr.name.lexeme) &&
            scopes.peek().containsKey(expr.name.lexeme) &&
            scopes.peek().get(expr.name.lexeme).isDefined == Boolean.FALSE) {
            Lox.error(expr.name, "Cannot read local variable in its own initializer");
        }
        resolveLocal(expr, expr.name);
        markUsed(expr.name);

        return null;
    }

    @Override
    public Void visitAssignExpr(Expr.Assign expr) {
        resolve(expr.rhs);
        resolveLocal(expr, expr.name);

        return null;
    }

    @Override
    public Void visitFunctionDefStmt(Stmt.FunctionDef funcDef) {
        declare(funcDef.name, ObjectType.FUNCTION);
        define(funcDef.name);
        resolveFunctionDef(funcDef, FunctionType.FUNCTION);

        return null;
    }

    @Override
    public Void visitFunctionDeclStmt(Stmt.FunctionDecl funcDecl) {
        declare(funcDecl.name, ObjectType.FUNCTION);
        // define(funcDecl.name);
        resolveFunctionDecl(funcDecl);

        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        resolve(stmt.expression);

        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        resolve(stmt.condition);
        resolve(stmt.thenStmt);
        
        if (stmt.elseStmt != null) {
            resolve(stmt.elseStmt);
        }

        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        resolve(stmt.expression);

        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        if (currentFunction == FunctionType.NONE) {
            Lox.error(stmt.keyword,
                        "Cannot return from top-level code.");
        }
        if (stmt.value != null) {
            resolve(stmt.value);
        }

        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        LoopStatus enclosing = loopStatus;
        loopStatus = LoopStatus.LOOP;
        resolve(stmt.condition);
        resolve(stmt.body);
        loopStatus = enclosing;

        return null;
    }

    @Override 
    public Void visitForStmt(Stmt.For stmt) {
        LoopStatus enclosing = loopStatus;
        loopStatus = LoopStatus.LOOP;
        if (stmt.initialization != null) resolve(stmt.initialization);
        if (stmt.condition != null) resolve(stmt.condition);
        if (stmt.update != null) resolve(stmt.update);
        resolve(stmt.body);
        loopStatus = enclosing;

        return null;
    }

    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        if (loopStatus == LoopStatus.NONE) {
            Lox.error(stmt.keyword,
                "Cannot 'break' outside of loop");
        }
        return null;
    }

    @Override
    public Void visitContinueStmt(Stmt.Continue stmt) {
        if (loopStatus == LoopStatus.NONE) {
            Lox.error(stmt.keyword,
                    "Cannot 'continue' outside of loop");
        }
        return null;
    }

    @Override
    public Void visitBinaryExpr(Expr.Binary expr) {
        resolve(expr.left);
        resolve(expr.right);

        return null;
    }

    @Override
    public Void visitGroupingExpr(Expr.Grouping expr) {
        resolve(expr.expression);

        return null;
    }

    @Override
    public Void visitLiteralExpr(Expr.Literal expr) {
        return null;    // No variable usages in a literal...
    }

    @Override
    public Void visitCallExpr(Expr.Call expr) {
        resolve(expr.callee);
        for (Expr arg : expr.args) {
            resolve(arg);
        }

        return null;
    }

    @Override
    public Void visitUnaryExpr(Expr.Unary expr) {
        resolve(expr.right);

        return null;
    }

    @Override
    public Void visitTernaryExpr(Expr.Ternary expr) {
        resolve(expr.cond);
        resolve(expr.trueBranch);
        resolve(expr.falseBranch);

        return null;
    }

    @Override
    public Void visitIndexExpr(Expr.Index expr) {
        resolve(expr.object);
        resolve(expr.idxExpr);
        if (expr.idxExpr2 != null) resolve(expr.idxExpr2);

        return null;
    }

    @Override
    public Void visitPostfixExpr(Expr.Postfix expr) {
        resolveLocal(expr, expr.name);
        markUsed(expr.name);

        return null;
    }

    @Override
    public Void visitPrefixExpr(Expr.Prefix expr) {
        resolveLocal(expr, expr.name);
        markUsed(expr.name);

        return null;
    }

    @Override
    public Void visitAnonymousExpr(Expr.Anonymous expr) {
        /**
         * TODO: Eliminate duplicate code b/w this function & resolveFunctionDef
         */
        beginScope();
        for (Token param : expr.params) {
            declare(param, ObjectType.FUNCTION);
            define(param);
        }
        Stmt.Block funcBody = (Stmt.Block)expr.body;
        for (Stmt stmt : funcBody.statements) {
            resolve(stmt);
        }
        endScope();

        return null;
    }

    /** Helper methods */

    private void resolveFunctionDef(Stmt.FunctionDef funcDef, FunctionType type) {
        FunctionType enclosingFunction = currentFunction;
        currentFunction = type;
        beginScope();
        for (Token param : funcDef.params) {
            declare(param, ObjectType.VARIABLE);
            define(param);
        }
        Stmt.Block funcBody = (Stmt.Block)funcDef.body;
        for (Stmt stmt : funcBody.statements) {
            resolve(stmt);
        }
        endScope();
        currentFunction = enclosingFunction;
    }

    private void resolveFunctionDecl(Stmt.FunctionDecl funcDecl) {
        beginScope();
        for (Token param : funcDecl.params) {
            declare(param, ObjectType.VARIABLE);
            define(param);
        }
        endScope();
    }

    private void resolveLocal(Expr expr, Token name) {
        for (int i=scopes.size() - 1; i >= 0; --i) {
            if (scopes.get(i).containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size() - 1 - i);   // tell the interpreter where to find the correct name:value mapping (i.e. how many hops backwards from current scope should interpreter look?)
                return;
            }
        }
    }

    private void declare(Token name, ObjectType objectType) {
        if (scopes.isEmpty()) {
            globals.put(name.lexeme, new ResolverInfo(false, false, objectType, name));
            return;
        }
        Map<String, ResolverInfo> scope = scopes.peek();
        if (scope.containsKey(name.lexeme)) {
            Lox.error(name, "Already declared a variable with this name in this scope.");
        }
        scope.put(name.lexeme, new ResolverInfo(false, false, objectType, name));  // haven't finished resolving variable's initializer...
    }

    private void define(Token name) {
        if (scopes.isEmpty()) {
            globals.get(name.lexeme).isDefined = true;
            return;
        }
        scopes.peek().get(name.lexeme).isDefined = true;
    }

    private void markUsed(Token name) {
        if (scopes.isEmpty()) return;
        for (Map<String, ResolverInfo> scope : scopes) {
            if (scope.containsKey(name.lexeme)) {
                scope.get(name.lexeme).isUsed = true;
                break;
            }
        }
    }

    private void beginScope() {
        scopes.push(new HashMap<String, ResolverInfo>());
    }

    private void endScope() {
        for (String key : scopes.peek().keySet()) {
            if (scopes.peek().get(key).isUsed == false &&
                scopes.peek().get(key).objectType == ObjectType.VARIABLE) {
                Lox.warning(scopes.peek().get(key).name, "Unused variable.");
            }
        }
        scopes.pop();
    }

    private void resolve(Stmt stmt) {
        stmt.accept(this);
    }

    private void resolve(Expr expr) {
        expr.accept(this);
    }
}
