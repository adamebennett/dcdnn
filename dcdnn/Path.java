package dcdnn;

import java.util.*;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.IntVar;

public class Path {

	Network network;
	ArrayList<PathSegment> path;
	
	public Path(Network n) {
		network = n;
		path = new ArrayList<PathSegment>();
	}
	
	public Path(Path toCopy) {
		network = toCopy.network;
		path = new ArrayList<PathSegment>(toCopy.path);
	}
	
	public int getStaticLength() {
		int total = 0;
		for (PathSegment segment: path) {
			total += segment.component.getTotalDelay();
		}
		return total;
	}
	
	public IntVar getScaledLength(Model model) {
		IntVar[] scaledLengths = new IntVar[path.size()];
		for (int i = 0; i < path.size(); ++i) {
			scaledLengths[i] = model.intVar(0, ConstraintSolver.MAX_SCALE);
			model.times(path.get(i).component.scale, path.get(i).component.getTotalDelay(), scaledLengths[i]).post();
		}
		IntVar total = model.intVar(0, ConstraintSolver.MAX_SCALE * path.size());
		model.sum(scaledLengths, "=", total).post();
		return total;
	}
	
	public IntVar[] getVars(Model model) {
		int numVars = path.size() + 1;
		IntVar[] vars = new IntVar[numVars];
		for (int i = 0; i < path.size(); ++i) {
			vars[i] = path.get(i).connection.intVar;
		}
		IntVar scaledLength = getScaledLength(model);
		vars[numVars - 1] = scaledLength;
		return vars;
	}
	
	public boolean isCycle() {
		if (path.size() == 0) {
			return false;
		}
		Component component1 = network.findComponentOfNode(path.get(path.size() - 1).connection.connection.startNode);
		Component component2 = network.findComponentOfNode(path.get(0).connection.connection.endNode);
		return (component1 == component2);
	}
	
	public Path getSubPathInCycle() {
		// We only care about the section from the first segment in the list (last segment traversed) to the
		// first point where outside signals are introduced to the cycle. Other sections of the same cycle
		// can be dealt with in other Paths, since a Path is stored from every segment in every cycle.
		
		// return null if this Path is not a cycle
		if (!isCycle()) {
			return null;
		}
		
		// iterate over the segments and build a path representing the periodic section starting from
		// the first segment of this path
		Path subPath = new Path(network);
		// NEED TO SEARCH FORWARD <<AND>> BACKWARD FOR INSERTION POINTS TO THE CYCLE, RETURN PART OF CYCLE IN BETWEEN
		// ^^ I don't think we do (but check anyways, when there is more time)
		
		PathSegment currentSegment = path.get(0); // Since we are starting at path[0], we are missing parts of the cycle!
		//FIX THIS!!!!!!!!!!!!!
		// ^^ Uh, not sure if this is true. Things <<seem>> to be working alright as is

		for (PathSegment segment: path) {
			subPath.addSegment(segment);
			// check for connections from outside the cycle
			for (Node input: segment.component.inputs) {
				for (Connection conn: input.inputs) {
					Component prevComponent = network.findComponentOfNode(conn.startNode);
					if (!containsComponent(prevComponent)) {
						return subPath;
					}
				}
			}
		}
		return subPath;
	}
	
	public ArrayList<Path> findPeriodicSectionsOfCycle() {
		// this function only makes sense for cycles
		if (!isCycle()) {
			return null;
		}

		//System.out.println(this);
		
		ArrayList<Path> sections = new ArrayList<Path>();
		
		// basic corner case
		if (path.size() <= 1) {
			sections.add(this);
			System.out.println("blarg");
			return sections;
		}
				
		// find starting segment
		int currentIndex = 0;
		int startingIndex = -1;		
		while (currentIndex < path.size()) {
			// check for connections from outside the cycle
			for (Node input: path.get(currentIndex).component.inputs) {
				for (Connection conn: input.inputs) {
					Component prevComponent = network.findComponentOfNode(conn.startNode);
					if (!containsComponent(prevComponent)) {
						startingIndex = currentIndex;
						break;
					}
				}
				if (startingIndex >= 0) {
					break;
				}
			}
			// break if a starting segment has been found
			if (startingIndex >= 0) {
				break;
			}
			currentIndex += 1;
		}
		
		// in the case of a cycle with no inputs
		if (startingIndex == -1) {
			sections.add(this);
			System.out.println("asdasd");
			return sections;
		}
		
		// break this cycle into periodic segments, starting from starting segment
		currentIndex = startingIndex + 1;
		if (currentIndex == path.size()) {
			currentIndex = 0;
		}
		if (currentIndex == -1) { // for testing, so we can go backwards or forwards
			currentIndex = path.size() - 1;
		}
		Path section = new Path(network);
		int iterations = 0;
		while (iterations <= path.size()) {
			section.addSegment(path.get(currentIndex));			
			// check for connections from outside the cycle
			boolean foundExternalInput = false;
			for (Node input: path.get(currentIndex).component.inputs) {
				for (Connection conn: input.inputs) {
					Component prevComponent = network.findComponentOfNode(conn.startNode);
					if (!containsComponent(prevComponent) && !foundExternalInput) {
						sections.add(section);
						section = new Path(network);
						foundExternalInput = true;
						break;
					}
				}
				if (foundExternalInput) {
					break;
				}
			}
			
			// increment currentIndex and iterations
			iterations += 1;
			currentIndex += 1;
			if (currentIndex == path.size()) {
				currentIndex = 0;
			}
			if (currentIndex == -1) { // for testing, so we can go backwards or forwards
				currentIndex = path.size() - 1;
			}
		}
		
		return sections;
	}
	
	public boolean containsComponent(Component component) {
		for (PathSegment segment: path) {
			if (segment.component == component) {
				return true;
			}
		}
		return false;
	}
	
	public void addSegment(ConstrainedConnection conn, Component comp) {
		path.add(new PathSegment(conn, comp));
	}
	
	public void addSegment(PathSegment segment) {
		path.add(segment);
	}
	
	public void append(Path toAppend) {
		for (PathSegment segment: toAppend.path) {
			path.add(segment);
		}
	}

	// returns true if this is a subsection of cycle
	public boolean isSubsectionOfCycle(Path cycle) {
		if (!cycle.isCycle()) {
			return false; // cycle isn't a cycle
		}
		if (path.size() > cycle.path.size()) {
			return false; // subsection cannot be larger than cycle
		}
		// find where (if anywhere) this starts in cycle
		int start = -1;
		for (int i = 0; i < cycle.path.size(); ++i) {
			if (path.get(0).component == cycle.path.get(i).component) {
				start = i;
				break;
			}
		}
		// if start wasn't found in cycle then this isn't a subsection
		if (start == -1) {
			return false;
		}
		// make sure all of this is contained in cycle
		int i = start;
		for (PathSegment segment: path) {
			if (segment.component != cycle.path.get(i).component) {
				return false;
			}
			++i;
			if (i >= cycle.path.size()) {
				i = 0;
			}
		}
		return true;
	}
	
	public String toString() {
		String pathString = "";
		pathString += "Path (segments = " + path.size();
		pathString += ", isCycle = " + isCycle();
		pathString += "): ";
		if (path.size() > 0) {
			pathString += network.findComponentOfNode(path.get(0).connection.connection.endNode).label;
			for (PathSegment segment: path) {
				pathString += segment.toString();
			}
		}
		return pathString;
	}

}