package dcdnn;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.IntVar;
import java.util.ArrayList;

public class ConstraintSolver {
	
	public static int MAX_DELAY = 1000000;
	public static int MAX_SCALE = 1000;
	
	public static void adjustDelayLengths(DCDNN dcdnn, Network network) {
		adjustDelayLengths(dcdnn, network, new ArrayList<Component>());
	}

	public static void adjustDelayLengths(DCDNN dcdnn, Network network, ArrayList<Component> outputComponents) {
		Model model = new Model();
		
		// initialize component scale
		for (Component component: network.components) {
			component.scale = model.intVar(1, MAX_SCALE);
		}

		// initialize component connections
		int currLabel = 0;
		ArrayList<ConstrainedConnection> componentConnections = new ArrayList<ConstrainedConnection>();
		for (Component component: network.components) {
			for (Node node: component.inputs) {
				for (Connection connection: node.inputs) {
					IntVar var = model.intVar(("C" + currLabel), 1, MAX_DELAY);
					componentConnections.add(new ConstrainedConnection(network, connection, var));
					currLabel++;
				}
			}
		}		
		
		// find all cycles of components in the network
		for (ConstrainedConnection cc: componentConnections) {
			ArrayList<Connection> visitedConnections = new ArrayList<Connection>();
			ArrayList<Component> visitedComponents = new ArrayList<Component>();
			Component prevComponent = network.findComponentOfNode(cc.connection.startNode);
			Component nextComponent = network.findComponentOfNode(cc.connection.endNode);
			findCycles(cc, new Path(network), network, cc, visitedConnections, visitedComponents, componentConnections);
		}
				
		System.out.println();
		System.out.println("Cycles:");
		for (ConstrainedConnection cc: componentConnections) {
			for (Path path: cc.cycles) {
				System.out.println(path);
			}
		}
		System.out.println();
		
		// calculate component connection paths
		for (ConstrainedConnection cc: componentConnections) {
			/*System.out.println();
			System.out.println(findComponentOfNode(cc.connection.startNode, network.components).label);
			System.out.println("to");
			System.out.println(findComponentOfNode(cc.connection.endNode, network.components).label);*/
			ArrayList<Connection> visitedConnections = new ArrayList<Connection>();
			ArrayList<Component> visitedComponents = new ArrayList<Component>();
			Component prevComponent = network.findComponentOfNode(cc.connection.startNode);
			Component nextComponent = network.findComponentOfNode(cc.connection.endNode);
			for (Node inNode: nextComponent.inputs) {
				for (Connection connection: inNode.inputs) {
					visitedConnections.add(connection);
				}
			}
			findPath(cc, network, cc, visitedConnections, visitedComponents, componentConnections);
		}
		
		System.out.println();
		System.out.println("Paths:");
		for (ConstrainedConnection cc: componentConnections) {
			System.out.println(cc.path);
		}
		System.out.println();
		
		// implement constraints
		for (Component component: network.components) {
			ArrayList<ConstrainedConnection> inputs = new ArrayList<ConstrainedConnection>();
			for (Node node: component.inputs) {
				for (Connection connection: node.inputs) {
					inputs.add(getConstrainedConnection(connection, componentConnections));
				}
			}
			for (int i = 0; i < inputs.size(); ++i) {
				for (int j = 0; j < inputs.size(); ++j) {
					if (i != j) {
						IntVar[] iPathArray = inputs.get(i).path.getVars(model);
						IntVar[] jPathArray = inputs.get(j).path.getVars(model);
						IntVar sumValue = model.intVar(0, MAX_DELAY);
						model.sum(iPathArray, "=", sumValue).post();
						model.sum(jPathArray, "=", sumValue).post();
					}
				}
			}
		}
		// Need to make sure all cyclical paths have the same period
		// Actually, make sure that all periodic sections of cycles have same period
		System.out.println("Periodic sections of cycles:");
		IntVar globalPeriod = model.intVar(1,MAX_DELAY);
		for (ConstrainedConnection recurrentCC: componentConnections) {
			for (Path path: recurrentCC.cycles) {
				ArrayList<Path> periodicSections = path.findPeriodicSectionsOfCycle();
				for (Path section: periodicSections) {
					System.out.println(section);
					IntVar[] iPathArray = section.getVars(model);
					model.sum(iPathArray, "=", globalPeriod).post();
				}
			}
		}
		System.out.println();

		// Make sure all components with internal period sections of cycles are scaled such that they match the global period
		for (Component component: network.components) {
			if (component.getInternalPeriod() > 0) {
				model.times(component.scale, component.getInternalPeriod(), globalPeriod).post();
			}
		}

		// Make sure all output paths have the same path delay:
		// (need to get list of output components from parameter, set path length constraints to equal each other)
		IntVar outputPathValue = model.intVar(0, MAX_DELAY);
		for (Component component: outputComponents) {
			for (Node node: component.inputs) {
				for (Connection conn: node.inputs) {
					ConstrainedConnection cc = getConstrainedConnection(conn, componentConnections);
					IntVar[] outPath = cc.path.getVars(model);
					model.sum(outPath, "=", outputPathValue).post();
				}
			}
		}

		
		// find solution with minimum total delay length
		ArrayList<IntVar> allVars = new ArrayList<IntVar>();
		for (ConstrainedConnection cc: componentConnections) {
			allVars.add(cc.intVar);
		}
		for (Component component: network.components) {
			allVars.add(component.scale);
			// make sure only recurrent components are scaled
			if (component.internalPeriod == -1) {
				model.arithm(component.scale, "=", 1).post();
			}
		}
		IntVar[] allVarsArray = toIntVarArray(allVars);
		IntVar totalDelay = model.intVar(0, MAX_DELAY);
		model.sum(allVarsArray, "=", totalDelay).post();		
		model.setObjective(model.MINIMIZE, totalDelay);

		// so cycles can contain recurrent components
		model.arithm(outputPathValue, "=", globalPeriod).post();
		
		// solve for delay lengths
		System.out.println("Running Constraint Solver...");
		model.getSolver().solve();
		System.out.println("Done.\n");

		// determine if network is recurrent or not
		network.isRecurrent = false;
		for (Component component: network.components) {
			if (component.getInternalPeriod() != -1) {
				network.isRecurrent = true;
				break;
			}
		}
		for (ConstrainedConnection cc: componentConnections) {
			if (cc.cycles.size() > 0) {
				network.isRecurrent = true;
				break;
			}
		}


		if (network.isRecurrent == true) {
			network.period = globalPeriod.getValue();
			network.delay = 0;
		}
		else {
			network.period = -1;
			network.delay = outputPathValue.getValue();
		}

		dcdnn.addOutput("\nPeriod: " + network.period);
		dcdnn.addOutput("\nDelay: " + network.delay);

		// very hacky: need the least common multiple of the numbers of periodic sections of all cycles for DCDNN.clockify()
		// Since we already have periodic sections of cycles calculated, I'm going to do that here
		// (but it doesn't really belong here. I have to get this done today, though)
		int lcmForClockify = ConstraintSolver.periodicSectionsLCM(network, componentConnections);
		dcdnn.addOutput("\nClockified: " + lcmForClockify);
		network.clockified = lcmForClockify;

		// calculate the shift for this network if it were to be used as a component
		//dcdnn.addOutput("\nThroughput Time: " + network.calculateTotalDelay() + "\n");
		
		// set network to use solved delay lengths
		System.out.println("Connections:");
		for (ConstrainedConnection cc: componentConnections) {
			System.out.println(network.findComponentOfNode(cc.connection.startNode).label + " -> " + network.findComponentOfNode(cc.connection.endNode).label + ": " + cc.intVar.getValue());
			cc.connection.resetDelayLength(cc.intVar.getValue());
		}

		// scale recurrent components by scale
		System.out.println("\nComponent scales:");
		for (Component component: network.components) {
			System.out.println(component.label + ": " + component.scale.getValue());
			//if (component.internalPeriod != -1) {
				for (Node n: component.nodes) {
					for (Connection conn: n.inputs) {
						// make sure conn isn't from component's outputs to inputs (technically that would count as an external
						// connection, not an internal connection
						if (!(component.outputs.contains(conn.startNode) && component.inputs.contains(conn.endNode))) {
							Component prevComp = network.findComponentOfNode(conn.startNode);
							Component nextComp = network.findComponentOfNode(conn.endNode);
							if (prevComp == component && nextComp == component) {
								conn.resetDelayLength(component.scale.getValue() * conn.delayLength);
							}
						}
					}
				}
			//}
		}
		System.out.println("Total Delay: " + totalDelay.getValue());
		System.out.println();
	}
	
