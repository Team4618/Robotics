package team4618.dashboard.pages;

import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import team4618.dashboard.Main;
import team4618.dashboard.autonomous.DriveCurve;
import team4618.dashboard.autonomous.DriveCurve.BezierCurve;
import team4618.dashboard.autonomous.DriveCurve.DifferentialTrajectory;
import team4618.dashboard.autonomous.DriveCurve.Vector;
import team4618.dashboard.components.FieldTopdown;
import team4618.dashboard.components.MultiLineGraph;
import team4618.dashboard.pages.HomePage.RobotPosition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RobotPage extends DashboardPage {
    MultiLineGraph testGraph = new MultiLineGraph();
    HBox buttons = new HBox();
    Button showRecordedProfile = new Button("Show Recorded Profile");
    Button recalculateProfile = new Button("Recalculate Profile");
    Button simulateProfile = new Button("Simulate");
    VBox node = new VBox();
    FieldTopdown simulationField = new FieldTopdown();

    public RobotPage() {
        testGraph.prefWidthProperty().bind(node.widthProperty());
        buttons.getChildren().addAll(recalculateProfile, simulateProfile, showRecordedProfile);
        node.getChildren().addAll(testGraph, buttons, simulationField);
        simulationField.vboxSizing(node);

        recalculateProfile.setOnAction(evt -> {
            testGraph.graphs.clear();
            testGraph.toggles.getChildren().clear();

            if(AutonomousPage.selected instanceof DriveCurve) {
                DriveCurve driveCurve = (DriveCurve) AutonomousPage.selected;
                DriveCurve.SegmentedPath segmentedPath = driveCurve.generateSegmentedPath();
                ArrayList<DifferentialTrajectory> curveProfile = segmentedPath.buildProfile(5);

                curveProfile.forEach(traj -> {
                    testGraph.addData("Angle", MultiLineGraph.Units.Degrees, traj.angle, traj.t);
                    testGraph.addData("Left", MultiLineGraph.Units.FeetPerSecond, traj.l, traj.t);
                    testGraph.addData("Right", MultiLineGraph.Units.FeetPerSecond, traj.r, traj.t);
                    testGraph.addData("Speed", MultiLineGraph.Units.FeetPerSecond, (traj.l + traj.r) / 2, traj.t);
                });
            }
        });

        showRecordedProfile.setOnAction(evt -> {
            testGraph.graphs.clear();
            testGraph.toggles.getChildren().clear();

            AutonomousPage.recordedProfile.forEach(traj -> {
                testGraph.addData("Angle", MultiLineGraph.Units.Degrees, traj.angle, traj.t);
                testGraph.addData("Left", MultiLineGraph.Units.FeetPerSecond, traj.l, traj.t);
                testGraph.addData("Right", MultiLineGraph.Units.FeetPerSecond, traj.r, traj.t);
                testGraph.addData("Speed", MultiLineGraph.Units.FeetPerSecond, (traj.l + traj.r) / 2, traj.t);
            });
        });

        simulateProfile.setOnAction(evt -> {
            if(AutonomousPage.selected instanceof DriveCurve) {
                DriveCurve driveCurve = (DriveCurve) AutonomousPage.selected;
                DriveCurve.SegmentedPath segmentedPath = driveCurve.generateSegmentedPath();
                ArrayList<DifferentialTrajectory> curveProfile = segmentedPath.buildProfile(5);

                simulationField.overlay.clear();
                int curvei = 0;
                double elapsedTime = 0;
                double dt = 0.0001;
                RobotPosition pos = new RobotPosition(FieldPage.startingPositions.get("Right").x, FieldPage.startingPositions.get("Right").y, 0, 0);
                simulationField.overlay.add(pos);

                while(curvei < curveProfile.size()) {
                    DifferentialTrajectory currTraj = curveProfile.get(curvei);

                    double speed = (currTraj.r + currTraj.l) / 2;
                    double dtheta = (currTraj.r - currTraj.l) / (26.5 / 12.0);
                    double angle = dt * dtheta + pos.angle;
                    pos = new RobotPosition(pos.x + speed * dt * Math.cos(Math.toRadians(angle)),
                                            pos.y + speed * dt * Math.sin(Math.toRadians(angle)), elapsedTime, angle);
                    simulationField.overlay.add(pos);

                    while ((curvei < curveProfile.size()) && (elapsedTime > curveProfile.get(curvei).t)) {
                        curvei++;
                    }
                    elapsedTime += dt;
                }
            }
        });

        Main.redrawCallbacks.add(testGraph::draw);
    }

    public void setPageSelected(boolean selected) { }
    public Node getNode() { return node; }
}
