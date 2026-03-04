package examples.org.cloudbus.cloudsim.examples.CarbonLedger;

import java.util.*;

public class VMCarbonLedger {

    private Map<Integer, VMCarbonRecord> ledger = new HashMap<>();

    public VMCarbonRecord getRecord(int vmId){
        if(!ledger.containsKey(vmId)){
            ledger.put(vmId, new VMCarbonRecord(vmId));
        }
        return ledger.get(vmId);
    }

    public void updateCredit(int vmId, double delta){
        VMCarbonRecord record = getRecord(vmId);
        record.carbonCredit += delta;
        // Track debt separately when credit goes negative
        if(record.carbonCredit < 0){
            record.carbonDebt += Math.abs(delta);
        }
    }

    public void updateDebt(int vmId, double delta){
        VMCarbonRecord record = getRecord(vmId);
        record.carbonDebt += delta;
    }

    // Optional: reset a VM's ledger (e.g. after migration)
    public void resetRecord(int vmId){
        ledger.put(vmId, new VMCarbonRecord(vmId));
    }
}