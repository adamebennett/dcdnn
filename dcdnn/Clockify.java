package dcdnn;

import java.util.ArrayList;
import java.util.HashMap;

public class Clockify {

	public static void clockify(Network network) {
		// add input component for internal clock:	
		Node initNode1 = new InputNode(network, false, new float[] {1f});
		ArrayList<Node> in1Nodes = new ArrayList<Node>();
		in1Nodes.add(initNode1);
		Component clockIn = new Component("CLOCK_IN", network, in1Nodes, in1Nodes, in1Nodes);
		network.components.add(clockIn);
		network.inputComponents.add(clockIn);

		// add components for internal clock:
		// need to take into account the LCM of counts of periodic sections of cycles
		Component[] clocks = null;
		if (network.clockified <= 1) {
			clocks = new Component[2];
			Component prevClock = clockIn;
			for (int i = 0; i < clocks.length; ++i) {
				clocks[i] = new ComponentNode("CLOCK_" + i, network);
				network.components.add(clocks[i]);
				network.connections.add(new Connection(network, prevClock.outputs.get(0), clocks[i].inputs.get(0), 2, 1f));
				prevClock = clocks[i];
			}
			network.connections.add(new Connection(network, prevClock.outputs.get(0), clocks[0].inputs.get(0), 2, 1f));
		}
		else {
			clocks = new Component[network.clockified];
			Component prevClock = clockIn;
			for (int i = 0; i < clocks.length; ++i) {
				clocks[i] = new ComponentNode("CLOCK_" + i, network);
				network.components.add(clocks[i]);
				network.connections.add(new Connection(network, prevClock.outputs.get(0), clocks[i].inputs.get(0), 2, 1f));
				prevClock = clocks[i];
				// add spur here (to trick constraint solver into thinking the clock has periodic sections)
				Component spur = new ComponentNode("SPUR" + i, network);
				spur.inputs = new ArrayList<Node>();
				network.components.add(spur);
				network.connections.add(new Connection(network, spur.outputs.get(0), clocks[i].inputs.get(0), 2, 1f));
			}
			network.connections.add(new Connection(network, prevClock.outputs.get(0), clocks[0].inputs.get(0), 2, 1f));
		}
		network.connections.add(new Connection(network, clockIn.outputs.get(0), clocks[0].inputs.get(0), 2, 1f));

		// add AND gate for each input except the internal clock:
		for (int i = 0; i < network.inputComponents.size()-1; ++i) {
			// get list of nodes where input used to output to while removing those connections
			ArrayList<Node> oldOutputs = new ArrayList<Node>();
			for (Node node: network.inputComponents.get(i).outputs) {
				for (Connection conn: node.outputs) {
					oldOutputs.add(conn.endNode);
					conn.endNode.inputs.remove(conn);
					network.connections.remove(conn);
				}
				node.outputs = new ArrayList<Connection>();
			}

			// set up new and gate
			Component andI = new ComponentAnd("CLOCK_AND_" + i, network);
			network.components.add(andI);
			network.connections.add(new Connection(network, clocks[clocks.length-1].outputs.get(0), andI.inputs.get(0), 2, 1f));
			network.connections.add(new Connection(network, network.inputComponents.get(i).outputs.get(0), andI.inputs.get(1), 2, 1f));

			// have new and gate output to wherever the input used to output to
			for (Node node: oldOutputs) {
				network.connections.add(new Connection(network, andI.outputs.get(0), node, 2, 1f));
			}
		}

		// Add connection from clockIn to all clockified components:
		ArrayList<Component> oldComponents = new ArrayList<Component>(network.components);
		for (Component component: oldComponents) { // want to be able to add components to the list as we loop, get concurrent modification exceptions otherwise
			if (component.getClockified() != -1) {
				network.connections.add(new Connection(network, clockIn.outputs.get(0), component.inputs.get(component.inputs.size()-1), 2, 1f));
			}
		}
	}

	public static void pseudoClockify(Network network) {
		// add input component for internal clock:	
		Node initNode1 = new InputNode(network, false, new float[] {1f});
		ArrayList<Node> in1Nodes = new ArrayList<Node>();
		in1Nodes.add(initNode1);
		Component clockIn = new Component("CLOCK_IN", network, in1Nodes, in1Nodes, in1Nodes);
		network.components.add(clockIn);
		network.inputComponents.add(clockIn);

		// this clock does nothing, it just makes it easier to tell when to input to the network
		Component[] clocks = null;
		clocks = new Component[2];
		Component prevClock = clockIn;
		for (int i = 0; i < clocks.length; ++i) {
			clocks[i] = new ComponentNode("CLOCK_" + i, network);
			network.components.add(clocks[i]);
			network.connections.add(new Connection(network, prevClock.outputs.get(0), clocks[i].inputs.get(0), 2, 1f));
			prevClock = clocks[i];
		}
		network.connections.add(new Connection(network, prevClock.outputs.get(0), clocks[0].inputs.get(0), 2, 1f));
		network.connections.add(new Connection(network, clockIn.outputs.get(0), clocks[0].inputs.get(0), 2, 1f));

		// Add connection from clockIn to all clockified components:
		ArrayList<Component> oldComponents = new ArrayList<Component>(network.components);
		for (Component component: oldComponents) { // want to be able to add components to the list as we loop, get concurrent modification exceptions otherwise
			if (component.getClockified() != -1) {
				network.connections.add(new Connection(network, clockIn.outputs.get(0), component.inputs.get(component.inputs.size()-1), 2, 1f));
			}
		}
	}
}