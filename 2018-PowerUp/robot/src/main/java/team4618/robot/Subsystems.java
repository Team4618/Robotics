package team4618.robot;

import java.util.HashMap;

public class Subsystems {
    public static HashMap<String, Subsystem> subsystems = new HashMap<>();

    public static void init() { subsystems.values().forEach(Subsystem::initSystem); }
    public static void postState() { subsystems.values().forEach(Subsystem::postState); }

    public static void periodic() {
        subsystems.values().forEach(s -> {
            if(s.periodicEnabled)
                s.periodic();
        });
    }
}
