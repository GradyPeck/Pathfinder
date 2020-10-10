package myPack;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.*;
import javafx.scene.canvas.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class Main extends Application {
	
	static ArrayList<Room> rooms = new ArrayList<Room>();
	static int[][] tiles = new int[50][50];
	ArrayList<Point> changedTiles = new ArrayList<Point>();
	static HashMap<Integer, Color> pallette = new HashMap<Integer, Color>();
	static Random rand = new Random();

	public static void main(String[] args) {
		pallette.put(0, Color.WHITE);
		pallette.put(1, Color.BLACK);
		pallette.put(2, Color.CYAN);
		//pallette.put(3, Color.VIOLET);
		
		/*
		Room roomA = new Room("A");
		Room roomB = new Room("B");
		Room roomC = new Room("C");
		Room roomD = new Room("D");
		
		roomA.addDoor(roomB);
		roomB.addDoor(roomC);
		roomB.addDoor(roomD);
		roomC.addDoor(roomD);
		
		rooms.add(roomA);
		rooms.add(roomB);
		rooms.add(roomC);
		rooms.add(roomD);
		
		System.out.println("Rooms:");
		for (Room r: rooms) {
			System.out.print(r.name + ":");
			for (Room roomp: r.doors.keySet()) {
				System.out.print(roomp.name);
			}
			System.out.print("\n");
		}
		System.out.println("Paths:");
		findPath(roomA, roomD);
		*/
		
		launch();
	}

	@Override
	public void start(Stage stage) throws Exception {
		Group root = new Group();
		Scene scene = new Scene(root, 500, 500, Color.WHITE);
		Canvas canvas = new Canvas(500, 500);
		GraphicsContext gc = canvas.getGraphicsContext2D();
		//draw initial state, based on tiles
		for (int i = 0; i < tiles.length; i++) {
			for (int z = 0; z < tiles[i].length; z++) {
				if (tiles[i][z] != 0) {
					gc.fillRect(i*10, z*10, 10, 10);
				}
			}
		}
		
		new AnimationTimer()
        {
            public void handle(long currentNanoTime)
            {
            	//redraw tiles that have been changed
                if(changedTiles.size() > 0) {
                	//this counts backwards so removals don't disturb it
                	for (int i = changedTiles.size() - 1; i >= 0; i--) {
                		Point t = changedTiles.get(i);
                		gc.setFill(pallette.get(tiles[t.x][t.y]));
                		gc.fillRect(t.x * 10, t.y * 10, 10, 10);
                		changedTiles.remove(i);
                	}
                	gc.setFill(Color.BLACK);
                }
            }
        }.start();
		
		//input events
		scene.setOnMouseDragged(new EventHandler<MouseEvent>() {
        	public void handle(MouseEvent e) {
        		Point poked = new Point(Math.min((int) (e.getX()/10), 49), Math.min((int) (e.getY()/10), 49));
        		if(e.isSecondaryButtonDown()) {
        			if(tiles[poked.x][poked.y] == 1) {
        				ArrayList<Integer> neighbors = sampleAdjacents(tiles, poked.x, poked.y);
        				//if there's exactly one type adjacent...
        				if(neighbors.size() == 1) {
        					//and it's wall, make it open space
        					if(neighbors.get(0) == 1) {
        						tiles[poked.x][poked.y] = 0;
        						//TODO new room. we'll do 0 for now.
        					}
        					//and it's not wall, make it that
        					else tiles[poked.x][poked.y] = neighbors.get(0);
        				}
        				//if there are exactly two and one is wall, set it to the other one
        				else if(neighbors.size() == 2 && neighbors.contains(1)) {
        					if(neighbors.indexOf(1) == 0) tiles[poked.x][poked.y] = neighbors.get(1);
        					else tiles[poked.x][poked.y] = neighbors.get(0);
        				}
        				else {
        					tiles[poked.x][poked.y] = 0;
        					roomDetection(RDpreCheck(poked.x, poked.y));
        				}
        				changedTiles.add(poked);
        			}
        		}
        		else if(e.isPrimaryButtonDown()) {
	        		if(tiles[poked.x][poked.y] != 1) {
		        		tiles[poked.x][poked.y] = 1;
		        		changedTiles.add(poked);
		        		
		        		roomDetection(RDpreCheck(poked.x, poked.y));
	        		}
        		}
        	}
        });
		
		scene.setOnMouseClicked(new EventHandler<MouseEvent>() {
        	public void handle(MouseEvent e) {
        		if(e.isShiftDown()) {
	        		Point poked = new Point(Math.min((int) (e.getX()/10), 49), Math.min((int) (e.getY()/10), 49));
	        		if(tiles[poked.x][poked.y] == 1) {
	        			tiles[poked.x][poked.y] = 2;
		        		changedTiles.add(poked);
	        		}
        		}
        	}
        });

		//finishing up Start method
		root.getChildren().add(canvas);
		stage.setScene(scene);
		stage.show();
	}
	
	//the main pathfinding method that handles all the others
	public static void findPath(Room comingFrom, Room goingTo) {
		ArrayList <Room> fromAsAL = new ArrayList<Room>();
		fromAsAL.add(comingFrom);
		ArrayList<ArrayList<Room>> protopaths = seekRoom(fromAsAL, goingTo);
//		for(ArrayList<Room> listy : protopaths) {
//			for (Room r : listy) {
//				System.out.println(r.name);
//			}
//		}
	}
	
	//the recursive function that generates the room-paths
	public static ArrayList<ArrayList<Room>> seekRoom(ArrayList<Room> sofar, Room target) {
		ArrayList<ArrayList<Room>> myReturn = new ArrayList<ArrayList<Room>>();
		//find the last room checked- the "current room" of this pulse branch
		Room lastRoom = sofar.get(sofar.size()-1);
		//for each room this room has a door to
		for (Room r: lastRoom.doors.keySet()) {
			//if this is our goal room
			if(r.equals(target)) {
				//create a "sofar" consisting of the final path (including goal room) and return it
				ArrayList<Room> nextSoFar = new ArrayList<Room>();
				nextSoFar.addAll(sofar);
				nextSoFar.add(r);
				myReturn.add(nextSoFar);
			}
			//if this pulse hasn't passed through that room already
			else if(sofar.contains(r) == false) {
				//create a sofar for the new branch by adding the new room to a copy of sofar
				ArrayList<Room> nextSoFar = new ArrayList<Room>();
				nextSoFar.addAll(sofar);
				nextSoFar.add(r);
				//start a new branch into that room (and add that branch's result to ours)
				myReturn.addAll(seekRoom(nextSoFar, target));
			}
		}
		return myReturn;
	}

	//check if a room detection sweep needs to happen, and find seed coordinates for one
	public ArrayList<Point> RDpreCheck(int x, int y) {
		final Point[] offsets = {
			new Point(-1, -1),
			new Point(0, -1),
			new Point(1, -1),
			new Point(1, 0),
			new Point(1, 1),
			new Point(0, 1),
			new Point(-1, 1),
			new Point(-1, 0),
			new Point(-1, -1)
		};
		int lastfound = -1;
		ArrayList<Point> spaces = new ArrayList<Point>();
		for (int i = 0; i < 9; i++) {
			int nowfound;
			int checkx = x + offsets[i].x;
			int checky = y + offsets[i].y;
			//avoid checking out of bounds. Pretend all OOB tiles are walls. 
			if(checkx >= 0 && checkx < tiles.length && checky >= 0 && checky < tiles[0].length) nowfound = tiles[checkx][checky];
			else nowfound = 1;
			if (lastfound == 1 && nowfound != 1) spaces.add(new Point(checkx, checky));
			lastfound = nowfound;
		}
		return spaces;
		/* clockwise check sequence
		-1	-1
		0	-1
		1	-1
		1	0
		1	1
		0	1
		-1	1
		-1	0	*/
	}
	public void roomDetection (ArrayList<Point> origins) {
		//if room detection isn't actually necessary, forget it
		if(origins.size() <= 1) return;
		//System.out.println("Detection triggered!");
		
		//initialize tilesfound with -1
		int[][] tilesfound = new int[tiles.length][tiles[0].length];
		for(int i = 0; i < tilesfound.length; i++) {
			for (int z = 0; z < tilesfound[0].length; z++) {
				tilesfound[i][z] = -1;
			}
		}
		
		//stores distinct tiles found on all pulses so far
		ArrayList<Integer> typesfound = new ArrayList<Integer>();
		
		for (int i = 0; i < origins.size(); i++) {
			//stores distinct tiles found on this pulse only
			ArrayList<Integer> tfNow = rdPulse(origins.get(i), tilesfound);
			//if you only found one type of tile
			if(tfNow.size() == 1) {
				//if you only found open space
				if(tfNow.contains(0)) {
					int roomID = rand.nextInt();
					pallette.put(roomID, randomColor());
					replaceTiles(tilesfound, 1, roomID);
				}
				//if you only found one room
				else if(!tfNow.contains(0)) {
					//TODO room splitting
					//if this room has been found already
					if(typesfound.contains(tfNow.get(0))) {
						int roomID = rand.nextInt();
						pallette.put(roomID, randomColor());
						replaceTiles(tilesfound, 1, roomID);
					}
					else {
						replaceTiles(tilesfound, 1, tfNow.get(0));
					}
				}
			}
			else if (tfNow.size() == 0) {
				continue;
			}
			//if we found more than one type, ie because a wall was removed
			else {
				//if we only found one room plus empty space
				if(tfNow.size() == 2 && tfNow.contains(0)) {
					//just use the room
					if(tfNow.indexOf(0) == 0) replaceTiles(tilesfound, 1, tfNow.get(1));
					else replaceTiles(tilesfound, 1, tfNow.get(0));
				}
				//if we found more than one room (potentially plus empty space)
				else {
					//TODO room merging
					if(tfNow.indexOf(0) == 0) replaceTiles(tilesfound, 1, tfNow.get(1));
					else replaceTiles(tilesfound, 1, tfNow.get(0));
				}
			}
			typesfound.addAll(tfNow);
		}
		//write tilesfound into tiles. This can definitely be refactored. 
		for(int i = 0; i < tiles.length; i++) {
			for (int z = 0; z < tiles[0].length; z++) {
				if(tilesfound[i][z] != -1) tiles[i][z] = tilesfound[i][z];
				changedTiles.add(new Point(i, z));
			}
		}
	}
	
	//recursive method used in room detection
	//return is a list of all the distinct tile contents found (excluding walls)
	public ArrayList<Integer> rdPulse (Point origin, int[][] tilesfound) {
		ArrayList<Integer> myReturn = new ArrayList<Integer>();
		//step through all tiles adjacent to the origin (including diagonal)
		for (int i = -1; i < 2; i++) {
			for (int z = -1; z < 2; z++) {
				//exclude checking the origin itself. Commented portion switches on orthagonal mode. 
				if((i != 0 || z != 0) /**/&& (i == 0 || z == 0)/**/) {
					int checkx = origin.x + i;
					int checky = origin.y + z;
					//exclude checking tiles that are out of bounds
					if (checkx >= 0 && checkx < tiles.length && checky >= 0 && checky < tiles[0].length) {
						//if this tile has not been found and is not a wall
						if(tilesfound[checkx][checky] == -1 && tiles[checkx][checky] != 1 && tiles[checkx][checky] != 2) {
							Point pointy = new Point(checkx, checky);
							if(myReturn.contains(tiles[checkx][checky]) == false) myReturn.add(tiles[checkx][checky]);
							tilesfound[checkx][checky] = 1;
							for(Integer res: rdPulse(pointy, tilesfound)) {
								if(myReturn.contains(res) == false) myReturn.add(res);
							}
						}
					}
				}
			}
		}
		return myReturn;
	}
	
	public static Color randomColor() {
		double r = rand.nextDouble();
		double g = rand.nextDouble();
		double b = rand.nextDouble();
		return Color.color(r, g, b);
	}

	public void replaceTiles(int[][] grid, int find, int replace) {
		//boolean notechanges = grid == tiles;
		for(int i = 0; i < grid.length; i++) {
			for (int z = 0; z < grid[0].length; z++) {
				if(grid[i][z] == find) {
					grid[i][z] = replace;
					//if(notechanges) changedTiles.add(new Point(i, z));
				}
			}
		}
	}

	public ArrayList<Integer> sampleAdjacents(int[][] grid, int x, int y) {
		ArrayList<Integer> found = new ArrayList<Integer>();
		for (int i = -1; i < 2; i++) {
			for (int z = -1; z < 2; z++) {
				int checkx = x + i;
				int checky = y + z;
				//don't check the origin tile
				if ((checkx != 0 || checky != 0) && 
						//don't check out of bounds
						(checkx >= 0 && checkx < grid.length && checky >= 0 && checky < grid[0].length)) {
					if(found.contains(grid[checkx][checky]) == false) found.add(grid[checkx][checky]);
				}
			}
		}
		return found;
	}
}
