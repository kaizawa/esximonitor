package com.cafeform.esxi.esximonitor;

import java.util.List;

public interface ServerManager 
{
    public Server getDefaultServer() throws NoDefaultServerException; 

    /**
     * Specify default ESXi host to be shown in main window. This method must be
     * called from actionPerformed. (except for first load without default
     * server setting)
     *
     * @param hostname
     */
    public void setDefaultServerByHostname(String hostname);

    public void setDefaultServer(Server server);

    /**
     * Return List of ESXi host info stored in preferences.
     * @return list of esxi host
     */
    public List<Server> getServerList();

    public Server getServerByHostname(String hostname);

    public void addServer(Server server);

    public void removeServer(Server server);

    public void removeServerByHostname(String hostname);

    public void editServer(Server server, String username, String password);
}
