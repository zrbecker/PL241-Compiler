package cs241.parser.treenodes;

public class Relation {
	public enum RelationOperator {
		EQUALS,
		NOTEQUALS,
		LESSTHAN,
		LESSTHANEQ,
		GREATERTHAN,
		GREATERTHANEQ
	}
	
	Expression left;
	RelationOperator op;
	Expression right;
	
	public Relation(Expression left, RelationOperator op, Expression right) {
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
	
	public RelationOperator getOperator() {
		return op;
	}
}
