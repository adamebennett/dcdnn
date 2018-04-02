package dcdnn;

import java.util.ArrayList;

public class InputAllele extends Allele {

	public InputAllele(Genotype g, int i) {
		genotype = g;
		index = i;
	}

	public int getNumInputs() {
		return 0;
	}

	public int getNumOutputs() {
		return 1;
	}

	public void init() {
	}

	public boolean canChange() {
		return false;
	}

	public boolean mustExist() {
		return true;
	}

	public Allele getInput(int i) {
		return null;
	}

	public int getOutputIndexOf(int i) {
		return -1;
	}

	public String toString() {
		String str = "" + index + ": Input";
		return str;
	}

	public Allele clone() {
		return new InputAllele(genotype, index);
	}

	public Component getComponent(Network network) {
		ArrayList<Node> nodes = new ArrayList<Node>();
		nodes.add(new InputNode(network, false, new float[]{0f}));
		Component toReturn = new Component("Input " + index, network, nodes, nodes, nodes);
		return toReturn;
	}
}