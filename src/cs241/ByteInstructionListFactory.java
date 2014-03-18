package cs241;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cs241.Argument.BasicBlockID;
import cs241.Argument.DesName;
import cs241.Argument.FunctionArg;
import cs241.Argument.FunctionName;
import cs241.Argument.InstructionID;
import cs241.Argument.Value;
import cs241.Instruction.InstructionType;
import cs241.ByteInstruction.ByteJumpInstruction;

public class ByteInstructionListFactory {
	//Opcodes
	public static	int ADD = 0;
	public static	int SUB = 1;
	public static	int MUL = 2;
	public static	int DIV = 3;
	public static	int MOD = 4;
	public static	int CMP = 5;
	public static	int OR = 8;
	public static	int AND = 9;
	public static	int BIC = 10;
	public static	int XOR = 11;
	public static	int LSH = 12;
	public static	int ASH = 13;
	public static	int CHK = 14;
	public static	int ADDI = 16;
	public static	int SUBI = 17;
	public static	int MULI = 18;
	public static	int DIVI = 19;
	public static	int MODI = 20;
	public static	int CMPI = 21;
	public static	int ORI = 24;
	public static	int ANDI = 25;
	public static	int BICI = 26;
	public static	int XORI = 27;
	public static	int LSHI = 28;
	public static	int ASHI = 29;
	public static	int CHKI = 30;
	public static	int LDW = 32;
	public static	int LDX = 33;
	public static	int POP = 34;
	public static	int STW = 36;
	public static	int STX = 37;
	public static	int PSH = 38;
	public static	int BEQ = 40;
	public static	int BNE = 41;
	public static	int BLT = 42;
	public static	int BGE = 43;
	public static	int BLE = 44;
	public static	int BGT = 45;
	public static	int BSR = 46;
	public static	int JSR = 48;
	public static	int RET = 49;
	public static	int RDD = 50;
	public static	int WRD = 51;
	public static	int WRH = 52;
	public static	int WRL = 53;
	
	//Reserved Registers
	public static final int ZERO_REG = 0; //Register zero is always equal to zero
	public static final int R1 = 1; //First register for spillage
	public static final int R2 = 2; //Second register for spillage
	public static final int RETURN_VALUE = 3; //Return value of functions
	public static final int FRAME_POINTER = 28;
	public static final int STACK_POINTER = 29;
	public static final int HEAP_MEMORY_LOCATION = 30;
	public static final int BRANCH_RETURN_ADDRESS = 31;
	
	/*
	 * The instruction to register map should not use registers 0,28,29,30,31,1,2,3
	 */
	Map<InstructionID,Integer> instructionToRegister;
	
	/*
	 * The heap variable to offset map should map global variables and arrays to particular offsets from R30
	 */
	Map<Argument,Integer> heapVariableToOffset;
	
	/*
	 * The stack variable to offset map should map function parameters to particular offsets from R28
	 */
	Map<Argument,Integer> stackVariableToOffset;
	
	Set<Argument> onHeap;
	
	Map<BasicBlockID,Integer> basicBlockIDToSizeOfVariables;
	
	Map<String,BasicBlockID> functionToBasicBlockID;
	/*
	 * Map for fixing up jump locations
	 */
	private Map<BasicBlockID,Integer> bbIDToLoc;
	

	Argument currentR1; //Stores the current argument in the first register
	Argument currentR2; //Stores the current argument in the second register
	
	List<ByteInstruction> instructions;
	public ByteInstructionListFactory(Map<InstructionID,Integer> insToReg, Map<Argument,Integer> heapVarToOff, Map<Argument,Integer> stackVarToOff, Set<Argument> varsOnHeap, Map<BasicBlockID,Integer> bbIDsToVarSize, Map<String,BasicBlockID> funToBBID) {
		instructionToRegister = insToReg;
		for(InstructionID id : instructionToRegister.keySet()) {
			Integer reg = instructionToRegister.get(id);
			if(reg <=3 || reg >= 28)
				System.out.println("Error: using reserved register");
		}
		heapVariableToOffset = heapVarToOff;
		stackVariableToOffset = stackVarToOff;
		onHeap = varsOnHeap;
		basicBlockIDToSizeOfVariables = bbIDsToVarSize;
		functionToBasicBlockID = funToBBID;
		instructions = new ArrayList<ByteInstruction>();
		bbIDToLoc = new HashMap<BasicBlockID,Integer>();
	}
	
	/*
	 * All basic blocks should be made real before this call
	 */
	public List<ByteInstruction> getRealInstructionList() {
		fixUpJumpsAndReturns();
		return instructions;
	}
	
