package cs241;

import java.util.ArrayList;
import java.util.List;


public abstract class Argument {
	
	public Argument() {
	}
	
	public boolean isVariable() {
		return false;
	}
	
	public abstract int hashCode();
	
	public boolean equals(Object o) {
		return this.getClass().equals(o.getClass());
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
		public boolean equals(Object arg) {
			return super.equals(arg) && this.equals((Value)arg);
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
		public boolean equals(Object arg) {
			return super.equals(arg) && this.equals((BasicBlockID)arg);
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
		public boolean equals(Object arg) {
			return super.equals(arg) && this.equals((InstructionID)arg);
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
	
	public static class FunctionArg extends Argument {
		private String name;
		private String function;
		public FunctionArg(String f, String n) {
			function = f;
			name = n;
		}
		public String getName() {
			return name;
		}
		public String getFunction() {
			return function;
		}
		public int hashCode() {
			return name.hashCode() + function.hashCode();
		}
		public boolean equals(Object arg) {
			return super.equals(arg) && this.equals((FunctionArg)arg);
		}
		public boolean equals(FunctionArg farg) {
			return name.equals(farg.getName()) && function.equals(farg.getFunction());
		}
		public String toString() {
			return "FunctionArg." + function + "." + name;
		}
		public Argument clone() {
			return new FunctionArg(function,name);
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
		public boolean equals(Object arg) {
			return super.equals(arg) && this.equals((DesName)arg);
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
		public boolean equals(Object arg) {
			return super.equals(arg) && this.equals((FunctionName)arg);
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
			assert vl instanceof Value || vl instanceof InstructionID || vl instanceof FunctionArg;
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
		public int hashCode() {
			return var.hashCode() + 3*val.hashCode() + 5*def.hashCode();
		}
		
		public boolean isVariable() {
			return true;
		}
		public boolean equals(Object arg) {
			return super.equals(arg) && this.equals((VariableArg)arg);
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
		private List<String> copiedVars;
		private List<InstructionID> copyDefs;
		public CopiedVariable(String cvr, String vr, Argument vl, InstructionID d, InstructionID cd) {
			super(vr,vl,d);
			copiedVars = new ArrayList<String>();
			copiedVars.add(cvr);
			copyDefs = new ArrayList<InstructionID>();
			copyDefs.add(cd);
		}
		public CopiedVariable(String cvr, VariableArg v, InstructionID cd) {
			this(cvr,v.getVariableName(),v.getValue(),v.getDef(),cd);
		}
		public CopiedVariable(String cvr, CopiedVariable v, InstructionID cd) {
			super(v.getVariableName(),v.getValue(),v.getDef());
			copiedVars = new ArrayList<String>(v.getCopyChain());
			copiedVars.add(cvr);
			copyDefs = new ArrayList<InstructionID>(v.getCopyDefs());
			copyDefs.add(cd);
		}
		public void addCopy(String c, InstructionID d) {
			copiedVars.add(c);
			copyDefs.add(d);
		}
		public List<String> getCopyChain() {
			return copiedVars;
		}
		public List<InstructionID> getCopyDefs() {
			return copyDefs;
		}
		public boolean equals(Object arg) {
			return super.equals(arg) && this.equals((CopiedVariable)arg);
		}
		public boolean equals(CopiedVariable v) {
			if(!super.equals((VariableArg)v))
				return false;
			if(copiedVars.size() != v.getCopyChain().size())
				return false;
			for(int i = 0; i < copiedVars.size(); i++) {
				if(!copiedVars.get(i).equals(v.getCopyChain().get(i))) {
					return false;
				}
				if(!copyDefs.get(i).equals(v.getCopyDefs().get(i))) {
					return false;
				}
			}
			return true;
		}
		
		public boolean copyOf(VariableArg v) {
			if(!super.equals(v))
				return false;
			if(!(v instanceof CopiedVariable))
				return true;
			CopiedVariable cv = (CopiedVariable)v;
			if(cv.getCopyChain().size() > copiedVars.size())
				return false;
			for(int i = 0; i < cv.getCopyChain().size(); i++) {
				if(!copiedVars.get(i).equals(cv.getCopyChain().get(i))) {
					return false;
				}
				if(!copyDefs.get(i).equals(cv.getCopyDefs().get(i))) {
					return false;
				}
			}
			return true;
		}
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			for(int i = copiedVars.size() - 1; i >= 0; i--) {
				sb.append(copiedVars.get(i));
				sb.append(".");
				sb.append(copyDefs.get(i));
				sb.append(".copyof.");
			}
			sb.append(super.toString());
			return sb.toString();
		}
		public Argument clone() {
			CopiedVariable cv = new CopiedVariable(copiedVars.get(0),(VariableArg)super.clone(),copyDefs.get(0));
			for(int i = 1; i < copiedVars.size(); i++) {
				cv.addCopy(copiedVars.get(i), copyDefs.get(i));
			}
			return cv;
		}
	}
}
