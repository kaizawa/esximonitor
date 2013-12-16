/*
 * The code is copied from :
 * http://javainthebox.net/publication/20130824JJUGJavaFXHoL/text/20130824JavaFXHoL02.html
 */
package com.cafeform.esxi.esximonitor;

import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.VirtualMachine;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javax.swing.Icon;

/**
 *
 * @author kaizawa
 */
public class EsxiMonitorViewController implements Initializable
{

    static String version = "v0.3.0";
    public static final Logger logger = Logger.getLogger(Main.class.getName());
    final public static int iconSize = 15;
    static Icon lightbulb = null;
    static Icon lightbulb_off = null;
    private final ServerManager manager = new ServerManagerImpl();
    private final ObservableList<VirtualMachineEntry> vmEntryList
            = FXCollections.observableArrayList();
    private final ExecutorService executor
            = Executors.newSingleThreadScheduledExecutor();
    private boolean allServerMode = false;

    @FXML
    private TableView<VirtualMachineEntry> table;
    @FXML
    private TableColumn<VirtualMachineEntry, String> statusColumn;
    @FXML
    private TableColumn<VirtualMachineEntry, String> buttonColumn;
    @FXML
    private TableColumn<VirtualMachineEntry, String> serverColumn;
    @FXML
    private TableColumn<VirtualMachineEntry, String> osTypeColumn;
    @FXML
    private TableColumn<VirtualMachineEntry, String> vmNameColumn;
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
        } catch (NoDefaultServerException ex)
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

        updateVmListPanel();
    }

    /**
     * Update Main window's virtual machine list of defautl server.
     */
    protected void updateVmListPanel()
    {
        logger.finer("submitting task");
        final List<Server> serverList = manager.getServerList();
        logger.log(Level.FINER, "{0} server(s) registerd", serverList.size());
        progressBar.setProgress(-1.0f);
        executor.submit(new Task<ObservableList<VirtualMachineEntry>>()
        {
            @Override
            protected ObservableList<VirtualMachineEntry> call() throws Exception
            {
                ObservableList<VirtualMachineEntry> foundVmEntryList
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
                        VirtualMachineEntry vmEntry
                                = new VirtualMachineEntry(vm, server);
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
                ObservableList<VirtualMachineEntry> foundVmEntryList = getValue();
                table.setItems(foundVmEntryList);
                statusLabel.setText("");
                progressBar.setProgress(0f);
            }
        });
    }
}
