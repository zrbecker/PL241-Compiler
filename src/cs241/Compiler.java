package cs241;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cs241.Argument.ArrayName;
import cs241.Argument.BasicBlockID;
import cs241.Argument.FunctionName;
import cs241.Argument.InstructionID;
import cs241.Argument.Value;
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
import cs241.parser.treenodes.Function;
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
		
		//TODO: worry about variable and function param/variable parts
		//Compile the functions present in the code
		List<BasicBlock> functionBBs = new ArrayList<BasicBlock>();
		if(c.getFunctions() != null) {
			for(Function func : c.getFunctions()) {
				List<Statement> funcBody = func.getBody();
				BasicBlock funcHead = new BasicBlock();
				compileIntoBBs(funcBody, funcHead);
				functionBBs.add(funcHead);
			}
		}
		
		//Compile the main statement
		List<Statement> mainCode = c.getBody();
		BasicBlock lastBlock = compileIntoBBs(mainCode,mainRoot);
		lastBlock.appendInstruction(Instruction.makeInstruction(InstructionType.END,new Argument[0]));
		
		System.out.println(mainRoot);
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
	public BasicBlock compileIntoBBs(List<Statement> code, BasicBlock currBB) {
		for (Statement curr: code) {
			if(curr instanceof Assignment) {
				Assignment currAssignment = (Assignment) curr;
				Designator des = currAssignment.getLeft();
				
				//Compile the expression and implicitly assign the variable to arg
				Argument arg = compileExpression(currAssignment.getRight(),currBB);
				arg.var = des.getName();
				
				if((des.getIndices() == null || des.getIndices().size() == 0) 
						&& (arg instanceof InstructionID || arg instanceof Value)) {
					currBB.varLookupTable.put(arg.var, arg);
					currBB.changedVariables.add(arg.var);
				} else {
					//TODO: handle arrays
					System.out.println("Error cannot assign to arrays yet.");
				}
			} else if (curr instanceof FunctionCall) {
				FunctionCall currFC = (FunctionCall) curr;
	
				//TODO:handle function calls right
				//Compile arguments to the function
				List<Argument> args = new LinkedList<Argument>();
				args.add(new FunctionName(currFC.getName()));
				for(Expression exp : currFC.getArguments()) {
					args.add(compileExpression(exp,currBB));
				}
				
				//Add the function call instruction
				Argument[] argsArr = new Argument[args.size()];
				args.toArray(argsArr);
				currBB.appendInstruction(Instruction.makeInstruction(InstructionType.FUNCTION,argsArr));
			} else if (curr instanceof If) {
				If currIf = (If)curr;
				boolean thenOnly = currIf.getElseBlock() == null || currIf.getElseBlock().size() == 0;

				BasicBlock ifThen = new BasicBlock();
				ifThen.varLookupTable = new HashMap<String,Argument>(currBB.varLookupTable);
				BasicBlock ifElse = new BasicBlock();
				ifElse.varLookupTable = new HashMap<String,Argument>(currBB.varLookupTable);
				BasicBlock afterIf = new BasicBlock();
				
				//Set up parent/child relationship
				List<BasicBlock> oldChildren = currBB.children;
				currBB.children = new ArrayList<BasicBlock>();
				currBB.addChild(ifThen);
				if(!thenOnly)
					currBB.addChild(ifElse);
				ifThen.addChild(afterIf);
				if(!thenOnly)
					ifElse.addChild(afterIf);
				afterIf.children = oldChildren;
				
				ifThen.addParent(currBB);
				if(!thenOnly)
					ifElse.addParent(currBB);
				afterIf.addParent(ifThen);
				if(!thenOnly)
					afterIf.addParent(ifElse);
				for(BasicBlock child : oldChildren) {
					child.parents.remove(currBB);
					child.addParent(afterIf);
				}
				
				//Set up dominator tree
				currBB.addDominated(ifThen);
				if(!thenOnly)
					currBB.addDominated(ifElse);
				currBB.addDominated(afterIf);
				
				ifThen.setDominator(currBB);
				if(!thenOnly)
					ifElse.setDominator(currBB);
				afterIf.setDominator(currBB);
				
				//Set up linear instruction order linked list
				BasicBlock next = currBB.next;
				currBB.next = ifThen;
				ifThen.prev = currBB;
				if(thenOnly) {
					ifThen.next = afterIf;
					afterIf.prev = ifThen;
				} else {
					ifThen.next = ifElse;
					ifElse.prev = ifThen;
					ifElse.next = afterIf;
					afterIf.prev = ifElse;
				}
				afterIf.next = next;
				if(next != null)
					next.prev = afterIf;

				//Compile the condition for the if
				BasicBlockID branchID = thenOnly ? afterIf.bbID : ifElse.bbID;
				compileCondition(currIf.getCondition(),currBB, branchID);
				
				//Compile the branches
				BasicBlock lastIfThenBlock = compileIntoBBs(currIf.getThenBlock(), ifThen);
				BasicBlock lastIfElseBlock = ifElse;
				//Add the branch from the ifthen to after the ifelse
				if(!thenOnly) {
					lastIfThenBlock.appendInstruction(Instruction.makeInstruction(InstructionType.BRA,afterIf.bbID));
					lastIfElseBlock = compileIntoBBs(currIf.getElseBlock(), ifElse);
				}
				
				//Get the list of variables that were assigned in the banches
				Set<String> changedVars = new HashSet<String>();
				changedVars.addAll(ifThen.changedVariables);
				if(!thenOnly)
					changedVars.addAll(ifElse.changedVariables);
				
				//Create phi instructions
				Map<String,Argument> newVarLookupTable = new HashMap<String,Argument>(currBB.varLookupTable);
				for(String var : changedVars) {
					Argument loc1 = ifThen.varLookupTable.get(var);
					Argument loc2 = ifElse.varLookupTable.get(var);
					
					Instruction i = Instruction.makeInstruction(InstructionType.PHI,var,loc1,loc2);
					newVarLookupTable.put(var, i.instructionID);
					afterIf.appendInstruction(i);
				}
				
				//Set up the right variable lookups and changed variable list
				afterIf.varLookupTable = newVarLookupTable;
				changedVars.addAll(currBB.changedVariables);
				afterIf.changedVariables = changedVars;
				
				currBB = afterIf;
			} else if (curr instanceof While) {
				While currWhile = (While) curr;
				
				BasicBlock condition = new BasicBlock();
				BasicBlock loop = new BasicBlock();
				BasicBlock afterLoop = new BasicBlock();
				
				//Set up parent/child relationship
				List<BasicBlock> oldChildren = currBB.children;
				currBB.children = new ArrayList<BasicBlock>();
				currBB.addChild(condition);
				condition.addChild(loop);
				condition.addChild(afterLoop);
				loop.addChild(condition);
				afterLoop.children = oldChildren;
				
				condition.addParent(currBB);
				loop.addParent(condition);
				afterLoop.addParent(condition);
				condition.addParent(loop);
				for(BasicBlock child : oldChildren) {
					child.parents.remove(currBB);
					child.addParent(afterLoop);
				}
				
				//Set up dominator tree
				currBB.addDominated(condition);
				condition.addDominated(loop);
				condition.addDominated(afterLoop);
				
				condition.setDominator(currBB);
				loop.setDominator(condition);
				afterLoop.setDominator(condition);
				
				//Set up linear instruction order linked list
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
				compileCondition(currWhile.getCondition(),condition, afterLoop.bbID);
				
				//Compile loop block
				BasicBlock lastLoopBlock = compileIntoBBs(currWhile.getBlock(), loop);
				
				//Have the loop branch back to the condition
				lastLoopBlock.appendInstruction(Instruction.makeInstruction(InstructionType.BRA,condition.bbID));

				//TODO: remove println
				System.out.println("Warning: while loop phis not fully implemented yet.");

				//Set up the phi instructions and variable lookup table
				Set<String> changedVars = new HashSet<String>();
				changedVars.addAll(lastLoopBlock.changedVariables);
				Map<String,Argument> newVarLookupTable = new HashMap<String,Argument>(currBB.varLookupTable);
				for(String var : changedVars) {
					Argument loc1 = lastLoopBlock.varLookupTable.get(var);
					Argument loc2 = currBB.varLookupTable.get(var);
					
					Instruction i = Instruction.makeInstruction(InstructionType.PHI,var,loc1,loc2);
					newVarLookupTable.put(var, i.instructionID);
					condition.prependInstruction(i);
					//TODO: fixup old references to loc2,var in the loop
				}
				changedVars.addAll(currBB.changedVariables);
				afterLoop.changedVariables = changedVars;
				afterLoop.varLookupTable = newVarLookupTable;
				
				currBB = afterLoop;
			} else if (curr instanceof Return) {
				Return currReturn = (Return) curr;
				//TODO: compile return statement
				System.out.println("Error return statements not yet implemented.");
			} else {
				System.out.println("Error unexpected statement type.");
			}
		}
		return currBB;
	}
	/*
	 * Compile a condition into instructions, will branch on false to branchLocation
	 * Return value: the line with the branch call that needs to be fixed up
	 */
	public void compileCondition(Relation c, BasicBlock bb, Argument branchLocation) {
		//Compile the two sides of the condition
		Argument idLeft = compileExpression(c.getLeft(),bb);
		Argument idRight = compileExpression(c.getRight(),bb);
		
		bb.appendInstruction(Instruction.makeInstruction(InstructionType.CMP,idLeft,idRight));
		
		RelationOperator o = c.getOperator();
		InstructionType it;
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
			it = null;
			System.out.println("Error unexpected comparision operator.");
		}
		bb.appendInstruction(Instruction.makeInstruction(it,branchLocation));
	}
	
	/*
	 * Compiles a math expression into several instructions
	 * Return value: instruction id that computed the final value
	 */
	public Argument compileExpression(Expression e, BasicBlock bb) {
		Argument arg;
		if(e instanceof Binary) {
			Binary eBin = (Binary) e;
			arg = compileBinaryExpression(eBin, bb);
		} else if (e instanceof Number) {
			Number eNum = (Number) e;
			//Numbers do not need any compilation
			arg = new Value(eNum.getValue(), "#");
		} else if (e instanceof Designator) {
			Designator eDes = (Designator)e;
			arg = compileDesignator(eDes, bb);
		} else if (e instanceof FunctionCallExp) {
			FunctionCallExp eFCE = (FunctionCallExp) e;
			arg = compileFunctionCallExp(eFCE, bb);
		} else {
			arg = null;
			System.out.println("Error unexpected expression type.");
		}
		return arg;
	}
	
	public Argument compileBinaryExpression(Binary bin, BasicBlock bb) {
		//Compile the two sides of the binary operator
		Argument leftArg = compileExpression(bin.getLeft(), bb);
		Argument rightArg = compileExpression(bin.getRight(), bb);
		BinaryOperator o = bin.getOperator();
		
		Argument arg = null;//TODO: value propogation temporarily disabled
		if(false && leftArg instanceof Value && rightArg instanceof Value) {
			int l = ((Value) leftArg).getValue();
			int r = ((Value) rightArg).getValue();
			
			int exp;
			switch(o) {
			case ADDITION:
				exp = l+r;
				break;
			case SUBTRACTION:
				exp = l-r;
				break;
			case MULTIPLICATION:
				exp = l*r;
				break;
			case DIVISION:
				exp = l/r;
				break;
			default:
				exp = Integer.MIN_VALUE;
				System.out.println("Error unexpected binary operator.");
			}
			
			arg = new Value(exp,"#");
		} else {
			//Get the instruction type for the binary operator
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
			Instruction i = Instruction.makeInstruction(it,leftArg,rightArg);
			bb.appendInstruction(i);
			arg = i.instructionID;
		}
		return arg;
	}

	public Argument compileDesignator(Designator des, BasicBlock bb) {
		//Create an argument list for the load
		List<Argument> args = new ArrayList<Argument>();
		args.add(new ArrayName(des.getName()));
		if(des.getIndices() != null) {
			for(Expression exp : des.getIndices()) {
				args.add(compileExpression(exp,bb));
			}
		}
		Argument[] argsArr = new Argument[args.size()];
		args.toArray(argsArr);
		
		//Decide whether it is a known variable, unknown variable, or array load
		//TODO: something is weird here need to think more about behavior
		Instruction i = null;
		if(argsArr.length == 1) {
			Argument id = bb.varLookupTable.get(((ArrayName)argsArr[0]).getName());
			if(id == null) {
				//Unknown variable
				i = Instruction.makeInstruction(InstructionType.LOAD, des.getName(), argsArr);
				bb.appendInstruction(i);
				return i.instructionID;
			} else {
				//Known variable
				return id;
			}
		} else {
			//Array load
			i = Instruction.makeInstruction(InstructionType.LOADADD, des.getName(), argsArr);
			bb.appendInstruction(i);
			return i.instructionID;
		}
	}
	
	public Argument compileFunctionCallExp(FunctionCallExp fce, BasicBlock bb) {
		//TODO: handle function calls correctly
		//Create an argument list for the function call
		List<Argument> args = new LinkedList<Argument>();
		args.add(new FunctionName(fce.getName()));
		for(Expression exp : fce.getArguments()) {
			args.add(compileExpression(exp,bb));
		}
		
		Argument[] argsArr = new Argument[args.size()];
		args.toArray(argsArr);
		Instruction i = Instruction.makeInstruction(InstructionType.FUNCTION,argsArr);
		bb.appendInstruction(i);
		return i.instructionID;
	}
	
	//TODO: implement this function
	public Argument[] compileArgsToArray(List<Expression> exps, Argument first, BasicBlock bb) {
		List<Argument> args = new LinkedList<Argument>();
		args.add(first);
		for(Expression exp : exps) {
			args.add(compileExpression(exp,bb));
		}
		Argument[] argsArr = new Argument[args.size()];
		return args.toArray(argsArr);
	}
}
