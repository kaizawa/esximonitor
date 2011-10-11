package com.cafeform.esxi.esximonitor;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Dialog which show ESXi Server list.<br>
 * And it also allows to add new ESXi host.
 * 
 */
public class ServerDialog extends JDialog implements ActionListener, ChangeListener {

    Logger logger = Logger.getLogger(getClass().getName());
    Main esximon;
    JTextField hostnameTextField = new JTextField();
    JPasswordField passwordTextField = new JPasswordField();
    JTextField usernameTextField = new JTextField("root");
    JScrollPane serverListScrollPane = new JScrollPane();
    Preferences rootPref = Prefs.getRootPreferences();

    public ServerDialog(Main esximon) {
        super(esximon, "Server", true);
        logger.finer("ServerDialog called");
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        this.esximon = esximon;
        this.setBackground(Color.white);

        JPanel dialogPanel = new JPanel();        
        dialogPanel.setLayout(new BoxLayout(dialogPanel, BoxLayout.Y_AXIS));        
        JPanel operationPanel = new JPanel();
        operationPanel.setBorder(new LineBorder(Color.GRAY));                

        JPanel buttonPanel = new JPanel();
        JButton okButton = new JButton("OK");
        okButton.addActionListener(this);
        okButton.setAlignmentX(RIGHT_ALIGNMENT);
        buttonPanel.add(okButton);

        operationPanel.setLayout(new BoxLayout(operationPanel, BoxLayout.Y_AXIS));
        operationPanel.add(serverListScrollPane);
        JLabel addServerLabel = new JLabel("Add new Server");
        addServerLabel.setAlignmentX(RIGHT_ALIGNMENT);
        operationPanel.add(addServerLabel);
        
        operationPanel.add(getNewServerPanel());
        
        JLabel selectServerLabel = new JLabel("Select Server");
        selectServerLabel.setAlignmentX(RIGHT_ALIGNMENT);      
        dialogPanel.add(selectServerLabel);
        dialogPanel.add(operationPanel);
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
        String defaultServer = rootPref.get("defaultServer", "");
        logger.finer("defaultserver="+ defaultServer);
        ButtonGroup buttonGroup = new ButtonGroup();
        if (serverList.size() > 0) {
            serverListPanel.setLayout(new GridLayout(serverList.size(), 3));
            for (Server server : Prefs.getServers()) {
                logger.finer("Server: " + server.getHostname());
                JPanel serverPanel = new JPanel();
                serverPanel.setBackground(Color.white);
                serverPanel.setLayout(new BoxLayout(serverPanel, BoxLayout.X_AXIS));
                boolean isDefault = server.getHostname().equals(defaultServer);
                JRadioButton defaultRadioButton = new JRadioButton(server.getHostname(), isDefault);
                defaultRadioButton.addChangeListener(this);
                JButton deleteButton = new JButton("Delete");
                deleteButton.setActionCommand("Delete:" + server.getHostname());
                deleteButton.addActionListener(this);
                buttonGroup.add(defaultRadioButton);

                serverPanel.add(defaultRadioButton);
                serverPanel.add(deleteButton);
                serverPanel.setAlignmentX(RIGHT_ALIGNMENT);
                serverListPanel.add(serverPanel);
            }
        }
        serverListScrollPane.getViewport().setView(serverListPanel);        
    }

    private JComponent getNewServerPanel() {
        JPanel newServerPanel = new JPanel();
        newServerPanel.setLayout(new BoxLayout(newServerPanel, BoxLayout.Y_AXIS));

        JPanel textPanel = new JPanel();
        JButton addButton = new JButton("Add");
        addButton.addActionListener(this);

        textPanel.setLayout(new GridLayout(2, 3));
        textPanel.add(new JLabel("Host Name"));
        textPanel.add(new JLabel("User Name"));
        textPanel.add(new JLabel("Password"));
        textPanel.add(hostnameTextField);
        textPanel.add(usernameTextField);
        textPanel.add(passwordTextField);

        newServerPanel.add(textPanel);
        newServerPanel.add(addButton);

        return newServerPanel;
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        String cmd = ae.getActionCommand();
        final JDialog dialog = this;
        String hostname = hostnameTextField.getText();
        String username = usernameTextField.getText();
        String password = new String(passwordTextField.getPassword());

        logger.finer("get " + cmd + " action command");
        if ("Add".equals(cmd)) {
            Prefs.putServer(hostname, username, password);
            esximon.setHostname(hostname);
            esximon.setUsername(username);
            esximon.setPassword(password);
            esximon.setDefaultServer(hostname);
            
            hostnameTextField.setText("");
            passwordTextField.setText("");    
            esximon.getModel().addElement(hostname);
        } else if (cmd.startsWith("Delete")) {
            String pair[] = cmd.split(":", 2);
            logger.finer("get Delete action command. " + pair[0] + ", " + pair[1]);
            Prefs.popServer(pair[1]);

            DefaultComboBoxModel model = esximon.getModel();
            for(int i = 0; i < model.getSize(); i++) {
                String name = (String) model.getElementAt(i);
                if(name.equals(hostname)){
                    model.removeElement(name);
                    break;
                }
            }
        } else if ("OK".equals(cmd)) { 
            this.setVisible(false);
            this.dispose();
            for(Server server : Prefs.getServers()){
                if(server.getHostname().equals(rootPref.get("defaultServer", ""))){
                    esximon.setHostname(server.getHostname());
                    esximon.setUsername(server.getUsername());
                    esximon.setPassword(server.getPassword());
                }
            }
            esximon.updateVMLIstPanel();
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                updateServerList();
                dialog.pack();
            }
        });
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        JRadioButton cb = (JRadioButton) e.getSource();
        String serverName = cb.getText();        
        logger.finer(serverName + " changed");
        if (cb.isSelected()) {
            logger.finer(serverName + " is selected.");
            esximon.setDefaultServer(serverName);
        }
    }
    
}
