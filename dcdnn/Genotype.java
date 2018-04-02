package dcdnn;

import java.util.ArrayList;
import java.util.HashMap;

public class Genotype {

	public ArrayList<Allele> alleles;
	public int numInputs;
	public int numOutputs;
	public int minMachines;
	public int maxMachines;
	public double fitness;
	
	public Genotype() {
		alleles = new ArrayList<Allele>();
		numInputs = 0;
	}

	public Genotype(int numIn, int numMachines, int numOut, int minFSM, int maxFSM) {
		numInputs = numIn;
		numOutputs = numOut;
		minMachines = minFSM;
		maxMachines = maxFSM;
		alleles = new ArrayList<Allele>();
		for (int i = 0; i < numInputs; ++i) {
			alleles.add(new InputAllele(this, i));
		}
		for (int i = 0; i < numOut; ++i) {
			alleles.add(new OutputAllele(this, i + numInputs));
		}
		//alleles.add(BufferAllele.inputBuffer(this, numInputs + numOutputs));
		for (int i = 0; i < numMachines; ++i) {
			alleles.add(Allele.randomAllele(this, i + numInputs + numOutputs));
		}
		for (int i = 0; i < alleles.size(); ++i) {
			alleles.get(i).init();
		}
	}

	public Genotype(int numIn, int numOut, int minFSM, int maxFSM) {
		numInputs = numIn;
		numOutputs = numOut;
		minMachines = minFSM;
		maxMachines = maxFSM;
		alleles = new ArrayList<Allele>();
		for (int i = 0; i < numInputs; ++i) {
			alleles.add(new InputAllele(this, i));
		}
		for (int i = 0; i < numOut; ++i) {
			alleles.add(new OutputAllele(this, i + numInputs));
		}
		//alleles.add(BufferAllele.inputBuffer(this, numInputs + numOutputs));
		for (int i = 0; i < alleles.size(); ++i) {
			alleles.get(i).init();
		}
	}

	public int getNumAlleles() {
		return alleles.size();
	}

	public int getNumMachines() {
		return alleles.size() - (numInputs + numOutputs);
	}

	public void updateAlleles() {
		for (Allele a: alleles) {
			a.genotype = this;
			a.update();
		}
	}

	public ArrayList<AlleleConnection> getAlleleConnections() {
		ArrayList<AlleleConnection> connections = new ArrayList<AlleleConnection>();
		for (Allele allele: alleles) {
			for (int i = 0; i < allele.getNumInputs(); ++i) {
				Allele from = allele.getInput(i);
				AlleleConnection connection = new AlleleConnection(from, allele.getOutputIndexOf(i), allele, i);
				connections.add(connection);
				//System.out.println(connection);
			}
		}
		return connections;
	}

