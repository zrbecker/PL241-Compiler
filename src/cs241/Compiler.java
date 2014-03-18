package cs241;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cs241.Argument.BasicBlockID;
import cs241.Argument.CopiedVariable;
import cs241.Argument.DesName;
import cs241.Argument.FunctionArg;
import cs241.Argument.FunctionName;
import cs241.Argument.InstructionID;
import cs241.Argument.Value;
import cs241.Argument.VariableArg;
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
import cs241.parser.treenodes.Variable;
import cs241.vcg.ControlFlowGraphVCG;

public class Compiler {
	File inputFile;
	File outputFile;
	Parser parser;
	BasicBlock mainRoot;
	DefUseChain duchain;
	Map<String,Function> functions;
	Map<String,BasicBlock> functionBBs;
	Computation c;

	private Set<InstructionType> expressionInstructions;
	private Set<String> currentFunctionParams;
	private Set<String> globalVariables;
	
	public Compiler(File in, File out) {
		inputFile = in;
		outputFile = out;
		parser = new Parser();
		mainRoot = new BasicBlock();
		duchain = new DefUseChain();
		functions = new HashMap<String,Function>();
		functionBBs = new HashMap<String,BasicBlock>();
		expressionInstructions  = new HashSet<InstructionType>();
		expressionInstructions.add(InstructionType.ADD);
		expressionInstructions.add(InstructionType.DIV);
		expressionInstructions.add(InstructionType.MUL);
		expressionInstructions.add(InstructionType.PHI);
		expressionInstructions.add(InstructionType.SUB);
	}

