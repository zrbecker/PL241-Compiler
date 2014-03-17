import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import cs241.Compiler;

public class TestMain {
	public static void main(String[] args) throws IOException {
		File inFile = new File("./programs/factorial.txt");
		File outFile = null;
		Compiler comp = new Compiler(inFile,outFile);
		byte[] bytes;
		try {
			bytes = comp.compile();
		} catch (FileNotFoundException e1) {
			bytes = new byte[0];
			e1.printStackTrace();
		}

		//Files.write(FileSystems.getDefault().getPath("program.out"), bytes, StandardOpenOption.CREATE);
		System.out.println("finished");
	}
}
