package cs241;


public abstract class Argument {
	String var;
	
	public Argument(String v) {
		var = v;
	}
	
	public int hashCode() {
		return var.hashCode();
	}
	
	public String toString() {
		if(var.equals(""))
			return "";
		return var + ".";
	}
	
	public abstract Argument clone();
	
	public static class Value extends Argument {
		private int val;
		public Value(int v) {
			super("");
			val = v;
		}
		public Value(int v, String var) {
			super(var);
			val = v;
		}
		public int getValue() {
			return val;
		}
		public int hashCode() {
			return 37*val + super.hashCode();
		}
		public String toString() {
			return super.toString() + "#." + val;
		}
		public Argument clone() {
			return new Value(val);
		}
	}
	
	public static class BasicBlockID extends Argument {
		private int bbID;
		public BasicBlockID(int id) {
			super("");
			bbID = id;
		}
		public int getID() {
			return bbID;
		}
		public int hashCode() {
			return 31*bbID + super.hashCode();
		}
		public String toString() {
			return super.toString() + "BB." + bbID;
		}
		public Argument clone() {
			return new BasicBlockID(bbID);
		}
	}
	
	public static class InstructionID extends Argument {
		private int instructionID;
		public InstructionID(int id, String var) {
			super(var);
			instructionID = id;
		}
		public int getID() {
			return instructionID;
		}
		public int hashCode() {
			return 43*instructionID + super.hashCode();
		}
		public String toString() {
			return super.toString() + "Instruction." + instructionID;
		}
		public Argument clone() {
			return new InstructionID(instructionID, var);
		}
	}
	
	public static class DesName extends Argument {
		private String name;
		public DesName(String n) {
			super("");
			name = n;
		}
		public String getName() {
			return name;
		}
		public int hashCode() {
			return name.hashCode() + super.hashCode();
		}
		public String toString() {
			return super.toString() + "Des." + name;
		}
		public Argument clone() {
			return new DesName(name);
		}
	}
	public static class FunctionName extends Argument {
		private String name;
		public FunctionName(String n) {
			super("");
			name = n;
		}
		public String getName() {
			return name;
		}
		public int hashCode() {
			return name.hashCode() + super.hashCode();
		}
		public String toString() {
			return super.toString() + "Func." + name;
		}
		public Argument clone() {
			return new FunctionName(name);
		}
	}
}
