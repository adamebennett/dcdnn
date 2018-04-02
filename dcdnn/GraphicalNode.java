package dcdnn;

import java.awt.Color;

public class GraphicalNode {

	public Node node;
	public float x;
	public float y;
	public Color color;
	
	public GraphicalNode(Node n, float f1, float f2, Color c) {
		node = n;
		x = f1;
		y = f2;
		color = c;
	}

}