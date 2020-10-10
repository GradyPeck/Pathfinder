package myPack;

import java.util.HashMap;

public class Door {
	public int x;
	public int y;
	public Door portal;
	public HashMap<Door, Path> paths;
}
