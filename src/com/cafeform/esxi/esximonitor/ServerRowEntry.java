package com.cafeform.esxi.esximonitor;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.input.MouseEvent;

/**
 * Stands for ESXi host in ESXi server host list window
 */
public class ServerRowEntry
{
    // Note: 
    // Since there's no no need to observe these values dinamically,
    // no xxxProperty method is created here. (I mean, omitted)
    // PropertyValueFactory class in ServerListViewController will 
    // take care of this situaion and handle these properties as static values.
    private Button deleteButton;
    private String hostName;
    private Button editButton;

    public ServerRowEntry(
            final Server server,
            final ServerListViewController controller,
            final Control parent)
    {
        editButton = new Button("Edit");
        deleteButton = new Button("Del");
        hostName = server.getHostname();
        final ServerManager manager = controller.getServerManager();
        
        deleteButton.addEventHandler(
                MouseEvent.MOUSE_CLICKED, 
                new EventHandler<MouseEvent>()
                {
                    @Override
                    public void handle(MouseEvent t)
                    {
                        manager.removeServer(server);
                        controller.updateServerList(manager);                        
                    }
                });

        editButton.addEventHandler(
                MouseEvent.MOUSE_CLICKED,
                new EventHandler<MouseEvent>()
                {
                    @Override
                    public void handle(MouseEvent t)
                    {
                        try
                        {
                            ServerListViewController.createEditServerWindow(
                                    manager,
                                    server,
                                    parent.getScene().getWindow());
                        } 
                        catch (IOException ex)
                        {
                            //TODO: show erro dialog
                            Logger.getLogger(
                                    EsxiMonitorViewController.class.getName()).
                                    log(Level.SEVERE, null, ex);
                        }
                    }
                });
    }

    public Button getEditButton()
    {
        return editButton;
    }

    public void setEditButton(Button editButton)
    {
        this.editButton = editButton;
    }

    public Button getDeleteButton()
    {
        return deleteButton;
    }

    public void setDeleteButton(Button deleteButton)
    {
        this.deleteButton = deleteButton;
    }


    public String getHostName()
    {
        return hostName;
    }

    public void setHostName(String hostName)
    {
        this.hostName = hostName;
    }

}
