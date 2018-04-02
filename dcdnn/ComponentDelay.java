package dcdnn;

import java.util.ArrayList;

public class ComponentDelay extends Component {

	public ComponentDelay(String l, Network network, int delay) {
		super();
		label = l;
		inputs = new ArrayList<Node>();
		outputs = new ArrayList<Node>();
		nodes = new ArrayList<Node>();
		
		Neuron in = new Neuron(network, 1f);
		nodes.add(in);
		inputs.add(in);
				
		Neuron out = new Neuron(network, 1f);
		nodes.add(out);
		outputs.add(out);
		
		Connection inToOut = new Connection(network, in, out, delay, 1f);
		network.connections.add(inToOut);
		
		calculateTotalDelay();
		
		for (Node node: nodes) {
			network.nodes.add(node);
		}
	}

	public ComponentDelay(Network network, int delay) {
		this("", network, delay);
	}

}