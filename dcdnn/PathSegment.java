package dcdnn;

public class PathSegment {

	ConstrainedConnection connection;
	Component component;
	
	public PathSegment(ConstrainedConnection conn, Component comp) {
		connection = conn;
		component = comp;
	}
	
	public String toString() {
		return " <- " + component.label;
	}
	
}