	private static IntVar[] toIntVarArray(ArrayList<IntVar> vars) {
		IntVar[] intVars = new IntVar[vars.size()];
		for (int i = 0; i < intVars.length; ++i) {
			intVars[i] = vars.get(i);
		}
		return intVars;
	}
	
	private static void findPath(ConstrainedConnection cc, Network network, ConstrainedConnection fromConn, ArrayList<Connection> visitedConnections, ArrayList<Component> visitedComponents, ArrayList<ConstrainedConnection> allCompConns) {
		Component prevComponent = network.findComponentOfNode(fromConn.connection.startNode);
		Component nextComponent = network.findComponentOfNode(fromConn.connection.endNode);
		
		// checks if prevComponent is a component in a cycle
		boolean isPrevComponentInCycle = false;
		ConstrainedConnection connectionInCycle = null;
		
		for (Node node: prevComponent.outputs) {
			for (Connection conn: node.outputs) {
				if (getConstrainedConnection(conn, allCompConns) == null) {
					continue;
				}
				if (getConstrainedConnection(conn, allCompConns).cycles.size() > 0) {
					connectionInCycle = getConstrainedConnection(conn, allCompConns);
					isPrevComponentInCycle = true;
					break;
				}
			}
		}
		
		if (isPrevComponentInCycle) { // if the connection is part of a cycle
			Path tail = new Path(network);
			tail.addSegment(fromConn, prevComponent);
			
			//if (!isPrevComponentRecurrent) { // only need to do this if prevComponent isn't recurrent (since cycles can't contain other cycles)
				Path subPath = connectionInCycle.cycles.get(0).getSubPathInCycle();
				// Note how the first segment is skipped (since we have fromConn and prevComponent instead)
				for (int i = 1; i < subPath.path.size(); ++i) {
					tail.addSegment(subPath.path.get(i));
				}
			//}

			cc.path.append(tail);

			return;
		}
		else {			
			// Add to fromConn's path
			cc.path.addSegment(fromConn, prevComponent);
		}

		visitedComponents.add(prevComponent);
		
		// Debug code:		
		/*System.out.println(cc.path.getStaticLength());
		for (PathSegment ps: cc.path.path) {
			Component p = network.findComponentOfNode(ps.connection.connection.startNode);
			Component n = network.findComponentOfNode(ps.connection.connection.endNode);
			System.out.println(p.label + " -> " + n.label);
		}
		System.out.println();*/
		
		// find candidate connections:
		ArrayList<ConstrainedConnection> prevConnections = new ArrayList<ConstrainedConnection>();
		for (Node node: prevComponent.inputs) {
			for (Connection c: node.inputs) {
				if (!visitedConnections.contains(c)) {
				//if (!cc.path.contains(getConstrainedConnection(c, allCompConns))) {
					if (!visitedComponents.contains(network.findComponentOfNode(c.startNode))) {
						ConstrainedConnection candidate = getConstrainedConnection(c, allCompConns);
						prevConnections.add(candidate);
					}
				//}
				}
			}
		}
		
		// return if no valid candidates
		if (prevConnections.size() == 0) {
			return;
		}
					
		// Add alternative components that could have been chosen to visited
		for (Node inNode: prevComponent.inputs) {
			for (Connection connection: inNode.inputs) {
				visitedConnections.add(connection);
			}
		}
						
		// continue down 0th valid candidate:
		findPath(cc, network, prevConnections.get(0), visitedConnections, visitedComponents, allCompConns);
	}
	
