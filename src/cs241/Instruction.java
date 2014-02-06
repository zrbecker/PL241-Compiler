package cs241;

import java.util.HashMap;
import java.util.Map;

public class Instruction {
	public enum InstructionType {
		NEG,
		ADD,
		SUB,
		MUL,
		DIV,
		CMP,
		ADDA,
		LOAD,
		STORE,
		MOVE,
		PHI,
		END,
		BRA,
		BNE,
		BEQ,
		BLE,
		BLT,
		BGE,
		BGT,
		READ,
		WRITE,
		WLN
	}

	private static int nextInstructionID = 1;
	private static Map<Integer,Instruction> idToInstruction = new HashMap<Integer,Instruction>();
	
	public static Instruction getInstructionByID(int id) {
		return idToInstruction.get(id);
	}
	public static int nextInstructionID() {
		return nextInstructionID;
	}
	
	InstructionType type;
	int[] args;
	int instructionID;
	public Instruction(InstructionType t, int[] a) {
		type = t;
		args = new int[a.length];
		for(int i = 0; i < a.length; i++) {
			args[i] = a[i];
		}

		switch(t) {
			case NEG:
			case LOAD:
			case BRA:
				assert(args.length == 1);
				break;
			case END:
			case READ:
			case WRITE:
			case WLN:
				assert(args.length == 0);
				break;
			case PHI:
				assert(args.length >= 2);
				break;
			default:
				assert(args.length == 2);
				break;
		}
		instructionID = nextInstructionID;
		nextInstructionID++;
	}

	public int hashCode() {
		int hash = type.hashCode();
		int pow = 107;
		for(int i = 0; i < args.length; i++) {
			hash+=pow*args[i];
			pow*=107;
		}
		return hash;
	}
}
