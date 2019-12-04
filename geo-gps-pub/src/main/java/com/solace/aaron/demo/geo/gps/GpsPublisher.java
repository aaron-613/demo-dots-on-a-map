package com.solace.aaron.demo.geo.gps;

import com.solacesystems.jcsmp.JCSMPChannelProperties;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPSession;

public class GpsPublisher implements Runnable {

    
    final String host;
    final String vpn;
    final String user;
    final String pw;
    
    JCSMPSession session = null;
    
    
    
    public GpsPublisher(String host, String vpn, String user, String pw) {
        // should probably check they're not empty or some weird chars..?
        this.host = host;
        this.vpn = vpn;
        this.user = user;
        this.pw = pw;
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
        //session = JCSMPFactory.onlyInstance().createSession(arg0,arg1,arg2)
        
        
        


    
    
    
    
    
    }
    
    
    
    
    
    
    
    public static void main(String ... args) {
        
        
        
        
        
        
        
        
        
        
    }
}
