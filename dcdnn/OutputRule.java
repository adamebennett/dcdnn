package dcdnn;

import java.util.ArrayList;

public class OutputRule {
	
	public int[] inputValue;
	public int[] stateValue;
	public int[] outputValue;

	public OutputRule(int[] input, int[] state, int[] output) {
		inputValue = input;
		stateValue = state;
		outputValue = output;
	}

	public boolean applies(int[] currentInput, int[] currentState) {
		if (currentInput.length != inputValue.length) {
			System.out.println("Error: number of provided input bits does not match rule");
		}
		if (currentState.length != stateValue.length) {
			System.out.println("Error: number of provided state bits does not match rule");
		}

		// check if any of the current inputs don't match the rule
		for (int i = 0; i < currentInput.length; ++i) {
			if (inputValue[i] != -1 && inputValue[i] != currentInput[i]) {
				return false; // the rule doesn't apply
			}
		}

		// check if the state doesn't match
		for (int i = 0; i < currentState.length; ++i) {
			if (stateValue[i] != -1 && stateValue[i] != currentState[i]) {
				return false; // the rule doesn't apply
			}
		}

		// the rule applies
		return true;
	}

	public OutputRule clone() {
		return new OutputRule(FiniteStateMachine.copyBitString(inputValue), FiniteStateMachine.copyBitString(stateValue), FiniteStateMachine.copyBitString(outputValue));
	}
}