	public int[] compile() throws IOException {
		c = getParseTree();
		if(c == null) {
			System.out.println("Parsing error. Terminating.");
			return null;
		}
		
		//Compile the functions present in the code
		if(c.getFunctions() != null) {
			for(Function func : c.getFunctions()) {
				functions.put(func.getName(), func);
				
				currentFunctionParams = new HashSet<String>(func.getParameters());
				List<Statement> funcBody = func.getBody();
				BasicBlock funcHead = new BasicBlock();
				
				for(String param : currentFunctionParams) {
					FunctionArg paramFArg = new FunctionArg(func.getName(),param);
					Instruction i = Instruction.makeInstruction(InstructionType.MOVE,paramFArg, new DesName(param));
					//Do not append, because this move is a place holder for defuse purposes
					//currBB.appendInstruction(i);
					funcHead.updateVariable(param, paramFArg, i.getID());
				}
				
				for(Variable v : func.getVariables()) {
					Value val = new Value(0);
					Instruction i = Instruction.makeInstruction(InstructionType.MOVE,val, new DesName(v.getName()));
					funcHead.updateVariable(v.getName(), val, i.getID());
				}
				
				BasicBlock lastBlock = compileIntoBBs(funcBody, funcHead);
				if(!lastBlock.isReturnBlock()) {
					lastBlock.appendInstruction(Instruction.makeInstruction(InstructionType.RETURN));
					lastBlock.setIsReturnBlock();
				}
				functionBBs.put(func.getName(),funcHead);
			}
		}
		
		//Compile the main statement
		currentFunctionParams = new HashSet<String>();
		List<Statement> mainCode = c.getBody();
		for(Variable v : c.getVariables()) {
			Value val = new Value(0);
			Instruction i = Instruction.makeInstruction(InstructionType.MOVE,val, new DesName(v.getName()));
			mainRoot.updateVariable(v.getName(), val, i.getID());
		}
		
		BasicBlock lastBlock = compileIntoBBs(mainCode,mainRoot);
		lastBlock.appendInstruction(Instruction.makeInstruction(InstructionType.END));
		lastBlock.setIsReturnBlock();
		

		System.out.println("###### INTERMEDIATE REP ######");
		System.out.println(mainRoot);
		for(BasicBlock fbb : functionBBs.values()) {
			System.out.println(fbb);
		}
		
		ControlFlowGraphVCG exporter = new ControlFlowGraphVCG();
		exporter.exportAsVCG(inputFile.getName() + "_intermediate.vcg", mainRoot, functionBBs);
		
		//Simplify arguments and create new DefUse chain
		mainRoot.simplify();
		for(BasicBlock bb : functionBBs.values()) {
			bb.simplify();
		}
		refreshDefUseChain();
		
		//Replace STOREADD and LOADADD with ADD, MUL, STORE, and LOAD
		Map<String,Variable> varMap = new HashMap<String,Variable>();
		for(Variable v : c.getVariables()) {
			varMap.put(v.getName(), v);
		}
		removeArrayOps(mainRoot,varMap);
		for(String func : functionBBs.keySet()) {
			varMap = new HashMap<String,Variable>();
			for(Variable v : functions.get(func).getVariables()) {
				varMap.put(v.getName(), v);
			}
			removeArrayOps(functionBBs.get(func),varMap);
		}
		refreshDefUseChain();
		
		//Run common subexpression elimination
		runCommonSubexpressionEliminationAndCopyPropagation(mainRoot);
		for(BasicBlock bb : functionBBs.values()) {
			runCommonSubexpressionEliminationAndCopyPropagation(bb);
		}
		refreshDefUseChain();

		RegisterAllocator allocator = new RegisterAllocator();
		Map<InstructionID,Integer> mainAllocation = allocator.allocate(mainRoot);
		Map<String,Map<InstructionID,Integer>> functionToAllocation = new HashMap<String,Map<InstructionID,Integer>>();
		for(String funcName : functions.keySet()) {
			functionToAllocation.put(funcName, allocator.allocate(functionBBs.get(funcName)));
		}
		
		removePhis(mainRoot,mainAllocation);
		for(String funcName : functions.keySet()) {
			removePhis(functionBBs.get(funcName),functionToAllocation.get(funcName));
		}

		System.out.println("###### WITHOUT PHI INSTRUCTIONS ######");
		System.out.println(mainRoot);
		for(BasicBlock fbb : functionBBs.values()) {
			System.out.println(fbb);
		}
		
		exporter.exportAsVCG(inputFile.getName() + "_without_phi.vcg", mainRoot, functionBBs);
		
		
		//Create maps for compiling to real instructions
		Map<InstructionID,Integer> instructionToRegister = new HashMap<InstructionID,Integer>();
		Set<Argument> onHeap = new HashSet<Argument>();
		Map<Argument,Integer> heapVariableToOffset = new HashMap<Argument,Integer>();
		int currentHeapOffset = 0;
		//Handle arrays in main on heap
		for(Variable var : c.getVariables()) {
			if(var.getDimensions().size() == 0 && !globalVariables.contains(var.getName()))
				continue;
			int dim = 1;
			for(Integer d : var.getDimensions()) {
				dim*=d;
			}
			onHeap.add(new DesName(var.getName()));
			currentHeapOffset-=4*dim;
			heapVariableToOffset.put(new DesName(var.getName()),currentHeapOffset);
		}
		
		for(InstructionID id : mainAllocation.keySet()) {
			Integer reg = mainAllocation.get(id);
			if(reg <= 24) {
				instructionToRegister.put(id, reg + 3);
			} else {
				//Handle spilled registers in main on heap
				onHeap.add(id);
				currentHeapOffset-=4;
				heapVariableToOffset.put(id, currentHeapOffset);
			}
		}

		Map<Argument,Integer> stackVariableToOffset = new HashMap<Argument,Integer>();
		Map<BasicBlockID,Integer> basicBlockIDToSizeOfVariables = new HashMap<BasicBlockID,Integer>();
		for(Function func : c.getFunctions()) {
			//Handle function params on the stack
			int currentStackOffset = 4;
			for(String param : func.getParameters()) {
				FunctionArg farg = new FunctionArg(func.getName(), param);
				stackVariableToOffset.put(farg, currentStackOffset);
				currentStackOffset+=4;
			}
			int paramSize = currentStackOffset;
			
			//Handle function arrays on stack
			for(Variable var : func.getVariables()) {
				if(var.getDimensions().size() == 0)
					continue;
				int dim = 1;
				for(Integer d : var.getDimensions()) {
					dim*=d;
				}
				stackVariableToOffset.put(new DesName(var.getName()),currentStackOffset);
				currentStackOffset+=4*dim;
			}
			
			Map<InstructionID,Integer> funcAllocation = functionToAllocation.get(func.getName());
			for(InstructionID id : funcAllocation.keySet()) {
				Integer reg = funcAllocation.get(id);
				if(reg <= 24) {
					instructionToRegister.put(id, reg + 3);
				} else {
					//Handle spilled registers in a function on the stack
					stackVariableToOffset.put(id, currentStackOffset);
					currentStackOffset+=4;
				}
			}
			int totalSize = currentStackOffset;
			int variableSize = totalSize - paramSize;
			BasicBlockID bbID = functionBBs.get(func.getName()).getID();
			basicBlockIDToSizeOfVariables.put(bbID, variableSize);
		}
		
		Map<String,BasicBlockID> functionToBBIDs = new HashMap<String,BasicBlockID>();
		for(String func : functionBBs.keySet()) {
			functionToBBIDs.put(func, functionBBs.get(func).getID());
		}
		
		//Create real instructions
		ByteInstructionListFactory realInstructionMaker = new ByteInstructionListFactory(instructionToRegister,heapVariableToOffset,stackVariableToOffset,onHeap,basicBlockIDToSizeOfVariables,functionToBBIDs);
		realInstructionMaker.makeRealInstructions(mainRoot);
		for(BasicBlock fbb : functionBBs.values()) {
			realInstructionMaker.makeRealInstructions(fbb);
		}
		List<ByteInstruction> realInstructions = realInstructionMaker.getRealInstructionList();
		
		//Switch to a byte array
		int[] ops = new int[realInstructions.size()];
		for(int i = 0; i < realInstructions.size(); i++) {
			System.out.println(i +": " + realInstructions.get(i).opCode + " " + realInstructions.get(i).a + " " + realInstructions.get(i).b + " " + realInstructions.get(i).c);
			ops[i] = realInstructions.get(i).toInt();
		}
		
		return ops;
	}
	