	// is visitedComponents being used?
	private static void findCycles(ConstrainedConnection cc, Path currentPath, Network network, ConstrainedConnection fromConn, ArrayList<Connection> visitedConnections, ArrayList<Component> visitedComponents, ArrayList<ConstrainedConnection> allCompConns) {
		Component prevComponent = network.findComponentOfNode(fromConn.connection.startNode);
		Component nextComponent = network.findComponentOfNode(fromConn.connection.endNode);
		
		// cycles can totally contain other cycles!

		// Add to current path
		currentPath.addSegment(fromConn, prevComponent);
		
		// base case (currentPath is a cycle):
		if (currentPath.isCycle()) {
			cc.cycles.add(currentPath);
			return;
		}
		
		visitedComponents.add(prevComponent);
		
		// Debug code:		
		/*System.out.println(cc.path.getStaticLength());
		for (PathSegment ps: cc.path.path) {
			Component p = network.findComponentOfNode(ps.connection.connection.startNode);
			Component n = network.findComponentOfNode(ps.connection.connection.endNode);
			System.out.println(p.label + " -> " + n.label);
		}
		System.out.println();*/
		
		// find candidate connections:
		ArrayList<ConstrainedConnection> prevConnections = new ArrayList<ConstrainedConnection>();
		for (Node node: prevComponent.inputs) {
			for (Connection c: node.inputs) {
				if (!visitedConnections.contains(c)) {
					boolean crossesPath = false;
					for (PathSegment segment: currentPath.path) {
						if (segment.connection.connection.startNode == c.startNode ||
							//segment.connection.connection.startNode == c.endNode || // Are these two lines doing anything?
							//segment.connection.connection.endNode == c.startNode || // Yes, a component might contain only one node
							segment.connection.connection.endNode == c.endNode) {
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
					if (visitedComponents.contains(network.findComponentOfNode(c.startNode))) {
						crossesPath = true;
					}
					if (!crossesPath) {
						ConstrainedConnection candidate = getConstrainedConnection(c, allCompConns);
						prevConnections.add(candidate);
					}
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
		for (ConstrainedConnection candidate: prevConnections) {
			ArrayList<Connection> visitedConns = new ArrayList<Connection>(visitedConnections);
			visitedConns.add(candidate.connection);
			ArrayList<Component> visitedComps = new ArrayList<Component>(visitedComponents);
			Path nextPath = new Path(currentPath);
			findCycles(cc, nextPath, network, candidate, visitedConns, visitedComps, allCompConns);
		}
	}	
	
	private static ConstrainedConnection getConstrainedConnection(Connection c, ArrayList<ConstrainedConnection> allCompConns) {
		for (ConstrainedConnection cc: allCompConns) {
			if (cc.connection == c) {
				return cc;
			}
		}
		return null;
	}

	// Calculates the lowest common multiple of the numbers of periodic sections in all cycles
	// Also includes the clockified attribute of Components (if any)
	private static int periodicSectionsLCM(Network network, ArrayList<ConstrainedConnection> compConns) {
		ArrayList<Integer> nums = new ArrayList<Integer>();

		// get values for components
		for (Component component: network.components) {
			if (component.getClockified() != -1) {
				nums.add(new Integer(component.getClockified()));
			}
		}

		// get values for cycles
		for (ConstrainedConnection cc: compConns) {
			for (Path cycle: cc.cycles) {
				int numSections = cycle.findPeriodicSectionsOfCycle().size();
				if (numSections > 1) {
					numSections -= 1; // I think findPeriodicSectionsOfCycle overlaps by one (except in the simple case where there is a single periodic section)
				}
				nums.add(new Integer(numSections));
				
			}
		}

		// return 1 if there are no cycles or recurrent components
		if (nums.size() == 0) {
			return 1;
		}

		// find LCM of nums
		return (int)lcm(toLongArray(nums));
	}
	
	private static int[] toIntArray(ArrayList<Integer> vars) {
		int[] ints = new int[vars.size()];
		for (int i = 0; i < ints.length; ++i) {
			ints[i] = vars.get(i).intValue();
		}
		return ints;
	}
	
	private static long[] toLongArray(ArrayList<Integer> vars) {
		long[] longs = new long[vars.size()];
		for (int i = 0; i < longs.length; ++i) {
			longs[i] = (long)vars.get(i).intValue();
		}
		return longs;
	}

	// The next four functions are taken (with some modification) from:
	// https://stackoverflow.com/questions/4201860/how-to-find-gcd-lcm-on-a-set-of-numbers
	private static long gcd(long a, long b)
	{
    	while (b > 0)
    	{
        	long temp = b;
        	b = a % b; // % is remainder
        	a = temp;
    	}
    	return a;
	}

	private static long gcd(long[] input)
	{
    	long result = input[0];
    	for(int i = 1; i < input.length; i++) result = gcd(result, input[i]);
    	return result;
	}

	private static long lcm(long a, long b)
	{
		long divisor = gcd(a, b);
		if (divisor == 0) {
			divisor = 1;
		}
	    return a * (b / divisor);
	}
	
	private static long lcm(long[] input)
	{
	    long result = input[0];
	    for(int i = 1; i < input.length; i++) result = lcm(result, input[i]);
	    return result;
	}
	
}