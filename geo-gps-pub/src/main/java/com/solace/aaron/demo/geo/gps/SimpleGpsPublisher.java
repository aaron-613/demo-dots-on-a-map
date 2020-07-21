package com.solace.aaron.demo.geo.gps;

import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.solacesystems.jcsmp.BytesMessage;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.InvalidPropertiesException;
import com.solacesystems.jcsmp.JCSMPChannelProperties;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPProducerEventHandler;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPReconnectEventHandler;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.JCSMPStreamingPublishEventHandler;
import com.solacesystems.jcsmp.ProducerEventArgs;
import com.solacesystems.jcsmp.SessionEventArgs;
import com.solacesystems.jcsmp.SessionEventHandler;
import com.solacesystems.jcsmp.XMLMessageConsumer;
import com.solacesystems.jcsmp.XMLMessageListener;
import com.solacesystems.jcsmp.XMLMessageProducer;

public class SimpleGpsPublisher implements Runnable {
    
    private final String host;
    private final String vpn;
    private final String user;
    private final String pw;
    
    private JCSMPSession session = null;
    @SuppressWarnings("unused")
    private XMLMessageProducer producer = null;
    private XMLMessageConsumer consumer = null;
    private volatile boolean connected = false;

    final List<List<Point2D.Double>> coords = new ArrayList<>();
    final ScheduledExecutorService pool = Executors.newScheduledThreadPool(5);
    

    private static final Logger logger = LogManager.getLogger(SimpleGpsPublisher.class);
    
    public SimpleGpsPublisher(String host, String vpn, String user, String pw) throws IOException {
        // should probably check they're not empty or some weird chars..?
        this.host = host;
        this.vpn = vpn;
        this.user = user;
        this.pw = pw;
        // try to load the list of coordinates!
        loadFile();
    } 
    
    private static final String FILENAME = "coords2.txt";
    
