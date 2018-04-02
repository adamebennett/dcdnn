package dcdnn;

import java.util.*;

public abstract class Node {

	protected UUID uuid;
	public Network network;
	public ArrayList<Connection> inputs;
	public ArrayList<Connection> outputs;
	public ArrayList<Float> currentInput;
	
	public Node(Network n) {
		uuid = UUID.randomUUID();
		network = n;
		inputs = new ArrayList<Connection>();
		outputs = new ArrayList<Connection>();
	}
	
	public Node(Network n, UUID id) {
		uuid = id;
		network = n;
		inputs = new ArrayList<Connection>();
		outputs = new ArrayList<Connection>();
	}
	
	public UUID getID() {
		return uuid;
	}
	
	public void addInputConnection(Connection c) {
		inputs.add(c);
	}
	
	public void addOutputConnection(Connection c) {
		outputs.add(c);
	}
	
	public void gatherInputs() {
		currentInput = new ArrayList<Float>();
		for (Connection c: inputs) {
			currentInput.add(c.pop());
		}
	}
	
	public void process() {
	}

	public abstract boolean isFiring();
	
	protected void sendPulse(float pulse) {
		for (Connection c: outputs) {
			c.signals[0] = pulse;
		}
	}
	
	public String toString() {
		return "Node " + getID();
	}
}