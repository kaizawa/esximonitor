package com.cafeform.esxi.esximonitor;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;

/**
 * Dialog which show ESXi Server list.<br>
 * And it also allows to add new ESXi host.
 * 
 */
public class ServerDialog extends JDialog implements ActionListener, KeyListener {

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

        /* Main Panel */
        JPanel dialogPanel = new JPanel();
        dialogPanel.setLayout(new BoxLayout(dialogPanel, BoxLayout.Y_AXIS));
        
        /* Contents panel */
        JPanel contentsPanel = new JPanel();
        contentsPanel.setBorder(new LineBorder(Color.GRAY));
        contentsPanel.setLayout(new BoxLayout(contentsPanel, BoxLayout.Y_AXIS));
        
        /* Button Panel */
        JPanel buttonPanel = new JPanel();
        JButton okButton = new JButton("OK");
        okButton.setAlignmentX(LEFT_ALIGNMENT);
        okButton.addActionListener(this);
        buttonPanel.add(okButton);

        JLabel serverListLabel = new JLabel("ESXi Host List");
        serverListLabel.setAlignmentX(CENTER_ALIGNMENT);        
        contentsPanel.add(serverListLabel);        
        
        contentsPanel.add(serverListScrollPane);
        
        JLabel addServerLabel = new JLabel("Add New ESXi Host");
        addServerLabel.setAlignmentX(CENTER_ALIGNMENT);        
        
        contentsPanel.add(addServerLabel);

        contentsPanel.add(createNewServerPanel());

        dialogPanel.add(contentsPanel);
        dialogPanel.add(buttonPanel);

        this.getContentPane().add(dialogPanel);
        updateServerList();
        this.pack();
    }

    private void doAdd(String hostname, String username, String password) {
        Prefs.putServer(hostname, username, password);
        esximon.getModel().addElement(hostname);
        hostnameTextField.setText("");
        passwordTextField.setText("");
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
                JButton deleteButton = new JButton("Delete");
                deleteButton.setActionCommand("Delete:" + server.getHostname());
                deleteButton.addActionListener(this);

                serverPanel.add(serverLabel);
                serverPanel.add(deleteButton);
                serverPanel.setAlignmentX(LEFT_ALIGNMENT);
                serverListPanel.add(serverPanel);
            }
        }
        serverListScrollPane.getViewport().setView(serverListPanel);
    }

    private JComponent createNewServerPanel() {
        JPanel newServerPanel = new JPanel();
        newServerPanel.setLayout(new BoxLayout(newServerPanel, BoxLayout.Y_AXIS));

        JPanel textPanel = new JPanel();
        JButton addButton = new JButton("Add");
        addButton.addActionListener(this);

        hostnameTextField.addKeyListener(this);
        usernameTextField.addKeyListener(this);
        passwordTextField.addKeyListener(this);

        textPanel.setLayout(new GridLayout(2, 3));
        textPanel.add(new JLabel("Hostame"));
        textPanel.add(new JLabel("User"));
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


        logger.finer("get " + cmd + " action command");
        if ("Add".equals(cmd)) {
            String hostname = hostnameTextField.getText();
            String username = usernameTextField.getText();
            String password = new String(passwordTextField.getPassword());            
            doAdd(hostname, username, password);
        } else if (cmd.startsWith("Delete")) {
            String pair[] = cmd.split(":", 2);
            String hostname = pair[1];
            logger.finer("get Delete action command. " + hostname);
            Prefs.popServer(hostname);
            
            JComboBox combo = esximon.getServerComboBox();
            DefaultComboBoxModel model = esximon.getModel();    
            for(int i = 0; i < model.getSize(); i++) {
                String name = (String) model.getElementAt(i);
                if(name.equals(hostname)){
                    model.removeElementAt(i);
                    logger.info(hostname + " removed from server list");
                    break;
                }
            }
//            esximon.getServerComboBox().repaint();
            
            
        } else if ("OK".equals(cmd)) {
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

    @Override
    public void keyTyped(KeyEvent ke) {
    }

    @Override
    public void keyPressed(KeyEvent ke) {
        if (ke.getKeyCode() == 10) {
            final JDialog dialog = this;
            String hostname = hostnameTextField.getText();
            String username = usernameTextField.getText();
            String password = new String(passwordTextField.getPassword());
            doAdd(hostname, username, password);
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    updateServerList();
                    dialog.pack();
                }
            });
        }
    }

    @Override
    public void keyReleased(KeyEvent ke) {
    }
}
