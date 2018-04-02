package dcdnn;

import javax.swing.*;
import javax.swing.event.*;
import java.util.ArrayList;
import java.util.List;

public class PopulationWorker extends SwingWorker<Population, String> {

	protected static final long SLEEP_TIME = 1000;

	protected DCDNN dcdnn;
	protected MyAppendable myAppendable;
	protected FiniteStateMachine goal;

	public PopulationWorker(DCDNN d, MyAppendable appendable, FiniteStateMachine fsm) {
		dcdnn = d;
		myAppendable = appendable;
		goal = fsm.clone();
	}

	@Override
	protected Population doInBackground() throws Exception {
		Population population = new Population(6000, goal);
		int i = 0;
		while (true) {
			i++;
			population.iterate(400);
			if (population.genotypes.get(0).fitness == 0) {
				population.sortByFitness();
				if (population.genotypes.get(0).fitness == 0) {
					break;
				}
			}
			Thread.sleep(SLEEP_TIME);
			publish("\nIteration " + i + ": " + String.format("%.2f", population.genotypes.get(0).fitness));
		}
		population.sortByFitness();
		return population;
	}

	@Override
	protected void process(List<String> chunks) {
		for (String str: chunks) {
			myAppendable.append(str);
		}
	}

	@Override
	protected void done() {
		try {
			Population population = get();
			population.sortByFitness();
			Genotype genotype = population.genotypes.get(0);
			genotype.evaluate(population.goal, 80, 2);
			genotype.prune();
			myAppendable.append("\n\n" + genotype);
			System.out.println("\n" + genotype);
			myAppendable.append("\nDone");
			myAppendable.append("\nLoading network from results...");
			dcdnn.network = new Network(0, 0);
			dcdnn.view.network = dcdnn.network;
			dcdnn.loadNetworkFromGenotype(genotype);
			myAppendable.append("\nDone");
		}
		catch (Exception e) {
		}
	}
}