package cs241;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import cs241.Argument.BasicBlockID;
import cs241.Argument.DesName;
import cs241.Argument.FunctionName;
import cs241.Argument.InstructionID;
import cs241.Argument.Value;
import cs241.DefUseChain.DefUse;
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
	Parser parser;
	BasicBlock mainRoot;
	DefUseChain duchain;

	public Compiler(File in, File out) {
		inputFile = in;
		outputFile = out;
		parser = new Parser();
		mainRoot = new BasicBlock();
		duchain = new DefUseChain();
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
				String var = des.getName();
				
				if((des.getIndices() == null || des.getIndices().size() == 0) 
						&& (arg instanceof InstructionID || arg instanceof Value)) {
					//Implicit load instruction to make a def
					Instruction i = Instruction.makeInstruction(InstructionType.STORE, arg);
					//currBB.appendInstruction(i);
					currBB.updateVariable(var, arg, i.getID());
				} else {
					//TODO: handle arrays correctly
					Argument[] args = compileArgsToArray(des.getIndices(), new DesName(des.getName()), currBB);
					currBB.appendInstruction(Instruction.makeInstruction(InstructionType.STORE, args));
					
					System.out.println("Error cannot assign to arrays yet.");
				}
			} else if (curr instanceof FunctionCall) {
				FunctionCall currFC = (FunctionCall) curr;
	
				//TODO:handle function calls right
				//Compile arguments to the function
				Argument[] args = compileArgsToArray(currFC.getArguments(), new FunctionName(currFC.getName()), currBB);

				//Add the faked function call instruction
				currBB.appendInstruction(Instruction.makeInstruction(InstructionType.FUNCTION, args));
			} else if (curr instanceof If) {
				If currIf = (If)curr;
				boolean thenOnly = currIf.getElseBlock() == null || currIf.getElseBlock().size() == 0;

				BasicBlock ifThen = new BasicBlock();
				ifThen.copyVarTablesFrom(currBB);
				BasicBlock ifElse = new BasicBlock();
				ifElse.copyVarTablesFrom(currBB);
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
				BasicBlockID branchID = thenOnly ? afterIf.getID() : ifElse.getID();
				compileCondition(currIf.getCondition(),currBB, branchID);
				
				//Compile the branches
				BasicBlock lastIfThenBlock = compileIntoBBs(currIf.getThenBlock(), ifThen);
				BasicBlock lastIfElseBlock = ifElse;
				
				//Add the branch from the ifthen to after the ifelse
				if(!thenOnly) {
					lastIfThenBlock.appendInstruction(Instruction.makeInstruction(InstructionType.BRA,afterIf.getID()));
					lastIfElseBlock = compileIntoBBs(currIf.getElseBlock(), ifElse);
				}
				
				//Get the list of variables that were assigned in the banches
				Set<String> changedVars = new HashSet<String>();
				changedVars.addAll(lastIfThenBlock.getChangedVars());
				if(!thenOnly)
					changedVars.addAll(lastIfElseBlock.getChangedVars());
				
				//Create phi instructions
				afterIf.copyAllTablesFrom(currBB);
				for(String var : changedVars) {
					Argument loc1 = lastIfThenBlock.getVarArg(var);
					Argument loc2 = lastIfElseBlock.getVarArg(var);
					
					Instruction i = Instruction.makeInstruction(InstructionType.PHI,loc1,loc2);
					afterIf.updateVariable(var, i.getID(), i.getID());
					afterIf.appendInstruction(i);
				}
				
				currBB = afterIf;
			} else if (curr instanceof While) {
				While currWhile = (While) curr;
				
				BasicBlock condition = new BasicBlock();
				condition.copyVarTablesFrom(currBB);
				BasicBlock loop = new BasicBlock();
				loop.copyVarTablesFrom(currBB);
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
				compileCondition(currWhile.getCondition(),condition, afterLoop.getID());
				
				//Compile loop block
				BasicBlock lastLoopBlock = compileIntoBBs(currWhile.getBlock(), loop);
				
				//Have the loop branch back to the condition
				lastLoopBlock.appendInstruction(Instruction.makeInstruction(InstructionType.BRA,condition.getID()));

				//Set up the phi instructions and variable lookup table
				afterLoop.copyAllTablesFrom(currBB);
				for(String var : lastLoopBlock.getChangedVars()) {
					Argument loc1 = lastLoopBlock.getVarArg(var);
					Argument loc2 = currBB.getVarArg(var);
					
					Instruction i = Instruction.makeInstruction(InstructionType.PHI,loc1,loc2);
					afterLoop.updateVariable(var, i.getID(), i.getID());
					condition.prependInstruction(i);
					//Fix up old references in the loop that should actually point to phi
					replaceRefs(loop, var, currBB.getVarDef(var), loc2, i.getID(), currBB.getID());
				}
				
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
		
		Instruction i = Instruction.makeInstruction(InstructionType.CMP,idLeft,idRight);
		bb.appendInstruction(i);
		addPossibleUse(idLeft, bb, i.getID());
		addPossibleUse(idRight, bb, i.getID());
		
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
			arg = new Value(eNum.getValue());
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
		
		Argument arg = null;//TODO: value propagation temporarily disabled
//		if(leftArg instanceof Value && rightArg instanceof Value) {
//			int l = ((Value) leftArg).getValue();
//			int r = ((Value) rightArg).getValue();
//			
//			int exp;
//			switch(o) {
//			case ADDITION:
//				exp = l+r;
//				break;
//			case SUBTRACTION:
//				exp = l-r;
//				break;
//			case MULTIPLICATION:
//				exp = l*r;
//				break;
//			case DIVISION:
//				exp = l/r;
//				break;
//			default:
//				exp = Integer.MIN_VALUE;
//				System.out.println("Error unexpected binary operator.");
//			}
//			
//			arg = new Value(exp);
//		} else {
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
			arg = i.getID();
			
			//TODO may have an issue with value propagation
			addPossibleUse(leftArg,bb,i.getID());
			addPossibleUse(rightArg,bb,i.getID());
//		}
		return arg;
	}

	public Argument compileDesignator(Designator des, BasicBlock bb) {
		String var = des.getName();
		
		//Create an argument list for the load
		Argument[] args = compileArgsToArray(des.getIndices(), new DesName(var), bb);
		
		//Decide whether it is a known variable, unknown variable, or array load
		Instruction i = null;
		if(args.length == 1) {
			Argument id = bb.getVarDef(var);
			if(id == null) {
				//Unknown variable
				i = Instruction.makeInstruction(InstructionType.LOAD, var, args);
				bb.appendInstruction(i);//TODO: is this a semantic error?
				return i.getID();
			} else {
				//Known variable
				i = Instruction.makeInstruction(InstructionType.LOAD, var, args);
				bb.appendInstruction(i);
				
				//Make sure the instruction id points to the right variable
				id = id.clone();
				id.setVariable(var);
				
				return id;
			}
		} else {
			//Array load
			i = Instruction.makeInstruction(InstructionType.LOADADD, var, args);
			bb.appendInstruction(i);
			for(Argument arg : args) {
				addPossibleUse(arg,bb,i.getID());
			}
			return i.getID();
		}
	}
	
	public Argument compileFunctionCallExp(FunctionCallExp fce, BasicBlock bb) {
		//TODO: handle function calls correctly
		//Create an argument list for the function call
		Argument[] args = compileArgsToArray(fce.getArguments(), new FunctionName(fce.getName()), bb);
		
		//Create fake function call instruction
		Instruction i = Instruction.makeInstruction(InstructionType.FUNCTION,args);
		bb.appendInstruction(i);
		
		for(Argument arg : args) {
			addPossibleUse(arg,bb,i.getID());
		}
		
		return i.getID();
	}
	
	public Argument[] compileArgsToArray(List<Expression> exps, Argument first, BasicBlock bb) {
		List<Argument> args = new LinkedList<Argument>();
		if(first != null)
			args.add(first);
		if(exps != null) {
			for(Expression exp : exps) {
				args.add(compileExpression(exp,bb));
			}
		}
		Argument[] argsArr = new Argument[args.size()];
		return args.toArray(argsArr);
	}
	
	/*
	 * Replace all references to an argument with a different argument.
	 * Should only be used on while blocks.
	 */
	public void replaceRefs(BasicBlock bb, String var, InstructionID def, Argument oldArg, Argument newArg, BasicBlockID stop) {
		//Get the most recent use of var
		DefUse use = duchain.getMostRecentDefUse(var);
		
		//Iterate through instructions that reference this variable and replace their arguments
		while(use != null && use.getBasicBlockID().getID() > stop.getID()) {
			if(use.getDefLocation().equals(def)) {
				Instruction i = Instruction.getInstructionByID(use.getUseLocation());
				for(int a = 0; a < i.args.length; a++) {
					if(i.args[a].getVariable().equals(var) && i.args[a].equals(oldArg)) {
						i.args[a] = newArg;
					}
				}
			}
			use = use.getPreviousDefUse();
		}
	}
	
	public void addPossibleUse(Argument arg, BasicBlock bb, InstructionID id) {
		if(arg.hasVariable()) {
			String v = arg.getVariable();
			duchain.addDefUse(v, bb.getVarDef(v), bb.getVarArg(v), id, bb.getID());
		}
	}
}
