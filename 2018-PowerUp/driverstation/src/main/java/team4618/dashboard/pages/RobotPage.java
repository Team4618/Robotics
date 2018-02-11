package team4618.dashboard.pages;

import javafx.scene.layout.VBox;
import team4618.dashboard.components.MultiLineGraph;

public class RobotPage extends VBox {

    public RobotPage() {
        MultiLineGraph testGraph = new MultiLineGraph();
        testGraph.prefWidthProperty().bind(this.widthProperty());
        this.getChildren().add(testGraph);

        for(int i = 0; i < 110; i++) {
            testGraph.addData("Test1", MultiLineGraph.Units.Feet, i * Math.sin(i), i*10);
        }

        for(int i = 0; i < 100; i++) {
            testGraph.addData("Test2", MultiLineGraph.Units.Unitless, 0.7 * Math.sin(i), i*10);
        }

        for(int i = 0; i < 10; i++) {
            testGraph.addData("Test3", MultiLineGraph.Units.Degrees, i * 10, 1000);
        }
    }
}
