package team4618.robot;

import edu.wpi.first.networktables.*;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;

import static team4618.robot.CommandSequence.network;

public abstract class Subsystem implements TableEntryListener {
    @Retention(RetentionPolicy.RUNTIME) public @interface ParameterEnum { }
    @Retention(RetentionPolicy.RUNTIME) public @interface Command { String value() default ""; }
    public enum Units { Feet, FeetPerSecond, Degrees, DegreesPerSecond, Seconds, Unitless, Percent}
    @Retention(RetentionPolicy.RUNTIME) public @interface Unit { Units value(); }

    public abstract void init();
    public void updateParameters() { }
    public void periodic() { }
    public abstract void postState();
    public abstract String name();

    public boolean periodicEnabled = true;
    public NetworkTable table;
    public NetworkTable parameterTable;
    public NetworkTable stateTable;
    public NetworkTable commandTable;
    public Enum[] parameters;

    public double value(Enum param) { return parameterTable.getEntry(param.toString()).getDouble(0); }

    public Subsystem() { Subsystems.subsystems.put(name(), this); }

    public void initSystem() {
        table = network.getTable("Custom Dashboard/Subsystem/" + name());
        parameterTable = network.getTable("Custom Dashboard/Subsystem/" + name() + "/Parameters");
        stateTable = network.getTable("Custom Dashboard/Subsystem/" + name() + "/State");
        commandTable = network.getTable("Custom Dashboard/Subsystem/" + name() + "/Commands");

        int flag = EntryListenerFlags.kUpdate | EntryListenerFlags.kLocal | EntryListenerFlags.kNew;
        parameterTable.addEntryListener(this, flag);

        for(Class inner : this.getClass().getDeclaredClasses())
        {
            if(inner.isEnum() && inner.isAnnotationPresent(ParameterEnum.class))
            {
                parameters = (Enum[]) inner.getEnumConstants();
            }
        }

        for(Method function : this.getClass().getDeclaredMethods()) {
            if(function.isAnnotationPresent(Command.class)) {
                ArrayList<String> params = new ArrayList<>();
                ArrayList<String> units = new ArrayList<>();

                for(Parameter param : function.getParameters()) {
                    if(param.isAnnotationPresent(Unit.class) && (param.getType() != CommandSequence.CommandState.class)) {
                        params.add(param.getName());
                        units.add(param.getAnnotation(Unit.class).value().toString());
                    }
                }

                /*
                String[] paramsArray = new String[params.size()];
                for(int i = 0; i < paramsArray.length; i++) { paramsArray[i] = params.get(i); }

                String[] unitsArray = new String[units.size()];
                for(int i = 0; i < unitsArray.length; i++) { unitsArray[i] = units.get(i); }
                */

                commandTable.getEntry(function.getName() + "_ParamNames").setStringArray(params.toArray(new String[params.size()])); //paramsArray);
                commandTable.getEntry(function.getName() + "_ParamUnits").setStringArray(units.toArray(new String[units.size()])); //unitsArray);
            }
        }

        if(parameters != null) {
            HashMap<String, Double> paramFile = FileIO.MapFromFile(this);
            for (Enum p : parameters) {
                Double param = paramFile.getOrDefault(p.toString(), 0.0);
                System.out.println(name() + ":" + p.toString() + " = " + param);
                parameterTable.getEntry(p.toString()).setValue(param);
            }
        }

        init();
    }

    @Override
    public void valueChanged(NetworkTable table, String key, NetworkTableEntry entry, NetworkTableValue value, int flags) {
        this.updateParameters();

        HashMap<String, Double> paramFile = new HashMap<>();
        for(Enum p : parameters) {
            paramFile.put(p.toString(), value(p));
        }
        FileIO.MapToFile(this, paramFile);
    }

    public void PostState(String name, Units unit, double value) {
        stateTable.getEntry(name + "_Value").setValue(value);
        stateTable.getEntry(name + "_Unit").setValue(unit.toString());
    }
}