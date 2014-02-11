package cs241;


public abstract class Argument {
	String var;
	
	public Argument(String v) {
		var = v;
	}
	
	public String getVarName() {
		return var;
	}
	
	public String toString() {
		return var;
	}
	
	public int hashCode() {
		return var.hashCode();
	}
	
	public static class Value extends Argument {
		private int val;
		public Value(int v, String vs) {
			super(vs);
			val = v;
		}
		public int getValue() {
			return val;
		}
		public int hashCode() {
			return 37*val + super.hashCode();
		}
		public String toString() {
			return super.toString() + ".#." + val;
		}
	}
	
	public static class BasicBlockID extends Argument {
		private int bbID;
		public BasicBlockID(int id) {
			super("BB");
			bbID = id;
		}
		public int getID() {
			return bbID;
		}
		public int hashCode() {
			return 31*bbID + super.hashCode();
		}
		public String toString() {
			return super.toString() + "." + bbID;
		}
	}
	
	public static class InstructionID extends Argument {
		private int instructionID;
		public InstructionID(int id, String v) {
			super(v);
			instructionID = id;
		}
		public int getID() {
			return instructionID;
		}
		public int hashCode() {
			return 43*instructionID + super.hashCode();
		}
		public String toString() {
			return super.toString() + ".Instruction." + instructionID;
		}
	}
	
	public static class ArrayName extends Argument {
		private String name;
		public ArrayName(String n) {
			super("Arr");
			name = n;
		}
		public String getName() {
			return name;
		}
		public int hashCode() {
			return name.hashCode() + super.hashCode();
		}
		public String toString() {
			return super.toString() + "." + name;
		}
	}
	public static class FunctionName extends Argument {
		private String name;
		public FunctionName(String n) {
			super("Fun");
			name = n;
		}
		public String getName() {
			return name;
		}
		public int hashCode() {
			return name.hashCode() + super.hashCode();
		}
		public String toString() {
			return super.toString() + "." + name;
		}
	}
}
