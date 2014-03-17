package cs241;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cs241.Argument.InstructionID;
import cs241.Instruction.InstructionType;

public class RegisterAllocator {
	private class Graph {
		private Map<Instruction, List<Instruction>> edgeList;
		
		public Graph() {
			edgeList = new HashMap<Instruction, List<Instruction>>();
		}
		
		public void addNode(Instruction a) {
			if (!edgeList.containsKey(a))
				edgeList.put(a, new ArrayList<Instruction>());
		}
		
		public void addEdge(Instruction a, Instruction b) {
			if (!edgeList.containsKey(a))
				edgeList.put(a, new ArrayList<Instruction>());
			if (!edgeList.containsKey(b))
				edgeList.put(b, new ArrayList<Instruction>());
			if (!isEdge(a, b)) {
				edgeList.get(a).add(b);
				edgeList.get(b).add(a);
			}
		}
		
		public List<Instruction> getNeighbors(Instruction a) {
			if (!edgeList.containsKey(a))
				return new ArrayList<Instruction>();
			else
				return edgeList.get(a);
		}
		
		public int getDegree(Instruction a) {
			return edgeList.get(a).size();
		}
		
		public boolean isEdge(Instruction a, Instruction b ) {
			return edgeList.containsKey(a) && edgeList.get(a).contains(b);
		}
		
		public Set<Instruction> getNodes() {
			return edgeList.keySet();
		}
	}
	
	private class BasicBlockInfo {
		public int visited;
		public Set<Instruction> live;
		
		public BasicBlockInfo() {
			visited = 0;
			live = new HashSet<Instruction>();
		}
	}
	
	private Graph interferenceGraph;
	private Map<BasicBlock, BasicBlockInfo> bbInfo;
	private Set<BasicBlock> loopHeaders;
	private Map<Instruction, Integer> colors;
	
	public RegisterAllocator() {
	}
	
	public Map<InstructionID, Integer> allocate(BasicBlock b) {
		// Reset allocator
		interferenceGraph = new Graph();
		bbInfo = new HashMap<BasicBlock, BasicBlockInfo>();
		loopHeaders = new HashSet<BasicBlock>();
		colors = new HashMap<Instruction, Integer>();
		
		calcLiveRange(b, 0, 1);
		calcLiveRange(b, 0, 2);
		colorGraph();
		saveVCGGraph("interference.vcg"); // For debugging
		
		Map<InstructionID,Integer> coloredIDs = new HashMap<InstructionID,Integer>();
		for(Instruction i : colors.keySet()) {
			coloredIDs.put(i.getID(),colors.get(i));
		}
		
		return coloredIDs;
	}
	
	private void colorGraph() {
		Instruction maxDegree = null;
		do {
			maxDegree = null;
			for (Instruction a : interferenceGraph.getNodes()) {
				if (!colors.containsKey(a) && (maxDegree == null || interferenceGraph.getDegree(a) > interferenceGraph.getDegree(maxDegree)))
					maxDegree = a;
			}
			if(maxDegree == null)
				break;
			
			Set<Integer> taken = new HashSet<Integer>();
			for (Instruction b : interferenceGraph.getNeighbors(maxDegree)) {
				if (colors.containsKey(b))
					taken.add(colors.get(b));
			}
			
			int reg = 1;
			while (taken.contains(reg))
				reg += 1;
			
			colors.put(maxDegree, reg);
		} while (maxDegree != null);
	}
	
	private void saveVCGGraph(String filename) {
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(new File(filename)));
			
			out.write("graph {\n");
			out.write("title: \"Interference Graph\"\n");
			
