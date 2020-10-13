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
	
	static HashMap<Integer, Room> rooms = new HashMap<Integer, Room>();
	static int[][] tiles = new int[50][50];
	static ArrayList<Point> changedTiles = new ArrayList<Point>();
	static HashMap<Integer, Color> pallette = new HashMap<Integer, Color>();
	static Random rand = new Random();
	Room from = null;
	Room to = null;

	public static void main(String[] args) {
		pallette.put(0, Color.WHITE);
		pallette.put(1, Color.BLACK);
		pallette.put(2, Color.CYAN);
		
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
        			//if it's a wall...
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
        					ArrayList<Point> checks = RDpreCheck(poked.x, poked.y);
        					if (checks.size() == 1) {
            					tiles[poked.x][poked.y] = tiles[checks.get(0).x][checks.get(0).y];
        					}
        					else {
	        					tiles[poked.x][poked.y] = 0;
	        					roomDetection(checks);
        					}
        				}
        				changedTiles.add(poked);
        			}
        		}
        		else if(e.isPrimaryButtonDown()) {
        			
	        		if(tiles[poked.x][poked.y] != 1) {
	        			if(sampleAdjacents(tiles, poked.x, poked.y).contains(2)) {
	        				HashMap<Door, Room> doorsToRemove = findAdjDoors(tiles, poked.x, poked.y);
	        				for(Door d: doorsToRemove.keySet()) {
	        					doorsToRemove.get(d).deleteDoor(d);
	        				}
		        		}
		        		tiles[poked.x][poked.y] = 1;
		        		changedTiles.add(poked);
		        		
		        		roomDetection(RDpreCheck(poked.x, poked.y));
	        		}
        		}
        	}
        });
		
		scene.setOnMouseClicked(new EventHandler<MouseEvent>() {
        	public void handle(MouseEvent e) {
        		Point poked = new Point(Math.min((int) (e.getX()/10), 49), Math.min((int) (e.getY()/10), 49));
        		if(e.isShiftDown()) {
	        		if(tiles[poked.x][poked.y] == 1) {
	        			int above;
	        			if(inBounds(poked.x, poked.y + 1, tiles)) above = tiles[poked.x][poked.y + 1];
	        			else above = 1;
	        			int below;
	        			if(inBounds(poked.x, poked.y - 1, tiles)) below = tiles[poked.x][poked.y - 1];
	        			else below = 1;
	        			int left;
	        			if(inBounds(poked.x - 1, poked.y, tiles)) left = tiles[poked.x - 1][poked.y];
	        			else left = 1;
	        			int right;
	        			if(inBounds(poked.x + 1, poked.y, tiles)) right = tiles[poked.x + 1][poked.y];
	        			else right = 1;
	        			
	        			if(above == 1 && below == 1 && left > 99 && right > 99 && left != right) {
	        				rooms.get(left).createDoor(rooms.get(right), poked.x, poked.y);
	        			}
	        			else if(left == 1 && right == 1 && above > 99 && below > 99 && above != below) {
	        				rooms.get(above).createDoor(rooms.get(below), poked.x, poked.y);
	        			}
	        		}
        		}
        		else if(e.isAltDown()) {
        			int idfound = tiles[poked.x][poked.y];
        			if(idfound > 99) {
        				Room roomfound = rooms.get(tiles[poked.x][poked.y]);
        				if(from == null) {
        					from = roomfound;
        					System.out.println("From set");
        				}
        				else if(to == null) {
        					if(roomfound != from) {
        						to = roomfound;
            					System.out.println("To set");
            					ArrayList<ArrayList<Door>> doorpaths = findPath(from, to);
            					for (ArrayList<Door> doorpath: doorpaths) {
            						for (int i = 0; i < doorpath.size() - 1; i ++) {
            							gc.strokeLine(doorpath.get(i).x * 10 + 5, doorpath.get(i).y * 10 + 5, 
            									doorpath.get(i + 1).x * 10 + 5, doorpath.get(i + 1).y * 10 + 5);
            						}
            					}
        					}
    					}
    					else {
    						from = null;
    						to = null;
        					System.out.println("Reset");
    					}
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
	public static ArrayList<ArrayList<Door>> findPath(Room comingFrom, Room goingTo) {
		ArrayList <Room> fromAsAL = new ArrayList<Room>();
		fromAsAL.add(comingFrom);
		ArrayList<ArrayList<Room>> protopaths = seekRoom(fromAsAL, goingTo);
//		Uncomment this to print the room-paths to the console
//		for(ArrayList<Room> listy : protopaths) {
//			System.out.println("Path Start");
//			for (Room r : listy) {
//				System.out.println(r.id);
//			}
//		}
		ArrayList<ArrayList<Door>> Adoorpaths = new ArrayList<ArrayList<Door>>();
		for(ArrayList<Room> listy : protopaths) {
			ArrayList<ArrayList<Door>> doorpaths = new ArrayList<ArrayList<Door>>();
			for (Door d: listy.get(0).doors.get(listy.get(1))) {
				ArrayList<Door> justd = new ArrayList<Door>();
				justd.add(d);
				doorpaths.add(justd);
			}
			ArrayList<ArrayList<Door>> doorset = new ArrayList<ArrayList<Door>>();
			//set up the door lists in doorset
			for (int i = 1; i < listy.size() - 1; i++) {
				doorset.add(listy.get(i).doors.get(listy.get(i + 1)));
			}
			
			for (ArrayList<Door> doorlist: doorset) {
				ArrayList<ArrayList<Door>> Ndoorpaths = new ArrayList<ArrayList<Door>>();
				for (ArrayList<Door> doorpath: doorpaths) {
					for (Door d: doorlist) {
						ArrayList<Door> nextpath = new ArrayList<Door>();
						nextpath.addAll(doorpath);
						nextpath.add(d);
						Ndoorpaths.add(nextpath);
					}
				}
				doorpaths = Ndoorpaths;
			}
			Adoorpaths.addAll(doorpaths);
		}
		return Adoorpaths;
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
			if(inBounds(checkx, checky, tiles)) nowfound = tiles[checkx][checky];
			else nowfound = 1;
			if ((lastfound == 1 || lastfound == 2) && (nowfound != 1 && nowfound != 2)) spaces.add(new Point(checkx, checky));
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
		//consider rewriting this so you can use roomDetection in more situations?
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
			
			//stores doors that might be modified by this pulse's results
			ArrayList<Door> problemDoors = new ArrayList<Door>();
			
			//check for doors adjacent to the newly found area
			//first off, iterate through tilesfound
			for(int n = 0; n < tilesfound.length; n++) {
				for (int m = 0; m < tilesfound[0].length; m++) {
					//if this is a newly "found" tile...
					if(tilesfound[n][m] == 1) {
						Point[] offsets = {new Point(0, 1), new Point(1, 0), new Point(0, -1), new Point(-1, 0)};
						for (int k = 0; k < offsets.length; k++) {
							int checkx = n + offsets[k].x;
							int checky = m + offsets[k].y;
							//avoid checking out of bounds - the weirdness is to cover the NEXT check, too
							if(inBounds(checkx + offsets[k].x, checky + offsets[k].y, tiles)) {
								if(tiles[checkx][checky] == 2) {
									if(rooms.get(tiles[n][m]) != null) {
	//Bear with me here.
	//doorsToCheck is the list of doors owned by the room at the origin that lead to the room on the far side of the found door
										ArrayList<Door> doorsToCheck = 
												rooms.get(tiles[n][m]).doors.get(rooms.get(
														tiles[checkx + offsets[k].x][checky + offsets[k].y]));
										//finally grab the door in this room at that location
										if(doorsToCheck != null) {
											for(Door d: doorsToCheck) {
												if(d.x == checkx && d.y == checky) problemDoors.add(d);
											}
										}
									}
								}
							}
						}
					}
				}
			}
			
			//decide what our result type is for this pulse
			
			//if you only found one type of tile
			if(tfNow.size() == 1) {
				//if you only found open space
				if(tfNow.contains(0)) {
					//create a new room
					Room newRoom = new Room();
					replaceTiles(tilesfound, 1, newRoom.id);
				}
				//if you only found one room
				else if(!tfNow.contains(0)) {
					//if this room has been found already
					if(typesfound.contains(tfNow.get(0))) {
						//room splitting
						
						//create a new room
						Room newRoom = new Room();
						replaceTiles(tilesfound, 1, newRoom.id);
						//transfer all relevant doors
						for(Door d: problemDoors) {
							rooms.get(tfNow.get(0)).transferDoor(d, newRoom);
						}
					}
					//if this room hasn't been found already
					else {
						//just use it
						replaceTiles(tilesfound, 1, tfNow.get(0));
					}
				}
			}
			//This happens when merging rooms - the second pulse is surrounded by found tiles and fails instantly
			else if (tfNow.size() == 0) {
				//System.out.println("Pulse found no tile types");
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
					//migrate problem doors to the surviving room
					//now-internal doors are automatically deleted by Room.transferDoor()
					int chosenRoom;
					if(tfNow.indexOf(0) == 0) chosenRoom = tfNow.get(1);
					else chosenRoom = tfNow.get(0);
					replaceTiles(tilesfound, 1, chosenRoom);
					for(int tiletype: tfNow) {
						if(tiletype != 0 && tiletype != chosenRoom) {
							rooms.get(tiletype).transferAllDoors(rooms.get(chosenRoom));
						}
					}
				}
			}
			typesfound.addAll(tfNow);
		}
		//write tilesfound into tiles. This can probably be refactored. 
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
					if (inBounds(checkx, checky, tiles)) {
						//if this tile has not been found and is not a wall or a door
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
		for(int i = 0; i < grid.length; i++) {
			for (int z = 0; z < grid[0].length; z++) {
				if(grid[i][z] == find) {
					grid[i][z] = replace;
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
				//don't check the origin tile //don't check out of bounds
				if ((checkx != 0 || checky != 0) && inBounds(checkx, checky, grid)) {
					if(found.contains(grid[checkx][checky]) == false) found.add(grid[checkx][checky]);
				}
			}
		}
		return found;
	}
	
	public HashMap<Door, Room> findAdjDoors(int[][] grid, int x, int y) {
		HashMap<Door, Room> myReturn = new HashMap<Door, Room>();
		Point[] offsets = {new Point(0, 1), new Point(1, 0), new Point(0, -1), new Point(-1, 0)};
		for (int i = 0; i < offsets.length; i++) {
			int checkx = x + offsets[i].x;
			int checky = y + offsets[i].y;
			if (inBounds(checkx, checky, grid)) {
				if (grid[checkx][checky] == 2) {
					for (int index: rooms.keySet()) {
						Door d = rooms.get(index).getDoorByLocation(checkx, checky);
						if (d != null) {
							myReturn.put(d, rooms.get(index));
							break;
						}
					}
				}
			}
		}
		
		return myReturn;
	}
	
	public static boolean inBounds(int x, int y, int[][] grid) {
		if(x >= 0 && y >= 0 && x < grid.length && y < grid[0].length) return true;
		else return false;
	}

}
