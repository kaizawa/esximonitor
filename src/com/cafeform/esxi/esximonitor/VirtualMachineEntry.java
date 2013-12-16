package com.cafeform.esxi.esximonitor;

import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.mo.VirtualMachine;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 *
 * @author kaizawa
 */
public class VirtualMachineEntry
{
    VirtualMachine vm;
    Server server;
    private final BooleanProperty powerOn = new SimpleBooleanProperty();
    private final StringProperty vmName = new SimpleStringProperty();
    private final StringProperty osType = new SimpleStringProperty();
    private final StringProperty serverName = new SimpleStringProperty();

    public VirtualMachineEntry (VirtualMachine vm, Server server)
    {
        this.vm = vm;
        this.server = server;
        powerOn.set(vm.getSummary().getRuntime().getPowerState().
                equals(VirtualMachinePowerState.poweredOn));
        vmName.set(vm.getName());
        osType.set(vm.getConfig().getGuestFullName());
        serverName.set(server.getHostname());
    }

    public boolean isPowerOn()
    {
        return powerOn.get();
    }

    public void setPowerOn(boolean value)
    {
        powerOn.set(value);
    }

    public BooleanProperty powerOnProperty()
    {
        return powerOn;
    }

    public String getVmName()
    {
        return vmName.get();
    }

    public void setVmName(String value)
    {
        vmName.set(value);
    }

    public StringProperty vmNameProperty()
    {
        return vmName;
    }


    public String getOsType()
    {
        return osType.get();
    }

    public void setOsType(String value)
    {
        osType.set(value);
    }

    public StringProperty osTypeProperty()
    {
        return osType;
    }

    public String getServerName()
    {
        return serverName.get();
    }

    public void setServerName(String value)
    {
        serverName.set(value);
    }

    public StringProperty serverNameProperty()
    {
        return serverName;
    }
}
