package cs241;

import java.util.HashMap;
import java.util.Map;

import cs241.Argument.InstructionID;

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
		WLN,
		RETURN,
		LOADADD, //For arrays only
		STOREADD, //For arrays only
		FUNCTION //TODO: remove FUNCTION
	}

	private static int nextInstructionID = 1;
	private static Map<InstructionID,Instruction> idToInstruction = new HashMap<InstructionID,Instruction>();
	
	public static Instruction getInstructionByID(InstructionID id) {
		return idToInstruction.get(id);
	}
	
	public static Instruction makeInstruction(InstructionType t, Argument... args) {
		return new Instruction(t,"",args);
	}
	
	public static Instruction makeInstruction(InstructionType t, String s, Argument... args) {
		return new Instruction(t,s,args);
	}
	
	InstructionType type;
	Argument[] args;
	private InstructionID instructionID;
	private Instruction(InstructionType t, String s, Argument[] a) {
		type = t;
		args = new Argument[a.length];
		for(int i = 0; i < a.length; i++) {
			args[i] = a[i];
		}

		switch(t) {
			case FUNCTION:
				assert(args.length >= 1);
				break;
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
			case RETURN:
				assert(args.length == 0 || args.length == 1);
				break;
			case PHI:
			case LOADADD:
			case STOREADD:
				assert(args.length >= 2);
				break;
			default:
				assert(args.length == 2);
				break;
		}
		instructionID = new InstructionID(nextInstructionID,s);
		idToInstruction.put(instructionID, this);
		nextInstructionID++;
	}
	
	public InstructionID getID() {
		return instructionID;
	}
	
	public int hashCode() {
		int hash = type.hashCode();
		int pow = 107;
		for(int i = 0; i < args.length; i++) {
			hash+=pow*args[i].hashCode();
			pow*=107;
		}
		return hash;
	}
	
	public String toString() {
		String s = "\t" + instructionID +": ";
		switch(type) {
		case NEG:
			s += "NEG ";
			break;
		case ADD:
			s += "ADD ";
			break;
		case SUB:
			s += "SUB ";
			break;
		case MUL:
			s += "MUL ";
			break;
		case DIV:
			s += "DIV ";
			break;
		case CMP:
			s += "CMP ";
			break;
		case ADDA:
			s += "ADDA ";
			break;
		case LOAD:
			s += "LOAD ";
			break;
		case STORE:
			s += "STORE ";
			break;
		case MOVE:
			s += "MOVE ";
			break;
		case PHI:
			s += "PHI ";
			break;
		case END:
			s += "END ";
			break;
		case BRA:
			s += "BRA ";
			break;
		case BNE:
			s += "BNE ";
			break;
		case BEQ:
			s += "BEQ ";
			break;
		case BLE:
			s += "BLE ";
			break;
		case BLT:
			s += "BLT ";
			break;
		case BGE:
			s += "BGE ";
			break;
		case BGT:
			s += "BGT";
			break;
		case READ:
			s += "READ ";
			break;
		case WRITE:
			s += "WRITE ";
			break;
		case WLN:
			s += "WLN ";
			break;
		case LOADADD:
			s += "LOADADD ";
			break;
		case FUNCTION:
			s += "FUNCTION ";
			break;
		}
		for(Argument arg : args) {
			s += arg.toString() + " ";
		}
		s += "\n";
		return s;
	}
}
