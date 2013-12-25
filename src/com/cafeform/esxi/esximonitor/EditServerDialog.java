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
 * @deprecated this is SWING version. no longer used
 */
@Deprecated
public class EditServerDialog extends JDialog implements ActionListener, KeyListener {
    public static Logger logger = Logger.getLogger(EditServerDialog.class.getName());
    protected JTextField hostnameTextField = new JTextField(10);
    protected JPasswordField passwordTextField = new JPasswordField();
    protected JTextField usernameTextField = new JTextField("root");    
    protected Main esximon;
    protected String hostname;
    protected String username;
    protected String password;
    protected ServerManager manager;
    protected Server server;
    
    public String getHostname(){
        return hostname;
    }
    
    public EditServerDialog(Main esximon, Server server) {
        this(esximon);
        this.server = server;
        hostnameTextField.setText(server.getHostname());
        hostnameTextField.setEditable(false);
        usernameTextField.setText(server.getUsername());
        passwordTextField.setText(server.getPassword());        
    }
    
    public EditServerDialog(Main esximon){
        this(esximon, "Edit");
    }
    
    public EditServerDialog(Main esximon, String action) {
        super(esximon, action + " Server", true);   
        this.manager = esximon.getServerManager();
        /* Contents panel */
        Container contentpane = this.getContentPane();
        contentpane.setBackground(Color.white);
        contentpane.add(editServerPanel(action));
        pack();
    }
    
    protected void doAction(){
        System.out.println("doAction called " + hostname);
        manager.editServer(server, username, password);
    }
    
    @Override
    public void actionPerformed(ActionEvent ae) {
        String cmd = ae.getActionCommand();
        final JDialog dialog = this;

        logger.finer("get " + cmd + " action command");
        if ("doAction".equals(cmd)) {
            hostname = hostnameTextField.getText();
            username = usernameTextField.getText();
            password = new String(passwordTextField.getPassword());            
            doAction();
            hostnameTextField.setText("");
            passwordTextField.setText("");            
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
    
    public Server getServer(){
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
            doAction();
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
    
    protected final JComponent editServerPanel(String action) {
        JPanel newServerPanel = new JPanel();
        newServerPanel.setLayout(new BoxLayout(newServerPanel, BoxLayout.Y_AXIS));

        JPanel textPanel = new JPanel();
        JButton addButton = new JButton(action);
        addButton.setActionCommand("doAction");
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
