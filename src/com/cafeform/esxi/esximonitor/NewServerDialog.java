package com.cafeform.esxi.esximonitor;

import java.awt.event.ActionListener;
import java.awt.event.KeyListener;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @deprecated This is Swing version. no longer used.
 */
@Deprecated
public class NewServerDialog extends EditServerDialog implements ActionListener, KeyListener {

    public NewServerDialog(Main esximon) {
        super(esximon, "Add");
    }
    
    @Override
    protected void doAction(){
        System.out.println("doAdd called " + hostname);
        server = new ServerImpl(hostname, username, password);
        try
        {
            manager.addServer(server);
        } 
        catch (MalformedURLException | RemoteException ex)
        {
            Logger.getLogger(NewServerDialog.class.getName()).log(Level.SEVERE, null, ex);
        }
        hostnameTextField.setText("");
        passwordTextField.setText("");
    }    
}
