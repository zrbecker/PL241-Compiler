package cs241.parser;
import java.io.IOException;
import java.io.Reader;

public class Scanner {
	private Reader stream;
	private char in;
	private boolean EOF = false;
	private int lineNumber;
	private int charNumber;
	
	public Scanner(Reader stream) {
		this.stream = stream;
		lineNumber = 0;
		nextChar();
	}
	
	public int getLineNumber() {
		return lineNumber;
	}
	
	public int getCharNumber() {
		return charNumber;
	}
	
	private void nextChar() {
		charNumber += 1;
		if (in == '\n') {
			charNumber = 0;
			lineNumber += 1;
		}
		
		try {
			int input = stream.read();
			if (input == -1)
				EOF = true;
			else
				in = (char)input;
		} catch (IOException e) {
			e.printStackTrace();
			EOF = true;
		}
	}
	
	private static boolean isLetter(char c) {
		return ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'));
	}
	
	private static boolean isDigit(char c) {
		return (c >= '0' && c <= '9');
	}
	
	private static boolean isWhiteSpace(char c) {
		return (c == ' ' || c == '\t' || c == '\n' || c == '\r');
	}
	
	private void removeComment() {
		while (!EOF && in != '\n')
			nextChar();
	}
	
	private Token nextIdentifierOrKeyword() {
		StringBuilder sb = new StringBuilder();
		if (EOF || !isLetter(in))
			return new Token(Token.Type.SYNTAX_ERROR);
		
		sb.append(in);
		nextChar();
		
		while (!EOF && (isLetter(in) || isDigit(in))) {
			sb.append(in);
			nextChar();
		}
		
		return new Token(sb.toString());
	}
	
	private Token nextNumber() {
		if (EOF || !isDigit(in))
			return new Token(Token.Type.SYNTAX_ERROR);
		
		int value = in - '0';
		nextChar();
		
		while (!EOF && isDigit(in)) {
			value = value * 10 + (in - '0');
			nextChar();
			// TODO: Should we be checking overflow?
			if (value < 0)
				return new Token(Token.Type.SYNTAX_ERROR);
		}
		
		return new Token(value);
	}
	
	private Token nextSymbol() {
		if (EOF)
			return new Token(Token.Type.SYNTAX_ERROR);
		
		switch (in) {
		case '[': nextChar(); return new Token(Token.Type.LEFT_BRACKET);
		case ']': nextChar(); return new Token(Token.Type.RIGHT_BRACKET);
		case '(': nextChar(); return new Token(Token.Type.LEFT_PARANTHESIS);
		case ')': nextChar(); return new Token(Token.Type.RIGHT_PARANTHESIS);
		case '{': nextChar(); return new Token(Token.Type.LEFT_CURLY);
		case '}': nextChar(); return new Token(Token.Type.RIGHT_CURLY);
		case '.': nextChar(); return new Token(Token.Type.PERIOD);
		case ',': nextChar(); return new Token(Token.Type.COMMA);
		case ';': nextChar(); return new Token(Token.Type.SEMICOLON);
		case '*': nextChar(); return new Token(Token.Type.MULTIPLICATION);
		case '/': 
			nextChar();
			if (!EOF && in == '/') {
				removeComment();
				return next();
			} else {
				return new Token(Token.Type.DIVISION);
			}
		case '+': nextChar(); return new Token(Token.Type.ADDITION);
		case '-': nextChar(); return new Token(Token.Type.SUBTRACTION);
		case '#': 
			nextChar();
			removeComment();
			return next();
		case '=':
			nextChar();
			if (!EOF && in == '=') {
				nextChar();
				return new Token(Token.Type.EQUALS);
			} else {
				return null;
			}
		case '!':
			nextChar();
			if (!EOF && in == '=') {
				nextChar();
				return new Token(Token.Type.NOTEQUALS);
			} else {
				return null;
			}
		case '<':
			nextChar();
			if (!EOF && in == '=') {
				nextChar();
				return new Token(Token.Type.LESSTHANEQ);
			} else if (!EOF && in == '-') {
				nextChar();
				return new Token(Token.Type.ASSIGNMENT);
			} else {
				return new Token(Token.Type.LESSTHAN);
			}
		case '>':
			nextChar();
			if (!EOF && in == '=') {
				nextChar();
				return new Token(Token.Type.GREATERTHANEQ);
			} else {
				return new Token(Token.Type.GREATERTHAN);
			}
		default:
			return new Token(Token.Type.SYNTAX_ERROR);
		}
	}
	
	public Token next() {
		while (!EOF && isWhiteSpace(in)) {
			nextChar();
		}
		
		if (EOF)
			return null;

		Token token;
		if (isLetter(in))
			token = nextIdentifierOrKeyword();
		else if (isDigit(in))
			token = nextNumber();
		else
			token = nextSymbol();
		
		return token;
	}
}
