package com.cafeform.esxi.esximonitor;

import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Class handle Preferences used by this app.
 * 
 */
public class Prefs {

    static Logger logger = Logger.getLogger(Prefs.class.getName());
    static Preferences rootPrefs;
    static Preferences serversPrefs;

    /**
     * Return top of preference node for this app.
     * @return 
     */
    static private Preferences getRootPreferences() {
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

    /**
     * Store new ESXi host info to preferences
     * @param hostname
     * @param username
     * @param password 
     */
    static public void addServer(String hostname, String username, String password)
    {
        logger.finer("hostname=" + hostname + ", username=" + username);
        Preferences serverPrefs = getServersPreferences().node(hostname);
        serverPrefs.put("hostname", hostname);
        serverPrefs.put("username", username);        
        serverPrefs.put("password", password);
        
    }
    
    /**
     * Remove ESXi host information from preferences
     * @param hostname 
     */
    static public void removeServer(String hostname)
    {
        logger.finer("hostname=" + hostname);                        
        Preferences serverPrefs = getServersPreferences().node(hostname);
        try {
            serverPrefs.removeNode();
        } catch (BackingStoreException ex) {
            ex.printStackTrace();
        }
    }
    
    static public String getDefaultServerPreference() {
        Preferences rooPref = getRootPreferences();
        return(rooPref.get("defaultServer", ""));
    }
    
    static public void setDefaultServerPreference(String hostname){
        getRootPreferences().put("defaultServer", hostname);
    }
}