	private void removePhis(BasicBlock bb, Map<InstructionID, Integer> allocation) {
		List<Instruction> instructions = bb.getInstructions();
		while(instructions.size() > 0 && instructions.get(0).type == InstructionType.PHI) {
			Instruction phi = instructions.get(0);
			instructions.remove(0);
			Integer reg = allocation.get(phi.getID());
			if(reg == null) {
				//If the result of the phi has no register it was not used
				continue;
			}
			Argument arg1 = phi.args[0];
			Argument arg2 = phi.args[1];
			BasicBlock arg1BB;
			BasicBlock arg2BB;
			if(bb.isWhileConditionBlock()) {
				arg1BB = bb.parents.get(0).isLastWhileLoopBlock() ? bb.parents.get(0) : bb.parents.get(1);
				arg2BB = bb.parents.get(0).isLastWhileLoopBlock() ? bb.parents.get(1) : bb.parents.get(0);
				//arg1 comes from lastLoop
				//arg2 comes from beforeCondition
			} else {
				arg1BB = bb.parents.get(0).isLastThenBlock() ? bb.parents.get(0) : bb.parents.get(1);
				arg2BB = bb.parents.get(0).isLastThenBlock() ? bb.parents.get(1) : bb.parents.get(0);
				//arg1 comes from lastThen
				//arg2 comes from other
			}
			Integer reg1 = -1;
			if(arg1 instanceof InstructionID) {
				reg1 = allocation.get(arg1);
			}
			if(reg1 != reg) {
				arg1BB.appendInstruction(Instruction.makeInstruction(InstructionType.MOVE, arg1, phi.getID()));
			}
			Integer reg2 = -1;
			if(arg2 instanceof InstructionID) {
				reg2 = allocation.get(arg2);
			}
			if(reg2 != reg) {
				arg2BB.appendInstruction(Instruction.makeInstruction(InstructionType.MOVE, arg2, phi.getID()));
			}
		}
		
		
		if(bb.getNext() != null) {
			removePhis(bb.getNext(), allocation);
		}
	}