	public void makeRealInstructions(BasicBlock bb) {
		bbIDToLoc.put(bb.getID(), instructions.size());
		InstructionID conditionalBranch = null;
		for(Instruction i : bb.instructions) {
			InstructionID resultID = i.getID();
			switch(i.type) {
			case ADD:
				decideInstructionType(i.args[0], i.args[1], ADD, ADDI, resultID);
				break;
			case SUB:
				decideInstructionTypeNoncomm(i.args[0], i.args[1], SUB, SUBI, resultID);
				break;
			case MUL:
				decideInstructionType(i.args[0], i.args[1], MUL, MULI, resultID);
				break;
			case DIV:
				decideInstructionTypeNoncomm(i.args[0], i.args[1], DIV, DIVI, resultID);
				break;
			case LOAD:
				if (i.args.length == 1) {
					//Global variable load
					DesName var = (DesName)i.args[0];
					Integer offset = heapVariableToOffset.get(var);
					Integer resultReg = getRegisterFor(resultID);
					
					if (resultReg == -1) {
						resultReg = R2;
					}
					
					prepareRegister(resultReg,resultID);
					instructions.add(new ByteInstruction(LDW,resultReg,HEAP_MEMORY_LOCATION,offset));
				} else {
					//Array load
					DesName var = (DesName)i.args[0];
					Integer offset = getOffset(var);
					Integer lookupReg = getHeapOrStack(var);
					Integer resultReg = instructionToRegister.get(resultID);
					
					
					if(i.args[1] instanceof InstructionID || i.args[1] instanceof FunctionArg) {
						Argument index = i.args[1];
						Integer indexReg = getRegisterFor(i.args[1]);
						
						if (resultReg == -1 || indexReg == -1) {
							if(indexReg == -1 && resultReg == -1) {
								putArgInR2(index);
								indexReg = R2;
								resultReg = R2;
							} else if (indexReg == -1) {
								putArgInR2(index);
								indexReg = R2;
							} else {//resultReg == -1
								resultReg = R2;
							}
						}

						prepareRegister(R2,null);
						instructions.add(new ByteInstruction(ADDI,R2,indexReg,offset));
						prepareRegister(resultReg,resultID);
						instructions.add(new ByteInstruction(LDX,resultReg,lookupReg,R2));
					} else if (i.args[1] instanceof Value) {
						Value index = (Value)i.args[1];
						if(resultReg == -1) {
							resultReg = R2;
						}
						
						prepareRegister(resultReg, resultID);
						instructions.add(new ByteInstruction(LDW,resultReg,lookupReg,offset + index.getValue()));
					} else {
						System.out.println("Error: unrecognized argument type.");
					}
				}
				break;
			case STORE:
				if(i.args.length == 2) {
					//Global variable store
					DesName var = (DesName)i.args[0];
					Integer offset = getOffset(var);
					Integer lookupReg = getHeapOrStack(var);
					if(i.args[1] instanceof InstructionID || i.args[1] instanceof FunctionArg) {
						Argument val = (Argument)i.args[1];
						Integer valReg = getRegisterFor(val);
						if(valReg == -1) {
							putArgInR2(val);
							valReg = R2;
						}
						
						instructions.add(new ByteInstruction(STW,valReg,lookupReg,offset));
					} else if (i.args[1] instanceof Value) {
						Value val = (Value)i.args[1];
						putArgInR1(val);
						instructions.add(new ByteInstruction(STW,R1,lookupReg,offset));
					} else {
						System.out.println("Error: unrecognized argument type.");
					}
				} else {
					//Array store
					DesName var = (DesName)i.args[1];
					Integer offset = getOffset(var);
					Integer lookupReg = getHeapOrStack(var);
					Integer valReg;
					if(i.args[0] instanceof InstructionID || i.args[0] instanceof FunctionArg) {
						Argument val = i.args[0];
						valReg = getRegisterFor(val);
					} else {
						Value val = (Value)i.args[0];
						valReg = R1;
						putArgInR1(val);
					}
					
					if(i.args[2] instanceof InstructionID || i.args[2] instanceof FunctionArg) {
						Argument index = i.args[2];
						Integer indexReg = getRegisterFor(index);
						if(valReg == -1 || indexReg == -1) {
							if(valReg == -1 && indexReg == -1) {
								//indexReg must be R2 and valReg must be R1
								putArgInR1(i.args[0]);
								putArgInR2(index);
								valReg = R1;
								indexReg = R2;
							} else if (valReg == -1) {
								putArgInR1(i.args[0]);
								valReg = R1;
							} else {//indexReg == -1
								putArgInR2(index);
								indexReg = R2;
							}
						}
						prepareRegister(R2,null);
						instructions.add(new ByteInstruction(ADDI,R2,indexReg,offset));
						instructions.add(new ByteInstruction(STX,valReg,lookupReg,R2));
					} else {
						Value index = (Value)i.args[2];
						if(valReg == -1) {
							putArgInR1(i.args[0]);
							valReg = R1;
						}
						instructions.add(new ByteInstruction(STX,valReg,HEAP_MEMORY_LOCATION,offset + index.getValue()));
					}
				}
				break;
			case MOVE:
				InstructionID target = (InstructionID)i.args[1];
				Integer targetReg = instructionToRegister.get(target);
				if(i.args[0] instanceof InstructionID || i.args[0] instanceof FunctionArg) {
					Argument value =  i.args[0];
					Integer valueReg = getRegisterFor(value);
					if(targetReg == -1 || valueReg == -1) {
						if (targetReg == -1 && valueReg == -1) {
							targetReg = R1;
							putArgInR2(value);
							valueReg = R2;
						} else if (targetReg == -1) {
							targetReg = R1;
						} else { //valueReg == -1
							putArgInR2(value);
							valueReg = R2;
						}
					}
					prepareRegister(targetReg, value);
					instructions.add(new ByteInstruction(ADDI, targetReg, valueReg, 0));
				} else {
					Value value = (Value)i.args[0];
					if(targetReg == -1) {
						targetReg = R2;
					}
					prepareRegister(targetReg,value);
					instructions.add(new ByteInstruction(ADDI, targetReg, ZERO_REG, value.getValue()));
				}
				break;
			case FUNCTION:
				makeFunctionCallInstructions(i.args, resultID, bb);
				break;
			case CMP:
				decideInstructionTypeNoncomm(i.args[0], i.args[1], CMP, CMPI, resultID);
				//All compares signal the end of the basic block and that we should follow the branch instruction
				conditionalBranch = resultID;
				//Note: there should be no more instructions after a cmp
				break;
			case END:
				instructions.add(new ByteInstruction(RET,0,0,0));
				//Note: there should be no more instructions after an end
				break;
			case RETURN:
				makeReturnStatementInstructions(i.args);
				//Note: there should be no more instructions after a ret
				break;
			default:
				System.out.println("Error: unrecognized instruction type: " + i.type);//LOADADD and STOREADD have been removed and branches are special
			}
		}
		
		if(bb.getBranchInstruction() != null && !bb.isReturnBlock()) {
			Instruction branchIns = bb.getBranchInstruction();
			if(conditionalBranch != null)
				makeBranchInstruction((BasicBlockID)branchIns.args[0], conditionalBranch, branchIns.type);
			else
				makeBranchInstruction((BasicBlockID)branchIns.args[0]);
		}
			
		if(bb.getNext() != null)
			makeRealInstructions(bb.getNext());
	}
	
