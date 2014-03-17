package cs241;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cs241.Argument.BasicBlockID;
import cs241.Argument.DesName;
import cs241.Argument.FunctionArg;
import cs241.Argument.FunctionName;
import cs241.Argument.InstructionID;
import cs241.Argument.Value;
import cs241.Instruction.InstructionType;

public class RealInstruction {
	
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
	public static final int F1 = -1;
	public static final int F2 = -2;
	public static final int F3 = -3;
	public static final int NONE = -4;
	public static final int ZERO_REG = 0; //Always 0
	public static final int R1 = 1; //First register for spillage
	public static final int R2 = 2; //Second register for spillage
	public static final int RETURN_VALUE = 3; //Return value of functions
	public static final int FRAME_POINTER = 28;
	public static final int STACK_POINTER = 29;
	public static final int HEAP_MEMORY_LOCATION = 30; //Heap location
	public static final int BRANCH_RETURN_ADDRESS = 31; //Return address
	
	public static int[] instructionOpCodeToFormat = {F2, F2, F2, F2, F2, F2, NONE, NONE, F2, F2, F2, F2, F2, F2, F2, //14
									NONE, F1, F1, F1, F1, F1, F1, NONE, NONE, F1, F1, F1, F1, F1, F1, F1, //30
									NONE, F1, F2, F1, NONE, F1, F2, F1, //38
									NONE, F1, F1, F1, F1, F1, F1, F1, NONE, F3, F2, //49
									F2, F2, F2, F1};

	public class RealInstructionListFactory {
		/*
		 * The instruction to register map should not use registers 0,28,29,30,31,1,2,3
		 */
		Map<InstructionID,Integer> instructionToRegister;
		
		/*
		 * The heap variable to offset map should map global variables and arrays to particular offsets from R30
		 */
		Map<String,Integer> heapVariableToOffset;
		
		/*
		 * The stack variable to offset map should map function parameters to particular offsets from R28
		 */
		Map<FunctionArg,Integer> stackVariableToOffset;
		
		/*
		 * The instruction to offset map should map spilled instructions to particular offsets from R30
		 */
		Map<InstructionID,Integer> instructionToOffset;
		
		//For now we assume everything is on the heap
		
		Map<String,BasicBlockID> functionToBasicBlockID;
		

		Argument currentR1;
		Argument currentR2;
		
		List<RealInstruction> instructions;
		public RealInstructionListFactory(Map<InstructionID,Integer> insToReg, Map<String,Integer> varToOff, Map<InstructionID,Integer> insToOff, Map<String,BasicBlockID> funToBBID) {
			instructionToRegister = insToReg;
			instructionToOffset = insToOff;
			heapVariableToOffset = varToOff;
			functionToBasicBlockID = funToBBID;
			instructions = new ArrayList<RealInstruction>();
		}
		
		public List<RealInstruction> getRealInstructionList() {
			return instructions;
		}
		
