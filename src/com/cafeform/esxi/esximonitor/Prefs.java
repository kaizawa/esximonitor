package com.cafeform.esxi.esximonitor;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 *
 * @author ka78231
 */
public class Prefs {

    static Logger logger = Logger.getLogger(Prefs.class.getName());
    static Preferences rootPrefs;
    static Preferences serversPrefs;

    static List<Server> getServers() {
        List<Server> serverList = new ArrayList<Server>();
        try {
            logger.finer("getServers called");
            for(String hostname : getServersPreferences().childrenNames()){
                logger.finest("Server: " + hostname);                
                Server server = new Server();
                Preferences serverPrefs = serversPrefs.node(hostname);
                server.setHostname(hostname);
                server.setUsername(serverPrefs.get("username", ""));
                server.setPassword(serverPrefs.get("password", ""));
                serverList.add(server);
            }
        } catch (BackingStoreException ex) {
            ex.printStackTrace();
        }
        return serverList;
    }

    static public Preferences getRootPreferences() {
        if (rootPrefs == null) {
            rootPrefs = Preferences.userNodeForPackage(Main.class);
        }
        return rootPrefs;
    }

    static public Preferences getServersPreferences() {
        if (serversPrefs == null) {
            serversPrefs = getRootPreferences().node("servers");
        }
        return serversPrefs;
    }
    
    static public void putServer(String hostname, String username, String password)
    {
        logger.finer("hostname=" + hostname + ", username=" + username);
        Preferences serverPrefs = getServersPreferences().node(hostname);
        serverPrefs.put("hostname", hostname);
        serverPrefs.put("username", username);        
        serverPrefs.put("password", password);
        
    }
    
    static public void popServer(String hostname)
    {
        logger.finer("hostname=" + hostname);                        
        Preferences serverPrefs = getServersPreferences().node(hostname);
        try {
            serverPrefs.removeNode();
        } catch (BackingStoreException ex) {
            ex.printStackTrace();
        }
    }
}
