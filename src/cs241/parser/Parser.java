package cs241.parser;

import java.io.Reader;
import java.util.ArrayList;

import cs241.parser.treenodes.Computation;
import cs241.parser.treenodes.Expression;
import cs241.parser.treenodes.Function;
import cs241.parser.treenodes.Relation;
import cs241.parser.treenodes.Statement;
import cs241.parser.treenodes.Variable;

public class Parser {
	
	Scanner scanner;
	Token token;
	int tokenCount;

	public Parser() {
		
	}

	public Computation parse(Reader stream) throws ParserException {
		scanner = new Scanner(stream);
		tokenCount = -1;
		nextToken();
		return parseComputation();
	}

	private void nextToken() {
		token = scanner.next();
		++tokenCount;
	}
	
	private String listTypes(Token.Type...types) {
		StringBuilder sb = new StringBuilder();
		
		for (Token.Type type : types) {
			sb.append(type.toString() + " ");
		}
		
		return sb.toString();
	}
	
	private Token.Type peekTokens(Token.Type... types) throws ParserException {
		if (token == null)
			throw new ParserException("Expected one of " + listTypes(types) + " (EOF reached) " + " line: " + scanner.getLineNumber());
		for (Token.Type type : types) {
			if (token.isType(type))
				return type;
		}
		throw new ParserException("Expected one of " + listTypes(types) + " line: " + scanner.getLineNumber());
	}
	
	private Token.Type parseTokens(Token.Type...types) throws ParserException {
		Token.Type type = peekTokens(types);
		nextToken();
		return type;
	}
	
	private boolean isTokens(Token.Type... types) throws ParserException {
		if (token == null)
			throw new ParserException("Syntax Error (EOF reached)");
		try {
			peekTokens(types);
		} catch (ParserException e) {
			return false;
		}
		return true;
	}
	
	private boolean isVariableDeclaration() throws ParserException {
		return isTokens(Token.Type.KEYWORD_VAR, Token.Type.KEYWORD_ARRAY);
	}
	
	private boolean isFunctionDeclaration() throws ParserException {
		return isTokens(Token.Type.KEYWORD_PROCEDURE, Token.Type.KEYWORD_FUNCTION);
	}
	
	Computation parseComputation() throws ParserException {
		ArrayList<Variable> variables = null;
		ArrayList<Function> functions = null;
		
		parseTokens(Token.Type.KEYWORD_MAIN);
		
		if (isVariableDeclaration()) {
			variables = new ArrayList<Variable>();
			do {
				variables.addAll(parseVariableDeclaration());
			} while (isVariableDeclaration());
		}
		
		if (isFunctionDeclaration()) {
			functions = new ArrayList<Function>();
			do {
				functions.add(parseFunction());
			} while (isFunctionDeclaration());
		}

		parseTokens(Token.Type.LEFT_CURLY);

		ArrayList<Statement> body = parseStatSequence();
		
		parseTokens(Token.Type.RIGHT_CURLY);
		parseTokens(Token.Type.PERIOD);
		
		if (token != null)
			throw new ParserException("Expected end of file");
		
		return new Computation(variables, functions, body);
	}
	
	ArrayList<Variable> parseVariableDeclaration() throws ParserException {
		ArrayList<Variable> variables = new ArrayList<Variable>();
		
		switch (parseTokens(Token.Type.KEYWORD_VAR, Token.Type.KEYWORD_ARRAY)) {
		case KEYWORD_VAR:
			variables.add(new Variable(parseIdentifier()));
			while (isTokens(Token.Type.COMMA)) {
				nextToken();
				variables.add(new Variable(parseIdentifier()));
			}
			break;
		
		case KEYWORD_ARRAY:
			ArrayList<Integer> dimensions = new ArrayList<Integer>();
			do {
				parseTokens(Token.Type.LEFT_BRACKET);
				dimensions.add(parseNumber());
				parseTokens(Token.Type.RIGHT_BRACKET);
			} while (isTokens(Token.Type.LEFT_BRACKET));
			
			variables.add(new Variable(parseIdentifier()));
			while (isTokens(Token.Type.COMMA)) {
				nextToken();
				variables.add(new Variable(parseIdentifier(), dimensions));
			}
			break;
		default:
			throw new ParserException("Expected variable declaration");
		}
		
		parseTokens(Token.Type.SEMICOLON);
		
		return variables;
	}
	
