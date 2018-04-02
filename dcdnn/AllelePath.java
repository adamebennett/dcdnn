package dcdnn;

import java.util.ArrayList;

public class AllelePath {

	public ArrayList<AlleleConnection> path;

	public AllelePath() {
		path = new ArrayList<AlleleConnection>();
	}

	public AllelePath(AllelePath toCopy) {
		path = new ArrayList<AlleleConnection>(toCopy.path);
	}

	public boolean isCycle() {
		if (path.size() == 0) {
			return false;
		}
		Allele first = path.get(path.size()-1).from;
		Allele last = path.get(0).to;
		return (first == last);
	}

	public String toString() {
		String str = "AllelePath(" + isCycle() + "): ";
		for (int i = path.size() - 1; i >= 0; --i) {
			str += path.get(i).toString();
			if (i > 0) {
				str += ", ";
			}
		}
		str += "";
		return str;
	}
	
}