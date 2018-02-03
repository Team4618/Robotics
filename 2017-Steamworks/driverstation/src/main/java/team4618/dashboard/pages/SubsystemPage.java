package team4618.dashboard.pages;

import edu.wpi.first.networktables.*;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import team4618.dashboard.Main;
import team4618.dashboard.components.Compass;
import team4618.dashboard.components.MultiLineGraph;
import team4618.dashboard.components.ParameterTextbox;
import static team4618.dashboard.components.MultiLineGraph.*;

public class SubsystemPage extends ScrollPane implements TableEntryListener {
    VBox content = new VBox();

    MultiLineGraph graph = new MultiLineGraph();
    Compass compass = new Compass();

    Main.Subsystem subsystem;

    public SubsystemPage(Main.Subsystem inSubsystem) {
        subsystem = inSubsystem;
        this.setContent(content);

        //int flag = EntryListenerFlags.kUpdate | EntryListenerFlags.kLocal | EntryListenerFlags.kNew;
        //subsystem.stateTable.addEntryListener(this, flag);

        Button clear_graph = new Button("Clear Graph");
        clear_graph.setOnAction(event -> {
            graph.graphs.clear();
            graph.toggles.getChildren().clear();
            graph.draw();
        });

        content.getChildren().addAll(graph, clear_graph, compass);

        for(String param : subsystem.parameterTable.getKeys()) {
            content.getChildren().add(new ParameterTextbox(subsystem.parameterTable.getEntry(param)));
        }
    }

    public void onJavafxLoop() {
        for(String state : subsystem.stateTable.getSubTables()) {
            NetworkTable stateSubtable = subsystem.stateTable.getSubTable(state);
            Units unit = Units.valueOf(stateSubtable.getEntry("Unit").getString(""));
            double value = stateSubtable.getEntry("Value").getDouble(0);
            long time = System.currentTimeMillis();

            if(unit == Units.Degrees) {
                compass.setAngle(value);
            } else {
                graph.addData(state, unit, value, time / 100000);
            }
        }

        graph.draw();
    }

    @Override
    public void valueChanged(NetworkTable table, String key, NetworkTableEntry entry, NetworkTableValue nt_value, int flags) {
        /*
        String state = key.replace("_Value", "").replace("_Unit", "");
        if(table.containsKey(state + "_Value") && table.containsKey(state + "_Unit")) {
            String unit = table.getEntry(state + "_Unit").getString("");
            double value = table.getEntry(state + "_Value").getDouble(0);
            long time = table.getEntry(state + "_Value").getLastChange();

            if(unit.equals("Degrees")) {
                compass.setAngle(value);
            } else {
                graph.addData(state, unit, value, time / 100000);
            }
        }
        */
    }
}
