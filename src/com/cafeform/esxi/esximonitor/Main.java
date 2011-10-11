package com.cafeform.esxi.esximonitor;

import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualMachine;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * ESXiMonitor <br>
 * Simple Monitor of remote ESXi host. It enables to monitor power status
 * of each virtual macnine running o ESXi host.
 * 
 */
public class Main extends JFrame implements ActionListener, HyperlinkListener  {

    static String version = "v0.1.7";
    public static Logger logger = Logger.getLogger(Main.class.getName());
    private static ServiceInstance serviceInstance = null;
    final private static int iconSize = 15;
    static Icon lightbulb = null;
    static Icon lightbulb_off = null;
    private DefaultComboBoxModel model = new DefaultComboBoxModel();
    private JComboBox serverComboBox = new JComboBox(model);

    private Main() {
    }

    /* Load Icons */
    static {
        try {
            lightbulb = getScaledImageIcon("com/cafeform/esxi/esximonitor/lightbulb.png");
            lightbulb_off = getScaledImageIcon("com/cafeform/esxi/esximonitor/lightbulb_off.png");
        } catch (Exception ex) {
            logger.severe("cannot load icon image");
        }
    }

    public static void main(String args[]) {
        if (args.length != 0) {
            printUsage();
            System.exit(1);
        }
        logger.finer("main called");
        Main instance = new Main();
        instance.setSize(new Dimension(300, 100));
        instance.execute(args);
    }

    public static void printUsage() {
        System.err.println("Usage: java -jar escimonitor <server> <user> <password>");
    }

    public void execute(String[] args) {
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        Server server = Prefs.getDefaServer();
        if (server == null){
            new ServerDialog(this).setVisible(true);
        }
        setHostname(server.getHostname());
        setUsername(server.getUsername());
        setPassword(server.getPassword());

        setTitle("ESXiMonitor");
        addMenuBar();

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.add(getDefaultServerPanel());
        mainPanel.add(mainScrollPane);

        this.getContentPane().add(mainPanel);
        updateVMLIstPanel();
        this.setVisible(true);
    }

    private JComponent getDefaultServerPanel() {
        JPanel defaultServerPanel = new JPanel();
        defaultServerPanel.setLayout(new BoxLayout(defaultServerPanel, BoxLayout.X_AXIS));
        serverComboBox.addActionListener(this);

        List<Server> serverList = Prefs.getServers();
        String defaultServer = Prefs.getRootPreferences().get("defaultServer", "");
        if (serverList.size() > 0) {
            for (Server server : Prefs.getServers()) {
                model.addElement(server.getHostname());
                logger.fine("added " + server.getHostname());
                if (server.getHostname().equals(defaultServer)) {
                    serverComboBox.setSelectedItem(server.getHostname());
                }
            }
        }

        JButton button = new JButton("Update");
        button.addActionListener(this);

        defaultServerPanel.add(serverComboBox);        
        defaultServerPanel.add(button);
        defaultServerPanel.setAlignmentX(LEFT_ALIGNMENT);

        
        return defaultServerPanel;
    }

    private void addMenuBar() {
        MenuBar menuBar = new MenuBar();
        Menu editMenu = new Menu("Edit");
        editMenu.add("Servers");
        editMenu.addActionListener(this);

        Menu helpMenu = new Menu("Help");
        helpMenu.add("About ESXiMonitor");
        helpMenu.addActionListener(this);

        menuBar.add(editMenu);
        menuBar.add(helpMenu);
        this.setMenuBar(menuBar);
    }

