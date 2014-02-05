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
		tabs(); sb.append("main:\n");
		indent += 1;
		
		if (node.getVariables() != null) {
			for (Variable v : node.getVariables())
				Print(v);
		}
		
		if (node.getFunctions() != null) {
			for (Function f : node.getFunctions())
				Print(f);
		}
		
		if (node.getBody() != null) {
			for (Statement s : node.getBody())
				Print(s);
		}
		
		indent -= 1;
		
		String result = sb.toString();
		sb = new StringBuilder();
		return result;
	}
	
	private void Print(Relation node) {
		switch (node.getOperator()) {
		case EQUALS: 		tabs(); sb.append("==:\n"); break;
		case NOTEQUALS:		tabs(); sb.append("!=:\n"); break;
		case LESSTHAN:		tabs(); sb.append("<:\n"); break;
		case LESSTHANEQ:	tabs(); sb.append("<=:\n"); break;
		case GREATERTHAN:	tabs(); sb.append(">:\n"); break;
		case GREATERTHANEQ:	tabs(); sb.append(">=:\n"); break;
		}
		indent += 1;
		Print(node.getLeft());
		Print(node.getRight());
		indent -= 1;
	}
	
	private void Print(Variable node) {
		switch (node.getType()) {
		case VARIABLE:
			tabs(); sb.append("var " + node.getName() + "\n");
			break;
		case ARRAY:
			tabs(); sb.append("array " + node.getName());
			ArrayList<Integer> dimensions = node.getDimensions();
			for (int d : dimensions) {
				sb.append("[" + d + "]");
			}
			sb.append("\n");
			break;
		}
	}
	
	private void Print(Function node) {
		switch (node.getType()) {
		case PROCEDURE:
			tabs(); sb.append("procedure ");
			break;
		case FUNCTION:
			tabs(); sb.append("functions ");
			break;
		}
		sb.append(node.getName() + "(\n");
		indent += 1;
		if (node.getParameters() != null) {
			for (String parameter : node.getParameters()) {
				tabs(); sb.append(parameter + "\n");
			}
		}
		indent -= 1;
		tabs(); sb.append(")\n");
		indent += 1;
		
		if (node.getVariables() != null) {
			for (Variable v : node.getVariables()) {
				Print(v);
			}
		}
		
		if (node.getBody() != null) {
			for (Statement s : node.getBody()) {
				Print(s);
			}
		}
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
		tabs(); sb.append("<- :\n");
		indent += 1;
		Print(node.getLeft());
		Print(node.getRight());
		indent -= 1;
	}

	private void Print(Statement.FunctionCall node) {
		Print(node.getFunctionCallExpression());
	}

	private void Print(Statement.If node) {
		tabs(); sb.append("if:\n");
		indent += 1;
		Print(node.getCondition());
		indent -= 1;
		tabs(); sb.append("then:\n");
		indent += 1;
		for (Statement s : node.getThenBlock())
			Print(s);
		indent -= 1;
		if (node.getElseBlock() != null) {
			tabs(); sb.append("else:\n");
			indent += 1;
			for (Statement s: node.getElseBlock())
				Print(s);
			indent -= 1;
		}
	}

	private void Print(Statement.While node) {
		tabs(); sb.append("while:\n");
		indent += 1;
		Print(node.getCondition());
		indent -= 1;
		tabs(); sb.append("do:\n");
		indent += 1;
		for (Statement s : node.getBlock())
			Print(s);
		indent -= 1;
	}

	private void Print(Statement.Return node) {
		tabs(); sb.append("return: \n");
		indent += 1;
		Print(node.getExpression());
		indent -= 1;
	}
	
	private void Print(Expression node) {
		if (node instanceof Expression.Binary)
			Print((Expression.Binary) node);
		else if (node instanceof Expression.Designator)
			Print((Expression.Designator) node);
		else if (node instanceof Expression.FunctionCall)
			Print((Expression.FunctionCall) node);
		else if (node instanceof Expression.Number)
			Print((Expression.Number) node);
	}
	
	public void Print(Expression.Binary node) {
		switch (node.getOperator()) {
		case ADDITION: tabs(); sb.append("+ :\n"); break;
		case DIVISION: tabs(); sb.append("/ :\n"); break;
		case MULTIPLICATION: tabs(); sb.append("* :\n"); break;
		case SUBTRACTION: tabs(); sb.append("- :\n"); break;
		}
		indent += 1;
		Print(node.getLeft());
		Print(node.getRight());
		indent -= 1;
	}
	
	public void Print(Expression.Designator node) {
		tabs(); sb.append(node.getName() + "\n");
		if (node.getIndices() != null) {
			for (Expression e : node.getIndices()) {
				tabs(); sb.append("[\n");
				indent += 1;
				Print(e);
				indent -= 1;
				tabs(); sb.append("]\n");
			}
		}
		
	}
	
	public void Print(Expression.Number node) {
		tabs(); sb.append(node.getValue() + "\n");
	}
	
	public void Print(Expression.FunctionCall node) {
		tabs(); sb.append("call " + node.getName() + "\n");
		tabs(); sb.append("(\n");
		indent += 1;
		for (Expression argument : node.getArguments()) {
			Print(argument);
		}
		indent -= 1;
		tabs(); sb.append(")\n");
	}
}
