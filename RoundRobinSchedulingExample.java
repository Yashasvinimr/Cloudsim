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

public class RoundRobinSchedulingExample {

    public static void main(String[] args) {
        try {
            int numUsers = 1; // Number of cloud users
            CloudSim.init(numUsers, null, false);

            Datacenter datacenter = createDatacenter("Datacenter_0");

            DatacenterBroker broker = new DatacenterBroker("Broker_0");
            int brokerId = broker.getId();

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
                Vm vm = new Vm(i, brokerId, mips, 1, ram, bw, size, "Xen", new CloudletSchedulerSpaceShared());
                vmlist.add(vm);
            }
            broker.submitVmList(vmlist);

            System.out.println("Enter the number of Cloudlets: ");
            int numCloudlets = scanner.nextInt();
            List<CustomCloudlet> cloudletList = new ArrayList<>();

            UtilizationModel utilizationModel = new UtilizationModelFull();
            for (int i = 0; i < numCloudlets; i++) {
                System.out.println("Enter details for Cloudlet " + (i + 1) + " (Format: arrivalTime length fileSize outputSize): ");
                long arrivalTime = scanner.nextLong(); // Adding arrival time as input
                long length = scanner.nextLong();
                long fileSize = scanner.nextLong();
                long outputSize = scanner.nextLong();

                CustomCloudlet cloudlet = new CustomCloudlet(i, length, 1, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
                cloudlet.setUserId(brokerId);
                cloudlet.setArrivalTime(arrivalTime); // Set the arrival time for the cloudlet
                cloudletList.add(cloudlet);
            }
            broker.submitCloudletList(cloudletList);

            // Quantum time for Round Robin
            System.out.println("Enter quantum time (in ms): ");
            double quantum = scanner.nextDouble();

            // Perform Round Robin scheduling with quantum
            distributeCloudletsRoundRobin(broker, cloudletList, vmlist, quantum);

            CloudSim.startSimulation();

            List<Cloudlet> finishedCloudlets = broker.getCloudletReceivedList();
            CloudSim.stopSimulation();

            // Calculate the performance metrics
            double startTime = Double.MAX_VALUE;
            double finishTime = 0;
            int totalCloudlets = finishedCloudlets.size();
            long totalLength = 0;
            double totalWaitTime = 0; // Total wait time for the cloudlets
            double totalResponseTime = 0; // Total response time for the cloudlets
            double totalTurnaroundTime = 0; // Total turnaround time

            for (Cloudlet cloudlet : finishedCloudlets) {
                startTime = Math.min(startTime, cloudlet.getExecStartTime());
                finishTime = Math.max(finishTime, cloudlet.getFinishTime());
                totalLength += cloudlet.getCloudletLength();

                // Adjust for negative wait time
                double waitTime = cloudlet.getExecStartTime() - ((CustomCloudlet) cloudlet).getArrivalTime();
                totalWaitTime += (waitTime > 0) ? waitTime : 0; // If negative, ignore it (consider 0)

                // Adjust for negative response time
                double responseTime = cloudlet.getFinishTime() - ((CustomCloudlet) cloudlet).getArrivalTime();
                totalResponseTime += (responseTime > 0) ? responseTime : 0; // If negative, ignore it (consider 0)

                // Adjust for negative turnaround time
                double turnaroundTime = cloudlet.getFinishTime() - ((CustomCloudlet) cloudlet).getArrivalTime();
                totalTurnaroundTime += (turnaroundTime > 0) ? turnaroundTime : 0; // If negative, ignore it (consider 0)
            }

            double makeSpan = finishTime - startTime;
            double throughput = (double) totalCloudlets / makeSpan; // Throughput calculation
            double avgWaitTime = totalWaitTime / totalCloudlets;
            double avgResponseTime = totalResponseTime / totalCloudlets;
            double avgTurnaroundTime = totalTurnaroundTime / totalCloudlets;

            // Output the performance metrics to console
            System.out.println("Cloudlet ID | VM ID | Status  | Arrival Time | Start Time | Finish Time | Length  | File Size | Output Size");
            for (Cloudlet cloudlet : finishedCloudlets) {
                System.out.printf(
                        "%-12d | %-5d | %-7s | %-12d | %-10.2f | %-11.2f | %-7d | %-9d | %-11d\n",
                        cloudlet.getCloudletId(),
                        cloudlet.getVmId(),
                        cloudlet.getCloudletStatusString(),
                        ((CustomCloudlet) cloudlet).getArrivalTime(),
                        cloudlet.getExecStartTime(),
                        cloudlet.getFinishTime(),
                        cloudlet.getCloudletLength(),
                        cloudlet.getCloudletFileSize(),
                        cloudlet.getCloudletOutputSize()
                );
            }

            System.out.println("\nPerformance Metrics:");
            System.out.println("MakeSpan: " + makeSpan);
            System.out.println("Throughput: " + throughput); // Print throughput
            System.out.println("Total Cloudlet Length: " + totalLength);
            System.out.println("Average Wait Time: " + avgWaitTime);
            System.out.println("Average Response Time: " + avgResponseTime);
            System.out.println("Average Turnaround Time: " + avgTurnaroundTime);

            // Save results to CSV
            String rrFilePath = "rr_results.csv";
            saveResultsToCSV(rrFilePath, finishedCloudlets, makeSpan, throughput, totalLength, avgWaitTime, avgResponseTime, avgTurnaroundTime);

            String algorithmFilePath = "algorithm_results.csv";
            saveAlgorithmResultsToCSV(algorithmFilePath, makeSpan, throughput, totalLength, avgWaitTime, avgResponseTime, avgTurnaroundTime, totalCloudlets);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void distributeCloudletsRoundRobin(DatacenterBroker broker, List<CustomCloudlet> cloudletList, List<Vm> vmlist, double quantum) {
        cloudletList.sort(Comparator.comparingLong(CustomCloudlet::getArrivalTime));

        int vmCount = vmlist.size();
        Queue<CustomCloudlet> queue = new LinkedList<>(cloudletList);
        long currentTime = 0;

        while (!queue.isEmpty()) {
            CustomCloudlet cloudlet = queue.poll();
            Vm selectedVm = vmlist.get(cloudlet.getCloudletId() % vmCount);
            cloudlet.setVmId(selectedVm.getId());

            // Wait for cloudlet's arrival time if needed
            if (cloudlet.getArrivalTime() > currentTime) {
                currentTime = cloudlet.getArrivalTime();
            }

            // Calculate execution time based on quantum or remaining length
            double executionTime = Math.min(quantum, cloudlet.getRemainingLength());
            currentTime += executionTime;
            cloudlet.setRemainingLength(cloudlet.getRemainingLength() - executionTime);

            // If cloudlet is not finished, re-add it to the queue
            if (cloudlet.getRemainingLength() > 0) {
                queue.offer(cloudlet);
            } else {
                cloudlet.setExecStartTime(currentTime - executionTime);  // Set start time as the previous time before execution
                cloudlet.setFinishTime(currentTime);  // Set finish time to current time after execution
            }
        }

        broker.submitCloudletList(cloudletList); // Submit the updated list after scheduling
    }


    private static void saveResultsToCSV(String filePath, List<Cloudlet> finishedCloudlets, double makeSpan, double throughput,
                                         long totalLength, double avgWaitTime, double avgResponseTime, double avgTurnaroundTime) {
        try {
            File rrFile = new File(filePath);
            BufferedWriter rrWriter = new BufferedWriter(new FileWriter(rrFile, true));

            if (rrFile.length() == 0) {
                rrWriter.write("Cloudlet ID, VM ID, Status, Arrival Time, Start Time, Finish Time, Length, File Size, Output Size\n");
            }

            for (Cloudlet cloudlet : finishedCloudlets) {
                rrWriter.write(String.format("%-12d, %-5d, %-7s, %-12d, %-10.2f, %-11.2f, %-7d, %-9d, %-11d\n",
                        cloudlet.getCloudletId(),
                        cloudlet.getVmId(),
                        cloudlet.getCloudletStatusString(),
                        ((CustomCloudlet) cloudlet).getArrivalTime(),
                        cloudlet.getExecStartTime(),
                        cloudlet.getFinishTime(),
                        cloudlet.getCloudletLength(),
                        cloudlet.getCloudletFileSize(),
                        cloudlet.getCloudletOutputSize()));
            }
            rrWriter.close();
            System.out.println("Results saved to " + filePath);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void saveAlgorithmResultsToCSV(String filePath, double makeSpan, double throughput,
                                                  long totalLength, double avgWaitTime, double avgResponseTime,
                                                  double avgTurnaroundTime, int totalCloudlets) {
        try {
            File algorithmFile = new File(filePath);
            BufferedWriter algorithmWriter = new BufferedWriter(new FileWriter(algorithmFile, true));

            if (algorithmFile.length() == 0) {
                algorithmWriter.write("Algorithm Name, MakeSpan, Throughput, Avg Wait Time, Avg Response Time, Avg Turnaround Time, Number of Cloudlets\n");
            }

            algorithmWriter.write(String.format("Round Robin, %.2f, %.2f, %.2f, %.2f, %.2f, %d\n",
                    makeSpan,
                    throughput,
                    avgWaitTime,
                    avgResponseTime,
                    avgTurnaroundTime,
                    totalCloudlets));

            algorithmWriter.close();
            System.out.println("Algorithm results saved to " + filePath);

        } catch (IOException e) {
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

        String arch = "x86"; //architecture of physical hardware
        String os = "Linux";
        String vmm = "Xen"; //VMM
        double timeZone = 5.5;
        double cost = 3.0;
        double costPerMem = 0.05;
        double costPerStorage = 0.001;
        double costPerBw = 0.0;

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(arch, os, vmm, hostList, timeZone, cost, costPerMem, costPerStorage, costPerBw);
        return new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), new ArrayList<>(), 0);
    }
}
