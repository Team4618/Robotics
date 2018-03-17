package team4618.dashboard.pages;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import team4618.dashboard.Main;
import team4618.dashboard.components.Compass;
import team4618.dashboard.components.MultiLineGraph;
import team4618.dashboard.components.ParameterTextbox;
import static team4618.dashboard.components.MultiLineGraph.*;

public class SubsystemPage extends DashboardPage {
    ScrollPane node = new ScrollPane();
    VBox content = new VBox();

    MultiLineGraph graph = new MultiLineGraph();
    Compass compass = new Compass();

    Main.Subsystem subsystem;

    public SubsystemPage(Main.Subsystem inSubsystem) {
        subsystem = inSubsystem;
        node.setContent(content);

        content.getChildren().addAll(graph, compass);

        for(String param : subsystem.parameterTable.getKeys()) {
            content.getChildren().add(new ParameterTextbox(subsystem.parameterTable.getEntry(param)));
        }

        Main.redrawCallbacks.add(this::onJavafxLoop);
    }

    public void onJavafxLoop() {
        double time = (System.currentTimeMillis() - graph.startTime) / 1000.0;

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
                    //graph.addData(state, unit, value, time);
                }
            }
        }

        //graph.draw();
    }

    public void setPageSelected(boolean selected) { }
    public Node getNode() { return node; }
}
