package com.solace.aaron.demo.geo.gps;

import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.solacesystems.jcsmp.InvalidPropertiesException;
import com.solacesystems.jcsmp.JCSMPChannelProperties;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.SessionEventArgs;
import com.solacesystems.jcsmp.SessionEventHandler;

public class SimpleGpsPublisher implements Runnable {
    
    final String host;
    final String vpn;
    final String user;
    final String pw;
    
    final List<List<Point2D.Double>> coords = new ArrayList<>();
    
    JCSMPSession session = null;
    
    private final Logger LOGGER = LogManager.getLogger(SimpleGpsPublisher.class);
    
    public SimpleGpsPublisher(String host, String vpn, String user, String pw) throws IOException {
        // should probably check they're not empty or some weird chars..?
        this.host = host;
        this.vpn = vpn;
        this.user = user;
        this.pw = pw;
        
        loadFile();
    } 
    
    private static final String FILENAME = "coords2.txt";
    
    private void loadFile() throws IOException {
        LOGGER.info("Attempting to load file: "+FILENAME);
        List<String> lines = new ArrayList<>();
        ClassLoader classLoader = this.getClass().getClassLoader();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(classLoader.getResourceAsStream(FILENAME)))) {
            lines = reader.lines().collect(Collectors.toList());
        }
        LOGGER.info("Success!");
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
        }
        LOGGER.info("Loaded "+coords.size()+" routes");
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
                    
                    // TODO Auto-generated method stub
                    
                }
            });
        } catch (InvalidPropertiesException e) {
            e.printStackTrace();
            
            
            
        }
        
        
        


    
    
    
    
    
    }
    
    
    
    
    
    
    
    public static void main(String... args) throws IOException {
        
        
        SimpleGpsPublisher pub = new SimpleGpsPublisher("adsf","asdf","adsf","asdf");
        
        
        
        
        
        
        
    }
}
