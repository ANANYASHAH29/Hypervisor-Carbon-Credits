package examples.org.cloudbus.cloudsim.examples.CarbonLedger;

import java.util.LinkedList;
import java.util.Queue;

public class CarbonTrendAnalyzer {

    // Stores last 6 readings = 3 hours (each reading = 30 mins)
    private Queue<Double> recentReadings = new LinkedList<>();
    private static final int WINDOW_SIZE = 6;

    public void addReading(double carbonIntensity) {
        recentReadings.add(carbonIntensity);
        if (recentReadings.size() > WINDOW_SIZE) {
            recentReadings.poll();
        }
    }

    // Returns trend: negative = improving, positive = worsening
    public double getTrend() {
        if (recentReadings.size() < 2) return 0.0;
        Double[] values = recentReadings.toArray(new Double[0]);
        return values[values.length - 1] - values[0];
    }

    public boolean isTrendingDown() {
        return getTrend() < -10.0;
    }

    public boolean isTrendingUp() {
        return getTrend() > 10.0;
    }

    // ── Wait time meaning fixed ───────────────────────────
    // MIGRATE_NOW  → 0 mins   (act immediately)
    // MIGRATE_SOON → 15-30    (migrate within short window)
    // MONITOR      → 30-60    (watch closely, reassess soon)
    // WAIT         → 60-120   (conditions good, safe to wait)
    public int calculateWaitMinutes(
            String decision,
            double carbonIntensity,
            double credit,
            double cpu,
            double HIGH_CARBON_THRESHOLD) {

        if (decision.equals("MIGRATE_NOW")) {
            return 0; // act immediately, no waiting
        }

        if (decision.equals("MIGRATE_SOON")) {
            // 15 or 30 mins depending on urgency
            if (credit < -15 || cpu > 0.7) return 15;
            return 30;
        }

        if (decision.equals("MONITOR")) {
            // 30-60 mins — trend determines how soon to reassess
            if (isTrendingUp()) return 30;   // worsening, check sooner
            if (isTrendingDown()) return 60; // improving, more time
            return 45;                        // stable, middle ground
        }

        // WAIT — conditions are good, calculate how long safety lasts
        // Higher carbon ratio = shorter safe window
        double carbonRatio = carbonIntensity / HIGH_CARBON_THRESHOLD;
        int safeWindow = (int)((1.0 - carbonRatio) * 90) + 30; // 30-120 mins

        // Good credit extends safe window
        if (credit > 20) safeWindow = Math.min(120, safeWindow + 30);

        // Trending up shrinks safe window
        if (isTrendingUp()) safeWindow = Math.max(30, safeWindow - 30);

        // Round to nearest 15
        safeWindow = (int)(Math.round(safeWindow / 15.0) * 15);
        return Math.max(30, Math.min(120, safeWindow));
    }

    public String getRecommendation(
            double carbonIntensity,
            double credit,
            double cpu,
            double HIGH_CARBON_THRESHOLD,
            double LOW_CARBON_THRESHOLD) {

        boolean highCarbon     = carbonIntensity > HIGH_CARBON_THRESHOLD;
        boolean mediumCarbon   = carbonIntensity > LOW_CARBON_THRESHOLD;
        boolean lowCredit      = credit < 0;
        boolean criticalCredit = credit < -15; // lowered from -20 → more MIGRATE_NOW
        boolean veryLowCredit  = credit < -25;
        boolean highCpu        = cpu > 0.6;
        boolean veryhighCpu    = cpu > 0.8;

        String decision;

        // ── MIGRATE_NOW ───────────────────────────────────
        if (veryLowCredit) {
            // Credit critically depleted regardless of carbon
            decision = "MIGRATE_NOW";
        } else if (criticalCredit && highCarbon) {
            // Low credit + high carbon = must migrate
            decision = "MIGRATE_NOW";
        } else if (criticalCredit && veryhighCpu) {
            // Low credit + very high workload = must migrate
            decision = "MIGRATE_NOW";
        } else if (highCarbon && isTrendingUp() && lowCredit) {
            // Carbon rising + credit depleting = migrate now
            decision = "MIGRATE_NOW";

        // ── MIGRATE_SOON ──────────────────────────────────
        } else if (lowCredit && highCarbon) {
            // Credit low + high carbon but not yet critical
            decision = "MIGRATE_SOON";
        } else if (lowCredit && highCpu && mediumCarbon) {
            // Credit low + high CPU + medium carbon
            decision = "MIGRATE_SOON";
        } else if (highCarbon && isTrendingUp() && mediumCarbon) {
            // Carbon trending up into high zone
            decision = "MIGRATE_SOON";

        // ── MONITOR ───────────────────────────────────────
        } else if (lowCredit && mediumCarbon) {
            // Credit low but carbon acceptable
            decision = "MONITOR";
        } else if (highCarbon && isTrendingDown()) {
            // High carbon but improving — watch it
            decision = "MONITOR";
        } else if (mediumCarbon && isTrendingUp()) {
            // Medium carbon trending up — keep watching
            decision = "MONITOR";
        } else if (lowCredit) {
            // Only credit concerning
            decision = "MONITOR";

        // ── WAIT ──────────────────────────────────────────
        } else {
            // Conditions acceptable — safe to stay
            decision = "WAIT";
        }

        int waitMins = calculateWaitMinutes(
            decision, carbonIntensity, credit, cpu, HIGH_CARBON_THRESHOLD);

        if (decision.equals("MIGRATE_NOW")) {
            return "MIGRATE_NOW"; // no wait time for immediate action
        }
        return decision + "|" + waitMins;
    }
}