			Set<Instruction> done = new HashSet<Instruction>();
			for (Instruction a : interferenceGraph.getNodes()) {
				done.add(a);
				
				out.write("node {\n");
				out.write("title: \"" + a.toString() + "\"\n");
				out.write("label: \"" + "REG: " + colors.get(a) + " ::: " + a.toString() + "\"\n");
				out.write("}\n");
				
				for (Instruction b : interferenceGraph.getNodes()) {
					if (interferenceGraph.isEdge(a, b) && !done.contains(b)) {
						out.write("edge {\n");
						out.write("sourcename: \"" + a.toString() + "\"\n");
						out.write("targetname: \"" + b.toString() + "\"\n");
						out.write("}\n");
					}
				}
			}
			
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private Set<Instruction> calcLiveRange(BasicBlock b, int branch, int pass) {
		Set<Instruction> live = new HashSet<Instruction>();
		
		if (!bbInfo.containsKey(b))
			bbInfo.put(b, new BasicBlockInfo());
		
		if (b != null) {
			if (bbInfo.get(b).visited >= pass) {
				live.addAll(bbInfo.get(b).live);
			} else {
				bbInfo.get(b).visited += 1;
				if (bbInfo.get(b).visited == 2) {
					for (BasicBlock h : loopHeaders)
						bbInfo.get(b).live.addAll(bbInfo.get(h).live);
				}
				
				int index = 0;
				for (BasicBlock child : b.getChildren()) {
					if (b.isWhileConditionBlock() && index == 0)
						loopHeaders.add(b);
					
					live.addAll(calcLiveRange(child, index, pass));
					index += 1;
					
					if (b.isWhileConditionBlock() && index == 0)
						loopHeaders.remove(b);
				}
				
				// Handle non PHI instructions
				List<Instruction> reverse = new ArrayList<Instruction>();
				reverse.addAll(b.getInstructions());
				Collections.reverse(reverse);
				for (Instruction ins : reverse) {
					if (ins.type != InstructionType.PHI) {
						live.remove(ins);
						interferenceGraph.addNode(ins);
						for (Instruction other : live)
							interferenceGraph.addEdge(ins, other);
						for (Argument arg : ins.args) {
							if (arg instanceof InstructionID)
								live.add(Instruction.getInstructionByID((InstructionID) arg));
						}
					}
				}
				
				bbInfo.get(b).live = new HashSet<Instruction>();
				bbInfo.get(b).live.addAll(live);
			}
			
			// Handle PHI instructions
			List<Instruction> reverse = new ArrayList<Instruction>();
			reverse.addAll(b.getInstructions());
			Collections.reverse(reverse);
			for (Instruction ins : reverse) {
				if (ins.type == InstructionType.PHI) {
					live.remove(ins);
					interferenceGraph.addNode(ins);
					for (Instruction other : live)
						interferenceGraph.addEdge(ins, other);
					
					for (Argument arg : ins.args) {
						if (arg instanceof InstructionID)
							live.add(Instruction.getInstructionByID((InstructionID) arg));
					}
					
//					Argument arg = ins.args[branch];
//					if (arg instanceof InstructionID)
//						live.add(Instruction.getInstructionByID((InstructionID) arg));
				}
			}
		}
		
		return live;
	}

//	public void buildInterferenceGraph(BasicBlock block) {
//		// Goto end
//		BasicBlock curBlock = block;
//		while (curBlock.getNext() != null)
//			curBlock = curBlock.getNext();
//		
//		Set<Instruction> liveInstructions = new HashSet<Instruction>();
//		while (curBlock.getPrev() != null) {
//			List<Instruction> reverseInstructions = new ArrayList<Instruction>();
//			reverseInstructions.addAll(curBlock.getInstructions());
//			Collections.reverse(reverseInstructions);
//			
//			for (Instruction i : reverseInstructions) {
//				for (Argument arg : i.args) {
//					if (arg instanceof InstructionID) {
//						Instruction a = Instruction.getInstructionByID((InstructionID) arg);
//						for (Instruction b : liveInstructions) {
//							interferenceGraph.addEdge(a, b);
//						}
//						liveInstructions.add(a);
//					}
//				}
//				
//				if (liveInstructions.contains(i))
//					liveInstructions.remove(i);
//			}
//		}
//	}
	
}





























































