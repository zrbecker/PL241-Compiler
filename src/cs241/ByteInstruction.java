package cs241;

import cs241.Argument.BasicBlockID;

public class ByteInstruction {
	public static final int F1 = -1;
	public static final int F2 = -2;
	public static final int F3 = -3;
	public static final int NONE = -4;
	
	public static int[] instructionOpCodeToFormat = {F2, F2, F2, F2, F2, F2, NONE, NONE, F2, F2, F2, F2, F2, F2, F2, //14
									NONE, F1, F1, F1, F1, F1, F1, NONE, NONE, F1, F1, F1, F1, F1, F1, F1, //30
									NONE, F1, F2, F1, NONE, F1, F2, F1, //38
									NONE, F1, F1, F1, F1, F1, F1, F1, NONE, F3, F2, //49
									F2, F2, F2, F1};

	
	
	int opCode;
	int format;
	int a;
	int b;
	int c;
	public ByteInstruction(int opc, int a, int b, int c) {
		opCode = opc;
		format = instructionOpCodeToFormat[opc];
		this.a = a;
		this.b = b;
		this.c = c;
		if(format == NONE)
			System.out.println("Error: Instruction has bad format");
		if(a >= 32 || b >= 32)
			System.out.println("Error: args a and b should be less than 32.");
		if(format == F1 && c >= 65536)
			System.out.println("Error: arg c should be less than 65536.");
		if(format == F2 && c >= 32)
			System.out.println("Error: arg c should be less than 32.");
		if(format == F3 && c >= 67108864)
			System.out.println("Error: arg c should be less than 67108864.");
	}
	
	public int toInt() {
		int opbyte = ((opCode & 0x3F) << 26);
		int abyte = ((a & 0x1F) << 21);
		int bbyte = ((b & 0x1F) << 16);
		int cbyte;
		if(format == F1) {
			cbyte =  (c & 0xFFFF);
		} else if(format == F2) {
			cbyte = (c & 0x1F);
		} else { //format == F3
			cbyte =  (c & 0x3FFFFFF);
		}
		return (opbyte | abyte | bbyte | cbyte);
	}
	
	/*
	 * Use RealJumpInstruction during conversion, then once basicblock locations are know scan back through and fix things up
	 */
	public static class ByteJumpInstruction extends ByteInstruction {
		private BasicBlockID branchTargetID;
		public ByteJumpInstruction(int opc, int a, int b, int c, BasicBlockID bbID) {
			super(opc, a, b, c);
			branchTargetID = bbID;
		}
		public BasicBlockID getTargetBBID() {
			return branchTargetID;
		}
	}
}
