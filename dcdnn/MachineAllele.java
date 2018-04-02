package dcdnn;

import java.util.ArrayList;

public class MachineAllele extends Allele {

	public FiniteStateMachine machine;
	public int[] indices; // contains indices of alleles to use as input
	public int[] outputIndices; // contains indices to specify which bit of the output bitstring to use

	public MachineAllele(Genotype g, int i, FiniteStateMachine fsm) {
		genotype = g;
		index = i;
		machine = fsm;
		indices = new int[machine.numInputs];
		outputIndices = new int[machine.numInputs];
	}

	public int getNumInputs() {
		return machine.numInputs;
	}

	public int getNumOutputs() {
		return machine.numOutputs;
	}

	public void init() {
		for (int i = 0; i < getNumInputs(); ++i) {
			int numAlleles = genotype.getNumAlleles();
			int numOutputs = 0;
			while (numOutputs == 0) {
				indices[i] = (int)(Math.random() * (numAlleles - genotype.numInputs)) + genotype.numInputs;
				numOutputs = genotype.alleles.get(indices[i]).getNumOutputs();
			}
			outputIndices[i] = (int)(Math.random() * numOutputs);
		}
	}

	public void update() {
		super.update();
		for (int i = 0; i < indices.length; ++i) {
			if (indices[i] >= genotype.getNumAlleles()) {				
				int numAlleles = genotype.getNumAlleles();
				int numOutputs = 0;
				while (numOutputs == 0) {
					indices[i] = (int)(Math.random() * (numAlleles - genotype.numInputs)) + genotype.numInputs;
					numOutputs = genotype.alleles.get(indices[i]).getNumOutputs();
				}
				outputIndices[i] = (int)(Math.random() * numOutputs);
			}
			else if (genotype.alleles.get(indices[i]).getNumOutputs() == 0) {
				int numAlleles = genotype.getNumAlleles();
				int numOutputs = 0;
				while (numOutputs == 0) {
					indices[i] = (int)(Math.random() * (numAlleles - genotype.numInputs)) + genotype.numInputs;
					numOutputs = genotype.alleles.get(indices[i]).getNumOutputs();
				}
				outputIndices[i] = (int)(Math.random() * numOutputs);
			}
			else {
				int numOutputs = 0;
				numOutputs = genotype.alleles.get(indices[i]).getNumOutputs();
				while (outputIndices[i] >= numOutputs) {
					outputIndices[i] = (int)(Math.random() * numOutputs);
				}
			}
		}
	}

	public boolean canChange() {
		return true;
	}

	public boolean mustExist() {
		return false;
	}

	public void cleanUp() {
		super.cleanUp();
		machine.state = machine.copyBitString(machine.initialState);
	}

	public Allele getInput(int i) {
		return genotype.alleles.get(indices[i]);
	}

	public int getOutputIndexOf(int i) {
		return outputIndices[i];
	}

	public int getDelayPeriods() {
		return machine.delayPeriods;
	}

	public int getClockified() {
		return machine.clockified;
	}

	public String toString() {
		String str = "" + index + ": " + machine.name;
		for (int i = 0; i < getNumInputs(); ++i) {
			str += ", " + indices[i] + "[" + outputIndices[i] + "]";
		}
		return str;
	}

	public int[] transition(int[] inputs) {
		return machine.transition(inputs);
	}

	public Allele clone() {
		MachineAllele copy = new MachineAllele(genotype, index, machine.clone());
		copy.indices = genotype.copyIntArray(indices);
		copy.outputIndices = genotype.copyIntArray(indices);
		return copy;
	}

	public Component getComponent(Network network) {
		if (machine.componentPath == null) {
			return null;
		}
		return Component.loadFromFile(network, machine.componentPath, machine.name);
	}
}