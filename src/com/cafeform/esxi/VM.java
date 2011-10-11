package com.cafeform.esxi;

import java.util.logging.Logger;

/**
 * This class represents Gest Machines stored on ESXi repository.
 *         
 */

/*
dynamicType = <unset>, 
vm = 'vim.VirtualMachine:176', 
dynamicType = <unset>, 
dynamicType = <unset>, 
dynamicType = <unset>, 
vmDirectPathGen2Active = false, 
host = 'vim.HostSystem:ha-host', 
connectionState = "connected", 
powerState = "poweredOff", 
faultToleranceState = "notConfigured", 
toolsInstallerMounted = false, 
suspendTime = <unset>, 
bootTime = <unset>, 
suspendInterval = 0, 
memoryOverhead = 154107904, 
maxCpuUsage = 2390, 
maxMemoryUsage = 1024, 
numMksConnections = 0, 
recordReplayState = "inactive", 
cleanPowerOff = true, 
needSecondaryReason = <unset>, 
onlineStandby = true, 
minRequiredEVCModeKey = <unset>, 
dynamicType = <unset>, 
guestId = <unset>, 
guestFullName = <unset>, 
toolsStatus = "toolsNotRunning", 
toolsVersionStatus = "guestToolsNotInstalled", 
toolsRunningStatus = "guestToolsNotRunning", 
hostName = <unset>, 
ipAddress = <unset>, 
dynamicType = <unset>, 
name = "windows2003", 
template = false, 
vmPathName = "[Disk2-68G] Windows2003.vmx", 
memorySizeMB = 1024, 
cpuReservation = <unset>, 
memoryReservation = <unset>, 
numCpu = 1, 
numEthernetCards = 1, 
numVirtualDisks = 1, 
uuid = "564dbb3d-36b2-8d80-c5d7-858be47ec6f5", 
instanceUuid = "52b5370c-9dd6-1818-d44b-6760b616c445", 
guestId = "winNetEnterpriseGuest", 
guestFullName = "Microsoft Windows Server 2003, Enterprise Edition (32-bit)", 
annotation = "", 
installBootRequired = <unset>, 
dynamicType = <unset>, 
committed = 15032388415, 
uncommitted = 1073741824, 
unshared = 15032388415, 
timestamp = "2011-08-22T13:41:18.607832Z", 
dynamicType = <unset>, 
overallCpuUsage = <unset>, 
overallCpuDemand = <unset>, 
guestMemoryUsage = <unset>, 
hostMemoryUsage = <unset>, 
guestHeartbeatStatus = "gray", 
distributedCpuEntitlement = <unset>, 
distributedMemoryEntitlement = <unset>, 
staticCpuEntitlement = <unset>, 
staticMemoryEntitlement = <unset>, 
privateMemory = <unset>, 
sharedMemory = <unset>, 
swappedMemory = <unset>, 
balloonedMemory = <unset>, 
consumedOverheadMemory = <unset>, 
ftLogBandwidth = <unset>, 
ftSecondaryLatency = <unset>, 
ftLatencyStatus = <unset>, 
compressedMemory = <unset>, 
uptimeSeconds = <unset>, 
overallStatus = "green",                 
 */

public class VM {
    private int vmid;
    private String name;
    private String vmPathName;
    private String guestFullName;
    private String powerState;
    private boolean poweron;
    Logger logger = Logger.getLogger(getClass().getName());

    /**
     * @return the vmid
     */
    public int getVmid() {
        return vmid;
    }

    /**
     * @param vmid the vmid to set
     */
    public void setVmid(int vmid) {
        this.vmid = vmid;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the file
     */
    public String getVmPathName() {
        return vmPathName;
    }

    /**
     * @param file the file to set
     */
    public void setVmPathName(String file) {
        this.vmPathName = file;
    }

    /**
     * @return the guestOS
     */
    public String getGuestFullName() {
        return guestFullName;
    }

    /**
     * @param guestOS the guestOS to set
     */
    public void setGuestFullName(String guestOS) {
        this.guestFullName = guestOS;
    }

    /**
     * @return the powerState
     */
    public String getPowerState() {
        return powerState;
    }

    /**
     * @param powerState the powerState to set
     */
    public void setPowerState(String powerState) {
        this.powerState = powerState;
    }

    /**
     * @return the poweron
     */
    public boolean isPoweron() {
        return "poweredOn".equals(powerState);
    }
}
