package com.craftinginterpreters.lox;

import java.util.List;

abstract class Stmt {
	interface StmtVisitor<R> {
		R visitExpressionStmt(Stmt.Expression expression);
		R visitPrintStmt(Stmt.Print print);
		R visitVarStmt(Stmt.Var var);
		R visitIfStmt(Stmt.If ifStmt);
		R visitWhileStmt(Stmt.While whileStmt);
		R visitForStmt(Stmt.For forStmt);
		R visitBlockStmt(Stmt.Block block);
		R visitBreakStmt(Stmt.Break breakStmt);
		R visitContinueStmt(Stmt.Continue continueStmt);
		R visitFunctionDefStmt(Stmt.FunctionDef functiondef);
		R visitReturnStmt(Stmt.Return returnStmt);
		R visitClassStmt(Stmt.Class classStmt);
	}
	abstract <R> R accept(StmtVisitor<R> visitor);
	static class Expression extends Stmt {
		Expression(Expr expression) {
			this.expression = expression;
		}

		@Override
		public <R> R accept(StmtVisitor<R> visitor) {
			return visitor.visitExpressionStmt(this);
		}

		final Expr expression;
	}
	static class Print extends Stmt {
		Print(Expr expression) {
			this.expression = expression;
		}

		@Override
		public <R> R accept(StmtVisitor<R> visitor) {
			return visitor.visitPrintStmt(this);
		}

		final Expr expression;
	}
	static class Var extends Stmt {
		Var(Token name, Expr initializer) {
			this.name = name;
			this.initializer = initializer;
		}

		@Override
		public <R> R accept(StmtVisitor<R> visitor) {
			return visitor.visitVarStmt(this);
		}

		final Token name;
		final Expr initializer;
	}
	static class If extends Stmt {
		If(Expr condition, Stmt thenStmt, Stmt elseStmt) {
			this.condition = condition;
			this.thenStmt = thenStmt;
			this.elseStmt = elseStmt;
		}

		@Override
		public <R> R accept(StmtVisitor<R> visitor) {
			return visitor.visitIfStmt(this);
		}

		final Expr condition;
		final Stmt thenStmt;
		final Stmt elseStmt;
	}
	static class While extends Stmt {
		While(Expr condition, Stmt body) {
			this.condition = condition;
			this.body = body;
		}

		@Override
		public <R> R accept(StmtVisitor<R> visitor) {
			return visitor.visitWhileStmt(this);
		}

		final Expr condition;
		final Stmt body;
	}
	static class For extends Stmt {
		For(Stmt initialization, Expr condition, Stmt update, Stmt body) {
			this.initialization = initialization;
			this.condition = condition;
			this.update = update;
			this.body = body;
		}

		@Override
		public <R> R accept(StmtVisitor<R> visitor) {
			return visitor.visitForStmt(this);
		}

		final Stmt initialization;
		final Expr condition;
		final Stmt update;
		final Stmt body;
	}
	static class Block extends Stmt {
		Block(List<Stmt> statements) {
			this.statements = statements;
		}

		@Override
		public <R> R accept(StmtVisitor<R> visitor) {
			return visitor.visitBlockStmt(this);
		}

		final List<Stmt> statements;
	}
	static class Break extends Stmt {
		Break(Token keyword) {
			this.keyword = keyword;
		}

		@Override
		public <R> R accept(StmtVisitor<R> visitor) {
			return visitor.visitBreakStmt(this);
		}

		final Token keyword;
	}
	static class Continue extends Stmt {
		Continue(Token keyword) {
			this.keyword = keyword;
		}

		@Override
		public <R> R accept(StmtVisitor<R> visitor) {
			return visitor.visitContinueStmt(this);
		}

		final Token keyword;
	}
	static class FunctionDef extends Stmt {
		FunctionDef(Token name, List<Token> params, Stmt body) {
			this.name = name;
			this.params = params;
			this.body = body;
		}

		@Override
		public <R> R accept(StmtVisitor<R> visitor) {
			return visitor.visitFunctionDefStmt(this);
		}

		final Token name;
		final List<Token> params;
		final Stmt body;
	}
	static class Return extends Stmt {
		Return(Token keyword, Expr value) {
			this.keyword = keyword;
			this.value = value;
		}

		@Override
		public <R> R accept(StmtVisitor<R> visitor) {
			return visitor.visitReturnStmt(this);
		}

		final Token keyword;
		final Expr value;
	}
	static class Class extends Stmt {
		Class(Token name, List<Stmt.FunctionDef> methods) {
			this.name = name;
			this.methods = methods;
		}

		@Override
		public <R> R accept(StmtVisitor<R> visitor) {
			return visitor.visitClassStmt(this);
		}

		final Token name;
		final List<Stmt.FunctionDef> methods;
	}
}
