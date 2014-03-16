package cs241;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cs241.Argument.BasicBlockID;
import cs241.Argument.DesName;
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
	public static final int R0 = 0;
	public static final int R1 = 1;
	public static final int R2 = 2;
	public static final int R28 = 28;
	public static final int R29 = 29;
	public static final int R30 = 30;
	public static final int R31 = 31;
	
	public static int[] instructionOpCodeToFormat = {F2, F2, F2, F2, F2, F2, NONE, NONE, F2, F2, F2, F2, F2, F2, F2, //14
									NONE, F1, F1, F1, F1, F1, F1, NONE, NONE, F1, F1, F1, F1, F1, F1, F1, //30
									NONE, F1, F2, F1, NONE, F1, F2, F1, //38
									NONE, F1, F1, F1, F1, F1, F1, F1, NONE, F3, F2, //49
									F2, F2, F2, F1};

	public class RealInstructionListFactory {
		/*
		 * The instruction to register map should not use registers 0,28,29,30,31,1,2
		 */
		Map<InstructionID,Integer> instructionToRegister;
		
		/*
		 * The variable to offset map should map global variables and arrays to particular offsets from R30
		 */
		Map<String,Integer> variableToOffset;
		
		/*
		 * The instruction to offset map should map spilled instructions to particular offsets from R30
		 */
		Map<InstructionID,Integer> instructionToOffset;
		//For now we assume everything is on the heap
		
		Map<String,BasicBlockID> functionToBasicBlockID;

		InstructionID currentR1;
		InstructionID currentR2;
		
		List<RealInstruction> instructions;
		public RealInstructionListFactory(Map<InstructionID,Integer> insToReg, Map<String,Integer> varToOff, Map<InstructionID,Integer> insToOff, Map<String,BasicBlockID> funToBBID) {
			instructionToRegister = insToReg;
			instructionToOffset = insToOff;
			variableToOffset = varToOff;
			functionToBasicBlockID = funToBBID;
			instructions = new ArrayList<RealInstruction>();
		}
		
		public List<RealInstruction> getRealInstructionList() {
			return instructions;
		}
		
		public void makeRealInstructions(BasicBlock bb) {
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
						Integer offset = variableToOffset.get(v);
						Integer resultReg = instructionToRegister.get(resultID);
						if (resultReg == -1) {
							resultReg = findRegisterFor(resultID);
						}
						instructions.add(new RealInstruction(LDW,resultReg,R30,offset));
					} else {
						//Array load
						DesName var = (DesName)i.args[0];
						InstructionID index = (InstructionID)i.args[1];
						String v = var.getName();
						Integer offset = variableToOffset.get(v);
						Integer resultReg = instructionToRegister.get(resultID);
						Integer indexReg = instructionToRegister.get(index);
						if (resultReg == -1 || indexReg == -1) {
							//TODO: handle 2 spilled variables
						}
						instructions.add(new RealInstruction(ADDI,R2,indexReg,offset));
						currentR2 = null;
						instructions.add(new RealInstruction(LDX,resultReg,R30,R2));
					}
					break;
				case STORE:
					if(i.args.length == 2) {
						//Global variable store
						DesName var = (DesName)i.args[0];
						Integer valReg;
						if(i.args[1] instanceof InstructionID) {
							InstructionID val = (InstructionID)i.args[1];
							valReg = instructionToRegister.get(val);
							if(valReg == -1) {
								valReg = findRegisterFor(val);
							}
						} else {
							Value val = (Value)i.args[1];
							valReg = R1;
							currentR1 = null;
							instructions.add(new RealInstruction(ADDI,valReg,R0,val.getValue()));
						}
						String v = var.getName();
						Integer offset = variableToOffset.get(v);
						instructions.add(new RealInstruction(STW,valReg,R30,offset));
					} else {
						//Array store
						DesName var = (DesName)i.args[0];
						Integer valReg;
						if(i.args[1] instanceof InstructionID) {
							InstructionID val = (InstructionID)i.args[1];
							valReg = instructionToRegister.get(val);
						} else {
							Value val = (Value)i.args[1];
							valReg = R1;
							currentR1 = null;
							instructions.add(new RealInstruction(ADDI,valReg,R0,val.getValue()));
						}
						InstructionID index = (InstructionID)i.args[2];
						String v = var.getName();
						Integer offset = variableToOffset.get(v);
						Integer indexReg = instructionToRegister.get(index);
						if(valReg == -1 || indexReg == -1) {
							//TODO: handle 2 spilled variables
						}
						instructions.add(new RealInstruction(ADDI,R2,indexReg,offset));
						currentR2 = null;
						instructions.add(new RealInstruction(STX,valReg,R30,R2));
					}
					break;
				case MOVE:
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
		}
		
		private Integer findRegisterFor(InstructionID id) {
			Integer reg;
			if (currentR1 != null && id.equals(currentR1)) {
				reg = R1;
			} else if (currentR2 != null && id.equals(currentR2)) {
				reg = R2;
			} else {
				if(currentR1 == null || currentR2 == null) {
					if(currentR1 == null) {
						reg = R1;
						currentR1 = id;
					} else {
						reg = R2;
						currentR2 = id;
					}
				} else {
					reg = R1;//TODO: pick randomly?
					currentR1 = id;
					
					Integer offset = instructionToOffset.get(id);
					instructions.add(new RealInstruction(LDW, reg, R30, offset));
				}
			}
			return reg;
		}

		private void decideInstructionType(Argument a1, Argument a2, int opcode, int opcodei, InstructionID resultID) {
			if(a1 instanceof Value) {
				makeTwoRegOneValueInstruction(opcodei, resultID, (InstructionID)a2, ((Value)a1).getValue());
			} else if(a2 instanceof Value) {
				makeTwoRegOneValueInstruction(opcodei, resultID, (InstructionID)a1, ((Value)a2).getValue());
			} else {
				makeThreeRegInstruction(opcode, resultID, (InstructionID)a1, (InstructionID)a2);
			}
		}
		
		private void decideInstructionTypeNoncomm(Argument a1, Argument a2, int opcode, int opcodei, InstructionID resultID) {
			if(a1 instanceof Value) {
				return;//TODO: cannot put constant as first arg for immediate instruction
			} else if(a2 instanceof Value) {
				makeTwoRegOneValueInstruction(opcodei, resultID, (InstructionID)a1, ((Value)a2).getValue());
			} else {
				makeThreeRegInstruction(opcode, resultID, (InstructionID)a1, (InstructionID)a2);
			}
		}
		
		private void makeThreeRegInstruction(int opc, InstructionID resultID, InstructionID input1ID, InstructionID input2ID) {
			Integer resultReg = instructionToRegister.get(resultID);
			Integer input1Reg = instructionToRegister.get(input1ID);
			Integer input2Reg = instructionToRegister.get(input2ID);
			if(resultReg == -1 || input1Reg == -1 || input2Reg == -1) {
				//TODO: handle spilled variables
			}
			instructions.add(new RealInstruction(opc, resultReg, input1Reg, input2Reg));
		}
		
		private void makeTwoRegOneValueInstruction(int opc, InstructionID resultID, InstructionID inputID, int v) {
			Integer resultReg = instructionToRegister.get(resultID);
			Integer inputReg = instructionToRegister.get(inputID);
			if(resultReg == -1 || inputReg == -1) {
				//TODO: handle spilled variables
			}
			instructions.add(new RealInstruction(opc, resultReg, inputReg, v));
		}
		private void makeBranchInstruction(BasicBlockID targetID) {
			instructions.add(new RealBranchInstruction(BSR, 0, 0, 0, targetID));//TODO: is it always correct to use BSR?
		}
		private void makeBranchInstruction(BasicBlockID targetID, InstructionID inputID, InstructionType branchType) {
			Integer inputReg = instructionToRegister.get(inputID);
			if(inputReg == -1) {
				inputReg = findRegisterFor(inputID);
			}
			switch(branchType) {
			case BNE:
				instructions.add(new RealBranchInstruction(BNE, inputReg, 0, 0, targetID));
				break;
			case BEQ:
				instructions.add(new RealBranchInstruction(BEQ, inputReg, 0, 0, targetID));
				break;
			case BLE:
				instructions.add(new RealBranchInstruction(BLE, inputReg, 0, 0, targetID));
				break;
			case BLT:
				instructions.add(new RealBranchInstruction(BLT, inputReg, 0, 0, targetID));
				break;
			case BGE:
				instructions.add(new RealBranchInstruction(BGE, inputReg, 0, 0, targetID));
				break;
			case BGT:
				instructions.add(new RealBranchInstruction(BGT, inputReg, 0, 0, targetID));
				break;
			default:
				System.out.println("Error: bad branch type for branch instruction");
			}
		}
		
		private void makeFunctionCallInstructions(Argument[] args, InstructionID resultID) {
			Integer resultReg = instructionToRegister.get(resultID);
			FunctionName fn = (FunctionName) args[0];
			String f = fn.getName();
			if (f.equals("INPUTNUM")) {
				instructions.add(new RealInstruction(RDD,resultReg,0,0));
			} else if (f.equals("OUTPUTNUM")) {
				Integer inputReg = instructionToRegister.get((InstructionID)args[0]);
				if(inputReg == -1) {
					//TODO: handle spilled variables
					inputReg = findRegisterFor((InstructionID)args[0]);
				}
				instructions.add(new RealInstruction(WRD,0,inputReg,0));
			} else if (f.equals("OUTPUTNEWLINE")) {
				instructions.add(new RealInstruction(WRL,0,0,0));
			} else {
				//TODO: general function call
			}
		}
		
		private void makeReturnStatementInstructions(Argument[] args) {
			// TODO Auto-generated method stub
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
		return (byte) ((opCode<< (27)) | (a << 21) | (b << 16) | c);
	}
	
	/*
	 * Use RealBranchInstruction during conversion, then once basicblock locations are know scan back through and fix things up
	 */
	private class RealBranchInstruction extends RealInstruction {
		private BasicBlockID branchID;
		private RealBranchInstruction(int opc, int a, int b, int c, BasicBlockID bbID) {
			super(opc, a, b, c);
			branchID = bbID;
		}
		public BasicBlockID getTargetBBID() {
			return branchID;
		}
	}
	
	public static void fixUpBasicBlockIDs(List<RealInstruction> ins, Map<BasicBlockID,Integer> bbIDToLoc) {
		for(int i = 0; i < ins.size(); i++) {
			RealInstruction ri = ins.get(i);
			if(ri instanceof RealBranchInstruction) {
				ri.c = bbIDToLoc.get(((RealBranchInstruction)ri).getTargetBBID());
			}
		}
	}
}
