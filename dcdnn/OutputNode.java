package dcdnn;

public class OutputNode extends Node {
	
	public String label;
	public int activationDelay;
	private int ticksToRecord;
	private int ticksSoFar;
	private DCDNN dcdnn;
	
	public OutputNode(Network n, String s, int startAfter, int recordFor, DCDNN dcd) {
		super(n);
		label = s;
		activationDelay = startAfter;
		ticksToRecord = recordFor;
		ticksSoFar = 0;
		dcdnn = dcd;
	}
	
	public void process() {
		ticksSoFar++;
		if (ticksSoFar >= activationDelay && ticksSoFar - activationDelay < ticksToRecord) {		
			float sum = 0;
			for (Float f: currentInput) {
				sum += f;
			}		
			dcdnn.addOutput("\n\nOutput Node: " + label + ", Ticks Since Activation: " + (ticksSoFar - activationDelay) + ", Total Ticks: " + ticksSoFar + ", Sum: " + sum);
		}
	}

	public boolean isFiring() {
		return false;
	}
	
}