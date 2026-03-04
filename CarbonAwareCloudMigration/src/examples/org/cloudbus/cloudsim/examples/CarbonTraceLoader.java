package examples.org.cloudbus.cloudsim.examples;

import java.io.*;
import java.util.*;

public class CarbonTraceLoader {
    
    private List<Double> carbonIntensityTrace = new ArrayList<>();
    private int currentIndex = 0;
    
    public CarbonTraceLoader(String filePath) {
        loadFromCSV(filePath);
    }
    
    private void loadFromCSV(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean firstLine = true;
            
            while ((line = br.readLine()) != null) {
                if (firstLine) { firstLine = false; continue; }
                
                String[] cols = line.split(",");
                try {
                    double intensity = Double.parseDouble(cols[1].trim());
                    if (intensity > 0) {
                        carbonIntensityTrace.add(intensity);
                    }
                } catch (NumberFormatException e) {
                    // skip malformed rows
                }
            }
            System.out.println("Loaded " + carbonIntensityTrace.size() 
                + " real carbon intensity values.");
                
        } catch (IOException e) {
            System.err.println("Could not load carbon trace: " + e.getMessage());
        }
    }
    
    public double getNext() {
        if (carbonIntensityTrace.isEmpty()) {
            return 200 + Math.random() * 600;
        }
        double value = carbonIntensityTrace.get(currentIndex % carbonIntensityTrace.size());
        currentIndex++;
        return value;
    }
    
    public void printStats() {
        if (carbonIntensityTrace.isEmpty()) return;
        double min = Collections.min(carbonIntensityTrace);
        double max = Collections.max(carbonIntensityTrace);
        double avg = carbonIntensityTrace.stream()
            .mapToDouble(Double::doubleValue).average().orElse(0);
        System.out.println("=== Carbon Trace Stats ===");
        System.out.println("Min: " + min + " gCO2/kWh");
        System.out.println("Max: " + max + " gCO2/kWh");
        System.out.println("Avg: " + String.format("%.2f", avg) + " gCO2/kWh");
    }

	public int getTraceSize() {
		// TODO Auto-generated method stub
		 return carbonIntensityTrace.size();
	}

	public double getPercentile(int percentile) {
		List<Double> sorted = new ArrayList<>(carbonIntensityTrace);
	    Collections.sort(sorted);
	    int index = (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1;
	    return sorted.get(index);
	}
}