package com.cafeform.esxi.esximonitor;

import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.VirtualMachine;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 *
 * @author kaizawa
 */
public class EsxiMonitorViewController implements Initializable
{
    static String version = "v0.3.0";
    public static final Logger logger = Logger.getLogger(EsxiMonitorViewController.class.getName());
    final public static int iconSize = 15;
    private final ServerManager manager = new ServerManagerImpl();
    private final ExecutorService executor
            = Executors.newSingleThreadScheduledExecutor();
    private boolean allServerMode = false;

    @FXML
    private TableView<VirtualMachineRowEntry> table;
    @FXML
    private TableColumn<VirtualMachineRowEntry, Label> statusColumn;
    @FXML
    private TableColumn<VirtualMachineRowEntry, String> buttonColumn;
    @FXML
    private TableColumn<VirtualMachineRowEntry, String> serverColumn;
    @FXML
    private TableColumn<VirtualMachineRowEntry, String> osTypeColumn;
    @FXML
    private TableColumn<VirtualMachineRowEntry, String> vmNameColumn;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Label statusLabel;

    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        try
        {
            // Check if defautl server is set
            manager.getDefaultServer();
        } 
        catch (NoDefaultServerException ex)
        {
            // no default server set. must be first run.
            //TODO: hoge
            //@todo implement new server
            logger.finer("server is not set");
            //NewServerDialog newDialog = new NewServerDialog(this);
            //newDialog.setVisible(true);
            //manager.setDefaultServer(newDialog.getServer());
        }
        statusColumn.setCellValueFactory(new PropertyValueFactory("status"));
        serverColumn.setCellValueFactory(new PropertyValueFactory("serverName"));
        vmNameColumn.setCellValueFactory(new PropertyValueFactory("vmName")); 
        osTypeColumn.setCellValueFactory(new PropertyValueFactory("osType"));
        buttonColumn.setCellValueFactory(new PropertyValueFactory("buttonBox"));
        updateVmListPanel();
    }

    /**
     * Update Main window's virtual machine list of defautl server.
     */
    public void updateVmListPanel()
    {
        logger.finer("submitting task");
        final List<Server> serverList = manager.getServerList();
        logger.log(Level.FINER, "{0} server(s) registerd", serverList.size());
        progressBar.setProgress(-1.0f);
        final EsxiMonitorViewController controller = this;
        
        // Process retrieving VM entries in separate thread
        executor.submit(new Task<ObservableList<VirtualMachineRowEntry>>()
        {
            @Override
            protected ObservableList<VirtualMachineRowEntry> call() throws Exception
            {
                ObservableList<VirtualMachineRowEntry> foundVmEntryList
                        = FXCollections.observableArrayList();
                logger.fine("update task is running");
                for (Server server : serverList)
                {
                    if (!allServerMode && server
                            != manager.getDefaultServer())
                    {
                        continue;
                    }
                    ManagedEntity[] managedEntityArray
                            = server.getVirtualMachineArray();
                    logger.log(Level.FINER, "{0} VM found on {1}",
                            new Object[]
                            {
                                managedEntityArray.length,
                                server.getHostname()
                            });

                    for (ManagedEntity managedEntry : managedEntityArray)
                    {
                        VirtualMachine vm = (VirtualMachine) managedEntry;
                        VirtualMachineRowEntry vmEntry
                                = new VirtualMachineRowEntry(vm, server, controller);
                        foundVmEntryList.add(vmEntry);
                    }
                }

                logger.finer("creating new vmListePanel completed");
                return foundVmEntryList;
            }

            @Override
            protected void succeeded()
            {
                super.succeeded();
                // Collect vm entries from all servers which were retrieved
                // by separate threads
                ObservableList<VirtualMachineRowEntry> foundVmEntryList = getValue();
                table.setItems(foundVmEntryList);
                statusLabel.setText("");
                progressBar.setProgress(0f);
            }
        });
    }
    
    @FXML
    private void uandleUpdateButton (ActionEvent event) {
        updateVmListPanel();
    }
    
    @FXML
    private void handleExit (ActionEvent event)
    {
        Platform.exit();
    }
    
    /**
     * Handle Edit ESXi Host event
     * @param event 
     */
    @FXML 
    private void handleEditHosts(ActionEvent event)
    {
        try
        {
            FXMLLoader loader = 
                    new FXMLLoader(getClass().
                            getResource("ServerListView.fxml"));
            loader.load();
            Parent root = loader.getRoot();
            ServerListViewController controller = loader.getController();
            controller.updateServerList(manager);
            Scene scene = new Scene(root);
            Stage serverListWindows = new Stage(StageStyle.UTILITY);
            serverListWindows.setScene(scene);
            // Set parent window
            serverListWindows.initOwner(progressBar.getScene().getWindow());
            // Enable modal window
            serverListWindows.initModality(Modality.WINDOW_MODAL);
            serverListWindows.setResizable(false);
            serverListWindows.setTitle("ESXi Server List");
            serverListWindows.showAndWait(); 
        } 
        catch (IOException ex)
        {
            Logger.getLogger(
                    EsxiMonitorViewController.
                            class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @FXML
    private void handleAbout ()
    {
        try
        {
            FXMLLoader loader = 
                    new FXMLLoader(getClass().
                            getResource("AboutView.fxml"));
            loader.load();
            Parent root = loader.getRoot();
            AboutViewController controller = loader.getController();
            controller.setVersion(version);
            Scene scene = new Scene(root);
            Stage serverListWindows = new Stage(StageStyle.UTILITY);
            serverListWindows.setScene(scene);
            // Set parent window
            serverListWindows.initOwner(progressBar.getScene().getWindow());
            // Enable modal window
            serverListWindows.initModality(Modality.WINDOW_MODAL);
            serverListWindows.setResizable(false);
            serverListWindows.setTitle("About ESXiMonitor");
            serverListWindows.showAndWait(); 
        } 
        catch (IOException ex)
        {
            Logger.getLogger(
                    EsxiMonitorViewController.
                            class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public ProgressBar getProgressBar () 
    {
        return progressBar;
    }
    
    public Label getStatusLabel () 
    {
        return statusLabel;
    }
}
