package cs241.parser.treenodes;

import java.util.ArrayList;

public abstract class Statement {
	public static class Assignment extends Statement {
		Expression.Designator left;
		Expression right;
		
		public Assignment(Expression.Designator left, Expression right) {
			this.left = left;
			this.right = right;
		}
		
		public Expression.Designator getLeft() {
			return left;
		}
		
		public Expression getRight() {
			return right;
		}
	}
	
	public static class FunctionCall extends Statement {
		Expression.FunctionCall expression;
		
		public FunctionCall(String name) {
			this(name, null);
		}
		
		public FunctionCall(String name, ArrayList<Expression> arguments) {
			expression = new Expression.FunctionCall(name, arguments);
		}

		public FunctionCall(Expression.FunctionCall expression) {
			this.expression = expression;
		}
		
		public String getName() {
			return expression.getName();
		}
		
		public ArrayList<Expression> getArguments() {
			return expression.getArguments();
		}
		
		public Expression.FunctionCall getFunctionCallExpression() {
			return expression;
		}
	}
	
	public static class If extends Statement {
		Relation condition;
		ArrayList<Statement> thenBlock;
		ArrayList<Statement> elseBlock;
		
		public If(Relation condition, ArrayList<Statement> thenBlock) {
			this(condition, thenBlock, null);
		}
		
		public If(Relation condition, ArrayList<Statement> thenBlock, ArrayList<Statement> elseBlock) {
			this.condition = condition;
			this.thenBlock = thenBlock;
			this.elseBlock = elseBlock;
		}
		
		public Relation getCondition() {
			return condition;
		}
		
		public ArrayList<Statement> getThenBlock() {
			return thenBlock;
		}
		
		public ArrayList<Statement> getElseBlock() {
			return elseBlock;
		}
	}
	
	public static class While extends Statement {
		Relation condition;
		ArrayList<Statement> block;
		
		public While(Relation condition, ArrayList<Statement> block) {
			this.condition = condition;
			this.block = block;
		}
		
		public Relation getCondition() {
			return condition;
		}

		public ArrayList<Statement> getBlock() {
			return block;
		}
	}
	
	public static class Return extends Statement {
		Expression expression;
		
		public Return(Expression expression) {
			this.expression = expression;
		}
		
		public Expression getExpression() {
			return expression;
		}
	}
}
