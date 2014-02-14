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
	BasicBlock next;
	BasicBlock prev;
	
	//Dominator tree
	BasicBlock dominator;
	List<BasicBlock> dominated;
	
	List<Instruction> instructions;
	
	Map<String,Argument> varLookupTable;
	Map<String,InstructionID> varDefTable;
	Set<String> changedVariables;
	
	BasicBlockID bbID;
	
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
	
	public void setNext(BasicBlock n) {
		next = n;
	}
	
	public void setPrevious(BasicBlock p) {
		next = p;
	}
	
	public void updateVariable(String var, Argument arg, InstructionID id) {
		varLookupTable.put(var, arg);
		varDefTable.put(var, id);
		changedVariables.add(var);
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
