package com.solace.aaron.demo.geo.gps;

import java.awt.geom.Point2D;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.Destination;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.TextMessage;


public class Bus implements Runnable {

	static final int LAT_PADDING = 2;
	static final int LON_PADDING = 3;
	static final int LAT_FACTOR = 6;
	static final int LON_FACTOR = 6;
	static final int ROUTE_PADDING = 3;  // 000-999
	static final int VEH_NUM_PADDING = 4; // 1000-9999
	
	static RouteLoader busLoader = new RouteLoader();
    static {
        String filename = "coords2.txt";
        try {
            busLoader.load(filename);
        } catch (FileNotFoundException e) {
            System.out.println("Bus class could not find requested filename "+filename+" on the classpath");
            System.exit(-1);
        } catch (IOException e) {
            System.out.println("Bus class had I/O issue loading filename "+filename);
            e.printStackTrace();
            System.exit(-1);
        }
    }
    
    public enum Status {
    	OK,
    	FLAT_TIRE,
    	ENGINE_PROBLEM,
    }
    
	final int routeNum;
	int positionIndex;  // which 'tick' along the route is this?
	final int vehicleNum;
    Status status = Status.OK;
    boolean overrideStop = false;
    int busStopCount = 0;  // if == 0, moving; once arrive at bus stop, increase count to 4 and stay there

    // each Bus should be listening on their topics: comm/route/012, comm/bus/1234, comm/broadcast
    
    protected Bus(int busNum) {
		this.routeNum = (int)Math.floor(Math.random() * busLoader.getNumRoutes());  // start this car somewhere along the new raw route (pick an index between 0-length)
		this.positionIndex = (int)Math.floor(Math.random() * busLoader.getRoute(this.routeNum).coords.size()-1);  // start this car somewhere along the new raw route (pick an index between 0-length)
		if (this.positionIndex == busLoader.getRoute(this.routeNum).coords.size()) {
			throw new AssertionError("somehow made a position that's the same size!");
		}
		//this.vehicleNum = loader.addVehicle(this);  // not good code practice to call another method with 'this' while still in the constructor!
		this.vehicleNum = busNum;
	}
    
    protected Bus(int busNum, int routeNum, int positionIndex) {
    	this.routeNum = routeNum;
    	this.vehicleNum = busNum;
    	this.positionIndex = positionIndex;
    }
    
    void stopBus() {
    	overrideStop = true;
    }
    
    void startBus() {
    	overrideStop = false;
    }
    
    void fixBus() {
    	status = Status.OK;
    }
    
    int calcSpeed() {
		if (busStopCount == 0 && !overrideStop) return VehicleUtils.calcSpeedBus(this.routeNum, this.positionIndex);  // Point2D.Double.distance(lastPos.x, lastPos.y, getPosition().x, getPosition().y) * 80000;  // rougly km/h at 5 second updates
		else return 0;
    }

    // 192.168.56.101 default default

	public void tick() {
		if (overrideStop) {
			
		} else {
			if (status == Status.OK) {
				if (busStopCount == 0) {  // if so, move
					positionIndex++;
					if (positionIndex >= busLoader.getRouteCoords(routeNum).size()-1) {  // so size==10, then pos=9 and bus turns around
						positionIndex = 0;  // reset back to beginning... always go forward
//						direction = Direction.BACKWARD;
					}
					if (getPositionIndex() % 10 == 0) {  // bus stop every 10 ticks
						busStopCount = 4;  // stop for 4 ticks
					}
				} else {  // at a bus stop
					busStopCount--;
				}
			}
		}
		if (status != Status.OK && Math.random() < 0.01) {
			fixCar();
		}
		if (Math.random() < 0.0001) {
			fault();
		}
		GpsGenerator.onlyInstance().sendMessage(buildMessage(),genTopic());
		//BroadcastQueue.INSTANCE.queue.add.onlyInstance().sendMessage(buildMessage("speed="+speed+"status="+status),JCSMPFactory.onlyInstance().createTopic(genTopicString()));
	}

	@Override
	public void run() {
		try {
			tick();
		} catch (RuntimeException e) {
			e.printStackTrace();
			System.err.printf("route %d, pos %d, veh num %d, dir %s%n",routeNum,positionIndex,vehicleNum);
			System.exit(-1);
		} catch (Error e) {
			e.printStackTrace();
			System.err.printf("route %d, pos %d, veh num %d, dir %s%n",routeNum,positionIndex,vehicleNum);
			System.exit(-1);
		}
	}

	public Point2D.Double getPosition() {
		return busLoader.getRouteCoords(routeNum).get(positionIndex);
	}

	public int getPositionIndex() {
		return positionIndex;
	}

	@Override
	public String toString() {
		return String.format("Bus %d %s on route %d, pos %d",this.vehicleNum,this.routeNum,this.positionIndex);
	}

	void fixCar() {
		status = Status.OK;
		System.out.println(toString() + " is now fixed");
	}
	
	void receiveMessage(BytesXMLMessage msg) {
		// is this a control message?  Maybe we should stop?  Or start?
		System.out.println(toString()+" Received a message!");
	}
	
	public boolean isStopped() {
		if (status == Status.OK && busStopCount == 0) return false;
		else return true;
	}
	
	void fault() {
		status = Math.random() < 0.5 ? Status.FLAT_TIRE : Status.ENGINE_PROBLEM;
		sendAlertMessage();
	}

	void sendAlertMessage() {
		
		TextMessage msg = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
		msg.setText(String.format("Bus # %d (on route %d) is having issues... status == %s",
				this.vehicleNum,this.routeNum,status));
		System.out.println("ALERT MSG: "+msg.getText());
//		Broadcaster.onlyInstance().sendMessage(msg,Broadcaster.TOPIC_DISPATCH);
	}

	BytesXMLMessage buildMessage() {
		TextMessage msg = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
		msg.setText(genPayload());
		return msg;
	}

    Destination genTopic() {
    	// geo/bus/1234/001.238212/103.128345/023/OK
    	StringBuilder sb = new StringBuilder("geo/bus/");
//        sb.append(RangeUtils.helperMakeSubCoordString(quadrant.xNegativeModifier,innerX,xFactor,xPadding,xNeedNegs)).append("*/");
        sb.append(getPracticalStatus());
        return JCSMPFactory.onlyInstance().createTopic(sb.toString());
    }
    
    String getPracticalStatus() {
    	if (overrideStop) {
    		return "STOPPED";
    	} else {
    		return status == Status.OK ? (busStopCount == 0 ? "OK" : "STOPPED") : "FAULT";
    	}
    }
    
    String genPayload() {
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("vehicleNum",vehicleNum);
        job.add("routeNum",routeNum);
        job.add("lat",getPosition().x);
        job.add("lon",getPosition().y);
        if (getPracticalStatus().equals("FAULT")) {
        	job.add("status",getPracticalStatus()+": "+status);
        } else {
        	job.add("status",getPracticalStatus());
        }
        job.add("speed",calcSpeed() > 60 ? 60 : calcSpeed());
        return job.build().toString();
    }
    

    
    
    
}
