package team4618.robot;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.Timer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

public class CommandSequence {
    public static class CommandState<T> {
        public double startTime;
        public double elapsedTime;
        public boolean init = true;
        public NetworkTable currentlyExecutingTable;
        public T data;

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

    public static class CommandInstance implements Executable {
        public Command command;
        public double[] parameters;
        public CommandState state;
        Object data;

        public CommandInstance(String subsystemName, String commandName, double[] params) {
            try {
                String paramPrintout = "";
                for(double p : params) { paramPrintout += (p + ", "); }
                System.out.println(subsystemName + " -> " + commandName + "(" + paramPrintout + ")");

                command = Subsystems.subsystems.get(subsystemName).commands.get(commandName);
                parameters = params;

                if(command.initializer != null) {
                    Class<?>[] paramTypes = command.initializer.getParameterTypes();
                    boolean overflowArray = paramTypes[paramTypes.length - 1] == double[].class;

                    Object[] initializerParams = new Object[paramTypes.length];
                    if (overflowArray) {
                        initializerParams[paramTypes.length - 1] = Arrays.copyOfRange(parameters, paramTypes.length - 1, parameters.length);
                    }
                    for (int i = 0; i < paramTypes.length - (overflowArray ? 1 : 0); i++) {
                        initializerParams[i] = parameters[i];
                    }

                    System.out.println(Arrays.toString(initializerParams));
                    data = command.initializer.invoke(command.subsystem, initializerParams);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }

        public boolean execute(CommandSequence program) {
            try {
                NetworkTable currentlyExecutingTable = program.currentlyExecutingTable;

                if (currentlyExecutingTable != null) {
                    currentlyExecutingTable.getEntry("Subsystem Name").setString(command.subsystem.name());
                    currentlyExecutingTable.getEntry("Command Name").setString(command.command.getName());
                }

                if (state == null) {
                    System.out.println("Init " + command.command.getName());
                    state = new CommandState(currentlyExecutingTable);
                    state.data = data;
                }
                state.update();

                Class<?>[] paramTypes = command.command.getParameterTypes();
                boolean overflowArray = paramTypes[paramTypes.length - 1] == double[].class;

                Object[] params = new Object[paramTypes.length];
                params[0] = state;
                if (overflowArray) {
                    params[paramTypes.length - 1] = Arrays.copyOfRange(parameters, paramTypes.length - 2, parameters.length);
                }
                for (int i = 0; i < paramTypes.length - (overflowArray ? 2 : 1); i++) {
                    params[i + 1] = parameters[i];
                }

                Object ret = command.command.invoke(command.subsystem, params);
                state.init = false;
                return !(ret instanceof Boolean) || ((Boolean) ret);
            } catch(Exception e) { e.printStackTrace(); }
            return false;
        }

        public void reset() {
            state = null;
        }
    }

    public static class BranchOption {
        public ArrayList<Executable> commands;
        public String condition;
    }

    public static class BranchCommand implements Executable {
        public ArrayList<BranchOption> ifs = new ArrayList<>();
        public ArrayList<Executable> elseCommands;

        public boolean execute(CommandSequence program) {
            boolean branchPicked = false;
            for(BranchOption branch : ifs) {
                if(getCondition(branch.condition)) {
                    program.commands.addAll(program.currentlyExecuting, branch.commands);
                    program.commands.remove(program.currentlyExecuting + branch.commands.size());
                    branchPicked = true;
                    break;
                }
            }

            if(!branchPicked) {
                program.commands.addAll(program.currentlyExecuting, elseCommands);
                program.commands.remove(program.currentlyExecuting + elseCommands.size());
            }

            return false;
        }

        public void reset() {
            ifs.forEach(o -> o.commands.forEach(Executable::reset));
            elseCommands.forEach(Executable::reset);
        }
    }

    @Retention(RetentionPolicy.RUNTIME) public @interface Logic {}
    public static NetworkTableInstance network;
    public static NetworkTable table;
    public static NetworkTable autoTable;
    public static NetworkTable logicTable;
    public static NetworkTable teleopTable;

    public static HashMap<String, Method> conditions = new HashMap<>();
    public static Object logicProvider;

    public static void init(Object inLogicProvider, String name) {
        network = NetworkTableInstance.getDefault();
        table = network.getTable("Custom Dashboard");
        table.getEntry("name").setValue(name);
        autoTable = table.getSubTable("Autonomous");
        logicTable = table.getSubTable("Logic");
        teleopTable = table.getSubTable("Teleop");

        logicProvider = inLogicProvider;
        for(Method logicFunction : logicProvider.getClass().getDeclaredMethods()) {
            if(logicFunction.isAnnotationPresent(Logic.class)) {
                logicTable.getEntry(logicFunction.getName()).setString("Unknown");
                conditions.put(logicFunction.getName(), logicFunction);
            }
        }
    }

    public static boolean getCondition(String condition) {
        try {
            Method logicFunction = conditions.get(condition);
            return (Boolean) logicFunction.invoke(logicProvider);
        } catch (Exception e) { e.printStackTrace(); }
        return false;
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

    interface Executable {
        boolean execute(CommandSequence program);
        void reset();
    }

    ArrayList<Executable> loadedCommands = new ArrayList<>();
    ArrayList<Executable> commands = new ArrayList<>();
    int currentlyExecuting = 0;
    NetworkTable currentlyExecutingTable;

    public CommandSequence(NetworkTable in) { currentlyExecutingTable = in; }

    public void reset() {
        commands.clear();
        commands.addAll(loadedCommands);
        commands.forEach(c -> c.reset());
        currentlyExecuting = 0;
    }

    public boolean isDone() {
        return currentlyExecuting >= commands.size();
    }

    public static ArrayList<Executable> loadCommandsFromJSON(JSONArray jsonCommands) {
        ArrayList<Executable> result = new ArrayList<>();
        for(Object obj : jsonCommands) {
            JSONObject command = (JSONObject) obj;

            if(command.containsKey("Subsystem Name") && command.containsKey("Command Name") && command.containsKey("Params")) {
                JSONArray jsonParams = (JSONArray) command.get("Params");
                double[] params = new double[jsonParams.size()];
                for(int i = 0; i < params.length; i++)
                    params[i] = (double) jsonParams.get(i);

                CommandInstance newCommand = new CommandInstance((String) command.get("Subsystem Name"),
                                                                 (String) command.get("Command Name"),
                                                                 params);
                result.add(newCommand);
            } else if(command.containsKey("Ifs")) {
                JSONArray ifs = (JSONArray) command.get("Ifs");
                BranchCommand branchCommand = new BranchCommand();

                System.out.println("Branch");
                for(Object branchObj : ifs) {
                    JSONObject branch = (JSONObject) branchObj;
                    BranchOption branchOption = new BranchOption();

                    branchOption.condition = (String) branch.get("Condition");
                    branchOption.commands = loadCommandsFromJSON((JSONArray) branch.get("Commands"));
                    System.out.println("if(" + branchOption.condition + ")");
                    branchCommand.ifs.add(branchOption);
                }

                branchCommand.elseCommands = command.containsKey("Else") ? loadCommandsFromJSON((JSONArray) command.get("Else")) : new ArrayList<>();
                result.add(branchCommand);
            }
        }
        return result;
    }

    public void run() {
        if(!isDone()) {
            try {
                Executable currentCommand = commands.get(currentlyExecuting);

                if(currentCommand.execute(this)) {
                    if(currentlyExecutingTable != null) {
                        for (String key : currentlyExecutingTable.getKeys()) {
                            currentlyExecutingTable.getEntry(key).delete();
                        }
                    }
                    currentlyExecuting++;
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
    }
}
