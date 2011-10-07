
package com.cafeform.esxi;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Factory class of ESXiConnection
 * 
 */
public class ESXiConnectionFactory {
    Logger logger = Logger.getLogger(getClass().getName());    
    private static Map<String, ESXiConnection> serverMap = new ConcurrentHashMap<String, ESXiConnection>();    
    
    private ESXiConnectionFactory(){}

    /**
     * Return ESXiConnection instance for each server.
     * Create new Instance if it doesn't not exist.<br>
     * Otherwize return existing one.
     * @param server
     * @param username
     * @param password
     * @return 
     */
    public static ESXiConnection createInstance (String server, String username, String password) {
        ESXiConnection conn;
        
        if((conn = serverMap.get(server)) == null){
            conn = new ESXiConnection(server, username, password);
            serverMap.put(server, conn);
        } 
        
        return conn;
    }    
    
    /**
     * Return ESXiConnection instance for each server.<br>
     * Return null if doesn't exit.
     * 
     * @param server
     * @return 
     */
    public static ESXiConnection getInstance (String server) {
        return serverMap.get(server);
    }
    
    public static void remove(ESXiConnection conn){
        if(serverMap.containsValue(conn))
            serverMap.remove(conn);
    }
}
