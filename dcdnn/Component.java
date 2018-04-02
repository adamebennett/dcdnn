package dcdnn;

import java.util.ArrayList;
import org.chocosolver.solver.variables.IntVar;

public class Component {
	
	public String label;
	public ArrayList<Node> inputs;
	public ArrayList<Node> outputs;
	public ArrayList<Node> nodes;
	protected int totalDelay;
	protected int internalPeriod;
	protected int clockified;
	public IntVar scale;
	
	protected Component() {
		internalPeriod = -1;
		clockified = -1;
	}
		
	public Component(String l, Network network, ArrayList<Node> i, ArrayList<Node> o, ArrayList<Node> a) {
		this();
		label = l;
		inputs = i;
		outputs = o;
		nodes = a;
		calculateTotalDelay();
		internalPeriod = -1;
		clockified = -1;
		for (Node node: nodes) {
			network.nodes.add(node);
		}
	}
		
	public Component(Network network, ArrayList<Node> i, ArrayList<Node> o, ArrayList<Node> a) {
		this("", network, i, o, a);
	}
		
	public Component(Network network, ArrayList<Node> i, ArrayList<Node> o, ArrayList<Node> a, ArrayList<Connection> c) {
		this(network, i, o, a);
		for (Connection connection: c) {
			network.connections.add(connection);
		}
		calculateTotalDelay();
	}
		
	public Component(String l, Network network, ArrayList<Node> i, ArrayList<Node> o, ArrayList<Node> a, ArrayList<Connection> c) {
		this(network, i, o, a, c);
		label = l;
	}

	public Component(String l, Network network, ArrayList<Node> i, ArrayList<Node> o, ArrayList<Node> a, ArrayList<Connection> c, int td, int ip, int cf) {
		this(l, network, i, o, a, c);
		if (td != -1) {
			totalDelay = td;
		}
		else {
			calculateTotalDelay();	
		}
		internalPeriod = ip;
		clockified = cf;
	}

	public Component(Network network, ArrayList<Node> i, ArrayList<Node> o, ArrayList<Node> a, ArrayList<Connection> c, int td, int ip, int cf) {
		this("", network, i, o, a, c, td, ip, cf);
	}
	
	// Determines the total time it takes for a signal to travel through the component.
	// Assumes that all paths to a given node take the same number of time steps.
	protected void calculateTotalDelay() {	
		// find the output node with the longest total delay
		int[] delays = new int[outputs.size()];
		for (int i = 0; i < outputs.size(); ++i) {
			delays[i] = calculateDelayFrom(outputs.get(i), new ArrayList<Connection>());
		}
		totalDelay = max(delays);
		
		// modifies final connections so that all outputs take totalDelay timesteps to reach
		/*for (int i = 0; i < outputs.size(); ++i) {
			for (Connection connection: outputs.get(i).inputs) {
				connection.resetDelayLength(connection.delayLength + (totalDelay - delays[i]));
			}
		}*/
		
		System.out.println(label + " totalDelay = " + totalDelay);
	}
	
	private int calculateDelayFrom(Node startNode, ArrayList<Connection> visited) {
		// base cases
		if (inputs.contains(startNode)) {
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
	
	public int getTotalDelay() {
		return totalDelay;
	}
	
	public int getInternalPeriod() {
		return internalPeriod;
	}
	
	public int getClockified() {
		return clockified;
	}
	
	public static Component loadFromFile(Network network, String path, String label) {
		Component comp = DCDNNIO.importComponent(network, path);
		comp.label = label;
		return comp;
	}
	
}