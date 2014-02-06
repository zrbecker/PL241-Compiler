package cs241;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cs241.Argument.BBID;
import cs241.Argument.InstructionID;
import cs241.Argument.Value;
import cs241.Argument.ArrayName;
import cs241.Instruction.InstructionType;
import cs241.parser.Parser;
import cs241.parser.ParserException;
import cs241.parser.treenodes.Computation;
import cs241.parser.treenodes.Expression;
import cs241.parser.treenodes.Expression.Binary;
import cs241.parser.treenodes.Expression.Binary.BinaryOperator;
import cs241.parser.treenodes.Expression.Designator;
import cs241.parser.treenodes.Expression.FunctionCallExp;
import cs241.parser.treenodes.Expression.Number;
import cs241.parser.treenodes.Relation;
import cs241.parser.treenodes.Relation.RelationOperator;
import cs241.parser.treenodes.Statement;
import cs241.parser.treenodes.Statement.Assignment;
import cs241.parser.treenodes.Statement.FunctionCall;
import cs241.parser.treenodes.Statement.If;
import cs241.parser.treenodes.Statement.Return;
import cs241.parser.treenodes.Statement.While;

public class Compiler {
	File inputFile;
	File outputFile;
	BasicBlock mainRoot;
	Parser parser;

	public Compiler(File in, File out) {
		mainRoot = new BasicBlock();
		inputFile = in;
		outputFile = out;
		parser = new Parser();
	}

	public void compile() throws FileNotFoundException {
		Computation c = getParseTree();
		if(c == null)
			System.out.println("Parsing error. Terminating.");
		
		//TODO: worry about variable and function parts
		
		List<Statement> mainCode = c.getBody();
		compileIntoBBs(mainCode,mainRoot);
	}
	
