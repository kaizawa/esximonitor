package com.cafeform.esxi.esximonitor;

import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.mo.VirtualMachine;
import java.util.logging.Logger;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

/**
 * Stands for virtual machine row entry listed in main window.
 */
public class VirtualMachineRowEntry
{
    public static final Logger logger = Logger.getLogger(VirtualMachineRowEntry.class.getName());
    VirtualMachine vm;
    Server server;
    private final ObjectProperty<Label> status = new SimpleObjectProperty<>();
    private final ObjectProperty<HBox> buttonBox = new SimpleObjectProperty<>();    
    private final StringProperty vmName = new SimpleStringProperty();
    private final StringProperty osType = new SimpleStringProperty();
    private final StringProperty serverName = new SimpleStringProperty();
    static Image powerOnImage;
    static Image powerOffImage;
    final public static int iconSize = 15;
    EsxiMonitorViewController controller;

    public VirtualMachineRowEntry (
            VirtualMachine vm,
            Server server,
            EsxiMonitorViewController controller)
    {
        this.vm = vm;
        this.server = server;
        this.controller = controller;
        status.set(new Label());
        if (vm.getSummary().getRuntime().getPowerState().
                equals(VirtualMachinePowerState.poweredOn)) 
        {
            status.set(new Label("", new ImageView(powerOnImage)));
        } 
        else 
        {
            status.set(new Label("", new ImageView(powerOffImage)));            
        }
        vmName.set(vm.getName());
        osType.set(vm.getConfig().getGuestFullName());
        serverName.set(server.getHostname());
        buttonBox.set(new OperationButtonBox(vm, server, controller));
    }
    
    // Load Icons
    static 
    {
        try 
        {
            powerOnImage = 
                    new Image("com/cafeform/esxi/esximonitor/lightbulb.png"); 
            powerOffImage = 
                    new Image("com/cafeform/esxi/esximonitor/lightbulb_off.png");
        } 
        catch (Exception ex)
        {
            logger.severe("cannot load icon image");
        }
    }
    
    public ObjectProperty<Label> statusProperty ()
    {
        return status;
    }
    
    public Label getStatus () 
    {
        return status.get();
    }
    
    public void setSattus(Label label){
        status.set(label);
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
    
    public HBox getButtonBox()
    {
        return buttonBox.get();
    }

    public void setButtonBox(HBox value)
    {
        buttonBox.set(value);
    }

    public ObjectProperty buttonBoxProperty()
    {
        return buttonBox;
    }
}
