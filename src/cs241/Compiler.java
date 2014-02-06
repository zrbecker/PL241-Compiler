package cs241;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.List;

import cs241.Instruction.InstructionType;
import cs241.parser.Parser;
import cs241.parser.ParserException;
import cs241.parser.treenodes.Computation;
import cs241.parser.treenodes.Expression;
import cs241.parser.treenodes.Relation;
import cs241.parser.treenodes.Relation.Operator;
import cs241.parser.treenodes.Statement;
import cs241.parser.treenodes.Statement.Assignment;
import cs241.parser.treenodes.Statement.FunctionCall;
import cs241.parser.treenodes.Statement.If;
import cs241.parser.treenodes.Statement.While;
import cs241.parser.treenodes.Statement.Return;

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
	
	public void compileIntoBBs(List<Statement> code, BasicBlock currBB) {
		for (Statement curr: code) {
			if(curr instanceof Assignment) {
				Assignment currAssignment = (Assignment) curr;
				
				//Compile the expression and implicitly assign the variable
				int id = compileExpression(currAssignment.getRight(),currBB);
				
				if(currAssignment.getLeft().getIndices().size() == 0) {
					String varName = currAssignment.getLeft().getName();
					currBB.varLookupTable.put(varName, id);
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
			
				BasicBlock ifThen = new BasicBlock();
				BasicBlock ifElse = new BasicBlock();
				BasicBlock afterIf = new BasicBlock();
				
				//Set up parent/child relationship and dominator tree
				currBB.addChild(ifThen);
				currBB.addChild(ifElse);
				ifThen.addChild(afterIf);
				ifElse.addChild(afterIf);
				
				ifThen.addParent(currBB);
				ifElse.addParent(currBB);
				afterIf.addParent(ifThen);
				afterIf.addParent(ifElse);
				
				currBB.addDominated(ifThen);
				currBB.addDominated(ifElse);
				currBB.addDominated(afterIf);
				
				ifThen.setDominator(currBB);
				ifElse.setDominator(currBB);
				afterIf.setDominator(currBB);

				//Compile the condition for the while loop
				int jumpInsID = compileCondition(currIf.getCondition(),currBB);
				//Compile the branches
				compileIntoBBs(currIf.getThenBlock(), ifThen);
				compileIntoBBs(currIf.getElseBlock(), ifElse);
				
				//Fix the branch to the else code
				//TODO: code does not work if empty loop block
				int branchLocID = ifElse.instructions.get(0).instructionID;
				Instruction.getInstructionByID(jumpInsID).args[0] = branchLocID;
				
				//Fix up the ifthen to branch past the else
				//TODO: worry about no else block
				int[] branchArgs = {0};
				Instruction i = new Instruction(InstructionType.BRA,branchArgs);
				i.args[0] = Instruction.nextInstructionID();
				ifThen.appendInstruction(i);
				
				//TODO: handle phi instructions
				
				currBB = afterIf;
			} else if (curr instanceof While) {
				While currWhile = (While) curr;
				
				BasicBlock condition = new BasicBlock();
				BasicBlock loop = new BasicBlock();
				BasicBlock afterLoop = new BasicBlock();
				
				//Set up parent/child relationship and dominator tree
				currBB.addChild(condition);
				condition.addChild(loop);
				condition.addChild(afterLoop);
				loop.addChild(condition);
				
				condition.addParent(currBB);
				loop.addParent(condition);
				afterLoop.addParent(condition);
				condition.addParent(loop);
				
				currBB.addDominated(condition);
				condition.addDominated(loop);
				condition.addDominated(afterLoop);
				
				condition.setDominator(currBB);
				loop.setDominator(condition);
				afterLoop.setDominator(condition);
				
				//Compile the condition for the while loop
				int jumpInsID = compileCondition(currWhile.getCondition(),condition);
				//Compile loop block
				compileIntoBBs(currWhile.getBlock(), loop);
				
				//Have the loop branch back to the condition
				int conditionInsID = condition.instructions.get(0).instructionID;
				int[] branchArgs = {conditionInsID};
				Instruction i = new Instruction(InstructionType.BRA,branchArgs);
				loop.appendInstruction(i);
				
				//FIxup the condition branch to go past the loop
				//TODO: code does not work if empty afterloop block
				//TODO: maybe assign bbs different ids and fix up later???
				int branchLocID = Instruction.nextInstructionID();
				Instruction.getInstructionByID(jumpInsID).args[0] = branchLocID;
				
				//TODO: handle phi instructions
				
				currBB = afterLoop;
			} else if (curr instanceof Return) {
				//TODO: compile return statement
			} else {
				System.out.println("Error unexpected statement type.");
			}
		}
	}
	/*
	 * Compile a condition into instructions
	 * Return value: the line with the branch call that needs to be fixed up
	 */
	public int compileCondition(Relation c, BasicBlock bb) {
		int idLeft = compileExpression(c.getLeft(),bb);
		int idRight = compileExpression(c.getRight(),bb);
		int[] cmpArgs = {idLeft,idRight};
		
		Instruction whileCompare = new Instruction(InstructionType.CMP,cmpArgs);
		bb.appendInstruction(whileCompare);
		
		Operator o = c.getOperator();
		int[] branchLoc = {-1};
		Instruction i = null;
		switch(o) {
		case EQUALS:
			i = new Instruction(InstructionType.BNE, branchLoc);
			break;
		case NOTEQUALS:
			i = new Instruction(InstructionType.BEQ, branchLoc);
			break;
		case LESSTHAN:
			i = new Instruction(InstructionType.BGE, branchLoc);
			break;
		case LESSTHANEQ:
			i = new Instruction(InstructionType.BGT, branchLoc);
			break;
		case GREATERTHAN:
			i = new Instruction(InstructionType.BLE, branchLoc);
			break;
		case GREATERTHANEQ:
			i = new Instruction(InstructionType.BLT, branchLoc);
			break;
		default:
			System.out.println("Error unexpected comparision operator.");
		}
		bb.appendInstruction(i);
		return i.instructionID;
	}
	
	/*
	 * Compiles a math expression into several instructions
	 * Return value: instruction id that computed the final value
	 */
	public int compileExpression(Expression e, BasicBlock bb) {
		
		return 0;
	}
	

}
