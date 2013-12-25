package com.cafeform.esxi.esximonitor;

import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.VirtualMachine;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.GroupLayout.Alignment;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

/**
 * ESXiMonitor <br> Simple Monitor of remote ESXi host. It enables to monitor
 * power status of each virtual macnine running o ESXi host.
 * @deprecated No longer used.
 */
@Deprecated
public class Main extends JFrame implements ActionListener, HyperlinkListener {

    static String version = "v0.2.4";
    public static final Logger logger = Logger.getLogger(Main.class.getName());
    final public static int iconSize = 15;
    static Icon lightbulb = null;
    static Icon lightbulb_off = null;
    private ServerManager manager = new ServerManagerImpl();
    private DefaultComboBoxModel model = new DefaultComboBoxModel();
    private JComboBox serverComboBox = new JComboBox(model);
    private ExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private JProgressBar progressBar = null;
    private JLabel statusLabel = new JLabel();
    private JScrollPane mainScrollPane = new JScrollPane();
    private boolean allServerMode = false; // Show VMs on all servers

    private Main() {
    }

    public ServerManager getServerManager() 
    {
        return manager;
    }

    /*
     * Load Icons
     */
    static {
        try {
            lightbulb = getScaledImageIcon("com/cafeform/esxi/esximonitor/lightbulb.png");
            lightbulb_off = getScaledImageIcon("com/cafeform/esxi/esximonitor/lightbulb_off.png");
        } catch (Exception ex) {
            logger.severe("cannot load icon image");
        }
    }

    public static void main(String args[]) 
            throws MalformedURLException, RemoteException 
    {
        if (args.length != 0) {
            printUsage();
            System.exit(1);
        }
        logger.finer("main called");
        Main instance = new Main();
        instance.setMinimumSize(new Dimension(400, 80));
        instance.execute(args);
    }

    private static void printUsage() {
        System.err.println("Usage: java -cp lib/dom4j-1.6.1.jar:lib/vijava520110926.jar:"
                + "dist/esximonitor.jar:lib/ganymed-ssh2-build251beta1.jar"
                + " com.cafeform.esxi.esximonitor.Main");
    }

    private void execute(String[] args) 
            throws MalformedURLException, RemoteException 
    {
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Check if defautl server is set
        if (null == manager.getDefaultServer()) 
        {
            /*
             * no default server set. must be first run.
             */
            logger.finer("server is not set");
            NewServerDialog newDialog = new NewServerDialog(this);
            newDialog.setVisible(true);
            manager.setDefaultServer(newDialog.getServer());
        }

        setTitle("ESXiMonitor");
        addMenuBar();

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.add(createDefaultServerPanel());
        mainPanel.add(mainScrollPane);
        mainPanel.add(createStatusBarPanel());

        this.getContentPane().add(mainPanel);
        this.setVisible(true);
        updateVMLIstPanel();
    }

    private JComponent createDefaultServerPanel() {
        JPanel defaultServerPanel = new JPanel();
        defaultServerPanel.setLayout(new BorderLayout());
        JCheckBox allServerCheckBox = new JCheckBox("Show all Server");
        allServerCheckBox.setActionCommand("allServerChecked");
        JPanel comboBoxPanel = new JPanel();

        getServerComboBox().addActionListener(this);
        allServerCheckBox.addActionListener(this);
        JButton button = new JButton("Update");
        button.addActionListener(this);

        comboBoxPanel.add(getServerComboBox());
        comboBoxPanel.add(allServerCheckBox);
        defaultServerPanel.add(comboBoxPanel, BorderLayout.WEST);
        defaultServerPanel.add(button, BorderLayout.EAST);

        List<Server> serverList = manager.getServerList();
        if (serverList.size() > 0) {
            Server defaultServer;
            if (null == (defaultServer = manager.getDefaultServer()))
            {
                return defaultServerPanel;
            }
            for (Server server : serverList) {
                model.addElement(server.getHostname());
                logger.fine("added " + server.getHostname());
                if (server.getHostname().equals(defaultServer.getHostname())) {
                    getServerComboBox().setSelectedItem(server.getHostname());
                }
            }
        }
        return defaultServerPanel;
    }

