package team4618.robot;

import java.util.HashMap;

public class Subsystems {
    public static HashMap<String, Subsystem> subsystems = new HashMap<>();

    public static void init() {
        for(Subsystem s : subsystems.values()) {
            s.initSystem();
        }
    }

    public static void postState() {
        for(Subsystem s : subsystems.values()) {
            s.postState();
        }
    }
}
