package dcdnn;

import java.util.ArrayList;

public class ComponentNot extends Component {

	public ComponentNot(String l, Network network) {		
		super();
		label = l;
		inputs = new ArrayList<Node>();
		outputs = new ArrayList<Node>();
		nodes = new ArrayList<Node>();
		
		Neuron in = new Neuron(network, 1f);
		nodes.add(in);
		inputs.add(in);
				
		Neuron mid = new Neuron(network, 0f);
		nodes.add(mid);
				
		Neuron out = new Neuron(network, 1f);
		nodes.add(out);
		outputs.add(out);
		
		Connection inToMid = new Connection(network, in, mid, 1, -1f);
		network.connections.add(inToMid);
		Connection midToOut = new Connection(network, mid, out, 1, 1f);
		network.connections.add(midToOut);
		
		calculateTotalDelay();
		
		for (Node node: nodes) {
			network.nodes.add(node);
		}
	}

	public ComponentNot(Network network) {
		this("", network);
	}

}