    private void addMenuBar() {
        MenuBar menuBar = new MenuBar();
        Menu editMenu = new Menu("Edit");
        editMenu.add("ESXi hosts");
        editMenu.addActionListener(this);

        Menu helpMenu = new Menu("Help");
        helpMenu.add("About ESXiMonitor");
        helpMenu.addActionListener(this);

        menuBar.add(editMenu);
        menuBar.add(helpMenu);
        this.setMenuBar(menuBar);
    }

    /**
     * Update Main window's virtual machine list of defautl server.
     */
    protected void updateVMLIstPanel() {

        logger.finer("submitting task");
        final Main esximon = this;
        final List<Server> serverList = manager.getServerList();

        logger.finer(serverList.size() + " server(s) registerd");

        /*
         * try { server = manager.getDefaultServer(); } catch
         * (NoDefaultServerException ex) { logger.finer("no host is setup. Set
         * empty vmListePanel"); SwingUtilities.invokeLater(new Runnable() {
         *
         * @Override public void run() {
         * mainScrollPane.getViewport().setView(new JPanel()); pack();
         * setVisible(true); logger.fine("vm list updated"); } });
         *
         * getStatusLabel().setText("");
         * getProgressBar().setIndeterminate(false); return; }
         */

        executor.submit(new Runnable() {

            @Override
            public void run() {
                getProgressBar().setIndeterminate(true);
                try {
                    final JPanel vmListPanel = new JPanel();
                    logger.fine("update task is running");

                    vmListPanel.setBackground(Color.white);
                    GroupLayout layout = new GroupLayout(vmListPanel);

                    vmListPanel.setLayout(layout);
                    layout.setAutoCreateGaps(true);

                    /*
                     * Create vertical Sequential Group
                     */
                    SequentialGroup vGroup = layout.createSequentialGroup();

                    /*
                     * Create hirizontal Sequential Group
                     */
                    SequentialGroup hGroup = layout.createSequentialGroup();

                    /*
                     * Set vertical/horizontal Sqeuential Group
                     */
                    layout.setVerticalGroup(vGroup);
                    layout.setHorizontalGroup(hGroup);

                    /*
                     * Craete parallel group for each field
                     */
                    ParallelGroup paraGroupForPower = layout.createParallelGroup();
                    ParallelGroup paraGroupForName = layout.createParallelGroup();
                    ParallelGroup paraGroupForGuestOS = layout.createParallelGroup();
                    ParallelGroup paraGroupForButton = layout.createParallelGroup();
                    ParallelGroup paraGroupForServer = layout.createParallelGroup();

                    /*
                     * Add parallel group for each column (group of same
                     * parameter)
                     */
                    hGroup.addGroup(paraGroupForPower);
                    hGroup.addGroup(paraGroupForButton);
                    hGroup.addGroup(paraGroupForName);
                    hGroup.addGroup(paraGroupForServer);                    
                    hGroup.addGroup(paraGroupForGuestOS);

                    for (Server server : serverList) {
                        if( !allServerMode && server != manager.getDefaultServer()){
                            continue;
                        }
                        
                        getStatusLabel().setText("Updating VM List of " + server.getHostname());
                        ManagedEntity[] managedEntityArray = null;
                        try
                        {
                            managedEntityArray = server.getVirtualMachineArray();
                        } catch (RemoteException ex)
                        {
                            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (MalformedURLException ex)
                        {
                            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        logger.finer(managedEntityArray.length + " VM found on " + server.getHostname());

                        for (ManagedEntity managedEntry : managedEntityArray) {
                            VirtualMachine vm = (VirtualMachine) managedEntry;

                            logger.finer("found VM: " + vm.getName() + " " + vm.getSummary().getRuntime().getPowerState());
                            /*
                             * Create parallec group for each vms
                             */
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
                            nameLabel.setToolTipText("Virtuam Machine Name");
                            JLabel guestOSLabel = new JLabel(vm.getConfig().getGuestFullName());
                            guestOSLabel.setToolTipText("OS Type");
                            OperationButtonPanel buttonPanel = new OperationButtonPanel(esximon, vm, server);
                            JLabel serverLabel = new JLabel(server.getHostname());
                            serverLabel.setToolTipText("ESXi Server Name");

                            /*
                             * Add components to group for each column
                             */
                            paraGroupForPower.addComponent(powerLabel);
                            paraGroupForButton.addComponent(buttonPanel);
                            paraGroupForServer.addComponent(serverLabel);
                            paraGroupForName.addComponent(nameLabel);
                            paraGroupForGuestOS.addComponent(guestOSLabel);

                            /*
                             * Add components to group for each row
                             */
                            paraGroupForOneVM.addComponent(powerLabel).addComponent(nameLabel).addComponent(guestOSLabel).addComponent(buttonPanel).addComponent(serverLabel);

                            /*
                             * Add parallel group for each row (VM parameters)
                             */
                            vGroup.addGroup(paraGroupForOneVM);
                        }
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
                } finally {
                    getStatusLabel().setText("");
                    getProgressBar().setIndeterminate(false);
                }
            }
        });
    }

    /**
     * Load image file from given path, and return scaled icon image specified
     * by iconSize field.
     *
     * @param path
     * @return
     * @throws IOException
     */
    public static ImageIcon getScaledImageIcon(String path) throws IOException {
        /*
         * load icons
         */
        BufferedImage originalImage = ImageIO.read(Main.class.getClassLoader().getResource(path));
        Image smallImage = originalImage.getScaledInstance(iconSize, iconSize, java.awt.Image.SCALE_SMOOTH);
        ImageIcon icon = new javax.swing.ImageIcon(smallImage);
        return icon;
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        String cmd = ae.getActionCommand();
        logger.finer("actionPerformed called: " + cmd);
        if ("ESXi hosts".equals(cmd)) {
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
        } else if ("comboBoxChanged".equals(cmd)) {
            JComboBox comboBox = (JComboBox) ae.getSource();
            String selectedHostname = (String) comboBox.getSelectedItem();
            try
            {
                setDefaultServerByHostname(selectedHostname);
            } catch (MalformedURLException | RemoteException ex)
            {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
            updateVMLIstPanel();
        } else if ("allServerChecked".equals(cmd)) {
            final boolean comboBoxEnabled;

            JCheckBox checkBox = (JCheckBox) ae.getSource();
            if (checkBox.isSelected()) {
                logger.finer("All Server Check box is checked");
                allServerMode = true;
                comboBoxEnabled = false;
            } else {
                logger.finer("All Server Check box is unchecked");
                allServerMode = false;
                comboBoxEnabled = true;
            }
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    getServerComboBox().setEnabled(comboBoxEnabled);
                    pack();
                    setVisible(true);
                    logger.fine("vm list updated");
                }
            });
            updateVMLIstPanel();
        }

        repaint();
    }

    private void setDefaultServerByHostname(String hostname) 
            throws MalformedURLException, RemoteException {
        if (null != hostname) {
            logger.fine(hostname + " is selected.");
        } else {
            logger.fine("no host is selected.");
        }
        manager.setDefaultServerByHostname(hostname);
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
     * @return the model
     */
    public DefaultComboBoxModel getModel() {
        return model;
    }

    private JComponent createStatusBarPanel() {

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(5, 5));



        panel.add(getStatusLabel(), BorderLayout.WEST);
        panel.add(getProgressBar(), BorderLayout.EAST);
        Dimension bar_dem = getProgressBar().getMaximumSize();
        Dimension panel_dem = panel.getMaximumSize();
        panel_dem.setSize(panel_dem.width, bar_dem.height);
        panel.setMaximumSize(panel_dem);

        return panel;
    }

    /**
     * @return the progressBar
     */
    public JProgressBar getProgressBar() {
        if (progressBar == null) {
            progressBar = new JProgressBar();
            Dimension dem = progressBar.getMaximumSize();
            dem.setSize(30, dem.height);
            progressBar.setMaximumSize(dem);
        }
        return progressBar;
    }

    /**
     * @return the serverComboBox
     */
    public JComboBox getServerComboBox() {
        return serverComboBox;
    }

    /**
     * @return the statusLabel
     */
    public JLabel getStatusLabel() {
        return statusLabel;
    }
}