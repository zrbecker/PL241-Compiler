package cs241.parser;

import java.util.ArrayList;

import cs241.parser.treenodes.Computation;
import cs241.parser.treenodes.Expression;
import cs241.parser.treenodes.Function;
import cs241.parser.treenodes.Relation;
import cs241.parser.treenodes.Statement;
import cs241.parser.treenodes.Variable;

public class PrettyPrint {
	int indent;
	StringBuilder sb;
	
	public PrettyPrint() {
		indent = 0;
		sb = new StringBuilder();
	}
	
	private void tabs() {
		for (int i = 0; i < indent; ++i) {
			sb.append("    ");
		}
	}
	
	public String Print(Computation node) {
		tabs(); sb.append("Main => {\n");
		indent += 1;
		
		if (node.getVariables() != null) {
			tabs(); sb.append("Variables => {\n");
			indent += 1;
			for (Variable v : node.getVariables())
				Print(v);
			indent -= 1;
			tabs(); sb.append("},\n");
		}
		
		if (node.getFunctions() != null) {
			tabs(); sb.append("Functions => {\n");
			indent += 1;
			for (Function f : node.getFunctions())
				Print(f);
			indent -= 1;
			tabs(); sb.append("},\n");
		}
		
		tabs(); sb.append("Body => {\n");
		indent += 1;
		for (Statement s : node.getBody())
			Print(s);
		indent -= 1;
		tabs(); sb.append("},\n");
		
		indent -= 1;
		sb.append("},");
		
		String result = sb.toString();
		sb = new StringBuilder();
		return result;
	}
	
	private void Print(Relation node) {
		tabs(); sb.append("Relation => {\n");
		indent += 1;
		tabs(); sb.append("operator => ");
		switch (node.getOperator()) {
		case EQUALS: sb.append("==,\n"); break;
		case NOTEQUALS: sb.append("!=,\n"); break;
		case LESSTHAN: sb.append("<,\n"); break;
		case LESSTHANEQ: sb.append("<=,\n"); break;
		case GREATERTHAN: sb.append(">,\n"); break;
		case GREATERTHANEQ: sb.append(">=,\n"); break;
		}
		
		tabs(); sb.append("left => {\n");
		indent += 1;
		Print(node.getLeft());
		indent -= 1;
		tabs(); sb.append("},\n");
		
		tabs(); sb.append("right => {\n");
		indent += 1;
		Print(node.getRight());
		indent -= 1;
		tabs(); sb.append("},\n");
		
		indent -= 1;
		tabs(); sb.append("},\n");
	}
	
	private void Print(Variable node) {
		switch (node.getType()) {
		case VARIABLE:
			tabs(); sb.append("Variable => " + node.getName() + ",\n");
			break;
		case ARRAY:
			tabs(); sb.append("Array => {\n");
			indent += 1;
			tabs(); sb.append("name => " + node.getName() + ",\n");
			tabs(); sb.append("dimensions => { ");
			ArrayList<Integer> dimensions = node.getDimensions();
			for (int d : dimensions) {
				sb.append("[" + d + "]");
			}
			sb.append(" },\n");
			indent -= 1;
			tabs(); sb.append("},\n");
			break;
		}
	}
	
	private void Print(Function node) {
		tabs(); sb.append("Function => {\n");
		indent += 1;
		
		tabs(); sb.append("type => ");
		switch (node.getType()) {
		case PROCEDURE: sb.append("procedure,\n"); break;
		case FUNCTION: sb.append("function,\n"); break;
		}
		
		tabs(); sb.append("name => " + node.getName() + ",\n");
		tabs(); sb.append("parameters => { ");
		if (node.getParameters() != null) {
			sb.append("\n");
			indent += 1;
			for (String parameter : node.getParameters()) {
				tabs(); sb.append(parameter + ",\n");
			}
			indent -= 1;
			tabs();
		}
		sb.append("},\n");
		
		if (node.getVariables() != null) {
			tabs(); sb.append("variables => {\n");
			indent += 1;
			for (Variable v : node.getVariables()) {
				Print(v);
			}
			indent -= 1;
			tabs(); sb.append("},\n");
		}
		
		tabs(); sb.append("body => {\n");
		indent += 1;
		for (Statement s : node.getBody()) {
			Print(s);
		}
		indent -= 1;
		tabs(); sb.append("},\n");
		
		indent -= 1;
		tabs(); sb.append("},\n");
	}

	private void Print(Statement node) {
		if (node instanceof Statement.Assignment)
			Print((Statement.Assignment) node);
		else if (node instanceof Statement.FunctionCall)
			Print((Statement.FunctionCall) node);
		else if (node instanceof Statement.If)
			Print((Statement.If) node);
		else if (node instanceof Statement.While)
			Print((Statement.While) node);
		else if (node instanceof Statement.Return)
			Print((Statement.Return) node);
	}

