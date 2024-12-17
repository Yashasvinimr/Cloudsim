//package org.cloudbus.cloudsim.examples;
//
//import org.cloudbus.cloudsim.Cloudlet;
//import org.cloudbus.cloudsim.DatacenterBroker;
//import org.cloudbus.cloudsim.Vm;
//
//import java.util.List;
//
//public class PriorityBasedDatacenterBroker extends DatacenterBroker {
//    public PriorityBasedDatacenterBroker(String name) throws Exception {
//        super(name);
//    }
//
//    @Override
//    public void submitCloudlets() {
//        // Sort Cloudlets by priority before submission
//        List<Cloudlet> cloudlets = getCloudletList();
//        cloudlets.sort((c1, c2) -> {
//            if (c1 instanceof CustomCloudlet && c2 instanceof CustomCloudlet) {
//                return Integer.compare(((CustomCloudlet) c1).getPriority(), ((CustomCloudlet) c2).getPriority());
//            }
//            return 0;
//        });
//
//        // Submit the sorted cloudlets list
//        submitCloudletList(cloudlets);  // Use submitCloudletList() instead of submitCloudlet()
//    }
//}
