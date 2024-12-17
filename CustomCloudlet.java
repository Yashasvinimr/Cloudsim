package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.UtilizationModel;

public class CustomCloudlet extends Cloudlet {
    private int priority;  // Add a priority field

    // Constructor to initialize the Cloudlet with priority
    public CustomCloudlet(int cloudletId, long cloudletLength, int pesNumber, long cloudletFileSize,
                          long cloudletOutputSize, UtilizationModel utilizationModelCpu,
                          UtilizationModel utilizationModelRam, UtilizationModel utilizationModelBw,
                          int priority) {
        super(cloudletId, cloudletLength, pesNumber, cloudletFileSize, cloudletOutputSize,
                utilizationModelCpu, utilizationModelRam, utilizationModelBw);
        this.priority = priority;
    }

    // Getter and Setter for priority
    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }
}
