package com.cafeform.esxi.esximonitor;

import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Window;

/**
 * JavaFX Controller for ESXi Server Edit window
 */
public class EditServerViewController
{
    public static final Logger logger = 
            Logger.getLogger(EditServerViewController.class.getName());
    @FXML
    private TextField username;
    @FXML
    private TextField hostname;
    @FXML
    private PasswordField password;
    private ServerManager manager;
    private Server server;
    @FXML
    private Button editButton;
    
    public void setManager (ServerManager manager)
    {
        this.manager = manager;
    }
    
    public void setServer (Server server)
    {
        this.server = server;
        hostname.setDisable(true);
        hostname.setText(server.getHostname());
        username.setText(server.getUsername());
        password.setText(server.getPassword());
    }
    
    @FXML
    private void handleEditButton (ActionEvent event) 
    {
        if (null == manager)
        {
            throw new IllegalStateException("Manager is not set");
        }
        // Create new Server if this is new creationg.
        // Otherwise update existing Server object
        if (null == server)
        {

                server = new ServerImpl(
                        hostname.getText(),
                        username.getText(),
                        password.getText());
            try {                
                manager.addServer(server);
            } 
            catch (MalformedURLException | RemoteException ex) 
            {
                DialogFactory.showSimpleDialogAndLog("Cannot add server", 
                        "Error", getWindow(), logger, Level.SEVERE, ex);
            }
        } 
        else 
        {
            manager.editServer(server, username.getText(), password.getText());
        }
        getWindow().hide();       
    }
    
    @FXML
    private void handleCancel (ActionEvent event)
    {
        password.getScene().getWindow().hide();        
    }

    void setButtonText(String text)
    {
        editButton.setText(text);
    }

    private Window getWindow()
    {
        return password.getScene().getWindow();
    }
}

