package com.cafeform.esxi.esximonitor;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 *
 */
public class DialogViewController {

    @FXML
    private Label message;
    private boolean response = false;

    public boolean getResponse()
    {
        return response;
    }

    public void setResponse(boolean response)
    {
        this.response = response;
    }
    
    void setMessage(String message)
    {
       this.message.setText(message);
    }
    
    @FXML
    private void handleCancel (ActionEvent event)
    {
        response = false;
        message.getScene().getWindow().hide();
    }
    
    @FXML
    private void handleOk (ActionEvent event)
    {
        response = true;
        message.getScene().getWindow().hide();
    }
}
