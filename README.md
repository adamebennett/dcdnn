# dcdnn
Constraint programming technique for the automatic combination of networks of functional groupings of spiking neurons. DCDNN is written in Java.

For more information, visit:
https://curve.carleton.ca/36d3e7b7-6907-45b3-8d4a-fa172c3d9dd3

# Building
Compile all *.java files in the dcdnn folder. In Windows, this can be done by navigating to the outermost dcdnn folder and running the following command:
`javac -cp ./lib/choco-solver-4.0.3-with-dependencies.jar ./dcdnn/*.java`

# Running
From the outermost dcdnn folder, enter the following command to run the program:
`java -cp ./lib/choco-solver-4.0.3-with-dependencies.jar; dcdnn.DCDNN`

# Instructions
When the program is running, a window should appear with a text area, a command line, and an graphics panel. Commands can be entered into the command line. Output will appear in the text area above the command line. When networks are loaded, they will appear graphically in the graphics panel.

## Text Area
Shows text-based results of commands.

## Command Line
Networks can be loaded and run by entering commands here. Enter `?` into the command line and press the RETURN key to view a list of available commands. See the Commands section below for more details.

## Graphics Panel
The current network is shown visually here.

* Neurons appear as small circles. Neurons which are input neurons for a component are green, and neurons which are output neurons for a component are orange. Neurons which are both the input and output neuron of a component are orange. All other neurons are black.

* Synapses appear as black lines between two neurons. Signals traversing a synapse appear as either red or blue lines travelling across the synapse. Red signals are excitatory and blue signals are inhibitory.

* Components appear as black boxes around the neurons and intra-component synapses they are comprised of.

# Commands
* `?`: Shows a list of commands
* `run`: Runs the currently loaded network. Fails if no network is loaded or if the network is already running.
* `pause`: Stops the network from running. Fails if no network is loaded or if the network is not running.
* `step`: runs the currently loaded network for a single time step, then stops. Fails if no network is loaded or if the network is already running.
	* Optionally, use with the <x> argument (enter the command as `step x` to step for x time steps rather than once. For example, `step 5` will cause the network to run for 5 time steps.
* `s`: same as `step`
* `record`: writes spiking activity of output nodes of output components to plots/plot.csv
	* Optionally, use the <all> argument (enter the command as `record all`) to write the spiking activity of all neurons to plots/plot.csv.
* `stop`: stops recording spiking activity of nodes. Fails if not currently recording.
* `input`: Manually cause input neurons of input components to fire. Requires one argument per input neuron to the network. The values of the arguments are the activation levels of the signals fired by the input neurons.
	*For example, for a network with 3 input neurons, `input 1 0 2` would cause the first input neuron to fire with an activation level of 1, the second input neuron wouldn't fire, and the third input neuron would fire with an activation level of 2.
* `export`: exports the currently loaded network to networks/network.nn. The output file can then be manually editted to make a component file (this whole process needs documentation, and probably some streamlining).
* `disorg`: randomly generates a network.
* `xor`, `flipflop`, `memory`, `counter`, `doublecounter`, `quadcounter`, `decodedcounter`, `tmazememory`, `tmaze`: each of these commands corresponds to a pre-made network. Entering one of these commands will load the corresponding network.
* `clockify`: adds a global clock to the currently loaded network and routes synapses from input components through AND gate components. Ensures that input signals don't enter the non-input components except at certain timesteps as determined by the global clock. For more information, see Chapter 4 of https://curve.carleton.ca/36d3e7b7-6907-45b3-8d4a-fa172c3d9dd3. Note that this command adds a new input component containing a single input neuron to the network. This input is used for initiating the global clock.

# Example
The following commands will load and run an eight-bit counter network. First, enter `quadcounter` into the command line. Next, enter `clockify`. Finally, enter `run`. You should now be able to see signals traversing synapses in the network. Watch the OUT_0, OUT_1, ..., OUT_7 output components to see the network count in binary.