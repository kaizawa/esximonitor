package com.cafeform.esxi.esximonitor;

import com.vmware.vim25.mo.VirtualMachine;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.*;
import javax.swing.border.LineBorder;

/**
 * Dialog which show ESXi Server list.<br>
 * And it also allows to add new ESXi host.
 * 
 */
public class ServerDialog extends JDialog implements ActionListener {

    static public Logger logger = Logger.getLogger(ServerDialog.class.getName());
    Main esximon;
    JScrollPane serverListScrollPane = new JScrollPane();
    Preferences rootPref = Prefs.getRootPreferences();
    
    private VirtualMachine vm;
    static Icon delete_button = null;

    /* Load Icons */
    static {
        try {
            delete_button = Main.getScaledImageIcon("com/cafeform/esxi/esximonitor/delete.png");
        } catch (IOException ex) {
            logger.severe("Cannot load icon image");
        }
    }

    public ServerDialog(Main esximon) {
        super(esximon, "Server", true);
        logger.finer("ServerDialog called");
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        this.esximon = esximon;
        this.setBackground(Color.white);

        /* Main Panel */
        JPanel dialogPanel = new JPanel();
        dialogPanel.setLayout(new BoxLayout(dialogPanel, BoxLayout.Y_AXIS));
        
        /* Contents panel */
        JPanel contentsPanel = new JPanel();
        contentsPanel.setBackground(Color.white);
        contentsPanel.setBorder(new LineBorder(Color.GRAY));
        contentsPanel.setLayout(new BoxLayout(contentsPanel, BoxLayout.Y_AXIS));
        
        /* Button Panel */
        JPanel buttonPanel = new JPanel();
        JButton okButton = new JButton("OK");
        JButton newButton = new JButton("New");
        okButton.addActionListener(this);
        newButton.addActionListener(this);
        buttonPanel.add(newButton);
        buttonPanel.add(okButton);
        serverListScrollPane.setBackground(Color.white);

        JLabel serverListLabel = new JLabel("ESXi Host List");
        serverListLabel.setAlignmentX(CENTER_ALIGNMENT);        
        contentsPanel.add(serverListLabel);        
        contentsPanel.add(serverListScrollPane);
        dialogPanel.add(contentsPanel);
        dialogPanel.add(buttonPanel);

        this.getContentPane().add(dialogPanel);
        updateServerList();
        this.pack();
    }

    /**
     * Update ESXi host list shown in this Dialog window
     */
    private void updateServerList() {
        List<Server> serverList = Prefs.getServers();
        JPanel serverListPanel = new JPanel();
        if (serverList.size() > 0) {
            serverListPanel.setLayout(new BoxLayout(serverListPanel, BoxLayout.Y_AXIS));
            for (Server server : Prefs.getServers()) {
                logger.finer("Server: " + server.getHostname());
                JPanel serverPanel = new JPanel();
                JLabel serverLabel = new JLabel(server.getHostname());
                serverPanel.setBackground(Color.white);
                serverPanel.setLayout(new BoxLayout(serverPanel, BoxLayout.X_AXIS));
                JButton deleteButton = new JButton(delete_button);
                deleteButton.setActionCommand("Delete:" + server.getHostname());
                deleteButton.addActionListener(this);

                serverPanel.add(deleteButton);                
                serverPanel.add(serverLabel);
                serverPanel.setAlignmentX(LEFT_ALIGNMENT);
                serverListPanel.add(serverPanel);
            }
        }
        serverListScrollPane.getViewport().setView(serverListPanel);
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        String cmd = ae.getActionCommand();
        final JDialog dialog = this;
        DefaultComboBoxModel model = esximon.getModel();    

        logger.finer("get " + cmd + " action command");
        if ("New".equals(cmd)) {
            NewServerDialog newDialog = new NewServerDialog(esximon);
            newDialog.setVisible(true); 
            String newHostname = newDialog.getHostname();
            model.insertElementAt(newHostname, 0);
            updateServerList();
        } else if (cmd.startsWith("Delete")) {
            String pair[] = cmd.split(":", 2);
            String hostname = pair[1];
            logger.finer("get Delete action command. " + hostname);
            Prefs.popServer(hostname);
            
            JComboBox combo = esximon.getServerComboBox();
            for(int i = 0; i < model.getSize(); i++) {
                String name = (String) model.getElementAt(i);
                if(name.equals(hostname)){
                    model.removeElementAt(i);
                    logger.info(hostname + " removed from server list");
                    break;
                }
            }
            combo.repaint();
            esximon.updateVMLIstPanel();            
        } else if ("OK".equals(cmd)) {
            if(null == model.getSelectedItem()){
                model.setSelectedItem(model.getElementAt(0));
            }
            this.setVisible(false);
            this.dispose();
        }

        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                updateServerList();
                dialog.pack();
            }
        });
    }
}