	public void evaluate(FiniteStateMachine goal, int timeSteps, int iterations) {
		double sumFitness = 0.0; // lower is better

		// Preprocessing: set up genotype containing goal FSM
		Genotype goalGenotype = new Genotype(goal.numInputs, 0, goal.numOutputs, 0, 1);
		goalGenotype.alleles.add(new MachineAllele(goalGenotype, goal.numInputs + goal.numOutputs + 1, goal));
		int q = 0;
		// make outputs point at goal allele
		for (int i = goal.numInputs; i < goal.numInputs + goal.numOutputs; ++i) {
			((OutputAllele)goalGenotype.alleles.get(i)).inputIndex = goal.numInputs + goal.numOutputs + 1;
			((OutputAllele)goalGenotype.alleles.get(i)).outputIndex = q;
			++q;
		}
		// make goal point at buffer
		for (int i = 0; i < goal.numInputs; ++i) {
			((MachineAllele)goalGenotype.alleles.get(goal.numInputs + goal.numOutputs + 1)).indices[i] = goal.numInputs + goal.numOutputs;
			((MachineAllele)goalGenotype.alleles.get(goal.numInputs + goal.numOutputs + 1)).outputIndices[i] = i;
		}

		// Preprocessing: figure out connections between alleles:
		// generate set of connections between alleles
		ArrayList<AlleleConnection> connections = getAlleleConnections();

		ArrayList<AlleleConnection> goalConnections = goalGenotype.getAlleleConnections();

		// find which cycles each allele is in
		AlleleConnection.findCycles(connections);

		AlleleConnection.findCycles(goalConnections);

		// find which connections we should stop recursing backwards at
		AlleleConnection.markEntries(connections);

		AlleleConnection.markEntries(goalConnections);

		// Debug code for seeing which connections are marked
//		for  (AlleleConnection connection: connections) {
// 			System.out.println(connection);
// 		}

		// End of connection preprocessing.

		for (int i = 0; i < iterations; ++i) {
			// clean everything up
			for (Allele allele: alleles) {
				allele.cleanUp();
				allele.allocateTestValues(timeSteps);
			}
			// goal.state = goal.copyBitString(goal.initialState); // not needed?

			for (Allele allele: goalGenotype.alleles) {
				allele.cleanUp();
				allele.allocateTestValues(timeSteps);
			}

			int[][] allInputs = new int[timeSteps + Math.max(0,goal.delayPeriods)][];

			// initialize inputs for all time steps
			for (int j = 0; j < timeSteps; ++j) {
				// pick random inputs
				int[] inputs = new int[numInputs];
				for (int a = 0; a < numInputs; ++a) {
					if (goal.clockified == -1 || j % goal.clockified == 0) { // simulate clockification of modelled network
						if (Math.random() < 0.5) {
							inputs[a] = 0;
						}
						else {
							inputs[a] = 1;
						}
					}
					else {
						inputs[a] = 0; // inputs would be filtered out by network's clock (added by clockify)
					}
				}
				allInputs[j] = inputs;
			}

			// run timeSteps time steps
			for (int j = 0; j < timeSteps; ++j) {

				int[] inputs = allInputs[j];

				// run one iteration of this Genotype's goal
				goalGenotype.step(allInputs, goalConnections, j);
				int[] goalOutput = null;
				goalOutput = copyIntArray(goalGenotype.getOutputsAtTime(goalConnections, j));

				/*if (j > Math.max(0,goal.delayPeriods)) {
					goalOutput = copyIntArray(goal.transition(allInputs[j-Math.max(0,goal.delayPeriods)]));
				}
	            else {
					goalOutput = copyIntArray(goal.transition(new int[goal.numInputs]));
	            }*/

				// run one iteration of this Genotype's model
				//System.out.println();
				this.step(allInputs, connections, j);
				int[] modelOutput = null;
				modelOutput = copyIntArray(this.getOutputsAtTime(connections, j));

				// compare distance between goal and model
				sumFitness += distance(goalOutput, modelOutput);
			}
		}
		fitness = (sumFitness / (timeSteps * iterations)) * 100.0;

		// save memory
		for (Allele allele: alleles) {
			allele.testValues = null;
		}
		for (Allele allele: goalGenotype.alleles) {
			allele.testValues = null;
		}
	}

	public int distance(int[] a, int[] b) {
		int dist = 0;
		for (int i = 0; i < a.length; ++i) {
			if (a[i] != b[i]) {
				dist += 1;
			}
		}
		return dist;
	}

	private void step(int[][] allInputs, ArrayList<AlleleConnection> connections, int time) {
		ArrayList<AlleleTimePair> visited = new ArrayList<AlleleTimePair>();
		for (Allele allele: alleles) {
			evaluate(allele, connections, allInputs, time, false, visited); // recursive function to process single allele and dependencies
		}
	}

