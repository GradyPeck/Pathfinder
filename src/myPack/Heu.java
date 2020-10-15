package myPack;

import java.awt.Point;

import org.jgrapht.alg.interfaces.AStarAdmissibleHeuristic;

public class Heu implements AStarAdmissibleHeuristic<Point> {

	@Override
	public double getCostEstimate(Point arg0, Point arg1) {
		int distance = Math.abs(arg0.x - arg1.x) + Math.abs(arg0.y - arg1.y);
		return (double) distance;
	}

}
