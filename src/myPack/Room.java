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
	
	//change to a new PathingGraph and add all your doors to it
	public void refreshGraph(PathingGraph graphIn) {
		myGraph = graphIn;
		for (Room r: doors.keySet()) {
			for (Door d: doors.get(r)) {
				myGraph.setVertex(d.x, d.y, true);
			}
		}
	}
	
	//regenerate all the door-to-door paths in this room
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
	
	//check if all of your neighbors are still valid - currently unused
	public void refreshNeighbors() {
		ArrayList<Room> toWhack = new ArrayList<Room>();
		for(Room r: doors.keySet()) {
			if(doors.get(r).size() == 0) toWhack.add(r);
		}
		for(int i = toWhack.size() - 1; i >= 0; i--) {
			doors.remove(toWhack.get(i));
		}
	}
	
	//check if a specific neighbor is valid - used extensively by door handling methods
	public void checkNeighbor(Room r) {
		if(doors.get(r).size() == 0) {
			doors.remove(r);
		}
	}
	
	//Door Handling Methods
	
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
		//rewrite the tile
		Main.tiles[x][y] = 2;
		Main.changedTiles.add(new Point(x, y));
		refreshDoorPaths();
		destRoom.refreshDoorPaths();
	}
	
	//transfers a door from one room to another
	public void transferDoor (Door d, Room newRoom) {
		Room checkTarget = null;
		if(newRoom.equals(this)) return;
		for (Room dest: doors.keySet()) {
			ArrayList<Door> newList = new ArrayList<Door>();
			for (Door dor: doors.get(dest)) {
				if (dor.equals(d)) {
					if (newRoom.equals(dest)) deleteDoor(dest, d);
					else {
						newRoom.addDoor(dest, d);
						dest.rerouteDoor(this, d.portal, newRoom);
					}
					checkTarget = dest;
				}
				else newList.add(dor);
			}
			if(newList.containsAll(doors.get(dest)) == false) doors.put(dest, newList);
		}
		if(checkTarget != null) checkNeighbor(checkTarget);
		myGraph.setVertex(d.x, d.y, false);
		newRoom.refreshDoorPaths();
	}
	
	//overload of transferDoor that avoids iterating - used by transferAllDoors
	public void transferDoor (Room dest, Door d, Room newRoom) {
		//ignore transfers to yourself
		if(newRoom.equals(this)) return;
		//remove doors that would become internal to one room
		if(newRoom.equals(dest)) deleteDoor(dest, d);
		//remove target door from current room
		removeDoor(dest, d);
		//add target door to new room
		newRoom.addDoor(dest, d);
	}
	
	public void transferAllDoors (Room newRoom) {
		for (Room r: doors.keySet()) {
			for (Door d: doors.get(r)) {
				transferDoor(r, d, newRoom);
			}
		}
	}
	
	//changes the destination room of a door (this version currently unused)
	public void rerouteDoor (Door d, Room newRoom) {
		Room checkTarget = null;
		ArrayList<Door> outerList = new ArrayList<Door>();
		for (Room dest: doors.keySet()) {
			ArrayList<Door> newList = new ArrayList<Door>();
			for (Door dor: doors.get(dest)) {
				if (dor.equals(d)) {
					checkTarget = dest;
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
		if(checkTarget != null) checkNeighbor(checkTarget);
	}
	
	//overload of rerouteDoor that avoids iteration - used by transferDoor
	public void rerouteDoor (Room dest, Door d, Room newRoom) {
		//remove target door from current listing
		removeDoor(dest, d);
		checkNeighbor(dest);
		//add target door to new listing
		addDoor(newRoom, d);
	}
	
	//removes a door from this room without doing any other cleanup (this version currently unused)
	public void removeDoor (Door d) {
		Room checkTarget = null;
		for (Room dest: doors.keySet()) {
			ArrayList<Door> newList = new ArrayList<Door>();
			for (Door dor: doors.get(dest)) {
				if (!dor.equals(d)) newList.add(dor);
				else checkTarget = dest;
			}
			if(newList.containsAll(doors.get(dest)) == false) {
				doors.put(dest, newList);
				break;
			}
		}
		myGraph.setVertex(d.x, d.y, false);
		if(checkTarget != null) checkNeighbor(checkTarget);
	}
	
	//overload of removeDoor that avoids iteration - used by deleteDoor and tD and rrD overloads
	public void removeDoor (Room dest, Door d) {
		ArrayList<Door> destList = new ArrayList<Door>();
		destList.addAll(doors.get(dest));
		destList.remove(d);
		doors.put(dest, destList);
		myGraph.setVertex(d.x, d.y, false);
		checkNeighbor(dest);
	}
	
	//initiates the full process of deleting and cleaning up a door
	public void deleteDoor (Door d) {
		Room checkTarget = null;
		//rewrite the door tile
		Main.tiles[d.x][d.y] = 1;
		Main.changedTiles.add(new Point(d.x, d.y));
		//remove the portal-partner
		for (Room dest: doors.keySet()) {
			ArrayList<Door> newList = new ArrayList<Door>();
			for (Door dor: doors.get(dest)) {
				if (dor.equals(d)) {
					dest.removeDoor(this, d.portal);
					checkTarget = dest;
				}
				//the door itself is removed by simply failing to add it to the new list, as in other methods
				else newList.add(dor);
			}
			if(newList.containsAll(doors.get(dest)) == false) doors.put(dest, newList);
		}
		myGraph.setVertex(d.x, d.y, false);
		if(checkTarget != null) checkNeighbor(checkTarget);
	}
	
	//override of deleteDoor that avoids iteration - used by transferDoor
	public void deleteDoor (Room dest, Door d) {
		//rewrite the door tile
		Main.tiles[d.x][d.y] = 1;
		Main.changedTiles.add(new Point(d.x, d.y));
		removeDoor(dest, d);
		dest.removeDoor(this, d.portal);
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
		Color chroma = Main.randomColor();
		Main.pallette.put(id, chroma);
		Main.rooms.put(id, this);
		myGraph.setVertex(x, y, true);
	}
	
	//constructor for a room with the given PathingGraph
	public Room (PathingGraph graphIn) {
		id = nextID();
		Color chroma = Main.randomColor();
		Main.pallette.put(id, chroma);
		Main.rooms.put(id, this);
		myGraph = graphIn;
	}
	
}
