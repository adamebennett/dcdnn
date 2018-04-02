package dcdnn;

import java.util.*;

public class Connection {

	private UUID uuid;
	public Network network;
	public Node startNode;
	public Node endNode;
	public int delayLength;
	public float[] signals;
	public float weight;
	
	public Connection(Network n, Node s, Node e, int dl, float w) {
		uuid = UUID.randomUUID();
		network = n;
		startNode = s;
		startNode.addOutputConnection(this);
		endNode = e;
		endNode.addInputConnection(this);
		delayLength = dl;
		signals = new float[delayLength];
		for (int i = 0; i < delayLength; ++i) {
			signals[i] = 0f;
		}
		weight = w;
	}
	
	public Connection(Network n, UUID id, Node s, Node e, int dl, float w) {
		uuid = id;
		network = n;
		startNode = s;
		startNode.addOutputConnection(this);
		endNode = e;
		endNode.addInputConnection(this);
		delayLength = dl;
		signals = new float[delayLength];
		for (int i = 0; i < delayLength; ++i) {
			signals[i] = 0f;
		}
		weight = w;
	}
	
	public UUID getID() {
		return uuid;
	}
	
	public float pop() {
		float toReturn = signals[delayLength - 1];
		for (int i = delayLength - 1; i > 0; --i) {
			signals[i] = signals[i-1];
		}
		return (toReturn * weight);
	}
	
	public void resetDelayLength(int newLength) {
		delayLength = newLength;
		signals = new float[delayLength];
		for (int i = 0; i < delayLength; ++i) {
			signals[i] = 0f;
		}
	}
	
	public String toString() {
		return "Connection " + getID() + " " + startNode.getID() + " " + endNode.getID() + " " + delayLength + " " + weight;
	}
	
}