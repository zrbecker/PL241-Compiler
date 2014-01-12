package cs241.parser;

import java.io.Reader;

public class Parser {
	Scanner scanner;
	Token token;
	int tokenCount;

	public Parser() {
	}

	public void parse(Reader stream) {
		scanner = new Scanner(stream);
		tokenCount = -1;
		nextToken();
		parseComputation();
	}

	private void syntaxError(String message) {
		throw new RuntimeException("Syntax Error: " + message);
	}

	private void nextToken() {
		token = scanner.next();
		++tokenCount;
	}

	private void parseToken(Token.Type type, String expectedMessage) {
		if (token == null)
			syntaxError(expectedMessage + " (EOF reached)");
		else if (!token.isType(type))
			syntaxError(expectedMessage);
		nextToken();
	}

	private void parseRelOp() {
		switch (token.getType()) {
		case EQUALS:
		case NOTEQUALS:
		case LESSTHAN:
		case LESSTHANEQ:
		case GREATERTHAN:
		case GREATERTHANEQ:
			nextToken();
			break;
		default:
			syntaxError("Expected comparison operator");
			break;
		}
	}

	private void parseDesignator() {
		parseToken(Token.Type.IDENTIFIER, "Expected identifier");

		while (token.isType(Token.Type.LEFT_BRACKET)) {
			nextToken();
			parseExpression();
			parseToken(Token.Type.RIGHT_BRACKET, "Expected ']'");
		}
	}

	private void parseFactor() {
		switch (token.getType()) {
		case IDENTIFIER:
			parseDesignator();
			break;
		case NUMBER:
			nextToken();
			break;
		case LEFT_PARANTHESIS:
			nextToken();
			parseExpression();
			parseToken(Token.Type.RIGHT_PARANTHESIS, "Expected ')'");
			break;
		case KEYWORD_CALL:
			parseFuncCall();
			break;
		default:
			syntaxError("Expected Factor");
			break;
		}
	}

	private void parseTerm() {
		parseFactor();
		while (token.isType(Token.Type.MULTIPLICATION)
				|| token.isType(Token.Type.DIVISION)) {
			nextToken();
			parseFactor();
		}
	}

	private void parseExpression() {
		parseTerm();
		while (token.isType(Token.Type.ADDITION)
				|| token.isType(Token.Type.SUBTRACTION)) {
			nextToken();
			parseTerm();
		}
	}

	private void parseRelation() {
		parseExpression();
		parseRelOp();
		parseExpression();
	}

	private void parseAssignment() {
		parseToken(Token.Type.KEYWORD_LET, "Expected keyword let");
		parseDesignator();
		parseToken(Token.Type.ASSIGNMENT, "Expected assignment oprator '<-'");
		parseExpression();
	}

	private void parseFuncCall() {
		parseToken(Token.Type.KEYWORD_CALL, "Expected keyword call");
		parseToken(Token.Type.IDENTIFIER, "Expected identifier");

		if (token.isType(Token.Type.LEFT_PARANTHESIS)) {
			nextToken();
			if (!token.isType(Token.Type.RIGHT_PARANTHESIS)) {
				parseExpression();
				while (token.isType(Token.Type.COMMA)) {
					nextToken();
					parseExpression();
				}
			}
			parseToken(Token.Type.RIGHT_PARANTHESIS, "Expected ')'");
		}
	}

	private void parseIfStatement() {
		parseToken(Token.Type.KEYWORD_IF, "Expected keyword if");
		parseRelation();
		parseToken(Token.Type.KEYWORD_THEN, "Expected keyword then");
		parseStatSequence();
		if (token.isType(Token.Type.KEYWORD_ELSE)) {
			nextToken();
			parseStatSequence();
		}
		parseToken(Token.Type.KEYWORD_FI, "Expected keyword fi");
	}

	private void parseWhileStatement() {
		parseToken(Token.Type.KEYWORD_WHILE, "Expected keyword while");
		parseRelation();
		parseToken(Token.Type.KEYWORD_DO, "Expected keyword do");
		parseStatSequence();
		parseToken(Token.Type.KEYWORD_OD, "Expected keyword od");
	}

