package com.cafeform.esxi.esximonitor;

import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main Class for EsxiMonitor Application
 */
public class EsxiMonitor extends Application {

    
    @Override
    public void start(Stage stage) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("EsxiMonitorView.fxml"));
        
        Scene scene = new Scene(root);               
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }    
}
