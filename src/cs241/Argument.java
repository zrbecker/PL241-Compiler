package cs241;

public abstract class Argument {
	
	public Argument() {
	}
	
	public boolean equals(Argument a) {
		return this.getClass().equals(a.getClass());
	}
	
	public static class Value extends Argument {
		private int val;
		public Value(int v) {
			val = v;
		}
		public int getValue() {
			return val;
		}
		public int hashCode() {
			return 37*val + super.hashCode();
		}
		
		public boolean equals(Value v) {
			return val == v.val;
		}
		
		public String toString() {
			return "#." + val;
		}
	}
	
	public static class BasicBlockID extends Argument {
		private int bbID;
		public BasicBlockID(int id) {
			bbID = id;
		}
		public int getID() {
			return bbID;
		}
		public int hashCode() {
			return 31*bbID + super.hashCode();
		}
		public boolean equals(BasicBlockID id) {
			return bbID == id.bbID;
		}
		public String toString() {
			return "BB." + bbID;
		}
	}
	
	public static class InstructionID extends Argument {
		private int instructionID;
		public InstructionID(int id) {
			instructionID = id;
		}
		public int getID() {
			return instructionID;
		}
		public int hashCode() {
			return 43*instructionID + super.hashCode();
		}
		public boolean equals(InstructionID id) {
			return instructionID == id.instructionID;
		}
		public String toString() {
			return "Instruction." + instructionID;
		}
	}
	
	public static class DesName extends Argument {
		private String name;
		public DesName(String n) {
			name = n;
		}
		public String getName() {
			return name;
		}
		public int hashCode() {
			return name.hashCode() + super.hashCode();
		}
		public boolean equals(DesName dn) {
			return name.equals(dn.name);
		}
		public String toString() {
			return "Des." + name;
		}
	}
	public static class FunctionName extends Argument {
		private String name;
		public FunctionName(String n) {
			name = n;
		}
		public String getName() {
			return name;
		}
		public int hashCode() {
			return name.hashCode() + super.hashCode();
		}
		public boolean equals(FunctionName fn) {
			return name.equals(fn.name);
		}
		public String toString() {
			return "Func." + name;
		}
	}
	public static class VariableArg extends Argument {
		private String var;
		private Argument val;
		private InstructionID def;
		public VariableArg(String vr, Argument vl, InstructionID d) {
			var = vr;
			assert vl instanceof Value || vl instanceof InstructionID || vl instanceof VariableArg;
			if(vl instanceof VariableArg && ((VariableArg) vl).val instanceof VariableArg) {
				vl = ((VariableArg) vl).val;
			}
			val = vl;
			def = d;
		}
		public String getVariableName() {
			return var;
		}
		public Argument getValue() {
			return val;
		}
		public InstructionID getDef() {
			return def;
		}
		public boolean equals(VariableArg v) {
			System.out.println("Comparing " + this + " with " + v);
			return var.equals(v.var) && val.equals(v.val) && def.equals(v.def);
		}
		public String toString() {
			return var + "." + val + "." + def;
		}
	}
}
