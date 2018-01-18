package team4618.robot;

import edu.wpi.first.networktables.*;

import java.util.HashMap;
import static team4618.robot.Robot.*;

public abstract class Subsystem implements TableEntryListener {
    public @interface ParameterEnum { }
    public @interface Command { }
    public enum Units { Feet, FeetPerSecond, Degrees, DegreesPerSecond, Unitless }
    public @interface Unit { Units value(); }
    //public @interface SubsystemClass { String value(); }

    public abstract void init();
    public abstract void enable();
    public abstract void disable();
    public void updateParameters() { }
    public abstract void postState();
    public abstract String name(); //TODO: replace this with something better? An annotation?

    public NetworkTable table;
    public NetworkTable parameterTable;
    public NetworkTable stateTable;
    public Enum[] parameters;

    public double value(Enum param) {
        return parameterTable.getEntry(param.toString()).getDouble(0);
    }

    public void initSystem() {
        table = network.getTable("Custom Dashboard/" + name());
        parameterTable = network.getTable("Custom Dashboard/" + name() + "/Parameters");
        stateTable = network.getTable("Custom Dashboard/" + name() + "/State");
        //TODO: commands system with its own table?

        parameterTable.addEntryListener(this, EntryListenerFlags.kLocal);

        for(Class inner : this.getClass().getDeclaredClasses())
        {
            if(inner.isEnum() && inner.isAnnotationPresent(ParameterEnum.class))
            {
                parameters = (Enum[]) inner.getEnumConstants();
            }
        }

        HashMap<String, Double> paramFile = FileIO.MapFromFile(this);
        for(Enum p : parameters) {

            Double param = paramFile.containsKey(p.toString()) ? paramFile.get(p.toString()) : 0.0;
            parameterTable.getEntry(p.toString()).setValue(param);
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
        stateTable.getEntry(name + "#Value").setValue(value);
        stateTable.getEntry(name + "#Unit").setValue(unit.toString());
    }
}