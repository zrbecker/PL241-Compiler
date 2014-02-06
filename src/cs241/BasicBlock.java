package cs241;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**`
 * Class to keep track of what code is in what basic block
 */
public class BasicBlock {
	//Code structure
	List<BasicBlock> parents;
	List<BasicBlock> children;
	
	//Dominator tree
	BasicBlock dominator;
	List<BasicBlock> dominated;
	
	List<Instruction> instructions;
	Map<String,Integer> varLookupTable;
	
	public BasicBlock() {
		parents = new ArrayList<BasicBlock>();
		children = new ArrayList<BasicBlock>();
		dominated = new ArrayList<BasicBlock>();
		instructions = new LinkedList<Instruction>();
		varLookupTable = new HashMap<String,Integer>();
	}
	
	public void addParent(BasicBlock p) {
		parents.add(p);
	}

	public void addChild(BasicBlock c) {
		children.add(c);
	}

	public void setDominator(BasicBlock d) {
		dominator = d;
	}

	public void addDominated(BasicBlock d) {
		dominated.add(d);
	}

	public void prependInstruction(Instruction i) {
		instructions.add(0,i);
	} 

	public void appendInstruction(Instruction i) {
		instructions.add(i);
	}
}
