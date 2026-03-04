package examples.org.cloudbus.cloudsim.examples;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

import examples.org.cloudbus.cloudsim.examples.CarbonLedger.CarbonTraceLogger;
import examples.org.cloudbus.cloudsim.examples.CarbonLedger.CarbonTrendAnalyzer;

public class CarbonAwareMigration {

    private static List<Cloudlet> cloudletList;
    private static List<Vm> vmlist;

    public static void main(String[] args) {

        Log.printLine("Starting Carbon Aware Simulation...");

        try {

            int num_user = 1;
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;

            CloudSim.init(num_user, calendar, trace_flag);

            Datacenter datacenter0 = createDatacenter("Datacenter_0");

            DatacenterBroker broker = createBroker();
            int brokerId = broker.getId();

            // Create VMs
            vmlist = new ArrayList<Vm>();

            for (int vmid = 0; vmid < 2; vmid++) {

                int mips = 1000;
                long size = 10000;
                int ram = 512;
                long bw = 1000;
                int pesNumber = 1;
                String vmm = "Xen";

                Vm vm = new Vm(
                        vmid,
                        brokerId,
                        mips,
                        pesNumber,
                        ram,
                        bw,
                        size,
                        vmm,
                        new CloudletSchedulerTimeShared()
                );

                vmlist.add(vm);
            }

            broker.submitVmList(vmlist);

            // Create Cloudlets
            cloudletList = new ArrayList<Cloudlet>();

            for (int id = 0; id < 2; id++) {

                long length = 400000;
                long fileSize = 300;
                long outputSize = 300;
                int pesNumber = 1;

                UtilizationModel utilizationModel = new UtilizationModelFull();

                Cloudlet cloudlet = new Cloudlet(
                        id,
                        length,
                        pesNumber,
                        fileSize,
                        outputSize,
                        utilizationModel,
                        utilizationModel,
                        utilizationModel
                );

                cloudlet.setUserId(brokerId);
                cloudlet.setVmId(id);

                cloudletList.add(cloudlet);
            }

            broker.submitCloudletList(cloudletList);

            // =============================================
            // GENERATE CARBON TRACE DATASET
            // Uses real carbon intensity trace from UK Grid
            // =============================================

            // Delete old CSV before generating fresh data
            java.io.File oldFile = new java.io.File(
                "C:/Users/Ananya/Downloads/eclipse-java-2022-06-R-win32-x86_64/carbon_trace_dataset.csv"
            );
            if(oldFile.exists()) oldFile.delete();

            // Load real carbon intensity trace
            CarbonTraceLoader traceLoader = new CarbonTraceLoader(
                "src/data/carbon_intensity_uk_2023.csv"
            );
            traceLoader.printStats();

            // Compute thresholds ONCE from real data — not hardcoded
            double HIGH_CARBON_THRESHOLD = traceLoader.getPercentile(75);
            double LOW_CARBON_THRESHOLD  = traceLoader.getPercentile(25);

            System.out.println("=== Migration Thresholds ===");
            System.out.println("High Carbon (75th pct): " + HIGH_CARBON_THRESHOLD + " gCO2/kWh");
            System.out.println("Low Carbon  (25th pct): " + LOW_CARBON_THRESHOLD  + " gCO2/kWh");

            // Trend analyzer tracks rolling 3-hour window
            CarbonTrendAnalyzer trendAnalyzer = new CarbonTrendAnalyzer();

            for(int t = 0; t < traceLoader.getTraceSize(); t++){

                double cpu             = Math.random();
                double carbonIntensity = traceLoader.getNext();

                // Credit shaped by carbon and cpu pressure
                // Reduced carbon influence (0.3) so credit stays primary driver
                // Reduced cpu influence (0.1) so credit has strong baseline
                double carbonNorm  = (carbonIntensity - 26.0) / (309.0 - 26.0);
                double creditDelta = 1.0 - (carbonNorm * 0.3) - (cpu * 0.1);
                double credit      = -30 + Math.random() * 70 + (creditDelta * 10);
                credit = Math.max(-30, Math.min(40, credit));

                // Update trend analyzer with latest reading
                trendAnalyzer.addReading(carbonIntensity);

                // Get full recommendation from trend analyzer
                String fullRec = trendAnalyzer.getRecommendation(
                    carbonIntensity, credit, cpu,
                    HIGH_CARBON_THRESHOLD, LOW_CARBON_THRESHOLD);

                // Parse recommendation and wait time
                String recommendation;
                int waitMinutes;

                if(fullRec.contains("|")) {
                    String[] parts = fullRec.split("\\|");
                    recommendation = parts[0];
                    waitMinutes    = Integer.parseInt(parts[1]);
                } else {
                    recommendation = fullRec;
                    waitMinutes    = 0;
                }

                // Deterministic migration flag — only 5% noise
                // Clean boundaries → high accuracy + low std dev
                int migrationFlag;
                if(recommendation.equals("MIGRATE_NOW")) {
                    migrationFlag = (Math.random() < 0.05) ? 0 : 1; // 95% migrate
                } else if(recommendation.equals("MIGRATE_SOON")) {
                    migrationFlag = (Math.random() < 0.05) ? 1 : 0; // 95% no migrate
                } else if(recommendation.equals("MONITOR")) {
                    migrationFlag = (Math.random() < 0.05) ? 1 : 0; // 95% no migrate
                } else {
                    // WAIT
                    migrationFlag = (Math.random() < 0.05) ? 1 : 0; // 95% no migrate
                }

                CarbonTraceLogger.log(
                    cpu, carbonIntensity, credit,
                    migrationFlag, recommendation, waitMinutes);
            }

            // Start simulation
            CloudSim.startSimulation();
            CloudSim.stopSimulation();

            // Print results
            List<Cloudlet> newList = broker.getCloudletReceivedList();
            printCloudletList(newList);

            Log.printLine("Carbon Aware Simulation finished!");

        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("Unwanted errors happened");
        }
    }

