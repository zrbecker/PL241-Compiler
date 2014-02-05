package cs241.parser.treenodes;

public class Relation {
	public enum Operator {
		EQUALS,
		NOTEQUALS,
		LESSTHAN,
		LESSTHANEQ,
		GREATERTHAN,
		GREATERTHANEQ
	}
	
	Expression left;
	Operator op;
	Expression right;
	
	public Relation(Expression left, Operator op, Expression right) {
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
	
	public Operator getOperator() {
		return op;
	}
}
