/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cafeform.esxi.esximonitor;

import java.awt.*;
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

    public NewServerDialog(Main esximon) {
        super(esximon, "New Server", true);
        this.esximon = esximon;
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
            String hostname = hostnameTextField.getText();
            String username = usernameTextField.getText();
            String password = new String(passwordTextField.getPassword());            
            doAdd(hostname, username, password);  
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
    
    private void doAdd(String hostname, String username, String password) {
        Prefs.putServer(hostname, username, password);
        esximon.getModel().addElement(hostname);
        hostnameTextField.setText("");
        passwordTextField.setText("");
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