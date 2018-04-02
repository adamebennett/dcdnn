package dcdnn;

import java.util.ArrayList;
import java.awt.Color;

public class GraphicalComponent {

	public Component component;
	public float x;
	public float y;
	public float width;
	public float height;
	public Color color;
	
	public GraphicalComponent(Component comp, float f1, float f2, float f3, float f4, Color c, ArrayList<GraphicalNode> graphicalNodes) {
		component = comp;
		x = f1;
		y = f2;
		width = f3;
		height = f4;
		color = c;
		
		int i = 0;
		for (Node n: component.nodes) {			
			float xComponent = -(float)Math.cos((i / (float)component.nodes.size()) * 360 * (Math.PI / 180f)) * (width * 0.4f);
			float yComponent = (float)Math.sin((i / (float)component.nodes.size()) * 360 * (Math.PI / 180f)) * (height * 0.4f);
			float nx = (float)(xComponent + (width / 2f) + x);
			float ny = (float)(yComponent + (height / 2f) + y);
			if (component.outputs.contains(n)) {
				graphicalNodes.add(new GraphicalNode(n, nx, ny, new Color(229,126,0))); // DARK ORANGE
			}
			else if (component.inputs.contains(n)) {
				graphicalNodes.add(new GraphicalNode(n, nx, ny, new Color(9,175,0))); // DARK GREEN
			}
			else {
				graphicalNodes.add(new GraphicalNode(n, nx, ny, Color.BLACK));
			}
			i++;
		}
	}

}