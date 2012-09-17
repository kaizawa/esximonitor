/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cafeform.esxi.esximonitor;

import java.awt.Color;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.logging.Logger;
import javax.swing.*;


/**
 *
 * @author kaizawa
 */
public class NewServerDialog extends JDialog implements ActionListener, KeyListener {
    public static Logger logger = Logger.getLogger(NewServerDialog.class.getName());
    JTextField hostnameTextField = new JTextField(10);
    JPasswordField passwordTextField = new JPasswordField();
    JTextField usernameTextField = new JTextField("root");    
    Main esximon;
    private String hostname;
    private String username;
    private String password;
    ServerManager manager;
    Server server;
    
    public String getHostname(){
        return hostname;
    }

    public NewServerDialog(Main esximon) {
        super(esximon, "New Server", true);
        this.manager = esximon.getServerManager();
        /* Contents panel */
        Container contentpane = this.getContentPane();
        contentpane.setBackground(Color.white);
        contentpane.add(createNewServerPanel());
        pack();
    }
    
    @Override
    public void actionPerformed(ActionEvent ae) {
        String cmd = ae.getActionCommand();
        final JDialog dialog = this;


        logger.finer("get " + cmd + " action command");
        if ("Add".equals(cmd)) {
            hostname = hostnameTextField.getText();
            username = usernameTextField.getText();
            password = new String(passwordTextField.getPassword());            
            doAdd();
            this.setVisible(false);
            this.dispose();
        } else if ("Cancel".equals(cmd)) {
            this.setVisible(false);
            this.dispose();
        }

        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                dialog.pack();
            }
        });
    }
    
    private void doAdd(){
        System.out.println("doAdd called " + hostname);
        server = new Server(hostname, username, password);
        manager.addServer(server);
        hostnameTextField.setText("");
        passwordTextField.setText("");
    }
    
    public Server getNewServer(){
        return server;
    }
    
    @Override
    public void keyTyped(KeyEvent ke) {
    }

    @Override
    public void keyPressed(KeyEvent ke) {
        if (ke.getKeyCode() == 10) {
            final JDialog dialog = this;
            hostname = hostnameTextField.getText();
            username = usernameTextField.getText();
            password = new String(passwordTextField.getPassword());
            doAdd();
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    dialog.pack();
                }
            });
        }
    }

    @Override
    public void keyReleased(KeyEvent ke) {
    }
    
    private JComponent createNewServerPanel() {
        JPanel newServerPanel = new JPanel();
        newServerPanel.setLayout(new BoxLayout(newServerPanel, BoxLayout.Y_AXIS));

        JPanel textPanel = new JPanel();
        JButton addButton = new JButton("Add");
        JButton cancelButton = new JButton("Cancel");        
        addButton.addActionListener(this);
        cancelButton.addActionListener(this);

        textPanel.setLayout(new GridLayout(2, 3));
        textPanel.add(new JLabel("Hostame"));
        textPanel.add(new JLabel("User"));
        textPanel.add(new JLabel("Password"));
        textPanel.add(hostnameTextField);
        textPanel.add(usernameTextField);
        textPanel.add(passwordTextField);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addButton);
        buttonPanel.add(cancelButton);
        newServerPanel.add(textPanel);
        buttonPanel.setAlignmentX(LEFT_ALIGNMENT);
        newServerPanel.add(buttonPanel);

        return newServerPanel;
    }    
    
}
