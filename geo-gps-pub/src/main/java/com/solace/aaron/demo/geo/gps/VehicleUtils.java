package com.solace.aaron.demo.geo.gps;

import java.awt.geom.Point2D;

public class VehicleUtils {

    public static int calcHeadingForRoute(int route, int pos) {
    	if (pos > 0) {  // use the previous point to calculate heading
    		return heading(Bus.busLoader.getCoord(route,pos-1),Bus.busLoader.getCoord(route,pos));
    	} else {  // starting point, use the next one rather than the last
    		return heading(Bus.busLoader.getCoord(route,pos),Bus.busLoader.getCoord(route,pos+1));
    	}
    }

    /**
     * Calculate the heading between 2 points.  Assumes that north is 0 degrees, and the coordinates are lat/lon (i.e. x=lat, y=lon)<p>
     * https://math.stackexchange.com/questions/1596513/find-the-bearing-angle-between-two-points-in-a-2d-space
     * @param p1 Point2D.Double lat,lon
     * @param p2 Point2D.Double lat,lon
     * @return a degree integer heading 0-359, 0=north, from p1 to p2
     */
    public static int heading(Point2D.Double p1, Point2D.Double p2) {
    	double yd = p2.x - p1.x;
    	double xd = p2.y - p1.y;
    	double theta = Math.atan2(xd,yd);
    	int head = (int)Math.round((theta/Math.PI)*180);
    	if (head < 0) head += 360;
    	//System.out.printf("dLat=%f, dLon=%f, head=%d%n",p2.x-p1.x,p2.y-p1.y,head);
    	return head;
    }
/*
    public static int calcSpeed(int route, int pos, RouteLoader.Direction dir) {
    	if (pos > 0) {  // use the previous point to calculate heading
    		return speed(Bus.busLoader.getCoord(route,pos-1),Bus.busLoader.getCoord(route,pos));
    	} else {  // starting point, use the next one rather than the last
    		return speed(Bus.busLoader.getCoord(route,pos),Bus.busLoader.getCoord(route,pos+1));
    	}
    }
*/
    public static int calcSpeedBus(int route, int pos) {
    	if (pos > 0) {  // use the previous point to calculate heading
    		return speed(Bus.busLoader.getCoord(route,pos-1),Bus.busLoader.getCoord(route,pos));
    	} else {  // starting point, use the next one rather than the last
    		return speed(Bus.busLoader.getCoord(route,pos),Bus.busLoader.getCoord(route,pos+1));
    	}
    }


        
    public static int speed(Point2D.Double p1, Point2D.Double p2) {
    	// since 1 degree lat ~= 111km, then 111 * 1/3600 == how many km you drive per degree second
		return (int)Math.round(Point2D.Double.distance(p1.x,p2.y,p2.x,p2.y) * 80000);  // rougly km/h at 5 second updates
    }
	

}
