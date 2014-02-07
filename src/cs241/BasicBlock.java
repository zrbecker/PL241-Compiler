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
	List<BasicBlock> children;
	List<BasicBlock> parents;
	BasicBlock next;
	BasicBlock prev;
	
	//Dominator tree
	BasicBlock dominator;
	List<BasicBlock> dominated;
	
	List<Instruction> instructions;
	Map<String,Argument> varLookupTable;
	int bbID;
	
	private static int nextBBID = 1;
	private static Map<Integer,BasicBlock> idToBB = new HashMap<Integer,BasicBlock>();
	
	public static BasicBlock getBasicBlockByID(int id) {
		return idToBB.get(id);
	}
	public static int nextInstructionID() {
		return nextBBID;
	}
	
	public BasicBlock() {
		children = new ArrayList<BasicBlock>();
		parents = new ArrayList<BasicBlock>();
		dominated = new ArrayList<BasicBlock>();
		instructions = new LinkedList<Instruction>();
		varLookupTable = new HashMap<String,Argument>();
		bbID = nextBBID;
		idToBB.put(bbID, this);
		nextBBID++;
	}

	public void addChild(BasicBlock c) {
		children.add(c);
	}

	public void addParent(BasicBlock p) {
		parents.add(p);
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
	public void setNext(BasicBlock n) {
		next = n;
	}
	public void setPrevious(BasicBlock p) {
		next = p;
	}
}
