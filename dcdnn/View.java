package dcdnn;

import java.util.*;
import java.awt.*;
import javax.swing.*;

public class View extends JPanel {

	public int width;
	public int height;
	public Network network;
	public ArrayList<GraphicalNode> graphicalNodes;
	public ArrayList<GraphicalComponent> graphicalComponents;
	
	public View(int w, int h, Network n, ArrayList<GraphicalNode> g, ArrayList<GraphicalComponent> gc) {
		width = w;
		height = h;
		network = n;
		graphicalNodes = g;
		graphicalComponents = gc;
	}
	 
	public void refresh(int w, int h)
	{
		repaint();
		width = w;
		height = h;
	}
	 
	@Override
	public void paintComponent(Graphics graphics)
	{
		graphics.setColor(Color.WHITE);
		graphics.fillRect(0,0,width,height);
		if (network != null) {
			drawComponents(graphics);
			drawConnections(graphics);
			drawNodes(graphics); 
		}
	}
	
	public void drawConnections(Graphics graphics) {
		for (Connection connection: network.connections) {
			GraphicalNode startNode = getGraphicalNode(connection.startNode);
			Point startPoint = calculateNodePosition(startNode);
			GraphicalNode endNode = getGraphicalNode(connection.endNode);
			Point endPoint = calculateNodePosition(endNode);
			graphics.setColor(Color.LIGHT_GRAY);
			graphics.drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y);
		}
		for (Connection connection: network.connections) {
			GraphicalNode startNode = getGraphicalNode(connection.startNode);
			Point startPoint = calculateNodePosition(startNode);
			GraphicalNode endNode = getGraphicalNode(connection.endNode);
			Point endPoint = calculateNodePosition(endNode);
			for (int j = 0; j < connection.delayLength; ++j) {
				if (connection.signals[j] != 0) {
					if (connection.weight > 0) {
						graphics.setColor(Color.RED);
					}
					else if (connection.weight < 0) {
						graphics.setColor(Color.BLUE);
					}
					else {
						graphics.setColor(Color.LIGHT_GRAY);
					}
					float signalWidth = (endPoint.x - startPoint.x) / (float)connection.delayLength;
					float signalHeight = (endPoint.y - startPoint.y) / (float)connection.delayLength;
					graphics.drawLine((int)(startPoint.x + (j * signalWidth)), (int)(startPoint.y + (j * signalHeight)),
						(int)(startPoint.x + ((j + 1) * signalWidth)), (int)(startPoint.y + ((j + 1) * signalHeight)));
				}
			}
		}
	}
	
	public void drawNodes(Graphics graphics) {
		for (Node node: network.nodes) {
			GraphicalNode graphicalNode = getGraphicalNode(node);
			graphics.setColor(graphicalNode.color);
			Point position = calculateNodePosition(graphicalNode);
			graphics.drawOval(position.x - 3, position.y - 3, 6, 6);
		}
	}
	
	public GraphicalNode getGraphicalNode(Node node) {
		for (GraphicalNode graphicalNode: graphicalNodes) {
			if (graphicalNode.node == node) {
				return graphicalNode;
			}
		}
		return null;
	}
	
	public Point calculateNodePosition(GraphicalNode graphicalNode) {
		return new Point((int)(width * graphicalNode.x), (int)(height * graphicalNode.y));
	}
	
	public void drawComponents(Graphics graphics) {
		for (Component component: network.components) {
			GraphicalComponent graphicalComponent = getGraphicalComponent(component);
			graphics.setColor(graphicalComponent.color);
			int x = (int)(width * graphicalComponent.x);
			int y = (int)(height * graphicalComponent.y);
			int w = (int)(width * graphicalComponent.width);
			int h = (int)(height * graphicalComponent.height);
			graphics.drawRect(x, y, w, h);
			graphics.setColor(Color.BLACK);
			graphics.drawString(graphicalComponent.component.label, x, y);
		}
	}
	
	public GraphicalComponent getGraphicalComponent(Component component) {
		for (GraphicalComponent graphicalComponent: graphicalComponents) {
			if (graphicalComponent.component == component) {
				return graphicalComponent;
			}
		}
		return null;
	}
}