	public int[] evaluate(Allele allele, ArrayList<AlleleConnection> connections, int[][] allInputs, int time, boolean isEntry, ArrayList<AlleleTimePair> visited) {
		// base case 1: time is negative
		if (time < 0) {
			int[] toReturn = new int[allele.getNumOutputs()]; // return array of zeros
			return toReturn;
		}

		// base case 2: allele is output allele
		if (allele.index >= numInputs && allele.index < numInputs + numOutputs) {
			return new int[] {0};
		}

		// base case 3: allele is already processed for this time step
		if (allele.testValues[time][0] != -1) { // all will be processed or none
			return copyIntArray(allele.testValues[time]);
		}

		// base case 4: allele is input allele
		if (allele.index < numInputs) {
			allele.testValues[time][0] = allInputs[time][allele.index]; // there is only one value to fill
			return copyIntArray(allele.testValues[time]);
		}

		// base case 5: allele has already been visited for this time step (is in infinite loop)
		for (AlleleTimePair pair: visited) {
			if (pair.allele == allele && pair.time == time) {
				allele.testValues[time] = new int[allele.getNumOutputs()];
				return copyIntArray(allele.testValues[time]);
			}
		}

		// add current allele/time to visited list
		visited.add(new AlleleTimePair(allele, time));

		// deal with recurrent alleles? (ie, alleles whose FSM represents a component with internal cycles)
		if (allele.getDelayPeriods() != -1) {
			if (time % allele.getDelayPeriods() != 0) {
				int newTime = (int)(Math.floor(time / allele.getDelayPeriods()) * allele.getDelayPeriods());
				int[] returned = evaluate(allele, connections, allInputs, newTime, isEntry, visited);
				allele.testValues[time] = copyIntArray(returned);
				return copyIntArray(allele.testValues[time]);
			}
		}

		// gather needed inputs
		int[] neededInputs = new int[allele.getNumInputs()];
		// need to simulate internal clock filtering for clockified machines
		if (allele.getClockified() == -1 || time % allele.getClockified() == 0) { // treat inputs as zero if they would be filtered out
			for (int i = 0; i < allele.getNumInputs(); ++i) {
				Allele from = allele.getInput(i);
				AlleleConnection conn = AlleleConnection.findConnection(from, allele.getOutputIndexOf(i), allele, i, connections);
				int newTime = time;
				if (isEntry) {
					newTime = time - 1;
				}
				int[] returned = evaluate(from, connections, allInputs, newTime, conn.isEntry, visited);
				neededInputs[i] = returned[allele.getOutputIndexOf(i)];
			}
		}

		// evaluate allele
		allele.testValues[time] = copyIntArray(allele.transition(neededInputs));
		//System.out.println(allele);
		return copyIntArray(allele.testValues[time]);
	}

	protected int[] getOutputsAtTime(ArrayList<AlleleConnection> connections, int time) {
		// get output connections, return vector of their values
		int[] toReturn = new int[numOutputs];
		int retIndex = 0;
		for (int i = numInputs; i < numInputs + numOutputs; ++i) {
			toReturn[retIndex] = alleles.get(i).getInput(0).testValues[time][alleles.get(i).getOutputIndexOf(0)];
			retIndex++;
		}
		return toReturn;
	}

	public void prune() {
		// initialize all alleles to be unpruned
		for (Allele allele: alleles) {
			allele.pruned = false;
		}
		// get connections
		ArrayList<AlleleConnection> connections = getAlleleConnections();
		
		// prune until it stops having an effect
		boolean somethingPruned = true;
		while (somethingPruned) {
			somethingPruned = false;
			for (Allele allele: alleles) {
				if (allele.pruned || allele.index < numInputs + numOutputs) {
					continue;
				}
				boolean goesElsewhere = false;
				for (AlleleConnection conn: connections) {
					if (conn.from == allele && conn.to != allele) {
						if (conn.leadsToOutput(conn, new ArrayList<AlleleConnection>(), connections, this)) {
							goesElsewhere = true;
						}
					}
				}
				if (!goesElsewhere) {
					allele.pruned = true;
					somethingPruned = true;
				}
			}
		}
		updateAlleles();
	}

	public static int[] copyIntArray(int[] toCopy) {
		if (toCopy == null) return null;
		int[] toReturn = new int[toCopy.length];
		for (int i = 0; i < toCopy.length; ++i) {
			toReturn[i] = toCopy[i];
		}
		return toReturn;
	}

	public void mutate(double rate) {
		if (Math.random() > rate) {
			return;
		}
		if (Math.random() > 0.5) {
			if (getNumMachines() > minMachines) {
				int indexToRemove = (int)(Math.random() * getNumAlleles());
				if (indexToRemove < numInputs + numOutputs + 1) {
					return;
				}
				if (alleles.get(indexToRemove).canChange() == true && alleles.get(indexToRemove).mustExist() == false) {
					// fix subsequent alleles
					for (int i = indexToRemove+1; i < alleles.size(); ++i) {
						if (alleles.get(i).canChange()) {
							MachineAllele mAllele = (MachineAllele)alleles.get(i); // we can do this since all alleles below the one being removed must be output alleles
							mAllele.index--;
							for (int j = 0; j < mAllele.indices.length; ++j) {
								if (mAllele.indices[j] > indexToRemove) {
									mAllele.indices[j]--;
								}
							}
						}
					}
					alleles.remove(indexToRemove);
					updateAlleles();
				}
			}
		}
		else {
			if (getNumMachines() < maxMachines) {
				Allele allele = Allele.randomAllele(this, getNumAlleles() + 1);
				alleles.add(allele);
				allele.init();
				updateAlleles();
			}
		}
	}

