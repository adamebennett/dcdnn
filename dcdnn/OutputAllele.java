package dcdnn;

import java.util.ArrayList;

public class OutputAllele extends Allele {

	public int inputIndex;
	public int outputIndex;

	public OutputAllele(Genotype g, int i) {
		genotype = g;
		index = i;
	}

	public int getNumInputs() {
		return 1;
	}

	public int getNumOutputs() {
		return 0;
	}

	public void init() {
		for (int i = 0; i < getNumInputs(); ++i) {
			int numAlleles = genotype.getNumAlleles();
			int numOutputs = 0;
			while (numOutputs == 0) {
				inputIndex = (int)(Math.random() * (numAlleles - genotype.numInputs)) + genotype.numInputs;
				numOutputs = genotype.alleles.get(inputIndex).getNumOutputs();
			}
			outputIndex = (int)(Math.random() * numOutputs);
		}
	}

	public void update() {
		super.update();
		if (inputIndex >= genotype.getNumAlleles()) {				
			int numAlleles = genotype.getNumAlleles();
			int numOutputs = 0;
			while (numOutputs == 0) {
				inputIndex = (int)(Math.random() * (numAlleles - genotype.numInputs)) + genotype.numInputs;
				numOutputs = genotype.alleles.get(inputIndex).getNumOutputs();
			}
			outputIndex = (int)(Math.random() * numOutputs);
		}
		else if (genotype.alleles.get(inputIndex).getNumOutputs() == 0) {
			int numAlleles = genotype.getNumAlleles();
			int numOutputs = 0;
			while (numOutputs == 0) {
				inputIndex = (int)(Math.random() * (numAlleles - genotype.numInputs)) + genotype.numInputs;
				numOutputs = genotype.alleles.get(inputIndex).getNumOutputs();
			}
			outputIndex = (int)(Math.random() * numOutputs);
		}
		else {
			int numOutputs = 0;
			numOutputs = genotype.alleles.get(inputIndex).getNumOutputs();
			while (outputIndex >= numOutputs) {
				outputIndex = (int)(Math.random() * numOutputs);
			}
		}
	}

	public boolean canChange() {
		return true;
	}

	public boolean mustExist() {
		return true;
	}

	public Allele getInput(int i) {
		return genotype.alleles.get(inputIndex);
	}

	public int getOutputIndexOf(int i) {
		return outputIndex;
	}

	public String toString() {
		String str = "" + index + ": Output, " + inputIndex + "[" + outputIndex + "]";
		return str;
	}

	public Allele clone() {
		OutputAllele copy = new OutputAllele(genotype, index);
		copy.inputIndex = inputIndex;
		copy.outputIndex = outputIndex;
		return copy;
	}

	public Component getComponent(Network network) {
		ArrayList<Node> nodes = new ArrayList<Node>();
		nodes.add(new Neuron(network, 1));
		Component toReturn = new Component("Output " + (index - genotype.numInputs), network, nodes, nodes, nodes);
		return toReturn;
	}
}