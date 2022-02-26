package com.solace.aaron.demo.geo.gps;

import java.awt.geom.Point2D;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.json.JSONObject;

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
        String filename = "/sg-buses2.txt";
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
    final int busNum;
    Status status = Status.OK;
    double passgenerCapacity;
    boolean overrideStop = false;
    int busStopCount = 0;  // if == 0, moving; once arrive at bus stop, increase count to 4 and stay there
    String extraRouteChar = "";

    // each Bus should be listening on their topics: comm/route/012, comm/bus/1234, comm/broadcast
    
    protected Bus(int busNum) {
        this.routeNum = (int)Math.floor(Math.random() * busLoader.getNumRoutes());  // start this car somewhere along the new raw route (pick an index between 0-length)
        this.positionIndex = (int)Math.floor(Math.random() * busLoader.getRoute(this.routeNum).coords.size()-1);  // start this car somewhere along the new raw route (pick an index between 0-length)
        if (this.positionIndex == busLoader.getRoute(this.routeNum).coords.size()) {
            throw new AssertionError("somehow made a position that's the same size!");
        }
        //this.vehicleNum = loader.addVehicle(this);  // not good code practice to call another method with 'this' while still in the constructor!
        this.busNum = busNum;
        passgenerCapacity = Math.random();  // start between 0 and 1
        int c = (int)(Math.random()*10);
        if (c < 4) extraRouteChar = "A";
        else if (c < 7) extraRouteChar = "B";
        else if (c < 9) extraRouteChar = "M";
        else extraRouteChar = "X";
//        if (Math.random() < 0.5) extraRouteChar = "A";
    }
    
    protected Bus(int busNum, int routeNum, int positionIndex) {
        this.routeNum = routeNum;
        this.busNum = busNum;
        this.positionIndex = positionIndex;
        passgenerCapacity = Math.random();  // start between 0 and 1
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
//                        direction = Direction.BACKWARD;
                    }
                    if (getPositionIndex() % 10 == 0) {  // bus stop every 10 ticks
                        busStopCount = 4;  // stop for 4 ticks
                    }
                } else {  // at a bus stop
                    busStopCount--;
                    // change the passenger capacity a bit
                    passgenerCapacity += ((Math.random()*0.2)-0.1);  // +/- 0.2 for change in passengers
                    passgenerCapacity = Math.max(0,passgenerCapacity);
                    passgenerCapacity = Math.min(1,passgenerCapacity);
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
            GpsGenerator.onlyInstance().rescheduleMe(this);
        } catch (RuntimeException e) {
            e.printStackTrace();
            System.err.printf("route %d, pos %d, veh num %d, dir %s%n",routeNum,positionIndex,busNum);
            System.exit(-1);
        } catch (Error e) {
            e.printStackTrace();
            System.err.printf("route %d, pos %d, veh num %d, dir %s%n",routeNum,positionIndex,busNum);
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
        return String.format("Bus %d on route %d, pos %d", busNum, routeNum, positionIndex);
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
                this.busNum,this.routeNum,status));
        System.out.println("ALERT MSG: "+msg.getText());
//        Broadcaster.onlyInstance().sendMessage(msg,Broadcaster.TOPIC_DISPATCH);
    }

    BytesXMLMessage buildMessage() {
        TextMessage msg = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
        msg.setText(genPayload());
        return msg;
    }

    Destination genTopic() {
        // geo/bus/1234/001.238212/103.128345/023/OK
    	// NEW : bus/gps/v2/001.12345/0103.12345/{head}/132/8293/
        StringBuilder sb = new StringBuilder("bus_trak/gps/v2/");
//        sb.append(RangeUtils.helperMakeSubCoordString(quadrant.xNegativeModifier,innerX,xFactor,xPadding,xNeedNegs)).append("*/");
        sb.append(String.format("%03d", routeNum+1)).append(extraRouteChar).append('/');
        sb.append(String.format("%05d", busNum)).append('/');
        sb.append(String.format("%09.5f", getPosition().x)).append('/');
        sb.append(String.format("%010.5f", getPosition().y)).append('/');
        String base4Heading = Integer.toString((int)Math.floor(VehicleUtils.calcHeadingForRoute(routeNum, positionIndex)/22.5),4);
        if (base4Heading.length() < 2) sb.append('0');
        sb.append(base4Heading).append('/');
//        sb.append(String.format("%s", Integer.toString(VehicleUtils.calcHeadingForRoute(routeNum, positionIndex)/10))).append('/');
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
    
    double toFixed(double d, int decimals) {
    	d *= Math.pow(10, decimals);
    	d = Math.round(d);
    	d /= Math.pow(10, decimals);
    	return d;
    }
    
    String genPayload() {
    	JSONObject job = new JSONObject();
        job.put("busNum",busNum);
        job.put("routeNum",(routeNum+1) + extraRouteChar);
        job.put("latitude",getPosition().x);
        job.put("longitude",getPosition().y);
        if (getPracticalStatus().equals("FAULT")) {
            job.put("status",getPracticalStatus()+": "+status);
        } else {
            job.put("status",getPracticalStatus());
        }
        job.put("speed",calcSpeed() > 60 ? 60 : calcSpeed());
        job.put("heading", VehicleUtils.calcHeadingForRoute(routeNum, positionIndex));
        job.put("psgrCap", toFixed(passgenerCapacity,2));
        return job.toString(2);
    }
    

    
    
    
}
