package com.solace.aaron.demo.geo.gps;

import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class RouteLoader {

    public class Route {
//        final String start;  // address
//        final String end;    // address
//        final String dist;
//        final String time;
        final List<Point2D.Double> coords;  // lat=x, lon=y

        public Route(String start, String end, String dist, String time, List<Point2D.Double> coords) {
//            this.start = start;
//            this.end = end;
//            this.dist = dist;
//            this.time = time;
            this.coords = coords;
        }
        
        public Route(List<Point2D.Double> coords) {
            this.coords = coords;
        }
    }

    List<Route> routes = new ArrayList<Route>();
    //List<Vehicle> vehicles = new ArrayList<Vehicle>();
    
    public int getNumRoutes() {
        return routes.size();
    }
    
    public Route getRoute(int routeNum) {
        return routes.get(routeNum);
    }
    
    public List<Point2D.Double> getRouteCoords(int routeNum) {
        return routes.get(routeNum).coords;
    }
    
    public Point2D.Double getCoord(int routeNum, int routeIndex) {
        return routes.get(routeNum).coords.get(routeIndex);
    }
    
/*    synchronized int addVehicle(Vehicle v) {
        int carNum = vehicles.size()+1000;
        vehicles.add(v);
        return carNum;
    }
*/    
    
    ///////////////////////////////////
    
    Route parseRawRouteText(String routeText) { //copyright, String startAddr, String endAddr, String dist, String time, List<Point2D.Double> coords) {
//        String[] fields = routeText.split("\\|");
//        String start = fields[1];
//        String end = fields[2];
//        String dist = fields[3];
//        String time = fields[4];
//        String[] textCoords = fields[5].split(";");
        String[] textCoords = routeText.split(";");
        List<Point2D.Double> coords = new ArrayList<Point2D.Double>();
        for (String coord : textCoords) {
            String[] latlon = coord.split(",");
            double lat = Double.parseDouble(latlon[0]);
            double lon = Double.parseDouble(latlon[1]);
            coords.add(new Point2D.Double(lat,lon));  // x=lat, y=lon
        }
//        Route route = new Route(start,end,dist,time,coords);
        Route route = new Route(coords);
        return route;
    }

    void load(String filename) throws FileNotFoundException, IOException {
//        BufferedReader reader = new BufferedReader(new FileReader(filename));
    	BufferedReader reader = new BufferedReader(new InputStreamReader(RouteLoader.class.getResourceAsStream(filename)));
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                routes.add(parseRawRouteText(line));
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
            }
        }
    }
    

}