	private Integer getHeapOrStack(Argument arg) {
		if(onHeap.contains(arg))
			return HEAP_MEMORY_LOCATION;
		else
			return FRAME_POINTER;
	}
	
	private Integer getOffset(Argument arg) {
		if(onHeap.contains(arg)) {
			return heapVariableToOffset.get(arg);
		} else {
			return stackVariableToOffset.get(arg);
		}
	}
	
	private Integer getRegisterFor(Argument arg) {
		Integer i = instructionToRegister.get(arg);
		if(i == null)
			return -1;
		else
			return i;
	}
	
	private void prepareRegister(Integer resultReg, Argument arg) {
		if(resultReg == R1) {
			if(currentR1 != null) {
				storeR1();
			}
			currentR1 = arg;
		} else if (resultReg == R2) {
			if(currentR2 != null) {
				storeR2();
			}
			currentR2 = arg;
		}
	}

	private void storeR1() {
		if(currentR1 != null && currentR1 instanceof InstructionID) {
			Integer offset = getOffset(currentR1);
			Integer lookupReg = getHeapOrStack(currentR1);
			instructions.add(new ByteInstruction(STW, R1, lookupReg, offset));
		}
		currentR1 = null;
	}
	
	private void storeR2() {
		if(currentR2 != null && currentR2 instanceof InstructionID) {
			Integer offset = getOffset(currentR2);
			Integer lookupReg = getHeapOrStack(currentR2);
			instructions.add(new ByteInstruction(STW, R2, lookupReg, offset));
		}
		currentR2 = null;
	}
	
