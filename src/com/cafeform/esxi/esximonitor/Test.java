package com.cafeform.esxi.esximonitor;

import java.net.URL;
import com.vmware.vim25.*;
import com.vmware.vim25.mo.*;

/**
 * Test Program
 */
public class Test {

    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();
        ServiceInstance si = new ServiceInstance(new URL("https://192.168.1.20/sdk"), "root", "password", true);
        long end = System.currentTimeMillis();
        System.out.println("time taken:" + (end - start));
        Folder rootFolder = si.getRootFolder();
        String name = rootFolder.getName();
        System.out.println("root:" + name);
        ManagedEntity[] mes = new InventoryNavigator(rootFolder).searchManagedEntities("VirtualMachine");

        if (mes == null || mes.length == 0) {
            return;
        }

        int numVMs = mes.length;

        while (true) {
            for (int i = 0; i < numVMs; i++) {
                VirtualMachine vm = (VirtualMachine) mes[i];

                VirtualMachineConfigInfo vminfo = vm.getConfig();
                VirtualMachineCapability vmc = vm.getCapability();


                vm.getResourcePool();
                System.out.println("---------------------------------");
                System.out.println("Name: " + vm.getName());
                System.out.println("GuestOS: " + vminfo.getGuestFullName());
                System.out.println("Power: " + vm.getSummary().getRuntime().getPowerState());
//                System.out.println("Multiple snapshot supported: " + vmc.isMultipleSnapshotsSupported());
            }

            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
            }

        }

        //si.getServerConnection().logout();
    }
}
