package cs241;

import java.io.File;
import java.io.Reader;
import java.io.FileReader;
import java.io.FileNotFoundException;

import cs241.parser.Parser;
import cs241.parser.ParserException;

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
		try {
			parser.parse(reader);
		} catch (ParserException e) {
			e.printStackTrace();
		}
	}
}
