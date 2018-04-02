package dcdnn;

import java.util.ArrayList;

public class AlleleConnection {

	public Allele from;
	public int fromIndex;
	public Allele to;
	public int toIndex;
	public ArrayList<AllelePath> cycles;
	public boolean isEntry;

	public AlleleConnection(Allele f, int fi, Allele t, int ti) {
		from = f;
		fromIndex = fi;
		to = t;
		toIndex = ti;
		cycles = new ArrayList<AllelePath>();
		isEntry = false;
	}

	public static void findCycles(ArrayList<AlleleConnection> connections) {
		for (AlleleConnection connection: connections) {
			connection.cycles = new ArrayList<AllelePath>();
			// recursively find any cycles that allele might be in
			connection.findCycles(new AllelePath(), connection, new ArrayList<AlleleConnection>(), new ArrayList<Allele>(), connections);
		}
	}

	public String toString() {
		String str = "(" + from.toString() + ")[" + fromIndex + "] -> (" + to.toString() + ")[" + toIndex + "], isEntry: " + isEntry;
		return str;
	}

	private void findCycles(AllelePath currentPath, AlleleConnection fromConn, ArrayList<AlleleConnection> visitedConnections, ArrayList<Allele> visitedAlleles, ArrayList<AlleleConnection> allConnections) {
		Allele prevAllele = fromConn.from;
		Allele nextAllele = fromConn.to;

		currentPath.path.add(fromConn);

		if (currentPath.isCycle()) {
			cycles.add(currentPath);
			return;
		}

		visitedAlleles.add(prevAllele);

		// find candidate connections
		ArrayList<AlleleConnection> prevConnections = new ArrayList<AlleleConnection>();
		for (int i = 0; i < prevAllele.getNumInputs(); ++i) {
			AlleleConnection c = AlleleConnection.findConnection(prevAllele.getInput(i), prevAllele.getOutputIndexOf(i), prevAllele, i, allConnections);
			if (!visitedConnections.contains(c)) {
				boolean crossesPath = false;
				for (AlleleConnection conn: currentPath.path) {
					if ((conn.from == c.from && conn.fromIndex == c.fromIndex) ||
						//(conn.from == c.to) ||
						//(conn.to == c.from) ||
						(conn.to == c.to) && conn.toIndex == c.toIndex) {
						// This check is to make sure a path isn't found which cycles over itself
						// It is intended to make sure only simple cycles are found
						// Imagine a cycle of cycles.
						// Also, it seems to prevent some proper cycles from being identified, which
						// is a problem and needs to be fixed.
						crossesPath = true;
					}
				}
				// make sure that c doesn't lead to a visited component
				// (only checking c.startNode since the next component (ie, component of c.endNode) is
				// prevComponent and therefore it is known that we have visited it)
				if (visitedAlleles.contains(c.from)) {
					crossesPath = true;
				}

				if (!crossesPath) {
					prevConnections.add(c);
				}
			}
		}

		
		// return if no valid candidates
		// (This is not actually needed, but I want to be explicit for now that the function returns
		// if there are no candidate connections)
		if (prevConnections.size() == 0) {
			return;
		}
						
		// continue down any avaiblable connections
		for (AlleleConnection candidate: prevConnections) {
			ArrayList<AlleleConnection> visitedConns = new ArrayList<AlleleConnection>(visitedConnections);
			visitedConns.add(candidate);
			ArrayList<Allele> visitedAs = new ArrayList<Allele>(visitedAlleles);
			AllelePath nextPath = new AllelePath(currentPath);
			findCycles(nextPath, candidate, visitedConns, visitedAs, allConnections);
		}
	}

	public static ArrayList<AlleleConnection> findConnections(Allele a, Allele b, ArrayList<AlleleConnection> connections) {
		ArrayList<AlleleConnection> toReturn = new ArrayList<AlleleConnection>();
		for (AlleleConnection conn: connections) {
			if (conn.from == a && conn.to == b) {
				toReturn.add(conn);
			}
		}
		return toReturn;
	}

	public static AlleleConnection findConnection(Allele a, int ai, Allele b, int bi, ArrayList<AlleleConnection> connections) {
		for (AlleleConnection conn: connections) {
			if (conn.from == a && conn.fromIndex == ai && conn.to == b && conn.toIndex == bi) {
				return conn;
			}
		}
		return null;
	}

	// sets isEntry to true for every AlleleConnection which leads into a cycle but isn't part of that cycle
	// used to figure out where to stop propogating signals through Allele network per time step
	public static void markEntries(ArrayList<AlleleConnection> connections) {
		for (AlleleConnection connection: connections) {
			connection.isEntry = false;
		}
		for (AlleleConnection c1: connections) {
			for (AlleleConnection c2: connections) {
				if (c1.to == c2.from) {
					if (c2.cycles.size() > 0) {
						if (c1.to.getNumInputs() > 1) {
							c1.isEntry = true;
							break;
						}
					}
				}
			}
		}
	}

	public static boolean leadsToOutput(AlleleConnection conn, ArrayList<AlleleConnection> visited, ArrayList<AlleleConnection> connections, Genotype genotype) {
		// base case 1: conn leads directly to output
		if (conn.to.index >= genotype.numInputs && conn.to.index < genotype.numInputs + genotype.numOutputs) {
			return true;
		}

		// base case 2: conn leads to pruned allele
		if (conn.to.pruned) {
			return false;
		}

		// base case 3: conn has already been visited
		if (visited.contains(conn)) {
			return false;
		}

		visited.add(conn);

		// get all output connections for conn.to and test where they go
		for (AlleleConnection c: connections) {
			if (c.from == conn.to) {
				if (leadsToOutput(c, new ArrayList<AlleleConnection>(visited), connections, genotype)) {
					return true;
				}
			}
		}

		return false;
	}

}