	/*
	 * Overwrites R1 with arg
	 */
	private void putArgInR1(Argument arg) {
		if(currentR1 != null && arg.equals(currentR1))
			return;
		storeR1();
		if(arg instanceof InstructionID || arg instanceof FunctionArg) {
			Integer offset = getOffset(arg);
			Integer lookupReg = getHeapOrStack(arg);
			instructions.add(new ByteInstruction(LDW, R1, lookupReg, offset));
		} else if(arg instanceof Value) {
			instructions.add(new ByteInstruction(ADDI, R1, ZERO_REG, ((Value)arg).getValue()));
		} else {
			System.out.println("Error: putting bad type arg into R1");
		}
		currentR1 = arg;
	}
	
	/*
	 * Overwrites R2 with arg
	 */
	private void putArgInR2(Argument arg) {
		if(currentR2 != null && arg.equals(currentR2))
			return;
		storeR2();
		if(arg instanceof InstructionID || arg instanceof FunctionArg) {
			Integer offset = getOffset(arg);
			Integer lookupReg = getHeapOrStack(arg);
			instructions.add(new ByteInstruction(LDW, R2, lookupReg, offset));
		} else if(arg instanceof Value) {
			instructions.add(new ByteInstruction(ADDI, R2, ZERO_REG, ((Value)arg).getValue()));
		} else {
			System.out.println("Error: putting bad type arg into R2");
		}
		currentR2 = arg;
	}

	private void decideInstructionType(Argument a1, Argument a2, int opcode, int opcodei, InstructionID resultID) {
		if(a1 instanceof Value) {
			makeTwoRegOneValueInstruction(opcodei, resultID, a2, ((Value)a1).getValue());
		} else if(a2 instanceof Value) {
			makeTwoRegOneValueInstruction(opcodei, resultID, a1, ((Value)a2).getValue());
		} else {
			makeThreeRegInstruction(opcode, resultID, a1, a2);
		}
	}
	
	private void decideInstructionTypeNoncomm(Argument a1, Argument a2, int opcode, int opcodei, InstructionID resultID) {
		if(a2 instanceof Value) {
			makeTwoRegOneValueInstruction(opcodei, resultID, a1, ((Value)a2).getValue());
		} else {
			makeThreeRegInstruction(opcode, resultID, a1, a2);
		}
	}
	
	private void makeThreeRegInstruction(int opc, InstructionID resultID, Argument arg1, Argument arg2) {
		Integer resultReg = getRegisterFor(resultID);
		Integer input1Reg = getRegisterFor(arg1);
		Integer input2Reg = getRegisterFor(arg2);
		if(resultReg == -1) {
			resultReg = R1;
		}
		if(input1Reg == -1) {
			putArgInR1(arg1);
			input1Reg = R1;
		}
		if(input2Reg == -1) {
			putArgInR2(arg2);
			input2Reg = R2;
		}
		
		prepareRegister(resultReg,resultID);
		instructions.add(new ByteInstruction(opc, resultReg, input1Reg, input2Reg));
	}
	
	private void makeTwoRegOneValueInstruction(int opc, InstructionID resultID, Argument arg, int v) {
		Integer resultReg = getRegisterFor(resultID);
		Integer inputReg = getRegisterFor(arg);
		if(inputReg == -1) {
			putArgInR1(arg);
			inputReg = R1;
		}
		if(resultReg == -1) {
			resultReg = R2;
		}
		prepareRegister(resultReg,resultID);
		instructions.add(new ByteInstruction(opc, resultReg, inputReg, v));
	}
	
	private void makeBranchInstruction(BasicBlockID targetID) {
		instructions.add(new ByteJumpInstruction(JSR, 0, 0, 0, targetID));
	}
	
	private void makeBranchInstruction(BasicBlockID targetID, InstructionID inputID, InstructionType branchType) {
		Integer inputReg = getRegisterFor(inputID);
		if(inputReg == -1) {
			putArgInR1(inputID);
			inputReg = R1;
		}
		switch(branchType) {
		case BNE:
			instructions.add(new ByteJumpInstruction(BNE, inputReg, 0, 0, targetID));
			break;
		case BEQ:
			instructions.add(new ByteJumpInstruction(BEQ, inputReg, 0, 0, targetID));
			break;
		case BLE:
			instructions.add(new ByteJumpInstruction(BLE, inputReg, 0, 0, targetID));
			break;
		case BLT:
			instructions.add(new ByteJumpInstruction(BLT, inputReg, 0, 0, targetID));
			break;
		case BGE:
			instructions.add(new ByteJumpInstruction(BGE, inputReg, 0, 0, targetID));
			break;
		case BGT:
			instructions.add(new ByteJumpInstruction(BGT, inputReg, 0, 0, targetID));
			break;
		default:
			System.out.println("Error: bad branch type for branch instruction");
		}
	}
	
