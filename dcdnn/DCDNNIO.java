package dcdnn;

import java.io.*;
import java.util.*;

public class DCDNNIO {
	
	public static boolean exportAsNetwork(Network network, String filename) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
			writer.write("Period " + network.period + "\n");
			writer.write("Delay " + network.delay + "\n");
			writer.write("Clockified " + network.clockified + "\n");
			for (Node n: network.nodes) {
				writer.write("# " + network.findComponentOfNode(n).label + "\n");
				writer.write(n + "\n");
			}
			for (Connection c: network.connections) {
				writer.write(c + "\n");
			}
			writer.close();			
		}
		catch (Exception e) {
			return false;
		}
		return true;
	}
	
	public static Component importComponent(Network network, String path) {
		ArrayList<Node> inputNodes = new ArrayList<Node>();
		ArrayList<Node> outputNodes = new ArrayList<Node>();
		ArrayList<NodeWithID> nodes = new ArrayList<NodeWithID>();
		ArrayList<Connection> connections = new ArrayList<Connection>();
		int internalPeriod = -1;
		int clockified = -1;
		int totalDelay = -1;
		try {
			String currentLine;
			BufferedReader reader = new BufferedReader(new InputStreamReader(DCDNN.class.getResourceAsStream(path)));
			while ((currentLine = reader.readLine()) != null) {
				if (currentLine.startsWith("#")) {
					continue;
				}
				if (currentLine.startsWith("Period")) {
					String[] tokens = currentLine.split(" ");
					internalPeriod = Integer.parseInt(tokens[1]);
					continue;
				}
				if (currentLine.startsWith("Delay")) {
					String[] tokens = currentLine.split(" ");
					totalDelay = Integer.parseInt(tokens[1]);
					continue;
				}
				if (currentLine.startsWith("Clockified")) {
					String[] tokens = currentLine.split(" ");
					clockified = Integer.parseInt(tokens[1]);
					continue;
				}
				if (currentLine.startsWith("Neuron")) {
					NodeWithID node = readNode(network, currentLine);
					nodes.add(node);
					if (currentLine.endsWith("isInput")) {
						inputNodes.add(node.node);
					}
					if (currentLine.endsWith("isOutput")) {
						outputNodes.add(node.node);
					}
				}
				if (currentLine.startsWith("Connection")) {
					Connection connection = readConnection(network, currentLine, nodes);
					connections.add(connection);
				}
			}
			reader.close();
		}
		catch (Exception e) {
		}
		ArrayList<Node> nodesWithoutIDs = new ArrayList<Node>();
		for (NodeWithID n: nodes) {
			nodesWithoutIDs.add(n.node);
		}
		Component component = new Component(network, inputNodes, outputNodes, nodesWithoutIDs, connections, totalDelay, internalPeriod, clockified);
		return component;
	}
	
	public static NodeWithID readNode(Network network, String currentLine) {
		NodeWithID node = null;
		String[] tokens = currentLine.split(" ");
		if (tokens[0].equals("Neuron")) {
			node = new NodeWithID(new Neuron(network, Float.parseFloat(tokens[2])), tokens[1]);
		}
		return node;
	}
	
	public static Connection readConnection(Network network, String currentLine, ArrayList<NodeWithID> nodes) {
		Connection connection = null;
		String[] tokens = currentLine.split(" ");
		if (tokens[0].equals("Connection")) {
			connection = new Connection(network, getNodeByID(nodes, tokens[2]).node,
				getNodeByID(nodes, tokens[3]).node, Integer.parseInt(tokens[4]), 
				Float.parseFloat(tokens[5]));
		}
		return connection;
	}
	
	public static NodeWithID getNodeByID(ArrayList<NodeWithID> nodes, String id) {
		for (NodeWithID node: nodes) {
			if (node.id.equals(id)) {
				return node;
			}
		}
		return null;
	}

	public static void startRecording(Network network, boolean recordAll) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter("plots/plot.csv", false));
			writer.write(",");
			if (recordAll) {
				for (Node node: network.nodes) {
					writer.write(node.getID() + ",");
				}
			}
			else {
				for (Component component: network.inputComponents) {
					writer.write(component.label + ",");
				}
				for (Component component: network.outputComponents) {
					writer.write(component.label + ",");
				}
			}
			writer.write("\n");
			writer.close();			
		}
		catch (Exception e) {
			return;
		}
	}

	public static void record(Network network, int time, boolean recordAll) {
		try {		
			BufferedWriter writer = new BufferedWriter(new FileWriter("plots/plot.csv", true));
			writer.write(time + ",");
			int column = 1;
			if (recordAll) {
				for (Node node: network.nodes) {
					if (node.isFiring()) {
						writer.write(column + ",");
					}
					else {
						writer.write(",");
					}
					column++;
				}
			}
			else {
				for (Component component: network.inputComponents) {
					if (component.nodes.get(0).isFiring()) {
						writer.write(column + ",");
					}
					else {
						writer.write(",");
					}
					column++;
				}
				for (Component component: network.outputComponents) {
					if (component.nodes.get(0).isFiring()) {
						writer.write(column + ",");
					}
					else {
						writer.write(",");
					}
					column++;
				}
			}
			writer.write("\n");
			writer.close();		
		}
		catch (Exception e) {
			return;
		}
	}
	
}