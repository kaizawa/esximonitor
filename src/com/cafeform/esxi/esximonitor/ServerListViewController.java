package com.cafeform.esxi.esximonitor;

import com.vmware.vim25.mo.VirtualMachine;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/**
 * JavaFX Controller class for Server List window
 */
public class ServerListViewController implements Initializable
{
    private static final Logger logger 
            = Logger.getLogger(ServerListViewController.class.getName());
    private VirtualMachine vm;
    private ServerManager manager;

    @FXML
    private TableView<ServerRowEntry> serverListTable;
    @FXML
    private TableColumn<ServerRowEntry, Button> editButtonColumn;
    @FXML
    private TableColumn<ServerRowEntry, Button> deleteButtonColumn;
    @FXML
    private TableColumn<ServerRowEntry, String> hostNameColumn;
    private EsxiMonitorViewController monitorViewController;
    
    @Override
    public void initialize(URL url, ResourceBundle rb) 
    {
        editButtonColumn.setCellValueFactory(new PropertyValueFactory("editButton"));
        deleteButtonColumn.setCellValueFactory(new PropertyValueFactory("deleteButton"));
        hostNameColumn.setCellValueFactory(new PropertyValueFactory("hostName")); 
    }
    
    /**
     * Update ESXi host list shown in this Dialog window
     * @param manager
     */
    public void updateServerList(ServerManager manager) 
    {
        this.manager = manager;
        ObservableList<ServerRowEntry> observerList
            = FXCollections.observableArrayList();
        List<Server> serverList = manager.getServerList();
        
        if (serverList.size() > 0) 
        {
            for (Server server : serverList) 
            {
                logger.log(Level.FINER, "Server: {0}", server.getHostname());                
                observerList.add(new ServerRowEntry(
                        server, 
                        this,
                        serverListTable
                ));
            }
        }
        serverListTable.setItems(observerList);
    }
 
    @FXML
    private void handleNewButton (ActionEvent event)
    {
        if (null == manager)
        {
            throw new IllegalStateException("manager is not set");
        }
        
        try
        {
            createEditServerWindow(manager, null, getWindow()); 
        } 
        catch (IOException ex)
        {
            Logger.getLogger(
                    EsxiMonitorViewController.
                            class.getName()).log(Level.SEVERE, null, ex);
        }
        updateServerList(manager);
    }

    public static void createEditServerWindow (
            ServerManager manager,
            Server server,
            Window parent) throws IOException
    {
        FXMLLoader loader =
                new FXMLLoader(ServerListViewController.class.
                        getResource("EditServerView.fxml"));
        loader.load();
        Parent root = loader.getRoot();
        EditServerViewController controller = loader.getController();
        controller.setManager(manager);
        
        Scene scene = new Scene(root);
        Stage editServerWindow = new Stage(StageStyle.UTILITY);
        editServerWindow.setScene(scene);
        if(null != parent)
        {
            // Set parent window            
            editServerWindow.initOwner(parent);                        
            // Enable modal window            
            editServerWindow.initModality(Modality.WINDOW_MODAL);
        }
        editServerWindow.setResizable(false);
        if (null == server)
        {
            controller.setButtonText("Add");
            editServerWindow.setTitle("Add Server");        
        }
        else 
        {
            controller.setServer(server);
            controller.setButtonText("Save");        
            editServerWindow.setTitle("Modify Server");            
        }

        editServerWindow.showAndWait();
    }
    
    @FXML
    private void handleOkButton(ActionEvent event)
    {
        getWindow().hide();
    }
    
    public ServerManager getServerManager()
    {
        return manager;
    }

    private Window getWindow()
    {
        return serverListTable.getScene().getWindow();
    }
}
