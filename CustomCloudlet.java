package org.cloudbus.cloudsim.examples;
//
//import org.cloudbus.cloudsim.Cloudlet;
//import org.cloudbus.cloudsim.UtilizationModel;
//
//public class CustomCloudlet extends Cloudlet {
//
//    private long arrivalTime;
//
//    public CustomCloudlet(int id, long length, int pes, long fileSize, long outputSize,
//                          UtilizationModel utilizationModel, UtilizationModel utilizationModel2,
//                          UtilizationModel utilizationModel3) {
//        super(id, length, pes, fileSize, outputSize, utilizationModel, utilizationModel2, utilizationModel3);
//    }
//
//    // Getter and Setter for arrivalTime
//    public long getArrivalTime() {
//        return arrivalTime;
//    }
//
//    public void setArrivalTime(long arrivalTime) {
//        this.arrivalTime = arrivalTime;
//    }
//
//}
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.UtilizationModel;

public class CustomCloudlet extends Cloudlet {
    private double remainingLength;
    private long arrivalTime; // Already present
    private double finishTime;

    public CustomCloudlet(int cloudletId, long cloudletLength, int pesNumber, long cloudletFileSize, long cloudletOutputSize,
                          UtilizationModel utilizationModelCpu, UtilizationModel utilizationModelRam, UtilizationModel utilizationModelBw) {
        super(cloudletId, cloudletLength, pesNumber, cloudletFileSize, cloudletOutputSize, utilizationModelCpu, utilizationModelRam, utilizationModelBw);
        this.remainingLength = cloudletLength; // Initialize remaining length
    }

    public double getRemainingLength() {
        return remainingLength;
    }

    public void setRemainingLength(double remainingLength) {
        this.remainingLength = remainingLength;
    }

    public long getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(long arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public double getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(double finishTime) {
        this.finishTime = finishTime;
    }
}
