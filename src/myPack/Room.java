package myPack;

import java.util.ArrayList;
import java.util.HashMap;

import javafx.scene.paint.Color;

public class Room {
	public HashMap<Room, ArrayList<Door>> doors = new HashMap<Room, ArrayList<Door>>();
	static int lastID = 99;
	public int id;
	public Color chroma;
	
	//creates a new door from this room to a target room
	//TODO rewrite this to respect location
	public void addDoor (Room destRoom) {
		ArrayList<Door> destDoors = new ArrayList<Door>();
		if(doors.get(destRoom) != null) destDoors.addAll(doors.get(destRoom));
		destDoors.add(new Door());
		doors.put(destRoom, destDoors);
	}
	
	//transfers a door from one room to another
	public void transferDoor (Door d, Room newRoom) {
		rerouteDoor(d.portal, newRoom);
		for (Room dest: doors.keySet()) {
			ArrayList<Door> newList = new ArrayList<Door>();
			for (Door dor: doors.get(dest)) {
				if (dor.equals(d)) newRoom.addDoor(dest);
				else newList.add(dor);
			}
			if(newList.containsAll(doors.get(dest)) == false) doors.put(dest, newList);
		}
	}
	
	//changes the destination room of a door
	public void rerouteDoor (Door d, Room destRoom) {
		for (Room dest: doors.keySet()) {
			ArrayList<Door> newList = new ArrayList<Door>();
			for (Door dor: doors.get(dest)) {
				if (dor.equals(d)) {
					ArrayList<Door> current;
					if(doors.containsKey(destRoom)) current = doors.get(destRoom);
					else current = new ArrayList<Door>();
					current.add(d);
					doors.put(destRoom, current);
				}
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
	
//	Room constructor used in tests - pending removal
//	public Room (int idin, Color colorin) {
//		id = idin;
//		chroma = colorin;
//	}
	
	public Room () {
		id = nextID();
		chroma = Main.randomColor();
		Main.pallette.put(id, chroma);
		Main.rooms.put(id, this);
	}
	
}
