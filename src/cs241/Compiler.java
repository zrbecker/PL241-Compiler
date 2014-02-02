import java.io.File;
import java.io.Reader;
import java.io.FileReader;
import java.io.FileNotFoundException;

import parser.Parser;

public class Compiler {
	File inputFile;
	File outputFile;
	BasicBlock root;
	Parser parser;

	public Compiler(File in, File out) {
		root = new BasicBlock();
		inputFile = in;
		outputFile = out;
		parser = new Parser();
	}

	public void compile() throws FileNotFoundException {
		Reader reader = new FileReader(inputFile);
		parser.parse(reader);
	}
}
