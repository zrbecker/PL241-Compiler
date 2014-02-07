package cs241;


public interface Argument {
	public static class Value implements Argument {
		private int val;
		public Value(int v) {
			val = v;
		}
		public int getValue() {
			return val;
		}
		public int hashCode() {
			return 37*val;
		}
		public String toString() {
			return "#" + val;
		}
	}
	
	public static class BasicBlockID implements Argument {
		private int bbID;
		public BasicBlockID(int id) {
			bbID = id;
		}
		public int getID() {
			return bbID;
		}
		public int hashCode() {
			return 31*bbID;
		}
		public String toString() {
			return "BB." + bbID;
		}
	}
	
	public static class InstructionID implements Argument {
		private int instructionID;
		public InstructionID(int id) {
			instructionID = id;
		}
		public int getID() {
			return instructionID;
		}
		public int hashCode() {
			return 43*instructionID;
		}
		public String toString() {
			return "Instruction." + instructionID;
		}
	}
	
	public static class ArrayName implements Argument {
		private String name;
		public ArrayName(String n) {
			name = n;
		}
		public String getName() {
			return name;
		}
		public int hashCode() {
			return name.hashCode();
		}
		public String toString() {
			return "Arr." + name;
		}
	}
	public static class FunctionName implements Argument {
		private String name;
		public FunctionName(String n) {
			name = n;
		}
		public String getName() {
			return name;
		}
		public int hashCode() {
			return name.hashCode();
		}
		public String toString() {
			return "Fun." + name;
		}
	}
}
