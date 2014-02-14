package cs241;


public abstract class Argument {
	private String var;
	
	public Argument() {
		var = "";
	}
	
	public Argument(String v) {
		var = v;
	}

	public boolean hasVariable() {
		return !var.equals("");
	}
	
	public String getVariable() {
		return var;
	}
	
	public void setVariable(String v) {
		var = v;
	}
	
	public int hashCode() {
		return var.hashCode();
	}
	
	public boolean equals(Argument a) {
		return this.getClass().getName().equals(a.getClass().getName());// && var.equals(a.var);
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
		
		public boolean equals(Value v) {
			return val == v.val && super.equals(v);
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
			bbID = id;
		}
		public int getID() {
			return bbID;
		}
		public int hashCode() {
			return 31*bbID + super.hashCode();
		}
		public boolean equals(BasicBlockID id) {
			return bbID == id.bbID && super.equals(id);
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
		public boolean equals(InstructionID id) {
			return instructionID == id.instructionID && super.equals(id);
		}
		public String toString() {
			return super.toString() + "Instruction." + instructionID;
		}
		public Argument clone() {
			return new InstructionID(instructionID, this.getVariable());
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
			return name.equals(dn.name) && super.equals(dn);
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
			name = n;
		}
		public String getName() {
			return name;
		}
		public int hashCode() {
			return name.hashCode() + super.hashCode();
		}
		public boolean equals(FunctionName fn) {
			return name.equals(fn.name) && super.equals(fn);
		}
		public String toString() {
			return super.toString() + "Func." + name;
		}
		public Argument clone() {
			return new FunctionName(name);
		}
	}
}
