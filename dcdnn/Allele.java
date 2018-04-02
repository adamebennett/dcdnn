package dcdnn;

import java.util.ArrayList;

public abstract class Allele {

	public Genotype genotype;
	public int index;
	public int[][] testValues;
	public boolean pruned;

	public abstract int getNumInputs();

	public abstract int getNumOutputs();

	public abstract void init();

	public void update() {
		index = genotype.alleles.indexOf(this);
	}

	public abstract boolean canChange();

	public abstract boolean mustExist();

	public void cleanUp() {
		testValues = null;
	}

	public void allocateTestValues(int numTimeSteps) {
		testValues = new int[numTimeSteps][];
		for (int i = 0; i < numTimeSteps; ++i) {
			testValues[i] = new int[getNumOutputs()];
			for (int j = 0; j < getNumOutputs(); ++j) {
				testValues[i][j] = -1;
			}
		}
	}

	// get ith input Allele
	public abstract Allele getInput(int i);

	public abstract int getOutputIndexOf(int i);

	public int getDelayPeriods() {
		return -1;
	}

	public int getClockified() {
		return -1;
	}

	public abstract String toString();

	public int[] transition(int[] inputs) {
		return new int[getNumOutputs()];
	}

	public static Allele randomAllele(Genotype g, int i) {
		int rand = (int)(Math.random() * 1);
		FiniteStateMachine fsm = null;
		if (rand == 0) {
			fsm = FiniteStateMachine.nandMachine();
		}
		return new MachineAllele(g, i, fsm); // still need to init to random indices
	}

	public AlleleConnection[] getInputConnections(ArrayList<AlleleConnection> connections) {
		AlleleConnection[] inputConns = new AlleleConnection[getNumInputs()];
		for (int i = 0; i < inputConns.length; ++i) {
			Allele from = getInput(i);
			for (AlleleConnection connection: connections) {
				if (connection.from == from && connection.to == this) {
					inputConns[i] = connection;
					break;
				}
			}
		}
		return inputConns;
	}

	public abstract Allele clone();

	public abstract Component getComponent(Network network);
}