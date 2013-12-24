package com.cafeform.esxi.esximonitor;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 *
 */
public class EditServerViewController implements Initializable
{
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

    @Override
    public void initialize(URL url, ResourceBundle rb)
    {

    }
    
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
    private void handleEdit (ActionEvent event)
    {
        if (null == manager)
        {
            throw new IllegalStateException("Manager is not set");
        }
        // Create new Server if this is new creationg.
        // Otherwise update existing Server object
        if (null == server)
        {
            if (null == manager)
            {
                throw new IllegalStateException("Manager is not set");
            }
            server = new ServerImpl(
                    hostname.getText(), 
                    username.getText(), 
                    password.getText());
            manager.addServer(server);
        } 
        else 
        {
            manager.editServer(server, username.getText(), password.getText());
        }
        password.getScene().getWindow().hide();       
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
}

