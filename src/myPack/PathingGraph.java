package myPack;

import java.awt.Point;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.AStarShortestPath;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

public class PathingGraph {

	public Graph<Point, DefaultEdge> realGraph = new SimpleGraph<>(DefaultEdge.class);
	public Point[][] refArray = new Point[Main.tiles.length][Main.tiles[0].length];
	public Point rootPoint = new Point(-1, -1);
    AStarShortestPath<Point, DefaultEdge> shorty = new AStarShortestPath<Point, DefaultEdge>(realGraph, new Heu());
	
	public void setVertex(int x, int y, boolean passable) {
		if(passable) {
			if(rootPoint.x == -1) {
				rootPoint = new Point(x, y);
				refArray = new Point[1][1];
			}
			else {
				if(x < rootPoint.x || y < rootPoint.y) {
					int xGrow = Math.max(rootPoint.x - x, 0);
					int yGrow = Math.max(rootPoint.y - y, 0);
					Point newRoot = new Point(rootPoint.x - xGrow, rootPoint.y - yGrow);
					Point[][] newArray = new Point[refArray.length + xGrow][refArray[0].length + yGrow];
					for (int i = 0; i < refArray.length; i++) {
						for (int j = 0; j < refArray[0].length; j++) {
							newArray[i + xGrow][j + yGrow] = refArray[i][j];
						}
					}
					refArray = newArray;
					rootPoint = newRoot;
				}
				if(x > rootPoint.x + (refArray.length - 1) || y > rootPoint.y + (refArray[0].length - 1)) {
					int xGrow = Math.max(x - ((refArray.length - 1) + rootPoint.x), 0);
					int yGrow = Math.max(y - ((refArray[0].length - 1) + rootPoint.y), 0);
					Point[][] newArray = new Point[refArray.length + xGrow][refArray[0].length + yGrow];
					for (int i = 0; i < refArray.length; i++) {
						for (int j = 0; j < refArray[0].length; j++) {
							newArray[i][j] = refArray[i][j];
						}
					}
					refArray = newArray;
				}
			}
			Point checkPoint = new Point(x - rootPoint.x, y - rootPoint.y);
			//new vertex
			if(refArray[checkPoint.x][checkPoint.y] == null) {
				Point newV = new Point(x, y);
				realGraph.addVertex(newV);
				refArray[checkPoint.x][checkPoint.y] = newV;

				//connect to all existing neighbbors (not null, within bounds)
				//commented chunks turn on diagonal mode, which isn't quite done
				Point[] offsets = {new Point(0, 1), new Point(0, -1), new Point(1, 0), new Point(-1, 0)
						//additional points for diagonal pathing
						/*, new Point(-1, -1), new Point(-1, 1), new Point(1, 1), new Point(1, -1)/**/
						};
				for (int k = 0; k < offsets.length; k++) {
					int checkx = checkPoint.x + offsets[k].x;
					int checky = checkPoint.y + offsets[k].y;
					if(inBounds(checkx, checky, true)) {
						if(refArray[checkx][checky] != null) {
							//this weird conditional is for diagonal connectors
							/*if(Math.abs(offsets[k].x) + Math.abs(offsets[k].y) != 2 || 
									(refArray[checkx][checkPoint.y] != null && refArray[checkPoint.x][checky] != null))/**/
								realGraph.addEdge(newV, refArray[checkx][checky]);
						}
					}
				}
				//more diagonal goofiness
				/*if(inBounds(checkPoint.x - 1, checkPoint.y - 1, true)) {
					if(refArray[checkPoint.x - 1][checkPoint.y] != null && 
							refArray[checkPoint.x][checkPoint.y - 1] != null &&
								refArray[checkPoint.x - 1][checkPoint.y - 1] != null) {
						realGraph.addEdge(refArray[checkPoint.x - 1][checkPoint.y], refArray[checkPoint.x][checkPoint.y - 1]);
					}
				}*/
			}
		}
		//if we're setting the point impassable (and it's within bounds)
		else if(inBounds(x, y, false)) {
			Point checkPoint = new Point(x - rootPoint.x, y - rootPoint.y);
			if(refArray[checkPoint.x][checkPoint.y] != null) {
				realGraph.removeVertex(refArray[checkPoint.x][checkPoint.y]);
				refArray[checkPoint.x][checkPoint.y] = null;
			}
		}
	}
	
	public GraphPath<Point, DefaultEdge> getPath(int x1, int y1, int x2, int y2) {
		//localize the coordinates for refArray
		x1 -= rootPoint.x;
		x2 -= rootPoint.x;
		y1 -= rootPoint.y;
		y2 -= rootPoint.y;
		if(inBounds(x1, y1, true) && inBounds(x2, y2, true)) {
			if(refArray[x1][y1] != null && refArray[x2][y2] != null) {
				return shorty.getPath(refArray[x1][y1], refArray[x2][y2]);
			}
		}
		return null;
	}
	
	//returns whether or not a coordinate pair is valid for refArray. 
	//Set "local" true to use local coordinates or false to use global coordinates. 
	public boolean inBounds(int x, int y, boolean local) {
		if(local) {
			x += rootPoint.x;
			y += rootPoint.y;
		}
		if(x >= rootPoint.x && x < rootPoint.x + refArray.length && 
				y >= rootPoint.y && y < rootPoint.y + refArray[0].length){
			return true;
		}
		else return false;
	}
	
}
