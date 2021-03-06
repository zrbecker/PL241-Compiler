package cs241;

import java.util.HashMap;
import java.util.Map;

import cs241.Argument.BasicBlockID;
import cs241.Argument.InstructionID;

public class DefUseChain {
	public class DefUse {
		private InstructionID defInstruction;//The line where the value was deffed
		private Argument arg;//The value assigned at the def
		private InstructionID useInstruction;//The location of the use
		private BasicBlockID bbID;//Basic block of use
		private DefUse prevDU;
		private DefUse nextDU;
		private DefUse(InstructionID d, Argument a, InstructionID u, BasicBlockID b) {
			defInstruction = d;
			arg = a;
			useInstruction = u;
			bbID = b;
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
	
	private Map<InstructionID,DefUse> varToMostRecent;
	
	public DefUseChain() {
		varToMostRecent = new HashMap<InstructionID,DefUse>();
	}
	
	public void addDefUse(InstructionID d, Argument a, InstructionID u, BasicBlockID b)  {
		DefUse use = new DefUse(d, a, u, b);
		DefUse prev = varToMostRecent.get(d);
		if(prev != null) {
			prev.nextDU = use;
			use.prevDU = prev;
		}
		varToMostRecent.put(d, use);
	}
	
	public void updateUse(DefUse du, InstructionID newDef, Argument newArg) {
		du.defInstruction = newDef;
		du.arg = newArg;
	}
	
	public DefUse getMostRecentDefUse(InstructionID d) {
		return varToMostRecent.get(d);
	}

	public void removeDefUse(DefUse use) {
		DefUse prev = use.prevDU;
		DefUse next = use.nextDU;
		if(prev!=null)
			prev.nextDU = next;
		if(next!=null)
			next.prevDU = prev;
	}
}