	Function parseFunction() throws ParserException {
		Function.Type type;
		switch (parseTokens(Token.Type.KEYWORD_PROCEDURE, Token.Type.KEYWORD_FUNCTION)) {
		case KEYWORD_PROCEDURE:
			type = Function.Type.PROCEDURE;
			break;
		case KEYWORD_FUNCTION:
			type = Function.Type.FUNCTION;
			break;
		default:
			throw new ParserException("Expected function declaration");
		}
		
		String name = parseIdentifier();
		ArrayList<String> parameters = null;
		
		if (isTokens(Token.Type.LEFT_PARANTHESIS)) {
			nextToken();
			if (!isTokens(Token.Type.RIGHT_PARANTHESIS)) {
				parameters = new ArrayList<String>();
				parameters.add(parseIdentifier());
				while (isTokens(Token.Type.COMMA)) {
					nextToken();
					parameters.add(parseIdentifier());
				}
			}
			parseTokens(Token.Type.RIGHT_PARANTHESIS);
		}

		parseTokens(Token.Type.SEMICOLON);
		
		ArrayList<Variable> variables = null;
		if (isVariableDeclaration()) {
			variables = new ArrayList<Variable>();
			do {
				variables.addAll(parseVariableDeclaration());
			} while (isVariableDeclaration());
		}
		
		parseTokens(Token.Type.LEFT_CURLY);
		
		ArrayList<Statement> body = parseStatSequence();
		
		parseTokens(Token.Type.RIGHT_CURLY);
		parseTokens(Token.Type.SEMICOLON);
		
		return new Function(type, name, parameters, variables, body);
	}
	
	ArrayList<Statement> parseStatSequence() throws ParserException {
		ArrayList<Statement> statements = new ArrayList<Statement>();
		statements.add(parseStatement());
		while (isTokens(Token.Type.SEMICOLON)) {
			nextToken();
			statements.add(parseStatement());
		}
		return statements;
	}
	
	Statement parseStatement() throws ParserException {
		Token.Type type = peekTokens(Token.Type.KEYWORD_LET, Token.Type.KEYWORD_CALL,
				Token.Type.KEYWORD_IF, Token.Type.KEYWORD_WHILE, Token.Type.KEYWORD_RETURN);
		switch (type) {
		case KEYWORD_LET:
			return parseAssignment();
		case KEYWORD_CALL:
			return parseFunctionCall();
		case KEYWORD_IF:
			return parseIfStatement();
		case KEYWORD_WHILE:
			return parseWhileStatement();
		case KEYWORD_RETURN:
			return parseReturnStatement();
		default:
			throw new ParserException("Expected Statement");
		}
	}
	
	Statement.Assignment parseAssignment() throws ParserException {
		parseTokens(Token.Type.KEYWORD_LET);
		Expression.Designator left = parseDesignator();
		parseTokens(Token.Type.ASSIGNMENT);
		Expression right = parseExpression();
		return new Statement.Assignment(left, right);
	}
	
	Statement.FunctionCall parseFunctionCall() throws ParserException {
		Expression.FunctionCall expression = parseFunctionCallExpression();
		return new Statement.FunctionCall(expression);
	}
	
	Statement.If parseIfStatement() throws ParserException {
		parseTokens(Token.Type.KEYWORD_IF);
		Relation condition = parseRelation();
		parseTokens(Token.Type.KEYWORD_THEN);
		ArrayList<Statement> thenBlock = parseStatSequence();
		ArrayList<Statement> elseBlock = null;
		if (isTokens(Token.Type.KEYWORD_ELSE)) {
			nextToken();
			elseBlock = parseStatSequence();
		}
		parseTokens(Token.Type.KEYWORD_FI);
		return new Statement.If(condition, thenBlock, elseBlock);
	}
	
	Statement.While parseWhileStatement() throws ParserException {
		parseTokens(Token.Type.KEYWORD_WHILE);
		Relation condition = parseRelation();
		parseTokens(Token.Type.KEYWORD_DO);
		ArrayList<Statement> block = parseStatSequence();
		parseTokens(Token.Type.KEYWORD_OD);
		return new Statement.While(condition, block);
	}
	
	Statement.Return parseReturnStatement() throws ParserException {
		parseTokens(Token.Type.KEYWORD_RETURN);
		return new Statement.Return(parseExpression());
	}
	