    protected void updateVMLIstPanel() {
        ExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        logger.finer("submitting task");
        final Main esximon = this;

        executor.submit(new Runnable() {

            @Override
            public void run() {
                logger.finer("retrieving VMs task is running");
                final JPanel vmListPanel = new JPanel();
                vmListPanel.setBackground(Color.white);
                GroupLayout layout = new GroupLayout(vmListPanel);

                vmListPanel.setLayout(layout);
                layout.setAutoCreateGaps(true);

                /* Create vertical Sequential Group */
                SequentialGroup vGroup = layout.createSequentialGroup();

                /* Create  hirizontal Sequential Group */
                SequentialGroup hGroup = layout.createSequentialGroup();

                /*  Set vertical/horizontal Sqeuential Group */
                layout.setVerticalGroup(vGroup);
                layout.setHorizontalGroup(hGroup);

                /* Craete parallel group for each field */
                ParallelGroup paraGroupForPower = layout.createParallelGroup();
                ParallelGroup paraGroupForName = layout.createParallelGroup();
                ParallelGroup paraGroupForGuestOS = layout.createParallelGroup();
                ParallelGroup paraGroupForButton = layout.createParallelGroup();

                /* Add parallel group for each column (group of same parameter) */
                hGroup.addGroup(paraGroupForPower);
                hGroup.addGroup(paraGroupForButton);
                hGroup.addGroup(paraGroupForName);
                hGroup.addGroup(paraGroupForGuestOS);

                ManagedEntity[] mes = null;

                boolean retried = false; /* retry once if error happen  */                
                while (true) {
                    try {
                        InventoryNavigator in = new InventoryNavigator(getRootFolder());
                        logger.fine("RootFolder: " + getRootFolder().getName());
                        mes = new InventoryNavigator(getRootFolder()).searchManagedEntities("VirtualMachine");
                        if (mes == null || mes.length == 0) {
                            setServiceInstance(null);
                            setRootFolder(null);
                            if (retried) {
                                logger.fine("no vm exist");
                                return;
                            }
                            logger.fine("no vm returned. retrying...");
                            retried = true;
                            continue;
                        }
                        logger.finer("total " + mes.length + " VMs found ");
                    } catch (IOException ex) {
                        logger.fine("retrying to conect to ESXi host...");
                        /* Clear existing service instance and root foler */
                        setServiceInstance(null);
                        setRootFolder(null);
                        if (retried) {
                            ex.printStackTrace();
                            logger.severe("Cannot get VM list");
                            return;
                        }
                        retried = true;
                        continue;
                    }
                    break;
                }

                for (ManagedEntity me : mes) {
                    VirtualMachine vm = (VirtualMachine) me;
                    logger.finer("found VM: " + vm.getName() + " " + vm.getSummary().getRuntime().getPowerState());
                    /* Create parallec group for each vms */
                    ParallelGroup paraGroupForOneVM = layout.createParallelGroup(Alignment.BASELINE);
                    /* 
                     * Create Labels corresponding to VM info fields 
                     */
                    JLabel powerLabel = new JLabel();
                    if (vm.getSummary().getRuntime().getPowerState().equals(VirtualMachinePowerState.poweredOn)) {
                        powerLabel.setToolTipText("Powered ON");
                        powerLabel.setIcon(lightbulb);
                    } else {
                        powerLabel.setToolTipText("Powered OFF");
                        powerLabel.setIcon(lightbulb_off);
                    }
                    JLabel nameLabel = new JLabel(vm.getName());
                    JLabel guestOSLabel = new JLabel(vm.getConfig().getGuestFullName());
                    OperationButtonPanel buttonPanel = new OperationButtonPanel(esximon, vm);

                    /* Add components to group for each column */
                    paraGroupForPower.addComponent(powerLabel);
                    paraGroupForButton.addComponent(buttonPanel);
                    paraGroupForName.addComponent(nameLabel);
                    paraGroupForGuestOS.addComponent(guestOSLabel);

                    /* Add components to group for each row */
                    paraGroupForOneVM.addComponent(powerLabel).addComponent(nameLabel).addComponent(guestOSLabel).addComponent(buttonPanel);

                    /* Add parallel group for each row (VM parameters) */
                    vGroup.addGroup(paraGroupForOneVM);
                }


                logger.finer("creating new vmListePanel completed");
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        mainScrollPane.getViewport().setView(vmListPanel);
                        pack();
                        setVisible(true);
                        logger.fine("vm list updated");
                    }
                });
            }
        });
    }

    /**
     * @param aServiceInstance the serviceInstance to set
     */
    public static void setServiceInstance(ServiceInstance aServiceInstance) {
        serviceInstance = aServiceInstance;
    }

    /**
     * @param aRootFolder the rootFolder to set
     */
    public static void setRootFolder(Folder aRootFolder) {
        rootFolder = aRootFolder;
    }
    private String hostname;
    private String username;
    private String password;
    private static Folder rootFolder = null;

    /**
     * @return the hostname
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * @param aHostname the hostname to set
     */
    public void setHostname(String aHostname) {
        hostname = aHostname;
        for (int i = 0; i < getModel().getSize(); i++) {
            String name = (String) getModel().getElementAt(i);
            if (name.equals(aHostname)) {
                serverComboBox.setSelectedItem(name);
                break;
            }
        }
    }

    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param aUsername the username to set
     */
    public void setUsername(String aUsername) {
        username = aUsername;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param aPassword the password to set
     */
    public void setPassword(String aPassword) {
        password = aPassword;
    }
    private JScrollPane mainScrollPane = new JScrollPane();

    /**
     * @return the serviceInstance
     */
    public ServiceInstance getServiceInstance() throws RemoteException, MalformedURLException {
        if (serviceInstance == null) {
            serviceInstance = new ServiceInstance(new URL("https://" + getHostname() + "/sdk"), getUsername(), getPassword(), true);
        }
        return serviceInstance;
    }

    /**
     * @return the rootFolder
     */
    public Folder getRootFolder() throws RemoteException, MalformedURLException {
        if (rootFolder == null) {
            rootFolder = getServiceInstance().getRootFolder();
        }
        return rootFolder;
    }

    /**
     * Load image file from given path, and return scaled icon image specified by
     * iconSize field.
     * 
     * @param path
     * @return
     * @throws IOException 
     */
    public static ImageIcon getScaledImageIcon(String path) throws IOException {
        /* load icons  */
        BufferedImage originalImage = ImageIO.read(Main.class.getClassLoader().getResource(path));
        Image smallImage = originalImage.getScaledInstance(iconSize, iconSize, java.awt.Image.SCALE_SMOOTH);
        ImageIcon icon = new javax.swing.ImageIcon(smallImage);
        return icon;
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        String cmd = ae.getActionCommand();
        logger.finer("actionPerformed called: " + cmd);
        if ("Servers".equals(cmd)) {
            ServerDialog dialog = new ServerDialog(this);
            dialog.setVisible(true);
        } else if ("About ESXiMonitor".equals(cmd)) {
            StringBuilder msg = new StringBuilder();
            JEditorPane editorPane = new JEditorPane();
            editorPane.setContentType("text/html");
            editorPane.addHyperlinkListener(this);
            editorPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
            editorPane.setBackground(UIManager.getColor("control"));

            msg.append("ESXiMonitor Version " + version + "<br>");
            msg.append("<a href=https://github.com/kaizawa/esximonitor/wiki/esximinitor>"
                    + "https://github.com/kaizawa/esximonitor/wiki/esximinitor</a><br>");
            editorPane.setText(msg.toString());
            JOptionPane.showMessageDialog(this, editorPane);

        } else if ("Update".equals(cmd)) {
            updateVMLIstPanel();
        } else if ("comboBoxChanged".equals(cmd)){
            JComboBox comboBox = (JComboBox) ae.getSource();
            String hostname = (String)comboBox.getSelectedItem();
            logger.fine(hostname + " is selected.");
            setDefaultServer(hostname);
            updateVMLIstPanel();
        }
        repaint();

    }

    @Override
    public void hyperlinkUpdate(HyperlinkEvent he) {
        logger.finer(he.toString());
        if (he.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            try {
                Desktop.getDesktop().browse(new URI(he.getDescription()));
            } catch (IOException ex) {
                logger.severe((ex.getMessage()));
            } catch (URISyntaxException ex) {
                logger.severe((ex.getMessage()));
            }
        }
    }

    /**
     * Specify default ESXi host to be shown in main window.
     * 
     * @param hostname 
     */
    protected void setDefaultServer(String hostname) {
        setHostname(hostname);
        /* Change default server to this new server */
        Prefs.getRootPreferences().put("defaultServer", hostname);

        /* get Server object of default server*/
        Server server = Prefs.getDefaServer();
        if (server == null){
            new ServerDialog(this).setVisible(true);
        }
        setHostname(server.getHostname());
        setUsername(server.getUsername());
        setPassword(server.getPassword());

        /* clear service instance and root folder, so that new service instance will becreated */        
        setServiceInstance(null);
        setRootFolder(null);
    }


    /**
     * @return the model
     */
    public DefaultComboBoxModel getModel() {
        return model;
    }
}