	public Computation getParseTree() throws FileNotFoundException {
		Reader reader = new FileReader(inputFile);
		try {
			Computation c = parser.parse(reader);
			return c;
		} catch (ParserException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/*
	 * Compiles code into a basic block structure
	 */
	//TODO: handle passing around var lookup table and phi instructions
	public void compileIntoBBs(List<Statement> code, BasicBlock currBB) {
		for (Statement curr: code) {
			if(curr instanceof Assignment) {
				Assignment currAssignment = (Assignment) curr;
				Designator des = currAssignment.getLeft();
				
				//Compile the expression and implicitly assign the variable to arg
				Argument arg = compileExpression(currAssignment.getRight(),currBB);
				
				if(des.getIndices().size() == 0 && arg instanceof InstructionID) {
					String varName = currAssignment.getLeft().getName();
					currBB.varLookupTable.put(varName, arg);
				} else {
					//TODO: handle arrays
					System.out.println("Error cannot assign to arrays yet.");
				}
			} else if (curr instanceof FunctionCall) {
				FunctionCall currFC = (FunctionCall) curr;
				
				BasicBlock afterCall = new BasicBlock();
				
				//TODO: compile the function call
				System.out.println("Error cannot call functions yet.");
			} else if (curr instanceof If) {
				If currIf = (If)curr;
				//TODO: handle empty else block
			
				BasicBlock ifThen = new BasicBlock();
				BasicBlock ifElse = new BasicBlock();
				BasicBlock afterIf = new BasicBlock();
				
				//Set up parent/child relationship and dominator tree
				List<BasicBlock> oldChildren = currBB.children;
				currBB.children = new ArrayList<BasicBlock>();
				currBB.addChild(ifThen);
				currBB.addChild(ifElse);
				ifThen.addChild(afterIf);
				ifElse.addChild(afterIf);
				afterIf.children = oldChildren;
				
				currBB.addDominated(ifThen);
				currBB.addDominated(ifElse);
				currBB.addDominated(afterIf);
				
				ifThen.setDominator(currBB);
				ifElse.setDominator(currBB);
				afterIf.setDominator(currBB);
				
				BasicBlock next = currBB.next;
				currBB.next = ifThen;
				ifThen.prev = currBB;
				ifThen.next = ifElse;
				ifElse.prev = ifThen;
				ifElse.next = afterIf;
				afterIf.prev = ifElse;
				afterIf.next = next;
				if(next != null)
					next.prev = afterIf;

				//Compile the condition for the if
				BBID ifElseID = new BBID(ifElse.bbID);
				compileCondition(currIf.getCondition(),currBB, ifElseID);
				
				//Compile the branches
				compileIntoBBs(currIf.getThenBlock(), ifThen);
				compileIntoBBs(currIf.getElseBlock(), ifElse);
				
				//Add the branch from the ifthen to after the ifelse
				BBID afterIfID = new BBID(afterIf.bbID);
				Argument[] branchArgs = {afterIfID};
				Instruction i = new Instruction(InstructionType.BRA,branchArgs);
				ifThen.appendInstruction(i);
				
				//TODO: handle phi instructions
				
				currBB = afterIf;
			} else if (curr instanceof While) {
				While currWhile = (While) curr;
				
				BasicBlock condition = new BasicBlock();
				BasicBlock loop = new BasicBlock();
				BasicBlock afterLoop = new BasicBlock();
				
				//Set up child relationship and dominator tree
				List<BasicBlock> oldChildren = currBB.children;
				currBB.children = new ArrayList<BasicBlock>();
				currBB.addChild(condition);
				condition.addChild(loop);
				condition.addChild(afterLoop);
				loop.addChild(condition);
				afterLoop.children = oldChildren;
				
				currBB.addDominated(condition);
				condition.addDominated(loop);
				condition.addDominated(afterLoop);
				
				condition.setDominator(currBB);
				loop.setDominator(condition);
				afterLoop.setDominator(condition);
				
				BasicBlock next = currBB.next;
				currBB.next = condition;
				condition.prev = currBB;
				condition.next = loop;
				loop.prev = condition;
				loop.next = afterLoop;
				afterLoop.prev = loop;
				afterLoop.next = next;
				if(next != null)
					next.prev = afterLoop;
				
				//Compile the condition for the while loop
				BBID afterLoopID = new BBID(afterLoop.bbID);
				compileCondition(currWhile.getCondition(),condition, afterLoopID);
				
				//Compile loop block
				compileIntoBBs(currWhile.getBlock(), loop);
				
				//Have the loop branch back to the condition
				BBID conditionID = new BBID(condition.bbID);
				Argument[] branchArgs = {conditionID};
				Instruction i = new Instruction(InstructionType.BRA,branchArgs);
				loop.appendInstruction(i);
				
				//TODO: handle phi instructions
				
				currBB = afterLoop;
			} else if (curr instanceof Return) {
				Return currReturn = (Return) curr;
				//TODO: compile return statement
			} else {
				System.out.println("Error unexpected statement type.");
			}
		}
	}
	/*
	 * Compile a condition into instructions, will branch on false to branchLocation
	 * Return value: the line with the branch call that needs to be fixed up
	 */
	public void compileCondition(Relation c, BasicBlock bb, Argument branchLocation) {
		//Compile the two sides of the condition
		Argument idLeft = compileExpression(c.getLeft(),bb);
		Argument idRight = compileExpression(c.getRight(),bb);
		Argument[] cmpArgs = {idLeft,idRight};
		
		Instruction compare = new Instruction(InstructionType.CMP,cmpArgs);
		bb.appendInstruction(compare);
		
		RelationOperator o = c.getOperator();
		Argument[] branchLocArr = {branchLocation};
		InstructionType it = null;
		switch(o) {
		case EQUALS:
			it = InstructionType.BNE;
			break;
		case NOTEQUALS:
			it = InstructionType.BEQ;
			break;
		case LESSTHAN:
			it = InstructionType.BGE;
			break;
		case LESSTHANEQ:
			it = InstructionType.BGT;
			break;
		case GREATERTHAN:
			it = InstructionType.BLE;
			break;
		case GREATERTHANEQ:
			it =InstructionType.BLT;
			break;
		default:
			System.out.println("Error unexpected comparision operator.");
		}
		Instruction i = new Instruction(it,branchLocArr);
		bb.appendInstruction(i);
	}
	
	/*
	 * Compiles a math expression into several instructions
	 * Return value: instruction id that computed the final value
	 */
	public Argument compileExpression(Expression e, BasicBlock bb) {
		Argument arg = null;
		if(e instanceof Binary) {
			Binary eBin = (Binary) e;
			
			//Compile the two sides of the binary operator
			Argument leftArg = compileExpression(eBin.getLeft(), bb);
			Argument rightArg = compileExpression(eBin.getRight(), bb);
			Argument[] args = {leftArg,rightArg};
			
			//Get the instruction type for the binary operator
			BinaryOperator o = eBin.getOperator();
			InstructionType it = null;
			switch(o) {
			case ADDITION:
				it = InstructionType.ADD;
				break;
			case SUBTRACTION:
				it = InstructionType.SUB;
				break;
			case MULTIPLICATION:
				it = InstructionType.MUL;
				break;
			case DIVISION:
				it = InstructionType.DIV;
				break;
			default:
					System.out.println("Error unexpected binary operator.");
			}
			
			//Combine the two sides of the expression with an instruction
			Instruction i = new Instruction(it,args);
			bb.appendInstruction(i);
			arg = new InstructionID(i.instructionID);
		} else if (e instanceof Number) {
			Number eNum = (Number) e;
			
			//Numbers do not need any compilation
			arg = new Value(eNum.getValue());
		} else if (e instanceof Designator) {
			Designator eDes = (Designator)e;
			
			//Create an argument list for the load
			List<Argument> args = new ArrayList<Argument>();
			args.add(new ArrayName(eDes.getName()));
			for(Expression exp : eDes.getIndices()) {
				args.add(compileExpression(exp,bb));
			}
			Argument[] argsArr = new Argument[args.size()];
			args.toArray(argsArr);
			
			//Decide whether it is a known variable, unknown variable, or array load
			Instruction i = null;
			if(argsArr.length == 1) {
				Argument id = bb.varLookupTable.get(((ArrayName)argsArr[0]).getName());
				if(id == null) {
					//Unknown variable
					i = new Instruction(InstructionType.LOAD, argsArr);
					bb.appendInstruction(i);
					arg = new InstructionID(i.instructionID);
				} else {
					//Known variable
					arg = id;
				}
			} else {
				//Array load
				i = new Instruction(InstructionType.LOADADD, argsArr);
				bb.appendInstruction(i);
				arg = new InstructionID(i.instructionID);
			}
		} else if (e instanceof FunctionCallExp) {
			//TODO: handle function calls
		} else {
			System.out.println("Error unexpected expression type.");
		}
		return arg;
	}
	

}
