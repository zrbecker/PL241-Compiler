package cs241.parser.treenodes;

import java.util.ArrayList;

public class Function {
	public enum Type {
		PROCEDURE,
		FUNCTION
	}
	
	Type type;
	String name;
	ArrayList<String> parameters;
	ArrayList<Variable> variables;
	ArrayList<Statement> body;
	
	public Function(Type type, String name, ArrayList<String> parameters, ArrayList<Variable> variables, ArrayList<Statement> body) {
		this.type = type;
		this.name = name;
		this.parameters = (parameters == null) ? new ArrayList<String>() : parameters;
		this.variables = (variables == null) ? new ArrayList<Variable>() : variables;
		this.body = (body == null) ? new ArrayList<Statement>() : body;
	}
	
	public Type getType() {
		return type;
	}
	
	public String getName() {
		return name;
	}
	
	public ArrayList<String> getParameters() {
		return parameters;
	}
	
	public ArrayList<Variable> getVariables() {
		return variables;
	}
	
	public ArrayList<Statement> getBody() {
		return body;
	}
}
