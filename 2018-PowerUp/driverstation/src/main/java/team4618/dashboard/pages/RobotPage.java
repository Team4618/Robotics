package team4618.dashboard.pages;

import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Slider;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import team4618.dashboard.Main;
import team4618.dashboard.autonomous.DriveCurve;
import team4618.dashboard.autonomous.DriveCurve.BezierCurve;
import team4618.dashboard.autonomous.DriveCurve.DifferentialTrajectory;
import team4618.dashboard.autonomous.DriveCurve.Vector;
import team4618.dashboard.components.MultiLineGraph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RobotPage extends DashboardPage {
    MultiLineGraph testGraph = new MultiLineGraph();
    Slider tSlider = new Slider(0.0, 1.0, 0.5);
    Slider timeSlider = new Slider(1.0, 20.0, 1.0);
    VBox node = new VBox();

    public RobotPage() {
        testGraph.prefWidthProperty().bind(node.widthProperty());
        node.getChildren().addAll(testGraph, tSlider, timeSlider);

        timeSlider.valueProperty().addListener(l -> {
            testGraph.graphs.clear();
            testGraph.toggles.getChildren().clear();

            if(AutonomousPage.selected instanceof DriveCurve) {
                DriveCurve driveCurve = (DriveCurve) AutonomousPage.selected;
                BezierCurve curve = driveCurve.generateCurve();
                ArrayList<DifferentialTrajectory> curveProfile = curve.buildProfile(timeSlider.getValue());

                curveProfile.forEach(traj -> {
                    testGraph.addData("Angle", MultiLineGraph.Units.Degrees, traj.angle, traj.t);
                    testGraph.addData("Left", MultiLineGraph.Units.FeetPerSecond, traj.l, traj.t);
                    testGraph.addData("Right", MultiLineGraph.Units.FeetPerSecond, traj.r, traj.t);
                });
            }
        });

        Main.redrawCallbacks.add(testGraph::draw);
    }

    public void setPageSelected(boolean selected) { }
    public Node getNode() { return node; }
}
