package dcdnn;

import java.util.*;

public class Neuron extends Node {

	public float threshold;
	private int remainingRefractoryTime;
	private int remainingLeakTime;

	public Neuron(Network n, float t) {
		super(n);
		threshold = t;
		remainingRefractoryTime = 0;
		remainingLeakTime = 0;
	}
	
	public Neuron(Network n, UUID id, float t) {
		super(n, id);
		threshold = t;
		remainingRefractoryTime = 0;
		remainingLeakTime = 0;
	}
	
	public void process() {
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
	
	public String toString() {
		return "Neuron " + getID() + " " + threshold;
	}
}