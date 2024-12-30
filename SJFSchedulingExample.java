package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class SJFSchedulingExample {

    public static void main(String[] args) {
        try {
            // Initialize CloudSim
            int numUsers = 1; // Number of cloud users
            CloudSim.init(numUsers, null, false);

            // Create Datacenter and Broker
            Datacenter datacenter = createDatacenter("Datacenter_0");
            DatacenterBroker broker = new DatacenterBroker("Broker_0");
            int brokerId = broker.getId();

            // Get VM details from user input
            Scanner scanner = new Scanner(System.in);
            System.out.println("Enter the number of VMs: ");
            int numVms = scanner.nextInt();
            List<Vm> vmlist = new ArrayList<>();

            // Collect VM configurations
            for (int i = 0; i < numVms; i++) {
                System.out.println("Enter details for VM " + (i + 1) + " (Format: mips ram bw size): ");
                int mips = scanner.nextInt();
                int ram = scanner.nextInt();
                int bw = scanner.nextInt();
                long size = scanner.nextLong();
                Vm vm = new Vm(i, brokerId, mips, 1, ram, bw, size, "Xen", new CloudletSchedulerSpaceShared());
                vmlist.add(vm);
            }
            broker.submitVmList(vmlist);

            // Get Cloudlet details from user input
            System.out.println("Enter the number of Cloudlets: ");
            int numCloudlets = scanner.nextInt();
            List<CustomCloudlet> cloudletList = new ArrayList<>();

            UtilizationModel utilizationModel = new UtilizationModelFull();
            for (int i = 0; i < numCloudlets; i++) {
                System.out.println("Enter details for Cloudlet " + (i + 1) + " (Format: arrivalTime length fileSize outputSize): ");
                long arrivalTime = scanner.nextLong();
                long length = scanner.nextLong();
                long fileSize = scanner.nextLong();
                long outputSize = scanner.nextLong();

                // Create CustomCloudlet instance
                CustomCloudlet cloudlet = new CustomCloudlet(i, length, 1, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
                cloudlet.setArrivalTime(arrivalTime); // Set the arrival time
                cloudlet.setUserId(brokerId);
                cloudletList.add(cloudlet);
            }

            // Sort cloudlets by length (Shortest Job First)
            cloudletList.sort(Comparator.comparingLong(CustomCloudlet::getCloudletLength));
            broker.submitCloudletList(cloudletList);

            // Start CloudSim simulation
            CloudSim.startSimulation();
            List<Cloudlet> finishedCloudlets = broker.getCloudletReceivedList();
            CloudSim.stopSimulation();

            // Calculate performance metrics
            double startTime = Double.MAX_VALUE;
            double finishTime = 0;
            int totalCloudlets = finishedCloudlets.size();
            long totalLength = 0;
            double totalWaitTime = 0;
            double totalResponseTime = 0;
            double totalTurnaroundTime = 0;

            for (Cloudlet cloudlet : finishedCloudlets) {
                CustomCloudlet customCloudlet = (CustomCloudlet) cloudlet;
                long arrivalTime = customCloudlet.getArrivalTime();

                double execStartTime = cloudlet.getExecStartTime();
                double finishTimeForCloudlet = cloudlet.getFinishTime();

                if (execStartTime <= 0) {
                    execStartTime = arrivalTime;
                }
                if (finishTimeForCloudlet <= 0) {
                    finishTimeForCloudlet = execStartTime + cloudlet.getCloudletLength() / 1000.0;
                }

                double waitTime = Math.max(execStartTime - arrivalTime, 0);
                double responseTime = Math.max(finishTimeForCloudlet - arrivalTime, 0);
                double turnaroundTime = Math.max(finishTimeForCloudlet - arrivalTime, 0);

                totalWaitTime += waitTime;
                totalResponseTime += responseTime;
                totalTurnaroundTime += turnaroundTime;

                startTime = Math.min(startTime, execStartTime);
                finishTime = Math.max(finishTime, finishTimeForCloudlet);

                totalLength += cloudlet.getCloudletLength();
            }

            double makeSpan = finishTime - startTime;
            if (makeSpan <= 0) makeSpan = 1.0;
            double avgWaitTime = totalWaitTime / totalCloudlets;
            double avgResponseTime = totalResponseTime / totalCloudlets;
            double avgTurnaroundTime = totalTurnaroundTime / totalCloudlets;
            double throughput = totalCloudlets / makeSpan;

            // Output performance metrics to console
            System.out.println("Cloudlet ID | VM ID | Arrival Time | Status  | Start Time | Finish Time | Length  | File Size | Output Size");
            for (Cloudlet cloudlet : finishedCloudlets) {
                CustomCloudlet customCloudlet = (CustomCloudlet) cloudlet;
                long arrivalTime = customCloudlet.getArrivalTime();
                System.out.printf("%-12d | %-5d | %-12d | %-7s | %-10.2f | %-11.2f | %-7d | %-9d | %-11d\n",
                        cloudlet.getCloudletId(),
                        cloudlet.getVmId(),
                        arrivalTime,
                        cloudlet.getCloudletStatusString(),
                        cloudlet.getExecStartTime(),
                        cloudlet.getFinishTime(),
                        cloudlet.getCloudletLength(),
                        cloudlet.getCloudletFileSize(),
                        cloudlet.getCloudletOutputSize());
            }

            System.out.println("\nPerformance Metrics:");
            System.out.println("MakeSpan: " + makeSpan);
            System.out.println("Total Cloudlet Length: " + totalLength);
            System.out.println("Average Wait Time: " + avgWaitTime);
            System.out.println("Average Response Time: " + avgResponseTime);
            System.out.println("Average Turnaround Time: " + avgTurnaroundTime);
            System.out.println("Throughput: " + throughput);

            // Save results to CSV files
            FileWriter csvWriter = new FileWriter(new File("sjf_results.csv"));
            csvWriter.append("Cloudlet ID,VM ID,Arrival Time,Status,Start Time,Finish Time,Length,File Size,Output Size,Response Time\n");
            for (Cloudlet cloudlet : finishedCloudlets) {
                CustomCloudlet customCloudlet = (CustomCloudlet) cloudlet;
                csvWriter.append(String.format("%d, %d, %d, %s, %.2f, %.2f, %d, %d, %d, %.2f\n",
                        cloudlet.getCloudletId(),
                        cloudlet.getVmId(),
                        customCloudlet.getArrivalTime(),
                        cloudlet.getCloudletStatusString(),
                        cloudlet.getExecStartTime(),
                        cloudlet.getFinishTime(),
                        cloudlet.getCloudletLength(),
                        cloudlet.getCloudletFileSize(),
                        cloudlet.getCloudletOutputSize(),
                        Math.max(cloudlet.getExecStartTime() - customCloudlet.getArrivalTime(), 0)
                ));
            }
            csvWriter.flush();
            csvWriter.close();

            // Append algorithm-level metrics to CSV
            FileWriter algorithmCsvWriter = new FileWriter(new File("algorithm_results.csv"), true);
            File file = new File("algorithm_results.csv");
            if (file.length() == 0) {
                algorithmCsvWriter.append("Algorithm Name,MakeSpan,Throughput,Average Waiting Time,Average Response Time,Average Turnaround Time,Number of Cloudlets\n");
            }
            algorithmCsvWriter.append(String.format("SJF, %.2f, %.2f, %.2f, %.2f, %.2f, %d\n",
                    makeSpan,
                    throughput,
                    avgWaitTime,
                    avgResponseTime,
                    avgTurnaroundTime,
                    numCloudlets));
            algorithmCsvWriter.flush();
            algorithmCsvWriter.close();
            System.out.println("Results saved to algorithm_results.csv");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Datacenter createDatacenter(String name) throws Exception {
        List<Host> hostList = new ArrayList<>();
        int mips = 5000;
        int ram = 16384; // 16 GB
        long storage = 1_000_000; // 1 TB
        int bw = 10_000; // 10 GBps

        List<Pe> peList = new ArrayList<>();
        peList.add(new Pe(0, new PeProvisionerSimple(mips)));

        Host host = new Host(0, new RamProvisionerSimple(ram), new BwProvisionerSimple(bw), storage, peList, new VmSchedulerTimeShared(peList));
        hostList.add(host);

        String arch = "x86";
        String os = "Linux";
        String vmm = "Xen";
        double timeZone = 5.5;
        double cost = 3.0;
        double costPerMem = 0.05;
        double costPerStorage = 0.001;
        double costPerBw = 0.0;

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(arch, os, vmm, hostList, timeZone, cost, costPerMem, costPerStorage, costPerBw);
        return new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), new LinkedList<>(), 0);
    }
}
