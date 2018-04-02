package dcdnn;

import java.util.ArrayList;

public class ComponentNode extends Component {

	public ComponentNode(String l, Network network) {
		super();
		label = l;
		inputs = new ArrayList<Node>();
		outputs = new ArrayList<Node>();
		nodes = new ArrayList<Node>();
		
		Neuron n = new Neuron(network, 1f);
		nodes.add(n);
		inputs.add(n);
		outputs.add(n);
		
		calculateTotalDelay();
		
		for (Node node: nodes) {
			network.nodes.add(node);
		}
	}

	public ComponentNode(Network network) {
		this("", network);
	}
	
}