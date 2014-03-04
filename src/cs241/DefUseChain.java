package cs241;

import java.util.HashMap;
import java.util.Map;

import cs241.Argument.BasicBlockID;
import cs241.Argument.InstructionID;

public class DefUseChain {
	public class DefUse {
		private String variable;
		private InstructionID defInstruction;//The line where the value was assigned
		private Argument arg;//The value assigned at the def
		private InstructionID useInstruction;//The location of the use
		private BasicBlockID bbID;//Basic block of use
		private DefUse prevDU;
		private DefUse nextDU;
		private DefUse(String v, InstructionID d, Argument a, InstructionID u, BasicBlockID b) {
			variable = v;
			defInstruction = d;
			arg = a;
			useInstruction = u;
			bbID = b;
		}
		
		public String getVariable() {
			return variable;
		}
		
		public Argument getArgumentForVariable() {
			return arg;
		}
		
		public InstructionID getDefLocation() {
			return defInstruction;
		}
		
		public InstructionID getUseLocation() {
			return useInstruction;
		}
		
		public BasicBlockID getBasicBlockID() {
			return bbID;
		}
		
		public DefUse getNextDefUse() {
			return nextDU;
		}
		
		public DefUse getPreviousDefUse() {
			return prevDU;
		}
	}
	
	private Map<String,DefUse> varToMostRecent;
	
	public DefUseChain() {
		varToMostRecent = new HashMap<String,DefUse>();
	}
	
	public void addDefUse(String v, InstructionID d, Argument a, InstructionID u, BasicBlockID b)  {
		DefUse use = new DefUse(v, d, a, u, b);
		DefUse prev = varToMostRecent.get(v);
		if(prev != null) {
			prev.nextDU = use;
			use.prevDU = prev;
		}
		varToMostRecent.put(v, use);
	}
	
	public void updateUse(DefUse du, InstructionID newDef, Argument newArg) {
		du.defInstruction = newDef;
		du.arg = newArg;
	}
	
	public DefUse getMostRecentDefUse(String var) {
		return varToMostRecent.get(var);
	}
}
