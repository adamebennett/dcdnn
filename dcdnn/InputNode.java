package dcdnn;

public class InputNode extends Node {
	
	public boolean repeat;
	public float[] stream;
	public int position;
	private float prevPulse;
	
	public InputNode(Network n, boolean r, float[] s) {
		super(n);
		repeat = r;
		stream = s;
		position = 0;
		prevPulse = 0f;
	}
	
	public void process() {
		if (position < stream.length) {
			sendPulse(stream[position]);
			prevPulse = stream[position];
			position++;
		}
		else {
			sendPulse(0f);
			prevPulse = 0f;
		}
		if (position == stream.length && repeat) {
			position = 0;
		}
	}

	public boolean isFiring() {
		return prevPulse != 0f;
	}
	
	public String toString() {
		return "Neuron " + getID() + " 1.0 isInput";
	}
}