package myPack;

import java.util.ArrayList;
import java.util.HashMap;

import javafx.scene.paint.Color;

public class Room {
	public HashMap<Room, ArrayList<Door>> doors = new HashMap<Room, ArrayList<Door>>();
	static int lastID = 99;
	public int id;
	public Color chroma;
	
	
	public void addDoor (Room destRoom) {
		ArrayList<Door> destDoors = new ArrayList<Door>();
		if(doors.get(destRoom) != null) destDoors.addAll(doors.get(destRoom));
		destDoors.add(new Door());
		doors.put(destRoom, destDoors);
	}
	
	public static int nextID () {
		lastID++;
		return lastID;
	}
	
//	public Room (int idin, Color colorin) {
//		id = idin;
//		chroma = colorin;
//	}
	
	public Room () {
		id = nextID();
		chroma = Main.randomColor();
	}
	
}