	public static Genotype crossover(Genotype a, Genotype b) {
		// initialize offspring
		Genotype c = new Genotype(a.numInputs, 0, a.numOutputs, a.minMachines, a.maxMachines);

		// get required inputs from parent a
		for (int i = 0; i < a.numInputs; ++i) {
			c.alleles.set(i, a.alleles.get(i).clone());
		}

		// get the outputs from a random parent
		for (int i = a.numInputs; i < a.numInputs + a.numOutputs; ++i) {
			if (Math.random() < 0.5) {
				c.alleles.set(i, a.alleles.get(i).clone());
			}
			else {
				c.alleles.set(i, b.alleles.get(i).clone());
			}
		}

		// get the input buffer allele from parent a
		c.alleles.set(a.numInputs + a.numOutputs, a.alleles.get(a.numInputs + a.numOutputs).clone());

		// decide the length of c
		int cLength;
		if (Math.random() < 0.5) {
			cLength = a.alleles.size();
		}
		else {
			cLength = b.alleles.size();
		}

		// fill out c's alleles
		for (int i = a.numInputs + a.numOutputs + 1; i < cLength; ++i) {
			if (Math.random() < 0.5) {
				if (i < a.alleles.size()) {
					c.alleles.add(a.alleles.get(i).clone());
				}
				else {
					c.alleles.add(b.alleles.get(i).clone());
				}
			}
			else {
				if (i < b.alleles.size()) {
					c.alleles.add(b.alleles.get(i).clone());
				}
				else {
					c.alleles.add(a.alleles.get(i).clone());
				}
			}
		}

		c.updateAlleles();
		return c;
	}

	public String toString() {
		String str = "";
		for (Allele allele: alleles) {
			str += allele;
			if (allele.pruned) {
				str += " (pruned)";
			}
			str += "\n";
		}
		return str;
	}

