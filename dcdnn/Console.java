package dcdnn;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.ArrayList;

public class Console extends JPanel implements MyAppendable {

	public DCDNN dcdnn;
	public JTextField commandLine;
	public JTextArea textArea;

	public Console(DCDNN d) {
		dcdnn = d;
		setLayout(new BorderLayout());
		Panel textPanel = new Panel();
		textPanel.setLayout(new BorderLayout());
		commandLine = new JTextField();
		commandLine.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				handleCommand();
			}
		});
		textArea = new JTextArea();
		textArea.setEditable(false);
		textArea.append("Enter a command (enter \"?\" for a list of commands)");		
		textPanel.add(commandLine, BorderLayout.PAGE_END);
		JScrollPane scrollPane = new JScrollPane(textArea);
		textPanel.add(scrollPane, BorderLayout.CENTER);
		add(textPanel, BorderLayout.CENTER);
	}

	@Override
	public void append(String s) {
		textArea.append(s);
		textArea.setCaretPosition(textArea.getDocument().getLength());
	}
	
	private void handleCommand() {
		String commandString = new String(commandLine.getText());
		textArea.append("\n\n> " + commandString);
		String[] tokens = commandString.split(" ");
		String command = tokens[0];
		commandLine.setText("");
		if (command.equals("?")) {
			textArea.append("\n\nAvailable commands:");
			textArea.append("\n  \"?\" - Display the list of available commands");
			textArea.append("\n  \"run\" - Runs the current SNN");
			textArea.append("\n  \"pause\" - Pauses the currently running SNN");
			textArea.append("\n  \"step <x>\" or \"s <x>\" - Runs the current SNN for one time step. Include a number as <x> argument to step x times instead of once.");
			textArea.append("\n  \"record <all>\" - Records a spike plot of the currently running network (include \"all\" as <all> argument to record all neurons, otherwise only inputs and outputs are recorded)");
			textArea.append("\n  \"stop\" - Stops recording the currently running SNN");
			textArea.append("\n  \"input <inputs>\" - inputs the values in <inputs>  into the network");
			textArea.append("\n  \"export\" - Exports the current network to a file");
			textArea.append("\n  \"disorg\" - Generates a disorganized SNN");
			textArea.append("\n  \"xor\" - Generates an XOR SNN");
			textArea.append("\n  \"flipflop\" - Generates a Flip Flop circuit");
			textArea.append("\n  \"memory\" - Generates an 8 bit register");
			textArea.append("\n  \"counter\" - Generates a 2 bit counter");
			textArea.append("\n  \"doublecounter\" - Generates a 4 bit counter");
			textArea.append("\n  \"quadcounter\" - Generates a 8 bit counter");
			textArea.append("\n  \"decodedcounter\" - Generates a 3 bit counter with one-hot output");
			textArea.append("\n  \"tmazememory\" - Generates memory circuit for T-Maze solver");
			textArea.append("\n  \"tmaze\" - Generates robot controller for T-Maze problem");
			//textArea.append("\n  \"learn <fsm>\" - Attempts to train a network to behave like the fsm specified in the fsm argument");
			textArea.append("\n  \"clockify\" - Adds internal clock to network and ANDs it with inputs");
			textArea.append("\n  \"pseudoclockify\" - Initializes all internal clocks but doesn't add a global clock");
		}
		else if (command.equals("run")) {
			if (dcdnn.paused) {
				dcdnn.paused = false;
				textArea.append("\nRunning");
			}
			else {
				textArea.append("\nNetwork is already running");
			}
		}
		else if (command.equals("pause")) {
			if (!dcdnn.paused) {
				dcdnn.paused = true;
				textArea.append("\nPaused");
			}
			else {
				textArea.append("\nNetwork is already paused");
			}
		}
		else if (command.equals("record")) {
			boolean all = false;
			if (dcdnn.paused) {
				if (!dcdnn.recording) {
					if (tokens.length == 2) {
						if (tokens[1].equals("all")) {
							all = true;
						}
					}
					dcdnn.startRecording(all);
					textArea.append("\nRecording");
				}
				else {
					textArea.append("\nNetwork is already recording");
				}
			}
			else {
				textArea.append("\nError: Network must be paused");
			}
		}
		else if (command.equals("stop")) {
			if (dcdnn.recording) {
				dcdnn.recording = false;
				textArea.append("\nStopped Recording");
			}
			else {
				textArea.append("\nNetwork isn't recording");
			}
		}
		else if (command.equals("step") || command.equals("s")) {
			if (dcdnn.paused) {
				if (tokens.length == 1) {
					textArea.append("\nRunning 1 time step...");
					dcdnn.step();
					textArea.append("\nDone");
				}
				else if (tokens.length == 2) {
					int numSteps = Integer.parseInt(tokens[1]);
					textArea.append("\nRunning " + numSteps + " time step(s)...");
					for (int i = 0; i < numSteps; ++i) {
						dcdnn.step();
					}
					textArea.append("\nDone");
				}
				else {
					textArea.append("\nError: step command takes 1 optional argument (number of steps)");
				}
			}
			else {
				textArea.append("\nError: Network must be paused");
			}
		}
		else if (command.equals("input")) {
			if (tokens.length-1 != dcdnn.network.inputComponents.size()) {
				textArea.append("\nError: <inputs> must contain the same number of arguments as network has inputs");
			}
			else {
				textArea.append("\nSetting inputs...");
				float[] inputs = new float[tokens.length-1];
				for (int i = 1; i < tokens.length; ++i) {
					inputs[i-1] = Float.parseFloat(tokens[i]);
				}
				dcdnn.setInputs(inputs);
				textArea.append("\nDone");
			}
		}
		else if (command.equals("disorg")) {
			textArea.append("\nGenerating disorganized SNN...");
			dcdnn.network = new Network(50, 2f, 2f);
			dcdnn.view.network = dcdnn.network;
			dcdnn.addInputNodes();
			textArea.append("\nDone");
		}
		else if (command.equals("xor")) {
			textArea.append("\nGenerating XOR SNN...");
			dcdnn.network = new Network(0, 0);
			dcdnn.view.network = dcdnn.network;
			dcdnn.addXORComponents();
			textArea.append("\nDone");
		}
		else if (command.equals("flipflop")) {
			textArea.append("\nGenerating flip flop network...");
			dcdnn.network = new Network(0, 0);
			dcdnn.view.network = dcdnn.network;
			dcdnn.addFlipFlopComponents();
			textArea.append("\nDone");
		}
		else if (command.equals("halfcounter")) {
			textArea.append("\nGenerating halfcounter network...");
			dcdnn.network = new Network(0, 0);
			dcdnn.view.network = dcdnn.network;
			dcdnn.addHalfCounterComponents();
			textArea.append("\nDone");
		}
		else if (command.equals("counter")) {
			textArea.append("\nGenerating counter network...");
			dcdnn.network = new Network(0, 0);
			dcdnn.view.network = dcdnn.network;
			dcdnn.addRecurrentCounterComponents();
			textArea.append("\nDone");
		}
		else if (command.equals("doublecounter")) {
			textArea.append("\nGenerating doublecounter network...");
			dcdnn.network = new Network(0, 0);
			dcdnn.view.network = dcdnn.network;
			dcdnn.addDoubleCounterComponents();
			textArea.append("\nDone");
		}
		else if (command.equals("decodedcounter")) {
			textArea.append("\nGenerating decoded counter network...");
			dcdnn.network = new Network(0, 0);
			dcdnn.view.network = dcdnn.network;
			dcdnn.addDecodedCounterComponents();
			textArea.append("\nDone");
		}
		else if (command.equals("quadcounter")) {
			textArea.append("\nGenerating quadcounter network...");
			dcdnn.network = new Network(0, 0);
			dcdnn.view.network = dcdnn.network;
			dcdnn.addQuadCounterComponents();
			textArea.append("\nDone");
		}
		else if (command.equals("memory")) {
			textArea.append("\nGenerating memory network...");
			dcdnn.network = new Network(0, 0);
			dcdnn.view.network = dcdnn.network;
			dcdnn.addMemoryComponents();
			textArea.append("\nDone");
		}
		else if (command.equals("nibble")) {
			textArea.append("\nGenerating SNN which functions as a nibble...");
			dcdnn.network = new Network(0, 0);
			dcdnn.view.network = dcdnn.network;
			dcdnn.addNibbleComponents();
			textArea.append("\nDone");
		}
		else if (command.equals("tmazememory")) {
			textArea.append("\nGenerating memory module for T-Maze problem...");
			dcdnn.network = new Network(0, 0);
			dcdnn.view.network = dcdnn.network;
			dcdnn.addTMazeMemoryComponents();
			textArea.append("\nDone");
		}
		else if (command.equals("tmaze")) {
			textArea.append("\nGenerating robot controller for T-Maze problem...");
			dcdnn.network = new Network(0, 0);
			dcdnn.view.network = dcdnn.network;
			dcdnn.addTMazeComponents();
			textArea.append("\nDone");
		}
		else if (command.equals("export")) {
			if (dcdnn.network == null) {
				textArea.append("\nError: No network to export");
			}
			else {
				textArea.append("\nExporting to file...");
				boolean result = DCDNNIO.exportAsNetwork(dcdnn.network, "networks/network.nn");
				if (result) {
					textArea.append("\nDone");
				}
				else {
					textArea.append("\nAn error was encountered while attempting to export the network.");
				}
			}
		}
		else if (command.equals("learn")) {
			if (dcdnn.paused) {
				if (tokens.length == 1) {
					textArea.append("\nError: A finite state machine must be specified");
				}
				else if (tokens.length == 2) {
					String fsmName = tokens[1];
					FiniteStateMachine fsm = null;
					if (fsmName.equals("xor")) {
						fsm = FiniteStateMachine.xorMachine();
					}
					else if (fsmName.equals("and")) {
						fsm = FiniteStateMachine.andMachine();
					}
					else if (fsmName.equals("flipflop")) {
						fsm = FiniteStateMachine.flipFlopMachine();
					}
					else if (fsmName.equals("memory")) {
						fsm = FiniteStateMachine.memoryMachine();
					}
					else if (fsmName.equals("counter")) {
						fsm = FiniteStateMachine.counterMachine();
					}
					else if (fsmName.equals("memory")) {
						fsm = FiniteStateMachine.memoryMachine();
					}
					else {
						textArea.append("\nError: fsm not recognized");
					}
					if (fsm != null) {
						textArea.append("\nTraining network...");
						dcdnn.learn(fsm);
					}
				}
				else {
					textArea.append("\nError: learn command takes 1 argument <fsm>");
				}
			}
			else {
				textArea.append("\nError: Network must be paused");
			}
		}
		else if (command.equals("clockify")) {
			if (dcdnn.network == null) {
				textArea.append("\nError: No network to clockify");
			}
			else {
				dcdnn.clockifyNetwork();
			}
		}
		else if (command.equals("pseudoclockify")) {
			if (dcdnn.network == null) {
				textArea.append("\nError: No network to pseudoclockify");
			}
			else {
				dcdnn.pseudoClockifyNetwork();
			}
		}
		else if (command.equals("test")) {
			/*Genotype genotype = Genotype.andOrGenotype();
			genotype.prune();
			System.out.println(genotype);
			//genotype.evaluate(FiniteStateMachine.flipFlopMachine(), 100, 1);
			dcdnn.network = new Network(0, 0);
			dcdnn.view.network = dcdnn.network;
			dcdnn.loadNetworkFromGenotype(genotype);*/
			dcdnn.network = new Network(0, 0);
			dcdnn.view.network = dcdnn.network;
			dcdnn.addAndOrComponents();
		}
		else {
			textArea.append("\nCommand not recognized. Enter \"?\" for a list of available commands.");
		}
		textArea.setCaretPosition(textArea.getDocument().getLength());
	}
}