	private void parseReturnStatement() {
		parseToken(Token.Type.KEYWORD_RETURN, "Expected keyword return");
		switch (token.getType()) {
		case IDENTIFIER:
		case NUMBER:
		case LEFT_PARANTHESIS:
		case KEYWORD_CALL:
			parseExpression();
			break;
		default:
			break;
		}
	}

	private void parseStatement() {
		switch (token.getType()) {
		case KEYWORD_LET:
			parseAssignment();
			break;
		case KEYWORD_CALL:
			parseFuncCall();
			break;
		case KEYWORD_IF:
			parseIfStatement();
			break;
		case KEYWORD_WHILE:
			parseWhileStatement();
			break;
		case KEYWORD_RETURN:
			parseReturnStatement();
			break;
		default:
			syntaxError("Expected statement");
			break;
		}
	}

	private void parseStatSequence() {
		parseStatement();
		while (token.isType(Token.Type.SEMICOLON)) {
			nextToken();
			parseStatement();
		}
	}

	private void parseTypeDecl() {
		switch (token.getType()) {
		case KEYWORD_VAR:
			nextToken();
			break;
		case KEYWORD_ARRAY:
			nextToken();
			parseToken(Token.Type.LEFT_BRACKET, "Expected '['");
			parseToken(Token.Type.NUMBER, "Expected number");
			parseToken(Token.Type.RIGHT_BRACKET, "Expected ']'");
			while (token.isType(Token.Type.LEFT_BRACKET)) {
				nextToken();
				parseToken(Token.Type.NUMBER, "Expected number");
				parseToken(Token.Type.RIGHT_BRACKET, "Expected ']'");
			}
			break;
		default:
			syntaxError("Expected type declaration");
			break;
		}
	}

	private void parseVarDecl() {
		parseTypeDecl();
		parseToken(Token.Type.IDENTIFIER, "Expected identifier");
		while (token.isType(Token.Type.COMMA)) {
			nextToken();
			parseToken(Token.Type.IDENTIFIER, "Expected identifier");
		}
		parseToken(Token.Type.SEMICOLON, "Expected ';'");
	}

	private void parseFuncDecl() {
		switch (token.getType()) {
		case KEYWORD_FUNCTION:
		case KEYWORD_PROCEDURE:
			nextToken();
			break;
		default:
			syntaxError("Expected keyword function or procedure");
			break;
		}
		parseToken(Token.Type.IDENTIFIER, "Expected identifier");
		if (!token.isType(Token.Type.SEMICOLON))
			parseFormalParam();
		parseToken(Token.Type.SEMICOLON, "Expected ';'");
		parseFuncBody();
		parseToken(Token.Type.SEMICOLON, "Expected ';'");
	}

	private void parseFormalParam() {
		parseToken(Token.Type.LEFT_PARANTHESIS, "Expected '('");
		if (!token.isType(Token.Type.RIGHT_PARANTHESIS)) {
			parseToken(Token.Type.IDENTIFIER, "Expected identifier");
			while (token.isType(Token.Type.COMMA)) {
				nextToken();
				parseToken(Token.Type.IDENTIFIER, "Expected identifier");
			}
		}
		parseToken(Token.Type.RIGHT_PARANTHESIS, "Expected ')'");
	}

	private void parseFuncBody() {
		while (token.isType(Token.Type.KEYWORD_VAR)
				|| token.isType(Token.Type.KEYWORD_ARRAY)) {
			parseVarDecl();
		}
		parseToken(Token.Type.LEFT_CURLY, "Expected '{'");
		if (!token.isType(Token.Type.RIGHT_CURLY)) {
			parseStatSequence();
		}
		parseToken(Token.Type.RIGHT_CURLY, "Expected '}'");
	}

	private void parseComputation() {
		parseToken(Token.Type.KEYWORD_MAIN, "Expected keyword main");
		while (token.isType(Token.Type.KEYWORD_VAR)
				|| token.isType(Token.Type.KEYWORD_ARRAY)) {
			parseVarDecl();
		}
		while (token.isType(Token.Type.KEYWORD_FUNCTION)
				|| token.isType(Token.Type.KEYWORD_PROCEDURE)) {
			parseFuncDecl();
		}
		parseToken(Token.Type.LEFT_CURLY, "Expected '{'");
		parseStatSequence();
		parseToken(Token.Type.RIGHT_CURLY, "Expected '}'");
		parseToken(Token.Type.PERIOD, "Expected '.'");
	}
}
