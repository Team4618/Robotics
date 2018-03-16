package team4618.robot;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.Timer;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

public class CommandSequence {
    public static class CommandState {
        public double startTime;
        public double elapsedTime;
        public boolean init = true;
        public NetworkTable currentlyExecutingTable;

        public CommandState(NetworkTable in) {
            currentlyExecutingTable = in;
            startTime = Timer.getFPGATimestamp();
        }

        public void update() {
            elapsedTime = Timer.getFPGATimestamp() - startTime;
        }

        public void postState(String name, Subsystem.Units unit, double value) {
            if(currentlyExecutingTable != null) {
                currentlyExecutingTable.getEntry(name + "_Value").setValue(value);
                currentlyExecutingTable.getEntry(name + "_Unit").setValue(unit.toString());
            }
        }
    }

    public static class Command {
        public Subsystem subsystem;
        public Method command;
        public double[] parameters;
        public CommandState state;

        public Command(String subsystemName, String commandName, double[] params) {
            try {
                String paramPrintout = "";
                for(double p : params) { paramPrintout += (p + ", "); }
                System.out.println(subsystemName + " -> " + commandName + "(" + paramPrintout + ")");

                subsystem = Subsystems.subsystems.get(subsystemName);
                for(Method m : subsystem.getClass().getDeclaredMethods()) {
                    if(m.getName().equals(commandName))
                        command = m;
                }
                parameters = params;
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    @Retention(RetentionPolicy.RUNTIME) public @interface Logic {}
    public static NetworkTableInstance network;
    public static NetworkTable table;
    public static NetworkTable autoTable;
    public static NetworkTable logicTable;
    public static NetworkTable teleopTable;

    public static void init(Object logicProvider, String name) {
        network = NetworkTableInstance.getDefault();
        table = network.getTable("Custom Dashboard");
        table.getEntry("name").setValue(name);
        autoTable = table.getSubTable("Autonomous");
        logicTable = table.getSubTable("Logic");
        teleopTable = table.getSubTable("Teleop");

        for(Method logicFunction : logicProvider.getClass().getDeclaredMethods()) {
            if(logicFunction.isAnnotationPresent(Logic.class)) {
                logicTable.getEntry(logicFunction.getName()).setString("Unknown");
            }
        }
    }

    public static void resetLogic(Object logicProvider) {
        for (Method logicFunction : logicProvider.getClass().getDeclaredMethods()) {
            if (logicFunction.isAnnotationPresent(Logic.class)) {
                try {
                    logicTable.getEntry(logicFunction.getName()).setString((Boolean) logicFunction.invoke(logicProvider) ? "True" : "False");
                } catch (Exception e) { e.printStackTrace(); }
            }
        }
    }


    ArrayList<Command> commands = new ArrayList<>();
    int currentlyExecuting = 0;
    NetworkTable currentlyExecutingTable;

    public CommandSequence(NetworkTable in) { currentlyExecutingTable = in; }

    public void reset() {
        commands.clear();
        currentlyExecuting = 0;
    }

    public void addCommand(String subsystemName, String commandName, double... params) {
        commands.add(new Command(subsystemName, commandName, params));
    }

    public boolean isDone() {
        return currentlyExecuting < commands.size();
    }

    public void loadCommandsFromTable(NetworkTable commandTable) {
        int[] ordered = commandTable.getSubTables().stream().mapToInt(Integer::valueOf).toArray();
        Arrays.sort(ordered);
        boolean choseConditional = false;
        for (int i : ordered) {
            NetworkTable currCommandTable = commandTable.getSubTable(String.valueOf(i));

            if(currCommandTable.containsKey("Subsystem Name") && currCommandTable.containsKey("Command Name") && currCommandTable.containsKey("Params")) {
                this.addCommand(currCommandTable.getEntry("Subsystem Name").getString(""),
                                currCommandTable.getEntry("Command Name").getString(""),
                                currCommandTable.getEntry("Params").getDoubleArray(new double[0]));
            } else if(currCommandTable.containsKey("Conditional") && currCommandTable.containsSubTable("commands") && !choseConditional) {
                boolean condition = logicTable.getEntry(currCommandTable.getEntry("Conditional").getString("")).getString("").equals("True");
                System.out.println(currCommandTable.getEntry("Conditional").getString("") + " " + condition);
                if(condition) {
                    choseConditional = true;
                    loadCommandsFromTable(currCommandTable.getSubTable("commands"));
                }
            }
        }
    }

    public void run() {
        if(isDone()) {
            try {
                Command currentCommand = commands.get(currentlyExecuting);

                if (currentCommand.state == null) {
                    currentCommand.state = new CommandState(currentlyExecutingTable);

                    if(currentlyExecutingTable != null) {
                        currentlyExecutingTable.getEntry("Subsystem Name").setString(currentCommand.subsystem.name());
                        currentlyExecutingTable.getEntry("Command Name").setString(currentCommand.command.getName());
                    }
                }
                currentCommand.state.update();

                Object[] params = new Object[1 + currentCommand.parameters.length];
                params[0] = currentCommand.state;
                for (int i = 0; i < currentCommand.parameters.length; i++) {
                    params[i + 1] = currentCommand.parameters[i];
                }

                Object ret = currentCommand.command.invoke(currentCommand.subsystem, params);
                if(!(ret instanceof Boolean) || ((Boolean) ret)){
                    if(currentlyExecutingTable != null) {
                        for (String key : currentlyExecutingTable.getKeys()) {
                            currentlyExecutingTable.getEntry(key).delete();
                        }
                    }
                    currentlyExecuting++;
                }

                currentCommand.state.init = false;
            } catch (Exception e) { e.printStackTrace(); }
        }
    }
}
