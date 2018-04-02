package dcdnn;

import java.util.ArrayList;

public class ComponentAnd extends Component {

	public ComponentAnd(String l, Network network) {
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
				
		Neuron out = new Neuron(network, 2f);
		nodes.add(out);
		outputs.add(out);
		
		Connection in1ToOut = new Connection(network, in1, out, 1, 1f);
		network.connections.add(in1ToOut);
		
		Connection in2ToOut = new Connection(network, in2, out, 1, 1f);
		network.connections.add(in2ToOut);
		
		calculateTotalDelay();
		
		for (Node node: nodes) {
			network.nodes.add(node);
		}
	}

	public ComponentAnd(Network network) {
		this("", network);
	}
	
}