	private void Print(Statement.Assignment node) {
		tabs(); sb.append("Assignment => {\n");
		indent += 1;
		
		tabs(); sb.append("left => {\n");
		indent += 1;
		Print(node.getLeft());
		indent -= 1;
		tabs(); sb.append("},\n");
		
		tabs(); sb.append("right => {\n");
		indent += 1;
		Print(node.getRight());
		indent -= 1;
		tabs(); sb.append("},\n");
		
		indent -= 1;
		tabs(); sb.append("},\n");
	}

	private void Print(Statement.FunctionCall node) {
		Print(node.getFunctionCallExpression());
	}

	private void Print(Statement.If node) {
		tabs(); sb.append("If => {\n");
		indent += 1;
		
		tabs(); sb.append("conditon => {\n");
		indent += 1;
		Print(node.getCondition());
		indent -= 1;
		tabs(); sb.append("},\n");
		
		tabs(); sb.append("then => {\n");
		indent += 1;
		for (Statement s : node.getThenBlock())
			Print(s);
		indent -= 1;
		tabs(); sb.append("},\n");
		
		if (node.getElseBlock() != null) {
			tabs(); sb.append("else => {\n");
			indent += 1;
			for (Statement s : node.getElseBlock())
				Print(s);
			indent -= 1;
			tabs(); sb.append("},\n");
		}
		
		indent -= 1;
		tabs(); sb.append("},\n");
	}

	private void Print(Statement.While node) {
		tabs(); sb.append("While => {\n");
		indent += 1;
		
		tabs(); sb.append("condition => {\n");
		indent += 1;
		Print(node.getCondition());
		indent -= 1;
		tabs(); sb.append("},\n");
		
		tabs(); sb.append("do => {\n");
		indent += 1;
		for (Statement s : node.getBlock())
			Print(s);
		indent -= 1;
		tabs(); sb.append("},\n");
		
		indent -= 1;
		tabs(); sb.append("},\n");
	}

	private void Print(Statement.Return node) {
		tabs(); sb.append("Return => {\n");
		indent += 1;
		Print(node.getExpression());
		indent -= 1;
		tabs(); sb.append("},\n");
	}
	
	private void Print(Expression node) {
		if (node instanceof Expression.Binary)
			Print((Expression.Binary) node);
		else if (node instanceof Expression.Designator)
			Print((Expression.Designator) node);
		else if (node instanceof Expression.FunctionCallExp)
			Print((Expression.FunctionCallExp) node);
		else if (node instanceof Expression.Number)
			Print((Expression.Number) node);
	}
	
	public void Print(Expression.Binary node) {
		tabs(); sb.append("BinaryOperator => {\n");
		indent += 1;
		
		tabs(); sb.append("operator => ");
		switch (node.getOperator()) {
		case ADDITION: sb.append("+,\n"); break;
		case DIVISION: sb.append("/,\n"); break;
		case MULTIPLICATION: sb.append("*,\n"); break;
		case SUBTRACTION: sb.append("-,\n"); break;
		}
		
		tabs(); sb.append("left => {\n");
		indent += 1;
		Print(node.getLeft());
		indent -= 1;
		tabs(); sb.append("},\n");
		
		tabs(); sb.append("right => {\n");
		indent += 1;
		Print(node.getRight());
		indent -= 1;
		tabs(); sb.append("},\n");
		
		indent -= 1;
		tabs(); sb.append("},\n");
	}
	
	public void Print(Expression.Designator node) {
		tabs(); sb.append("Designator => {\n");
		indent += 1;
		tabs(); sb.append("name => " + node.getName() + ",\n");
		if (node.getIndices() != null) {
			tabs(); sb.append("indices => {\n");
			indent += 1;
			for (Expression e : node.getIndices()) {
				Print(e);
			}
			indent -= 1;
			tabs();sb.append("},\n");
		}
		indent -= 1;
		tabs(); sb.append("},\n");
	}
	
	public void Print(Expression.Number node) {
		tabs(); sb.append("Number => " + node.getValue() + ",\n");
	}
	
	public void Print(Expression.FunctionCallExp node) {
		tabs(); sb.append("FunctionCall => {\n");
		indent += 1;
		tabs(); sb.append("name => " + node.getName() + ",\n");
		tabs(); sb.append("arguments => { ");
		if (node.getArguments() != null) {
			sb.append("\n");
			indent += 1;
			for (Expression argument : node.getArguments()) {
				Print(argument);
			}
			indent -= 1;
			tabs();
		}
		sb.append("},\n");
		indent -= 1;
		tabs(); sb.append("},\n");
	}
}
