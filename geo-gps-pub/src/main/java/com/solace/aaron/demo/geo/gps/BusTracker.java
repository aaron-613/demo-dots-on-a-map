package com.solace.aaron.demo.geo.gps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BusTracker {

	
	List<Bus> buses = new ArrayList<Bus>();
	Map<Integer,Set<Bus>> busByRoute = new HashMap<Integer,Set<Bus>>();
	//Map<Integer,Bus> busByVehNum = new HashMap<Integer,Bus>();
	
	
	
	synchronized Bus addRandomBus() {
		int carNum = buses.size()+1000;
		Bus bus = new Bus(carNum);
		buses.add(bus);
		if (!busByRoute.containsKey(bus.routeNum)) {
			busByRoute.put(bus.routeNum,new HashSet<Bus>());
		}
		busByRoute.get(bus.routeNum).add(bus);
		//busByVehNum.put(bus.vehicleNum,bus);
		return bus;
	}
	
/*	synchronized Bus addBus(int route, int position) {
		int carNum = buses.size()+1000;
		Bus bus = new Bus(carNum,route,position);
		buses.add(bus);
		if (!busByRoute.containsKey(bus.routeNum)) {
			busByRoute.put(bus.routeNum,new HashSet<Bus>());
		}
		busByRoute.get(bus.routeNum).add(bus);
		//busByVehNum.put(bus.vehicleNum,bus);
		return bus;
	}
*/	
	
	Bus getBus(int busNum) {
		try {
			return buses.get(busNum-1000);
		} catch (IndexOutOfBoundsException e) {
			e.printStackTrace();
			return null;
		}
	}
	
/*	void initBuses() {
		for (int route=0;route<Bus.busLoader.getNumRoutes();route++) { //RouteLoader.Route route : Bus.busLoader.routes) {
			for (int position=0;position<(int)Math.floor(Bus.busLoader.getRoute(route).coords.size()/15);position++) {
				addBus(route,position*15);
			}
		}
	}
*/
	
}
