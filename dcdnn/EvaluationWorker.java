package dcdnn;

import javax.swing.*;
import javax.swing.event.*;
import java.util.ArrayList;
import java.util.List;

public class EvaluationWorker extends SwingWorker<Population, Void> {

	Population population;

	public EvaluationWorker(Population p) {
		population = p;
	}

	@Override
	protected Population doInBackground() throws Exception {
		// evaluate the populattion
		for (Genotype genotype: population.genotypes) {
			genotype.evaluate(population.goal, 150, 2);
		}

		return population;
	}

	@Override
	protected void done() {
	}
}