	// returns a sample genotype for testing
	public static Genotype counterGenotype() {
		Genotype genotype = new Genotype(1, 2, 0, 25);
		genotype.alleles.add(new MachineAllele(genotype, 3, FiniteStateMachine.andMachine())); // cAnd1
		genotype.alleles.add(new MachineAllele(genotype, 4, FiniteStateMachine.andMachine())); // cAnd2
		genotype.alleles.add(new MachineAllele(genotype, 5, FiniteStateMachine.notMachine())); // b0Not
		genotype.alleles.add(new MachineAllele(genotype, 6, FiniteStateMachine.orMachine())); // b0Or
		genotype.alleles.add(new MachineAllele(genotype, 7, FiniteStateMachine.andMachine())); // b0And
		genotype.alleles.add(new MachineAllele(genotype, 8, FiniteStateMachine.andMachine())); // b0Q
		genotype.alleles.add(new MachineAllele(genotype, 9, FiniteStateMachine.notMachine())); // b0QBar
     
		genotype.alleles.add(new MachineAllele(genotype, 10, FiniteStateMachine.andMachine())); // cAnd3
		genotype.alleles.add(new MachineAllele(genotype, 11, FiniteStateMachine.andMachine())); // cAnd4
		genotype.alleles.add(new MachineAllele(genotype, 12, FiniteStateMachine.andMachine())); // cAnd5
		genotype.alleles.add(new MachineAllele(genotype, 13, FiniteStateMachine.andMachine())); // cAnd6
		genotype.alleles.add(new MachineAllele(genotype, 14, FiniteStateMachine.notMachine())); // b1Not
		genotype.alleles.add(new MachineAllele(genotype, 15, FiniteStateMachine.orMachine())); // b1Or
		genotype.alleles.add(new MachineAllele(genotype, 16, FiniteStateMachine.andMachine())); // b1And
		genotype.alleles.add(new MachineAllele(genotype, 17, FiniteStateMachine.andMachine())); // b1Q
		genotype.alleles.add(new MachineAllele(genotype, 18, FiniteStateMachine.notMachine())); // b1QBar
     
		((OutputAllele)genotype.alleles.get(1)).inputIndex = 8;
		((OutputAllele)genotype.alleles.get(2)).inputIndex = 17;
     
		((MachineAllele)genotype.alleles.get(3)).indices[0] = 0;
		((MachineAllele)genotype.alleles.get(3)).indices[1] = 9;
		((MachineAllele)genotype.alleles.get(4)).indices[0] = 0;
		((MachineAllele)genotype.alleles.get(4)).indices[1] = 8;
		((MachineAllele)genotype.alleles.get(5)).indices[0] = 4;
		((MachineAllele)genotype.alleles.get(6)).indices[0] = 3;
		((MachineAllele)genotype.alleles.get(6)).indices[1] = 8;
		((MachineAllele)genotype.alleles.get(7)).indices[0] = 5;
		((MachineAllele)genotype.alleles.get(7)).indices[1] = 6;
		((MachineAllele)genotype.alleles.get(8)).indices[0] = 7;
		((MachineAllele)genotype.alleles.get(8)).indices[1] = 7;
		((MachineAllele)genotype.alleles.get(9)).indices[0] = 7;
     
		((MachineAllele)genotype.alleles.get(10)).indices[0] = 0;
		((MachineAllele)genotype.alleles.get(10)).indices[1] = 8;
		((MachineAllele)genotype.alleles.get(11)).indices[0] = 0;
		((MachineAllele)genotype.alleles.get(11)).indices[1] = 8;
		((MachineAllele)genotype.alleles.get(12)).indices[0] = 10;
		((MachineAllele)genotype.alleles.get(12)).indices[1] = 18;
		((MachineAllele)genotype.alleles.get(13)).indices[0] = 11;
		((MachineAllele)genotype.alleles.get(13)).indices[1] = 17;
		((MachineAllele)genotype.alleles.get(14)).indices[0] = 13;
		((MachineAllele)genotype.alleles.get(15)).indices[0] = 12;
		((MachineAllele)genotype.alleles.get(15)).indices[1] = 17;
		((MachineAllele)genotype.alleles.get(16)).indices[0] = 14;
		((MachineAllele)genotype.alleles.get(16)).indices[1] = 15;
		((MachineAllele)genotype.alleles.get(17)).indices[0] = 16;
		((MachineAllele)genotype.alleles.get(17)).indices[1] = 16;
		((MachineAllele)genotype.alleles.get(18)).indices[0] = 16;

		return genotype;
	}

