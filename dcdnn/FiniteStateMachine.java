package dcdnn;

import java.util.ArrayList;

public class FiniteStateMachine {
	
	public String name;
	public int[] initialState;
	public int[] state;
	public int numInputs;
	public int numOutputs;
	public ArrayList<TransitionRule> transitionRules;
	public ArrayList<OutputRule> outputRules;
	public String componentPath;
	public int delayPeriods;
	public int clockified;

	public FiniteStateMachine(String n, int[] i, int ni, int no, ArrayList<OutputRule> outRules, ArrayList<TransitionRule> tRules, int dp, int cf, String cp) {
		name = n;
		initialState = i;
		state = copyBitString(initialState);
		numInputs = ni;
		numOutputs = no;
		outputRules = outRules;
		transitionRules = tRules;
		delayPeriods = dp;
		clockified = cf;
		componentPath = cp;
	}

	public int[] transition(int[] inputs) {
		int[] toReturn = null;

		// find and apply first applicable output rule
		for (OutputRule r: outputRules) {
			if (r.applies(inputs, state)) {
				toReturn = copyBitString(r.outputValue);
				break;
			}
		}

		// find and apply first applicable transition rule
		for (TransitionRule r: transitionRules) {
			if (r.applies(inputs, state)) {
				state = copyBitString(r.transitionValue);
				break;
			}
		}

		return toReturn;
	}

	public int transition(int[] inputs, int outBit) {
		int[] output = transition(inputs);
		return output[outBit];
	}

	public FiniteStateMachine clone() {
		FiniteStateMachine copy = new FiniteStateMachine(name, copyBitString(initialState), numInputs, numOutputs, new ArrayList<OutputRule>(), new ArrayList<TransitionRule>(), delayPeriods, clockified, componentPath);
		for (OutputRule rule: outputRules) {
			copy.outputRules.add(rule.clone());
		}
		for (TransitionRule rule: transitionRules) {
			copy.transitionRules.add(rule.clone());
		}
		return copy;
	}

	public static int[] copyBitString(int[] toCopy) {
		int[] toReturn = new int[toCopy.length];
		for (int i = 0; i < toCopy.length; ++i) {
			toReturn[i] = toCopy[i];
		}
		return toReturn;
	}

	// some basic predefined FSMs for testing:
	public static FiniteStateMachine notMachine() {
		ArrayList<OutputRule> oRules = new ArrayList<OutputRule>();
		ArrayList<TransitionRule> tRules = new ArrayList<TransitionRule>();
		oRules.add(new OutputRule(new int[]{0}, new int[]{-1}, new int[]{1}));
		oRules.add(new OutputRule(new int[]{1}, new int[]{-1}, new int[]{0}));
		return new FiniteStateMachine("NOT", new int[]{0}, 1, 1, oRules, tRules, -1, -1, "/networks/not.ng");
	}

	public static FiniteStateMachine andMachine() {
		ArrayList<OutputRule> oRules = new ArrayList<OutputRule>();
		ArrayList<TransitionRule> tRules = new ArrayList<TransitionRule>();
		oRules.add(new OutputRule(new int[]{0,-1}, new int[]{}, new int[]{0}));
		oRules.add(new OutputRule(new int[]{-1,0}, new int[]{}, new int[]{0}));
		oRules.add(new OutputRule(new int[]{1,1}, new int[]{}, new int[]{1}));
		return new FiniteStateMachine("AND", new int[]{}, 2, 1, oRules, tRules, -1, -1, "/networks/and.ng");
	}

	public static FiniteStateMachine nandMachine() {
		ArrayList<OutputRule> oRules = new ArrayList<OutputRule>();
		ArrayList<TransitionRule> tRules = new ArrayList<TransitionRule>();
		oRules.add(new OutputRule(new int[]{0,0}, new int[]{}, new int[]{1}));
		oRules.add(new OutputRule(new int[]{1,0}, new int[]{}, new int[]{1}));
		oRules.add(new OutputRule(new int[]{0,1}, new int[]{}, new int[]{1}));
		oRules.add(new OutputRule(new int[]{1,1}, new int[]{}, new int[]{0}));
		return new FiniteStateMachine("NAND", new int[]{}, 2, 1, oRules, tRules, -1, -1, "/networks/nand.ng");
	}

