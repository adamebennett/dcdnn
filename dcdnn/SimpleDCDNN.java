package dcdnn;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.ArrayList;

public class SimpleDCDNN extends JFrame {

	public Network network;
	public ArrayList<GraphicalNode> graphicalNodes;
	public ArrayList<GraphicalComponent> graphicalComponents;
	public View view;
	public Timer timer;
	public JTextField commandLine;
	public JTextArea textArea;
	public boolean paused;

	public SimpleDCDNN() {
		super("DCDNN");
		setDefaultCloseOperation(EXIT_ON_CLOSE); // allow window to close
		setSize(500, 500);	
										
		graphicalNodes = new ArrayList<GraphicalNode>();
		graphicalComponents = new ArrayList<GraphicalComponent>();
		paused = true;
		
		view = new View(500, 500, network, graphicalNodes, graphicalComponents);	
		
		// Add a timer
		timer = new Timer(250, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				handleTimerTick();
			}
		});
		timer.start();
		
		commandLine = new JTextField();
		commandLine.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				handleCommand();
			}
		});
		
		JPanel textPanel = new JPanel();
		textPanel.setLayout(new BorderLayout());
		textArea = new JTextArea();
		textArea.setEditable(false);
		textArea.append("Enter a command (enter '?' for a list of commands)");		
		textPanel.add(commandLine, BorderLayout.PAGE_END);
		JScrollPane scrollPane = new JScrollPane(textArea);
		textPanel.add(scrollPane, BorderLayout.CENTER);
		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, view, textPanel);
		splitPane.setDividerLocation(350);
		this.getContentPane().add(splitPane);
	}
	
	/*public static void main(String[] args) {
		SimpleDCDNN dcdnn = new SimpleDCDNN();
		dcdnn.setVisible(true);
		dcdnn.commandLine.requestFocus();
	}*/
	
	private void handleTimerTick()
	{
		if (!paused) {
			step();
		}
		view.refresh((int)view.getSize().getWidth(), (int)view.getSize().getHeight());
	}
	
	private void step() {
		if (network != null) {
			network.step();
		}
		view.refresh((int)view.getSize().getWidth(), (int)view.getSize().getHeight());
	}
	
	private void handleCommand() {
		String commandString = new String(commandLine.getText());
		String[] tokens = commandString.split(" ");
		String command = tokens[0];
		commandLine.setText("");
		if (command.equals("?")) {
			textArea.append("\n\nAvailable commands:");
			textArea.append("\n  '?' - Display the list of available commands");
			textArea.append("\n  'run' - Runs the current SNN");
			textArea.append("\n  'pause' - Pauses the currently running SNN");
			textArea.append("\n  'step' or 's' - Runs the current SNN for one time step");
			textArea.append("\n  'disorg' - Generates a disorganized SNN");
			textArea.append("\n  'export' - Exports the current network to a file");
		}
		else if (command.equals("run")) {
			if (paused) {
				paused = false;
				textArea.append("\n\nRunning");
			}
			else {
				textArea.append("\n\nNetwork is already running");
			}
		}
		else if (command.equals("pause")) {
			if (!paused) {
				paused = true;
				textArea.append("\n\nPaused");
			}
			else {
				textArea.append("\n\nNetwork is already paused");
			}
		}
		else if (command.equals("step") || command.equals("s")) {
			if (paused) {
				if (tokens.length == 1) {
					textArea.append("\n\nRunning 1 time step...");
					step();
					textArea.append("\nDone");
				}
				else if (tokens.length == 2) {
					int numSteps = Integer.parseInt(tokens[1]);
					textArea.append("\n\nRunning " + numSteps + " time step(s)...");
					for (int i = 0; i < numSteps; ++i) {
						step();
					}
					textArea.append("\nDone");
				}
				else {
					textArea.append("\n\nError: Step command takes 1 optional argument (number of steps)");
				}
			}
			else {
				textArea.append("\n\nError: Network must be paused");
			}
		}
		else if (command.equals("disorg")) {
			textArea.append("\n\nGenerating disorganized SNN...");
			network = new Network(50, 2f, 2f);
			view.network = network;
			addInputNodes();
			textArea.append("\nDone");
		}
		else if (command.equals("export")) {
			if (network == null) {
				textArea.append("\n\nError: No network to export");
			}
			else {
				textArea.append("\n\nExporting to file...");
				boolean result = DCDNNIO.exportAsNetwork(network, "network.nn");
				if (result) {
					textArea.append("\nDone");
				}
				else {
					textArea.append("\nAn error was encountered while attempting to export the network.");
				}
			}
		}
		else {
			textArea.append("\n\nCommand not recognized. Enter '?' for a list of available commands.");
		}
		textArea.setCaretPosition(textArea.getDocument().getLength());
	}
	
	public void addOutput(String s) {
		textArea.append(s);
		textArea.setCaretPosition(textArea.getDocument().getLength());
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
}