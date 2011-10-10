package com.cafeform.esxi.esximonitor;

import com.vmware.vim25.InvalidState;
import com.vmware.vim25.RuntimeFault;
import com.vmware.vim25.TaskInProgress;
import com.vmware.vim25.ToolsUnavailable;
import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.mo.Task;
import com.vmware.vim25.mo.VirtualMachine;
import java.awt.Color;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * Panel for buttons for virtial macnine operations
 * 
 */
public class OperationButtonPanel extends JPanel implements ActionListener {

    public Logger logger = Logger.getLogger(getClass().getName());
    private Main esximon;

    private OperationButtonPanel() {
    }
    private VirtualMachine vm;

    public OperationButtonPanel(Main esximon, VirtualMachine vm) throws IOException {
        this.vm = vm;
        this.esximon = esximon;
        boolean poweredOn = vm.getSummary().getRuntime().getPowerState().equals(VirtualMachinePowerState.poweredOn);

        this.setLayout(new GridLayout(1, 4));
        this.setBackground(Color.white);

        /* Power off */
        JButton powerOffButton = new JButton(Main.getScaledImageIcon("com/cafeform/esxi/esximonitor/control_stop_blue.png"));
        powerOffButton.setBackground(Color.white);
        powerOffButton.setToolTipText("Power OFF");
        powerOffButton.setActionCommand("poweroff");
        powerOffButton.addActionListener(this);
        if (poweredOn == false) {
            powerOffButton.setEnabled(false);
        }

        /* Power On */
        JButton powerOnButton = new JButton(Main.getScaledImageIcon("com/cafeform/esxi/esximonitor/control_play_blue.png"));
        powerOffButton.setBackground(Color.white);
        powerOnButton.setToolTipText("Power ON");
        powerOnButton.setActionCommand("poweron");
        powerOnButton.addActionListener(this);
        if (poweredOn) {
            powerOnButton.setEnabled(false);
        }

        /* Power reset */
        JButton resetButton = new JButton(Main.getScaledImageIcon("com/cafeform/esxi/esximonitor/control_pause_blue.png"));
        resetButton.setBackground(Color.white);        
        resetButton.setToolTipText("Reset");
        resetButton.setActionCommand("reset");
        resetButton.addActionListener(this);
        if (poweredOn == false) {
            resetButton.setEnabled(false);
        }

        /* Shutdown Guest OS */
        JButton shutdownButton = new JButton(Main.getScaledImageIcon("com/cafeform/esxi/esximonitor/exclamation.png"));
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
        logger.finer(ae.getActionCommand() + " event recieved.");
        ExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        /* try to execute command in backgroup */
        executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    Task task = null;
                    if ("poweroff".equals(actionCommand)) {
                        int response = JOptionPane.showConfirmDialog(esximon,
                                "Are you sure want to power down \"" + vm.getName() + "\" ?",
                                "Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                        if (response == JOptionPane.NO_OPTION) {
                            return;
                        } else {
                            task = vm.powerOffVM_Task();
                        }
                    } else if ("poweron".equals(actionCommand)) {
                        task = vm.powerOnVM_Task(null);
                    } else if ("reset".equals(actionCommand)) {
                        int response = JOptionPane.showConfirmDialog(esximon,
                                "Are you sure want to reset \"" + vm.getName() + "\" ?",
                                "Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                        if (response == JOptionPane.NO_OPTION) {
                            return;
                        } else {
                            task = vm.resetVM_Task();
                        }
                    } else if ("shutdown".equals(actionCommand)) {
                        int response = JOptionPane.showConfirmDialog(esximon,
                                "Are you sure want to shutdown \"" + vm.getName() + "\" ?",
                                "Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                        if (response == JOptionPane.NO_OPTION) {
                            return;
                        } else {
                            vm.shutdownGuest();
                        }
                    }
                    if(task != null)
                        task.waitForTask();
                } catch (InterruptedException ex){
                    // interrupted 
                } catch (InvalidState ex) {
                    JOptionPane.showMessageDialog(esximon, "Invalid State\n"
                            + ex.getMessage(), "Error", JOptionPane.WARNING_MESSAGE);
                } catch (TaskInProgress ex) {
                    JOptionPane.showMessageDialog(esximon, "Task In progress\n" 
                            + ex.getMessage() + "\n" + ex.getTask().getVal() + 
                            "\n" + ex.getTask().getType(), "Error", JOptionPane.WARNING_MESSAGE);
                } catch (ToolsUnavailable ex) {
                    JOptionPane.showMessageDialog(esximon, "Cannot complete operation "
                            + "because VMware\n Tools is not running in this virtual machine."
                            , "Error", JOptionPane.WARNING_MESSAGE);
                } catch (RuntimeFault ex) {
                    JOptionPane.showMessageDialog(esximon, "Runtime Fault\n"
                            +ex.getMessage() , "Error", JOptionPane.WARNING_MESSAGE);
                } catch (RemoteException ex) {
                    JOptionPane.showMessageDialog(esximon, "Remote Fault\n"
                            + ex.getMessage() , "Error", JOptionPane.WARNING_MESSAGE);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                esximon.updateVMLIstPanel();
                logger.finer("panel update request posted");                
            }
        });
    }
}
