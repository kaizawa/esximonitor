package com.cafeform.esxi.esximonitor;

import static com.cafeform.esxi.esximonitor.EsxiMonitorViewController.logger;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/**
 * Factory to create simple dialog
 */
public class DialogFactory
{
    static final Logger logger = Logger.getLogger(DialogFactory.class.getName());

    /**
     * Show dialog box which retrieve boolean value.
     * This method must be called from JavaFX application thread
     * @param title
     * @param message
     * @param parent
     * @return 
     */
    static boolean showBooleanDialog(
            String message,            
            String title,
            Window parent)
    {
        DialogViewController controller = showDialog(message, title, parent);
        return null == controller ? false : controller.getResponse();
    }

    /**
     * Show simple dialog.
     * This method can be called from non-JavaFX application thread.
     * @param title
     * @param message
     * @param parent 
     */
    static void showSimpleDialog(
            final String message,            
            final String title,
            final Window parent)
    {
        Platform.runLater(new Runnable()
        {
            @Override
            public void run()
            {
                showDialog(message, title, parent);
            }
        });
    }

    private static DialogViewController showDialog (
            String message, final String title, final Window parent)
    {
        try
        {
            FXMLLoader loader
                    = new FXMLLoader(DialogFactory.class.
                            getResource("DialogView.fxml"));
            loader.load();
            final Parent root = loader.getRoot();
            final DialogViewController controller = loader.getController();
            controller.setMessage(message);
            Scene scene = new Scene(root);
            Stage dialog = new Stage(StageStyle.UTILITY);
            dialog.setScene(scene);
            if (null != parent)
            {
                // Set parent window
                dialog.initOwner(parent);
                // Enable modal window
                dialog.initModality(Modality.WINDOW_MODAL);
            }
            dialog.setResizable(false);
            dialog.setTitle(title);
            dialog.showAndWait();
            return controller;
        } 
        catch (IOException ex)
        {
            logger.log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    static void showSimpleDialogAndLog(
            final String message,            
            final String title,
            final Window parent,
            Logger logger,                                    
            Level level,
            Throwable ex
            )
    {
        if(null != ex) {
            logger.log(Level.SEVERE, message, ex);
        } else {
            logger.log(Level.SEVERE, message);            
        }
        showSimpleDialog(message, title, parent);
    }
}