	// returns a sample genotype for testing
	public static Genotype recurrentCounterGenotype() {
		Genotype genotype = new Genotype(1, 2, 0, 25);
		genotype.alleles.add(new MachineAllele(genotype, 3, FiniteStateMachine.andMachine())); // cAnd1
		genotype.alleles.add(new MachineAllele(genotype, 4, FiniteStateMachine.andMachine())); // cAnd2
		genotype.alleles.add(new MachineAllele(genotype, 5, FiniteStateMachine.flipFlopMachine())); // bit0
     
		genotype.alleles.add(new MachineAllele(genotype, 6, FiniteStateMachine.andMachine())); // cAnd3
		genotype.alleles.add(new MachineAllele(genotype, 7, FiniteStateMachine.andMachine())); // cAnd4
		genotype.alleles.add(new MachineAllele(genotype, 8, FiniteStateMachine.andMachine())); // cAnd5
		genotype.alleles.add(new MachineAllele(genotype, 9, FiniteStateMachine.andMachine())); // cAnd6
		genotype.alleles.add(new MachineAllele(genotype, 10, FiniteStateMachine.flipFlopMachine())); // bit 1

		genotype.alleles.add(new MachineAllele(genotype, 11, FiniteStateMachine.orMachine())); // clock
		genotype.alleles.add(new MachineAllele(genotype, 12, FiniteStateMachine.orMachine())); // clock
		genotype.alleles.add(new MachineAllele(genotype, 13, FiniteStateMachine.orMachine())); // clock
     
		((OutputAllele)genotype.alleles.get(1)).inputIndex = 5;
		((OutputAllele)genotype.alleles.get(2)).inputIndex = 10;
     
		((MachineAllele)genotype.alleles.get(3)).indices[0] = 13;
		((MachineAllele)genotype.alleles.get(3)).indices[1] = 5;
		((MachineAllele)genotype.alleles.get(3)).outputIndices[1] = 1;
		((MachineAllele)genotype.alleles.get(4)).indices[0] = 13;
		((MachineAllele)genotype.alleles.get(4)).indices[1] = 5;
		((MachineAllele)genotype.alleles.get(4)).outputIndices[1] = 0;
		((MachineAllele)genotype.alleles.get(5)).indices[0] = 3;
		((MachineAllele)genotype.alleles.get(5)).indices[1] = 4;
     
		((MachineAllele)genotype.alleles.get(6)).indices[0] = 13;
		((MachineAllele)genotype.alleles.get(6)).indices[1] = 5;
		((MachineAllele)genotype.alleles.get(6)).outputIndices[1] = 0;
		((MachineAllele)genotype.alleles.get(7)).indices[0] = 6;
		((MachineAllele)genotype.alleles.get(7)).indices[1] = 10;
		((MachineAllele)genotype.alleles.get(7)).outputIndices[1] = 1;
		((MachineAllele)genotype.alleles.get(8)).indices[0] = 13;
		((MachineAllele)genotype.alleles.get(8)).indices[1] = 5;
		((MachineAllele)genotype.alleles.get(8)).outputIndices[1] = 0;
		((MachineAllele)genotype.alleles.get(9)).indices[0] = 8;
		((MachineAllele)genotype.alleles.get(9)).indices[1] = 10;
		((MachineAllele)genotype.alleles.get(9)).outputIndices[1] = 0;
		((MachineAllele)genotype.alleles.get(10)).indices[0] = 7;
		((MachineAllele)genotype.alleles.get(10)).indices[1] = 9;

		((MachineAllele)genotype.alleles.get(11)).indices[0] = 0;
		((MachineAllele)genotype.alleles.get(11)).indices[1] = 13;
		((MachineAllele)genotype.alleles.get(12)).indices[0] = 11;
		((MachineAllele)genotype.alleles.get(12)).indices[1] = 11;
		((MachineAllele)genotype.alleles.get(13)).indices[0] = 12;
		((MachineAllele)genotype.alleles.get(13)).indices[1] = 12;

		return genotype;
	}

