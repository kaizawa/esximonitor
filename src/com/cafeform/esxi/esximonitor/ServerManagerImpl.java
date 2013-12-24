/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cafeform.esxi.esximonitor;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 *
 * @author kaizawa
 */
public class ServerManagerImpl implements ServerManager
{
    public static final Logger logger = Logger.getLogger(ServerManager.class.getName());
    private Server defaultServer = null;
    private List<Server> serverList = null;

    @Override
    public Server getDefaultServer() throws NoDefaultServerException
    {
        if (null == defaultServer)
        {
            String defaultServerName = Prefs.getDefaultServerPreference();
            defaultServer = getServerByHostname(defaultServerName);
            if (null == defaultServer)
            {
                throw new NoDefaultServerException();
            }
        }
        return defaultServer;
    }

    /**
     * Specify default ESXi host to be shown in main window. This method must be
     * called from actionPerformed. (except for first load without default
     * server setting)
     *
     * @param hostname
     */
    @Override
    public void setDefaultServerByHostname(String hostname)
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
    {
        if (null == server)
        {
            Prefs.setDefaultServerPreference("");
            defaultServer = null;
        } 
        else
        {
            /*
             * First connection with current server
             */
            try
            {
                getDefaultServer().resetServer();
            } 
            catch (NoDefaultServerException ex)
            {
                /*
                 * could be here, if this is first access
                 */
            }
            /*
             * Change default server to new server name.
             */
            Prefs.setDefaultServerPreference(server.getHostname());
            defaultServer = server;
        }
    }

    /**
     * Return List of ESXi host info stored in preferences.
     *
     */
    @Override
    public List<Server> getServerList()
    {
        /* 
         * If this is first time to get server list, get list from Preferences.
         * After that, manage list in own list.
         */
        if (null == serverList)
        {
            serverList = new ArrayList<>();
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
            } catch (BackingStoreException ex)
            {
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
    {
        Prefs.addServer(
                server.getHostname(), 
                server.getUsername(), 
                server.getPassword());
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
