package dcdnn;

import java.util.ArrayList;

public class BufferAllele extends MachineAllele {

	public BufferAllele(Genotype g, int i, FiniteStateMachine fsm) {
		super(g, i, fsm);
	}

	public int getNumInputs() {
		return machine.numInputs;
	}

	public int getNumOutputs() {
		return machine.numOutputs;
	}

	public void init() {
	}

	public void update() {
		super.update();
	}

	public boolean canChange() {
		return false;
	}

	public boolean mustExist() {
		return true;
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
		BufferAllele copy = new BufferAllele(genotype, index, machine.clone());
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

	public static BufferAllele inputBuffer(Genotype g, int i) {
		FiniteStateMachine fsm = null;
		if (g.numInputs == 1) {
			fsm = FiniteStateMachine.buffer1Machine();
		}
		else if (g.numInputs == 2) {
			fsm = FiniteStateMachine.buffer2Machine();
		}
		else if (g.numInputs == 3) {
			fsm = FiniteStateMachine.buffer3Machine();
		}
		else {
			// make this more robust at some point
		}
		BufferAllele buffer = new BufferAllele(g, i, fsm);
		for (int j = 0; j < g.numInputs; ++j) {
			buffer.indices[j] = j;
		}
		return buffer;
	}
}