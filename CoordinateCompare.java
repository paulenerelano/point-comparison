import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class CoordinateCompare {

	static class FileParser {
		private String filePath = null;
		private static final String DELIM = ",";
		
		public FileParser(String filepath) {
			filePath = filepath;
		}
		
		public ArrayList<Point> parseFile(){
			ArrayList<Point> points = new ArrayList<Point>();
			
			FileInputStream stream = null;
	        try {
	            stream = new FileInputStream(filePath);
	        } catch (FileNotFoundException e) {
	            e.printStackTrace();
	        }
	        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
	        String strLine;
	        ArrayList<String> lines = new ArrayList<String>();
	        try {
	            while ((strLine = reader.readLine()) != null) {
	                String delims[] = strLine.trim().split(DELIM, 2);
	                try {
	                	points.add(new Point(Integer.parseInt(delims[0]), Integer.parseInt(delims[1])));
	                }
	                catch (NumberFormatException e) {
	                	continue;
	                }
	            }
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	        try {
	            reader.close();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	        return points;
	    }
	}
	
	static class Point {
		int x;
		int y;
		
		public Point(int x, int y) {
			this.x = x;
			this.y = y;	
		}
	}
	
	
	static class YGroup extends HashMap<Integer, ArrayList<Point>> {}
	static class GroupedPoints extends HashMap<Integer, YGroup> {}
	/* < X , < Y , Point List > */
	
	/* -------------
	 * |     |     |
	 * |     |     |		
	 * |---- 5 ----|
	 * |     |     |
	 * |     |     |
	 * |------------
	 * */
	
	private static final Integer KEY_INTERVAL_X = 50;
	private static final Integer KEY_INTERVAL_Y = 50;
	private static final Integer GRIDS_TO_COMPARE = 1;
	
	private static Integer getXCoordKey(Point pt) {
		return pt.x / KEY_INTERVAL_X;
	}
	
	private static Integer getYCoordKey(Point pt) {
		return pt.y / KEY_INTERVAL_Y;
	}
	
	public static GroupedPoints groupPoints(ArrayList<Point> points) {
		GroupedPoints groupOfPoints = new GroupedPoints();
		
		for (Point pt : points) {
			Integer xKey = getXCoordKey(pt);
			Integer yKey = getYCoordKey(pt);
			
			if (groupOfPoints.containsKey(xKey) == false) {
				// New Y coord group, create yGroup and Point list
				YGroup yGroup = new YGroup();
				ArrayList<Point> pointList = new ArrayList<Point>();
				
				pointList.add(pt);
				yGroup.put(yKey, pointList);
				
				groupOfPoints.put(xKey, yGroup);
				
			} else {
				// Existing Y coord group
				YGroup yGroup = groupOfPoints.get(xKey);
				
				if (yGroup.containsKey(yKey) == false) {
					// New Point list
					ArrayList<Point> pointList = new ArrayList<Point>();
					
					pointList.add(pt);
					
					yGroup.put(yKey, pointList);
				} else {
					// Point List exists
					ArrayList<Point> pointList = yGroup.get(yKey);
					
					pointList.add(pt);
				}
			}
		}
		
		return groupOfPoints;
	}
	
	/***********/
	static class NearestPoint {
		Point pt = null;
		Integer distance = -1;
		Integer xCoordKey = -1;
		Integer yCoordKey = -1;
		int listIdx = -1;
	}
	
	private static Integer computeDistance(Point p1, Point p2) {
		Integer x_diff = Math.abs(p1.x - p2.x);
		Integer y_diff = Math.abs(p1.y - p2.y);
		
		return (int) Math.sqrt(Math.pow(x_diff, 2) + Math.pow(y_diff, 2));
	}
	
	
	// Returns true if exact match is found
	private static boolean getNearestPoint(NearestPoint nPoint, Point tPoint, ArrayList<Point> compareList) {
		for (int idx = 0; idx < compareList.size(); idx++)
		{
			Point pt = (Point)compareList.get(idx);
			Integer distanceTmp = computeDistance(pt, tPoint);
			
			if (distanceTmp == 0) {
				// Exactly the same
				nPoint.distance = distanceTmp;
				nPoint.pt = pt;
				nPoint.listIdx = idx;
				return true;
			}
			
			if (nPoint.pt == null || distanceTmp < nPoint.distance) {
				nPoint.distance = distanceTmp;
				nPoint.pt = pt;
				nPoint.listIdx = idx;
			}
		}
		
		return false;
	}
	
	// Returns true if exact match is found
	private static boolean getNearestPointInYGroup(NearestPoint nPoint, Point tPoint, YGroup yGroup) {
		Integer yKey = getYCoordKey(tPoint);
		Integer yKeyTop = yKey + GRIDS_TO_COMPARE;
		Integer yKeyBot = yKey - GRIDS_TO_COMPARE;
		
		ArrayList<Point> compareList = null;
		ArrayList<Integer> yKeyList = new ArrayList<Integer>();
		yKeyList.add(yKey);
		yKeyList.add(yKeyTop);
		yKeyList.add(yKeyBot);
		
		for (Integer yk : yKeyList)
		{
			compareList = yGroup.get(yk);
			if (compareList != null) {
				boolean hasExactMatch = getNearestPoint(nPoint, tPoint, compareList);
				if (hasExactMatch) {
					nPoint.yCoordKey = yk;
					return true;
				}
			}
		}
		
		
		return false;
	}
	
	private static boolean getNearestPointInXGroup(NearestPoint nPoint, Point tPoint, GroupedPoints groupedPts) {
		Integer xKey = getXCoordKey(tPoint);
		Integer xKeyRight = xKey + GRIDS_TO_COMPARE;
		Integer xKeyLeft = xKey - GRIDS_TO_COMPARE;
		
		YGroup yGroup = null;
		ArrayList<Integer> xKeyList = new ArrayList<Integer>();
		xKeyList.add(xKey);
		xKeyList.add(xKeyRight);
		xKeyList.add(xKeyLeft);
		
		// Get nearest point, same xKey
		for (Integer xk : xKeyList)
		{
			yGroup = groupedPts.get(xk);
			if (yGroup != null) {
				boolean hasExactMatch = getNearestPointInYGroup(nPoint, tPoint, yGroup);
				if (hasExactMatch) {
					nPoint.xCoordKey = xk;
					return true;
				}
			}
		}
		
		return false;
	}
	
	static class DiffPoints {
		Point targetPoint = null;
		NearestPoint nearestPoint = null;
		
		public DiffPoints(Point tp, NearestPoint np) {
			this.targetPoint = tp;
			this.nearestPoint = np;
		}
	}
	
	public static void dumpGroupedPoints(GroupedPoints gp) {
		for(Map.Entry<Integer, YGroup> x: gp.entrySet()) {
			System.out.println(x.getKey());
			for (Map.Entry<Integer, ArrayList<Point>> y : x.getValue().entrySet()) {
				System.out.print("\t");
				System.out.println(y.getKey());
				for (Point pt : y.getValue()) {
					System.out.print("\t");
					System.out.print("\t");
					System.out.print(pt.x);
					System.out.print(",");
					System.out.println(pt.y);
				}
			}
		}
	}
	
	public static final Integer DISTANCE_DEVIATION = 50;
	public static ArrayList<DiffPoints> comparePoints(ArrayList<Point> points, ArrayList<Point> comparisonPoints)  {
		GroupedPoints groupedCompPts = groupPoints(comparisonPoints);
		ArrayList<DiffPoints> diffPoints = new ArrayList<DiffPoints>();
//		dumpGroupedPoints(groupedCompPts);
		
		for (Point pt : points) {
			NearestPoint nPoint = new NearestPoint();
			
			boolean hasExactMatch = getNearestPointInXGroup(nPoint, pt, groupedCompPts);
			if (!hasExactMatch) {
				// Check distance deviation
				if (nPoint.distance > 0 && nPoint.distance < DISTANCE_DEVIATION) {
					diffPoints.add(new DiffPoints(pt, nPoint));
				}
				
			}
		}
		
		return diffPoints;
	}
	
	public static String intToString(Integer i) {
		return Integer.toString(i);
	}
	
	public static void saveToFile(ArrayList<DiffPoints> diff) {
		try (PrintWriter out = new PrintWriter("./out.csv")) {
			out.println("TP X, TP Y, NP X, NP Y, Distance");
		    for (DiffPoints dp : diff) {
		    	
		    	if (dp.nearestPoint.pt != null)
		    	{
			    	out.println(
			    			intToString(dp.targetPoint.x) + 
			    			"," +
			    			intToString(dp.targetPoint.y) + 
			    			"," +
			    			intToString(dp.nearestPoint.pt.x) + 
			    			"," +
			    			intToString(dp.nearestPoint.pt.y) + 
			    			"," +
			    			intToString(dp.nearestPoint.distance)		    			
			    			);
		    	}
		    }
		} catch (FileNotFoundException e) {
			System.out.println("TP X, TP Y, NP X, NP Y, Distance");
		    for (DiffPoints dp : diff) {
		    	if (dp.nearestPoint != null)
		    	{
			    	System.out.println(
			    			intToString(dp.targetPoint.x) + 
			    			"," +
			    			intToString(dp.targetPoint.y) + 
			    			"," +
			    			intToString(dp.nearestPoint.pt.x) + 
			    			"," +
			    			intToString(dp.nearestPoint.pt.y) + 
			    			"," +
			    			intToString(dp.nearestPoint.distance)		    			
			    			);
		    	}
		    }
		}
	}
	
	public static void groupedComparison() {
		FileParser fpTarget = new FileParser("./target.csv");
		FileParser fpComp = new FileParser("./comparison.csv");
		long startTime;
		long endTime;
		
		ArrayList<Point> targetPtList = fpTarget.parseFile();
		ArrayList<Point> compPtList = fpComp.parseFile();
		
		System.out.println("Comparing " + Integer.toString(targetPtList.size()) + ":" + Integer.toString(compPtList.size()));
		
		startTime = System.currentTimeMillis();
			ArrayList<DiffPoints> diff = comparePoints(targetPtList, compPtList);
		endTime = System.currentTimeMillis();
		
		System.out.println("Comparetime: " + Long.toString(endTime-startTime));
		
		saveToFile(diff);
	}
	
	public static ArrayList<DiffPoints> compare(ArrayList<Point> targetPoints, ArrayList<Point> comparisonPoints) {
		ArrayList<DiffPoints> diffPoints = new ArrayList<DiffPoints>();
		HashSet<Point> skippedPoints = new HashSet<Point>();
		
		
		for (Point tp : targetPoints) {
			NearestPoint nPoint = new NearestPoint();
			boolean hasExactMatch = false;
			
			for (Point cp: comparisonPoints) {
				if (skippedPoints.contains(cp)) {
					continue;
				}
				
				
				Integer distanceTmp = computeDistance(cp, tp);
				
				if (distanceTmp == 0) {
					// Exactly the same
					nPoint.distance = distanceTmp;
					nPoint.pt = cp;
					skippedPoints.add(cp);
					
					hasExactMatch = true;
					break;
				}
				
				if (nPoint.pt == null || distanceTmp < nPoint.distance) {
					nPoint.distance = distanceTmp;
					nPoint.pt = cp;
				}
			}
			
			if (hasExactMatch == false && nPoint.pt != null) {
				diffPoints.add(new DiffPoints(tp, nPoint));
			}
		}

		return diffPoints;
	}
	
	public static void normalComparison() {
		FileParser fpTarget = new FileParser("./target.csv");
		FileParser fpComp = new FileParser("./comparison.csv");
		long startTime;
		long endTime;
		
		ArrayList<Point> targetPtList = fpTarget.parseFile();
		ArrayList<Point> compPtList = fpComp.parseFile();
		
		System.out.println("Comparing " + Integer.toString(targetPtList.size()) + ":" + Integer.toString(compPtList.size()));
		
		startTime = System.currentTimeMillis();
			ArrayList<DiffPoints> diff = compare(targetPtList, compPtList);
		endTime = System.currentTimeMillis();
		
		System.out.println("Normal Comparison");
		System.out.println("Comparetime: " + Long.toString(endTime-startTime));
		
		saveToFile(diff);
	}
	
	
	public static void main(String args[]) {
		
//		normalComparison();
		groupedComparison();
	}
}