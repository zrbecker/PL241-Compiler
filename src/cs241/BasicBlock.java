package cs241;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;

/**`
 * Class to keep track of what code is in what basic block
 */
public class BasicBlock {
	//Code structure
	BasicBlock parent;
	List<BasicBlock> children;
	
	//Dominator tree
	BasicBlock dominator;
	List<BasicBlock> dominated;
	
	List<Instruction> instructions;

	public BasicBlock() {
		children = new ArrayList<BasicBlock>();
		dominated = new ArrayList<BasicBlock>();
		instructions = new LinkedList<Instruction>();
	}
	
	public void setParent(BasicBlock p) {
		parent = p;
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
