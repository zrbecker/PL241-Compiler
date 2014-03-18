package cs241;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cs241.Argument.BasicBlockID;
import cs241.Argument.CopiedVariable;
import cs241.Argument.InstructionID;
import cs241.Argument.VariableArg;
import cs241.Instruction.InstructionType;

/**`
 * Class to keep track of what code is in what basic block
 */
public class BasicBlock {
	//Code structure
	List<BasicBlock> children;
	List<BasicBlock> parents;
	private BasicBlock next;
	private BasicBlock prev;
	private Instruction branch;
	
	//Dominator tree
	BasicBlock dominator;
	List<BasicBlock> dominated;
	
	List<Instruction> instructions;
	
	private boolean returnBlock;
	private boolean whileConditionBlock;
	private boolean lastWhileLoopBlock;
	private boolean lastThenBlock;
	
	private Map<String,VariableArg> varLookupTable;
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
		returnBlock = false;
		whileConditionBlock = false;
		varLookupTable = new HashMap<String,VariableArg>();
		changedVariables = new HashSet<String>();
		bbID = new BasicBlockID(nextBBID);
		idToBB.put(bbID, this);
		nextBBID++;
	}
	
	public List<BasicBlock> getChildren() {
		return children;
	}
	
	public List<Instruction> getInstructions() {
		return instructions;
	}

	public void addChild(BasicBlock c) {
		children.add(c);
	}

	public void addParent(BasicBlock p) {
		parents.add(p);
	}
	
	public Instruction getBranchInstruction() {
		return branch;
	}
	
	public void setBranchInstruction(Instruction b) {
		branch = b;
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
		if(i.type == InstructionType.RETURN)
			returnBlock = true;
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
	
	public boolean isReturnBlock() {
		return returnBlock;
	}
	
	public void setIsReturnBlock() {
		returnBlock = true;
	}

	public boolean isWhileConditionBlock() {
		return whileConditionBlock;
	}

	public void setWhileConditionBlock() {
		whileConditionBlock = true;
	}
	
	public boolean isLastWhileLoopBlock() {
		return lastWhileLoopBlock;
	}
	
	public void setLastWhileLoopBlock() {
		lastWhileLoopBlock = true;
	}
	
	public boolean isLastThenBlock() {
		return lastThenBlock;
	}
	
	public void setLastThenBlock() {
		lastThenBlock = true;
	}
	
	public void updateVariable(String var, Argument arg, InstructionID defInstructionID) {
		VariableArg v;
		if(arg instanceof CopiedVariable) {
			v = new CopiedVariable(var,(CopiedVariable)arg,defInstructionID);
		} else if(arg instanceof VariableArg) {
			v = new CopiedVariable(var,(VariableArg)arg,defInstructionID);
		} else {
			v = new VariableArg(var,arg,defInstructionID);
		}
		varLookupTable.put(var, v);
		changedVariables.add(var);
	}
	
	public VariableArg getVariable(String var) {
		return varLookupTable.get(var);
	}
	
	public Argument getValueForVariable(String var) {
		return varLookupTable.get(var).getValue();
	}
	
	public InstructionID getDefinitionForVariable(String var) {
		return varLookupTable.get(var).getDef();
	}
	
	public Set<String> getChangedVariables() {
		return changedVariables;
	}
	
	public void copyVariableTableFrom(BasicBlock bb) {
		varLookupTable.putAll(bb.varLookupTable);
	}
	
	public void copyAllTablesFrom(BasicBlock bb) {
		varLookupTable.putAll(bb.varLookupTable);
		changedVariables.addAll(bb.changedVariables);
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append(bbID + ": \n");
		for(Instruction i : instructions) {
			sb.append(i.toString());
		}
		
		if(branch != null)
			sb.append(branch.toString());
		
		if(next != null)
			sb.append(next.toString());
		
		return sb.toString();
	}
	
	/*
	 * Forget information about variables
	 * All arguments should be instructions, values, etc. not variables or copies
	 */
	public void simplify() {
		List<Instruction> newIns = new LinkedList<Instruction>();
		for(Instruction i : instructions) {
			for(int j = 0; j < i.args.length; j++) {
				if(i.args[j] instanceof VariableArg) {
					i.args[j] = ((VariableArg)i.args[j]).getValue();
				}
			}
			newIns.add(i);
		}
		instructions = newIns;
		if(next != null)
			next.simplify();
	}
}
