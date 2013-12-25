package com.cafeform.esxi.esximonitor;

import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.VirtualMachine;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/**
 * JavaFX controller for Main windows
 */
public class EsxiMonitorViewController implements Initializable
{

    static String version = "v0.3.0";
    public static final Logger logger = Logger.getLogger(EsxiMonitorViewController.class.getName());
    final public static int iconSize = 15;
    private final ServerManager manager = new ServerManagerImpl();
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
    @FXML
    private ChoiceBox<Server> serverChoiceBox;
    @FXML
    private CheckBox showAllCheckBox;

    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        // Check if defautl server is set
        if (null == manager.getDefaultServer())
        {
            // No default server set. must be first run.
            logger.finer("server is not set");
            try
            {
                ServerListViewController.
                        createEditServerWindow(manager, null, null);
            } 
            catch (IOException exi)
            {
                DialogFactory.showSimpleDialogAndLog(
                        "Cannot create Server edit window.",
                        "Error", getWindow(), logger, Level.SEVERE, exi);
            }
        }
        // Associate columns with corresponding property of 
        // VirtualMachineRowEntry class.
        statusColumn.setCellValueFactory(new PropertyValueFactory("status"));
        serverColumn.setCellValueFactory(new PropertyValueFactory("serverName"));
        vmNameColumn.setCellValueFactory(new PropertyValueFactory("vmName"));
        osTypeColumn.setCellValueFactory(new PropertyValueFactory("osType"));
        buttonColumn.setCellValueFactory(new PropertyValueFactory("buttonBox"));

        // Set server list and set default server to Choice Box
        serverChoiceBox.setItems(manager.getServerList());
        serverChoiceBox.setValue(manager.getDefaultServer());

        // Set listener for Choice Box
        serverChoiceBox.getSelectionModel().selectedItemProperty().addListener(
                new ChangeListener<Server>()
                {
                    @Override
                    public void changed(
                            ObservableValue<? extends Server> ov,
                            Server oldServer,
                            Server newServer)
                    {
                        if (null != newServer)
                        {
                            try
                            {
                                manager.setDefaultServer(newServer);
                            } 
                            catch (MalformedURLException | RemoteException ex)
                            {
                                DialogFactory.showSimpleDialogAndLog(
                                        "Cannot set default server", "Error", 
                                        getWindow(), logger,Level.SEVERE, ex);
                            } 
                            try 
                            {
                                updateVmListPanel();                                
                            }
                            catch (InterruptedException ex)
                            {
                                DialogFactory.showSimpleDialogAndLog(
                                        "Cannot udpate server list","Error", 
                                        getWindow(), logger, Level.SEVERE, ex);
                            }
                        }
                    }
                });

        // Set listener for All Server Check Box
        showAllCheckBox.selectedProperty().addListener(new ChangeListener<Boolean>()
        {
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean oldVal, Boolean newVal)
            {
                allServerMode = newVal;
                serverChoiceBox.setDisable(newVal);
                try
                {
                    updateVmListPanel();
                } catch (InterruptedException ex)
                {
                    DialogFactory.showSimpleDialogAndLog(
                            "Cannot udpate server list", "Error", getWindow(),
                            logger, Level.SEVERE, ex);
                }
            }
        });

        // Update VM list 
        try
        {
            updateVmListPanel();
        } catch (InterruptedException ex)
        {
            DialogFactory.showSimpleDialogAndLog(
                    "Cannot udpate server list", "Error", getWindow(), 
                    logger, Level.SEVERE, ex);
        }
    }

    /**
     * Update Main window's virtual machine list
     * @throws java.lang.InterruptedException
     */
    public void updateVmListPanel() throws InterruptedException
    {
        logger.finer("submitting task");
        final List<Server> serverList = manager.getServerList();
        logger.log(Level.FINER, "{0} server(s) registerd", serverList.size());
        statusLabel.setText("Updating virtual machine list");
        progressBar.setProgress(-1.0f);
        final EsxiMonitorViewController controller = this;
        final ExecutorService executor
                = Executors.newSingleThreadScheduledExecutor();

        // Process retrieving VM entries in separate thread
        executor.submit(new Task<ObservableList<VirtualMachineRowEntry>>()
        {
            @Override
            protected ObservableList<VirtualMachineRowEntry> call() throws Exception
            {
                ObservableList<VirtualMachineRowEntry> foundVmEntryList
                        = FXCollections.observableArrayList();
                logger.fine("update task is running");
                for (final Server server : serverList)
                {
                    Platform.runLater(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            statusLabel.setText("Accessing " 
                                    + server.getHostname());                            
                        }
                    });

                    // If "All Server" mode, show all VMs of all server
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
        executor.shutdown();
    }

    @FXML
    private void uandleUpdateButton(ActionEvent event)
    {
        try
        {
            updateVmListPanel();
        } 
        catch (InterruptedException ex)
        {
            DialogFactory.showSimpleDialogAndLog(
                    "Cannot udpate server list",
                    "Error", getWindow(),
                    logger, Level.SEVERE,
                    ex);
        }
    }

    @FXML
    private void handleExit(ActionEvent event)
    {
        Platform.exit();
    }

    /**
     * Handle Edit ESXi Host event. Create ESXi Server List window
     *
     * @param event
     */
    @FXML
    private void handleEditServers(ActionEvent event)
    {
        try
        {
            FXMLLoader loader
                    = new FXMLLoader(getClass().
                            getResource("ServerListView.fxml"));
            loader.load();
            Parent root = loader.getRoot();
            ServerListViewController controller = loader.getController();
            controller.updateServerList(manager);
            Scene scene = new Scene(root);
            Stage serverListWindows = new Stage(StageStyle.UTILITY);
            serverListWindows.setScene(scene);
            // Set parent window
            serverListWindows.initOwner(getWindow());
            // Enable modal window
            serverListWindows.initModality(Modality.WINDOW_MODAL);
            serverListWindows.setResizable(false);
            serverListWindows.setTitle("ESXi Server List");
            serverListWindows.showAndWait();
        } 
        catch (IOException ex)
        {
            DialogFactory.showSimpleDialogAndLog(
                    "Cannot create Server list window", "Error", getWindow(), 
                    logger, Level.SEVERE, ex);
        }
    }

    /**
     * Create Help->About window
     */
    @FXML
    private void handleAbout()
    {
        try
        {
            FXMLLoader loader
                    = new FXMLLoader(getClass().
                            getResource("AboutView.fxml"));
            loader.load();
            Parent root = loader.getRoot();
            AboutViewController controller = loader.getController();
            controller.setVersion(version);
            Scene scene = new Scene(root);
            Stage serverListWindows = new Stage(StageStyle.UTILITY);
            serverListWindows.setScene(scene);
            // Set parent window
            serverListWindows.initOwner(getWindow());
            // Enable modal window
            serverListWindows.initModality(Modality.WINDOW_MODAL);
            serverListWindows.setResizable(false);
            serverListWindows.setTitle("About ESXiMonitor");
            serverListWindows.showAndWait();
        } 
        catch (IOException ex)
        {
            DialogFactory.showSimpleDialogAndLog(
                    "Cannot create about window", "Error", getWindow(), 
                    logger, Level.SEVERE, ex);
        }
    }

    public ProgressBar getProgressBar()
    {
        return progressBar;
    }

    public Label getStatusLabel()
    {
        return statusLabel;
    }

    private Window getWindow()
    {
        return progressBar.getScene().getWindow();
    }
}
