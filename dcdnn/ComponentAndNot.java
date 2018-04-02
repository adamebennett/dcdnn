package dcdnn;

import java.util.ArrayList;

public class ComponentAndNot extends Component {

	public ComponentAndNot(String l, Network network) {		
		super();
		label = l;
		inputs = new ArrayList<Node>();
		outputs = new ArrayList<Node>();
		nodes = new ArrayList<Node>();
		
		Neuron in1 = new Neuron(network, 1f);
		nodes.add(in1);
		inputs.add(in1);
		
		Neuron in2 = new Neuron(network, 1f);
		nodes.add(in2);
		inputs.add(in2);
				
		Neuron mid = new Neuron(network, 0f);
		nodes.add(mid);
				
		Neuron out = new Neuron(network, 2f);
		nodes.add(out);
		outputs.add(out);
		
		Connection in2ToMid = new Connection(network, in2, mid, 1, -1f);
		network.connections.add(in2ToMid);
		Connection midToOut = new Connection(network, mid, out, 1, 1f);
		network.connections.add(midToOut);
		Connection in1ToOut = new Connection(network, in1, out, 2, 1f);
		network.connections.add(in1ToOut);
		
		calculateTotalDelay();
		
		for (Node node: nodes) {
			network.nodes.add(node);
		}
	}

	public ComponentAndNot(Network network) {
		this("", network);
	}

}