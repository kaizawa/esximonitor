package com.cafeform.esxi.esximonitor;

import com.cafeform.esxi.RecieveErrorMessageException;
import static com.cafeform.esxi.esximonitor.CommandType.*;
import com.vmware.vim25.*;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Window;

/**
 * Panel for buttons for virtial macnine operations
 *
 */
public class OperationButtonBox extends HBox
{
    private static final Logger logger = 
            Logger.getLogger(OperationButtonBox.class.getName());    
    final public static int iconSize = 15;
    private final VirtualMachine vm;
    private ImageView stopIcon;
    private ImageView playIcon;
    private ImageView pauseIcon;
    private ImageView exclamationIcon;
    private final Server server;
    private final ExecutorService executor = 
            Executors.newSingleThreadScheduledExecutor();
    EsxiMonitorViewController controller;

    public OperationButtonBox(
            VirtualMachine vm, 
            Server server,
            EsxiMonitorViewController controller)
    {
        loadImages();

        this.vm = vm;
        this.server = server;
        boolean poweredOn = vm.getSummary().getRuntime().getPowerState().
                equals(VirtualMachinePowerState.poweredOn);
        this.controller = controller;

        // Power off
        Button powerOffButton = new Button("", stopIcon);
        powerOffButton.setDisable(!poweredOn);
        //powerOffButton.setToolTipText("Power OFF");
        powerOffButton.addEventHandler(
                MouseEvent.MOUSE_CLICKED,
                new ButtonEventHandler(POWER_OFF));

        /* Power On */
        Button powerOnButton = new Button("", playIcon);
        powerOnButton.setDisable(poweredOn);
        powerOnButton.addEventHandler(
                MouseEvent.MOUSE_CLICKED,
                new ButtonEventHandler(POWER_ON));

        /* Power reset */
        Button resetButton = new Button("", pauseIcon);
        resetButton.setDisable(!poweredOn);
        resetButton.addEventHandler(
                MouseEvent.MOUSE_CLICKED,
                new ButtonEventHandler(RESET));

        /* Shutdown Guest OS */
        Button shutdownButton = new Button("", exclamationIcon);
        shutdownButton.setDisable(!poweredOn);
        shutdownButton.addEventHandler(
                MouseEvent.MOUSE_CLICKED,
                new ButtonEventHandler(SHUTDOWN));

        this.getChildren().addAll(
                powerOnButton,
                powerOffButton,
                resetButton,
                shutdownButton);
    }

    private void doCommand(CommandType command) 
    {
        try
        {
            Task task = null;
            switch(command)
            {
                case POWER_OFF:
                    task = vm.powerOffVM_Task();
                    break;
                case POWER_ON:
                    task = vm.powerOnVM_Task(null);                        
                    break;
                case RESET:
                    task = vm.resetVM_Task();
                    break;
                case SHUTDOWN:
                    vm.shutdownGuest();
                    break;
            }
            if (task != null)
            {
                task.waitForTask();
            }
        } 
        catch (InterruptedException ex)
        {
            // interrupted 
        } 
        catch (InvalidState ex)
        {
            DialogFactory.showSimpleDialog(
                    "Invalid State\n", 
                    "Error", 
                    this.getScene().getWindow());
        } 
        catch (TaskInProgress ex)
        {
            DialogFactory.showSimpleDialog("Task Inprogress\n"  +
                    ex.getMessage() + "\n" + ex.getTask().getVal() + 
                    "\n" + ex.getTask().getType(), 
                    "Error", 
                    getWindow());
        } 
        catch (ToolsUnavailable ex)
        {
            DialogFactory.showSimpleDialog("Cannot complete operation " +
                    "because VMware\n Tools is not running in this virtual machine.", 
                    "Error", 
                    getWindow());
        } 
        catch (RestrictedVersion ex)
        {
            try
            {
                /* Seems remote ESXi server doesn't accept command via VI API
                 * try to run command via SSH
                 */
                logger.finer("Get RestrictedVersion from ESXi. Try command via SSH.");
                server.runCommandViaSsh(command, vm);
            } 
            catch (RecieveErrorMessageException ex2)
            {
                DialogFactory.showSimpleDialog(
                        ex2.getMessage(), 
                        "Error", 
                        getWindow());
            } 
            catch (IOException ex3)
            {
                // Ouch, command faild via SSH too... 
                // Report the result to user.
                logger.log(Level.SEVERE, "runCommandViaSSH recieved {0}", 
                        ex3.toString());
                DialogFactory.showSimpleDialog(ex3.toString(), 
                        "Error", 
                        getWindow());
            }

        } 
        catch (RuntimeFault ex)
        {
            DialogFactory.showSimpleDialog(
                    "RuntimeFault\n",
                    "Error", 
                    getWindow());
        } 
        catch (RemoteException ex)
        {
            DialogFactory.showSimpleDialog(
                    "RemoteFault\n",
                    "Error",
                    getWindow());
        } 
        finally
        {
            //controller.getProgressBar().setProgress(0f);
        }
        controller.updateVmListPanel();
        logger.finer("panel update request posted");
    }
    
    private Window getWindow ()
    {
        return this.getScene().getWindow();
    }

    private void loadImages()
    {
        stopIcon = new ImageView(
                new Image("com/cafeform/esxi/esximonitor/control_stop_blue.png"));
        playIcon = new ImageView(
                new Image("com/cafeform/esxi/esximonitor/control_play_blue.png"));
        pauseIcon = new ImageView(
                new Image("com/cafeform/esxi/esximonitor/control_pause_blue.png"));
        exclamationIcon = new ImageView(
                new Image("com/cafeform/esxi/esximonitor/exclamation.png"));
    }
    
    private class ButtonEventHandler implements EventHandler<MouseEvent>
    {
        private final CommandType command;
        public ButtonEventHandler(CommandType command)
        {
            this.command = command;
        }
        
        @Override
        public void handle(MouseEvent t)
        {
            if (CommandType.POWER_OFF.equals(command) ||
                    CommandType.RESET.equals(command) ||
                    CommandType.SHUTDOWN.equals(command))
            {
                boolean ok = DialogFactory.showBooleanDialog(
                        "Are you sure want to " + command +  " " +
                                vm.getName() + "?",
                        "Confirmation",
                        getWindow());                
                if (!ok)
                {
                    return;
                } 
            } 
            controller.getProgressBar().setProgress(-1.0f);
            controller.getStatusLabel().setText("Running " + command +
                    " on " + vm.getName());

            executor.submit(new javafx.concurrent.Task<Void>()
            {

                @Override
                protected Void call() throws Exception
                {
                    doCommand(command);
                    return null;
                }
                
                @Override
                protected void succeeded()
                {
                    controller.getProgressBar().setProgress(0f);
                    controller.getStatusLabel().setText("");
                }
            });
                    
        }
    }
}
