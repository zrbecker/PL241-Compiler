package cs241.parser;
import java.util.HashMap;


public class Token {
	public static enum Type {
		NUMBER,				// digit {digit}
		
		LEFT_BRACKET,       // [
		RIGHT_BRACKET,		// ]
		LEFT_PARANTHESIS,   // (
		RIGHT_PARANTHESIS,	// )
		LEFT_CURLY,			// {
		RIGHT_CURLY,		// }
		PERIOD,				// .
		COMMA,				// ,
		SEMICOLON,			// ;
		MULTIPLICATION,		// *
		DIVISION,			// /
		ADDITION,			// +
		SUBTRACTION,		// -
		ASSIGNMENT,			// <-
		EQUALS,				// ==
		NOTEQUALS,			// !=
		LESSTHAN,			// <
		LESSTHANEQ,			// <=
		GREATERTHAN,		// >
		GREATERTHANEQ,		// >=
		
		IDENTIFIER,			// letter {letter | digit}
		KEYWORD_LET,		// let
		KEYWORD_CALL,		// call
		KEYWORD_IF,			// if
		KEYWORD_THEN,		// then
		KEYWORD_ELSE,		// else
		KEYWORD_FI,			// fi
		KEYWORD_WHILE,		// while
		KEYWORD_DO,			// do
		KEYWORD_OD,			// od
		KEYWORD_RETURN,		// return
		KEYWORD_VAR,		// var
		KEYWORD_ARRAY,		// array
		KEYWORD_FUNCTION,	// function
		KEYWORD_PROCEDURE,	// procedure
		KEYWORD_MAIN,		// main
		
		SYNTAX_ERROR
	}
	
	private Type type;
	private int value;
	private String name;
	private static HashMap<String, Type> keywords = null;
	
	public Token(Type type) {
		this.type = type;
		value = 0;
		name = type.toString();
	}
	
	public Token(String name) {
		if (keywords == null) {
			keywords = new HashMap<String, Type>();
			keywords.put("let", Type.KEYWORD_LET);
			keywords.put("call", Type.KEYWORD_CALL);
			keywords.put("if", Type.KEYWORD_IF);
			keywords.put("then", Type.KEYWORD_THEN);
			keywords.put("else", Type.KEYWORD_ELSE);
			keywords.put("fi", Type.KEYWORD_FI);
			keywords.put("while", Type.KEYWORD_WHILE);
			keywords.put("do", Type.KEYWORD_DO);
			keywords.put("od", Type.KEYWORD_OD);
			keywords.put("return", Type.KEYWORD_RETURN);
			keywords.put("var", Type.KEYWORD_VAR);
			keywords.put("array", Type.KEYWORD_ARRAY);
			keywords.put("function", Type.KEYWORD_FUNCTION);
			keywords.put("procedure", Type.KEYWORD_PROCEDURE);
			keywords.put("main", Type.KEYWORD_MAIN);
		}
		
		if (keywords.containsKey(name)) {
			type = keywords.get(name);
			value = 0;
			this.name = name;
		} else {
			type = Type.IDENTIFIER;
			value = 0;
			this.name = name;
		}
	}
	
	public Token(int value) {
		type = Type.NUMBER;
		this.value = value;
		name = Integer.toString(value);
	}
	
	public boolean isType(Type type) {
		return (this.type == type);
	}
	
	public Type getType() {
		return type;
	}
	
	public int getValue() {
		return value;
	}
	
	public String getName() {
		return name;
	}
}
