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

class PriorityCloudlet extends Cloudlet {
    private int priority; // Custom field to store priority
    private long arrivalTime;

    public PriorityCloudlet(int cloudletId, long length, int pesNumber, long fileSize, long outputSize,
                            UtilizationModel utilizationModel, int priority, long arrivalTime) {
        super(cloudletId, length, pesNumber, fileSize, outputSize,
                utilizationModel, utilizationModel, utilizationModel);
        this.priority = priority;
        this.arrivalTime = arrivalTime;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public long getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(long arrivalTime) {
        this.arrivalTime = arrivalTime;
    }
}

public class PrioritySchedulingExample {

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
            List<PriorityCloudlet> cloudletList = new ArrayList<>();

            UtilizationModel utilizationModel = new UtilizationModelFull();
            for (int i = 0; i < numCloudlets; i++) {
                System.out.println("Enter details for Cloudlet " + (i + 1) + " (Format: arrivalTime length fileSize outputSize priority): ");
                long arrivalTime = scanner.nextLong(); // Get the arrival time from user
                long length = scanner.nextLong();
                long fileSize = scanner.nextLong();
                long outputSize = scanner.nextLong();
                int priority = scanner.nextInt();

                // Create PriorityCloudlet instance with correct constructor
                PriorityCloudlet cloudlet = new PriorityCloudlet(i, length, 1, fileSize, outputSize, utilizationModel, priority, arrivalTime);
                cloudlet.setUserId(brokerId);
                cloudletList.add(cloudlet);
            }

            // Sort cloudlets by arrival time first (FCFS) and then by priority
            cloudletList.sort(Comparator.comparingLong(PriorityCloudlet::getArrivalTime)
                    .thenComparingInt(PriorityCloudlet::getPriority));

            // Submit the sorted cloudlets list to the broker
            broker.submitCloudletList(new ArrayList<>(cloudletList));

            // Simulate CloudSim
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
            double totalThroughput = 0; // Add total throughput calculation

            for (Cloudlet cloudlet : finishedCloudlets) {
                PriorityCloudlet priorityCloudlet = (PriorityCloudlet) cloudlet; // Cast to PriorityCloudlet
                long arrivalTime = priorityCloudlet.getArrivalTime();
                double execStartTime = cloudlet.getExecStartTime();
                double finishTimeForCloudlet = cloudlet.getFinishTime();

                // Calculate the wait time, response time, turnaround time, and throughput
                double waitTime = Math.max(execStartTime - arrivalTime, 0);
                double responseTime = Math.max(finishTimeForCloudlet - arrivalTime, 0);
                double turnaroundTime = Math.max(finishTimeForCloudlet - arrivalTime, 0);
                double throughput = (cloudlet.getCloudletLength() / Math.max(finishTime - startTime, 0));// Throughput in cloudlets per second

                totalWaitTime += waitTime;
                totalResponseTime += responseTime;
                totalTurnaroundTime += turnaroundTime;
                totalThroughput += throughput; // Accumulate throughput

                // Update start and finish times
                startTime = Math.min(startTime, execStartTime);
                finishTime = Math.max(finishTime, finishTimeForCloudlet);

                totalLength += cloudlet.getCloudletLength();
            }

            double makeSpan = Math.max(finishTime - startTime, 0);
            double avgWaitTime = totalWaitTime / totalCloudlets;
            double avgResponseTime = totalResponseTime / totalCloudlets;
            double avgTurnaroundTime = totalTurnaroundTime / totalCloudlets;
            double avgThroughput = totalThroughput / totalCloudlets; // Calculate average throughput

            // Output the performance metrics to console
            System.out.println("Cloudlet ID | VM ID | Arrival Time | Priority | Status  | Start Time | Finish Time | Length  | File Size | Output Size | Throughput");
            for (Cloudlet cloudlet : finishedCloudlets) {
                PriorityCloudlet priorityCloudlet = (PriorityCloudlet) cloudlet; // Cast to PriorityCloudlet
                long arrivalTime = priorityCloudlet.getArrivalTime();
                // Calculate throughput
                System.out.printf(
                        "%-12d | %-5d | %-12d | %-8d | %-7s | %-10.2f | %-11.2f | %-7d | %-9d | %-11d\n",
                        cloudlet.getCloudletId(),
                        cloudlet.getVmId(),
                        arrivalTime, // Print arrival time
                        priorityCloudlet.getPriority(),
                        cloudlet.getCloudletStatusString(),
                        cloudlet.getExecStartTime(),
                        cloudlet.getFinishTime(),
                        cloudlet.getCloudletLength(),
                        cloudlet.getCloudletFileSize(),
                        cloudlet.getCloudletOutputSize()
                );
            }
            double throughput = numCloudlets/ makeSpan;
            System.out.println("\nPerformance Metrics:");
            System.out.println("MakeSpan: " + makeSpan);
            System.out.println("Total Cloudlet Length: " + totalLength);
            System.out.println("Average Wait Time: " + avgWaitTime);
            System.out.println("Average Response Time: " + avgResponseTime);
            System.out.println("Average Turnaround Time: " + avgTurnaroundTime);
            System.out.println("Throughput: " + throughput);

