package com.cafeform.esxi;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.ConnectionInfo;
import ch.ethz.ssh2.InteractiveCallback;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;
import com.sun.org.apache.bcel.internal.generic.GETSTATIC;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author ka78231
 */
public class ESXiConnection extends Connection {

    private String username = "";
    private String password = "";
    private boolean connected = false;
    Logger logger = Logger.getLogger(getClass().getName());
    private Vmsvc vmsvc = null;


    public ESXiConnection(String server, String username, String password) {
        super(server);
        this.username = username;
        this.password = password;
    }

    public ESXiConnection(String server) {
        super(server);
    }

    public Vmsvc getVmsvc() {
        if(vmsvc == null){
            vmsvc = new Vmsvc(this);
        }
        return vmsvc;
    }

    /**
     * Callback routine for interactive login.
     */
    private class InteractiveCallBack implements InteractiveCallback {
        @Override
        public String[] replyToChallenge(String name, String instruction, 
        int numPrompts, String[] prompt, boolean[] echo) throws Exception {

            String[] responses = new String[numPrompts];
            logger.finer("name=" + name + ", instruction=" + instruction + ", numPrompts=" + numPrompts);
            for(int i = 0 ; i < numPrompts ; i++){
                logger.finer("prompts=" + prompt[i] + ", echo=" + echo[i]);
                /* If prompto contain [P]assword, then set password string */
                if(prompt[i].contains("assword")){
                    responses[i] = password;
                }
            }
            return responses;
        }
    }

    protected Session getSession() throws IOException {
        if (connected == false) {
            /* Now connect */
            connect();
            /* Authenticate.
             * If you get an IOException saying something like
             * "Authentication method password not supported by the server at this stage."
             * then please check the FAQ.            
             */
            logger.finer("Server accept keyboard-interractive: " + isAuthMethodAvailable(username, "keyboard-interactive"));
            logger.finer("Server accept password: " + isAuthMethodAvailable(username, "password"));            
            
            boolean isAuthenticated = false;

            if (isAuthMethodAvailable(username, "password")){                
                isAuthenticated = authenticateWithPassword(username, password);
            } else if(isAuthMethodAvailable(username, "keyboard-interactive")){
                isAuthenticated = authenticateWithKeyboardInteractive(username, new InteractiveCallBack());                     
            } else {
                throw new IOException("No supported authentication found.");                
            }
            
            if (isAuthenticated == false) {
                throw new IOException("Authentication failed.");
            }
            connected = true;
        }
        /* Create a session */
        return openSession();
    }

    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public synchronized void close() {
        /* First try to remove from connection list within Factory */
        ESXiConnectionFactory.remove(this);
        super.close();
    }
}
