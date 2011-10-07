/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cafeform.esxi.esximonitor;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;
import com.cafeform.esxi.ESXiConnection;
import com.cafeform.esxi.ESXiConnectionFactory;
import com.cafeform.esxi.VM;
import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;

/**
 *
 * @author ka78231
 */
public class ESXiMonitor extends JFrame {

    public static Logger logger = Logger.getLogger(ESXiMonitor.class.getName());
    private static ESXiConnection connection;
    private static String hostname = "stslab04";
    private static String username = "root";
    private static String password = "handl3bar";
    private JScrollPane mainScrollPane = new JScrollPane();
    private static ESXiMonitor instance = new ESXiMonitor();

    private ESXiMonitor() {
    }

    public static void main(String args[]) {
        if (args.length != 0) {
            printUsage();
            System.exit(1);
        }
        getInstance().execute(args);
    }

    public static ESXiMonitor getInstance() {
        return instance;
    }

    public static void printUsage() {
        System.err.println("Usage: java -jar escimonitor <server> <user> <password>");
    }

    public void execute(String[] args) {
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        updateVMLIstPanel();
        this.getContentPane().add(mainScrollPane);
    }

    protected void updateVMLIstPanel() {

        ExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        executor.submit(new Runnable() {
            @Override
            public void run() {
                final JPanel vmListPanel = new JPanel();
                vmListPanel.setBorder(new LineBorder(Color.BLACK , 2));
                GroupLayout layout = new GroupLayout(vmListPanel);
                OperationButtonPanel dummyPanel = null;

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
                    for (VM vm : getConnection().getVmsvc().getAllvms()) {
                        /* Create parallec group for each vms */
                        ParallelGroup paraGroupForOneVM = layout.createParallelGroup(Alignment.BASELINE);
//                paraGroupForOneVM.addGap(0, 5, Short.MAX_VALUE);

                        /* 
                         * Create Labels corresponding to VM info fields 
                         */
                        JLabel powerLabel = new JLabel();
                        if (vm.isPoweron()) {
                            powerLabel.setToolTipText("Powered ON");
                            powerLabel.setIcon(getSizedImageIcon("com/cafeform/esxi/esximonitor/lightbulb.png"));
                        } else {
                            powerLabel.setToolTipText("Powered OFF");
                            powerLabel.setIcon(getSizedImageIcon("com/cafeform/esxi/esximonitor/lightbulb_off.png"));
                        }
                        JLabel nameLabel = new JLabel(vm.getName());
                        JLabel guestOSLabel = new JLabel(vm.getGuestFullName());
                        OperationButtonPanel buttonPanel = new OperationButtonPanel(vm);

                        /* Add components to group for each column */
                        paraGroupForPower.addComponent(powerLabel);
                        paraGroupForButton.addComponent(buttonPanel);                        
                        paraGroupForName.addComponent(nameLabel);
                        paraGroupForGuestOS.addComponent(guestOSLabel);

                        /* Add components to group for each row */
                        paraGroupForOneVM.addComponent(powerLabel).addComponent(nameLabel).addComponent(guestOSLabel).addComponent(buttonPanel);

                        /* Add parallel group for each row (VM parameters) */
                        vGroup.addGroup(paraGroupForOneVM);
                        dummyPanel = buttonPanel;
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                    logger.severe("Can't get vm list");
                }

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

    public void doTest(String[] args) {
        try {
            /* Create a connection instance */
            Connection conn = new Connection(hostname);
            /* Now connect */
            conn.connect();
            /* Authenticate.
             * If you get an IOException saying something like
             * "Authentication method password not supported by the server at this stage."
             * then please check the FAQ.
             */
            boolean isAuthenticated = conn.authenticateWithPassword(username, password);
            if (isAuthenticated == false) {
                throw new IOException("Authentication failed.");
            }
            /* Create a session */
            Session sess = conn.openSession();
            sess.execCommand("vim-cmd vmsvc/getallvms");
            /* This basic example does not handle stderr, which is sometimes dangerous
             * (please read the FAQ).
             */
            InputStream stdout = new StreamGobbler(sess.getStdout());
            BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
            while (true) {
                String line = br.readLine();
                if (line == null) {
                    break;
                }
                System.out.println(line);
            }
            /* Show exit status, if available (otherwise "null") */
            System.out.println("ExitCode: " + sess.getExitStatus());
            /* Close this session */
            sess.close();
            /* Close the connection */
            conn.close();
        } catch (IOException e) {
            e.printStackTrace(System.err);
            System.exit(2);
        }
    }

    public static ImageIcon getSizedImageIcon(String path) throws IOException {
        /* load icons  */
        BufferedImage originalImage = ImageIO.read(ESXiMonitor.class.getClassLoader().getResource(path));
        Image smallImage = originalImage.getScaledInstance(15, 15, java.awt.Image.SCALE_SMOOTH);
        ImageIcon icon = new javax.swing.ImageIcon(smallImage);
        return icon;
    }

    /**
     * @return the connection
     */
    protected static ESXiConnection getConnection() {

        if (connection == null) {
            setConnection(ESXiConnectionFactory.createInstance(hostname, username, password));
        }
        return connection;
    }

    /**
     * @param connection the connection to set
     */
    protected static void setConnection(ESXiConnection conn) {
        connection = conn;
    }
}
