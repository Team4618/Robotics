package team4618.dashboard.autonomous;

import edu.wpi.first.networktables.NetworkTable;
import team4618.dashboard.Main;

import java.util.HashMap;

public class AutonomousCommandTemplate {
    public String subsystemName;
    public String commandName;
    public String[] parameterUnits;
    public String[] parameterNames;

    public String hashName() { return subsystemName + ":" + commandName; }

    public static HashMap<String, AutonomousCommandTemplate> templates = new HashMap<>();
    public enum ConditionalState { True, False, Unknown }
    public static HashMap<String, ConditionalState> conditionals = new HashMap<>();

    public static void refreshCommandsAndLogic() {
        conditionals.clear();
        for(String conditional : Main.logicTable.getKeys()) {
            conditionals.put(conditional, ConditionalState.valueOf(Main.logicTable.getEntry(conditional).getString("Unknown")));
        }
        conditionals.put("alwaysTrue", ConditionalState.True);

        templates.clear();
        for(String subsystem : Main.subsystemTable.getSubTables()) {
            NetworkTable currSubsystemCommandsTable = Main.network.getTable("Custom Dashboard/Subsystem/" + subsystem + "/Commands");
            for (String key : currSubsystemCommandsTable.getKeys()) {
                if (key.endsWith("_ParamNames")) {
                    String commandName = key.replace("_ParamNames", "");
                    AutonomousCommandTemplate commandTemplate = new AutonomousCommandTemplate();
                    commandTemplate.commandName = commandName;
                    commandTemplate.subsystemName = subsystem;
                    commandTemplate.parameterNames = currSubsystemCommandsTable.getEntry(commandName + "_ParamNames").getStringArray(new String[0]);
                    commandTemplate.parameterUnits = currSubsystemCommandsTable.getEntry(commandName + "_ParamUnits").getStringArray(new String[0]);
                    AutonomousCommandTemplate.templates.put(commandTemplate.hashName(), commandTemplate);
                }
            }
        }
    }
}
