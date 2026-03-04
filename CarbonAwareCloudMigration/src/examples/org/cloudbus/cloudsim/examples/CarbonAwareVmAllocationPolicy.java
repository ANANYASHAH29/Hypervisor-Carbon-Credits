package examples.org.cloudbus.cloudsim.examples;

import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicy;

import examples.org.cloudbus.cloudsim.examples.CarbonLedger.*;

public class CarbonAwareVmAllocationPolicy extends VmAllocationPolicy {
    
    // UK Grid 2023 peak carbon intensity (gCO2/kWh)
    private static final double HIGH_CARBON_THRESHOLD = 309.0;
    
    private CarbonMigrationController controller =
            new CarbonMigrationController();
    
    public CarbonAwareVmAllocationPolicy(List<? extends Host> list) {
        super(list);
    }

    @Override
    public boolean allocateHostForVm(Vm vm){
        if(getHostList() == null || getHostList().size() < 2)
            return false;
        Host highCarbonHost = getHostList().get(0);
        Host lowCarbonHost = getHostList().get(1);

        double carbonHighIntensity = HIGH_CARBON_THRESHOLD; // real UK grid max
        double cpuUsageEstimate = 0.5;

        if(controller.shouldMigrate(vm, carbonHighIntensity, cpuUsageEstimate)){
            if(lowCarbonHost.vmCreate(vm)){
                vm.setHost(lowCarbonHost);
                System.out.println("VM " + vm.getId() + " migrated to GREEN host (Recovery Mode)");
                return true;
            }
        }

        if(highCarbonHost.getVmList().isEmpty()){
            if(highCarbonHost.vmCreate(vm)){
                vm.setHost(highCarbonHost);
                System.out.println("VM " + vm.getId() + " placed in high carbon host (Initial boot)");
                return true;
            }
        }

        if(lowCarbonHost.vmCreate(vm)){
            vm.setHost(lowCarbonHost);
            return true;
        }
        return false;
    }

    @Override
    public void deallocateHostForVm(Vm vm) {
        if(vm.getHost() != null){
            vm.getHost().vmDestroy(vm);
        }
    }

    @Override
    public Host getHost(Vm vm) {
        return vm.getHost();
    }

    @Override
    public Host getHost(int vmId, int userId) {
        for(Host host : getHostList()){
            for(Vm vm : host.getVmList()){
                if(vm.getId()==vmId && vm.getUserId()==userId){
                    return host;
                }
            }
        }
        return null;
    }

    @Override
    public List<Map<String, Object>> optimizeAllocation(List<? extends Vm> vmList) {
        return null;
    }

    @Override
    public boolean allocateHostForVm(Vm arg0, Host arg1) {
        return false;
    }
}