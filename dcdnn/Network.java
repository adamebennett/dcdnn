package dcdnn;

import java.util.*;

public class Network {

	public int refractoryPeriod;
	public int leakPeriod;
	public ArrayList<Node> nodes;
	public ArrayList<Connection> connections;
	public ArrayList<Component> components;
	public ArrayList<Component> inputComponents;
	public ArrayList<Component> outputComponents;
	public int period;
	public int delay;
	public int clockified;
	public boolean isRecurrent;
	
	public Network(int rP, int lP) {
		this(0, 2f, 2f, rP, lP);
	}
	
	public Network(int numNodes, float minThreshold, float maxThreshold) {
		this(numNodes, minThreshold, maxThreshold, 5, 1);
	}
	
	public Network(int numNodes, float minThreshold, float maxThreshold, int rP, int lP) {
		refractoryPeriod = rP;
		leakPeriod = lP;
		nodes = new ArrayList<Node>();
		connections = new ArrayList<Connection>();
		components = new ArrayList<Component>();
		
		for (int i = 0; i < numNodes; i++) {
			float newThreshold = (float)(Math.random() * (maxThreshold - minThreshold)) + minThreshold;
			Neuron n = new Neuron(this, newThreshold);
			nodes.add(n);
		}

		for (int i = 0; i < nodes.size(); i++) {
			for (int j = 0; j < nodes.size(); j++) {
				if (nodes.get(i) != nodes.get(j)) {
					if ((int)(Math.random() * 10) < 5) {
						int dl = (int)(Math.random() * (100 - 15) + 15); // select delay length for connection
						float signal = 1f; // select whether connection is excitatory or inhibitory
						if (Math.random() * 10 < 3) {
							signal = -1f;
						}
						Connection c = new Connection(this, nodes.get(i), nodes.get(j), dl, signal);
						connections.add(c);
					}
				}
			}
		}
	}
	
	public void step() {
		// prime nodes
		for (Node n: nodes) {
			n.gatherInputs();
		}
		
		// process nodes
		for (Node n: nodes) {
			n.process();
		}
	}	
	
	public Component findComponentOfNode(Node node) {
		for (Component component: components) {
			if (component.nodes.contains(node)) {
				return component;
			}
		}
		return null;
	}
	
	public Node getNodeByID(UUID id) {
		for (Node node: nodes) {
			if (node.getID().equals(id)) {
				return node;
			}
		}
		return null;
	}
	
	// Determines the total time it takes for a signal to travel through the network.
	// Assumes that all paths to a given node take the same number of time steps. 
	protected int calculateTotalDelay() {	
		// find the output node with the longest total delay
		int[] delays = new int[outputComponents.size()];
		for (int i = 0; i < outputComponents.size(); ++i) {
			delays[i] = calculateDelayFrom(outputComponents.get(i).outputs.get(0), new ArrayList<Connection>());
		}
		return max(delays);
	}
	
	private int calculateDelayFrom(Node startNode, ArrayList<Connection> visited) {
		// base cases
		if (getInputNodes().contains(startNode)) {
			return 0;
		}
		if (startNode.inputs.size() == 0) {
			return Integer.MAX_VALUE;
		}
		
		// recursion
		boolean validCandidate = false;
		int[] delays = new int[startNode.inputs.size()];
		for (int i = 0; i < startNode.inputs.size(); ++i) {
			if (!visited.contains(startNode.inputs.get(i))) {
				validCandidate = true;
				ArrayList<Connection> totalVisited = new ArrayList<Connection>(visited);
				totalVisited.add(startNode.inputs.get(i));
				int prevDelay = calculateDelayFrom(startNode.inputs.get(i).startNode, totalVisited);			
				if (prevDelay == Integer.MAX_VALUE) {
					delays[i] = Integer.MAX_VALUE;
				}
				else {
				   delays[i] = prevDelay + startNode.inputs.get(i).delayLength;
				}
			}
		}
		if (validCandidate) {
			return min(delays);
		}
		return Integer.MAX_VALUE;
	}

	private ArrayList<Node> getInputNodes() {
		ArrayList<Node> inputNodes = new ArrayList<Node>();
		for (Component component: inputComponents) {
			for (Node node: component.inputs) {
				inputNodes.add(node);
			}
		}
		return inputNodes;
	}
	
	private int min(int[] arr) {
		int minSoFar = Integer.MAX_VALUE;
		for (int i = 0; i < arr.length; ++i) {
			if (arr[i] != Integer.MAX_VALUE) {
				minSoFar = Math.min(minSoFar, arr[i]);
			}
		}
		return minSoFar;
	}
	
	private int max(int[] arr) {
		int maxSoFar = Integer.MIN_VALUE;
		for (int i = 0; i < arr.length; ++i) {
			if (arr[i] != Integer.MAX_VALUE) {
				maxSoFar = Math.max(maxSoFar, arr[i]);
			}
		}
		return maxSoFar;
	}
}