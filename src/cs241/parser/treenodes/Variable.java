package cs241.parser.treenodes;

import java.util.ArrayList;

public class Variable {
	public enum Type {
		VARIABLE,
		ARRAY
	}
	
	Type type;
	ArrayList<Integer> dimensions;
	String name;
	
	public Variable(String name) {
		type = Type.VARIABLE;
		this.name = name;
		this.dimensions = null;
	}
	
	public Variable(String name, ArrayList<Integer> dimensions) {
		type = Type.ARRAY;
		this.name = name;
		this.dimensions = dimensions;
	}
	
	public Type getType() {
		return type;
	}
	
	public String getName() {
		return name;
	}
	
	public ArrayList<Integer> getDimensions() {
		return dimensions;
	}
}