	private void makeFunctionCallInstructions(Argument[] args, InstructionID resultID, BasicBlock bb) {
		Integer resultReg = getRegisterFor(resultID);
		FunctionName fn = (FunctionName) args[0];
		String f = fn.getName();
		if (f.equals("InputNum")) {
			if(resultReg == -1) {
				resultReg = R1;
			}
			prepareRegister(resultReg,resultID);
			instructions.add(new ByteInstruction(RDD,resultReg,0,0));
		} else if (f.equals("OutputNum")) {
			Integer inputReg = getRegisterFor(args[1]);
			if(inputReg == -1) {
				putArgInR1(args[1]);
				inputReg = R1;
			}
			instructions.add(new ByteInstruction(WRD,0,inputReg,0));
		} else if (f.equals("OutputNewLine")) {
			instructions.add(new ByteInstruction(WRL,0,0,0));
		} else {
			instructions.add(new ByteInstruction(PSH, FRAME_POINTER, STACK_POINTER, 4)); //Push old frame pointer
			instructions.add(new ByteInstruction(ADDI, FRAME_POINTER, STACK_POINTER, 4)); //Put frame pointer at new position
			//Push parameters
			for(int i = 1; i < args.length; i++) {
				Integer paramReg = getRegisterFor(args[i]);
				if(paramReg == -1) {
					putArgInR1(args[i]);
					paramReg = R1;
				}
				instructions.add(new ByteInstruction(PSH, paramReg, STACK_POINTER, 4));
			}
			//Push variables
			Integer size = basicBlockIDToSizeOfVariables.get(functionToBasicBlockID.get(f));
			instructions.add(new ByteInstruction(PSH, 0, STACK_POINTER, size));
			
			//Push registers
			for(int i = 4; i <= 27; i++) {
				instructions.add(new ByteInstruction(PSH, i, STACK_POINTER, 4));
			}
			
			prepareRegister(R1,null);
			prepareRegister(R2,null);
			int retAddress = 4*(instructions.size()+3);
			ByteInstruction retValInstruction = new ByteInstruction(ADDI, R1, ZERO_REG, retAddress);
			instructions.add(retValInstruction);
			instructions.add(new ByteInstruction(PSH, R1, STACK_POINTER, 4));
			instructions.add(new ByteJumpInstruction(JSR,0,0,0,functionToBasicBlockID.get(f)));
			
			Integer returnReg = getRegisterFor(resultID);
			if(returnReg == -1) {
				Integer offset = getOffset(resultID);
				if(offset == null)
					return;//This means that the instructionID we would return into is not a variable
				returnReg = R1;
			}
			prepareRegister(returnReg,resultID);
			instructions.add(new ByteInstruction(ADDI,returnReg,RETURN_VALUE,0));
		}
	}
	
	private void makeReturnStatementInstructions(Argument[] args) {
		prepareRegister(R1,null);
		prepareRegister(R2,null);
		instructions.add(new ByteInstruction(POP, R1, STACK_POINTER, -4));// Pop return address
		
		//Pop saved registers
		for(int i = 27; i >= 4; i--) {
			instructions.add(new ByteInstruction(POP, i, STACK_POINTER, -4));
		}
		
		instructions.add(new ByteInstruction(ADDI, STACK_POINTER, FRAME_POINTER, -8));//Stack pointer set to old stack pointer
		instructions.add(new ByteInstruction(ADDI, FRAME_POINTER, FRAME_POINTER, -4));//Frame pointer now points at old frame location
		instructions.add(new ByteInstruction(ADDI, FRAME_POINTER, FRAME_POINTER, 0));//Previous frame
		
		if(args.length != 0) {
			Integer reg = getRegisterFor(args[0]);
			if(reg == -1) {
				reg = R1;
				putArgInR1(args[0]);
			}
			prepareRegister(reg, args[0]);
			instructions.add(new ByteInstruction(ADDI, RETURN_VALUE, reg, 0));
		}
	}
	
	public void fixUpJumpsAndReturns() {
		for(int i = 0; i < instructions.size(); i++) {
			ByteInstruction ri = instructions.get(i);
			if(ri instanceof ByteJumpInstruction) {
				int target = bbIDToLoc.get(((ByteJumpInstruction)ri).getTargetBBID());
				int current = i;
				int op = ri.opCode;
				if(op == JSR)
					ri.c = 4*target;
				else
					ri.c = target - current;
			}
		}
	}
}
