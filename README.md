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

* Neurons appear as small circles. Neurons which are input neurons for a component are green, and neurons which are output neurons for a component are orange. All other neurons are black.

* Synapses appear as black lines between two neurons. Signals traversing a synapse appear as either red or blue lines travelling across the synapse. Red signals are excitatory and blue signals are inhibitory.

* Components appear as black boxes around the neurons and intra-component synapses they are comprised of.

# Commands
* `?`: Shows a list of commands
* `run`: Runs the currently loaded network. Fails if no network is loaded or if the network is already running.
* `pause`: Stops the network from running. Fails if no network is loaded or if the network is not running.
* `step`: runs the currently loaded network for a single time step, then stops. Fails if no network is loaded or if the network is already running.
** Optionally, use with the <x> argument to step for x time steps rather than once. For example, `step 5` will cause the network to run for 5 time steps.
* `s`: same as `step`

# Example