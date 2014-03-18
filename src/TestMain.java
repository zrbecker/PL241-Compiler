import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import cs241.Compiler;
import cs241.ByteInstruction;
import cs241.ByteInstructionListFactory;

public class TestMain {
	public static void main(String[] args) throws IOException {
		File inFile = new File("./programs/factorial.txt");
		File outFile = null;
		Compiler comp = new Compiler(inFile,outFile);
		int[] ops;
		try {
			ops = comp.compile();
		} catch (FileNotFoundException e1) {
			ops = new int[0];
			e1.printStackTrace();
		}
		System.out.println("finished with " + ops.length + " operations");
		boolean runProgram = true;
		
		if(ops.length != 0 && runProgram) {
			DLX.load(ops);
			DLX.execute();
		}
	}
}
