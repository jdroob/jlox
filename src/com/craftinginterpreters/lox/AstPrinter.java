package com.craftinginterpreters.lox;

public class AstPrinter implements Expr.ExprVisitor<String> {
    String print(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return parenthesize("group", expr.expression);
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        if (expr.value == null) return "nil";
        return expr.value.toString();
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return parenthesize(expr.operator.lexeme, expr.right);
    }

    @Override
    public String visitTernaryExpr(Expr.Ternary expr) {
        return parenthesize(expr.operator.lexeme, expr.cond, expr.trueBranch, expr.falseBranch);
    }

    @Override
    public String visitVariableExpr(Expr.Variable var) {
        return parenthesize(var.name.lexeme, new Expr.Literal(null));
    }

    @Override
    public String visitAssignExpr(Expr.Assign assignment) {
        return parenthesize(assignment.name.lexeme, assignment.rhs);
    }

    @Override
    public String visitIndexAssignExpr(Expr.IndexAssign obj) {
        return parenthesize(obj.object.toString(), obj.idxExpr);
    }

    
    @Override
    public String visitIndexPostfixExpr(Expr.IndexPostfix idx) {
        return parenthesize(idx.object.toString(), idx.idxExpr);
    }

    @Override
    public String visitIndexPrefixExpr(Expr.IndexPrefix idx) {
        return parenthesize(idx.object.toString(), idx.idxExpr);
    }

    @Override
    public String visitIndexExpr(Expr.Index idx) {
        return parenthesize(idx.object.toString(), idx.idxExpr);
    }

    @Override
    public String visitPostfixExpr(Expr.Postfix postfix) {
        return parenthesize(postfix.name.lexeme, postfix);
    }

    @Override
    public String visitPrefixExpr(Expr.Prefix prefix) {
        return parenthesize(prefix.name.lexeme, prefix);
    }

    @Override
    public String visitCallExpr(Expr.Call call) {
        return parenthesize(call.callee.toString(), call);
    }

    @Override
    public String visitAnonymousExpr(Expr.Anonymous anon) {
        return parenthesize("anon", anon);
    }

    @Override
    public String visitGetExpr(Expr.Get getExpr) {
        return parenthesize(getExpr.name.lexeme, getExpr.object);
    }

    @Override
    public String visitSetExpr(Expr.Set setExpr) {
        return parenthesize(setExpr.name.lexeme, setExpr.object, setExpr.rhs);
    }

    @Override
    public String visitThisExpr(Expr.This thisExpr) {
        return parenthesize(thisExpr.keyword.lexeme, thisExpr);
    }

    @Override
    public String visitSuperExpr(Expr.Super superExpr) {
        return parenthesize(superExpr.keyword.lexeme, superExpr);
    }

    @Override
    public String visitListExprExpr(Expr.ListExpr listExpr) {
        return parenthesize("list", listExpr);
    }

    @Override
    public String visitMapExprExpr(Expr.MapExpr mapExpr) {
        return parenthesize("map", mapExpr);
    }

    private String parenthesize(String name, Expr... exprs) {
        StringBuilder builder = new StringBuilder();
        builder.append(("(")).append(name);
        for (Expr expr : exprs) {
            builder.append(" ");
            builder.append(expr.accept(this));
        }
        builder.append(")");

        return builder.toString();
    }

    public static void main(String[] args) {
        // Expr expression = new Expr.Binary(
        //     new Expr.Unary(
        //         new Token(TokenType.MINUS, "-", null, 1),
        //         new Expr.Literal(123)
        //     ),
        // new Token(TokenType.STAR, "*", null, 1),
        // new Expr.Grouping(
        //     new Expr.Literal(45.67)
        // ));
        /*
         * (1+2) * (4-3)
        */
        Expr expression = new Expr.Binary(
            new Expr.Grouping(
                new Expr.Binary(
                    new Expr.Literal(1),
                    new Token(TokenType.PLUS, "+", null, 1),
                    new Expr.Literal(2)
                )
            ),
            new Token(TokenType.STAR, "*", null, 1),
            new Expr.Grouping(
                new Expr.Binary(
                    new Expr.Literal(4),
                    new Token(TokenType.MINUS, "-", null, 1),
                    new Expr.Literal(3)
                )
            )
        );
        System.out.println(new AstPrinter().print(expression));
        // System.out.println(new AstPrinter().print(
        //     new Expr.Binary(new Expr.Literal(1), new Token(TokenType.PLUS, "+", null, 0), new Expr.Literal(2))
        // ));
    }
}