	// returns a sample genotype for testing
	public static Genotype memoryGenotype() {
		Genotype genotype = new Genotype(3, 2, 0, 25);
		genotype.alleles.add(new MachineAllele(genotype, 5, FiniteStateMachine.notMachine())); // cNot1
		genotype.alleles.add(new MachineAllele(genotype, 6, FiniteStateMachine.andMachine())); // cAnd1
		genotype.alleles.add(new MachineAllele(genotype, 7, FiniteStateMachine.andMachine())); // cAnd2
		genotype.alleles.add(new MachineAllele(genotype, 8, FiniteStateMachine.notMachine())); // b0Not
		genotype.alleles.add(new MachineAllele(genotype, 9, FiniteStateMachine.orMachine())); // b0Or
		genotype.alleles.add(new MachineAllele(genotype, 10, FiniteStateMachine.andMachine())); // b0And
		genotype.alleles.add(new MachineAllele(genotype, 11, FiniteStateMachine.andMachine())); // b0Q
		genotype.alleles.add(new MachineAllele(genotype, 12, FiniteStateMachine.notMachine())); // b0QBar   
		genotype.alleles.add(new MachineAllele(genotype, 13, FiniteStateMachine.notMachine())); // cNot2  
		genotype.alleles.add(new MachineAllele(genotype, 14, FiniteStateMachine.andMachine())); // cAnd3
		genotype.alleles.add(new MachineAllele(genotype, 15, FiniteStateMachine.andMachine())); // cAnd4
		genotype.alleles.add(new MachineAllele(genotype, 16, FiniteStateMachine.notMachine())); // b1Not
		genotype.alleles.add(new MachineAllele(genotype, 17, FiniteStateMachine.orMachine())); // b1Or
		genotype.alleles.add(new MachineAllele(genotype, 18, FiniteStateMachine.andMachine())); // b1And
		genotype.alleles.add(new MachineAllele(genotype, 19, FiniteStateMachine.andMachine())); // b1Q
		genotype.alleles.add(new MachineAllele(genotype, 20, FiniteStateMachine.notMachine())); // b1QBar
     
		((OutputAllele)genotype.alleles.get(3)).inputIndex = 11;
		((OutputAllele)genotype.alleles.get(4)).inputIndex = 19;
     
		((MachineAllele)genotype.alleles.get(5)).indices[0] = 1;
		((MachineAllele)genotype.alleles.get(6)).indices[0] = 0;
		((MachineAllele)genotype.alleles.get(6)).indices[1] = 1;
		((MachineAllele)genotype.alleles.get(7)).indices[0] = 0;
		((MachineAllele)genotype.alleles.get(7)).indices[1] = 5;
		((MachineAllele)genotype.alleles.get(8)).indices[0] = 7;
		((MachineAllele)genotype.alleles.get(9)).indices[0] = 6;
		((MachineAllele)genotype.alleles.get(9)).indices[1] = 11;
		((MachineAllele)genotype.alleles.get(10)).indices[0] = 8;
		((MachineAllele)genotype.alleles.get(10)).indices[1] = 9;
		((MachineAllele)genotype.alleles.get(11)).indices[0] = 10;
		((MachineAllele)genotype.alleles.get(11)).indices[1] = 10;
		((MachineAllele)genotype.alleles.get(12)).indices[0] = 10;
		((MachineAllele)genotype.alleles.get(13)).indices[0] = 2;
		((MachineAllele)genotype.alleles.get(14)).indices[0] = 0;
		((MachineAllele)genotype.alleles.get(14)).indices[1] = 2;
		((MachineAllele)genotype.alleles.get(15)).indices[0] = 0;
		((MachineAllele)genotype.alleles.get(15)).indices[1] = 13;
		((MachineAllele)genotype.alleles.get(16)).indices[0] = 15;
		((MachineAllele)genotype.alleles.get(17)).indices[0] = 14;
		((MachineAllele)genotype.alleles.get(17)).indices[1] = 19;
		((MachineAllele)genotype.alleles.get(18)).indices[0] = 16;
		((MachineAllele)genotype.alleles.get(18)).indices[1] = 17;
		((MachineAllele)genotype.alleles.get(19)).indices[0] = 18;
		((MachineAllele)genotype.alleles.get(19)).indices[1] = 18;
		((MachineAllele)genotype.alleles.get(20)).indices[0] = 18;

		return genotype;
	}

	// returns a sample genotype for testing
	public static Genotype flipFlopGenotype() {
		Genotype genotype = new Genotype(2, 2, 0, 25);
		genotype.alleles.add(new MachineAllele(genotype, 5, FiniteStateMachine.andOrMachine()));
		genotype.alleles.add(new MachineAllele(genotype, 6, FiniteStateMachine.notMachine()));
		genotype.alleles.add(new MachineAllele(genotype, 7, FiniteStateMachine.orMachine()));

		((OutputAllele)genotype.alleles.get(2)).inputIndex = 7;
		((OutputAllele)genotype.alleles.get(3)).inputIndex = 6;
     
		((MachineAllele)genotype.alleles.get(5)).indices[0] = 7;
		((MachineAllele)genotype.alleles.get(5)).indices[1] = 0;
		((MachineAllele)genotype.alleles.get(5)).indices[2] = 1;
		((MachineAllele)genotype.alleles.get(6)).indices[0] = 7;
		((MachineAllele)genotype.alleles.get(7)).indices[0] = 5;
		((MachineAllele)genotype.alleles.get(7)).indices[1] = 5;

		return genotype;
	}