	public Computation getParseTree() throws FileNotFoundException {
		Reader reader = new FileReader(inputFile);
		try {
			Computation c = parser.parse(reader);
			globalVariables = parser.getGlobals();
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
				
				if(des.getIndices() == null || des.getIndices().size() == 0) {
					if(des.isGlobal()) {
						Instruction i = Instruction.makeInstruction(InstructionType.STORE, new DesName(var), arg);
						addPossibleUse(currBB,i.getID(),arg);
						currBB.appendInstruction(i);
					} else {
						Instruction i = Instruction.makeInstruction(InstructionType.MOVE,arg, new DesName(var));
						//Do not append, because this move is a place holder for defuse purposes
						//currBB.appendInstruction(i);
						addPossibleUse(currBB,i.getID(),arg);
						currBB.updateVariable(var, arg, i.getID());
					}
				} else {
					Argument[] args = compileArgsToArray(des.getIndices(), currBB, arg, new DesName(des.getName()));
					Instruction i = Instruction.makeInstruction(InstructionType.STOREADD, args);
					addPossibleUse(currBB,i.getID(),args);
					currBB.appendInstruction(i);
				}
			} else if (curr instanceof FunctionCall) {
				FunctionCall currFC = (FunctionCall) curr;
				
				//Compile arguments to the function
				Argument[] args = compileArgsToArray(currFC.getArguments(), currBB, new FunctionName(currFC.getName()));

				//Add the function call instruction
				Instruction i = Instruction.makeInstruction(InstructionType.FUNCTION, args);
				currBB.appendInstruction(i);
				addPossibleUse(currBB, i.getID(), args);
			} else if (curr instanceof If) {
				If currIf = (If)curr;
				boolean thenOnly = currIf.getElseBlock() == null || currIf.getElseBlock().size() == 0;

				BasicBlock ifThen = new BasicBlock();
				ifThen.copyVariableTableFrom(currBB);
				BasicBlock ifElse = new BasicBlock();
				ifElse.copyVariableTableFrom(currBB);
				BasicBlock afterIf = new BasicBlock();
				
				//Set up parent/child relationship
				List<BasicBlock> oldChildren = currBB.children;
				currBB.children = new ArrayList<BasicBlock>();
				currBB.addChild(ifThen);
				if(!thenOnly)
					currBB.addChild(ifElse);
				else
					currBB.addChild(afterIf);
				ifThen.addChild(afterIf);
				if(!thenOnly)
					ifElse.addChild(afterIf);
				afterIf.children = oldChildren;
				
				ifThen.addParent(currBB);
				if(!thenOnly)
					ifElse.addParent(currBB);
				else
					afterIf.addParent(currBB);
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
				BasicBlock next = currBB.getNext();
				currBB.setNext(ifThen);
				ifThen.setPrevious(currBB);
				if(thenOnly) {
					ifThen.setNext(afterIf);
					afterIf.setPrevious(ifThen);
				} else {
					ifThen.setNext(ifElse);
					ifElse.setPrevious(ifThen);
					ifElse.setNext(afterIf);
					afterIf.setPrevious(ifElse);
				}
				afterIf.setNext(next);
				if(next != null)
					next.setPrevious(afterIf);

				//Compile the condition for the if
				BasicBlockID branchID = thenOnly ? afterIf.getID() : ifElse.getID();
				compileCondition(currIf.getCondition(),currBB, branchID);
				
				//Compile the branches
				BasicBlock lastIfThenBlock = compileIntoBBs(currIf.getThenBlock(), ifThen);
				lastIfThenBlock.setLastThenBlock();
				BasicBlock lastIfElseBlock = ifElse;
				if(!thenOnly) {
					//Add the branch from the ifthen to after the ifelse
					lastIfThenBlock.setBranchInstruction(Instruction.makeInstruction(InstructionType.BRA,afterIf.getID()));
					lastIfElseBlock = compileIntoBBs(currIf.getElseBlock(), ifElse);
				}
				
				boolean ifReturn = lastIfThenBlock.isReturnBlock();
				boolean elseReturn = lastIfElseBlock.isReturnBlock();
				if(ifReturn && elseReturn) {
					//If both branches return, then we returned
					currBB.setIsReturnBlock();
					return afterIf;
				} else if(ifReturn) {
					afterIf.copyAllTablesFrom(lastIfElseBlock);
				} else if(elseReturn) {
					afterIf.copyAllTablesFrom(lastIfThenBlock);
				} else {
					//Get the list of variables that were assigned in the branches
					Set<String> changedVars = new HashSet<String>();
					changedVars.addAll(lastIfThenBlock.getChangedVariables());
					if(!thenOnly)
						changedVars.addAll(lastIfElseBlock.getChangedVariables());
					
					//Create phi instructions
					afterIf.copyAllTablesFrom(currBB);
					for(String var : changedVars) {
						VariableArg loc1 = lastIfThenBlock.getVariable(var);
						VariableArg loc2 = lastIfElseBlock.getVariable(var);
						if(loc1 == null || loc2 == null)
							continue;
						
						Instruction i = Instruction.makeInstruction(InstructionType.PHI,loc1,loc2);
						//Implicit store instruction to make a def
						Instruction t = Instruction.makeInstruction(InstructionType.MOVE,i.getID(), new DesName(var));

						duchain.addDefUse(lastIfThenBlock.getDefinitionForVariable(var), loc1, i.getID(), afterIf.getID());
						duchain.addDefUse(lastIfElseBlock.getDefinitionForVariable(var), loc2, i.getID(), afterIf.getID());
						afterIf.appendInstruction(i);
						afterIf.updateVariable(var, i.getID(), t.getID());
					}
				}
				
				currBB = afterIf;
			} else if (curr instanceof While) {
				While currWhile = (While) curr;
				
				BasicBlock condition = new BasicBlock();
				condition.setWhileConditionBlock();
				condition.copyAllTablesFrom(currBB);
				BasicBlock loop = new BasicBlock();
				loop.copyVariableTableFrom(currBB);
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
				BasicBlock next = currBB.getNext();
				currBB.setNext(condition);
				condition.setPrevious(currBB);
				condition.setNext(loop);
				loop.setPrevious(condition);
				loop.setNext(afterLoop);
				afterLoop.setPrevious(loop);
				afterLoop.setNext(next);
				if(next != null)
					next.setPrevious(afterLoop);
				
				//Compile the condition for the while loop
				compileCondition(currWhile.getCondition(),condition, afterLoop.getID());
				
				//Compile loop block
				BasicBlock lastLoopBlock = compileIntoBBs(currWhile.getBlock(), loop);
				lastLoopBlock.setLastWhileLoopBlock();
				
				if(lastLoopBlock.isReturnBlock()) {
					afterLoop.copyAllTablesFrom(condition);
				} else {
					//Have the loop branch back to the condition
					lastLoopBlock.setBranchInstruction(Instruction.makeInstruction(InstructionType.BRA,condition.getID()));
	
					//Set up the phi instructions and variable lookup table
					afterLoop.copyAllTablesFrom(currBB);
					for(String var : lastLoopBlock.getChangedVariables()) {
						VariableArg loc1 = lastLoopBlock.getVariable(var);
						VariableArg loc2 = currBB.getVariable(var);
						
						if(loc1 == null || loc2 == null)
							continue;
						
						Instruction i = Instruction.makeInstruction(InstructionType.PHI,loc1,loc2);
						//Implicit store instruction to make a def
						Instruction t = Instruction.makeInstruction(InstructionType.STORE,i.getID());
						//Fix up old references in the loop that should actually point to phi
						replaceRefs(var, currBB.getDefinitionForVariable(var), t.getID(), loc2, i.getID(), condition.getID());
						
						//Only add the phi as a possible use after the replacement
						duchain.addDefUse(lastLoopBlock.getDefinitionForVariable(var), loc1, i.getID(), condition.getID());
						duchain.addDefUse(currBB.getDefinitionForVariable(var), loc2, i.getID(), condition.getID());
						
						condition.prependInstruction(i);
						afterLoop.updateVariable(var, i.getID(), t.getID());
					}
				}
				
				currBB = afterLoop;
			} else if (curr instanceof Return) {
				Return currReturn = (Return) curr;
				Instruction i;
				if(currReturn.getExpression() != null) {
					Argument arg = compileExpression(currReturn.getExpression(), currBB);
					i = Instruction.makeInstruction(InstructionType.RETURN, arg);
					addPossibleUse(currBB, i.getID(), arg);
				} else {
					i = Instruction.makeInstruction(InstructionType.RETURN);
				}
				currBB.appendInstruction(i);
				return currBB;
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
		addPossibleUse(bb, i.getID(), idLeft, idRight);
		
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
		bb.setBranchInstruction(Instruction.makeInstruction(it,branchLocation));
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
		Argument arg = i.getID();
		addPossibleUse(bb, i.getID(), leftArg, rightArg);
		return arg;
	}

	public Argument compileDesignator(Designator des, BasicBlock bb) {
		String var = des.getName();
		
		//Create an argument list for the load
		Argument[] args = compileArgsToArray(des.getIndices(), bb, new DesName(var));
		
		//Decide whether it is a known variable, unknown variable, or array load
		Instruction i = null;
		if(args.length == 1) {
			VariableArg varg = bb.getVariable(var);
			if(varg == null) {
				//Global variable
				i = Instruction.makeInstruction(InstructionType.LOAD, args);
				bb.appendInstruction(i);
				return i.getID();
			} else {
				//Known variable				
				return varg;
			}
		} else {
			//Array load
			i = Instruction.makeInstruction(InstructionType.LOADADD, args);
			bb.appendInstruction(i);
			addPossibleUse(bb,i.getID(),args);
			return i.getID();
		}
	}
	
	public Argument compileFunctionCallExp(FunctionCallExp fce, BasicBlock bb) {
		Instruction i;
		if(fce.getArguments() != null) {
			//Create an argument list for the function call
			Argument[] args = compileArgsToArray(fce.getArguments(), bb, new FunctionName(fce.getName()));
			i = Instruction.makeInstruction(InstructionType.FUNCTION,args);
			addPossibleUse(bb,i.getID(),args);
		} else {
			i = Instruction.makeInstruction(InstructionType.FUNCTION,new FunctionName(fce.getName()));
		}
		bb.appendInstruction(i);
		
		return i.getID();
	}
	
	public Argument[] compileArgsToArray(List<Expression> exps, BasicBlock bb, Argument... first) {
		List<Argument> args = new LinkedList<Argument>();
		if(first != null) {
			for(Argument a : first)
				args.add(a);
		}
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
	public void replaceRefs(String var, InstructionID def, InstructionID newDef, VariableArg oldArg, InstructionID newArg, BasicBlockID stop) {
		//Get the most recent use of var
		DefUse use = duchain.getMostRecentDefUse(def);
		
		//Iterate through instructions that reference this variable and replace their arguments
		while(use != null && use.getBasicBlockID().getID() >= stop.getID()) {
			DefUse prevUse = use.getPreviousDefUse();
			
			Instruction i = Instruction.getInstructionByID(use.getUseLocation());
			for(int a = 0; a < i.args.length; a++) {
				Argument arg = i.args[a];

				if(arg.equals(use.getArgumentForVariable())) {
					assert arg.isVariable();
					VariableArg v = (VariableArg)arg;
					
					if(v instanceof CopiedVariable) {
						CopiedVariable cvArg = (CopiedVariable)v;
						if(!cvArg.copyOf(oldArg)) {
							continue;
						}
					} else if(oldArg instanceof CopiedVariable){
						continue;
					}
					
					i.args[a] = i.args[a].clone();
					((VariableArg)i.args[a]).updateValueAndDef(newArg,newDef);
					duchain.removeDefUse(use);
					duchain.addDefUse(newDef, i.args[a], i.getID(), use.getBasicBlockID());
				}
			}
			
			use = prevUse;
		}
	}
	
	public void addPossibleUse(BasicBlock bb, InstructionID id, Argument... args) {
		for(Argument arg : args) {
			if(arg.isVariable()) {
				VariableArg v = ((VariableArg)arg);
				duchain.addDefUse(v.getDef(), v, id, bb.getID());
			}
		}
	}
	
	private void refreshDefUseChain() {
		duchain = new DefUseChain();
		createNewDefUseChain(mainRoot);
		for(BasicBlock bb : functionBBs.values()) {
			createNewDefUseChain(bb);
		}
	}
	
	private void createNewDefUseChain(BasicBlock bb) {
		for(Instruction i : bb.getInstructions()) {
			for(Argument a : i.args) {
				if(a instanceof InstructionID)
					duchain.addDefUse((InstructionID)a, a, i.getID(), bb.getID());
			}
		}
		if(bb.getNext() != null)
			createNewDefUseChain(bb.getNext());
	}
	
	private void simpleReplace(InstructionID oldID, Argument newArg) {
		DefUse du = duchain.getMostRecentDefUse(oldID);
		while(du != null) {
			Instruction use = Instruction.getInstructionByID(du.getUseLocation());
			for(int j = 0; j < use.args.length; j++) {
				if(use.args[j].equals(oldID)) {
					use.args[j] = newArg;
				}
			}
			du = du.getPreviousDefUse();
		}
	}

	private void removeArrayOps(BasicBlock bb, Map<String,Variable> variables) {
		for(int i = 0; i < bb.getInstructions().size(); i++) {
			Instruction in = bb.instructions.get(i);
			if(in.type == InstructionType.LOADADD) {
				DesName arrName = (DesName)in.args[0];
				Variable v = variables.get(arrName.getName());
				List<Integer> dims = v.getDimensions();
				InstructionID newID;
				if(dims.size() == 1) {
					bb.instructions.remove(i);
					Instruction t = Instruction.makeInstruction(InstructionType.LOAD, in.args);
					bb.instructions.add(i,t);
					newID = t.getID();
				} else {
					int[] partials = new int[dims.size()];
					int prod = 1;
					for(int j = dims.size() - 1; j >= 0; j--) {
						partials[j] = prod;
						prod*=dims.get(j);
					}
					bb.instructions.remove(i);
					Instruction res = Instruction.makeInstruction(InstructionType.MUL, in.args[1], new Value(partials[0]));
					bb.instructions.add(i,res);
					i++;
					for(int j = 2; j < in.args.length-1; j++) {
						Instruction next = Instruction.makeInstruction(InstructionType.MUL, in.args[j], new Value(partials[j-1]));
						bb.instructions.add(i,next);
						i++;
						res = Instruction.makeInstruction(InstructionType.ADD, next.getID(), res.getID());
						bb.instructions.add(i,res);
						i++;
					}
					res = Instruction.makeInstruction(InstructionType.ADD, res.getID(), in.args[in.args.length-1]);
					bb.instructions.add(i,res);
					i++;
					Instruction t = Instruction.makeInstruction(InstructionType.LOAD, arrName,res.getID());
					bb.instructions.add(i,t);
					newID = t.getID();
				}
				simpleReplace(in.getID(),newID);
			} else if (in.type == InstructionType.STOREADD) {
				DesName arrName = (DesName)in.args[1];
				Variable v = null;
				if(globalVariables.contains(arrName.getName())) {
					for(Variable var : c.getVariables()) {
						if(var.getName().equals(arrName.getName())) {
							v = var;
							break;
						}
					}
					if(v == null) {
						System.out.println("Error: global variable " + arrName + " is null");
					}
				} else {
					v = variables.get(arrName.getName());
				}
				// TODO: Nullpointer exception on global array, double check works
				List<Integer> dims = v.getDimensions();
				InstructionID newID;
				if(dims.size() == 1) {
					bb.instructions.remove(i);
					Instruction t = Instruction.makeInstruction(InstructionType.STORE, in.args);
					bb.instructions.add(i,t);
					newID = t.getID();
				} else {
					int[] partials = new int[dims.size()];
					int prod = 1;
					for(int j = dims.size() - 1; j >= 0; j--) {
						partials[j] = prod;
						prod*=dims.get(j);
					}
					bb.instructions.remove(i);
					Instruction res = Instruction.makeInstruction(InstructionType.MUL, in.args[2], new Value(partials[0]));
					bb.instructions.add(i,res);
					i++;
					for(int j = 3; j < in.args.length-1; j++) {
						Instruction next = Instruction.makeInstruction(InstructionType.MUL, in.args[j], new Value(partials[j-2]));
						bb.instructions.add(i,next);
						i++;
						res = Instruction.makeInstruction(InstructionType.ADD, next.getID(), res.getID());
						bb.instructions.add(i,res);
						i++;
					}
					res = Instruction.makeInstruction(InstructionType.ADD, res.getID(), in.args[in.args.length-1]);
					bb.instructions.add(i,res);
					i++;
					Instruction t = Instruction.makeInstruction(InstructionType.STORE, in.args[0], arrName,res.getID());
					bb.instructions.add(i,t);
					newID = t.getID();
				}
				simpleReplace(in.getID(),newID);
			}
		}
		if(bb.getNext() != null)
			removeArrayOps(bb.getNext(), variables);
	}
	
	public void runCommonSubexpressionEliminationAndCopyPropagation(BasicBlock bb) {
		Map<InstructionType,List<Instruction>> m = new HashMap<InstructionType,List<Instruction>>();
		for(InstructionType it : expressionInstructions)
			m.put(it, new LinkedList<Instruction>());
		runConstantPropagation(bb);
		runCommonSubexpressionElimination(bb, m);
	}
	
	private void runConstantPropagation(BasicBlock bb) {
		for(int i = 0; i < bb.getInstructions().size(); i++) {
			Instruction ins = bb.getInstructions().get(i);
			InstructionType it = ins.type;

			//Attempt to eliminate any expression of only values
			if(expressionInstructions.contains(it)) {
				if(it != InstructionType.PHI) {
					boolean allValues = true;
					for(Argument a : ins.args) {
						if(!(a instanceof Value))
							allValues = false;
					}
					if(allValues) {
						Value v = evaluate(ins);
						simpleReplace(ins.getID(),v);
						bb.getInstructions().remove(i);
						i--;
						continue;
					}
				}
			}
		}
		
		//Recurse into the next block
		if(bb.getNext() != null)
			runConstantPropagation(bb.getNext());
	}

	private void runCommonSubexpressionElimination(BasicBlock bb, Map<InstructionType,List<Instruction>> lookup) {
		for(int i = 0; i < bb.getInstructions().size(); i++) {
			Instruction ins = bb.getInstructions().get(i);
			InstructionType it = ins.type;
			
			if(expressionInstructions.contains(it)) {
				//Check if this is dominated by an identical expression
				List<Instruction> l = lookup.get(it);
				Instruction replace = lookForCopy(ins,l);
				
				if(replace != null) {
					simpleReplace(ins.getID(),replace.getID());
					bb.getInstructions().remove(i);
					i--;
					continue;
				}
				
				//Add to the list of expressions if not replaced yet
				l.add(ins);
			}
		}
		
		//Recurse into dominated blocks
		for(BasicBlock bbDom : bb.dominated) {
			Map<InstructionType,List<Instruction>> newLookup = new HashMap<InstructionType,List<Instruction>>();
			for(InstructionType it : lookup.keySet()) {
				List<Instruction> l = lookup.get(it);
				newLookup.put(it, new LinkedList<Instruction>(l));
			}
			runCommonSubexpressionElimination(bbDom, newLookup);
		}
	}

	private Instruction lookForCopy(Instruction ins, List<Instruction> l) {
		for(Instruction ins2 : l) {
			if(ins.args.length == ins2.args.length) {
				boolean same = true;
				for(int i = 0; i < ins.args.length; i++) {
					if(!ins.args[i].equals(ins2.args[i])) {
						same = false;
					}
				}
				if(same)
					return ins2;
			}
		}
		return null;
	}

	/*
	 * The input, ins, should be an expression type
	 */
	private Value evaluate(Instruction ins) {
		InstructionType it = ins.type;
		
		switch(it) {
			case ADD:
				return new Value(((Value)ins.args[0]).getValue() + ((Value)ins.args[1]).getValue());
			case DIV:
				return new Value(((Value)ins.args[0]).getValue() / ((Value)ins.args[1]).getValue());
			case MUL:
				return new Value(((Value)ins.args[0]).getValue() * ((Value)ins.args[1]).getValue());
			case SUB:
				return new Value(((Value)ins.args[0]).getValue() - ((Value)ins.args[1]).getValue());
			default:
				return null;
		}
	}
}