		public int makeRealInstructions(BasicBlock bb) {
			int indexToReturn = instructions.size();
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
					if (i.args.length == 1) {//TODO: what if index is a value
						//Global variable load
						DesName var = (DesName)i.args[0];
						String v = var.getName();
						Integer offset = heapVariableToOffset.get(v);
						Integer resultReg = getRegisterFor(resultID);
						
						if (resultReg == -1) {
							resultReg = R2;
						}
						
						prepareRegister(resultReg,resultID);
						instructions.add(new RealInstruction(LDW,resultReg,HEAP_MEMORY_LOCATION,offset));
					} else {
						//Array load
						DesName var = (DesName)i.args[0];
						String v = var.getName();
						Integer offset = heapVariableToOffset.get(v);
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
							instructions.add(new RealInstruction(ADDI,R2,indexReg,offset));
							prepareRegister(resultReg,resultID);
							instructions.add(new RealInstruction(LDX,resultReg,HEAP_MEMORY_LOCATION,R2));
						} else if (i.args[1] instanceof Value) {
							Value index = (Value)i.args[1];
							if(resultReg == -1) {
								resultReg = R2;
							}
							
							prepareRegister(resultReg, resultID);
							instructions.add(new RealInstruction(LDW,resultReg,HEAP_MEMORY_LOCATION,offset + index.getValue()));
						} else {
							System.out.println("Error: unrecognized argument type.");
						}
					}
					break;
				case STORE:
					if(i.args.length == 2) {
						//Global variable store
						DesName var = (DesName)i.args[0];
						String v = var.getName();
						Integer offset = heapVariableToOffset.get(v);
						if(i.args[1] instanceof InstructionID || i.args[1] instanceof FunctionArg) {
							Argument val = (Argument)i.args[1];
							Integer valReg = getRegisterFor(val);
							if(valReg == -1) {
								putArgInR2(val);
								valReg = R2;
							}
							
							instructions.add(new RealInstruction(STW,valReg,HEAP_MEMORY_LOCATION,offset));
						} else if (i.args[1] instanceof Value) {
							Value val = (Value)i.args[1];
							putArgInR1(val);
							instructions.add(new RealInstruction(STW,R1,HEAP_MEMORY_LOCATION,offset));
						} else {
							System.out.println("Error: unrecognized argument type.");
						}
					} else {
						//Array store
						DesName var = (DesName)i.args[1];
						String v = var.getName();
						Integer offset = heapVariableToOffset.get(v);
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
							instructions.add(new RealInstruction(ADDI,R2,indexReg,offset));
							instructions.add(new RealInstruction(STX,valReg,HEAP_MEMORY_LOCATION,R2));
						} else {
							Value index = (Value)i.args[2];
							if(valReg == -1) {
								putArgInR1(i.args[0]);
								valReg = R1;
							}
							instructions.add(new RealInstruction(STX,valReg,HEAP_MEMORY_LOCATION,offset + index.getValue()));
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
						instructions.add(new RealInstruction(ADDI, targetReg, valueReg, 0));
					} else {
						Value value = (Value)i.args[0];
						if(targetReg == -1) {
							targetReg = R2;
						}
						prepareRegister(targetReg,value);
						instructions.add(new RealInstruction(ADDI, targetReg, ZERO_REG, value.getValue()));
					}
					break;
				case FUNCTION:
					makeFunctionCallInstructions(i.args, resultID);
					break;
				case CMP:
					decideInstructionTypeNoncomm(i.args[0], i.args[1], CMP, CMPI, resultID);
					//All compares signal the end of the basic block and that we should follow the branch instruction
					conditionalBranch = resultID;
					//Note: there should be no more instructions after a cmp
					break;
				case END:
					instructions.add(new RealInstruction(RET,0,0,0));
					//Note: there should be no more instructions after an end
					break;
				case RETURN:
					makeReturnStatementInstructions(i.args);
					//Note: there should be no more instructions after a ret
					break;
				default:
					System.out.println("Error: unrecognized instruction type.");//LOADADD and STOREADD have been removed and branches are special
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
			return indexToReturn;
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
				Integer offset = instructionToOffset.get(currentR1);
				instructions.add(new RealInstruction(STW, R1, HEAP_MEMORY_LOCATION, offset));
			}
			currentR1 = null;
		}
		
		private void storeR2() {
			if(currentR2 != null && currentR1 instanceof InstructionID) {
				Integer offset = instructionToOffset.get(currentR2);
				instructions.add(new RealInstruction(STW, R2, HEAP_MEMORY_LOCATION, offset));
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
			if(arg instanceof InstructionID) {
				Integer offset = instructionToOffset.get(arg);
				instructions.add(new RealInstruction(LDW, R1, HEAP_MEMORY_LOCATION, offset));
			} else if(arg instanceof Value) {
				instructions.add(new RealInstruction(ADDI, R1, ZERO_REG, ((Value)arg).getValue()));
			} else { //arg instanceof FunctionArg
				Integer offset = stackVariableToOffset.get(arg);
				instructions.add(new RealInstruction(LDW, R1, FRAME_POINTER, offset));
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
			if(arg instanceof InstructionID) {
				Integer offset = instructionToOffset.get(arg);
				instructions.add(new RealInstruction(LDW, R2, HEAP_MEMORY_LOCATION, offset));
			} else if(arg instanceof Value) {
				instructions.add(new RealInstruction(ADDI, R2, ZERO_REG, ((Value)arg).getValue()));
			} else { //arg instanceof FunctionArg
				Integer offset = stackVariableToOffset.get(arg);
				instructions.add(new RealInstruction(LDW, R2, FRAME_POINTER, offset));
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
			instructions.add(new RealInstruction(opc, resultReg, input1Reg, input2Reg));
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
			instructions.add(new RealInstruction(opc, resultReg, inputReg, v));
		}
		
		private void makeBranchInstruction(BasicBlockID targetID) {
			instructions.add(new RealJumpInstruction(JSR, 0, 0, 0, targetID));
		}
		
		private void makeBranchInstruction(BasicBlockID targetID, InstructionID inputID, InstructionType branchType) {
			Integer inputReg = getRegisterFor(inputID);
			if(inputReg == -1) {
				putArgInR1(inputID);
				inputReg = R1;
			}
			switch(branchType) {
			case BNE:
				instructions.add(new RealJumpInstruction(BNE, inputReg, 0, 0, targetID));
				break;
			case BEQ:
				instructions.add(new RealJumpInstruction(BEQ, inputReg, 0, 0, targetID));
				break;
			case BLE:
				instructions.add(new RealJumpInstruction(BLE, inputReg, 0, 0, targetID));
				break;
			case BLT:
				instructions.add(new RealJumpInstruction(BLT, inputReg, 0, 0, targetID));
				break;
			case BGE:
				instructions.add(new RealJumpInstruction(BGE, inputReg, 0, 0, targetID));
				break;
			case BGT:
				instructions.add(new RealJumpInstruction(BGT, inputReg, 0, 0, targetID));
				break;
			default:
				System.out.println("Error: bad branch type for branch instruction");
			}
		}
		
		private void makeFunctionCallInstructions(Argument[] args, InstructionID resultID) {
			Integer resultReg = getRegisterFor(resultID);
			FunctionName fn = (FunctionName) args[0];
			String f = fn.getName();
			if (f.equals("INPUTNUM")) {
				if(resultReg == -1) {
					resultReg = R1;
				}
				prepareRegister(resultReg,resultID);
				instructions.add(new RealInstruction(RDD,resultReg,0,0));
			} else if (f.equals("OUTPUTNUM")) {
				Integer inputReg = getRegisterFor(args[1]);
				if(inputReg == -1) {
					putArgInR1(args[1]);
					inputReg = R1;
				}
				instructions.add(new RealInstruction(WRD,0,inputReg,0));
			} else if (f.equals("OUTPUTNEWLINE")) {
				instructions.add(new RealInstruction(WRL,0,0,0));
			} else {
				//TODO: general function call
				int total = 0;
				instructions.add(new RealInstruction(ADDI, FRAME_POINTER, STACK_POINTER, 4));
				for(int i = 1; i < args.length; i++) {
					Integer paramReg = getRegisterFor(args[i]);
					if(paramReg == -1) {
						putArgInR1(args[i]);
						paramReg = R1;
					}
					instructions.add(new RealInstruction(PSH, paramReg, STACK_POINTER, 4));
					total+=4;
				}
				prepareRegister(R1,null);
				instructions.add(new RealInstruction(ADDI, R1, ZERO_REG, total));
				instructions.add(new RealInstruction(PSH, R1, STACK_POINTER, 4));
				
				RealInstruction retValInstruction = new RealInstruction(ADDI, R1, ZERO_REG, 4*(instructions.size()+4));
				instructions.add(retValInstruction);
				instructions.add(new RealInstruction(PSH, R1, STACK_POINTER, 4));
				instructions.add(new RealJumpInstruction(JSR,0,0,0,functionToBasicBlockID.get(f)));
				
				Integer returnReg = getRegisterFor(resultID);
				if(returnReg == -1) {
					Integer heapOffset = heapVariableToOffset.get(resultID);
					if(heapOffset == null)
						return;//This means that the instructionID we would return into is not a variable
					//TODO: handle return values better
					returnReg = R1;
				}
				instructions.add(new RealInstruction(ADDI,returnReg,RETURN_VALUE,0));
			}//TODO: check what happens at end of functions
		}
		
		private void makeReturnStatementInstructions(Argument[] args) {
			prepareRegister(R1,null);
			prepareRegister(R2,null);
			instructions.add(new RealInstruction(POP, R1, STACK_POINTER, -4));//return address
			instructions.add(new RealInstruction(POP, R2, FRAME_POINTER, -8));//distance to previous frame
			instructions.add(new RealInstruction(ADDI, STACK_POINTER, FRAME_POINTER, 4));
			instructions.add(new RealInstruction(SUB, FRAME_POINTER, FRAME_POINTER, R2));
			if(args.length != 0) {
				Integer reg = getRegisterFor(args[0]);
				if(reg == -1) {
					reg = R1;
					putArgInR1(args[0]);
				}
				prepareRegister(reg, args[0]);
				instructions.add(new RealInstruction(ADDI, RETURN_VALUE, reg, 0));
			}
		}
	}
	
	int opCode;
	int format;
	int a;
	int b;
	int c;
	private RealInstruction(int opc, int a, int b, int c) {
		opCode = opc;
		format = instructionOpCodeToFormat[opc];
		this.a = a;
		this.b = b;
		this.c = c;
		if(format == NONE)
			System.out.println("Error: Instruction has bad format");
		if(a >= 32 || b >= 32)
			System.out.println("Error: args a and b should be less than 32.");
		if(format == F1 && c >= 65536)
			System.out.println("Error: arg c should be less than 65536.");
		if(format == F2 && c >= 32)
			System.out.println("Error: arg c should be less than 32.");
		if(format == F3 && c >= 67108864)
			System.out.println("Error: arg c should be less than 67108864.");
	}
	
	public byte toByte() {
		int opbyte = ((opCode & 0b111111) << (27));
		int abyte = ((a & 0b11111) << 21);
		int bbyte = ((b & 0b11111) << 16);
		int cbyte;
		if(format == F1) {
			cbyte =  (c & 0xFFFF);
		} else if(format == F2) {
			cbyte = (c & 0b11111);
		} else { //format == F3
			cbyte =  (c & 0x6FFFFFF);
		}
		return (byte) (opbyte | abyte | bbyte | cbyte);
	}
	
	/*
	 * Use RealJumpInstruction during conversion, then once basicblock locations are know scan back through and fix things up
	 */
	private class RealJumpInstruction extends RealInstruction {
		private BasicBlockID branchID;
		private RealJumpInstruction(int opc, int a, int b, int c, BasicBlockID bbID) {
			super(opc, a, b, c);
			branchID = bbID;
		}
		public BasicBlockID getTargetBBID() {
			return branchID;
		}
	}
	
	public static void fixUpJumpsAndReturns(List<RealInstruction> ins, Map<BasicBlockID,Integer> bbIDToLoc) {
		for(int i = 0; i < ins.size(); i++) {
			RealInstruction ri = ins.get(i);
			if(ri instanceof RealJumpInstruction) {
				ri.c = bbIDToLoc.get(((RealJumpInstruction)ri).getTargetBBID());
			}
		}
	}
}