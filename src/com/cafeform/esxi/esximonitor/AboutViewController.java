package com.cafeform.esxi.esximonitor;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

/**
 *
 */
public class AboutViewController implements Initializable
{
    @FXML
    private Label versionText;

    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
    }
    
    public void setVersion (String version)
    {
        versionText.setText("ESXiMonitor Version " + version);        
    }
    
    @FXML
    private void handleOk ()
    {
        versionText.getScene().getWindow().hide();
    }
}
