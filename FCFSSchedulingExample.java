package org.cloudbus.cloudsim.examples;
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class FCFSSchedulingExample {

    public static void main(String[] args) {
        try {
            // Step 1: Initialize CloudSim
            int numUsers = 1; // Number of cloud users
            CloudSim.init(numUsers, null, false);

            // Step 2: Create Datacenter
            Datacenter datacenter = createDatacenter("Datacenter_0");

            // Step 3: Create Broker
            DatacenterBroker broker = new DatacenterBroker("Broker_0");
            int brokerId = broker.getId();

            // Step 4: Interactive Input for VMs
            Scanner scanner = new Scanner(System.in);
            System.out.println("Enter the number of VMs: ");
            int numVms = scanner.nextInt();
            List<Vm> vmlist = new ArrayList<>();

            for (int i = 0; i < numVms; i++) {
                System.out.println("Enter details for VM " + (i + 1) + " (Format: mips ram bw size): ");
                int mips = scanner.nextInt();
                int ram = scanner.nextInt();
                int bw = scanner.nextInt();
                long size = scanner.nextLong();
                // Use SpaceShared scheduler for FCFS behavior
                Vm vm = new Vm(i, brokerId, mips, 1, ram, bw, size, "Xen", new CloudletSchedulerSpaceShared());
                vmlist.add(vm);
            }
            broker.submitVmList(vmlist);

            // Step 5: Interactive Input for Cloudlets
            System.out.println("Enter the number of Cloudlets: ");
            int numCloudlets = scanner.nextInt();
            List<Cloudlet> cloudletList = new ArrayList<>();

            UtilizationModel utilizationModel = new UtilizationModelFull();
            for (int i = 0; i < numCloudlets; i++) {
                System.out.println("Enter details for Cloudlet " + (i + 1) + " (Format: length fileSize outputSize): ");
                long length = scanner.nextLong();
                long fileSize = scanner.nextLong();
                long outputSize = scanner.nextLong();

                Cloudlet cloudlet = new Cloudlet(i, length, 1, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
                cloudlet.setUserId(brokerId);
                cloudletList.add(cloudlet);
            }
            broker.submitCloudletList(cloudletList);

            // Step 6: Start Simulation
            CloudSim.startSimulation();

            // Retrieve results
            List<Cloudlet> finishedCloudlets = broker.getCloudletReceivedList();
            CloudSim.stopSimulation();

            // Print results
            System.out.println("Cloudlet ID | Status  | Start Time | Finish Time");
            for (Cloudlet cloudlet : finishedCloudlets) {
                System.out.printf("%-12d | %-7s | %-10.2f | %-11.2f\n",
                        cloudlet.getCloudletId(),
                        cloudlet.getCloudletStatusString(),
                        cloudlet.getExecStartTime(),
                        cloudlet.getFinishTime());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Datacenter createDatacenter(String name) throws Exception {
        List<Host> hostList = new ArrayList<>();

        int mips = 1000;
        int ram = 16384; // 16 GB
        long storage = 1_000_000; // 1 TB
        int bw = 10_000; // 10 GBps

        List<Pe> peList = new ArrayList<>();
        peList.add(new Pe(0, new PeProvisionerSimple(mips)));

        Host host = new Host(0, new RamProvisionerSimple(ram), new BwProvisionerSimple(bw), storage, peList, new VmSchedulerTimeShared(peList));
        hostList.add(host);
//characteristics of the data center
        String arch = "x86"; //architecture of physical hardware
        String os = "Linux";
        String vmm = "Xen"; //VMM(standard virtualization tec in cloud env, simple)
        double timeZone = 10.0;
        double cost = 3.0;
        double costPerMem = 0.05;
        double costPerStorage = 0.001;
        double costPerBw = 0.0;

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(arch, os, vmm, hostList, timeZone, cost, costPerMem, costPerStorage, costPerBw);
        return new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), new ArrayList<>(), 0);
    }
}
