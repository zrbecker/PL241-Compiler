package cs241.vcg;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import cs241.BasicBlock;
import cs241.Instruction;

public class ControlFlowGraphVCG {

	public ControlFlowGraphVCG() {
		
	}
	
	public void writeNodes(BufferedWriter out, String unitName, BasicBlock current) throws IOException {
		while (current != null) {
			String name = unitName + " " + current.getID().getID();

			StringBuilder sb = new StringBuilder();
			sb.append(name + ": \n");
			if (current.getInstructions() != null) {
				for(Instruction i : current.getInstructions()) {
					sb.append(i.toString());
				}
				if (current.getBranchInstruction() != null)
					sb.append(current.getBranchInstruction().toString());
			}
			String label = sb.toString();
			
			out.write("node {\n");
			out.write("title: \"" + name + "\"\n");
			out.write("label: \"" + label + "\"\n");
			out.write("}\n");
			
			if (current.getChildren() != null) {
				for (BasicBlock child : current.getChildren()) {
					String destName = unitName + " " + child.getID().getID();
					out.write("edge {\n");
					out.write("sourcename: \"" + name + "\"\n");
					out.write("targetname: \"" + destName + "\"\n");
					out.write("}\n");
				}
			}
			
			current = current.getNext();
		}
	}
	
	public void exportAsVCG(String filename, BasicBlock main, Map<String, BasicBlock> functionBBs) {
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(new File(filename)));
			
			out.write("graph {\n");
			out.write("title: \"Control Flow Graph\"\n");
			out.write("manhattan_edges: yes\n");
			out.write("smanhattan_edges: yes\n");
			
			writeNodes(out, "main", main);
			for (Entry<String, BasicBlock> keyvalue : functionBBs.entrySet()) {
				String functionName = keyvalue.getKey();
				BasicBlock basicblock = keyvalue.getValue();
				writeNodes(out, functionName, basicblock);
			}
			
			out.write("}\n");
			
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
