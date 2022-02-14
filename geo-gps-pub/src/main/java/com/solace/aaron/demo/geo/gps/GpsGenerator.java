package com.solace.aaron.demo.geo.gps;

import java.io.FileNotFoundException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.Destination;
import com.solacesystems.jcsmp.JCSMPChannelProperties;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPProducerEventHandler;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPReconnectEventHandler;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.JCSMPStreamingPublishCorrelatingEventHandler;
import com.solacesystems.jcsmp.ProducerEventArgs;
import com.solacesystems.jcsmp.Topic;
import com.solacesystems.jcsmp.XMLMessageConsumer;
import com.solacesystems.jcsmp.XMLMessageListener;
import com.solacesystems.jcsmp.XMLMessageProducer;

public class GpsGenerator {

	private static GpsGenerator INSTANCE = null;
	
    public final static int GPS_UPDATE_RATE_MS = 5000;
    
    final String host;
    final String vpn;
    final String user;
    final String pw;

    JCSMPSession session = null;
    XMLMessageProducer producer = null;
    XMLMessageConsumer consumer = null;
    volatile boolean connected = false;
    
    BusTracker busTracker = new BusTracker();
    ScheduledExecutorService service = Executors.newScheduledThreadPool(20);
    
    //public static Broadcaster INSTANCE;
    public static Topic TOPIC_BROADCAST = JCSMPFactory.onlyInstance().createTopic("bus_trak/comms/broadcast");
    public static Topic TOPIC_DISPATCH = JCSMPFactory.onlyInstance().createTopic("bus_trak/comms/dispatch");

    public static void initializeSingletonBroadcaster(String host, String vpn, String user, String pw) {
    	if (INSTANCE != null) throw new AssertionError();
    	else INSTANCE = new GpsGenerator(host,vpn,user,pw);
    }
    
    public static GpsGenerator onlyInstance() {
    	if (INSTANCE == null) throw new AssertionError("Instance hasn't been initialized!");
    	else return INSTANCE;
    }
    
    private GpsGenerator(String host, String vpn, String user, String pw) {
        this.host = host;
        this.vpn = vpn;
        this.user = user;
        this.pw = pw;
    }
    
    void sendMessage(BytesXMLMessage message, Destination dest) {
        //System.out.println("sending to "+topic+" -- "+message.dump());
        if (4.4 > 3.3) {
	        try {
	        	//Topic destination = JCSMPFactory.onlyInstance().createTopic(topic);
	        	if (connected) producer.send(message,dest);
	        	else {
//	        		System.out.println("not connected, can't send");
	        	}
	        } catch (JCSMPException e) {
	        	System.out.println("Had an issue sending a message");
	        }
        }
    }
    
    
    
    /////////////////////////////////// USER INTERACTION PARTS

    
    void addRandomBus(int number) {
        for (int i=0;i<number;i++) {
        	Bus bus = busTracker.addRandomBus();
            service.scheduleAtFixedRate(bus,(long)(Math.random()*GPS_UPDATE_RATE_MS),GPS_UPDATE_RATE_MS,TimeUnit.MILLISECONDS);
//            System.out.println("added a bus");
        }
    }
    
    void addBues() {
    	busTracker.initBuses();
    	for (Bus bus : busTracker.buses) {
            service.scheduleAtFixedRate(bus,(long)(Math.random()*GPS_UPDATE_RATE_MS),GPS_UPDATE_RATE_MS,TimeUnit.MILLISECONDS);
        }
    	System.out.println(busTracker.buses.size()+" buses added.");
    }
    

