package cs241.parser.treenodes;

import java.util.ArrayList;

public class Computation {

	ArrayList<Variable> variables;
	ArrayList<Function> functions;
	ArrayList<Statement> body;
	
	public Computation(ArrayList<Variable> variables, ArrayList<Function> functions, ArrayList<Statement> body) {
		this.variables = (variables == null) ? new ArrayList<Variable>() : variables;
		this.functions = (functions == null) ? new ArrayList<Function>() : functions;
		this.body = (body == null) ? new ArrayList<Statement>() : body;
	}
	
	public ArrayList<Variable> getVariables() {
		return variables;
	}
	
	public ArrayList<Function> getFunctions() {
		return functions;
	}
	
	public ArrayList<Statement> getBody() {
		return body;
	}

}
