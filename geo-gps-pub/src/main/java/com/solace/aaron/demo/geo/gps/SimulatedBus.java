package com.solace.aaron.demo.geo.gps;

import java.awt.geom.Point2D;
import java.util.List;

public class SimulatedBus {

    int busNum;
    int routeNum;
    List<Point2D.Double> routeCoords;
    int posIndex;
    double passgenerCapacity;
    int speed;
    
    public SimulatedBus(int busNum, int routeNum, List<Point2D.Double> routeCoords) {
        this.busNum = busNum;
        this.routeNum = routeNum;
        this.routeCoords = routeCoords;
        this.posIndex = (int)(Math.floor(Math.random()*routeCoords.size()));
        passgenerCapacity = Math.random();
        speed = (int)(Math.random()*50);
    }

    void tick() {
        // move the position along one
        posIndex++;
        if (posIndex >= routeCoords.size()) {
            posIndex = 0;  // reset back to start
        }
        // change the speed a bit
        speed += (int)((Math.random()*10)-5);  // +/- 5 for changing speed
        speed = Math.max(0,speed);
        speed = Math.min(60,speed);
        // change the passenger capacity a bit
        passgenerCapacity += ((Math.random()*0.2)-0.1);  // +/- 0.2 for change in passengers
        passgenerCapacity = Math.max(0,passgenerCapacity);
        passgenerCapacity = Math.min(1,passgenerCapacity);
    }
    
    public int getBusNum() {
        return busNum;
    }
    
    public int getRouteNum() {
        return routeNum;
    }
    
    public Point2D.Double getPosition() {
        return routeCoords.get(posIndex);
    }
}
