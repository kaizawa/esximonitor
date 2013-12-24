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
import javax.swing.Icon;

/**
 * JavaFX Controller class for ServerListView
 */
public class ServerListViewController implements Initializable
{
    private static final Logger logger = Logger.getLogger(ServerDialog.class.getName());
    private VirtualMachine vm;
    private static Icon delete_button = null;
    private static Icon edit_button = null;
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
            FXMLLoader loader = 
                    new FXMLLoader(getClass().getResource("EditServerView.fxml"));
            loader.load();
            Parent root = loader.getRoot();
            EditServerViewController controller = loader.getController();
            controller.setManager(manager);
            controller.setButtonText("Add");

            Scene scene = new Scene(root);
            Stage newServerWindows = new Stage(StageStyle.UTILITY);
            newServerWindows.setScene(scene);
            // Set parent window
            newServerWindows.initOwner(serverListTable.getScene().getWindow());
            // Enable modal window
            newServerWindows.initModality(Modality.WINDOW_MODAL);
            newServerWindows.setResizable(false);
            newServerWindows.setTitle("Add New Server");
            newServerWindows.showAndWait(); 
        } 
        catch (IOException ex)
        {
            Logger.getLogger(
                    EsxiMonitorViewController.
                            class.getName()).log(Level.SEVERE, null, ex);
        }
        updateServerList(manager);
    }
    
    @FXML
    private void handleOkButton(ActionEvent event)
    {
        serverListTable.getScene().getWindow().hide();
    }
    
    public ServerManager getServerManager()
    {
        return manager;
    }
}
