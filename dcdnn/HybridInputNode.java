package dcdnn;

// Ok, things are getting really really hacky here. This is a combination InputNode/Neuron. It outputs its stream
// and then "turns" into a regular Neuron.
public class HybridInputNode extends Node {
	
	public boolean doneAsInput;
	public float[] stream;
	public int position;
	public float threshold;
	private int remainingRefractoryTime;
	private int remainingLeakTime;
	
	public HybridInputNode(Network n, float[] s, float th) {
		super(n);
		doneAsInput = false;
		stream = s;
		position = 0;
		threshold = th;
		remainingRefractoryTime = 0;
		remainingLeakTime = 0;
	}
	
	public void process() {
		if (!doneAsInput) {
			if (position < stream.length) {
				sendPulse(stream[position]);
				position++;
			}
			else {
				sendPulse(0f);
			}
			if (position == stream.length) {
				doneAsInput = true;
			}
		}
		else {
			processAsNeuron();
		}
	}
	
	public void processAsNeuron() {
		if (remainingLeakTime > 0) {
			sendPulse(1f);
			remainingLeakTime--;
			return;
		}
		if (remainingRefractoryTime > 0) {
			sendPulse(0f);
			remainingRefractoryTime--;
			return;
		}
		float sum = 0;
		for (Float f: currentInput) {
			sum += f;
		}
		if (sum >= threshold) {
			sendPulse(1f);
			remainingRefractoryTime = network.refractoryPeriod;
			remainingLeakTime = network.leakPeriod;
			return;
		}
		sendPulse(0f);
	}
	
	public boolean isFiring() {
		if (!doneAsInput) {
			if (position < stream.length) {
				if (stream[position] == 1) {
					return true;
				}
				else {
					return false;
				}
			}
			else {
				return false;
			}
		}
		else {			
			if (remainingLeakTime > 0) {
				return true;
			}
			if (remainingRefractoryTime > 0) {
				return false;
			}
			float sum = 0;
			for (Float f: currentInput) {
				sum += f;
			}
			if (sum >= threshold) {
				return true;
			}
			return false;
		}
	}
	
	public String toString() {
		return "Neuron " + getID() + " " + threshold + " isInput";
	}
}