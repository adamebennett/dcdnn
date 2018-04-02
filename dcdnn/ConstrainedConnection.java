package dcdnn;

import org.chocosolver.solver.variables.IntVar;
import java.util.ArrayList;

public class ConstrainedConnection {

	public Network network;
	public Connection connection;
	public IntVar intVar;	
	public Path path;
	public ArrayList<Path> cycles; // all cycles which contain this connection
	
	public ConstrainedConnection(Network n, Connection conn, IntVar iv) {
		network = n;
		connection = conn;
		intVar = iv;		
		path = new Path(network);
		cycles = new ArrayList<Path>();
	}
}