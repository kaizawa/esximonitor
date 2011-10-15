package com.cafeform.esxi.esximonitor;

import com.cafeform.esxi.ESXiConnection;
import com.cafeform.esxi.VM;
import com.cafeform.esxi.Vmsvc;
import com.vmware.vim25.InvalidState;
import com.vmware.vim25.RestrictedVersion;
import com.vmware.vim25.RuntimeFault;
import com.vmware.vim25.TaskInProgress;
import com.vmware.vim25.ToolsUnavailable;
import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;
import java.awt.Color;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * Panel for buttons for virtial macnine operations
 * 
 */
public class OperationButtonPanel extends JPanel implements ActionListener {

    static public Logger logger = Logger.getLogger(OperationButtonPanel.class.getName());
    private Main esximon;

    private OperationButtonPanel() {
    }
    private VirtualMachine vm;
    static Icon control_stop_blue = null;
    static Icon control_play_blue = null;
    static Icon control_pause_blue = null;
    static Icon exclamation = null;

    /* Load Icons */
    static {
        try {
            control_stop_blue = Main.getScaledImageIcon("com/cafeform/esxi/esximonitor/control_stop_blue.png");
            control_play_blue = Main.getScaledImageIcon("com/cafeform/esxi/esximonitor/control_play_blue.png");
            control_pause_blue = Main.getScaledImageIcon("com/cafeform/esxi/esximonitor/control_pause_blue.png");
            exclamation = Main.getScaledImageIcon("com/cafeform/esxi/esximonitor/exclamation.png");
        } catch (IOException ex) {
            logger.severe("Cannot load icon image");
        }
    }

    public OperationButtonPanel(Main esximon, VirtualMachine vm) {
        this.vm = vm;
        this.esximon = esximon;
        boolean poweredOn = vm.getSummary().getRuntime().getPowerState().equals(VirtualMachinePowerState.poweredOn);

        this.setLayout(new GridLayout(1, 4));
        this.setBackground(Color.white);

        /* Power off */
        JButton powerOffButton = new JButton(control_stop_blue);
        powerOffButton.setBackground(Color.white);
        powerOffButton.setToolTipText("Power OFF");
        powerOffButton.setActionCommand("poweroff");
        powerOffButton.addActionListener(this);
        if (poweredOn == false) {
            powerOffButton.setEnabled(false);
        }

        /* Power On */
        JButton powerOnButton = new JButton(control_play_blue);
        powerOffButton.setBackground(Color.white);
        powerOnButton.setToolTipText("Power ON");
        powerOnButton.setActionCommand("poweron");
        powerOnButton.addActionListener(this);
        if (poweredOn) {
            powerOnButton.setEnabled(false);
        }

        /* Power reset */
        JButton resetButton = new JButton(control_pause_blue);
        resetButton.setBackground(Color.white);
        resetButton.setToolTipText("Reset");
        resetButton.setActionCommand("reset");
        resetButton.addActionListener(this);
        if (poweredOn == false) {
            resetButton.setEnabled(false);
        }

        /* Shutdown Guest OS */
        JButton shutdownButton = new JButton(exclamation);
        shutdownButton.setBackground(Color.white);
        shutdownButton.setToolTipText("Shutdown Guest OS");
        shutdownButton.setActionCommand("shutdown");
        shutdownButton.addActionListener(this);
        if (poweredOn == false) {
            shutdownButton.setEnabled(false);
        }

        this.add(powerOnButton);
        this.add(powerOffButton);
        this.add(resetButton);
        this.add(shutdownButton);
        setMaximumSize(getSize());
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        final String actionCommand = ae.getActionCommand();
        logger.finer(ae.getActionCommand() + " event for " + vm.getName() + " recieved.");
        ExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        /* try to execute command in backgroup */
        executor.submit(new Runnable() {

            @Override
            public void run() {
                doCommand(actionCommand);
            }
        });
    }

    private void doCommand(String command) throws HeadlessException {
        esximon.getProgressBar().setIndeterminate(true);
        
                System.out.println("GuestID: " + vm.getGuest().getGuestId());
                
        try {
            Task task = null;
            if ("poweroff".equals(command)) {
                int response = JOptionPane.showConfirmDialog(esximon,
                        "Are you sure want to power down \"" + vm.getName() + "\" ?",
                        "Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (response == JOptionPane.NO_OPTION) {
                    return;
                } else {
                    task = vm.powerOffVM_Task();
                }
            } else if ("poweron".equals(command)) {
                task = vm.powerOnVM_Task(null);
            } else if ("reset".equals(command)) {
                int response = JOptionPane.showConfirmDialog(esximon,
                        "Are you sure want to reset \"" + vm.getName() + "\" ?",
                        "Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (response == JOptionPane.NO_OPTION) {
                    return;
                } else {
                    task = vm.resetVM_Task();
                }
            } else if ("shutdown".equals(command)) {
                int response = JOptionPane.showConfirmDialog(esximon,
                        "Are you sure want to shutdown \"" + vm.getName() + "\" ?",
                        "Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (response == JOptionPane.NO_OPTION) {
                    return;
                } else {
                    vm.shutdownGuest();
                }
            }
            if (task != null) {
                task.waitForTask();
            }
        } catch (InterruptedException ex) {
            // interrupted 
        } catch (InvalidState ex) {
            JOptionPane.showMessageDialog(esximon, "Invalid State\n", "Error", JOptionPane.WARNING_MESSAGE);
        } catch (TaskInProgress ex) {
            JOptionPane.showMessageDialog(esximon, "Task Inprogress\n"
                    + ex.getMessage() + "\n" + ex.getTask().getVal()
                    + "\n" + ex.getTask().getType(), "Error", JOptionPane.WARNING_MESSAGE);
        } catch (ToolsUnavailable ex) {
            JOptionPane.showMessageDialog(esximon, "Cannot complete operation "
                    + "because VMware\n Tools is not running in this virtual machine.", "Error", JOptionPane.WARNING_MESSAGE);
        } catch (RestrictedVersion ex) {
            try {
                /* Seems remote ESXi server doesn't accept command via VI API
                 * try to run command via SSH
                 */
                runCommandViaSsh(command);
            } catch (Exception ex2) {
                /* Fummm, command faild via SSH too... Report the result to user. */
                logger.severe("runCommandViaSSH recieved IOException");
                ex.printStackTrace();
                JOptionPane.showMessageDialog(esximon, "RestrictedVersion\n", "Error", JOptionPane.WARNING_MESSAGE);
            }

        } catch (RuntimeFault ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(esximon, "RuntimeFault\n", "Error", JOptionPane.WARNING_MESSAGE);
        } catch (RemoteException ex) {
            JOptionPane.showMessageDialog(esximon, "RemoteFault\n", "Error", JOptionPane.WARNING_MESSAGE);
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            esximon.getProgressBar().setIndeterminate(false);
        }
        esximon.updateVMLIstPanel();
        logger.finer("panel update request posted");
    }

    void runCommandViaSsh(String actionCommand) throws IOException {
        logger.finer("runCommandViaSsh called");
        ESXiConnection conn = new ESXiConnection(esximon.getHostname(), esximon.getUsername(), esximon.getPassword());
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
        logger.finer("Submit " + actionCommand + " via SSH succeeded");
    }
}