	public static FiniteStateMachine orMachine() {
		ArrayList<OutputRule> oRules = new ArrayList<OutputRule>();
		ArrayList<TransitionRule> tRules = new ArrayList<TransitionRule>();
		oRules.add(new OutputRule(new int[]{1,-1}, new int[]{-1}, new int[]{1}));
		oRules.add(new OutputRule(new int[]{-1,1}, new int[]{-1}, new int[]{1}));
		oRules.add(new OutputRule(new int[]{0,0}, new int[]{-1}, new int[]{0}));
		return new FiniteStateMachine("OR", new int[]{0}, 2, 1, oRules, tRules, -1, -1, "/networks/or.ng");
	}

	public static FiniteStateMachine buffer1Machine() {
		ArrayList<OutputRule> oRules = new ArrayList<OutputRule>();
		ArrayList<TransitionRule> tRules = new ArrayList<TransitionRule>();
		oRules.add(new OutputRule(new int[]{0}, new int[]{}, new int[]{0}));
		oRules.add(new OutputRule(new int[]{1}, new int[]{}, new int[]{1}));
		return new FiniteStateMachine("INPUT BUFFER 1", new int[]{}, 1, 1, oRules, tRules, -1, -1, "/networks/input_buffer_1.ng");
	}

	public static FiniteStateMachine buffer2Machine() {
		ArrayList<OutputRule> oRules = new ArrayList<OutputRule>();
		ArrayList<TransitionRule> tRules = new ArrayList<TransitionRule>();
		oRules.add(new OutputRule(new int[]{0,0}, new int[]{}, new int[]{0,0}));
		oRules.add(new OutputRule(new int[]{0,1}, new int[]{}, new int[]{0,1}));
		oRules.add(new OutputRule(new int[]{1,0}, new int[]{}, new int[]{1,0}));
		oRules.add(new OutputRule(new int[]{1,1}, new int[]{}, new int[]{1,1}));
		return new FiniteStateMachine("INPUT BUFFER 2", new int[]{}, 2, 2, oRules, tRules, -1, -1, "/networks/input_buffer_2.ng");
	}

	public static FiniteStateMachine buffer3Machine() {
		ArrayList<OutputRule> oRules = new ArrayList<OutputRule>();
		ArrayList<TransitionRule> tRules = new ArrayList<TransitionRule>();
		oRules.add(new OutputRule(new int[]{0,0,0}, new int[]{}, new int[]{0,0,0}));
		oRules.add(new OutputRule(new int[]{0,0,1}, new int[]{}, new int[]{0,0,1}));
		oRules.add(new OutputRule(new int[]{0,1,0}, new int[]{}, new int[]{0,1,0}));
		oRules.add(new OutputRule(new int[]{0,1,1}, new int[]{}, new int[]{0,1,1}));
		oRules.add(new OutputRule(new int[]{1,0,0}, new int[]{}, new int[]{1,0,0}));
		oRules.add(new OutputRule(new int[]{1,0,1}, new int[]{}, new int[]{1,0,1}));
		oRules.add(new OutputRule(new int[]{1,1,0}, new int[]{}, new int[]{1,1,0}));
		oRules.add(new OutputRule(new int[]{1,1,1}, new int[]{}, new int[]{1,1,1}));
		return new FiniteStateMachine("INPUT BUFFER 3", new int[]{}, 3, 3, oRules, tRules, -1, -1, "/networks/input_buffer_3.ng");
	}

