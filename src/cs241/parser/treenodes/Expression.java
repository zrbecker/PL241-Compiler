package cs241.parser.treenodes;

import java.util.ArrayList;

public abstract class Expression {
	public static class Binary extends Expression {
		public enum BinaryOperator {
			MULTIPLICATION,
			DIVISION,
			ADDITION,
			SUBTRACTION
		}
		
		BinaryOperator op;
		Expression left;
		Expression right;
		
		public Binary(Expression left, BinaryOperator op, Expression right) {
			this.left = left;
			this.op = op;
			this.right = right;
		}
		
		public Expression getLeft() {
			return left;
		}
		
		public Expression getRight() {
			return right;
		}
		
		public BinaryOperator getOperator() {
			return op;
		}
	}
	
	public static class Number extends Expression {
		int value;
		
		public Number(int value) {
			this.value = value;
		}
		
		public int getValue() {
			return value;
		}
	}
	
	/*
	 * A designator is always an expression unless it is being used
	 * in an assignment statement. The assignment statement will
	 * simply refer to this class.
	 */
	public static class Designator extends Expression {
		String name;
		ArrayList<Expression> indices;
		boolean isGlobal;
		
		public Designator(String name, boolean isGlobal) {
			this(name, isGlobal, null);
		}
		
		public Designator(String name, boolean isGlobal, ArrayList<Expression> indices) {
			this.name = name;
			this.isGlobal = isGlobal;
			this.indices = indices;
		}
		
		public String getName() {
			return name;
		}
		
		public ArrayList<Expression> getIndices() {
			return indices;
		}
		
		public boolean isGlobal() {
			return isGlobal;
		}
	}
	
	/*
	 * A function call can be both an expression and a statement, the grammar
	 * allows for a procedure to be an expression although it would be a 
	 * semantic error. A function call statement will simply be a wrapper
	 * around this class.
	 */
	public static class FunctionCallExp extends Expression {
		String name;
		ArrayList<Expression> arguments;
		
		public FunctionCallExp(String name) {
			this(name, new ArrayList<Expression>());
		}
		
		public FunctionCallExp(String name, ArrayList<Expression> arguments) {
			this.name = name;
			this.arguments = arguments == null ? new ArrayList<Expression>() : arguments;
		}
		
		public String getName() {
			return name;
		}
		
		public ArrayList<Expression> getArguments() {
			return arguments;
		}
	}
}
