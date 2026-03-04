package examples.org.cloudbus.cloudsim.examples.CarbonLedger;

public class VMCarbonRecord {

    public int    vmId;
    public double carbonCredit;
    public double carbonDebt;
    public double debtTrendSlope;
    public boolean recoveryMode;

    public VMCarbonRecord(int vmId){
        this.vmId          = vmId;
        this.carbonCredit  = 0.0;
        this.carbonDebt    = 0.0;
        this.debtTrendSlope = 0.0;
        this.recoveryMode  = false;
    }

    @Override
    public String toString(){
        return String.format(
            "VM[%d] Credit=%.2f Debt=%.2f Recovery=%b",
            vmId, carbonCredit, carbonDebt, recoveryMode
        );
    }
}