            // Save results to CSV
            String priorityFilePath = "priority_results.csv";
            savePriorityResultsToCSV(priorityFilePath, totalCloudlets, finishedCloudlets, makeSpan, totalLength, avgWaitTime, avgResponseTime, avgTurnaroundTime, avgThroughput);

            String algorithmFilePath = "algorithm_results.csv";
            saveAlgorithmResultsToCSV(algorithmFilePath, "Priority", makeSpan, totalLength, avgWaitTime, avgResponseTime, avgTurnaroundTime, totalCloudlets);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void savePriorityResultsToCSV(String filePath, int totalCloudlets, List<Cloudlet> finishedCloudlets, double makeSpan, long totalLength, double avgWaitTime, double avgResponseTime, double avgTurnaroundTime, double avgThroughput) {
        try {
            File file = new File(filePath);
            BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));

            // Write the header if the file is empty
            if (file.length() == 0) {
                writer.write("Cloudlet ID, VM ID, Arrival Time, Priority, Status, Start Time, Finish Time, Length, File Size, Output Size, Throughput, Wait Time, Response Time, Turnaround Time\n");
            }

            // Write the cloudlet data along with performance metrics
            for (Cloudlet cloudlet : finishedCloudlets) {
                PriorityCloudlet priorityCloudlet = (PriorityCloudlet) cloudlet;
                long arrivalTime = priorityCloudlet.getArrivalTime();
                double startTime = cloudlet.getExecStartTime();
                double finishTime = cloudlet.getFinishTime();
                long length = cloudlet.getCloudletLength();
                long fileSize = cloudlet.getCloudletFileSize();
                long outputSize = cloudlet.getCloudletOutputSize();

                // Calculate throughput, wait time, response time, and turnaround time
                double throughput = totalCloudlets / makeSpan;  // Throughput = length / (finishTime - startTime)
                double waitTime = startTime - arrivalTime;
                double responseTime = finishTime - arrivalTime;
                double turnaroundTime = finishTime - arrivalTime;

                // Write data to CSV
                writer.write(String.format("%d, %d, %d, %d, %s, %.2f, %.2f, %d, %d, %d, %.2f, %.2f, %.2f, %.2f\n",
                        cloudlet.getCloudletId(),
                        cloudlet.getVmId(),
                        arrivalTime,
                        priorityCloudlet.getPriority(),
                        cloudlet.getCloudletStatusString(),
                        startTime,
                        finishTime,
                        length,
                        fileSize,
                        outputSize,
                        throughput,
                        waitTime,
                        responseTime,
                        turnaroundTime));
            }

            writer.close();
            System.out.println("Priority results saved to " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void saveAlgorithmResultsToCSV(String filePath, String algorithmName, double makeSpan,
                                                  long totalLength, double avgWaitTime, double avgResponseTime,
                                                  double avgTurnaroundTime, int cloudletCount) {
        try {
            File file = new File(filePath);
            BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));

            // Write the header if the file is empty
            if (file.length() == 0) {
                writer.write("Algorithm Name, MakeSpan, Throughput, Avg Wait Time, Avg Response Time, Avg Turnaround Time, Cloudlet Count\n");
            }

            // Calculate throughput
            double throughput = cloudletCount / makeSpan; // Throughput = totalLength / MakeSpan

            // Write the performance metrics to CSV
            writer.write(String.format("%s, %.2f, %.2f, %.2f, %.2f, %.2f, %d\n",
                    algorithmName,
                    makeSpan,
                    throughput,
                    avgWaitTime,
                    avgResponseTime,
                    avgTurnaroundTime,
                    cloudletCount));

            writer.close();
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

        Host host = new Host(0, new RamProvisionerSimple(ram), new BwProvisionerSimple(bw), storage, peList,
                new VmSchedulerTimeShared(peList));
        hostList.add(host);

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                "x86", "Linux", "Xen", hostList, 10, 1, 1, 1, 1);
        Datacenter datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), null, 0);

        return datacenter;
    }
}
