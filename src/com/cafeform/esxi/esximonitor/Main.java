package com.cafeform.esxi.esximonitor;

import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.mo.Folder;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import com.vmware.vim25.mo.VirtualApp;
import com.vmware.vim25.mo.VirtualMachine;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Image;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;

/**
 *
 * @author ka78231
 */
public class Main extends JFrame implements ActionListener {

    public static Logger logger = Logger.getLogger(Main.class.getName());
//    private static ESXiConnection connection;
    private static ServiceInstance serviceInstance = null;
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
    public  ServiceInstance getServiceInstance() throws RemoteException, MalformedURLException {
        serviceInstance = new ServiceInstance(new URL("https://" + getHostname() + "/sdk"), getUsername(), getPassword(), true);
        return serviceInstance;
    }

    /**
     * @return the rootFolder
     */
    public  Folder getRootFolder() throws RemoteException, MalformedURLException {
        rootFolder = getServiceInstance().getRootFolder();

        return rootFolder;
    }

    private Main() {
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
        List<Server> servers = Prefs.getServers();
        Preferences rooPref = Prefs.getRootPreferences();
        String defaultServer = rooPref.get("defaultServer", "");
        if(servers.size() == 0){
            new ServerDialog(this).setVisible(true);
        }
        for(Server server : servers){
            if(server.getHostname().equals(defaultServer)){
                setHostname(server.getHostname());
                setUsername(server.getUsername());
                setPassword(server.getPassword());
            }
        }
        
        setTitle("ESXiMonitor");
        addMenuBar();
        
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.add(getButtonPanel());
        mainPanel.add(mainScrollPane);

        this.getContentPane().add(mainPanel);
        updateVMLIstPanel();
        this.setVisible(true);
    }
    
    private JComponent getButtonPanel(){
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));        
        JLabel serverNameLabel = new JLabel(hostname);
        JButton button = new JButton("Update");
        button.addActionListener(this);

        buttonPanel.add(serverNameLabel);        
        buttonPanel.add(button);
        
        buttonPanel.setAlignmentX(LEFT_ALIGNMENT);         
       
       return buttonPanel;
        
    }

    private void addMenuBar() {
        MenuBar menuBar = new MenuBar();
        Menu editMenu = new Menu("Edit");
        editMenu.add("Servers");
        editMenu.addActionListener(this);
        menuBar.add(editMenu);
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
                vmListPanel.setBorder(new LineBorder(Color.BLACK, 2));
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

                try {
                    logger.finer("RootFolder: " + getRootFolder().getName());
                    InventoryNavigator in = new InventoryNavigator(getRootFolder());
                    //VirtualMachine[] vms = (VirtualMachine[]) in.searchManagedEntities("VirtualMachine");
                    ManagedEntity[] mes = new InventoryNavigator(rootFolder).searchManagedEntities("VirtualMachine");
                    logger.finer("total " + mes.length + " VMs found ");

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
                            powerLabel.setIcon(getSizedImageIcon("com/cafeform/esxi/esximonitor/lightbulb.png"));
                        } else {
                            powerLabel.setToolTipText("Powered OFF");
                            powerLabel.setIcon(getSizedImageIcon("com/cafeform/esxi/esximonitor/lightbulb_off.png"));
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
                } catch (IOException ex) {
                    ex.printStackTrace();
                    logger.severe("Can't get vm list");
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

    public static ImageIcon getSizedImageIcon(String path) throws IOException {
        /* load icons  */
        BufferedImage originalImage = ImageIO.read(Main.class.getClassLoader().getResource(path));
        Image smallImage = originalImage.getScaledInstance(15, 15, java.awt.Image.SCALE_SMOOTH);
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
        } else if("Update".equals(cmd)){
            updateVMLIstPanel();
        }
        repaint();
    }
}
