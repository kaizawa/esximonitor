package com.cafeform.esxi.esximonitor;

import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Manage ESXi server to be monitored.
 * This is concrete class of ServerManager interface
 */
public class ServerManagerImpl implements ServerManager
{
    public static final Logger logger = Logger.getLogger(ServerManager.class.getName());
    private Server defaultServer = null;
    private ObservableList<Server> serverList = null;

    @Override
    public Server getDefaultServer() 
    {
        if (null == defaultServer)
        {
            String defaultServerName = Prefs.getDefaultServerPreference();
            defaultServer = getServerByHostname(defaultServerName);
        }
        return defaultServer;
    }

    /**
     * Specify default ESXi host to be shown in main window. 
     * This method must be called from actionPerformed. 
     * (except for first load without default server setting)
     *
     * @param hostname
     */
    @Deprecated
    @Override
    public void setDefaultServerByHostname(String hostname) 
            throws MalformedURLException, RemoteException
    {
        if (null == hostname)
        {
            setDefaultServer(null);
        } else
        {
            setDefaultServer(getServerByHostname(hostname));
        }
    }

    @Override
    public synchronized void setDefaultServer(Server server) 
            throws MalformedURLException, RemoteException
    {
        if (null == server)
        {
            Prefs.setDefaultServerPreference("");
            defaultServer = null;
        } 
        else
        {
            // First connection with current server
            if (null != getDefaultServer()) 
            {
                getDefaultServer().resetServer();
            }
            // Change default server to new server name.
            Prefs.setDefaultServerPreference(server.getHostname());
            defaultServer = server;
        }
    }

    /**
     * Return List of ESXi host info stored in preferences.
     */
    @Override
    public ObservableList<Server> getServerList()
    {
         // If this is first time to get server list, get list from Preferences.
         // After that, manage list in own list.
        if (null == serverList)
        {
            serverList = FXCollections.observableArrayList();
            try
            {
                logger.finer("getServers called");
                for (String hostname : Prefs.getServersPreferences().childrenNames())
                {
                    logger.log(Level.FINEST, "Server: {0}", hostname);
                    Server server = new ServerImpl();
                    Preferences serverPrefs = Prefs.getServersPreferences().node(hostname);
                    server.setHostname(hostname);
                    server.setUsername(serverPrefs.get("username", ""));
                    server.setPassword(serverPrefs.get("password", ""));
                    serverList.add(server);
                }
            } 
            catch (BackingStoreException ex)
            {
                logger.log(Level.INFO, 
                        "Cannot get server list from preferences", ex);
                DialogFactory.showSimpleDialog(
                        "Cannot get server list from preferences", "Error", null);
            }
        }
        return serverList;
    }

    @Override
    public synchronized Server getServerByHostname(String hostname)
    {
        for (Server server : getServerList())
        {
            if (server.getHostname().equals(hostname))
            {
                return server;
            }
        }
        return null;
    }

    @Override
    public synchronized void addServer(Server server) 
            throws MalformedURLException, RemoteException
    {
        Prefs.addServer(
                server.getHostname(), 
                server.getUsername(), 
                server.getPassword());
        if (0 == getServerList().size() || null == getDefaultServer())
        {
            // This is a first server or no default server is set yet.
            // Set this server as a default server.
            setDefaultServer(server);
        }
        getServerList().add(server);
        logger.log(Level.FINER, "{0} is added", server.getHostname());
    }

    @Override
    public synchronized void removeServer(Server server)
    {
        Prefs.removeServer(server.getHostname());
        getServerList().remove(server);
    }

    @Override
    public void removeServerByHostname(String hostname)
    {
        removeServer(getServerByHostname(hostname));
    }

    @Override
    public void editServer(Server server, String username, String password)
    {
        server.setUsername(username);
        server.setPassword(password);
        Prefs.addServer(server.getHostname(), username, password);
    } 
}