	Expression parseExpression() throws ParserException {
		Expression left = parseTerm();
		if (isTokens(Token.Type.ADDITION, Token.Type.SUBTRACTION)) {
			Token.Type type = parseTokens(Token.Type.ADDITION, Token.Type.SUBTRACTION);
			Expression.Binary.Operator op;
			if (type == Token.Type.ADDITION)
				op = Expression.Binary.Operator.ADDITION;
			else
				op = Expression.Binary.Operator.SUBTRACTION;
			Expression right = parseExpression();
			return new Expression.Binary(left, op, right);
		}
		else
			return left;
	}
	
	Expression parseTerm() throws ParserException {
		Expression left = parseFactor();
		if (isTokens(Token.Type.MULTIPLICATION, Token.Type.DIVISION)) {
			Token.Type type = parseTokens(Token.Type.MULTIPLICATION, Token.Type.DIVISION);
			Expression.Binary.Operator op;
			if (type == Token.Type.MULTIPLICATION)
				op = Expression.Binary.Operator.MULTIPLICATION;
			else
				op = Expression.Binary.Operator.DIVISION;
			Expression right = parseTerm();
			return new Expression.Binary(left, op, right);
		}
		else
			return left;
	}
	
	Expression parseFactor() throws ParserException {
		Token.Type type = peekTokens(Token.Type.IDENTIFIER, Token.Type.NUMBER, Token.Type.LEFT_PARANTHESIS, Token.Type.KEYWORD_CALL);
		switch (type) {
		case IDENTIFIER:
			return parseDesignator();
		case NUMBER:
			return new Expression.Number(parseNumber());
		case LEFT_PARANTHESIS:
			parseTokens(Token.Type.LEFT_PARANTHESIS);
			Expression expression = parseExpression();
			parseTokens(Token.Type.RIGHT_PARANTHESIS);
			return expression;
		case KEYWORD_CALL:
			return parseFunctionCallExpression();
		default:
			throw new ParserException("Expected Expression");
		}
	}
	
	Expression.Designator parseDesignator() throws ParserException {
		String name = parseIdentifier();
		ArrayList<Expression> indices = null;
		while (isTokens(Token.Type.LEFT_BRACKET)) {
			if (indices == null)
				indices = new ArrayList<Expression>();
			nextToken();
			indices.add(parseExpression());
			parseTokens(Token.Type.RIGHT_BRACKET);
		}
		return new Expression.Designator(name, indices);
	}
	
	Expression.FunctionCall parseFunctionCallExpression() throws ParserException {
		parseTokens(Token.Type.KEYWORD_CALL);
		String name = parseIdentifier();
		ArrayList<Expression> arguments = null;
		if (isTokens(Token.Type.LEFT_PARANTHESIS)) {
			nextToken();
			if (!isTokens(Token.Type.RIGHT_PARANTHESIS)) {
				arguments = new ArrayList<Expression>();
				arguments.add(parseExpression());
				while (isTokens(Token.Type.COMMA)) {
					nextToken();
					arguments.add(parseExpression());
				}
			}
			parseTokens(Token.Type.RIGHT_PARANTHESIS);
		}
		return new Expression.FunctionCall(name, arguments);
	}
	
	Relation parseRelation() throws ParserException {
		Expression left = parseExpression();
		Token.Type type = parseTokens(Token.Type.EQUALS, Token.Type.NOTEQUALS,
				Token.Type.LESSTHAN, Token.Type.LESSTHANEQ, Token.Type.GREATERTHAN, Token.Type.GREATERTHANEQ);
		Relation.Operator op;
		switch (type) {
		case EQUALS:
			op = Relation.Operator.EQUALS;
			break;
		case NOTEQUALS:
			op = Relation.Operator.NOTEQUALS;
			break;
		case LESSTHAN:
			op = Relation.Operator.LESSTHAN;
			break;
		case LESSTHANEQ:
			op = Relation.Operator.LESSTHANEQ;
			break;
		case GREATERTHAN:
			op = Relation.Operator.GREATERTHAN;
			break;
		case GREATERTHANEQ:
			op = Relation.Operator.GREATERTHANEQ;
			break;
		default:
			throw new ParserException("Expection relational operator");
		}
		Expression right = parseExpression();
		return new Relation(left, op, right);
	}
	
	int parseNumber() throws ParserException {
		int value = 0;
		if (isTokens(Token.Type.NUMBER)) {
			value = token.getValue();
			nextToken();
		} else
			throw new ParserException("Expected Number");
		return value;
	}
	
	String parseIdentifier() throws ParserException {
		String name;
		if (isTokens(Token.Type.IDENTIFIER)) {
			name = token.getName();
			nextToken();
		} else
			throw new ParserException("Expected Identifer");
		return name;
	}
}
