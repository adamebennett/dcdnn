package dcdnn;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;

public class Population {

	protected static final long SLEEP_TIME = 1000;

	ArrayList<Genotype> genotypes;
	FiniteStateMachine goal;

	public Population(FiniteStateMachine fsm) {
		genotypes = new ArrayList<Genotype>();
		goal = fsm;
	}

	public Population(int initialSize, FiniteStateMachine fsm) {
		genotypes = new ArrayList<Genotype>();
		goal = fsm;
		for (int i = 0; i < initialSize; ++i) {
			genotypes.add(new Genotype(goal.numInputs, (int)(Math.random()*20), goal.numOutputs, 0, 100));
		}
	}

	public void iterate() {
		// mutate the population
		for (Genotype genotype: genotypes) {
			genotype.mutate(0.20);
		}

		// evaluate the populattion
		for (Genotype genotype: genotypes) {
			genotype.evaluate(goal, 20, 50);
		}

		// sort the population
		sortByFitness();

		// replace least fit individuals
		for (int i = (int)((genotypes.size()-1) / 2); i < genotypes.size(); ++i) {
			int aIndex = (int)(Math.random() * (genotypes.size() / 2));
			int bIndex = (int)(Math.random() * (genotypes.size() / 2));
			Genotype a = genotypes.get(aIndex);
			Genotype b = genotypes.get(bIndex);
			genotypes.set(i, Genotype.crossover(a, b));
		}
	}

	public void iterate(int numThreads) {
		// mutate the population
		for (Genotype genotype: genotypes) {
			genotype.mutate(0.05);
		}

		try {
			// start the evaluation threads
			EvaluationWorker[] threads = new EvaluationWorker[numThreads];
			int batchStart = 0;
			for (int i = 0; i < numThreads; ++i) {
				Population pop = new Population(goal.clone());
				int batchEnd = batchStart + (int)(genotypes.size() / numThreads);
				if (i == numThreads-1) {
					batchEnd = genotypes.size();
				}
				for (int j = batchStart; j < batchEnd; ++j) {
					if (j >= genotypes.size()) {
						break;
					}
					pop.genotypes.add(genotypes.get(j));
				}
				batchStart = batchEnd;
				threads[i] = new EvaluationWorker(pop);
				threads[i].execute();
			}
			genotypes.clear();
	
			// wait for the evaluation to finish
			while (true) {
				Thread.sleep(SLEEP_TIME);
				boolean done = true;
				for (int i = 0; i < numThreads; ++i) {
					if (!threads[i].isDone()) {
						done = false;
					}
				}
				if (done) {
					break;
				}
			}

			// get evaluated genotypes
			for (int i = 0; i < numThreads; ++i) {
				Population pop = threads[i].get();
				for (Genotype genotype: pop.genotypes) {
					genotypes.add(genotype);
				}
			}
		}
		catch (Exception e) {
			System.out.println(e);
		}

		// sort the population
		sortByFitness();

		// replace least fit individuals
		for (int i = (int)((genotypes.size()-1) / 2); i < genotypes.size(); ++i) {
			int aIndex = (int)(Math.random() * (genotypes.size() / 2));
			int bIndex = (int)(Math.random() * (genotypes.size() / 2));
			Genotype a = genotypes.get(aIndex);
			Genotype b = genotypes.get(bIndex);
			genotypes.set(i, Genotype.crossover(a, b));
		}
	}

	public void sortByFitness() {
		Collections.sort(genotypes, new Comparator<Genotype>() {
			@Override
			public int compare(Genotype a, Genotype b) {
				if (a.fitness == b.fitness) {
					return 0;
				}
				if (a.fitness < b.fitness) {
					return -1;
				}
				return 1;
			}
		});
	}
}