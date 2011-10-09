package com.cafeform.esxi.esximonitor;

/**
 *
 * @author ka78231
 */
public class Server {
    private String hostname;
    private String username;
    private String password;
    
    public Server(){}
    
    public Server(String hostname, String username, String password){
        this.hostname = hostname;
        this.username = username;
        this.password = password;
    }

    /**
     * @return the hostname
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * @param hostname the hostname to set
     */
    public void setHostname(String hostname) {
        this.hostname = hostname;
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
}
