package team4618.dashboard.pages;

import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import team4618.dashboard.Main;
import team4618.dashboard.components.Compass;
import team4618.dashboard.components.MultiLineGraph;
import team4618.dashboard.components.ParameterTextbox;
import static team4618.dashboard.components.MultiLineGraph.*;

public class SubsystemPage extends ScrollPane {
    VBox content = new VBox();

    MultiLineGraph graph = new MultiLineGraph();
    Compass compass = new Compass();

    Main.Subsystem subsystem;
    long startTime;

    public SubsystemPage(Main.Subsystem inSubsystem) {
        subsystem = inSubsystem;
        this.setContent(content);

        Button clear_graph = new Button("Clear Graph");
        clear_graph.setOnAction(event -> {
            graph.graphs.clear();
            graph.toggles.getChildren().clear();
        });

        Button resetGraphMinTime = new Button("Reset Min Time");
        resetGraphMinTime.setOnAction(event -> {
            graph.minTime = System.currentTimeMillis() / 1000;
            graph.maxTime = graph.minTime;
        });

        content.getChildren().addAll(graph, clear_graph, resetGraphMinTime, compass);

        for(String param : subsystem.parameterTable.getKeys()) {
            content.getChildren().add(new ParameterTextbox(subsystem.parameterTable.getEntry(param)));
        }

        Main.redrawCallbacks.add(this::onJavafxLoop);

        startTime = System.currentTimeMillis();
    }

    public void onJavafxLoop() {
        double time = (System.currentTimeMillis() - startTime) / 1000.0f;

        for(String key : subsystem.stateTable.getKeys()) {
            if(key.endsWith("_Value")) {
                String state = key.replace("_Value", "");
                Units unit = Units.valueOf(subsystem.stateTable.getEntry(state + "_Unit").getString(""));
                double value = subsystem.stateTable.getEntry(state + "_Value").getDouble(0);

                if (unit == Units.Degrees) {
                    if(state.equals("Angle")){
                        compass.setAngle(value);
                    }
                } else {
                    graph.addData(state, unit, value, time);
                }
            }
        }

        graph.draw();
        System.out.println("Sampled at " + time);
    }
}
