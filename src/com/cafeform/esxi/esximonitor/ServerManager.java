package com.cafeform.esxi.esximonitor;

import java.net.MalformedURLException;
import java.rmi.RemoteException;
import javafx.collections.ObservableList;

/**
 * Manage list of ESXi host to be monitored.
 */
public interface ServerManager 
{
    public Server getDefaultServer() throws NoDefaultServerException; 

    /**
     * Specify default ESXi host to be shown in main window. This method must be
     * called from actionPerformed. (except for first load without default
     * server setting)
     *
     * @param hostname
     * @throws java.net.MalformedURLException
     * @throws java.rmi.RemoteException
     */
    public void setDefaultServerByHostname(String hostname)
            throws MalformedURLException, RemoteException;


    public void setDefaultServer(Server server) 
            throws MalformedURLException, RemoteException;

    /**
     * Return List of ESXi host info stored in preferences.
     * @return list of esxi host
     */
    public ObservableList<Server> getServerList();

    public Server getServerByHostname(String hostname);

    public void addServer(Server server)
            throws MalformedURLException, RemoteException;

    public void removeServer(Server server);

    public void removeServerByHostname(String hostname);

    public void editServer(Server server, String username, String password);
    
}
