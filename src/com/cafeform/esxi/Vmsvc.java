package com.cafeform.esxi;

import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.print.attribute.HashAttributeSet;
import javax.swing.JOptionPane;

/**
 * <p>This class represents vimsvc options of vim-cmd</p>
 * 
 * vimsvc allows you to list virtual machines, and control power state of 
 * each machines.(e.g. power-on/off, reset)
 * 
 */
class Vmsvc {

    private ESXiConnection conn;
    Logger logger = Logger.getLogger(getClass().getName());

    private Vmsvc() {
    }

    public Vmsvc(ESXiConnection conn) {
        this.conn = conn;
    }

    /** 
     * <p>Get List of Virtual Machine info</p>
     * Corresponding to vim-cmd vmsvc/getallvms command.
     * @return List<VM>
     */
    public List<VM> getAllvms() throws IOException {
        List<VM> vms = new ArrayList<VM>();
        Session sess = conn.getSession();

        sess.execCommand("vim-cmd vmsvc/getallvms");

        InputStream stdout = new StreamGobbler(sess.getStdout());

        BufferedReader br = new BufferedReader(new InputStreamReader(stdout));

        while (true) {
            int vmid;
            String line = br.readLine();
            if (line == null) {
                break;
            }

            if (line.startsWith("Vmid")) {
                /* It's header */
                continue;
            }

            /* 
             * We can get some info about vm from this output, but 
             * it's hard to parse each field because there's no separater
             * of the field.
             * So I decided to parse ony vmid here.
             * Detail information would be getStatus().
             */
            StringTokenizer tokenizer = new StringTokenizer(line, " ");
            if (tokenizer.hasMoreTokens()) {
                try {
                    vmid = Integer.parseInt(tokenizer.nextToken());
                } catch (NumberFormatException ex ){
                    /* just ignore this line */
                    continue;
                }
                VM vm = getVM(vmid);
                vms.add(vm);
            }
        }

        /* Close this session */
        sess.close();
        return vms;
    }

    /**
     * Get summary info of guest machine specified by vmid
     * @param vmid virtual macnine ID
     * @return
     * @throws IOException 
     */
    public VM getVM(int vmid) throws IOException {
        Session sess = conn.getSession();
        sess.execCommand("vim-cmd vmsvc/get.summary " + vmid);
        VM vm = new VM(); /* new VM instance */

        vm.setVmid(vmid);
        InputStream stdout = new StreamGobbler(sess.getStdout());
        BufferedReader br = new BufferedReader(new InputStreamReader(stdout));

        while (true) {
            String line = br.readLine();
            if (line == null) {
                break;
            }
            String pair[] = line.split("=");
            if (pair.length < 2) {
                /* Seems not be a line we want to read */
                continue;
            }

            String param = pair[0].replace(",", "").replace("\"", "").trim();
            String val = pair[1].replace(",", "").replace("\"", "").trim();

            logger.finer("param=" + param + ", val=" + val);

            if (param.equals("name")) {
                vm.setName(val);
            } else if (param.equals("vmPathName")) {
                vm.setVmPathName(val);
            } else if (param.equals("guestFullName")) {
                vm.setGuestFullName(val);
            } else if (param.equals("powerState")){
                vm.setPowerState(val);
            }
        }
        sess.close ();
        return vm ;
    }
    
    /**
     * <p>Shutdown guest system</p>
     * @param vmid
     * @return
     * @throws IOException 
     */
    public void powerShutdown(int vmid) throws IOException {
        doPowerCommand("shutdown", vmid);
    }    
    
    /**
     * <p>Reset Power of Virtual Machine</p>
     * @param vmid
     * @return
     * @throws IOException 
     */
    public void powerReset(int vmid) throws IOException {
        doPowerCommand("reset", vmid);
    }
    
    /**
     * <p>Power on Virtual Machine</p>
     * @param vmid
     * @return
     * @throws IOException 
     */
    public void powerOn(int vmid) throws IOException {
        doPowerCommand("on", vmid);
    }  
    
    /**
     * <p>Power off Virtual Machine</p>
     * @param vmid
     * @return
     * @throws IOException 
     */
    public void powerOff(int vmid) throws IOException {
        doPowerCommand("off", vmid);
    }          
        
    private void doPowerCommand(String command, int vmid) throws IOException {        
        Session sess = conn.getSession();

        sess.execCommand("vim-cmd vmsvc/power." + command + " " + vmid);
        InputStream stdout = new StreamGobbler(sess.getStdout());
        BufferedReader br = new BufferedReader(new InputStreamReader(stdout));

        Map<String, String> map = getMapFromBfferedReader(br);

        String msg;
        if((msg = map.get("msg")) != null){
            /* Some messsage recived from server */
            sess.close();
            throw new RecieveMessageException(msg);
        }
        /* Close this session */
        sess.close();
    }     
    
    
    private Map<String, String> getMapFromBfferedReader(BufferedReader br) throws IOException {
        Map map = new ConcurrentHashMap<String, String>();
        
       while (true) {
            String line = br.readLine();
            if (line == null) {
                break;
            }
            
            String pair[] = line.split("=");
            if (pair.length < 2) {
                /* Seems not be a line we want to read */
                continue;
            }
            String param = pair[0].replace(",", "").replace("\"", "").trim();
            String val = pair[1].replace(",", "").replace("\"", "").trim();
            map.put(param, val);
        }        
        return map;
    }
 }