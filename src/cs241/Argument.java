package cs241;


public abstract class Argument {
	
	public Argument() {
	}
	
	public boolean isVariable() {
		return false;
	}
	
	public boolean equals(Argument a) {
		return this.getClass().equals(a.getClass());
	}
	
	public abstract Argument clone();
	
	public static class Value extends Argument {
		private int val;
		public Value(int v) {
			val = v;
		}
		public int getValue() {
			return val;
		}
		public int hashCode() {
			return 37*val;
		}
		
		public boolean equals(Value v) {
			return val == v.val;
		}
		
		public String toString() {
			return "#." + val;
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
			return 31*bbID;
		}
		public boolean equals(BasicBlockID id) {
			return bbID == id.bbID;
		}
		public String toString() {
			return "BB." + bbID;
		}
		public Argument clone() {
			return new BasicBlockID(bbID);
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
			return 43*instructionID;
		}
		public boolean equals(InstructionID id) {
			return instructionID == id.instructionID;
		}
		public String toString() {
			return "Instruction." + instructionID;
		}
		public Argument clone() {
			return new InstructionID(instructionID);
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
			return name.hashCode();
		}
		public boolean equals(DesName dn) {
			return name.equals(dn.name);
		}
		public String toString() {
			return "Des." + name;
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
			return name.hashCode();
		}
		public boolean equals(FunctionName fn) {
			return name.equals(fn.name);
		}
		public String toString() {
			return "Func." + name;
		}
		public Argument clone() {
			return new FunctionName(name);
		}
	}
	public static class VariableArg extends Argument {
		private String var;
		private Argument val;
		private InstructionID def;
		public VariableArg(String vr, Argument vl, InstructionID d) {
			var = vr;
			assert vl instanceof Value || vl instanceof InstructionID;
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
		public boolean isVariable() {
			return true;
		}
		public boolean equals(VariableArg v) {
			return var.equals(v.var) && val.equals(v.val) && def.equals(v.def);
		}
		public String toString() {
			return var + "." + val + "." + def;
		}
		public void updateValueAndDef(InstructionID newArg, InstructionID newDef) {
			def = newDef;
			val = newArg;
		}
		public Argument clone() {
			return new VariableArg(var,val.clone(),(InstructionID)def.clone());
		}
	}
	public static class CopiedVariable extends VariableArg {
		private String copiedVar;
		private InstructionID copyDef;
		private BasicBlockID bbID;//Basic block of the def
		public CopiedVariable(String cvr, String vr, Argument vl, InstructionID d, InstructionID cd, BasicBlockID b) {
			super(vr,vl,d);
			copiedVar = cvr;
			copyDef = cd;
			bbID = b;
		}
		public CopiedVariable(String cvr, VariableArg v, InstructionID cd, BasicBlockID b) {
			this(cvr,v.getVariableName(),v.getValue(),v.getDef(),cd,b);
		}
		public String getCopiedVar() {
			return copiedVar;
		}
		public InstructionID getCopyDef() {
			return copyDef;
		}
		public BasicBlockID getBasicBlockID() {
			return bbID;
		}
		public boolean equals(CopiedVariable v) {
			return super.equals((VariableArg)v) && copiedVar.equals(v.copiedVar) && copyDef.equals(v.copyDef) && bbID.equals(v.bbID);
		}
		public String toString() {
			return copiedVar + "." + copyDef + "." + bbID + ".copyof." + super.toString();
		}
		public Argument clone() {
			return new CopiedVariable(copiedVar,(VariableArg)super.clone(),copyDef,bbID);
		}
	}
}
