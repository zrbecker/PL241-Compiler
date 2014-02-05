import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import cs241.parser.Parser;
import cs241.parser.ParserException;
import cs241.parser.PrettyPrint;
import cs241.parser.treenodes.Computation;

public class TestMain {
	public static void main(String[] args) {
		Parser parser = new Parser();
		try {
			BufferedReader stream = new BufferedReader(new FileReader(new File("./programs/test001.txt")));
			Computation c = parser.parse(stream);
			stream.close();
			
			PrettyPrint printer = new PrettyPrint();
			String s = printer.Print(c);
			BufferedWriter output = new BufferedWriter(new FileWriter(new File("./output.txt")));
			output.write(s);
			output.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParserException e) {
			e.printStackTrace();
		}
		
		System.out.println("finished");
		
//		File directory = new File("./programs/");
//		File[] files = directory.listFiles();
//		
//		if (files == null) {
//			System.err.println("Invalid Directory");
//		} else {
//			for (File file : files) {
//				BufferedReader stream;
//				try {
//					stream = new BufferedReader(new FileReader(file));
//				} catch (FileNotFoundException e) {
//					System.err.println(file.getName() + " - Not Found");
//					continue;
//				}
//				
//				System.out.print(file.getName() + " - ");
//				try {
//					parser.parse(stream);
//					System.out.println("Success");
//				} catch (ParserException e) {
//					System.err.println("Syntax Error.");
//					System.err.flush();
//				}
//				
//				try {
//					stream.close();
//				} catch (IOException e) { }
//			}
//		}
//		
	}
}