	// returns a sample genotype for testing
	public static Genotype oldFlipFlopGenotype() {
		Genotype genotype = new Genotype(2, 2, 0, 25);
		genotype.alleles.add(new MachineAllele(genotype, 4, FiniteStateMachine.notMachine()));
		genotype.alleles.add(new MachineAllele(genotype, 5, FiniteStateMachine.orMachine()));
		genotype.alleles.add(new MachineAllele(genotype, 6, FiniteStateMachine.andMachine()));
		genotype.alleles.add(new MachineAllele(genotype, 7, FiniteStateMachine.notMachine()));

		((OutputAllele)genotype.alleles.get(2)).inputIndex = 6;
		((OutputAllele)genotype.alleles.get(3)).inputIndex = 7;
     
		((MachineAllele)genotype.alleles.get(4)).indices[0] = 1;
		((MachineAllele)genotype.alleles.get(5)).indices[0] = 0;
		((MachineAllele)genotype.alleles.get(5)).indices[1] = 6;
		((MachineAllele)genotype.alleles.get(6)).indices[0] = 4;
		((MachineAllele)genotype.alleles.get(6)).indices[1] = 5;
		((MachineAllele)genotype.alleles.get(7)).indices[0] = 6;

		return genotype;
	}

	// returns a sample genotype for testing
	public static Genotype andOrGenotype() {
		Genotype genotype = new Genotype(3, 1, 0, 25);
		genotype.alleles.add(new MachineAllele(genotype, 4, FiniteStateMachine.notMachine()));
		genotype.alleles.add(new MachineAllele(genotype, 5, FiniteStateMachine.orMachine()));
		genotype.alleles.add(new MachineAllele(genotype, 6, FiniteStateMachine.andMachine()));

		((OutputAllele)genotype.alleles.get(3)).inputIndex = 6;
     
		((MachineAllele)genotype.alleles.get(4)).indices[0] = 2;
		((MachineAllele)genotype.alleles.get(5)).indices[0] = 0;
		((MachineAllele)genotype.alleles.get(5)).indices[1] = 1;
		((MachineAllele)genotype.alleles.get(6)).indices[0] = 4;
		((MachineAllele)genotype.alleles.get(6)).indices[1] = 5;

		return genotype;
	}

	// returns a sample genotype for testing
	public static Genotype nandGenotype() {
		Genotype genotype = new Genotype(2, 1, 0, 25);
		genotype.alleles.add(new MachineAllele(genotype, 4, FiniteStateMachine.andMachine()));
		genotype.alleles.add(new MachineAllele(genotype, 5, FiniteStateMachine.notMachine()));

		((OutputAllele)genotype.alleles.get(2)).inputIndex = 5;
     
		((MachineAllele)genotype.alleles.get(4)).indices[0] = 0;
		((MachineAllele)genotype.alleles.get(4)).indices[1] = 1;
		((MachineAllele)genotype.alleles.get(5)).indices[0] = 4;

		return genotype;
	}

	// returns a sample genotype for testing
	public static Genotype bufferGenotype() {
		Genotype genotype = new Genotype(3, 3, 0, 25);

		((OutputAllele)genotype.alleles.get(3)).inputIndex = 0;
		((OutputAllele)genotype.alleles.get(4)).inputIndex = 1;
		((OutputAllele)genotype.alleles.get(5)).inputIndex = 2;

		return genotype;
	}

	/*public static void main(String[] args) {
		Genotype genotype = new Genotype(2, 5, 1, 0, 20);
		genotype.alleles.add(new MachineAllele(genotype, 3, FiniteStateMachine.notMachine()));
		genotype.alleles.add(new MachineAllele(genotype, 4, FiniteStateMachine.andMachine()));
		genotype.alleles.add(new MachineAllele(genotype, 5, FiniteStateMachine.orMachine()));
		((OutputAllele)genotype.alleles.get(2)).inputIndex = 4;
		((OutputAllele)genotype.alleles.get(2)).outputIndex = 0;
		((MachineAllele)genotype.alleles.get(3)).indices[0] = 1;
		((MachineAllele)genotype.alleles.get(4)).indices[0] = 3;
		((MachineAllele)genotype.alleles.get(4)).indices[1] = 5;
		((MachineAllele)genotype.alleles.get(5)).indices[0] = 0;
		((MachineAllele)genotype.alleles.get(5)).indices[1] = 4;
		

		for (int i = 0; i < 50; ++i) {
			genotype.mutate(0.20);
		}

		genotype.evaluate(FiniteStateMachine.xorMachine(), 50, 1);

		System.out.println();
		for (Allele a: genotype.alleles) {
			System.out.println(a);
		}
	
		System.out.println();
		System.out.println("Fitness: " + genotype.fitness);
	}*/
}