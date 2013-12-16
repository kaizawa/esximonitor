package com.cafeform.esxi.esximonitor;

import java.awt.event.ActionListener;
import java.awt.event.KeyListener;


/**
 *
 * @author kaizawa
 */
public class NewServerDialog extends EditServerDialog implements ActionListener, KeyListener {

    public NewServerDialog(Main esximon) {
        super(esximon, "Add");
    }
    
    @Override
    protected void doAction(){
        System.out.println("doAdd called " + hostname);
        server = new ServerImpl(hostname, username, password);
        manager.addServer(server);
        hostnameTextField.setText("");
        passwordTextField.setText("");
    }    
}
