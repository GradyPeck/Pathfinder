package myPack;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;

import javafx.scene.paint.Color;

public class Room {
	public HashMap<Room, ArrayList<Door>> doors = new HashMap<Room, ArrayList<Door>>();
	static int lastID = 99;
	public int id;
	public Color chroma;
	
	//creates a new door from this room to a target room at an arbitrary location
	//currently unused
	public void addDoor (Room destRoom, int x, int y) {
		ArrayList<Door> destDoors = new ArrayList<Door>();
		if(doors.get(destRoom) != null) destDoors.addAll(doors.get(destRoom));
		destDoors.add(new Door(x, y));
		doors.put(destRoom, destDoors);
	}
	
	//overload that takes an existing door object instead of location - used by createDoor and transferDoor
	public void addDoor (Room destRoom, Door d) {
		ArrayList<Door> destDoors = new ArrayList<Door>();
		if(doors.get(destRoom) != null) destDoors.addAll(doors.get(destRoom));
		destDoors.add(d);
		doors.put(destRoom, destDoors);
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
	}
	
	public void transferAllDoors (Room newRoom) {
		for (Room r: doors.keySet()) {
			for (Door d: doors.get(r)) {
				transferDoor(d, newRoom);
			}
		}
	}
	
	//changes the destination room of a door
	public void rerouteDoor (Door d, Room destRoom) {
		ArrayList<Door> outerList = new ArrayList<Door>();
		for (Room dest: doors.keySet()) {
			ArrayList<Door> newList = new ArrayList<Door>();
			for (Door dor: doors.get(dest)) {
				if (dor.equals(d)) {
					ArrayList<Door> current;
					if(doors.containsKey(destRoom)) {
						current = doors.get(destRoom);
						current.add(d);
						doors.put(destRoom, current);
					}
					else { 
						current = new ArrayList<Door>();
						current.add(d);
						outerList = current;
					}
				}
				else newList.add(dor);
			}
			if(newList.containsAll(doors.get(dest)) == false) doors.put(dest, newList);
		}
		//this is here to avoid a ConcurrentModificationException when adding a new destination room
		if(outerList.size() != 0) doors.put(destRoom, outerList);
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
	}
	
	//returns the next possible roomID and advances to the next one
	public static int nextID () {
		lastID++;
		return lastID;
	}
	
	public Room () {
		id = nextID();
		chroma = Main.randomColor();
		Main.pallette.put(id, chroma);
		Main.rooms.put(id, this);
	}
	
}
