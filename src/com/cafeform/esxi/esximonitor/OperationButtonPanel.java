package com.cafeform.esxi.esximonitor;

import com.cafeform.esxi.RecieveErrorMessageException;
import com.cafeform.esxi.VM;
import com.cafeform.esxi.Vmsvc;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
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
    
    private OperationButtonPanel() {
    }
    private VM vm;

    public OperationButtonPanel(VM vm) throws IOException {
        this.vm = vm;

        this.setLayout(new GridLayout(1, 4));
        
        /* Power off */
        JButton powerOffButton = new JButton(ESXiMonitor.getSizedImageIcon("com/cafeform/esxi/esximonitor/control_stop_blue.png"));
        powerOffButton.setToolTipText("Power OFF");
        powerOffButton.setActionCommand("poweroff");
        powerOffButton.addActionListener(this);
        if(vm.isPoweron() == false)
            powerOffButton.setEnabled(false);

        /* Power On */
        JButton powerOnButton = new JButton(ESXiMonitor.getSizedImageIcon("com/cafeform/esxi/esximonitor/control_play_blue.png"));
        powerOnButton.setToolTipText("Power ON");
        powerOnButton.setActionCommand("poweron");
        powerOnButton.addActionListener(this);
        if(vm.isPoweron())
            powerOnButton.setEnabled(false); 

        /* Power reset */
        JButton resetButton = new JButton(ESXiMonitor.getSizedImageIcon("com/cafeform/esxi/esximonitor/control_pause_blue.png"));
        resetButton.setToolTipText("Reset");
        resetButton.setActionCommand("reset");      
        resetButton.addActionListener(this);
        if(vm.isPoweron() == false)
            resetButton.setEnabled(false);

        /* Shutdown Guest OS */
        JButton shutdownButton = new JButton(ESXiMonitor.getSizedImageIcon("com/cafeform/esxi/esximonitor/exclamation.png"));
        shutdownButton.setToolTipText("Shutdown Guest OS");
        shutdownButton.setActionCommand("shutdown");
        shutdownButton.addActionListener(this);
        if(vm.isPoweron() == false)
            shutdownButton.setEnabled(false);         

        this.add(powerOnButton);
        this.add(powerOffButton);
        this.add(resetButton);
        this.add(shutdownButton);
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        Vmsvc vmsvc = ESXiMonitor.getConnection().getVmsvc();
        logger.finer(ae.getActionCommand() + " event recieved.");
        
        try {
        if ("poweroff".equals(ae.getActionCommand())) {
            int response = JOptionPane.showConfirmDialog(this.getParent(), 
                    "Are you sure want to power down \"" + vm.getName() + "\" ?",
                    "Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);            
            if(response == JOptionPane.NO_OPTION)
                return;
            else
                vmsvc.powerOff(vm.getVmid());
        } else if ("poweron".equals(ae.getActionCommand())) {
            vmsvc.powerOn(vm.getVmid());            
        } else if ("reset".equals(ae.getActionCommand())) {   
            int response = JOptionPane.showConfirmDialog(this.getParent(), 
                    "Are you sure want to reset \"" + vm.getName() + "\" ?",
                    "Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);            
            if(response == JOptionPane.NO_OPTION)
                return;
            else            
            vmsvc.powerReset(vm.getVmid());                        
        } else if ("shutdown".equals(ae.getActionCommand())) {      
            int response = JOptionPane.showConfirmDialog(this.getParent(), 
                    "Are you sure want to shutdown \"" + vm.getName() + "\" ?",
                    "Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);            
            if(response == JOptionPane.NO_OPTION)
                return;
            else            
            vmsvc.powerShutdown(vm.getVmid());                        
        }
        } catch (RecieveErrorMessageException ex ){
            JOptionPane.showMessageDialog(this.getParent(),ex.getMessage(), "Error", JOptionPane.WARNING_MESSAGE);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        ESXiMonitor.getInstance().updateVMLIstPanel();
        logger.finer("panel update request posted");        
    }
}