	public static FiniteStateMachine xorMachine() {
		ArrayList<OutputRule> oRules = new ArrayList<OutputRule>();
		ArrayList<TransitionRule> tRules = new ArrayList<TransitionRule>();
		oRules.add(new OutputRule(new int[]{0,0}, new int[]{-1}, new int[]{0}));
		oRules.add(new OutputRule(new int[]{0,1}, new int[]{-1}, new int[]{1}));
		oRules.add(new OutputRule(new int[]{1,0}, new int[]{-1}, new int[]{1}));
		oRules.add(new OutputRule(new int[]{1,1}, new int[]{-1}, new int[]{0}));
		return new FiniteStateMachine("XOR", new int[]{0}, 2, 1, oRules, tRules, -1, -1, null);
	}

	public static FiniteStateMachine flipFlopMachine() {
		ArrayList<OutputRule> oRules = new ArrayList<OutputRule>();
		ArrayList<TransitionRule> tRules = new ArrayList<TransitionRule>();
		oRules.add(new OutputRule(new int[]{-1,-1}, new int[]{0}, new int[]{0, 1}));
		oRules.add(new OutputRule(new int[]{-1,-1}, new int[]{1}, new int[]{1, 0}));
		tRules.add(new TransitionRule(new int[]{0,1}, new int[]{-1}, new int[]{0}));
		tRules.add(new TransitionRule(new int[]{1,0}, new int[]{-1}, new int[]{1}));
		tRules.add(new TransitionRule(new int[]{1,1}, new int[]{-1}, new int[]{0}));
		return new FiniteStateMachine("FLIPFLOP", new int[]{0}, 2, 2, oRules, tRules, -1, 2, "/networks/flipflop.ng");
	}

	public static FiniteStateMachine andOrMachine() {
		ArrayList<OutputRule> oRules = new ArrayList<OutputRule>();
		ArrayList<TransitionRule> tRules = new ArrayList<TransitionRule>();
		oRules.add(new OutputRule(new int[]{-1,-1,1}, new int[]{}, new int[]{0}));
		oRules.add(new OutputRule(new int[]{0,0,0}, new int[]{}, new int[]{0}));
		oRules.add(new OutputRule(new int[]{1,0,0}, new int[]{}, new int[]{1}));
		oRules.add(new OutputRule(new int[]{0,1,0}, new int[]{}, new int[]{1}));
		oRules.add(new OutputRule(new int[]{1,1,0}, new int[]{}, new int[]{1}));
		return new FiniteStateMachine("ANDOR", new int[]{}, 3, 1, oRules, tRules, -1, -1, "/networks/andor.ng");
	}

	public static FiniteStateMachine memoryMachine() {
		ArrayList<OutputRule> oRules = new ArrayList<OutputRule>();
		ArrayList<TransitionRule> tRules = new ArrayList<TransitionRule>();
		oRules.add(new OutputRule(new int[]{-1,-1,-1}, new int[]{0,0}, new int[]{0,0}));
		oRules.add(new OutputRule(new int[]{-1,-1,-1}, new int[]{0,1}, new int[]{0,1}));
		oRules.add(new OutputRule(new int[]{-1,-1,-1}, new int[]{1,0}, new int[]{1,0}));
		oRules.add(new OutputRule(new int[]{-1,-1,-1}, new int[]{1,1}, new int[]{1,1}));
		tRules.add(new TransitionRule(new int[]{0,-1,-1}, new int[]{0,0}, new int[]{0,0}));
		tRules.add(new TransitionRule(new int[]{0,-1,-1}, new int[]{0,1}, new int[]{0,1}));
		tRules.add(new TransitionRule(new int[]{0,-1,-1}, new int[]{1,0}, new int[]{1,0}));
		tRules.add(new TransitionRule(new int[]{0,-1,-1}, new int[]{1,1}, new int[]{1,1}));
		tRules.add(new TransitionRule(new int[]{1,0,0}, new int[]{-1,-1}, new int[]{0,0}));
		tRules.add(new TransitionRule(new int[]{1,0,1}, new int[]{-1,-1}, new int[]{0,1}));
		tRules.add(new TransitionRule(new int[]{1,1,0}, new int[]{-1,-1}, new int[]{1,0}));
		tRules.add(new TransitionRule(new int[]{1,1,1}, new int[]{-1,-1}, new int[]{1,1}));
		return new FiniteStateMachine("MEMORY", new int[]{0,0}, 3, 2, oRules, tRules, 1, -1, null);
	}

