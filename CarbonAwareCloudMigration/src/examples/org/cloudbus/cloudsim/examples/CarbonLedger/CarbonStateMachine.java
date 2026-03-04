package examples.org.cloudbus.cloudsim.examples.CarbonLedger;

public class CarbonStateMachine {

    public static final int GREEN    = 0;
    public static final int WARNING  = 1;
    public static final int BROWN    = 2;
    public static final int RECOVERY = 3;

    public static int evaluateState(double credit){
        if     (credit >  20) return GREEN;
        else if(credit >   0) return WARNING;
        else if(credit > -20) return BROWN;
        else                  return RECOVERY;
    }

    // Human-readable label for logging/debugging
    public static String stateName(int state){
        switch(state){
            case GREEN:    return "GREEN";
            case WARNING:  return "WARNING";
            case BROWN:    return "BROWN";
            case RECOVERY: return "RECOVERY";
            default:       return "UNKNOWN";
        }
    }
}