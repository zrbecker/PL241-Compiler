package cs241;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cs241.Argument.BasicBlockID;
import cs241.Argument.InstructionID;

/**`
 * Class to keep track of what code is in what basic block
 */
public class BasicBlock {
	//Code structure
	List<BasicBlock> children;
	List<BasicBlock> parents;
	private BasicBlock next;
	private BasicBlock prev;
	
	//Dominator tree
	BasicBlock dominator;
	List<BasicBlock> dominated;
	
	List<Instruction> instructions;
	
	private Map<String,Argument> varLookupTable;
	private Map<String,InstructionID> varDefTable;
	private Set<String> changedVariables;
	
	private BasicBlockID bbID;
	
	private static int nextBBID = 1;
	private static Map<BasicBlockID,BasicBlock> idToBB = new HashMap<BasicBlockID,BasicBlock>();
	
	public static BasicBlock getBasicBlockByID(BasicBlockID id) {
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
		varDefTable = new HashMap<String,InstructionID>();
		changedVariables = new HashSet<String>();
		bbID = new BasicBlockID(nextBBID);
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
	
	public BasicBlock getNext() {
		return next;
	}
	
	public void setNext(BasicBlock n) {
		next = n;
	}
	
	public BasicBlock getPrev() {
		return prev;
	}
	
	public void setPrevious(BasicBlock p) {
		prev = p;
	}
	
	public BasicBlockID getID() {
		return bbID;
	}
	
	public void updateVariable(String var, Argument arg, InstructionID defInstructionID) {
		arg = arg.clone();
		arg.setVariable(var);
		varLookupTable.put(var, arg);
		varDefTable.put(var, defInstructionID);
		changedVariables.add(var);
	}
	
	public Argument getVarArg(String var) {
		return varLookupTable.get(var);
	}
	
	public InstructionID getVarDef(String var) {
		return varDefTable.get(var);
	}
	
	public Set<String> getChangedVars() {
		return changedVariables;
	}
	
	public void copyVarTablesFrom(BasicBlock bb) {
		varLookupTable.putAll(bb.varLookupTable);
		varDefTable.putAll(bb.varDefTable);
	}
	
	public void copyAllTablesFrom(BasicBlock bb) {
		varLookupTable.putAll(bb.varLookupTable);
		changedVariables.addAll(bb.changedVariables);
		varDefTable.putAll(bb.varDefTable);
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append(bbID + ": \n");
		for(Instruction i : instructions) {
			sb.append(i.toString());
		}
		if(next != null)
			sb.append(next.toString());
		
		return sb.toString();
	}
}