	public static FiniteStateMachine counterMachine() {
		ArrayList<OutputRule> oRules = new ArrayList<OutputRule>();
		ArrayList<TransitionRule> tRules = new ArrayList<TransitionRule>();
		oRules.add(new OutputRule(new int[]{-1}, new int[]{0,0}, new int[]{0,0}));
		oRules.add(new OutputRule(new int[]{-1}, new int[]{0,1}, new int[]{0,1}));
		oRules.add(new OutputRule(new int[]{-1}, new int[]{1,0}, new int[]{1,0}));
		oRules.add(new OutputRule(new int[]{-1}, new int[]{1,1}, new int[]{1,1}));
		tRules.add(new TransitionRule(new int[]{0}, new int[]{0,0}, new int[]{0,0}));
		tRules.add(new TransitionRule(new int[]{0}, new int[]{0,1}, new int[]{0,1}));
		tRules.add(new TransitionRule(new int[]{0}, new int[]{1,0}, new int[]{1,0}));
		tRules.add(new TransitionRule(new int[]{0}, new int[]{1,1}, new int[]{1,1}));
		tRules.add(new TransitionRule(new int[]{1}, new int[]{0,0}, new int[]{0,1}));
		tRules.add(new TransitionRule(new int[]{1}, new int[]{0,1}, new int[]{1,0}));
		tRules.add(new TransitionRule(new int[]{1}, new int[]{1,0}, new int[]{1,1}));
		tRules.add(new TransitionRule(new int[]{1}, new int[]{1,1}, new int[]{0,0}));
		return new FiniteStateMachine("COUNTER", new int[]{0,0}, 1, 2, oRules, tRules, 0, -1, null);
	}

	public static FiniteStateMachine autoCounterMachine() {
		ArrayList<OutputRule> oRules = new ArrayList<OutputRule>();
		ArrayList<TransitionRule> tRules = new ArrayList<TransitionRule>();
		oRules.add(new OutputRule(new int[]{-1}, new int[]{0,0,0}, new int[]{0,0}));
		oRules.add(new OutputRule(new int[]{-1}, new int[]{0,0,1}, new int[]{0,0}));
		oRules.add(new OutputRule(new int[]{-1}, new int[]{0,1,0}, new int[]{0,0}));
		oRules.add(new OutputRule(new int[]{-1}, new int[]{0,1,1}, new int[]{0,0}));
		oRules.add(new OutputRule(new int[]{-1}, new int[]{1,0,0}, new int[]{0,0}));
		oRules.add(new OutputRule(new int[]{-1}, new int[]{1,0,1}, new int[]{0,1}));
		oRules.add(new OutputRule(new int[]{-1}, new int[]{1,1,0}, new int[]{1,0}));
		oRules.add(new OutputRule(new int[]{-1}, new int[]{1,1,1}, new int[]{1,1}));
		tRules.add(new TransitionRule(new int[]{1}, new int[]{0,0,0}, new int[]{1,1,0}));
		tRules.add(new TransitionRule(new int[]{1}, new int[]{0,0,1}, new int[]{1,0,1}));
		tRules.add(new TransitionRule(new int[]{1}, new int[]{0,1,0}, new int[]{1,1,1}));
		tRules.add(new TransitionRule(new int[]{1}, new int[]{0,1,1}, new int[]{1,0,0}));
		tRules.add(new TransitionRule(new int[]{-1}, new int[]{1,0,0}, new int[]{1,1,0}));
		tRules.add(new TransitionRule(new int[]{-1}, new int[]{1,0,1}, new int[]{1,1,1}));
		tRules.add(new TransitionRule(new int[]{-1}, new int[]{1,1,0}, new int[]{1,0,1}));
		tRules.add(new TransitionRule(new int[]{-1}, new int[]{1,1,1}, new int[]{1,0,0}));
		return new FiniteStateMachine("COUNTER", new int[]{0,0,0}, 1, 2, oRules, tRules, 2, -1, null);
	}
}