package com.craftinginterpreters.lox;

public class TreePrinter implements Expr.ExprVisitor<String> {
    public TreePrinter() {}
    public String print(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return format(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return format("group", expr.expression);
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        if (expr.value == null) return "nil";
        return expr.value.toString();
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return format(expr.operator.lexeme, expr.right);
    }

    @Override
    public String visitTernaryExpr(Expr.Ternary expr) {
        return format(expr.operator.lexeme, expr.cond, expr.trueBranch, expr.falseBranch);
    }

    @Override
    public String visitVariableExpr(Expr.Variable var) {
        return format(var.name.lexeme, new Expr.Literal(null));
    }

    @Override
    public String visitAssignExpr(Expr.Assign assignment) {
        return format(assignment.name.lexeme, assignment.rhs);
    }

    @Override
    public String visitIndexExpr(Expr.Index idx) {
        return format(idx.object.toString(), idx.idxExpr);
    }

    @Override
    public String visitPostfixExpr(Expr.Postfix postfix) {
        return format(postfix.name.lexeme, postfix);
    }

    @Override
    public String visitPrefixExpr(Expr.Prefix prefix) {
        return format(prefix.name.lexeme, prefix);
    }

    @Override
    public String visitCallExpr(Expr.Call call) {
        return format(call.callee.toString(), call);
    }

    @Override
    public String visitAnonymousExpr(Expr.Anonymous anon) {
        return format("anon", anon);
    }

    @Override
    public String visitGetExpr(Expr.Get getExpr) {
        return format(getExpr.name.lexeme, getExpr.object);
    }

    @Override
    public String visitSetExpr(Expr.Set setExpr) {
        return format(setExpr.name.lexeme, setExpr.object, setExpr.rhs);
    }

    private String format(String name, Expr... exprs) {
        StringBuilder builder = new StringBuilder();
        builder.append(name).append("\n");
        for (int i = 0; i < exprs.length; i++) {
            builder.append(branch(exprs[i].accept(this), i == exprs.length - 1));
        }
        return builder.toString();
    }

    private String branch(String text, boolean isLast) {
        String[] lines = text.split("\n");
        StringBuilder builder = new StringBuilder();
        String prefix = isLast ? "└── " : "├── ";
        for (int i = 0; i < lines.length; i++) {
            builder.append(prefix).append(lines[i]).append("\n");
            prefix = isLast ? "    " : "│   ";
        }
        return builder.toString();
    }

    public static void main(String[] args) {
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
        System.out.println(new TreePrinter().print(expression));
    }
}
