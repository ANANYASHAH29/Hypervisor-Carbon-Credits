package examples.org.cloudbus.cloudsim.examples.CarbonLedger;

import java.io.FileWriter;
import java.io.IOException;

public class CarbonTraceLogger {

    private static final String FILE =
        "C:/Users/Ananya/Downloads/eclipse-java-2022-06-R-win32-x86_64/carbon_trace_dataset.csv";

    public static void log(
            double cpu,
            double carbonIntensity,
            double credit,
            int migrationFlag,
            String recommendation,
            int waitMinutes) {
        try {
            FileWriter fw = new FileWriter(FILE, true);
            fw.append(
                String.format("%.10f,%.10f,%.10f,%d,%s,%d%n",
                    cpu, carbonIntensity, credit,
                    migrationFlag, recommendation, waitMinutes)
            );
            fw.flush();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Keep old method for backward compatibility
    public static void log(
            double cpu,
            double carbonIntensity,
            double credit,
            int migrationFlag) {
        log(cpu, carbonIntensity, credit, migrationFlag, "UNKNOWN", 0);
    }
}