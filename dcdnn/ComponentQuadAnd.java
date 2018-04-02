package dcdnn;

import java.util.ArrayList;

public class ComponentQuadAnd extends Component {

	public ComponentQuadAnd(String l, Network network) {
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
		
		Neuron in3 = new Neuron(network, 1f);
		nodes.add(in3);
		inputs.add(in3);
		
		Neuron in4 = new Neuron(network, 1f);
		nodes.add(in4);
		inputs.add(in4);
				
		Neuron out = new Neuron(network, 4f);
		nodes.add(out);
		outputs.add(out);
		
		Connection in1ToOut = new Connection(network, in1, out, 1, 1f);
		network.connections.add(in1ToOut);
		
		Connection in2ToOut = new Connection(network, in2, out, 1, 1f);
		network.connections.add(in2ToOut);
		
		Connection in3ToOut = new Connection(network, in3, out, 1, 1f);
		network.connections.add(in3ToOut);
		
		Connection in4ToOut = new Connection(network, in4, out, 1, 1f);
		network.connections.add(in4ToOut);
		
		calculateTotalDelay();
		
		for (Node node: nodes) {
			network.nodes.add(node);
		}
	}

	public ComponentQuadAnd(Network network) {
		this("", network);
	}
	
}