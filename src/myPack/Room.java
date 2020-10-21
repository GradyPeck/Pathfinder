package myPack;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;

import org.jgrapht.GraphPath;
import org.jgrapht.graph.DefaultEdge;

import javafx.scene.paint.Color;

public class Room {
	public HashMap<Room, ArrayList<Door>> doors = new HashMap<Room, ArrayList<Door>>();
	public PathingGraph myGraph = new PathingGraph();
	static int lastID = 99;
	public int id;
	public Color chroma;
	
	//returns a door object at these global coordinates, if owned by this room
	public Door getDoorByLocation(int x, int y) {
		for (Room r: doors.keySet()) {
			for (Door d: doors.get(r)) {
				if (d.x == x && d.y == y) return d;
			}
		}
		return null;
	}
	
	//requests a path between the target coordinates
	public Path getPath(int x1, int y1, int x2, int y2) {
		GraphPath<Point, DefaultEdge> gp = myGraph.getPath(x1, y1, x2, y2);
		Path myReturn;
		if(gp != null) myReturn = new Path(gp.getLength(), gp.getVertexList());
		else myReturn = null;
		return myReturn;
	}
	
	public void refreshDoorPaths() {
		for (Room r: doors.keySet()) {
			for (Door d: doors.get(r)) {
				//yup, nested iteration, sorry
				for (Room ro: doors.keySet()) {
					for (Door dor: doors.get(ro)) {
						if(dor.equals(d) == false) {
							d.paths.put(dor, getPath(d.x, d.y, dor.x, dor.y));
						}
					}
				}
			}
		}
	}
	
	//Door Handling Methods
	//TODO rewrite these to get rid of unnecessary iterations
	
	//creates a new door from this room to a target room at an arbitrary location
	//currently unused
	public void addDoor (Room destRoom, int x, int y) {
		ArrayList<Door> destDoors = new ArrayList<Door>();
		if(doors.get(destRoom) != null) destDoors.addAll(doors.get(destRoom));
		destDoors.add(new Door(x, y));
		doors.put(destRoom, destDoors);
		myGraph.setVertex(x, y, true);
	}
	
	//overload that takes an existing door object instead of location - used by createDoor and transferDoor
	public void addDoor (Room destRoom, Door d) {
		ArrayList<Door> destDoors = new ArrayList<Door>();
		if(doors.get(destRoom) != null) destDoors.addAll(doors.get(destRoom));
		destDoors.add(d);
		doors.put(destRoom, destDoors);
		myGraph.setVertex(d.x, d.y, true);
	}
	
	//initiates the full process of creating a door-pair
	public void createDoor (Room destRoom, int x, int y) {
		Door newDoor = new Door(x, y);
		Door portalpal = new Door(x, y);
		newDoor.portal = portalpal;
		portalpal.portal = newDoor;
		addDoor(destRoom, newDoor);
		destRoom.addDoor(this, portalpal);
		Main.tiles[x][y] = 2;
		Main.changedTiles.add(new Point(x, y));
		refreshDoorPaths();
		destRoom.refreshDoorPaths();
	}
	
	//transfers a door from one room to another
	public void transferDoor (Door d, Room newRoom) {
		if(newRoom.equals(this)) return;
		for (Room dest: doors.keySet()) {
			ArrayList<Door> newList = new ArrayList<Door>();
			for (Door dor: doors.get(dest)) {
				if (dor.equals(d)) {
					if (newRoom.equals(dest)) deleteDoor(d);
					else {
						newRoom.addDoor(dest, d);
						dest.rerouteDoor(d.portal, newRoom);
					}
				}
				else newList.add(dor);
			}
			if(newList.containsAll(doors.get(dest)) == false) doors.put(dest, newList);
		}
		refreshNeighbors();
		myGraph.setVertex(d.x, d.y, false);
		newRoom.refreshDoorPaths();
	}
	
	public void transferAllDoors (Room newRoom) {
		for (Room r: doors.keySet()) {
			for (Door d: doors.get(r)) {
				transferDoor(d, newRoom);
			}
		}
	}
	
	//changes the destination room of a door
	public void rerouteDoor (Door d, Room newRoom) {
		ArrayList<Door> outerList = new ArrayList<Door>();
		for (Room dest: doors.keySet()) {
			ArrayList<Door> newList = new ArrayList<Door>();
			for (Door dor: doors.get(dest)) {
				if (dor.equals(d)) {
					ArrayList<Door> current;
					current = new ArrayList<Door>();
					if(doors.get(newRoom) != null) current.addAll(doors.get(newRoom));
					current.add(d);
					outerList = current;
				}
				else newList.add(dor);
			}
			if(newList.containsAll(doors.get(dest)) == false) doors.put(dest, newList);
		}
		//this is here to avoid a ConcurrentModificationException
		if(outerList.size() != 0) doors.put(newRoom, outerList);
	}
	
	//removes a door from this room without doing any other cleanup - used by deleteDoor
	public void removeDoor (Door d) {
		for (Room dest: doors.keySet()) {
			ArrayList<Door> newList = new ArrayList<Door>();
			for (Door dor: doors.get(dest)) {
				if (!dor.equals(d)) newList.add(dor);
			}
			if(newList.containsAll(doors.get(dest)) == false) doors.put(dest, newList);
		}
		myGraph.setVertex(d.x, d.y, false);
		refreshNeighbors();
	}
	
	//initiates the full process of deleting and cleaning up a door
	public void deleteDoor (Door d/*, boolean open*/) {
		//rewrite the door tile
		/*if(open) Main.tiles[d.x][d.y] = id;
		else*/ Main.tiles[d.x][d.y] = 1;
		Main.changedTiles.add(new Point(d.x, d.y));
		//remove the portal-partner
		for (Room dest: doors.keySet()) {
			ArrayList<Door> newList = new ArrayList<Door>();
			for (Door dor: doors.get(dest)) {
				if (dor.equals(d)) {
					dest.removeDoor(d.portal);
				}
				//the door itself is removed by simply failing to add it to the new list, as in other methods
				else newList.add(dor);
			}
			if(newList.containsAll(doors.get(dest)) == false) doors.put(dest, newList);
		}
		myGraph.setVertex(d.x, d.y, false);
		refreshNeighbors();
	}
	
	
	public void refreshNeighbors() {
		ArrayList<Room> toWhack = new ArrayList<Room>();
		for(Room r: doors.keySet()) {
			if(doors.get(r).size() == 0) toWhack.add(r);
		}
		for(int i = toWhack.size() - 1; i >= 0; i--) {
			doors.remove(toWhack.get(i));
		}
	}
	
	//Constructor Stuff
	
	//returns the next possible roomID and advances to the next one
	public static int nextID () {
		lastID++;
		return lastID;
	}
	
	//constructor for a one-tile room at the target coordinates
	public Room (int x, int y) {
		id = nextID();
		chroma = Main.randomColor();
		Main.pallette.put(id, chroma);
		Main.rooms.put(id, this);
		myGraph.setVertex(x, y, true);
	}
	
	//constructor for a room with the given PathingGraph
	public Room (PathingGraph graphIn) {
		id = nextID();
		chroma = Main.randomColor();
		Main.pallette.put(id, chroma);
		Main.rooms.put(id, this);
		myGraph = graphIn;
	}
	
}