    void run() throws JCSMPException {
        System.out.println("About to create GpsGenerator session.");
        JCSMPProperties properties = new JCSMPProperties();
        properties.setProperty(JCSMPProperties.HOST,host);
        properties.setProperty(JCSMPProperties.VPN_NAME,vpn);
        properties.setProperty(JCSMPProperties.USERNAME,user);
        properties.setProperty(JCSMPProperties.PASSWORD,pw);
        JCSMPChannelProperties cp = new JCSMPChannelProperties();
        cp.setReconnectRetries(-1);
        properties.setProperty(JCSMPProperties.CLIENT_CHANNEL_PROPERTIES,cp);
        session = JCSMPFactory.onlyInstance().createSession(properties);
        session.connect();
        session.setProperty(JCSMPProperties.CLIENT_NAME,"gpsgen_"+session.getProperty(JCSMPProperties.CLIENT_NAME));
        try {
	        try {
	            producer = session.getMessageProducer(new JCSMPStreamingPublishCorrelatingEventHandler() {
	                
	                @Override
	                public void responseReceivedEx(Object key) {
	                	// won't since we're only sending Direct messages
	                	System.out.println("prodcer response received");
	                }
	                
	                @Override
	                public void handleErrorEx(Object key, JCSMPException cause, long timestamp) {
	                	// shouldn't get a publisher error (NACK), but may have exceptions thrown for connectivity
	                	System.err.println("Producer handleError() got something: "+cause.toString());
	                }
	            }, new JCSMPProducerEventHandler() {
					@Override
					public void handleEvent(ProducerEventArgs event) {
	                	System.err.println("Producer EVENT handler got something: "+event.toString());
						
					}
				});
	            consumer = session.getMessageConsumer(new JCSMPReconnectEventHandler() {
	                
	                @Override
	                public boolean preReconnect() throws JCSMPException {
	                	System.out.println("RECONNECTING...");
	                	connected = false;
	                    return true;
	                }
	                
	                @Override
	                public void postReconnect() throws JCSMPException {
	                	System.out.println("RECONNECTED!");
	                    connected = true;
	                }
	            }, new XMLMessageListener() {
	                @Override
	                public void onReceive(BytesXMLMessage message) {
	                	System.out.println("HERE");
	                	System.out.println(message.dump());
	                    /* this is where you will add a switch/case statement to call various methods to
	                     *  a) add n more random cars
	                     *  b) add a user car
	                     *  c) fault a user car
	                     *  etc.
	                     *      geo/bus/vehNum/lat/lon/routeNum/status
	                     *      geo/taxi/vehNum/lat/lon/status
	                     *      geo/train/vehNum/lat/lon/routeNum
	                			comms/bus/1234
	                			comms/route/012
	                			comms/broadcast
	                			comms/dispatch
							    ctrl/bus/vehNum/stop
							    ctrl/bus/vehNum/start
							    ctrl/bus/vehNum/flat
							    ctrl/bus/vehNum/crash
							    ctrl/bus/vehNum/fix
	                     */
	                	String topic = message.getDestination().getName();
	                	
	                	System.out.println(topic);
	                	
	                	if (topic.startsWith("bus_trak/comms/")) {
	                		if (topic.equals(TOPIC_DISPATCH.getName())) {  // this one is meant for me, command and control
	                			// or maybe the map display... I dunno
	                			
	                		} else if (message.getDestination().equals(TOPIC_BROADCAST)) {
	                			// need to tell every bus that something has happened
	                			for (Bus bus : busTracker.buses) {
	                				bus.receiveMessage(message);
	                			}
	                		} else if (topic.startsWith("bus_trak/comms/route/")) {
	                			int routeNum = Integer.parseInt(topic.split("\\/")[2]);
	                			for (Bus bus : busTracker.busByRoute.get(routeNum)) {
	                				bus.receiveMessage(message);
	                			}
	                		} else if (topic.startsWith("bus_trak/comms/bus/")) {
	                			int busNum = Integer.parseInt(topic.split("\\/")[2]);
	                			busTracker.getBus(busNum).receiveMessage(message);
	                		} else {
	                			// have no idea what other kind of comms message I'd receive
	                		}
	                		
	                	} else if (topic.startsWith("bus_trak/ctrl/")) {
                			int busNum = Integer.parseInt(topic.split("\\/")[2]);
                			String action = topic.split("\\/")[3];
                			if (action.equals("start")) {
                				busTracker.getBus(busNum).startBus();
                			} else if (action.equals("stop")) {
                				busTracker.getBus(busNum).stopBus();
                			} 
	                	} else {
	                		System.err.println("Received unexpected bus topic: "+topic);
	                	}
	                }
	                
	                @Override
	                public void onException(JCSMPException exception) {
	                	System.out.println("EXCEPTION");
	                    // won't happen as all Direct messages
	                }
	            });
	            System.out.println("Sending \"GPS\" data now...");
	            connected = true;
	            session.addSubscription(JCSMPFactory.onlyInstance().createTopic("bus_trak/ctrl/>"),true);
	            session.addSubscription(JCSMPFactory.onlyInstance().createTopic("bus_trak/comms/>"),true);
	            consumer.start();
	        } catch (JCSMPException e) {
	        	// should only throw an exception during start up (from this thread)... otherwise the exception
	        	// will be thrown and caught by other processes
	        	System.err.println("GpsGenerator: I have had an exception thrown to be at the outside");
	        	System.err.println(Thread.currentThread().getName());
	            e.printStackTrace();
	        }
            while (true) {
                Thread.sleep(100000);
            }
        } catch (InterruptedException e) {
            System.out.println("Thread interrupted... terminating");
            // ready to fall out
        } finally {
            if (session != null) {
                session.closeSession();
            }
        }
    }


    public static void main(String... args) throws FileNotFoundException, JCSMPException {
        if (args.length < 3) {
            System.out.println("Not enough args");
            System.out.println("Usage: GpsGenerator <IP or hostname> <vpn> <user> [password]");
            System.exit(-1);
        }
        String host = args[0];
        String vpn = args[1];
        String user = args[2];
        String pw = args.length > 3 ? args[3] : "";

  
        initializeSingletonBroadcaster(host, vpn, user, pw);
//        onlyInstance().addBues();
        onlyInstance().addRandomBus(500);
        //onlyInstance().addRandomTaxi(1000);
        onlyInstance().run();
    }
}
