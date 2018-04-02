package dcdnn;

import java.util.ArrayList;

public class ComponentRepeat extends Component {

	public ComponentRepeat(String l, Network network, int delay, boolean autoStart) {	
		super();
		label = l;
		internalPeriod = delay; // ???
		inputs = new ArrayList<Node>();
		outputs = new ArrayList<Node>();
		nodes = new ArrayList<Node>();
		
		Neuron in = new Neuron(network, 1f);
		nodes.add(in);
		inputs.add(in);
		
		Neuron mid = new Neuron(network, 1f);
		nodes.add(mid);
				
		Neuron out = new Neuron(network, 1f);
		nodes.add(out);
		outputs.add(out);
		
		Connection inToMid = new Connection(network, in, mid, 1, 1f);
		network.connections.add(inToMid);
		Connection midToOut = new Connection(network, mid, out, Math.max(1, delay - 2), 1f);
		network.connections.add(midToOut);
		Connection outToIn = new Connection(network, out, in, 1, 1f);
		network.connections.add(outToIn);
		
		if (autoStart) {
			Node autoStarter = new InputNode(network, false, new float[] {1f});
			nodes.add(autoStarter);
			Connection autoStarterToMid = new Connection(network, autoStarter, mid, 1, 1f);
			network.connections.add(autoStarterToMid);
		}
		
		calculateTotalDelay();
		
		for (Node node: nodes) {
			network.nodes.add(node);
		}
	}

	public ComponentRepeat(Network network, int delay, boolean autoStart) {
		this("", network, delay, autoStart);
	}

}