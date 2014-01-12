import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import cs241.parser.Parser;

public class TestMain {
	public static void main(String[] args) {
		Parser parser = new Parser();
		File directory = new File("./programs/");
		File[] files = directory.listFiles();
		
		if (files == null) {
			System.err.println("Invalid Directory");
		} else {
			for (File file : files) {
				BufferedReader stream;
				try {
					stream = new BufferedReader(new FileReader(file));
				} catch (FileNotFoundException e) {
					System.err.println(file.getName() + " - Not Found");
					continue;
				}
				
				System.out.print(file.getName() + " - ");
				try {
					parser.parse(stream);
					System.out.println("SUCCESS");
				} catch (RuntimeException e) {
					System.err.println(e.getMessage());
					System.err.flush();
				}
				
				try {
					stream.close();
				} catch (IOException e) { }
			}
		}
		
	}
}