    private void loadFile() throws IOException {
        logger.info("Attempting to load file: "+FILENAME);
        List<String> lines = new ArrayList<>();
        ClassLoader classLoader = this.getClass().getClassLoader();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(classLoader.getResourceAsStream(FILENAME)))) {
            lines = reader.lines().collect(Collectors.toList());
        }
        logger.info("Success!");
        for (String line : lines) {
            List<Point2D.Double> route = new ArrayList<>();
            String[] textCoords = line.split(";");
            for (String textCoord : textCoords) {
                String[] latlon = textCoord.split(",");
                double lat = Double.parseDouble(latlon[0]);
                double lon = Double.parseDouble(latlon[1]);
                route.add(new Point2D.Double(lon,lat));  // x=lon, y=lat
            }
            coords.add(route);
            new SimulatedBus(1,1,route);
        }
        logger.info("Loaded "+coords.size()+" routes");
        
        
        
    }
    
    private String makeTopic(int routeIndex) {
        StringBuilder sb = new StringBuilder();
        sb.append("geo/pos/");
        sb.append(routeIndex);
        return sb.toString();
    }
    

    @Override
    public void run() {
        JCSMPProperties props = new JCSMPProperties();
        props.setProperty(JCSMPProperties.HOST,host);
        props.setProperty(JCSMPProperties.VPN_NAME,vpn);
        props.setProperty(JCSMPProperties.USERNAME,user);
        props.setProperty(JCSMPProperties.PASSWORD,pw);
        JCSMPChannelProperties cp = new JCSMPChannelProperties();
        cp.setReconnectRetries(-1);  // retry forever!
        props.setProperty(JCSMPProperties.CLIENT_CHANNEL_PROPERTIES,cp);
        try {
            session = JCSMPFactory.onlyInstance().createSession(props,JCSMPFactory.onlyInstance().getDefaultContext(),new SessionEventHandler() {
                
                @Override
                public void handleEvent(SessionEventArgs event) {
                    // don't do much, just log
                    logger.info("Session Event Handler caught something: "+event.toString());
                }
            });
            session.setProperty(JCSMPProperties.CLIENT_NAME,"GpsPub_"+session.getProperty(JCSMPProperties.CLIENT_NAME));
            session.connect();
            logger.info("Connected!");
            connected = true;
            // create the message producer... just basic anonymous classes are fine
            producer = session.getMessageProducer(new JCSMPStreamingPublishEventHandler() {
                
                @Override
                public void responseReceived(String messageID) {
                    // publishing Direct messages, so this shouldn't be called?
                    logger.info("Streaming Publish Event Handler received response for messaageID "+messageID);
                }
                
                @Override
                public void handleError(String messageID, JCSMPException cause, long timestamp) {
                    // publishing Direct messages, so this shouldn't be called?
                    logger.warn("Streaming Publish Event Handler received error for messaageID "+messageID);
                    logger.warn(cause);
                }
            },new JCSMPProducerEventHandler() {
                @Override
                public void handleEvent(ProducerEventArgs event) {
                    // don't do much, just log
                    logger.info("Producer Event Handler received: "+event);
                }
            });
            // define an empty (for now) Message Consumer, to catch the reconnection events
            consumer = session.getMessageConsumer(new JCSMPReconnectEventHandler() {
                @Override
                public boolean preReconnect() throws JCSMPException {
                    if (connected) {
                        connected = false;
                        logger.info("Disconnected!");
                    }
                    logger.info("Trying to reconnect session...");
                    return true;
                }
                
                @Override
                public void postReconnect() throws JCSMPException {
                    logger.info("Reconnected!");
                    connected = true;
                }
            },new XMLMessageListener() {
                @Override
                public void onReceive(BytesXMLMessage message) {
                    // not subscribing yet, so no need to implement
                }
                
                @Override
                public void onException(JCSMPException e) {
                    logger.warn("Message Listener caught an exception",e);
                }
            });
            consumer.start();

            // block main thread, wait...
            try {
                while (true) {
//                    for (int i=0;i<coords.size();i++) {
                    for (int i=coords.size()-1;i>=0;i--) {
                        for (int j=0;j<coords.get(i).size();j++) {
                            BytesMessage msg = JCSMPFactory.onlyInstance().createMessage(BytesMessage.class);
                            String payload = String.format("{\"busNum\":%d, \"routeNum\":%d, \"lat\":%f, \"lon\":%f}",i,i,coords.get(i).get(j).y,coords.get(i).get(j).x);
                            //System.out.println(payload);
                            String topic = "gps/pos/"+i;
                            msg.setData(payload.getBytes(Charset.forName("UTF-8")));
                            producer.send(msg,JCSMPFactory.onlyInstance().createTopic(topic));
                            Thread.sleep(50);
                        }
                    }
                    
                    
                    
                    
                    
                    
                    
                    Thread.sleep(10000);
                }
            } catch (InterruptedException e) {
                logger.info("Main thread got interrupted, probably quitting!",e);
            }
        } catch (InvalidPropertiesException e) {
            logger.error("Something somehow went wrong trying to create the Session Properties",e);
        } catch (JCSMPException e) {
            logger.warn("Issue encountered during (?) connection?",e);
        } finally {
            try {
                pool.awaitTermination(1000,TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
            }
            pool.shutdownNow();
            if (consumer != null) {
                consumer.stop();
                consumer.close();
            }
            if (session != null) {
                session.closeSession();
            }
        }
        
        
        



    
    
    
    
    
    }
    
    
    
    
    
    
    
    public static void main(String... args) throws IOException {
        
        if (args.length < 3) {
            System.out.println("Not enough arguments!");
            System.out.println("Usage: SimpleGpsPublisher <host:port> <vpn-name> <username> [password]");
            System.exit(-1);
        }
        String host = args[0];
        String vpn = args[1];
        String user = args[2];
        String pw = "";
        if (args.length > 3) {
            pw = args[3];
        }
        SimpleGpsPublisher pub = new SimpleGpsPublisher(host,vpn,user,pw);
        
        pub.run();
        
        
        
        
        
    }
}
