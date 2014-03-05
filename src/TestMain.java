import java.io.File;
import java.io.FileNotFoundException;

import cs241.Compiler;

public class TestMain {
	public static void main(String[] args) {
		File inFile = new File("./programs/test_simple_copy_prop.txt");
		File outFile = null;
		Compiler comp = new Compiler(inFile,outFile);
		try {
			comp.compile();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		System.out.println("finished");
	}
}
