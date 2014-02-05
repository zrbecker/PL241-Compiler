package cs241.parser.treenodes;

import java.util.ArrayList;

public class Computation {

	ArrayList<Variable> variables;
	ArrayList<Function> functions;
	ArrayList<Statement> body;
	
	public Computation(ArrayList<Variable> variables, ArrayList<Function> functions, ArrayList<Statement> body) {
		this.variables = variables;
		this.functions = functions;
		this.body = body;
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
