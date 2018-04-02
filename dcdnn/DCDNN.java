package dcdnn;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.ArrayList;
import java.util.HashMap;

public class DCDNN extends JFrame {

	public Network network;
	public ArrayList<GraphicalNode> graphicalNodes;
	public ArrayList<GraphicalComponent> graphicalComponents;
	public View view;
	public Timer timer;
	public Console console;
	public boolean paused;
	public boolean recording;
	public boolean recordMode;
	public int time;
	public int lcmForClockify;

	public DCDNN() {
		super("DCDNN");
		setDefaultCloseOperation(EXIT_ON_CLOSE); // allow window to close
		setSize(500, 500);	
										
		graphicalNodes = new ArrayList<GraphicalNode>();
		graphicalComponents = new ArrayList<GraphicalComponent>();
		paused = true;
		
		view = new View(500, 500, network, graphicalNodes, graphicalComponents);	
		
		// Add a timer
		timer = new Timer(50, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				handleTimerTick();
			}
		});
		timer.start();
		
		console = new Console(this);
		
		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, console, view);
		splitPane.setDividerLocation(150);
		this.getContentPane().add(splitPane);
	}
	
	public static void main(String[] args) {
		try {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					DCDNN dcdnn = new DCDNN();
					dcdnn.setVisible(true);
					dcdnn.console.commandLine.requestFocus();
				}
			});
		}
		catch (Exception e) {
			System.out.println(e);
		}
	}
	
	private void handleTimerTick()
	{
		if (!paused) {
			step();
		}
		view.refresh((int)view.getSize().getWidth(), (int)view.getSize().getHeight());
	}
	
	public void step() {
		if (network != null) {
			network.step();
			record();
		}
		view.refresh((int)view.getSize().getWidth(), (int)view.getSize().getHeight());
	}
	
	public void addOutput(String s) {
		console.append(s);
	}
	
	public void addInputNodes() {				
		for (Node node: network.nodes) {
			graphicalNodes.add(new GraphicalNode(node, (float)(Math.random()), (float)(Math.random()), Color.BLACK));
		}
		
		for (int i = 0; i < 5; ++i) {
			InputNode inputNode = new InputNode(network, false, new float[] {1f, 1f});
			graphicalNodes.add(new GraphicalNode(inputNode, (float)(Math.random()), (float)(Math.random()), Color.CYAN));
			network.nodes.add(inputNode);
			for (Node node: network.nodes) {
				if ((int)(Math.random() * 10) < 6) {
					int dl = (int)(Math.random() * (100 - 15) + 15);
					network.connections.add(new Connection(network, inputNode, node, dl, 1f));
				}
			}
		}
	}

	public void setInputs(float[] inputs) {
		for (int i = 0; i < inputs.length; ++i) {
			if (inputs[i] != -1) {
				InputNode input = (InputNode)network.inputComponents.get(i).nodes.get(0);
				input.repeat = false;
				input.stream = new float[]{inputs[i]};
				input.position = 0;
			}
		}
	}

	public void startRecording(boolean allNodes) {
		recording = true;
		recordMode = allNodes;
		DCDNNIO.startRecording(network, recordMode);
		time = 0;
	}

	public void record() {
		if (recording) {
			DCDNNIO.record(network, time, recordMode);
		}
		time++;
	}

	public void learn(FiniteStateMachine goal) {
		PopulationWorker worker = new PopulationWorker(this, console, goal.clone());
		worker.execute();
	}

	public void loadNetworkFromGenotype(Genotype genotype) {
		ArrayList<Component> components = new ArrayList<Component>();
		network.inputComponents = new ArrayList<Component>();
		network.outputComponents = new ArrayList<Component>();

		HashMap<Allele, Component> mapping = new HashMap<Allele, Component>();

		// get components from genotype
		for (int i = 0; i < genotype.alleles.size(); ++i) {
			Allele allele = genotype.alleles.get(i);
			if (allele.pruned) {
				continue;
			}
			Component component = allele.getComponent(network);
			if (component == null) {
				addOutput("Error: missing component file");
				return;
			}
			components.add(component);
			mapping.put(allele, component);
			if (i < genotype.numInputs) {
				network.inputComponents.add(component);
			}
			if (i >= genotype.numInputs && i < genotype.numInputs + genotype.numOutputs) {
				network.outputComponents.add(component);
			}
		}

		// get connections from genotype
		for (int a = 0; a < genotype.alleles.size(); ++a) {
			Allele allele = genotype.alleles.get(a);
			if (allele.pruned) {
				continue;
			}
			for (int i = 0; i < allele.getNumInputs(); ++i) {
				Allele from = allele.getInput(i);
				if (from.pruned) {
					continue;
				}
				int fromIndex = allele.getOutputIndexOf(i);
				Component toComp = mapping.get(allele);
				Component fromComp = mapping.get(allele.getInput(i));
				Connection connection = new Connection(network, fromComp.outputs.get(fromIndex), toComp.inputs.get(i), 2, 1f);
				network.connections.add(connection);
			}
		}

		int currZ = 0;
		int currX = currZ;
		int currY = 0;
		for (Component component: components) {
			network.components.add(component);
			float width = 0.10f;
			float height = 0.10f;
			float x = currX * 0.12f + 0.01f;
			float y = currY * 0.12f + 0.01f;
			currX -= 1;
			currY += 1;
			if (currY > currZ) {
				currZ += 1;
				currX = currZ;
				currY = 0;
			}
			//System.out.println("x: " + x + ", y: " + y + ", w: " + width + ", h: " + height);
			graphicalComponents.add(new GraphicalComponent(component, x, y, width, height, Color.LIGHT_GRAY, graphicalNodes));
		}
		
		ConstraintSolver.adjustDelayLengths(this, network, network.outputComponents);
	}

	public void clockifyNetwork() {		
		Clockify.clockify(network);

		// (re)set up graphical components
		graphicalNodes.clear();
		graphicalComponents.clear();
		int currX = 0;
		int currY = 0;
		for (Component component: network.components) {
			float width = 0.10f;
			float height = 0.10f;
			float x = currX * 0.12f + 0.01f;
			float y = currY * 0.12f + 0.01f;
			currX += 1;
			if (currX > 7) {
				currY += 1;
				currX = 0;
			}
			//System.out.println("x: " + x + ", y: " + y + ", w: " + width + ", h: " + height);
			graphicalComponents.add(new GraphicalComponent(component, x, y, width, height, Color.LIGHT_GRAY, graphicalNodes));
		}
		
		if (network.outputComponents != null) {
			ConstraintSolver.adjustDelayLengths(this, network, network.outputComponents);
		}
		else {
			ConstraintSolver.adjustDelayLengths(this, network);
		}
	}

	public void pseudoClockifyNetwork() {		
		Clockify.pseudoClockify(network);

		// (re)set up graphical components
		graphicalNodes.clear();
		graphicalComponents.clear();
		int currX = 0;
		int currY = 0;
		for (Component component: network.components) {
			float width = 0.10f;
			float height = 0.10f;
			float x = currX * 0.12f + 0.01f;
			float y = currY * 0.12f + 0.01f;
			currX += 1;
			if (currX > 7) {
				currY += 1;
				currX = 0;
			}
			//System.out.println("x: " + x + ", y: " + y + ", w: " + width + ", h: " + height);
			graphicalComponents.add(new GraphicalComponent(component, x, y, width, height, Color.LIGHT_GRAY, graphicalNodes));
		}
		
		if (network.outputComponents != null) {
			ConstraintSolver.adjustDelayLengths(this, network, network.outputComponents);
		}
		else {
			ConstraintSolver.adjustDelayLengths(this, network);
		}
	}
	
	public void addXORComponents() {		
		ArrayList<Component> components = new ArrayList<Component>();
		network.inputComponents = new ArrayList<Component>();
		network.outputComponents = new ArrayList<Component>();
		
		// add input component(s):
		InputNode initNode1 = new InputNode(network, false, new float[] {0f});
		ArrayList<Node> in1Nodes = new ArrayList<Node>();
		in1Nodes.add(initNode1);
		Component in1 = new Component("In 1", network, in1Nodes, in1Nodes, in1Nodes);
		components.add(in1);
		network.inputComponents.add(in1);
		
		InputNode initNode2 = new InputNode(network, false, new float[] {1f});
		ArrayList<Node> in2Nodes = new ArrayList<Node>();
		in2Nodes.add(initNode2);
		Component in2 = new Component("In 2", network, in2Nodes, in2Nodes, in2Nodes);
		components.add(in2);
		network.inputComponents.add(in2);
		
		// add other components:		
		Component nand1 = new ComponentNand("Nand 1", network);
		components.add(nand1);
		
		network.connections.add(new Connection(network, in1.outputs.get(0), nand1.inputs.get(0), 2, 1f));
		network.connections.add(new Connection(network, in2.outputs.get(0), nand1.inputs.get(1), 2, 1f));
		
		Component nand2 = new ComponentNand("Nand 2", network);
		components.add(nand2);
		
		network.connections.add(new Connection(network, in1.outputs.get(0), nand2.inputs.get(0), 2, 1f));
		network.connections.add(new Connection(network, nand1.outputs.get(0), nand2.inputs.get(1), 2, 1f));
		
		Component nand3 = new ComponentNand("Nand 3", network);
		components.add(nand3);
		
		network.connections.add(new Connection(network, in2.outputs.get(0), nand3.inputs.get(0), 2, 1f));
		network.connections.add(new Connection(network, nand1.outputs.get(0), nand3.inputs.get(1), 2, 1f));
		
		Component nand4 = new ComponentNand("Nand 4", network);
		components.add(nand4);
		
		network.connections.add(new Connection(network, nand2.outputs.get(0), nand4.inputs.get(0), 2, 1f));
		network.connections.add(new Connection(network, nand3.outputs.get(0), nand4.inputs.get(1), 2, 1f));
		
		// need some extra components to prevent garbage signals between whole runs
		Component or = new ComponentOr("Or 1", network);
		components.add(or);
		
		network.connections.add(new Connection(network, in1.outputs.get(0), or.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, in2.outputs.get(0), or.inputs.get(1), 1, 1f));
		
		Component and = new ComponentAnd("And 1", network);
		components.add(and);
		
		network.connections.add(new Connection(network, nand4.outputs.get(0), and.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, or.outputs.get(0), and.inputs.get(1), 1, 1f));
		
		// add output component:
		Node finalNode = new Neuron(network, 1);
		ArrayList<Node> outNodes = new ArrayList<Node>();
		outNodes.add(finalNode);
		Component output = new Component("Out", network, outNodes, outNodes, outNodes);
		components.add(output);
		network.outputComponents.add(output);
		
		int delayBeforeOutput = 5;
		network.connections.add(new Connection(network, and.outputs.get(0), finalNode, delayBeforeOutput, 1f));
		
		int currZ = 0;
		int currX = currZ;
		int currY = 0;
		for (Component component: components) {
			network.components.add(component);
			float width = 0.18f;
			float height = 0.18f;
			float x = currX * 0.2f + 0.01f;
			float y = currY * 0.2f + 0.01f;
			currX -= 1;
			currY += 1;
			if (currY > currZ) {
				currZ += 1;
				currX = currZ;
				currY = 0;
			}
			//System.out.println("x: " + x + ", y: " + y + ", w: " + width + ", h: " + height);
			graphicalComponents.add(new GraphicalComponent(component, x, y, width, height, Color.LIGHT_GRAY, graphicalNodes));
		}
		
		ConstraintSolver.adjustDelayLengths(this, network, network.outputComponents);
	}
	
	public void addAndOrComponents() {		
		ArrayList<Component> components = new ArrayList<Component>();
		network.inputComponents = new ArrayList<Component>();
		network.outputComponents = new ArrayList<Component>();
		
		// add input component(s):
		InputNode initNode1 = new InputNode(network, false, new float[] {0f});
		ArrayList<Node> in1Nodes = new ArrayList<Node>();
		in1Nodes.add(initNode1);
		Component in1 = new Component("In 1", network, in1Nodes, in1Nodes, in1Nodes);
		components.add(in1);
		network.inputComponents.add(in1);
		
		InputNode initNode2 = new InputNode(network, false, new float[] {0f});
		ArrayList<Node> in2Nodes = new ArrayList<Node>();
		in2Nodes.add(initNode2);
		Component in2 = new Component("In 2", network, in2Nodes, in2Nodes, in2Nodes);
		components.add(in2);
		network.inputComponents.add(in2);
		
		InputNode initNode3 = new InputNode(network, false, new float[] {0f});
		ArrayList<Node> in3Nodes = new ArrayList<Node>();
		in3Nodes.add(initNode3);
		Component in3 = new Component("In 3", network, in3Nodes, in3Nodes, in3Nodes);
		components.add(in3);
		network.inputComponents.add(in3);
		
		// add other components:		
		Component or = new ComponentOr("OR", network);
		components.add(or);		
		network.connections.add(new Connection(network, in1.outputs.get(0), or.inputs.get(0), 2, 1f));
		network.connections.add(new Connection(network, in2.outputs.get(0), or.inputs.get(1), 2, 1f));

		Component not = new ComponentNot("NOT", network);
		components.add(not);		
		network.connections.add(new Connection(network, in3.outputs.get(0), not.inputs.get(0), 2, 1f));

		Component and = new ComponentAnd("AND", network);
		components.add(and);		
		network.connections.add(new Connection(network, or.outputs.get(0), and.inputs.get(0), 2, 1f));
		network.connections.add(new Connection(network, not.outputs.get(0), and.inputs.get(1), 2, 1f));
		
		// add output component:
		Node finalNode = new Neuron(network, 1);
		ArrayList<Node> outNodes = new ArrayList<Node>();
		outNodes.add(finalNode);
		Component output = new Component("Out", network, outNodes, outNodes, outNodes);
		components.add(output);
		network.outputComponents.add(output);
		
		int delayBeforeOutput = 5;
		network.connections.add(new Connection(network, and.outputs.get(0), finalNode, delayBeforeOutput, 1f));
		
		int currZ = 0;
		int currX = currZ;
		int currY = 0;
		for (Component component: components) {
			network.components.add(component);
			float width = 0.18f;
			float height = 0.18f;
			float x = currX * 0.2f + 0.01f;
			float y = currY * 0.2f + 0.01f;
			currX -= 1;
			currY += 1;
			if (currY > currZ) {
				currZ += 1;
				currX = currZ;
				currY = 0;
			}
			//System.out.println("x: " + x + ", y: " + y + ", w: " + width + ", h: " + height);
			graphicalComponents.add(new GraphicalComponent(component, x, y, width, height, Color.LIGHT_GRAY, graphicalNodes));
		}
		
		ConstraintSolver.adjustDelayLengths(this, network, network.outputComponents);
	}
	
	public void addFlipFlopComponents() {		
		ArrayList<Component> components = new ArrayList<Component>();
		network.inputComponents = new ArrayList<Component>();
		network.outputComponents = new ArrayList<Component>();
		
		// add input component(s):	
		InputNode initNode1 = new InputNode(network, false, new float[] {0f});
		ArrayList<Node> in1Nodes = new ArrayList<Node>();
		in1Nodes.add(initNode1);
		Component s = new Component("S", network, in1Nodes, in1Nodes, in1Nodes);
		components.add(s);
		network.inputComponents.add(s);
		
		InputNode initNode2 = new InputNode(network, false, new float[] {0f});
		ArrayList<Node> in2Nodes = new ArrayList<Node>();
		in2Nodes.add(initNode2);
		Component r = new Component("R", network, in2Nodes, in2Nodes, in2Nodes);
		components.add(r);
		network.inputComponents.add(r);
		
		// add other component(s):		
		Component andOr = Component.loadFromFile(network, "/networks/andor.ng", "ANDOR");
		components.add(andOr);
		network.connections.add(new Connection(network, s.outputs.get(0), andOr.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, andOr.outputs.get(0), andOr.inputs.get(1), 1, 1f));
		network.connections.add(new Connection(network, r.outputs.get(0), andOr.inputs.get(2), 1, 1f));

		Component not1 = new ComponentNot("NOT_1", network);
		components.add(not1);
		network.connections.add(new Connection(network, andOr.outputs.get(0), not1.inputs.get(0), 1, 1f));
		
		
		// add output components:
		Neuron finalNode1 = new Neuron(network, 1);
		ArrayList<Node> out1Nodes = new ArrayList<Node>();
		out1Nodes.add(finalNode1);
		Component q = new Component("Q", network, out1Nodes, out1Nodes, out1Nodes);
		components.add(q);
		network.outputComponents = new ArrayList<Component>();
		network.outputComponents.add(q);
		
		Neuron finalNode2 = new Neuron(network, 1);
		ArrayList<Node> out2Nodes = new ArrayList<Node>();
		out2Nodes.add(finalNode2);
		Component qBar = new Component("Q BAR", network, out2Nodes, out2Nodes, out2Nodes);
		components.add(qBar);
		network.outputComponents.add(qBar);
		
		int delayBeforeOutput = 1;
		network.connections.add(new Connection(network, andOr.outputs.get(0), q.inputs.get(0), 2, 1f));
		network.connections.add(new Connection(network, not1.outputs.get(0), qBar.inputs.get(0), 2, 1f));
				
		int currZ = 0;
		int currX = currZ;
		int currY = 0;
		for (Component component: components) {
			network.components.add(component);
			float width = 0.10f;
			float height = 0.10f;
			float x = currX * 0.12f + 0.01f;
			float y = currY * 0.12f + 0.01f;
			currX -= 1;
			currY += 1;
			if (currY > currZ) {
				currZ += 1;
				currX = currZ;
				currY = 0;
			}
			//System.out.println("x: " + x + ", y: " + y + ", w: " + width + ", h: " + height);
			graphicalComponents.add(new GraphicalComponent(component, x, y, width, height, Color.LIGHT_GRAY, graphicalNodes));
		}
		
		ConstraintSolver.adjustDelayLengths(this, network, network.outputComponents);
	}
	
	public void addSlackCounterComponentsFourBits() {		
		ArrayList<Component> components = new ArrayList<Component>();
		network.inputComponents = new ArrayList<Component>();
		
		// add input component(s):	
		InputNode initNode1 = new InputNode(network, false, new float[] {0f});
		ArrayList<Node> in1Nodes = new ArrayList<Node>();
		in1Nodes.add(initNode1);
		Component inc = new Component("INCREMENT", network, in1Nodes, in1Nodes, in1Nodes);
		components.add(inc);
		network.inputComponents.add(inc);

		InputNode initNode2 = new InputNode(network, false, new float[] {0f});
		ArrayList<Node> in2Nodes = new ArrayList<Node>();
		in2Nodes.add(initNode2);
		Component reset = new Component("RESET", network, in2Nodes, in2Nodes, in2Nodes);
		components.add(reset);
		network.inputComponents.add(reset);
		
		// add other component(s):	
		
		// Control gates for bit 0
		Component cAnd0Set = new ComponentAnd("SET BIT 0", network);
		components.add(cAnd0Set);
		network.connections.add(new Connection(network, inc.outputs.get(0), cAnd0Set.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, reset.outputs.get(0), cAnd0Set.inputs.get(1), 1, 1f));

		Component cNot0Reset = new ComponentNot("RESET BIT 0 NOT", network);
		components.add(cNot0Reset);
		network.connections.add(new Connection(network, reset.outputs.get(0), cNot0Reset.inputs.get(0), 1, 1f));

		Component cAnd0Reset = new ComponentAnd("RESET BIT 0 AND", network);
		components.add(cAnd0Reset);
		network.connections.add(new Connection(network, inc.outputs.get(0), cAnd0Reset.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, cNot0Reset.outputs.get(0), cAnd0Reset.inputs.get(1), 1, 1f));
      
		// Bit 0      
		Component bit0 = Component.loadFromFile(network, "/networks/flipflop.ng", "BIT_0");
		components.add(bit0);
		network.connections.add(new Connection(network, cAnd0Set.outputs.get(0), bit0.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, cAnd0Reset.outputs.get(0), bit0.inputs.get(1), 1, 1f));
		
		// Control gates for bit 1
		Component cAnd1Set = new ComponentAnd("SET BIT 1", network);
		components.add(cAnd1Set);
		network.connections.add(new Connection(network, inc.outputs.get(0), cAnd1Set.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, bit0.outputs.get(0), cAnd1Set.inputs.get(1), 1, 1f));

		Component cNot1Reset = new ComponentNot("RESET BIT 1 NOT", network);
		components.add(cNot1Reset);
		network.connections.add(new Connection(network, bit0.outputs.get(0), cNot1Reset.inputs.get(0), 1, 1f));

		Component cAnd1Reset = new ComponentAnd("RESET BIT 1 AND", network);
		components.add(cAnd1Reset);
		network.connections.add(new Connection(network, inc.outputs.get(0), cAnd1Reset.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, cNot1Reset.outputs.get(0), cAnd1Reset.inputs.get(1), 1, 1f));
      
		// Bit 1      
		Component bit1 = Component.loadFromFile(network, "/networks/flipflop.ng", "BIT_1");
		components.add(bit1);
		network.connections.add(new Connection(network, cAnd1Set.outputs.get(0), bit1.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, cAnd1Reset.outputs.get(0), bit1.inputs.get(1), 1, 1f));
		
		// Control gates for bit 2
		Component cAnd2Set = new ComponentAnd("SET BIT 2", network);
		components.add(cAnd2Set);
		network.connections.add(new Connection(network, inc.outputs.get(0), cAnd2Set.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, bit1.outputs.get(0), cAnd2Set.inputs.get(1), 1, 1f));

		Component cNot2Reset = new ComponentNot("RESET BIT 2 NOT", network);
		components.add(cNot2Reset);
		network.connections.add(new Connection(network, bit1.outputs.get(0), cNot2Reset.inputs.get(0), 1, 1f));

		Component cAnd2Reset = new ComponentAnd("RESET BIT 2 AND", network);
		components.add(cAnd2Reset);
		network.connections.add(new Connection(network, inc.outputs.get(0), cAnd2Reset.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, cNot2Reset.outputs.get(0), cAnd2Reset.inputs.get(1), 1, 1f));
      
		// Bit 2      
		Component bit2 = Component.loadFromFile(network, "/networks/flipflop.ng", "BIT_2");
		components.add(bit2);
		network.connections.add(new Connection(network, cAnd2Set.outputs.get(0), bit2.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, cAnd2Reset.outputs.get(0), bit2.inputs.get(1), 1, 1f));
		
		// Control gates for bit 3
		Component cAnd3Set = new ComponentAnd("SET BIT 3", network);
		components.add(cAnd3Set);
		network.connections.add(new Connection(network, inc.outputs.get(0), cAnd3Set.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, bit2.outputs.get(0), cAnd3Set.inputs.get(1), 1, 1f));

		Component cNot3Reset = new ComponentNot("RESET BIT 3 NOT", network);
		components.add(cNot3Reset);
		network.connections.add(new Connection(network, bit2.outputs.get(0), cNot3Reset.inputs.get(0), 1, 1f));

		Component cAnd3Reset = new ComponentAnd("RESET BIT 3 AND", network);
		components.add(cAnd3Reset);
		network.connections.add(new Connection(network, inc.outputs.get(0), cAnd3Reset.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, cNot3Reset.outputs.get(0), cAnd3Reset.inputs.get(1), 1, 1f));
      
		// Bit 3      
		Component bit3 = Component.loadFromFile(network, "/networks/flipflop.ng", "BIT_3");
		components.add(bit3);
		network.connections.add(new Connection(network, cAnd3Set.outputs.get(0), bit3.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, cAnd3Reset.outputs.get(0), bit3.inputs.get(1), 1, 1f));
	
		// Output components      
		Component out0 = new ComponentNode("OUT 0", network);
		components.add(out0);
		network.outputComponents = new ArrayList<Component>();
		network.outputComponents.add(out0);
		network.connections.add(new Connection(network, bit0.outputs.get(0), out0.inputs.get(0), 1, 1f));
      
		Component out1 = new ComponentNode("OUT 1", network);
		components.add(out1);
		network.outputComponents.add(out1);
		network.connections.add(new Connection(network, bit1.outputs.get(0), out1.inputs.get(0), 1, 1f));
      
		Component out2 = new ComponentNode("OUT 2", network);
		components.add(out2);
		network.outputComponents.add(out2);
		network.connections.add(new Connection(network, bit2.outputs.get(0), out2.inputs.get(0), 1, 1f));
      
		Component out3 = new ComponentNode("OUT 3", network);
		components.add(out3);
		network.outputComponents.add(out3);
		network.connections.add(new Connection(network, bit3.outputs.get(0), out3.inputs.get(0), 1, 1f));
		
		int currZ = 0;
		int currX = currZ;
		int currY = 0;
		for (Component component: components) {
			network.components.add(component);
			float width = 0.10f;
			float height = 0.10f;
			float x = currX * 0.12f + 0.01f;
			float y = currY * 0.12f + 0.01f;
			currX -= 1;
			currY += 1;
			if (currY > currZ) {
				currZ += 1;
				currX = currZ;
				currY = 0;
			}
			//System.out.println("x: " + x + ", y: " + y + ", w: " + width + ", h: " + height);
			graphicalComponents.add(new GraphicalComponent(component, x, y, width, height, Color.LIGHT_GRAY, graphicalNodes));
		}
		
		ConstraintSolver.adjustDelayLengths(this, network, network.outputComponents);
	}
	
	public void addSlackCounterComponentsEightBits() {		
		ArrayList<Component> components = new ArrayList<Component>();
		network.inputComponents = new ArrayList<Component>();
		
		// add input component(s):	
		InputNode initNode1 = new InputNode(network, false, new float[] {0f});
		ArrayList<Node> in1Nodes = new ArrayList<Node>();
		in1Nodes.add(initNode1);
		Component in = new Component("IN", network, in1Nodes, in1Nodes, in1Nodes);
		components.add(in);
		network.inputComponents.add(in);

		InputNode initNode2 = new InputNode(network, false, new float[] {0f});
		ArrayList<Node> in2Nodes = new ArrayList<Node>();
		in2Nodes.add(initNode2);
		Component reset = new Component("RESET", network, in2Nodes, in2Nodes, in2Nodes);
		components.add(reset);
		network.inputComponents.add(reset);
		
		// add other component(s):
      
		// counter 0
		Component counter0 = Component.loadFromFile(network, "/networks/slack_counter_4_bits.ng", "COUNTER_0");
		components.add(counter0);
		network.connections.add(new Connection(network, in.outputs.get(0), counter0.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, reset.outputs.get(0), counter0.inputs.get(1), 1, 1f));
      
		// counter 1
		Component counter1 = Component.loadFromFile(network, "/networks/slack_counter_4_bits.ng", "COUNTER_1");
		components.add(counter1);
		network.connections.add(new Connection(network, in.outputs.get(0), counter1.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, counter0.outputs.get(3), counter1.inputs.get(1), 1, 1f));

		//network.connections.add(new Connection(network, counter1.outputs.get(3), counter0.inputs.get(1), 1, 1f));
	
		// Output components      
		Component out0 = new ComponentNode("OUT 0", network);
		components.add(out0);
		network.outputComponents = new ArrayList<Component>();
		network.outputComponents.add(out0);
		network.connections.add(new Connection(network, counter0.outputs.get(0), out0.inputs.get(0), 1, 1f));
      
		Component out1 = new ComponentNode("OUT 1", network);
		components.add(out1);
		network.outputComponents.add(out1);
		network.connections.add(new Connection(network, counter0.outputs.get(1), out1.inputs.get(0), 1, 1f));
      
		Component out2 = new ComponentNode("OUT 2", network);
		components.add(out2);
		network.outputComponents.add(out2);
		network.connections.add(new Connection(network, counter0.outputs.get(2), out2.inputs.get(0), 1, 1f));
      
		Component out3 = new ComponentNode("OUT 3", network);
		components.add(out3);
		network.outputComponents.add(out3);
		network.connections.add(new Connection(network, counter0.outputs.get(3), out3.inputs.get(0), 1, 1f));

		Component out4 = new ComponentNode("OUT 4", network);
		components.add(out4);
		network.outputComponents.add(out4);
		network.connections.add(new Connection(network, counter1.outputs.get(0), out4.inputs.get(0), 1, 1f));
      
		Component out5 = new ComponentNode("OUT 5", network);
		components.add(out5);
		network.outputComponents.add(out5);
		network.connections.add(new Connection(network, counter1.outputs.get(1), out5.inputs.get(0), 1, 1f));
      
		Component out6 = new ComponentNode("OUT 6", network);
		components.add(out6);
		network.outputComponents.add(out6);
		network.connections.add(new Connection(network, counter1.outputs.get(2), out6.inputs.get(0), 1, 1f));
      
		Component out7 = new ComponentNode("OUT 7", network);
		components.add(out7);
		network.outputComponents.add(out7);
		network.connections.add(new Connection(network, counter1.outputs.get(3), out7.inputs.get(0), 1, 1f));
		
		int currZ = 0;
		int currX = currZ;
		int currY = 0;
		for (Component component: components) {
			network.components.add(component);
			float width = 0.10f;
			float height = 0.10f;
			float x = currX * 0.12f + 0.01f;
			float y = currY * 0.12f + 0.01f;
			currX -= 1;
			currY += 1;
			if (currY > currZ) {
				currZ += 1;
				currX = currZ;
				currY = 0;
			}
			//System.out.println("x: " + x + ", y: " + y + ", w: " + width + ", h: " + height);
			graphicalComponents.add(new GraphicalComponent(component, x, y, width, height, Color.LIGHT_GRAY, graphicalNodes));
		}
		
		ConstraintSolver.adjustDelayLengths(this, network, network.outputComponents);
	}
	
	public void addMemoryComponents() {		
		ArrayList<Component> components = new ArrayList<Component>();
		network.inputComponents = new ArrayList<Component>();
		
		// add input component(s):	
		InputNode initNodeSet = new InputNode(network, false, new float[] {0f});
		ArrayList<Node> setNodes = new ArrayList<Node>();
		setNodes.add(initNodeSet);
		Component inputSet = new Component("SET", network, setNodes, setNodes, setNodes);
		components.add(inputSet);
		network.inputComponents.add(inputSet);

		InputNode initNode0 = new InputNode(network, false, new float[] {0f});
		ArrayList<Node> in0Nodes = new ArrayList<Node>();
		in0Nodes.add(initNode0);
		Component in0 = new Component("IN 0", network, in0Nodes, in0Nodes, in0Nodes);
		components.add(in0);
		network.inputComponents.add(in0);

		InputNode initNode1 = new InputNode(network, false, new float[] {0f});
		ArrayList<Node> in1Nodes = new ArrayList<Node>();
		in1Nodes.add(initNode1);
		Component in1 = new Component("IN 1", network, in1Nodes, in1Nodes, in1Nodes);
		components.add(in1);
		network.inputComponents.add(in1);

		InputNode initNode2 = new InputNode(network, false, new float[] {0f});
		ArrayList<Node> in2Nodes = new ArrayList<Node>();
		in2Nodes.add(initNode2);
		Component in2 = new Component("IN 2", network, in2Nodes, in2Nodes, in2Nodes);
		components.add(in2);
		network.inputComponents.add(in2);

		InputNode initNode3 = new InputNode(network, false, new float[] {0f});
		ArrayList<Node> in3Nodes = new ArrayList<Node>();
		in3Nodes.add(initNode3);
		Component in3 = new Component("IN 3", network, in3Nodes, in3Nodes, in3Nodes);
		components.add(in3);
		network.inputComponents.add(in3);

		InputNode initNode4 = new InputNode(network, false, new float[] {0f});
		ArrayList<Node> in4Nodes = new ArrayList<Node>();
		in4Nodes.add(initNode4);
		Component in4 = new Component("IN 4", network, in4Nodes, in4Nodes, in4Nodes);
		components.add(in4);
		network.inputComponents.add(in4);

		InputNode initNode5 = new InputNode(network, false, new float[] {0f});
		ArrayList<Node> in5Nodes = new ArrayList<Node>();
		in5Nodes.add(initNode5);
		Component in5 = new Component("IN 5", network, in5Nodes, in5Nodes, in5Nodes);
		components.add(in5);
		network.inputComponents.add(in5);

		InputNode initNode6 = new InputNode(network, false, new float[] {0f});
		ArrayList<Node> in6Nodes = new ArrayList<Node>();
		in6Nodes.add(initNode6);
		Component in6 = new Component("IN 6", network, in6Nodes, in6Nodes, in6Nodes);
		components.add(in6);
		network.inputComponents.add(in6);

		InputNode initNode7 = new InputNode(network, false, new float[] {0f});
		ArrayList<Node> in7Nodes = new ArrayList<Node>();
		in7Nodes.add(initNode7);
		Component in7 = new Component("IN 7", network, in7Nodes, in7Nodes, in7Nodes);
		components.add(in7);
		network.inputComponents.add(in7);
		
		// add other component(s):	

		// control gates for Bit 0:
		Component setAnd0 = new ComponentAnd("SET AND 0", network);
		components.add(setAnd0);
		network.connections.add(new Connection(network, inputSet.outputs.get(0), setAnd0.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, in0.outputs.get(0), setAnd0.inputs.get(1), 1, 1f));

		Component resetNot0 = new ComponentNot("RESET NOT 0", network);
		components.add(resetNot0);
		network.connections.add(new Connection(network, in0.outputs.get(0), resetNot0.inputs.get(0), 1, 1f));

		Component resetAnd0 = new ComponentAnd("RESET AND 0", network);
		components.add(resetAnd0);
		network.connections.add(new Connection(network, inputSet.outputs.get(0), resetAnd0.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, resetNot0.outputs.get(0), resetAnd0.inputs.get(1), 1, 1f));
      
		// Bit 0      
		Component bit0 = Component.loadFromFile(network, "/networks/flipflop.ng", "BIT_0");
		components.add(bit0);
		network.connections.add(new Connection(network, setAnd0.outputs.get(0), bit0.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, resetAnd0.outputs.get(0), bit0.inputs.get(1), 1, 1f));

		// control gates for Bit 1:
		Component setAnd1 = new ComponentAnd("SET AND 1", network);
		components.add(setAnd1);
		network.connections.add(new Connection(network, inputSet.outputs.get(0), setAnd1.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, in1.outputs.get(0), setAnd1.inputs.get(1), 1, 1f));

		Component resetNot1 = new ComponentNot("RESET NOT 1", network);
		components.add(resetNot1);
		network.connections.add(new Connection(network, in1.outputs.get(0), resetNot1.inputs.get(0), 1, 1f));

		Component resetAnd1 = new ComponentAnd("RESET AND 1", network);
		components.add(resetAnd1);
		network.connections.add(new Connection(network, inputSet.outputs.get(0), resetAnd1.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, resetNot1.outputs.get(0), resetAnd1.inputs.get(1), 1, 1f));
      
		// Bit 1      
		Component bit1 = Component.loadFromFile(network, "/networks/flipflop.ng", "BIT_1");
		components.add(bit1);
		network.connections.add(new Connection(network, setAnd1.outputs.get(0), bit1.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, resetAnd1.outputs.get(0), bit1.inputs.get(1), 1, 1f));

		// control gates for Bit 2:
		Component setAnd2 = new ComponentAnd("SET AND 2", network);
		components.add(setAnd2);
		network.connections.add(new Connection(network, inputSet.outputs.get(0), setAnd2.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, in2.outputs.get(0), setAnd2.inputs.get(1), 1, 1f));

		Component resetNot2 = new ComponentNot("RESET NOT 2", network);
		components.add(resetNot2);
		network.connections.add(new Connection(network, in2.outputs.get(0), resetNot2.inputs.get(0), 1, 1f));

		Component resetAnd2 = new ComponentAnd("RESET AND 2", network);
		components.add(resetAnd2);
		network.connections.add(new Connection(network, inputSet.outputs.get(0), resetAnd2.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, resetNot2.outputs.get(0), resetAnd2.inputs.get(1), 1, 1f));
      
		// Bit 2      
		Component bit2 = Component.loadFromFile(network, "/networks/flipflop.ng", "BIT_2");
		components.add(bit2);
		network.connections.add(new Connection(network, setAnd2.outputs.get(0), bit2.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, resetAnd2.outputs.get(0), bit2.inputs.get(1), 1, 1f));

		// control gates for Bit 3:
		Component setAnd3 = new ComponentAnd("SET AND 3", network);
		components.add(setAnd3);
		network.connections.add(new Connection(network, inputSet.outputs.get(0), setAnd3.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, in3.outputs.get(0), setAnd3.inputs.get(1), 1, 1f));

		Component resetNot3 = new ComponentNot("RESET NOT 3", network);
		components.add(resetNot3);
		network.connections.add(new Connection(network, in3.outputs.get(0), resetNot3.inputs.get(0), 1, 1f));

		Component resetAnd3 = new ComponentAnd("RESET AND 3", network);
		components.add(resetAnd3);
		network.connections.add(new Connection(network, inputSet.outputs.get(0), resetAnd3.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, resetNot3.outputs.get(0), resetAnd3.inputs.get(1), 1, 1f));
      
		// Bit 3      
		Component bit3 = Component.loadFromFile(network, "/networks/flipflop.ng", "BIT_3");
		components.add(bit3);
		network.connections.add(new Connection(network, setAnd3.outputs.get(0), bit3.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, resetAnd3.outputs.get(0), bit3.inputs.get(1), 1, 1f));

		// control gates for Bit 4:
		Component setAnd4 = new ComponentAnd("SET AND 4", network);
		components.add(setAnd4);
		network.connections.add(new Connection(network, inputSet.outputs.get(0), setAnd4.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, in4.outputs.get(0), setAnd4.inputs.get(1), 1, 1f));

		Component resetNot4 = new ComponentNot("RESET NOT 4", network);
		components.add(resetNot4);
		network.connections.add(new Connection(network, in4.outputs.get(0), resetNot4.inputs.get(0), 1, 1f));

		Component resetAnd4 = new ComponentAnd("RESET AND 4", network);
		components.add(resetAnd4);
		network.connections.add(new Connection(network, inputSet.outputs.get(0), resetAnd4.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, resetNot4.outputs.get(0), resetAnd4.inputs.get(1), 1, 1f));
      
		// Bit 4      
		Component bit4 = Component.loadFromFile(network, "/networks/flipflop.ng", "BIT_4");
		components.add(bit4);
		network.connections.add(new Connection(network, setAnd4.outputs.get(0), bit4.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, resetAnd4.outputs.get(0), bit4.inputs.get(1), 1, 1f));

		// control gates for Bit 5:
		Component setAnd5 = new ComponentAnd("SET AND 5", network);
		components.add(setAnd5);
		network.connections.add(new Connection(network, inputSet.outputs.get(0), setAnd5.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, in5.outputs.get(0), setAnd5.inputs.get(1), 1, 1f));

		Component resetNot5 = new ComponentNot("RESET NOT 5", network);
		components.add(resetNot5);
		network.connections.add(new Connection(network, in5.outputs.get(0), resetNot5.inputs.get(0), 1, 1f));

		Component resetAnd5 = new ComponentAnd("RESET AND 5", network);
		components.add(resetAnd5);
		network.connections.add(new Connection(network, inputSet.outputs.get(0), resetAnd5.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, resetNot5.outputs.get(0), resetAnd5.inputs.get(1), 1, 1f));
      
		// Bit 5      
		Component bit5 = Component.loadFromFile(network, "/networks/flipflop.ng", "BIT_5");
		components.add(bit5);
		network.connections.add(new Connection(network, setAnd5.outputs.get(0), bit5.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, resetAnd5.outputs.get(0), bit5.inputs.get(1), 1, 1f));

		// control gates for Bit 6:
		Component setAnd6 = new ComponentAnd("SET AND 6", network);
		components.add(setAnd6);
		network.connections.add(new Connection(network, inputSet.outputs.get(0), setAnd6.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, in6.outputs.get(0), setAnd6.inputs.get(1), 1, 1f));

		Component resetNot6 = new ComponentNot("RESET NOT 6", network);
		components.add(resetNot6);
		network.connections.add(new Connection(network, in6.outputs.get(0), resetNot6.inputs.get(0), 1, 1f));

		Component resetAnd6 = new ComponentAnd("RESET AND 6", network);
		components.add(resetAnd6);
		network.connections.add(new Connection(network, inputSet.outputs.get(0), resetAnd6.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, resetNot6.outputs.get(0), resetAnd6.inputs.get(1), 1, 1f));
      
		// Bit 6      
		Component bit6 = Component.loadFromFile(network, "/networks/flipflop.ng", "BIT_6");
		components.add(bit6);
		network.connections.add(new Connection(network, setAnd6.outputs.get(0), bit6.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, resetAnd6.outputs.get(0), bit6.inputs.get(1), 1, 1f));

		// control gates for Bit 7:
		Component setAnd7 = new ComponentAnd("SET AND 7", network);
		components.add(setAnd7);
		network.connections.add(new Connection(network, inputSet.outputs.get(0), setAnd7.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, in7.outputs.get(0), setAnd7.inputs.get(1), 1, 1f));

		Component resetNot7 = new ComponentNot("RESET NOT 7", network);
		components.add(resetNot7);
		network.connections.add(new Connection(network, in7.outputs.get(0), resetNot7.inputs.get(0), 1, 1f));

		Component resetAnd7 = new ComponentAnd("RESET AND 7", network);
		components.add(resetAnd7);
		network.connections.add(new Connection(network, inputSet.outputs.get(0), resetAnd7.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, resetNot7.outputs.get(0), resetAnd7.inputs.get(1), 1, 1f));
      
		// Bit 7      
		Component bit7 = Component.loadFromFile(network, "/networks/flipflop.ng", "BIT_7");
		components.add(bit7);
		network.connections.add(new Connection(network, setAnd7.outputs.get(0), bit7.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, resetAnd7.outputs.get(0), bit7.inputs.get(1), 1, 1f));
	
		// Output components      
		Component out0 = new ComponentNode("OUT 0", network);
		components.add(out0);
		network.outputComponents = new ArrayList<Component>();
		network.outputComponents.add(out0);
		network.connections.add(new Connection(network, bit0.outputs.get(0), out0.inputs.get(0), 1, 1f));
      
		Component out1 = new ComponentNode("OUT 1", network);
		components.add(out1);
		network.outputComponents.add(out1);
		network.connections.add(new Connection(network, bit1.outputs.get(0), out1.inputs.get(0), 1, 1f));
      
		Component out2 = new ComponentNode("OUT 2", network);
		components.add(out2);
		network.outputComponents.add(out2);
		network.connections.add(new Connection(network, bit2.outputs.get(0), out2.inputs.get(0), 1, 1f));
      
		Component out3 = new ComponentNode("OUT 3", network);
		components.add(out3);
		network.outputComponents.add(out3);
		network.connections.add(new Connection(network, bit3.outputs.get(0), out3.inputs.get(0), 1, 1f));
      
		Component out4 = new ComponentNode("OUT 4", network);
		components.add(out4);
		network.outputComponents.add(out4);
		network.connections.add(new Connection(network, bit4.outputs.get(0), out4.inputs.get(0), 1, 1f));
      
		Component out5 = new ComponentNode("OUT 5", network);
		components.add(out5);
		network.outputComponents.add(out5);
		network.connections.add(new Connection(network, bit5.outputs.get(0), out5.inputs.get(0), 1, 1f));
      
		Component out6 = new ComponentNode("OUT 6", network);
		components.add(out6);
		network.outputComponents.add(out6);
		network.connections.add(new Connection(network, bit6.outputs.get(0), out6.inputs.get(0), 1, 1f));
      
		Component out7 = new ComponentNode("OUT 7", network);
		components.add(out7);
		network.outputComponents.add(out7);
		network.connections.add(new Connection(network, bit7.outputs.get(0), out7.inputs.get(0), 1, 1f));
		
		int currX = 0;
		int currY = 0;
		for (Component component: components) {
			network.components.add(component);
			float width = 0.08f;
			float height = 0.08f;
			float x = currX * 0.10f + 0.01f;
			float y = currY * 0.10f + 0.01f;
			currX += 1;
			if (currX > 9) {
				currY += 1;
				currX = 0;
			}
			//System.out.println("x: " + x + ", y: " + y + ", w: " + width + ", h: " + height);
			graphicalComponents.add(new GraphicalComponent(component, x, y, width, height, Color.LIGHT_GRAY, graphicalNodes));
		}
		
		ConstraintSolver.adjustDelayLengths(this, network, network.outputComponents);
	}
	
	public void addHalfCounterComponents() {		
		ArrayList<Component> components = new ArrayList<Component>();
		network.inputComponents = new ArrayList<Component>();
		
		// add input component(s):	
		InputNode initNode1 = new InputNode(network, true, new float[] {1f});
		ArrayList<Node> in1Nodes = new ArrayList<Node>();
		in1Nodes.add(initNode1);
		Component in = new Component("IN", network, in1Nodes, in1Nodes, in1Nodes);
		components.add(in);
		network.inputComponents.add(in);
		
		// add other component(s):	

		Component cAnd1 = new ComponentAnd("CONTROL AND 1", network);
		components.add(cAnd1);
		network.connections.add(new Connection(network, in.outputs.get(0), cAnd1.inputs.get(0), 1, 1f));

		Component cAnd2 = new ComponentAnd("CONTROL AND 2", network);
		components.add(cAnd2);
		network.connections.add(new Connection(network, in.outputs.get(0), cAnd2.inputs.get(0), 1, 1f));
      
		// Bit 0      
		Component bit0 = Component.loadFromFile(network, "/networks/flipflop.ng", "BIT_0");
		components.add(bit0);
		network.connections.add(new Connection(network, cAnd1.outputs.get(0), bit0.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, cAnd2.outputs.get(0), bit0.inputs.get(1), 1, 1f));
		network.connections.add(new Connection(network, bit0.outputs.get(0), cAnd2.inputs.get(1), 1, 1f));
		network.connections.add(new Connection(network, bit0.outputs.get(1), cAnd1.inputs.get(1), 1, 1f));
	
		// Output components      
		Component out0 = new ComponentNode("OUT 0", network);
		components.add(out0);
		network.outputComponents = new ArrayList<Component>();
		network.outputComponents.add(out0);
		network.connections.add(new Connection(network, bit0.outputs.get(0), out0.inputs.get(0), 1, 1f));
		
		int currZ = 0;
		int currX = currZ;
		int currY = 0;
		for (Component component: components) {
			network.components.add(component);
			float width = 0.10f;
			float height = 0.10f;
			float x = currX * 0.12f + 0.01f;
			float y = currY * 0.12f + 0.01f;
			currX -= 1;
			currY += 1;
			if (currY > currZ) {
				currZ += 1;
				currX = currZ;
				currY = 0;
			}
			//System.out.println("x: " + x + ", y: " + y + ", w: " + width + ", h: " + height);
			graphicalComponents.add(new GraphicalComponent(component, x, y, width, height, Color.LIGHT_GRAY, graphicalNodes));
		}
		
		ConstraintSolver.adjustDelayLengths(this, network, network.outputComponents);
	}
	
	public void addRecurrentCounterComponents() {		
		ArrayList<Component> components = new ArrayList<Component>();
		network.inputComponents = new ArrayList<Component>();
		
		// add input component(s):	
		InputNode initNode1 = new InputNode(network, true, new float[] {1f});
		ArrayList<Node> in1Nodes = new ArrayList<Node>();
		in1Nodes.add(initNode1);
		Component in = new Component("IN", network, in1Nodes, in1Nodes, in1Nodes);
		components.add(in);
		network.inputComponents.add(in);
		
		// add other component(s):	
		
		// Control gates for bit 0
		//Component cNot1 = new ComponentNot("CONTROL NOT 1", network);
		//components.add(cNot1);

		Component cAnd1 = new ComponentAnd("CONTROL AND 1", network);
		components.add(cAnd1);
		network.connections.add(new Connection(network, in.outputs.get(0), cAnd1.inputs.get(0), 1, 1f));

		Component cAnd2 = new ComponentAnd("CONTROL AND 2", network);
		components.add(cAnd2);
		network.connections.add(new Connection(network, in.outputs.get(0), cAnd2.inputs.get(0), 1, 1f));
      
		// Bit 0      
		Component bit0 = Component.loadFromFile(network, "/networks/flipflop.ng", "BIT_0");
		components.add(bit0);
		network.connections.add(new Connection(network, cAnd1.outputs.get(0), bit0.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, cAnd2.outputs.get(0), bit0.inputs.get(1), 1, 1f));
		network.connections.add(new Connection(network, bit0.outputs.get(0), cAnd2.inputs.get(1), 1, 1f));
		network.connections.add(new Connection(network, bit0.outputs.get(1), cAnd1.inputs.get(1), 1, 1f));
      
		// Control gates for bit 1
		Component cAnd3 = new ComponentAnd("CONTROL AND 3", network);
		components.add(cAnd3);
		network.connections.add(new Connection(network, in.outputs.get(0), cAnd3.inputs.get(0), 1, 1f));
      
		Component cAnd4 = new ComponentAnd("CONTROL AND 4", network);
		components.add(cAnd4);
		network.connections.add(new Connection(network, in.outputs.get(0), cAnd4.inputs.get(0), 1, 1f));
      
		Component cAnd5 = new ComponentAnd("CONTROL AND 5", network);
		components.add(cAnd5);
		network.connections.add(new Connection(network, bit0.outputs.get(0), cAnd5.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, cAnd3.outputs.get(0), cAnd5.inputs.get(1), 1, 1f));
      
		Component cAnd6 = new ComponentAnd("CONTROL AND 6", network);
		components.add(cAnd6);
		network.connections.add(new Connection(network, bit0.outputs.get(0), cAnd6.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, cAnd4.outputs.get(0), cAnd6.inputs.get(1), 1, 1f));
      
		// Bit 1   
		Component bit1 = Component.loadFromFile(network, "/networks/flipflop.ng", "BIT_1");
		components.add(bit1);
		network.connections.add(new Connection(network, cAnd5.outputs.get(0), bit1.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, cAnd6.outputs.get(0), bit1.inputs.get(1), 1, 1f));
		network.connections.add(new Connection(network, bit1.outputs.get(1), cAnd3.inputs.get(1), 1, 1f));
		network.connections.add(new Connection(network, bit1.outputs.get(0), cAnd4.inputs.get(1), 1, 1f));
	
		// Output components      
		Component out0 = new ComponentNode("OUT 0", network);
		components.add(out0);
		network.outputComponents = new ArrayList<Component>();
		network.outputComponents.add(out0);
		network.connections.add(new Connection(network, bit0.outputs.get(0), out0.inputs.get(0), 1, 1f));
      
		Component out1 = new ComponentNode("OUT 1", network);
		components.add(out1);
		network.outputComponents.add(out1);
		network.connections.add(new Connection(network, bit1.outputs.get(0), out1.inputs.get(0), 1, 1f));
      
		//Component out2 = new ComponentAnd("OUT", network);
		//components.add(out2);
		//network.connections.add(new Connection(network, out0.outputs.get(0), out2.inputs.get(0), 1, 1f));
		//network.connections.add(new Connection(network, out1.outputs.get(0), out2.inputs.get(1), 1, 1f));
		
		int currZ = 0;
		int currX = currZ;
		int currY = 0;
		for (Component component: components) {
			network.components.add(component);
			float width = 0.10f;
			float height = 0.10f;
			float x = currX * 0.12f + 0.01f;
			float y = currY * 0.12f + 0.01f;
			currX -= 1;
			currY += 1;
			if (currY > currZ) {
				currZ += 1;
				currX = currZ;
				currY = 0;
			}
			//System.out.println("x: " + x + ", y: " + y + ", w: " + width + ", h: " + height);
			graphicalComponents.add(new GraphicalComponent(component, x, y, width, height, Color.LIGHT_GRAY, graphicalNodes));
		}
		
		ConstraintSolver.adjustDelayLengths(this, network, network.outputComponents);
	}
	
	public void addDoubleCounterComponents() {		
		ArrayList<Component> components = new ArrayList<Component>();
		network.inputComponents = new ArrayList<Component>();
		
		// add input component(s):	
		InputNode initNode1 = new InputNode(network, true, new float[] {1f});
		ArrayList<Node> in1Nodes = new ArrayList<Node>();
		in1Nodes.add(initNode1);
		Component in = new Component("IN", network, in1Nodes, in1Nodes, in1Nodes);
		components.add(in);
		network.inputComponents.add(in);
		
		// add other component(s):	
      
		// Counter 0      
		Component counter0 = Component.loadFromFile(network, "/networks/counter.ng", "COUTNER_0");
		components.add(counter0);
		network.connections.add(new Connection(network, in.outputs.get(0), counter0.inputs.get(0), 1, 1f));
      
		// Control gates for counter 1
		Component cAnd1 = new ComponentAnd("CONTROL AND 1", network);
		components.add(cAnd1);
		network.connections.add(new Connection(network, counter0.outputs.get(0), cAnd1.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, counter0.outputs.get(1), cAnd1.inputs.get(1), 1, 1f));
      
		Component cAnd2 = new ComponentAnd("CONTROL AND 2", network);
		components.add(cAnd2);
		network.connections.add(new Connection(network, in.outputs.get(0), cAnd2.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, in.outputs.get(0), cAnd2.inputs.get(1), 1, 1f));
      
		Component cAnd3 = new ComponentAnd("CONTROL AND 3", network);
		components.add(cAnd3);
		network.connections.add(new Connection(network, cAnd1.outputs.get(0), cAnd3.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, cAnd2.outputs.get(0), cAnd3.inputs.get(1), 1, 1f));
      
		// Counter 1   
		Component counter1 = Component.loadFromFile(network, "/networks/counter.ng", "COUTNER_1");
		components.add(counter1);
		network.connections.add(new Connection(network, cAnd3.outputs.get(0), counter1.inputs.get(0), 1, 1f));
	
		// Output components     
		network.outputComponents = new ArrayList<Component>();
 
		Component out0 = new ComponentNode("OUT 0", network);
		components.add(out0);
		network.outputComponents.add(out0);
		network.connections.add(new Connection(network, counter0.outputs.get(0), out0.inputs.get(0), 1, 1f));
      
		Component out1 = new ComponentNode("OUT 1", network);
		components.add(out1);
		network.outputComponents.add(out1);
		network.connections.add(new Connection(network, counter0.outputs.get(1), out1.inputs.get(0), 1, 1f));
      
		Component out2 = new ComponentNode("OUT 2", network);
		components.add(out2);
		network.outputComponents.add(out2);
		network.connections.add(new Connection(network, counter1.outputs.get(0), out2.inputs.get(0), 1, 1f));
      
		Component out3 = new ComponentNode("OUT 3", network);
		components.add(out3);
		network.outputComponents.add(out3);
		network.connections.add(new Connection(network, counter1.outputs.get(1), out3.inputs.get(0), 1, 1f));

		/*Component out = new ComponentQuadAnd("OUT", network);
		components.add(out);
		network.connections.add(new Connection(network, out0.outputs.get(0), out.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, out1.outputs.get(0), out.inputs.get(1), 1, 1f));
		network.connections.add(new Connection(network, out2.outputs.get(0), out.inputs.get(2), 1, 1f));
		network.connections.add(new Connection(network, out3.outputs.get(0), out.inputs.get(3), 1, 1f));*/
		
		int currZ = 0;
		int currX = currZ;
		int currY = 0;
		for (Component component: components) {
			network.components.add(component);
			float width = 0.10f;
			float height = 0.10f;
			float x = currX * 0.12f + 0.01f;
			float y = currY * 0.12f + 0.01f;
			currX -= 1;
			currY += 1;
			if (currY > currZ) {
				currZ += 1;
				currX = currZ;
				currY = 0;
			}
			//System.out.println("x: " + x + ", y: " + y + ", w: " + width + ", h: " + height);
			graphicalComponents.add(new GraphicalComponent(component, x, y, width, height, Color.LIGHT_GRAY, graphicalNodes));
		}
		
		ConstraintSolver.adjustDelayLengths(this, network, network.outputComponents);
	}
	
	public void addTMazeMemoryComponents() {		
		ArrayList<Component> components = new ArrayList<Component>();
		network.inputComponents = new ArrayList<Component>();
		
		// add input component(s):	
		InputNode initNodeSet = new InputNode(network, false, new float[] {0f});
		ArrayList<Node> setNodes = new ArrayList<Node>();
		setNodes.add(initNodeSet);
		Component inputSet = new Component("SET", network, setNodes, setNodes, setNodes);
		components.add(inputSet);
		network.inputComponents.add(inputSet);

		InputNode initNode0 = new InputNode(network, false, new float[] {0f});
		ArrayList<Node> in0Nodes = new ArrayList<Node>();
		in0Nodes.add(initNode0);
		Component in0 = new Component("IN 0", network, in0Nodes, in0Nodes, in0Nodes);
		components.add(in0);
		network.inputComponents.add(in0);

		InputNode initNode1 = new InputNode(network, false, new float[] {0f});
		ArrayList<Node> in1Nodes = new ArrayList<Node>();
		in1Nodes.add(initNode1);
		Component in1 = new Component("IN 1", network, in1Nodes, in1Nodes, in1Nodes);
		components.add(in1);
		network.inputComponents.add(in1);

		InputNode initNode2 = new InputNode(network, false, new float[] {0f});
		ArrayList<Node> in2Nodes = new ArrayList<Node>();
		in2Nodes.add(initNode2);
		Component in2 = new Component("IN 2", network, in2Nodes, in2Nodes, in2Nodes);
		components.add(in2);
		network.inputComponents.add(in2);

		InputNode initNode3 = new InputNode(network, false, new float[] {0f});
		ArrayList<Node> in3Nodes = new ArrayList<Node>();
		in3Nodes.add(initNode3);
		Component in3 = new Component("IN 3", network, in3Nodes, in3Nodes, in3Nodes);
		components.add(in3);
		network.inputComponents.add(in3);

		InputNode initNode4 = new InputNode(network, false, new float[] {0f});
		ArrayList<Node> in4Nodes = new ArrayList<Node>();
		in4Nodes.add(initNode4);
		Component in4 = new Component("IN 4", network, in4Nodes, in4Nodes, in4Nodes);
		components.add(in4);
		network.inputComponents.add(in4);

		InputNode initNode5 = new InputNode(network, false, new float[] {0f});
		ArrayList<Node> in5Nodes = new ArrayList<Node>();
		in5Nodes.add(initNode5);
		Component in5 = new Component("IN 5", network, in5Nodes, in5Nodes, in5Nodes);
		components.add(in5);
		network.inputComponents.add(in5);

		InputNode initNode6 = new InputNode(network, false, new float[] {0f});
		ArrayList<Node> in6Nodes = new ArrayList<Node>();
		in6Nodes.add(initNode6);
		Component in6 = new Component("IN 6", network, in6Nodes, in6Nodes, in6Nodes);
		components.add(in6);
		network.inputComponents.add(in6);

		InputNode initNode7 = new InputNode(network, false, new float[] {0f});
		ArrayList<Node> in7Nodes = new ArrayList<Node>();
		in7Nodes.add(initNode7);
		Component in7 = new Component("IN 7", network, in7Nodes, in7Nodes, in7Nodes);
		components.add(in7);
		network.inputComponents.add(in7);

		InputNode initNodeInc = new InputNode(network, false, new float[] {0f});
		ArrayList<Node> incNodes = new ArrayList<Node>();
		incNodes.add(initNodeInc);
		Component increment = new Component("INCREMENT", network, incNodes, incNodes, incNodes);
		components.add(increment);
		network.inputComponents.add(increment);
		
		// add other component(s):	
      
		// Decoded Counter     
		Component counter = Component.loadFromFile(network, "/networks/decoded_counter.ng", "COUNTER");
		components.add(counter);
		network.connections.add(new Connection(network, increment.outputs.get(0), counter.inputs.get(0), 1, 1f));
      
		// Memory   
		Component memory = Component.loadFromFile(network, "/networks/memory.ng", "MEMORY");
		components.add(memory);
		network.connections.add(new Connection(network, inputSet.outputs.get(0), memory.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, in0.outputs.get(0), memory.inputs.get(1), 1, 1f));
		network.connections.add(new Connection(network, in1.outputs.get(0), memory.inputs.get(2), 1, 1f));
		network.connections.add(new Connection(network, in2.outputs.get(0), memory.inputs.get(3), 1, 1f));
		network.connections.add(new Connection(network, in3.outputs.get(0), memory.inputs.get(4), 1, 1f));
		network.connections.add(new Connection(network, in4.outputs.get(0), memory.inputs.get(5), 1, 1f));
		network.connections.add(new Connection(network, in5.outputs.get(0), memory.inputs.get(6), 1, 1f));
		network.connections.add(new Connection(network, in6.outputs.get(0), memory.inputs.get(7), 1, 1f));
		network.connections.add(new Connection(network, in7.outputs.get(0), memory.inputs.get(8), 1, 1f));

		// Mask memory by counter
		Component maskAnd0 = new ComponentAnd("MASK AND 0", network);
		components.add(maskAnd0);
		network.connections.add(new Connection(network, counter.outputs.get(0), maskAnd0.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, memory.outputs.get(0), maskAnd0.inputs.get(1), 1, 1f));

		Component maskAnd1 = new ComponentAnd("MASK AND 1", network);
		components.add(maskAnd1);
		network.connections.add(new Connection(network, counter.outputs.get(1), maskAnd1.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, memory.outputs.get(1), maskAnd1.inputs.get(1), 1, 1f));

		Component maskAnd2 = new ComponentAnd("MASK AND 2", network);
		components.add(maskAnd2);
		network.connections.add(new Connection(network, counter.outputs.get(2), maskAnd2.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, memory.outputs.get(2), maskAnd2.inputs.get(1), 1, 1f));

		Component maskAnd3 = new ComponentAnd("MASK AND 3", network);
		components.add(maskAnd3);
		network.connections.add(new Connection(network, counter.outputs.get(3), maskAnd3.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, memory.outputs.get(3), maskAnd3.inputs.get(1), 1, 1f));

		Component maskAnd4 = new ComponentAnd("MASK AND 4", network);
		components.add(maskAnd4);
		network.connections.add(new Connection(network, counter.outputs.get(4), maskAnd4.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, memory.outputs.get(4), maskAnd4.inputs.get(1), 1, 1f));

		Component maskAnd5 = new ComponentAnd("MASK AND 5", network);
		components.add(maskAnd5);
		network.connections.add(new Connection(network, counter.outputs.get(5), maskAnd5.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, memory.outputs.get(5), maskAnd5.inputs.get(1), 1, 1f));

		Component maskAnd6 = new ComponentAnd("MASK AND 6", network);
		components.add(maskAnd6);
		network.connections.add(new Connection(network, counter.outputs.get(6), maskAnd6.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, memory.outputs.get(6), maskAnd6.inputs.get(1), 1, 1f));

		Component maskAnd7 = new ComponentAnd("MASK AND 7", network);
		components.add(maskAnd7);
		network.connections.add(new Connection(network, counter.outputs.get(7), maskAnd7.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, memory.outputs.get(7), maskAnd7.inputs.get(1), 1, 1f));

		// Or together the bits of the result of applying the mask
		Component turnOr1 = new ComponentOr("TURN OR 1", network);
		components.add(turnOr1);
		network.connections.add(new Connection(network, maskAnd0.outputs.get(0), turnOr1.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, maskAnd1.outputs.get(0), turnOr1.inputs.get(1), 1, 1f));

		Component turnOr2 = new ComponentOr("TURN OR 2", network);
		components.add(turnOr2);
		network.connections.add(new Connection(network, turnOr1.outputs.get(0), turnOr2.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, maskAnd2.outputs.get(0), turnOr2.inputs.get(1), 1, 1f));

		Component turnOr3 = new ComponentOr("TURN OR 3", network);
		components.add(turnOr3);
		network.connections.add(new Connection(network, turnOr2.outputs.get(0), turnOr3.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, maskAnd3.outputs.get(0), turnOr3.inputs.get(1), 1, 1f));

		Component turnOr4 = new ComponentOr("TURN OR 4", network);
		components.add(turnOr4);
		network.connections.add(new Connection(network, turnOr3.outputs.get(0), turnOr4.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, maskAnd4.outputs.get(0), turnOr4.inputs.get(1), 1, 1f));

		Component turnOr5 = new ComponentOr("TURN OR 5", network);
		components.add(turnOr5);
		network.connections.add(new Connection(network, turnOr4.outputs.get(0), turnOr5.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, maskAnd5.outputs.get(0), turnOr5.inputs.get(1), 1, 1f));

		Component turnOr6 = new ComponentOr("TURN OR 6", network);
		components.add(turnOr6);
		network.connections.add(new Connection(network, turnOr5.outputs.get(0), turnOr6.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, maskAnd6.outputs.get(0), turnOr6.inputs.get(1), 1, 1f));

		Component turnOr7 = new ComponentOr("TURN OR 7", network);
		components.add(turnOr7);
		network.connections.add(new Connection(network, turnOr6.outputs.get(0), turnOr7.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, maskAnd7.outputs.get(0), turnOr7.inputs.get(1), 1, 1f));
		
	
		// Output components     
		network.outputComponents = new ArrayList<Component>();

		Component out0 = new ComponentNode("OUT 0", network);
		components.add(out0);
		network.outputComponents.add(out0);
		network.connections.add(new Connection(network, turnOr7.outputs.get(0), out0.inputs.get(0), 1, 1f));
		
		int currZ = 0;
		int currX = currZ;
		int currY = 0;
		for (Component component: components) {
			network.components.add(component);
			float width = 0.10f;
			float height = 0.10f;
			float x = currX * 0.12f + 0.01f;
			float y = currY * 0.12f + 0.01f;
			currX -= 1;
			currY += 1;
			if (currY > currZ) {
				currZ += 1;
				currX = currZ;
				currY = 0;
			}
			//System.out.println("x: " + x + ", y: " + y + ", w: " + width + ", h: " + height);
			graphicalComponents.add(new GraphicalComponent(component, x, y, width, height, Color.LIGHT_GRAY, graphicalNodes));
		}
		
		ConstraintSolver.adjustDelayLengths(this, network, network.outputComponents);
	}
	
	public void addTMazeComponents() {		
		ArrayList<Component> components = new ArrayList<Component>();
		network.inputComponents = new ArrayList<Component>();
		
		// add input component(s):	
		InputNode initNodeSet = new InputNode(network, false, new float[] {0f});
		ArrayList<Node> setNodes = new ArrayList<Node>();
		setNodes.add(initNodeSet);
		Component inputSet = new Component("SET", network, setNodes, setNodes, setNodes);
		components.add(inputSet);
		network.inputComponents.add(inputSet);

		InputNode initNode0 = new InputNode(network, false, new float[] {0f});
		ArrayList<Node> in0Nodes = new ArrayList<Node>();
		in0Nodes.add(initNode0);
		Component in0 = new Component("IN 0", network, in0Nodes, in0Nodes, in0Nodes);
		components.add(in0);
		network.inputComponents.add(in0);

		InputNode initNode1 = new InputNode(network, false, new float[] {0f});
		ArrayList<Node> in1Nodes = new ArrayList<Node>();
		in1Nodes.add(initNode1);
		Component in1 = new Component("IN 1", network, in1Nodes, in1Nodes, in1Nodes);
		components.add(in1);
		network.inputComponents.add(in1);

		InputNode initNode2 = new InputNode(network, false, new float[] {0f});
		ArrayList<Node> in2Nodes = new ArrayList<Node>();
		in2Nodes.add(initNode2);
		Component in2 = new Component("IN 2", network, in2Nodes, in2Nodes, in2Nodes);
		components.add(in2);
		network.inputComponents.add(in2);

		InputNode initNode3 = new InputNode(network, false, new float[] {0f});
		ArrayList<Node> in3Nodes = new ArrayList<Node>();
		in3Nodes.add(initNode3);
		Component in3 = new Component("IN 3", network, in3Nodes, in3Nodes, in3Nodes);
		components.add(in3);
		network.inputComponents.add(in3);

		InputNode initNode4 = new InputNode(network, false, new float[] {0f});
		ArrayList<Node> in4Nodes = new ArrayList<Node>();
		in4Nodes.add(initNode4);
		Component in4 = new Component("IN 4", network, in4Nodes, in4Nodes, in4Nodes);
		components.add(in4);
		network.inputComponents.add(in4);

		InputNode initNode5 = new InputNode(network, false, new float[] {0f});
		ArrayList<Node> in5Nodes = new ArrayList<Node>();
		in5Nodes.add(initNode5);
		Component in5 = new Component("IN 5", network, in5Nodes, in5Nodes, in5Nodes);
		components.add(in5);
		network.inputComponents.add(in5);

		InputNode initNode6 = new InputNode(network, false, new float[] {0f});
		ArrayList<Node> in6Nodes = new ArrayList<Node>();
		in6Nodes.add(initNode6);
		Component in6 = new Component("IN 6", network, in6Nodes, in6Nodes, in6Nodes);
		components.add(in6);
		network.inputComponents.add(in6);

		InputNode initNode7 = new InputNode(network, false, new float[] {0f});
		ArrayList<Node> in7Nodes = new ArrayList<Node>();
		in7Nodes.add(initNode7);
		Component in7 = new Component("IN 7", network, in7Nodes, in7Nodes, in7Nodes);
		components.add(in7);
		network.inputComponents.add(in7);

		InputNode initNodeJunc = new InputNode(network, false, new float[] {0f});
		ArrayList<Node> juncNodes = new ArrayList<Node>();
		juncNodes.add(initNodeJunc);
		Component inputJunction = new Component("JUNCTION", network, juncNodes, juncNodes, juncNodes);
		components.add(inputJunction);
		network.inputComponents.add(inputJunction);
		
		// add other component(s):	
      
		// T-maze memory
		Component tMemory = Component.loadFromFile(network, "/networks/tmaze_memory.ng", "TMAZE MEMORY");
		components.add(tMemory);
		network.connections.add(new Connection(network, inputSet.outputs.get(0), tMemory.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, in0.outputs.get(0), tMemory.inputs.get(1), 1, 1f));
		network.connections.add(new Connection(network, in1.outputs.get(0), tMemory.inputs.get(2), 1, 1f));
		network.connections.add(new Connection(network, in2.outputs.get(0), tMemory.inputs.get(3), 1, 1f));
		network.connections.add(new Connection(network, in3.outputs.get(0), tMemory.inputs.get(4), 1, 1f));
		network.connections.add(new Connection(network, in4.outputs.get(0), tMemory.inputs.get(5), 1, 1f));
		network.connections.add(new Connection(network, in5.outputs.get(0), tMemory.inputs.get(6), 1, 1f));
		network.connections.add(new Connection(network, in6.outputs.get(0), tMemory.inputs.get(7), 1, 1f));
		network.connections.add(new Connection(network, in7.outputs.get(0), tMemory.inputs.get(8), 1, 1f));
		network.connections.add(new Connection(network, inputJunction.outputs.get(0), tMemory.inputs.get(9), 1, 1f));

		// junction delay
		Component[] junctionDelays = new Component[1];
		Component lastJunctionDelay = inputJunction;
		for (int i = 0; i < junctionDelays.length; ++i) {
			junctionDelays[i] = new ComponentNode("JUNCTION DELAY " + i, network);
			components.add(junctionDelays[i]);
			network.connections.add(new Connection(network, lastJunctionDelay.outputs.get(0), junctionDelays[i].inputs.get(0), 1, 1f));
			Component junctionDelayCycle = new ComponentNode("JDC " + i, network);
			components.add(junctionDelayCycle);
			network.connections.add(new Connection(network, junctionDelays[0].outputs.get(0), junctionDelayCycle.inputs.get(0), 1, 0f));
			network.connections.add(new Connection(network, junctionDelayCycle.outputs.get(0), junctionDelays[0].inputs.get(0), 1, 0f));
			lastJunctionDelay = junctionDelays[i];
		}

		// process memory output
		Component leftAnd = new ComponentAnd("LEFT AND", network);
		components.add(leftAnd);
		network.connections.add(new Connection(network, lastJunctionDelay.outputs.get(0), leftAnd.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, tMemory.outputs.get(0), leftAnd.inputs.get(1), 1, 1f));

		Component notLeft = new ComponentNot("NOT LEFT", network);
		components.add(notLeft);
		network.connections.add(new Connection(network, tMemory.outputs.get(0), notLeft.inputs.get(0), 1, 1f));

		Component rightAnd = new ComponentAnd("RIGHT AND", network);
		components.add(rightAnd);
		network.connections.add(new Connection(network, lastJunctionDelay.outputs.get(0), rightAnd.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, notLeft.outputs.get(0), rightAnd.inputs.get(1), 1, 1f));
	
		// Output components     
		network.outputComponents = new ArrayList<Component>();

		Component out0 = new ComponentNode("TURN LEFT", network);
		components.add(out0);
		network.outputComponents.add(out0);
		network.connections.add(new Connection(network, leftAnd.outputs.get(0), out0.inputs.get(0), 1, 1f));

		Component out1 = new ComponentNode("TURN RIGHT", network);
		components.add(out1);
		network.outputComponents.add(out1);
		network.connections.add(new Connection(network, rightAnd.outputs.get(0), out1.inputs.get(0), 1, 1f));
		
		int currZ = 0;
		int currX = currZ;
		int currY = 0;
		for (Component component: components) {
			network.components.add(component);
			float width = 0.10f;
			float height = 0.10f;
			float x = currX * 0.12f + 0.01f;
			float y = currY * 0.12f + 0.01f;
			currX -= 1;
			currY += 1;
			if (currY > currZ) {
				currZ += 1;
				currX = currZ;
				currY = 0;
			}
			//System.out.println("x: " + x + ", y: " + y + ", w: " + width + ", h: " + height);
			graphicalComponents.add(new GraphicalComponent(component, x, y, width, height, Color.LIGHT_GRAY, graphicalNodes));
		}
		
		ConstraintSolver.adjustDelayLengths(this, network, network.outputComponents);
	}
	
	public void addDecodedCounterComponents() {		
		ArrayList<Component> components = new ArrayList<Component>();
		network.inputComponents = new ArrayList<Component>();
		
		// add input component(s):	
		InputNode initNode1 = new InputNode(network, true, new float[] {1f});
		ArrayList<Node> in1Nodes = new ArrayList<Node>();
		in1Nodes.add(initNode1);
		Component in = new Component("IN", network, in1Nodes, in1Nodes, in1Nodes);
		components.add(in);
		network.inputComponents.add(in);
		
		// add other component(s):	
      
		// Counter 0      
		Component counter0 = Component.loadFromFile(network, "/networks/counter.ng", "COUNTER_0");
		components.add(counter0);
		network.connections.add(new Connection(network, in.outputs.get(0), counter0.inputs.get(0), 1, 1f));
      
		// Control gates for counter 1
		Component cAnd1 = new ComponentAnd("CONTROL AND 1", network);
		components.add(cAnd1);
		network.connections.add(new Connection(network, counter0.outputs.get(0), cAnd1.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, counter0.outputs.get(1), cAnd1.inputs.get(1), 1, 1f));
      
		Component cAnd2 = new ComponentAnd("CONTROL AND 2", network);
		components.add(cAnd2);
		network.connections.add(new Connection(network, in.outputs.get(0), cAnd2.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, in.outputs.get(0), cAnd2.inputs.get(1), 1, 1f));
      
		Component cAnd3 = new ComponentAnd("CONTROL AND 3", network);
		components.add(cAnd3);
		network.connections.add(new Connection(network, cAnd1.outputs.get(0), cAnd3.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, cAnd2.outputs.get(0), cAnd3.inputs.get(1), 1, 1f));
      
		// Counter 1   
		Component counter1 = Component.loadFromFile(network, "/networks/half_counter.ng", "HALF COUNTER 1");
		components.add(counter1);
		network.connections.add(new Connection(network, cAnd3.outputs.get(0), counter1.inputs.get(0), 1, 1f));

		// Not gates for counters:      
		Component cNot0 = new ComponentNot("COUNTER NOT 0", network);
		components.add(cNot0);
		network.connections.add(new Connection(network, counter0.outputs.get(0), cNot0.inputs.get(0), 1, 1f));

		Component cNot1 = new ComponentNot("COUNTER NOT 1", network);
		components.add(cNot1);
		network.connections.add(new Connection(network, counter0.outputs.get(1), cNot1.inputs.get(0), 1, 1f));

		Component cNot2 = new ComponentNot("COUNTER NOT 2", network);
		components.add(cNot2);
		network.connections.add(new Connection(network, counter1.outputs.get(0), cNot2.inputs.get(0), 1, 1f));

		// Individual decoded bits (they don't store data, just display it)
		Component value0 = new ComponentTriAnd("VALUE 0", network);
		components.add(value0);
		network.connections.add(new Connection(network, cNot0.outputs.get(0), value0.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, cNot1.outputs.get(0), value0.inputs.get(1), 1, 1f));
		network.connections.add(new Connection(network, cNot2.outputs.get(0), value0.inputs.get(2), 1, 1f));

		Component value1 = new ComponentTriAnd("VALUE 1", network);
		components.add(value1);
		network.connections.add(new Connection(network, counter0.outputs.get(0), value1.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, cNot1.outputs.get(0), value1.inputs.get(1), 1, 1f));
		network.connections.add(new Connection(network, cNot2.outputs.get(0), value1.inputs.get(2), 1, 1f));

		Component value2 = new ComponentTriAnd("VALUE 2", network);
		components.add(value2);
		network.connections.add(new Connection(network, cNot0.outputs.get(0), value2.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, counter0.outputs.get(1), value2.inputs.get(1), 1, 1f));
		network.connections.add(new Connection(network, cNot2.outputs.get(0), value2.inputs.get(2), 1, 1f));

		Component value3 = new ComponentTriAnd("VALUE 3", network);
		components.add(value3);
		network.connections.add(new Connection(network, counter0.outputs.get(0), value3.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, counter0.outputs.get(1), value3.inputs.get(1), 1, 1f));
		network.connections.add(new Connection(network, cNot2.outputs.get(0), value3.inputs.get(2), 1, 1f));

		Component value4 = new ComponentTriAnd("VALUE 4", network);
		components.add(value4);
		network.connections.add(new Connection(network, cNot0.outputs.get(0), value4.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, cNot1.outputs.get(0), value4.inputs.get(1), 1, 1f));
		network.connections.add(new Connection(network, counter1.outputs.get(0), value4.inputs.get(2), 1, 1f));

		Component value5 = new ComponentTriAnd("VALUE 5", network);
		components.add(value5);
		network.connections.add(new Connection(network, counter0.outputs.get(0), value5.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, cNot1.outputs.get(0), value5.inputs.get(1), 1, 1f));
		network.connections.add(new Connection(network, counter1.outputs.get(0), value5.inputs.get(2), 1, 1f));

		Component value6 = new ComponentTriAnd("VALUE 6", network);
		components.add(value6);
		network.connections.add(new Connection(network, cNot0.outputs.get(0), value6.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, counter0.outputs.get(1), value6.inputs.get(1), 1, 1f));
		network.connections.add(new Connection(network, counter1.outputs.get(0), value6.inputs.get(2), 1, 1f));

		Component value7 = new ComponentTriAnd("VALUE 7", network);
		components.add(value7);
		network.connections.add(new Connection(network, counter0.outputs.get(0), value7.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, counter0.outputs.get(1), value7.inputs.get(1), 1, 1f));
		network.connections.add(new Connection(network, counter1.outputs.get(0), value7.inputs.get(2), 1, 1f));
		
	
		// Output components     
		network.outputComponents = new ArrayList<Component>();

		Component out0 = new ComponentNode("OUT 0", network);
		components.add(out0);
		network.outputComponents.add(out0);
		network.connections.add(new Connection(network, value0.outputs.get(0), out0.inputs.get(0), 1, 1f));
      
		Component out1 = new ComponentNode("OUT 1", network);
		components.add(out1);
		network.outputComponents.add(out1);
		network.connections.add(new Connection(network, value1.outputs.get(0), out1.inputs.get(0), 1, 1f));
      
		Component out2 = new ComponentNode("OUT 2", network);
		components.add(out2);
		network.outputComponents.add(out2);
		network.connections.add(new Connection(network, value2.outputs.get(0), out2.inputs.get(0), 1, 1f));
      
		Component out3 = new ComponentNode("OUT 3", network);
		components.add(out3);
		network.outputComponents.add(out3);
		network.connections.add(new Connection(network, value3.outputs.get(0), out3.inputs.get(0), 1, 1f));
      
		Component out4 = new ComponentNode("OUT 4", network);
		components.add(out4);
		network.outputComponents.add(out4);
		network.connections.add(new Connection(network, value4.outputs.get(0), out4.inputs.get(0), 1, 1f));
      
		Component out5 = new ComponentNode("OUT 5", network);
		components.add(out5);
		network.outputComponents.add(out5);
		network.connections.add(new Connection(network, value5.outputs.get(0), out5.inputs.get(0), 1, 1f));
      
		Component out6 = new ComponentNode("OUT 6", network);
		components.add(out6);
		network.outputComponents.add(out6);
		network.connections.add(new Connection(network, value6.outputs.get(0), out6.inputs.get(0), 1, 1f));
      
		Component out7 = new ComponentNode("OUT 7", network);
		components.add(out7);
		network.outputComponents.add(out7);
		network.connections.add(new Connection(network, value7.outputs.get(0), out7.inputs.get(0), 1, 1f));
		
		int currZ = 0;
		int currX = currZ;
		int currY = 0;
		for (Component component: components) {
			network.components.add(component);
			float width = 0.10f;
			float height = 0.10f;
			float x = currX * 0.12f + 0.01f;
			float y = currY * 0.12f + 0.01f;
			currX -= 1;
			currY += 1;
			if (currY > currZ) {
				currZ += 1;
				currX = currZ;
				currY = 0;
			}
			//System.out.println("x: " + x + ", y: " + y + ", w: " + width + ", h: " + height);
			graphicalComponents.add(new GraphicalComponent(component, x, y, width, height, Color.LIGHT_GRAY, graphicalNodes));
		}
		
		ConstraintSolver.adjustDelayLengths(this, network, network.outputComponents);
	}
	
	public void addQuadCounterComponents() {		
		ArrayList<Component> components = new ArrayList<Component>();
		network.inputComponents = new ArrayList<Component>();
		
		// add input component(s):	
		InputNode initNode1 = new InputNode(network, true, new float[] {1f});
		ArrayList<Node> in1Nodes = new ArrayList<Node>();
		in1Nodes.add(initNode1);
		Component in = new Component("IN", network, in1Nodes, in1Nodes, in1Nodes);
		components.add(in);
		network.inputComponents.add(in);
		
		// add other component(s):	
      
		// Counter 0      
		Component counter0 = Component.loadFromFile(network, "/networks/doublecounter.ng", "DOUBLE_COUTNER_0");
		components.add(counter0);
		network.connections.add(new Connection(network, in.outputs.get(0), counter0.inputs.get(0), 1, 1f));
      
		// Control gates for counter 1
		Component cAnd1 = new ComponentAnd("CONTROL AND 1", network);
		components.add(cAnd1);
		network.connections.add(new Connection(network, counter0.outputs.get(0), cAnd1.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, counter0.outputs.get(1), cAnd1.inputs.get(1), 1, 1f));
      
		Component cAnd2 = new ComponentAnd("CONTROL AND 2", network);
		components.add(cAnd2);
		network.connections.add(new Connection(network, in.outputs.get(0), cAnd2.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, in.outputs.get(0), cAnd2.inputs.get(1), 1, 1f));
      
		Component cAnd3 = new ComponentAnd("CONTROL AND 3", network);
		components.add(cAnd3);
		network.connections.add(new Connection(network, counter0.outputs.get(2), cAnd3.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, counter0.outputs.get(3), cAnd3.inputs.get(1), 1, 1f));
      
		Component cAnd4 = new ComponentAnd("CONTROL AND 4", network);
		components.add(cAnd4);
		network.connections.add(new Connection(network, cAnd1.outputs.get(0), cAnd4.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, cAnd3.outputs.get(0), cAnd4.inputs.get(1), 1, 1f));
      
		Component cAnd5 = new ComponentAnd("CONTROL AND 5", network);
		components.add(cAnd5);
		network.connections.add(new Connection(network, cAnd2.outputs.get(0), cAnd5.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, cAnd4.outputs.get(0), cAnd5.inputs.get(1), 1, 1f));
      
		// Counter 1   
		Component counter1 = Component.loadFromFile(network, "/networks/doublecounter.ng", "DOUBLE_COUTNER_1");
		components.add(counter1);
		network.connections.add(new Connection(network, cAnd5.outputs.get(0), counter1.inputs.get(0), 1, 1f));
	
		// Output components     
		network.outputComponents = new ArrayList<Component>();
 
		Component out0 = new ComponentNode("OUT 0", network);
		components.add(out0);
		network.outputComponents.add(out0);
		network.connections.add(new Connection(network, counter0.outputs.get(0), out0.inputs.get(0), 1, 1f));
      
		Component out1 = new ComponentNode("OUT 1", network);
		components.add(out1);
		network.outputComponents.add(out1);
		network.connections.add(new Connection(network, counter0.outputs.get(1), out1.inputs.get(0), 1, 1f));
      
		Component out2 = new ComponentNode("OUT 2", network);
		components.add(out2);
		network.outputComponents.add(out2);
		network.connections.add(new Connection(network, counter0.outputs.get(2), out2.inputs.get(0), 1, 1f));
      
		Component out3 = new ComponentNode("OUT 3", network);
		components.add(out3);
		network.outputComponents.add(out3);
		network.connections.add(new Connection(network, counter0.outputs.get(3), out3.inputs.get(0), 1, 1f));
 
		Component out4 = new ComponentNode("OUT 4", network);
		components.add(out4);
		network.outputComponents.add(out4);
		network.connections.add(new Connection(network, counter1.outputs.get(0), out4.inputs.get(0), 1, 1f));
      
		Component out5 = new ComponentNode("OUT 5", network);
		components.add(out5);
		network.outputComponents.add(out5);
		network.connections.add(new Connection(network, counter1.outputs.get(1), out5.inputs.get(0), 1, 1f));
      
		Component out6 = new ComponentNode("OUT 6", network);
		components.add(out6);
		network.outputComponents.add(out6);
		network.connections.add(new Connection(network, counter1.outputs.get(2), out6.inputs.get(0), 1, 1f));
      
		Component out7 = new ComponentNode("OUT 7", network);
		components.add(out7);
		network.outputComponents.add(out7);
		network.connections.add(new Connection(network, counter1.outputs.get(3), out7.inputs.get(0), 1, 1f));

		/*Component out = new ComponentQuadAnd("OUT", network);
		components.add(out);
		network.connections.add(new Connection(network, out0.outputs.get(0), out.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, out1.outputs.get(0), out.inputs.get(1), 1, 1f));
		network.connections.add(new Connection(network, out2.outputs.get(0), out.inputs.get(2), 1, 1f));
		network.connections.add(new Connection(network, out3.outputs.get(0), out.inputs.get(3), 1, 1f));*/
		
		int currZ = 0;
		int currX = currZ;
		int currY = 0;
		for (Component component: components) {
			network.components.add(component);
			float width = 0.10f;
			float height = 0.10f;
			float x = currX * 0.12f + 0.01f;
			float y = currY * 0.12f + 0.01f;
			currX -= 1;
			currY += 1;
			if (currY > currZ) {
				currZ += 1;
				currX = currZ;
				currY = 0;
			}
			//System.out.println("x: " + x + ", y: " + y + ", w: " + width + ", h: " + height);
			graphicalComponents.add(new GraphicalComponent(component, x, y, width, height, Color.LIGHT_GRAY, graphicalNodes));
		}
		
		ConstraintSolver.adjustDelayLengths(this, network, network.outputComponents);
	}
	
	public void addCounterComponents() {		
		ArrayList<Component> components = new ArrayList<Component>();
		network.inputComponents = new ArrayList<Component>();
		
		// add input component(s):	
		InputNode initNode1 = new InputNode(network, false, new float[] {0f});
		ArrayList<Node> in1Nodes = new ArrayList<Node>();
		in1Nodes.add(initNode1);
		Component in = new Component("IN", network, in1Nodes, in1Nodes, in1Nodes);
		components.add(in);
		network.inputComponents.add(in);

		// INTERNAL CLOCK		
		Component clock1 = new ComponentDelay("CLOCK 1", network, 1);
		components.add(clock1);
		network.connections.add(new Connection(network, in.outputs.get(0), clock1.inputs.get(0), 1, 1f));
		
		Component clock2 = new ComponentDelay("CLOCK 2", network, 1);
		components.add(clock2);
		network.connections.add(new Connection(network, clock1.outputs.get(0), clock2.inputs.get(0), 1, 1f));
		
		Component clock3= new ComponentDelay("CLOCK 3", network, 1);
		components.add(clock3);
		network.connections.add(new Connection(network, clock2.outputs.get(0), clock3.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, clock3.outputs.get(0), clock1.inputs.get(0), 1, 1f));
		
		// add other component(s):	
		
		// Control gates for bit 0
		Component cAnd1 = new ComponentAnd("CONTROL AND 1", network);
		components.add(cAnd1);
		network.connections.add(new Connection(network, clock3.outputs.get(0), cAnd1.inputs.get(0), 1, 1f));

		Component cAnd2 = new ComponentAnd("CONTROL AND 2", network);
		components.add(cAnd2);
		network.connections.add(new Connection(network, clock3.outputs.get(0), cAnd2.inputs.get(0), 1, 1f));
      
		// Bit 0      
		Component b0Not = new ComponentNot("BIT 0 NOT", network);
		components.add(b0Not);
		network.connections.add(new Connection(network, cAnd2.outputs.get(0), b0Not.inputs.get(0), 1, 1f));
      
		Component b0Or = new ComponentOr("BIT 0 OR", network);
		components.add(b0Or);
		network.connections.add(new Connection(network, cAnd1.outputs.get(0), b0Or.inputs.get(0), 1 , 1f));
      
		Component b0And = new ComponentAnd("BIT 0 AND", network);
		components.add(b0And);
		network.connections.add(new Connection(network, b0Not.outputs.get(0), b0And.inputs.get(0), 1 , 1f));
		network.connections.add(new Connection(network, b0Or.outputs.get(0), b0And.inputs.get(1), 1 , 1f));
      
		Component b0Q = new ComponentOr("BIT 0 Q", network); // effectively just a node, since it ors something with itself
		components.add(b0Q);
		network.connections.add(new Connection(network, b0And.outputs.get(0), b0Q.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, b0And.outputs.get(0), b0Q.inputs.get(1), 1, 1f));
		network.connections.add(new Connection(network, b0Q.outputs.get(0), cAnd2.inputs.get(1), 1, 1f));
		network.connections.add(new Connection(network, b0Q.outputs.get(0), b0Or.inputs.get(1), 1 , 1f));
      
		Component b0QBar = new ComponentNot("BIT 0 QBAR", network);
		components.add(b0QBar);
		network.connections.add(new Connection(network, b0And.outputs.get(0), b0QBar.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, b0QBar.outputs.get(0), cAnd1.inputs.get(1), 1, 1f));
      
		// Control gates for bit 1
		Component cAnd3 = new ComponentAnd("CONTROL AND 3", network);
		components.add(cAnd3);
		network.connections.add(new Connection(network, clock3.outputs.get(0), cAnd3.inputs.get(0), 1, 1f));
      
		Component cAnd4 = new ComponentAnd("CONTROL AND 4", network);
		components.add(cAnd4);
		network.connections.add(new Connection(network, clock3.outputs.get(0), cAnd4.inputs.get(0), 1, 1f));
      
		Component cAnd5 = new ComponentAnd("CONTROL AND 5", network);
		components.add(cAnd5);
		network.connections.add(new Connection(network, b0Q.outputs.get(0), cAnd5.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, cAnd3.outputs.get(0), cAnd5.inputs.get(1), 1, 1f));
      
		Component cAnd6 = new ComponentAnd("CONTROL AND 6", network);
		components.add(cAnd6);
		network.connections.add(new Connection(network, b0Q.outputs.get(0), cAnd6.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, cAnd4.outputs.get(0), cAnd6.inputs.get(1), 1, 1f));
      
		// Bit 1   
		Component b1Not = new ComponentNot("BIT 1 NOT", network);
		components.add(b1Not);
		network.connections.add(new Connection(network, cAnd6.outputs.get(0), b1Not.inputs.get(0), 1, 1f));
      
		Component b1Or = new ComponentOr("BIT 1 OR", network);
		components.add(b1Or);
		network.connections.add(new Connection(network, cAnd5.outputs.get(0), b1Or.inputs.get(0), 1 , 1f));
      
		Component b1And = new ComponentAnd("BIT 1 AND", network);
		components.add(b1And);
		network.connections.add(new Connection(network, b1Not.outputs.get(0), b1And.inputs.get(0), 1 , 1f));
		network.connections.add(new Connection(network, b1Or.outputs.get(0), b1And.inputs.get(1), 1 , 1f));
      
		Component b1Q = new ComponentOr("BIT 1 Q", network); // effectively just a node, since it ors something with itself
		components.add(b1Q);
		network.connections.add(new Connection(network, b1And.outputs.get(0), b1Q.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, b1And.outputs.get(0), b1Q.inputs.get(1), 1, 1f));
		network.connections.add(new Connection(network, b1Q.outputs.get(0), cAnd4.inputs.get(1), 1, 1f));
		network.connections.add(new Connection(network, b1Q.outputs.get(0), b1Or.inputs.get(1), 1 , 1f));
      
		Component b1QBar = new ComponentNot("BIT 1 QBAR", network);
		components.add(b1QBar);
		network.connections.add(new Connection(network, b1And.outputs.get(0), b1QBar.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, b1QBar.outputs.get(0), cAnd3.inputs.get(1), 1, 1f));
	
		// Output components      
		Component out0 = new ComponentNode("OUT 0", network);
		components.add(out0);
		network.connections.add(new Connection(network, b0Q.outputs.get(0), out0.inputs.get(0), 1, 1f));
      
		Component out1 = new ComponentNode("OUT 1", network);
		components.add(out1);
		network.connections.add(new Connection(network, b1Q.outputs.get(0), out1.inputs.get(0), 1, 1f));
      
		Component out2 = new ComponentAnd("OUT", network);
		components.add(out2);
		network.connections.add(new Connection(network, out0.outputs.get(0), out2.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, out1.outputs.get(0), out2.inputs.get(1), 1, 1f));
		
		int currZ = 0;
		int currX = currZ;
		int currY = 0;
		for (Component component: components) {
			network.components.add(component);
			float width = 0.10f;
			float height = 0.10f;
			float x = currX * 0.12f + 0.01f;
			float y = currY * 0.12f + 0.01f;
			currX -= 1;
			currY += 1;
			if (currY > currZ) {
				currZ += 1;
				currX = currZ;
				currY = 0;
			}
			//System.out.println("x: " + x + ", y: " + y + ", w: " + width + ", h: " + height);
			graphicalComponents.add(new GraphicalComponent(component, x, y, width, height, Color.LIGHT_GRAY, graphicalNodes));
		}
		
		ConstraintSolver.adjustDelayLengths(this, network);
	}
	
	public void addNibbleComponents() {		
		ArrayList<Component> components = new ArrayList<Component>();

		ArrayList<Component> outputComponents = new ArrayList<Component>();
		
		// add input component(s):	
		InputNode initNode1 = new InputNode(network, false, new float[] {1f});
		ArrayList<Node> in1Nodes = new ArrayList<Node>();
		in1Nodes.add(initNode1);
		Component clockInput = new Component("CLOCK INPUT", network, in1Nodes, in1Nodes, in1Nodes);
		components.add(clockInput);
		
		InputNode initNode2 = new InputNode(network, true, new float[] {1f});
		ArrayList<Node> in2Nodes = new ArrayList<Node>();
		in2Nodes.add(initNode2);
		Component incrementInput = new Component("INCREMENT INPUT", network, in2Nodes, in2Nodes, in2Nodes);
		components.add(incrementInput);
		
		InputNode initNode3 = new InputNode(network, false, new float[] {0f});
		ArrayList<Node> in3Nodes = new ArrayList<Node>();
		in3Nodes.add(initNode3);
		Component valueInput = new Component("VALUE INPUT", network, in3Nodes, in3Nodes, in3Nodes);
		components.add(valueInput);
		
		InputNode initNode4 = new InputNode(network, false, new float[] {0f});
		ArrayList<Node> in4Nodes = new ArrayList<Node>();
		in4Nodes.add(initNode4);
		Component value0Input = new Component("VALUE 2^0 INPUT", network, in4Nodes, in4Nodes, in4Nodes);
		components.add(value0Input);
		
		InputNode initNode5 = new InputNode(network, false, new float[] {0f});
		ArrayList<Node> in5Nodes = new ArrayList<Node>();
		in5Nodes.add(initNode5);
		Component value1Input = new Component("VALUE 2^1 INPUT", network, in5Nodes, in5Nodes, in5Nodes);
		components.add(value1Input);
		
		InputNode initNode6 = new InputNode(network, false, new float[] {0f});
		ArrayList<Node> in6Nodes = new ArrayList<Node>();
		in6Nodes.add(initNode6);
		Component value2Input = new Component("VALUE 2^2 INPUT", network, in6Nodes, in6Nodes, in6Nodes);
		components.add(value2Input);
		
		InputNode initNode7 = new InputNode(network, false, new float[] {0f});
		ArrayList<Node> in7Nodes = new ArrayList<Node>();
		in7Nodes.add(initNode7);
		Component value3Input = new Component("VALUE 2^3 INPUT", network, in7Nodes, in7Nodes, in7Nodes);
		components.add(value3Input);
		
		
		// add other component(s):
		
		// INTERNAL CLOCK		
		Component clock1 = new ComponentDelay("CLOCK 1", network, 1);
		components.add(clock1);
		network.connections.add(new Connection(network, clockInput.outputs.get(0), clock1.inputs.get(0), 1, 1f));
		
		Component clock2 = new ComponentDelay("CLOCK 2", network, 1);
		components.add(clock2);
		network.connections.add(new Connection(network, clock1.outputs.get(0), clock2.inputs.get(0), 1, 1f));
		
		Component clock3= new ComponentDelay("CLOCK 3", network, 1);
		components.add(clock3);
		network.connections.add(new Connection(network, clock2.outputs.get(0), clock3.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, clock3.outputs.get(0), clock1.inputs.get(0), 1, 1f));
		
		// Only increment when signal to increment matches internal clock
		Component allowInc = new ComponentAnd("ALLOW INCREMENT", network);
		components.add(allowInc);
		network.connections.add(new Connection(network, clock3.outputs.get(0), allowInc.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, incrementInput.outputs.get(0), allowInc.inputs.get(1), 1, 1f));
		
		// INVERTED VALUES OF VALUE INPUTS
		Component not0 = new ComponentNot("NOT 0", network);
		components.add(not0);
		network.connections.add(new Connection(network, value0Input.outputs.get(0), not0.inputs.get(0), 1, 1f));
		
		Component not1 = new ComponentNot("NOT 1", network);
		components.add(not1);
		network.connections.add(new Connection(network, value1Input.outputs.get(0), not1.inputs.get(0), 1, 1f));
		
		Component not2 = new ComponentNot("NOT 2", network);
		components.add(not2);
		network.connections.add(new Connection(network, value2Input.outputs.get(0), not2.inputs.get(0), 1, 1f));
		
		Component not3 = new ComponentNot("NOT 3", network);
		components.add(not3);
		network.connections.add(new Connection(network, value3Input.outputs.get(0), not3.inputs.get(0), 1, 1f));
		
		// components for setting stored value to specific value
		Component setValue0 = new ComponentTriAnd("SET VALUE 0", network);
		components.add(setValue0);
		network.connections.add(new Connection(network, clock3.outputs.get(0), setValue0.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, valueInput.outputs.get(0), setValue0.inputs.get(1), 1, 1f));
		network.connections.add(new Connection(network, value0Input.outputs.get(0), setValue0.inputs.get(2), 1, 1f));
		
		Component resetValue0 = new ComponentTriAnd("RESET VALUE 0", network);
		components.add(resetValue0);
		network.connections.add(new Connection(network, clock3.outputs.get(0), resetValue0.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, valueInput.outputs.get(0), resetValue0.inputs.get(1), 1, 1f));
		network.connections.add(new Connection(network, not0.outputs.get(0), resetValue0.inputs.get(2), 1, 1f));
		
		Component setValue1 = new ComponentTriAnd("SET VALUE 1", network);
		components.add(setValue1);
		network.connections.add(new Connection(network, clock3.outputs.get(0), setValue1.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, valueInput.outputs.get(0), setValue1.inputs.get(1), 1, 1f));
		network.connections.add(new Connection(network, value1Input.outputs.get(0), setValue1.inputs.get(2), 1, 1f));
		
		Component resetValue1 = new ComponentTriAnd("RESET VALUE 1", network);
		components.add(resetValue1);
		network.connections.add(new Connection(network, clock3.outputs.get(0), resetValue1.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, valueInput.outputs.get(0), resetValue1.inputs.get(1), 1, 1f));
		network.connections.add(new Connection(network, not1.outputs.get(0), resetValue1.inputs.get(2), 1, 1f));
		
		Component setValue2 = new ComponentTriAnd("SET VALUE 2", network);
		components.add(setValue2);
		network.connections.add(new Connection(network, clock3.outputs.get(0), setValue2.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, valueInput.outputs.get(0), setValue2.inputs.get(1), 1, 1f));
		network.connections.add(new Connection(network, value2Input.outputs.get(0), setValue2.inputs.get(2), 1, 1f));
		
		Component resetValue2 = new ComponentTriAnd("RESET VALUE 2", network);
		components.add(resetValue2);
		network.connections.add(new Connection(network, clock3.outputs.get(0), resetValue2.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, valueInput.outputs.get(0), resetValue2.inputs.get(1), 1, 1f));
		network.connections.add(new Connection(network, not2.outputs.get(0), resetValue2.inputs.get(2), 1, 1f));
		
		Component setValue3 = new ComponentTriAnd("SET VALUE 3", network);
		components.add(setValue3);
		network.connections.add(new Connection(network, clock3.outputs.get(0), setValue3.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, valueInput.outputs.get(0), setValue3.inputs.get(1), 1, 1f));
		network.connections.add(new Connection(network, value3Input.outputs.get(0), setValue3.inputs.get(2), 1, 1f));
		
		Component resetValue3 = new ComponentTriAnd("RESET VALUE 3", network);
		components.add(resetValue3);
		network.connections.add(new Connection(network, clock3.outputs.get(0), resetValue3.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, valueInput.outputs.get(0), resetValue3.inputs.get(1), 1, 1f));
		network.connections.add(new Connection(network, not3.outputs.get(0), resetValue3.inputs.get(2), 1, 1f));
		
		// BIT 1
		Component and1 = new ComponentAnd("AND 1", network);
		components.add(and1);
		network.connections.add(new Connection(network, allowInc.outputs.get(0), and1.inputs.get(0), 1, 1f));
		
		Component and2 = new ComponentAnd("AND 2", network);
		components.add(and2);
		network.connections.add(new Connection(network, allowInc.outputs.get(0), and2.inputs.get(0), 1, 1f));
		
		Component flipFlop1 = Component.loadFromFile(network, "/networks/flipflop.ng", "FLIP FLOP 1");
		components.add(flipFlop1);		
		network.connections.add(new Connection(network, and1.outputs.get(0), flipFlop1.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, and2.outputs.get(0), flipFlop1.inputs.get(1), 1, 1f));
		network.connections.add(new Connection(network, flipFlop1.outputs.get(1), and1.inputs.get(1), 1, 1f));
		network.connections.add(new Connection(network, flipFlop1.outputs.get(0), and2.inputs.get(1), 1, 1f));
		network.connections.add(new Connection(network, setValue0.outputs.get(0), flipFlop1.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, resetValue0.outputs.get(0), flipFlop1.inputs.get(1), 1, 1f));
		
		// BIT 2
		Component and3 = new ComponentTriAnd("AND 3", network);
		components.add(and3);
		network.connections.add(new Connection(network, allowInc.outputs.get(0), and3.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, flipFlop1.outputs.get(0), and3.inputs.get(1), 1, 1f));
		
		Component and4 = new ComponentTriAnd("AND 4", network);
		components.add(and4);
		network.connections.add(new Connection(network, allowInc.outputs.get(0), and4.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, flipFlop1.outputs.get(0), and4.inputs.get(1), 1, 1f));
				
		Component flipFlop2 = Component.loadFromFile(network, "/networks/flipflop.ng", "FLIP FLOP 2");
		components.add(flipFlop2);		
		network.connections.add(new Connection(network, and3.outputs.get(0), flipFlop2.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, and4.outputs.get(0), flipFlop2.inputs.get(1), 1, 1f));
		network.connections.add(new Connection(network, flipFlop2.outputs.get(1), and3.inputs.get(2), 1, 1f));
		network.connections.add(new Connection(network, flipFlop2.outputs.get(0), and4.inputs.get(2), 1, 1f));
		network.connections.add(new Connection(network, setValue1.outputs.get(0), flipFlop2.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, resetValue1.outputs.get(0), flipFlop2.inputs.get(1), 1, 1f));
		
		// BIT 3
		Component and5 = new ComponentQuadAnd("AND 5", network);
		components.add(and5);
		network.connections.add(new Connection(network, allowInc.outputs.get(0), and5.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, flipFlop1.outputs.get(0), and5.inputs.get(1), 1, 1f));
		network.connections.add(new Connection(network, flipFlop2.outputs.get(0), and5.inputs.get(2), 1, 1f));
		
		Component and6 = new ComponentQuadAnd("AND 6", network);
		components.add(and6);
		network.connections.add(new Connection(network, allowInc.outputs.get(0), and6.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, flipFlop1.outputs.get(0), and6.inputs.get(1), 1, 1f));
		network.connections.add(new Connection(network, flipFlop2.outputs.get(0), and6.inputs.get(2), 1, 1f));
		
		Component flipFlop3 = Component.loadFromFile(network, "/networks/flipflop.ng", "FLIP FLOP 3");
		components.add(flipFlop3);		
		network.connections.add(new Connection(network, and5.outputs.get(0), flipFlop3.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, and6.outputs.get(0), flipFlop3.inputs.get(1), 1, 1f));
		network.connections.add(new Connection(network, flipFlop3.outputs.get(1), and5.inputs.get(3), 1, 1f));
		network.connections.add(new Connection(network, flipFlop3.outputs.get(0), and6.inputs.get(3), 1, 1f));
		network.connections.add(new Connection(network, setValue2.outputs.get(0), flipFlop3.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, resetValue2.outputs.get(0), flipFlop3.inputs.get(1), 1, 1f));
		
		// BIT 4
		Component and7 = new ComponentQuinAnd("AND 7", network);
		components.add(and7);
		network.connections.add(new Connection(network, allowInc.outputs.get(0), and7.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, flipFlop1.outputs.get(0), and7.inputs.get(1), 1, 1f));
		network.connections.add(new Connection(network, flipFlop2.outputs.get(0), and7.inputs.get(2), 1, 1f));
		network.connections.add(new Connection(network, flipFlop3.outputs.get(0), and7.inputs.get(3), 1, 1f));
		
		Component and8 = new ComponentQuinAnd("AND 8", network);
		components.add(and8);
		network.connections.add(new Connection(network, allowInc.outputs.get(0), and8.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, flipFlop1.outputs.get(0), and8.inputs.get(1), 1, 1f));
		network.connections.add(new Connection(network, flipFlop2.outputs.get(0), and8.inputs.get(2), 1, 1f));
		network.connections.add(new Connection(network, flipFlop3.outputs.get(0), and8.inputs.get(3), 1, 1f));
		
		Component flipFlop4 = Component.loadFromFile(network, "/networks/flipflop.ng", "FLIP FLOP 4");
		components.add(flipFlop4);		
		network.connections.add(new Connection(network, and7.outputs.get(0), flipFlop4.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, and8.outputs.get(0), flipFlop4.inputs.get(1), 1, 1f));
		network.connections.add(new Connection(network, flipFlop4.outputs.get(1), and7.inputs.get(4), 1, 1f));
		network.connections.add(new Connection(network, flipFlop4.outputs.get(0), and8.inputs.get(4), 1, 1f));
		network.connections.add(new Connection(network, setValue3.outputs.get(0), flipFlop4.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, resetValue3.outputs.get(0), flipFlop4.inputs.get(1), 1, 1f));
				
		// add output components:
		
		// temporary fix to static lengths of components being calculated incorrectly from different output nodes
		// make a final component that takes all outputs as input and cut it out of the ng file
		Component outputs = new ComponentQuadAnd("OUTPUTS", network);
		components.add(outputs);
		network.connections.add(new Connection(network, flipFlop1.outputs.get(0), outputs.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, flipFlop2.outputs.get(0), outputs.inputs.get(1), 1, 1f));
		network.connections.add(new Connection(network, flipFlop3.outputs.get(0), outputs.inputs.get(2), 1, 1f));
		network.connections.add(new Connection(network, flipFlop4.outputs.get(0), outputs.inputs.get(3), 1, 1f));
		outputComponents.add(outputs);
		
		int currZ = 0;
		int currX = currZ;
		int currY = 0;
		for (Component component: components) {
			network.components.add(component);
			float width = 0.10f;
			float height = 0.10f;
			float x = currX * 0.12f + 0.01f;
			float y = currY * 0.12f + 0.01f;
			currX -= 1;
			currY += 1;
			if (currY > currZ) {
				currZ += 1;
				currX = currZ;
				currY = 0;
			}
			//System.out.println("x: " + x + ", y: " + y + ", w: " + width + ", h: " + height);
			graphicalComponents.add(new GraphicalComponent(component, x, y, width, height, Color.LIGHT_GRAY, graphicalNodes));
		}
		
		ConstraintSolver.adjustDelayLengths(this, network, outputComponents);
	}
	
	/*public void addNibbleComponents() {		
		ArrayList<Component> components = new ArrayList<Component>();
		
		// add input component(s):	
		InputNode initNode1 = new InputNode(network, false, new float[] {1f});
		ArrayList<Node> in1Nodes = new ArrayList<Node>();
		in1Nodes.add(initNode1);
		Component input1 = new Component("In 1", network, in1Nodes, in1Nodes, in1Nodes);
		components.add(input1);
		
		// add other component(s):
		
		// clock
		Component clock1 = new ComponentDelay("CLOCK 1", network, 1);
		components.add(clock1);
		network.connections.add(new Connection(network, input1.outputs.get(0), clock1.inputs.get(0), 1, 1f));
		
		Component clock2 = new ComponentDelay("CLOCK 2", network, 1);
		components.add(clock2);
		network.connections.add(new Connection(network, clock1.outputs.get(0), clock2.inputs.get(0), 1, 1f));
		
		Component clock3= new ComponentDelay("CLOCK 3", network, 1);
		components.add(clock3);
		network.connections.add(new Connection(network, clock2.outputs.get(0), clock3.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, clock3.outputs.get(0), clock1.inputs.get(0), 1, 1f));
		
		
		// BIT 1
		Component and1 = new ComponentAnd("AND 1", network);
		components.add(and1);
		network.connections.add(new Connection(network, clock3.outputs.get(0), and1.inputs.get(0), 1, 1f));
		
		Component and2 = new ComponentAnd("AND 2", network);
		components.add(and2);
		network.connections.add(new Connection(network, clock3.outputs.get(0), and2.inputs.get(0), 1, 1f));
		
		Component flipFlop1 = Component.flipFlop(network, "FLIP FLOP 1");
		components.add(flipFlop1);		
		network.connections.add(new Connection(network, and1.outputs.get(0), flipFlop1.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, and2.outputs.get(0), flipFlop1.inputs.get(1), 1, 1f));
		network.connections.add(new Connection(network, flipFlop1.outputs.get(1), and1.inputs.get(1), 1, 1f));
		network.connections.add(new Connection(network, flipFlop1.outputs.get(0), and2.inputs.get(1), 1, 1f));
		test1 = flipFlop1.outputs.get(0);
		
		// BIT 2
		Component and3 = new ComponentTriAnd("AND 3", network);
		components.add(and3);
		network.connections.add(new Connection(network, clock3.outputs.get(0), and3.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, flipFlop1.outputs.get(0), and3.inputs.get(1), 1, 1f));
		
		Component and4 = new ComponentTriAnd("AND 4", network);
		components.add(and4);
		network.connections.add(new Connection(network, clock3.outputs.get(0), and4.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, flipFlop1.outputs.get(0), and4.inputs.get(1), 1, 1f));
				
		Component flipFlop2 = Component.flipFlop(network, "FLIP FLOP 2");
		components.add(flipFlop2);		
		network.connections.add(new Connection(network, and3.outputs.get(0), flipFlop2.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, and4.outputs.get(0), flipFlop2.inputs.get(1), 1, 1f));
		network.connections.add(new Connection(network, flipFlop2.outputs.get(1), and3.inputs.get(2), 1, 1f));
		network.connections.add(new Connection(network, flipFlop2.outputs.get(0), and4.inputs.get(2), 1, 1f));
		test2 = flipFlop2.outputs.get(0);		
		
		// BIT 3
		Component and5 = new ComponentQuadAnd("AND 5", network);
		components.add(and5);
		network.connections.add(new Connection(network, clock3.outputs.get(0), and5.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, flipFlop1.outputs.get(0), and5.inputs.get(1), 1, 1f));
		network.connections.add(new Connection(network, flipFlop2.outputs.get(0), and5.inputs.get(2), 1, 1f));
		
		Component and6 = new ComponentQuadAnd("AND 6", network);
		components.add(and6);
		network.connections.add(new Connection(network, clock3.outputs.get(0), and6.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, flipFlop1.outputs.get(0), and6.inputs.get(1), 1, 1f));
		network.connections.add(new Connection(network, flipFlop2.outputs.get(0), and6.inputs.get(2), 1, 1f));
		
		Component flipFlop3 = Component.flipFlop(network, "FLIP FLOP 3");
		components.add(flipFlop3);		
		network.connections.add(new Connection(network, and5.outputs.get(0), flipFlop3.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, and6.outputs.get(0), flipFlop3.inputs.get(1), 1, 1f));
		network.connections.add(new Connection(network, flipFlop3.outputs.get(1), and5.inputs.get(3), 1, 1f));
		network.connections.add(new Connection(network, flipFlop3.outputs.get(0), and6.inputs.get(3), 1, 1f));
		test3 = flipFlop3.outputs.get(0);
		
		// BIT 4
		Component and7 = new ComponentQuinAnd("AND 7", network);
		components.add(and7);
		network.connections.add(new Connection(network, clock3.outputs.get(0), and7.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, flipFlop1.outputs.get(0), and7.inputs.get(1), 1, 1f));
		network.connections.add(new Connection(network, flipFlop2.outputs.get(0), and7.inputs.get(2), 1, 1f));
		network.connections.add(new Connection(network, flipFlop3.outputs.get(0), and7.inputs.get(3), 1, 1f));
		
		Component and8 = new ComponentQuinAnd("AND 8", network);
		components.add(and8);
		network.connections.add(new Connection(network, clock3.outputs.get(0), and8.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, flipFlop1.outputs.get(0), and8.inputs.get(1), 1, 1f));
		network.connections.add(new Connection(network, flipFlop2.outputs.get(0), and8.inputs.get(2), 1, 1f));
		network.connections.add(new Connection(network, flipFlop3.outputs.get(0), and8.inputs.get(3), 1, 1f));
		
		Component flipFlop4 = Component.flipFlop(network, "FLIP FLOP 4");
		components.add(flipFlop4);		
		network.connections.add(new Connection(network, and7.outputs.get(0), flipFlop4.inputs.get(0), 1, 1f));
		network.connections.add(new Connection(network, and8.outputs.get(0), flipFlop4.inputs.get(1), 1, 1f));
		network.connections.add(new Connection(network, flipFlop4.outputs.get(1), and7.inputs.get(4), 1, 1f));
		network.connections.add(new Connection(network, flipFlop4.outputs.get(0), and8.inputs.get(4), 1, 1f));
		test4 = flipFlop4.outputs.get(0);
				
		// add output component:
		Node finalNode = new Neuron(network, 1.0f);
		ArrayList<Node> outNodes = new ArrayList<Node>();
		outNodes.add(finalNode);
		Component output = new Component("Out", network, outNodes, outNodes, outNodes);
		components.add(output);
		
		int delayBeforeOutput = 5;
		network.connections.add(new Connection(network, flipFlop4.outputs.get(0), finalNode, delayBeforeOutput, 1f));
		
		int currZ = 0;
		int currX = currZ;
		int currY = 0;
		for (Component component: components) {
			network.components.add(component);
			float width = 0.14f;
			float height = 0.14f;
			float x = currX * 0.16f + 0.01f;
			float y = currY * 0.16f + 0.01f;
			currX -= 1;
			currY += 1;
			if (currY > currZ) {
				currZ += 1;
				currX = currZ;
				currY = 0;
			}
			//System.out.println("x: " + x + ", y: " + y + ", w: " + width + ", h: " + height);
			graphicalComponents.add(new GraphicalComponent(component, x, y, width, height, Color.LIGHT_GRAY, graphicalNodes));
		}
		
		ConstraintSolver.adjustDelayLengths(this, network);
	}*/
}