package com.cafeform.esxi.esximonitor;

import com.vmware.vim25.mo.*;
import java.io.IOException;

/**
 * Interface which represents ESXi host.
 */
public interface Server 
{
    /**
     * @return the hostname
     */
    public String getHostname();

    /**
     * @param hostname the hostname to set
     */
    public void setHostname(String hostname);

    /**
     * @return the username
     */
    public String getUsername();


    /**
     * @param username the username to set
     */
    public void setUsername(String username);

    /**
     * @return the password
     */
    public String getPassword();

    /**
     * @param password the password to set
     */
    public void setPassword(String password);

    /**
     * Reset current connection with this server
     */
    public void resetServer();
    
    public void runCommandViaSsh(CommandType command, VirtualMachine vm) 
            throws IOException;

    public ManagedEntity[] getVirtualMachineArray ();
    
    public ManagedEntity[] getManagedEntryArray(String managedEntryName);
}
