package myPack;

import java.awt.Point;
import java.util.List;

public class Path {
	int length;
	List<Point> points;
	
	public void append(Path p) {
		length += p.length;
		points.addAll(p.points);
	}
	
	public Path(int lengthin, List<Point> pointsin) {
		length = lengthin;
		points = pointsin;
	}
}
