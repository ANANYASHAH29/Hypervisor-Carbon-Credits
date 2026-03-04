package examples.org.cloudbus.cloudsim.examples.CarbonLedger;
import org.cloudbus.cloudsim.Vm;

public class CarbonMigrationController {
    private VMCarbonLedger ledger = new VMCarbonLedger();
    
    public boolean shouldMigrate(Vm vm, double carbonIntensity, double cpuUsage){
        // Calculate how much credit this VM earns or loses this tick
        double deltaCredit = 1 - (carbonIntensity / 1000.0) * cpuUsage;
        ledger.updateCredit(vm.getId(), deltaCredit);
        double credit = ledger.getRecord(vm.getId()).carbonCredit;
        int state = CarbonStateMachine.evaluateState(credit);
        return state == CarbonStateMachine.RECOVERY;
    }
}