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

/**
 * <p>This class represents vimsvc options of vim-cmd</p>
 * 
 * vimsvc allows you to list virtual machines, and control power state of 
 * each machines.(e.g. power-on/off, reset)
 * 
 */
public class Vmsvc {
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

        List<Integer> vmidList = getVmIds();
        for(Integer vmid : vmidList){
            VM vm = getSummary(vmid);
            vms.add(vm);
        }
        return vms;
    }
    
    public List<Integer> getVmIds() throws IOException {
        Session sess = conn.getSession();
        String cmd = "vim-cmd vmsvc/getallvms";
        List<Integer> vmidList = new ArrayList<Integer>();

        logger.finer("cmd: " + cmd);        
        sess.execCommand(cmd);

        InputStream stdout = new StreamGobbler(sess.getStdout());
        BufferedReader br = new BufferedReader(new InputStreamReader(stdout));

        while (true) {
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
                    vmidList.add(Integer.parseInt(tokenizer.nextToken()));
                    
                } catch (NumberFormatException ex ){
                    /* just ignore this line */
                    continue;
                }
            }
        }

        /* Close this session */
        sess.close();
        return vmidList;
    }    

    /**
     * Get summary info of guest machine specified by vmid
     * @param vmid virtual macnine ID
     * @return
     * @throws IOException 
     */
    public VM getSummary(int vmid) throws IOException {
        Session sess = conn.getSession();
        String cmd = "vim-cmd vmsvc/get.summary " + vmid;

        logger.finer("cmd: " + cmd);
        sess.execCommand(cmd);
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

            logger.finest("param=" + param + ", val=" + val);

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
        String cmd = "vim-cmd vmsvc/power." + command + " " + vmid;

        logger.finer("cmd: " + cmd);
        sess.execCommand(cmd);
        
        /* TODO: should check the task status periodically and wait for completion, 
         * so that user can check the current status timely 
         */

        /* Get STDERR to check error messges */
        InputStream stderr = new StreamGobbler(sess.getStderr());
        BufferedReader br = new BufferedReader(new InputStreamReader(stderr));

        Map<String, String> map = getMapFromBfferedReader(br);

        String msg;
        if((msg = map.get("msg")) != null){
            /* Some error messsage recived from server */
            sess.close();
            throw new RecieveErrorMessageException(msg);
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
            logger.finer(line);
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