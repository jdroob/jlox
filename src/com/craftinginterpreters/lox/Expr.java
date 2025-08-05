package com.craftinginterpreters.lox;

import java.util.List;

abstract class Expr {
	interface ExprVisitor<R> {
		R visitBinaryExpr(Expr.Binary binary);
		R visitGroupingExpr(Expr.Grouping grouping);
		R visitLiteralExpr(Expr.Literal literal);
		R visitUnaryExpr(Expr.Unary unary);
		R visitTernaryExpr(Expr.Ternary ternary);
		R visitVariableExpr(Expr.Variable variable);
		R visitAssignExpr(Expr.Assign assign);
		R visitIndexExpr(Expr.Index index);
		R visitPrefixExpr(Expr.Prefix prefix);
		R visitPostfixExpr(Expr.Postfix postfix);
		R visitCallExpr(Expr.Call call);
		R visitAnonymousExpr(Expr.Anonymous anonymous);
		R visitGetExpr(Expr.Get get);
		R visitSetExpr(Expr.Set set);
		R visitThisExpr(Expr.This thisExpr);
		R visitSuperExpr(Expr.Super superExpr);
	}
	abstract <R> R accept(ExprVisitor<R> visitor);
	static class Binary extends Expr {
		Binary(Expr left, Token operator, Expr right) {
			this.left = left;
			this.operator = operator;
			this.right = right;
		}

		@Override
		public <R> R accept(ExprVisitor<R> visitor) {
			return visitor.visitBinaryExpr(this);
		}

		final Expr left;
		final Token operator;
		final Expr right;
	}
	static class Grouping extends Expr {
		Grouping(Expr expression) {
			this.expression = expression;
		}

		@Override
		public <R> R accept(ExprVisitor<R> visitor) {
			return visitor.visitGroupingExpr(this);
		}

		final Expr expression;
	}
	static class Literal extends Expr {
		Literal(Object value) {
			this.value = value;
		}

		@Override
		public <R> R accept(ExprVisitor<R> visitor) {
			return visitor.visitLiteralExpr(this);
		}

		final Object value;
	}
	static class Unary extends Expr {
		Unary(Token operator, Expr right) {
			this.operator = operator;
			this.right = right;
		}

		@Override
		public <R> R accept(ExprVisitor<R> visitor) {
			return visitor.visitUnaryExpr(this);
		}

		final Token operator;
		final Expr right;
	}
	static class Ternary extends Expr {
		Ternary(Expr cond, Token operator, Expr trueBranch, Expr falseBranch) {
			this.cond = cond;
			this.operator = operator;
			this.trueBranch = trueBranch;
			this.falseBranch = falseBranch;
		}

		@Override
		public <R> R accept(ExprVisitor<R> visitor) {
			return visitor.visitTernaryExpr(this);
		}

		final Expr cond;
		final Token operator;
		final Expr trueBranch;
		final Expr falseBranch;
	}
	static class Variable extends Expr {
		Variable(Token name) {
			this.name = name;
		}

		@Override
		public <R> R accept(ExprVisitor<R> visitor) {
			return visitor.visitVariableExpr(this);
		}

		final Token name;
	}
	static class Assign extends Expr {
		Assign(Token name, Expr rhs) {
			this.name = name;
			this.rhs = rhs;
		}

		@Override
		public <R> R accept(ExprVisitor<R> visitor) {
			return visitor.visitAssignExpr(this);
		}

		final Token name;
		final Expr rhs;
	}
	static class Index extends Expr {
		Index(Token lbrack, Expr object, Expr idxExpr, Expr idxExpr2) {
			this.lbrack = lbrack;
			this.object = object;
			this.idxExpr = idxExpr;
			this.idxExpr2 = idxExpr2;
		}

		@Override
		public <R> R accept(ExprVisitor<R> visitor) {
			return visitor.visitIndexExpr(this);
		}

		final Token lbrack;
		final Expr object;
		final Expr idxExpr;
		final Expr idxExpr2;
	}
	static class Prefix extends Expr {
		Prefix(Token operator, Token name) {
			this.operator = operator;
			this.name = name;
		}

		@Override
		public <R> R accept(ExprVisitor<R> visitor) {
			return visitor.visitPrefixExpr(this);
		}

		final Token operator;
		final Token name;
	}
	static class Postfix extends Expr {
		Postfix(Token name, Token operator) {
			this.name = name;
			this.operator = operator;
		}

		@Override
		public <R> R accept(ExprVisitor<R> visitor) {
			return visitor.visitPostfixExpr(this);
		}

		final Token name;
		final Token operator;
	}
	static class Call extends Expr {
		Call(Expr callee, Token paren, List<Expr> args) {
			this.callee = callee;
			this.paren = paren;
			this.args = args;
		}

		@Override
		public <R> R accept(ExprVisitor<R> visitor) {
			return visitor.visitCallExpr(this);
		}

		final Expr callee;
		final Token paren;
		final List<Expr> args;
	}
	static class Anonymous extends Expr {
		Anonymous(List<Token> params, Stmt body) {
			this.params = params;
			this.body = body;
		}

		@Override
		public <R> R accept(ExprVisitor<R> visitor) {
			return visitor.visitAnonymousExpr(this);
		}

		final List<Token> params;
		final Stmt body;
	}
	static class Get extends Expr {
		Get(Expr object, Token name) {
			this.object = object;
			this.name = name;
		}

		@Override
		public <R> R accept(ExprVisitor<R> visitor) {
			return visitor.visitGetExpr(this);
		}

		final Expr object;
		final Token name;
	}
	static class Set extends Expr {
		Set(Expr object, Token name, Expr rhs) {
			this.object = object;
			this.name = name;
			this.rhs = rhs;
		}

		@Override
		public <R> R accept(ExprVisitor<R> visitor) {
			return visitor.visitSetExpr(this);
		}

		final Expr object;
		final Token name;
		final Expr rhs;
	}
	static class This extends Expr {
		This(Token keyword) {
			this.keyword = keyword;
		}

		@Override
		public <R> R accept(ExprVisitor<R> visitor) {
			return visitor.visitThisExpr(this);
		}

		final Token keyword;
	}
	static class Super extends Expr {
		Super(Token keyword, Token method) {
			this.keyword = keyword;
			this.method = method;
		}

		@Override
		public <R> R accept(ExprVisitor<R> visitor) {
			return visitor.visitSuperExpr(this);
		}

		final Token keyword;
		final Token method;
	}
}
