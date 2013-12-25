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
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Window;

/**
 * Panel for buttons for virtial macnine operations
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
    private ImageView blankIcon;    
    private final Server server;
    EsxiMonitorViewController controller;
    private final boolean poweredOn;
    
    public OperationButtonBox(
            VirtualMachine vm, 
            Server server,
            EsxiMonitorViewController controller)
    {
        loadImages();

        this.vm = vm;
        this.server = server;
        poweredOn = vm.getSummary().getRuntime().getPowerState().
                equals(VirtualMachinePowerState.poweredOn);
        this.controller = controller;
        
        this.getChildren().addAll(
                createButton(playIcon, POWER_ON),
                createButton(stopIcon, POWER_OFF),
                createButton(pauseIcon, RESET),
                createButton(exclamationIcon, SHUTDOWN));  
    }

    private void doCommand(CommandType command) throws InterruptedException 
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
            DialogFactory.showSimpleDialog("Cannot complete operation because" +
                    " VMware Tools is not running in this virtual machine.", 
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
                logger.finer("Get RestrictedVersion from ESXi. " +
                        "Try command via SSH.");
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
        blankIcon = new ImageView(
                new Image("com/cafeform/esxi/esximonitor/control_blank.png"));
    }
    
    private Button createButton (
            ImageView icon, 
            final CommandType command)
    {
        final Button button;
        if ((poweredOn && POWER_ON.equals(command)) ||
                (!poweredOn && ! POWER_ON.equals(command)))
        {
            button = new Button("", blankIcon);            
            button.setDisable(true);
        }
        else 
        {
            button = new Button("", icon);
            button.setDisable(false);
        }
        
        // set tool tip need to berun on JavaFX application thread.
        Platform.runLater(new Runnable()
        {

            @Override
            public void run()
            {
                button.setTooltip(new Tooltip(command.toString()));                
            }
        });


        button.addEventHandler(
                MouseEvent.MOUSE_CLICKED,                        
                new ButtonEventHandler(command));
        return button;
    }
    
    private class ButtonEventHandler implements EventHandler<MouseEvent>
    {
        final ExecutorService executor = 
                Executors.newSingleThreadScheduledExecutor();

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
            executor.shutdown();                   
        }
    }
}
