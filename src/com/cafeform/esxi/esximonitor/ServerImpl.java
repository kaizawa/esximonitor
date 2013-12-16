/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cafeform.esxi.esximonitor;

import com.cafeform.esxi.ESXiConnection;
import com.cafeform.esxi.VM;
import com.cafeform.esxi.Vmsvc;
import com.vmware.vim25.InvalidLogin;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.logging.Logger;

/**
 *
 * @author kaizawa
 */
public class ServerImpl implements Server 
{
    private String hostname;
    private String username;
    private String password;
    private ServiceInstance serviceInstance = null;
    private Folder rootFolder = null;
    public static final Logger logger = Logger.getLogger(Server.class.getName());
    
    public ServerImpl(){}
    
    public ServerImpl(String hostname, String username, String password) {
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

    /**
     * Reset current connection with this server
     */
    public void resetServer() {
        try {
            ServiceInstance svcInst = getServiceInstance();
            if (null != svcInst) {
                svcInst.getServerConnection().logout();
            }
            setServiceInstance(null);
            setRootFolder(null);
        } catch (MalformedURLException ex) {
            logger.severe(ex.toString());
        } catch (RemoteException ex) {
            logger.severe(ex.toString());
        }
    }

    /**
     * @param aServiceInstance the serviceInstance to set
     */
    private void setServiceInstance(ServiceInstance aServiceInstance) {
        serviceInstance = aServiceInstance;
    }

    /**
     * @param aRootFolder the rootFolder to set
     */
    private void setRootFolder(Folder aRootFolder) {
        rootFolder = aRootFolder;
    }

    /**
     * @return the serviceInstance
     */
    private ServiceInstance getServiceInstance() throws MalformedURLException, RemoteException {
        if ("".equals(getHostname())) {
            logger.finer("getHostname returns null");
            return null;
        }
        
        if (serviceInstance == null) {
            logger.finer("serviceInstance is null");
            serviceInstance = new ServiceInstance(new URL("https://" + getHostname() + "/sdk"), getUsername(), getPassword(), true);
        }
        return serviceInstance;
    }

    /**
     * @return the rootFolder
     */
    private Folder getRootFolder() throws RemoteException, MalformedURLException {
        if (rootFolder == null) {
            rootFolder = getServiceInstance().getRootFolder();
        }
        return rootFolder;
    }    
    
    public void runCommandViaSsh(String actionCommand, VirtualMachine vm) throws IOException {
        logger.finer("runCommandViaSsh called");
        ESXiConnection conn = new ESXiConnection(hostname, username, password);
        Vmsvc vmsvc = conn.getVmsvc();
        int vmid = -1;
        
        for (VM ssh_vm : vmsvc.getAllvms()) {
            if (ssh_vm.getName().equals(vm.getName())) {
                vmid = ssh_vm.getVmid();
            }
        }
        
        if ("poweroff".equals(actionCommand)) {
            vmsvc.powerOff(vmid);
        } else if ("poweron".equals(actionCommand)) {
            vmsvc.powerOn(vmid);
        } else if ("reset".equals(actionCommand)) {
            vmsvc.powerReset(vmid);
        } else if ("shutdown".equals(actionCommand)) {
            vmsvc.powerShutdown(vmid);
        }
        logger.finer(vmsvc.powerGetState(vmid));
        logger.finer("Submit " + actionCommand + " via SSH succeeded");
    }
    
    public ManagedEntity[] getVirtualMachineArray (){
        return getManagedEntryArray("VirtualMachine");
    }
    
    public ManagedEntity[] getManagedEntryArray(String managedEntryName) {
        boolean retried = false; // retry once if error happen
        ManagedEntity[] managedEntryArray = new ManagedEntity[0];

        while (true) {
            try {
                logger.fine("RootFolder: " + getRootFolder().getName());
                managedEntryArray = new InventoryNavigator(getRootFolder()).searchManagedEntities(managedEntryName);
                if (managedEntryArray == null || managedEntryArray.length == 0) {
                    resetServer();
                    if (retried) {
                        logger.fine("no " + managedEntryName + " exist");
                        break;
                    }
                    logger.fine("no " + managedEntryName + " returned. retrying...");
                    retried = true;
                    continue;
                }
                logger.finer("total " + managedEntryArray.length +  " " + managedEntryName + " found ");
                break;
            } catch (InvalidLogin ex) {
                logger.finer("Login to " + getHostname() + " failed ");
                logger.severe(ex.toString());
                resetServer();
                break;
            } catch (RemoteException ex) {
                logger.finer("RemoteException happen when connecting to " + getHostname());
                logger.severe(ex.toString());
                resetServer();
                break;
            } catch (IOException ex) {
                logger.fine("retrying to conect to ESXi host...");
                resetServer();
                if (retried) {
                    ex.printStackTrace();
                    logger.severe("Cannot get  " + managedEntryName + " list");
                    break;
                }
                retried = true;
            }
        }
        return managedEntryArray;
    }
}
