package cs241;

import java.util.HashMap;
import java.util.Map;

import cs241.Argument.InstructionID;

public class DefUseChain {
	public class DefUse {
		String var;
		InstructionID def;//The line where the value was assigned
		Argument arg;//The value assigned at the def
		InstructionID use;//The location of the use
		DefUse prev;
		DefUse next;
		public DefUse(String v, InstructionID d, Argument a, InstructionID u) {
			var = v;
			def = d;
			arg = a;
			use = u;
		}
	}
	
	Map<String,DefUse> varToMostRecent;
	
	public DefUseChain() {
		varToMostRecent = new HashMap<String,DefUse>();
	}
	
	public void addDefUse(String v, InstructionID d, Argument a, InstructionID u)  {
		DefUse use = new DefUse(v, d, a, u);
		DefUse prev = varToMostRecent.get(v);
		if(prev != null) {
			prev.next = use;
			use.prev = prev;
		}
		varToMostRecent.put(v, use);
	}
}