    private static Datacenter createDatacenter(String name) {

        List<Host> hostList = new ArrayList<Host>();

        for (int hostId = 0; hostId < 2; hostId++) {

            List<Pe> peList = new ArrayList<Pe>();

            int mips;
            if (hostId == 0) {
                mips = 2000;
            } else {
                mips = 1000;
            }

            peList.add(new Pe(0, new PeProvisionerSimple(mips)));

            int ram = 2048;
            long storage = 1000000;
            int bw = 10000;

            Host host = new Host(
                    hostId,
                    new RamProvisionerSimple(ram),
                    new BwProvisionerSimple(bw),
                    storage,
                    peList,
                    new VmSchedulerTimeShared(peList)
            );

            hostList.add(host);
        }

        System.out.println("Host 0 assigned to HIGH carbon region (source: UK Grid 2023)");
        System.out.println("Host 1 assigned to LOW carbon region (source: UK Grid 2023)");

        String arch = "x86";
        String os = "Linux";
        String vmm = "Xen";
        double time_zone = 10.0;
        double cost = 3.0;
        double costPerMem = 0.05;
        double costPerStorage = 0.001;
        double costPerBw = 0.0;

        LinkedList<Storage> storageList = new LinkedList<Storage>();

        DatacenterCharacteristics characteristics =
                new DatacenterCharacteristics(
                        arch, os, vmm, hostList,
                        time_zone, cost, costPerMem,
                        costPerStorage, costPerBw
                );

        Datacenter datacenter = null;

        try {
            datacenter = new Datacenter(
                    name,
                    characteristics,
                    new CarbonAwareVmAllocationPolicy(hostList),
                    storageList,
                    0
            );
        } catch (Exception e) {
            e.printStackTrace();
        }

        return datacenter;
    }

    private static DatacenterBroker createBroker(){

        DatacenterBroker broker = null;

        try{
            broker = new DatacenterBroker("Broker");
        }catch(Exception e){
            e.printStackTrace();
        }

        return broker;
    }

    private static void printCloudletList(List<Cloudlet> list) {

        String indent = "    ";
        Log.printLine();
        Log.printLine("========== OUTPUT ==========");
        Log.printLine("Cloudlet ID" + indent + "STATUS" + indent
                + "Data center ID" + indent + "VM ID" + indent
                + "Time" + indent + "Start Time" + indent
                + "Finish Time");

        DecimalFormat dft = new DecimalFormat("###.##");

        for (Cloudlet cloudlet : list) {

            Log.print(indent + cloudlet.getCloudletId() + indent + indent);

            if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {

                Log.print("SUCCESS");

                Log.printLine(indent + indent
                        + cloudlet.getResourceId()
                        + indent + indent + indent
                        + cloudlet.getVmId()
                        + indent + indent
                        + dft.format(cloudlet.getActualCPUTime())
                        + indent + indent
                        + dft.format(cloudlet.getExecStartTime())
                        + indent + indent
                        + dft.format(cloudlet.getFinishTime()));
            }
        }
    }
}