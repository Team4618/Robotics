package team4618.dashboard.pages;

import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
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
    ToggleButton manualProfile = new ToggleButton("Use manual profile");
    VBox node = new VBox();
    FieldTopdown simulationField = new FieldTopdown();

    public ArrayList<DifferentialTrajectory> getProfile() {
        ArrayList<DifferentialTrajectory> profile = new ArrayList<>();

        if(manualProfile.isSelected() ) {
            profile.add(new DifferentialTrajectory(2, 2, 2, 0));
            profile.add(new DifferentialTrajectory(7, 1.25665 - 0.5 * 0.6943, 1.25665 + 0.5 * 0.6943, 0));
            profile.add(new DifferentialTrajectory(9, 2, 2, 90));
            profile.add(new DifferentialTrajectory(9.1, 0, 0, 90));
        } else if(AutonomousPage.selected instanceof DriveCurve) {
            DriveCurve driveCurve = (DriveCurve) AutonomousPage.selected;
            DriveCurve.SegmentedPath segmentedPath = driveCurve.generateSegmentedPath();
            profile = segmentedPath.buildProfile(5);
        }

        return profile;
    }

    public RobotPage() {
        testGraph.prefWidthProperty().bind(node.widthProperty());
        buttons.getChildren().addAll(recalculateProfile, simulateProfile, manualProfile, showRecordedProfile);
        node.getChildren().addAll(testGraph, buttons, simulationField);
        simulationField.vboxSizing(node);

        recalculateProfile.setOnAction(evt -> {
            testGraph.graphs.clear();
            testGraph.toggles.getChildren().clear();

            getProfile().forEach(traj -> {
                testGraph.addData("Angle", MultiLineGraph.Units.Degrees, traj.angle, traj.t);
                testGraph.addData("Left", MultiLineGraph.Units.FeetPerSecond, traj.l, traj.t);
                testGraph.addData("Right", MultiLineGraph.Units.FeetPerSecond, traj.r, traj.t);
                testGraph.addData("Speed", MultiLineGraph.Units.FeetPerSecond, (traj.l + traj.r) / 2, traj.t);
            });
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
            ArrayList<DifferentialTrajectory> curveProfile = getProfile();

            simulationField.overlay.clear();
            int curvei = 0;
            double elapsedTime = 0;
            double dt = 0.005;
            RobotPosition pos = new RobotPosition(AutonomousPage.startingPos.x, AutonomousPage.startingPos.y, 0, 0);
            simulationField.overlay.add(pos);

            while(curvei < curveProfile.size()) {
                DifferentialTrajectory currTraj = curveProfile.get(curvei);

                double speed = 12 * (currTraj.r + currTraj.l) / 2;
                double dtheta = (currTraj.r - currTraj.l) / (26.5 / 12.0);
                //TODO: we should start doing everything in radians, it makes stuff like this easier
                double angle = dt * dtheta + Math.toRadians(pos.angle);
                System.out.println("theta = " + angle + " dtheta = " + dt * dtheta + " distance = " + speed * dt);
                pos = new RobotPosition(pos.x + speed * dt * Math.cos(angle),
                                        pos.y + speed * dt * Math.sin(angle), elapsedTime, Math.toDegrees(angle));
                simulationField.overlay.add(pos);

                while ((curvei < curveProfile.size()) && (elapsedTime > curveProfile.get(curvei).t)) {
                    curvei++;
                }
                elapsedTime += dt;
            }
        });

        Main.redrawCallbacks.add(testGraph::draw);
    }

    public void setPageSelected(boolean selected) { }
    public Node getNode() { return node; }
}
