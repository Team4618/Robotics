package team4618.dashboard.pages;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import team4618.dashboard.Main;
import team4618.dashboard.autonomous.DriveCurve;
import team4618.dashboard.autonomous.DriveCurve.DifferentialTrajectory;
import team4618.dashboard.components.FieldTopdown;
import team4618.dashboard.components.MultiLineGraph;
import team4618.dashboard.pages.HomePage.RobotPosition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import static team4618.dashboard.components.MultiLineGraph.Units.*;

public class RobotPage extends DashboardPage {
    MultiLineGraph testGraph = new MultiLineGraph();
    HBox buttons = new HBox();
    Button showRecordedProfile = new Button("Show Recorded Profile");
    Button recalculateProfile = new Button("Recalculate Profile");
    Button simulateProfile = new Button("Simulate");
    Slider pidJitter = new Slider(0.0, 10.0, 1.5);
    VBox node = new VBox();
    FieldTopdown simulationField = new FieldTopdown();

    public DriveCurve.DifferentialProfile getProfile() {
        DriveCurve.DifferentialProfile profile = new DriveCurve.DifferentialProfile(new DifferentialTrajectory[0], 0.1);

        if(AutonomousPage.selected instanceof DriveCurve) {
            DriveCurve driveCurve = (DriveCurve) AutonomousPage.selected;
            DriveCurve.SegmentedPath segmentedPath = driveCurve.generateSegmentedPath();
            profile = segmentedPath.buildProfile(driveCurve.accelTime, driveCurve.deccelTime, driveCurve.speed, driveCurve.backwards);
        }

        return profile;
    }

    public RobotPage() {
        testGraph.prefWidthProperty().bind(node.widthProperty());
        buttons.getChildren().addAll(recalculateProfile, simulateProfile, showRecordedProfile, pidJitter);
        node.getChildren().addAll(testGraph, buttons, simulationField);
        simulationField.vboxSizing(node);

        recalculateProfile.setOnAction(evt -> {
            testGraph.graphs.clear();
            testGraph.toggles.getChildren().clear();

            /*
            double distance = 0;
            for(double t = 0; t < DriveCurve.profileTime(2, 2, 30, 5); t += 0.1) {
                double speed = DriveCurve.trapazoidalProfile(t, 2, 2, 30, 5);
                distance += speed * 0.1;
                testGraph.addData("Trapezoid", FeetPerSecond, speed, t);
                testGraph.addData("Trapezoid Pos", Feet, distance, t);
            }
            */

            Arrays.asList(getProfile().profile).forEach(traj -> {
                testGraph.addData("Angle", Degrees, traj.angle, traj.t);
                testGraph.addData("Left Velocity", FeetPerSecond, traj.vl, traj.t);
                testGraph.addData("Left Position", Feet, traj.pl, traj.t);
                testGraph.addData("Right Velocity", FeetPerSecond, traj.vr, traj.t);
                testGraph.addData("Right Position", Feet, traj.pr, traj.t);
                testGraph.addData("Speed", FeetPerSecond, (traj.vl + traj.vr) / 2, traj.t);
            });
        });

        showRecordedProfile.setOnAction(evt -> {
            testGraph.graphs.clear();
            testGraph.toggles.getChildren().clear();

            AutonomousPage.recordedProfile.forEach(traj -> {
                //TODO: rewrite or remove the recording stuff
                testGraph.addData("Angle", MultiLineGraph.Units.Degrees, traj.angle, traj.t);
                testGraph.addData("Left", FeetPerSecond, traj.vl, traj.t);
                testGraph.addData("Right", FeetPerSecond, traj.vr, traj.t);
                testGraph.addData("Speed", FeetPerSecond, (traj.vl + traj.vr) / 2, traj.t);
            });
        });

        simulateProfile.setOnAction(evt -> {
            DriveCurve.DifferentialProfile curveProfile = getProfile();

            simulationField.overlay.clear();
            double elapsedTime = 0;
            double dt = 0.005;
            double pl = 0;
            double pr = 0;
            Random rng = new Random();

            RobotPosition pos = new RobotPosition(AutonomousPage.startingPos.x, AutonomousPage.startingPos.y, 0, 0);
            if(AutonomousPage.selected instanceof DriveCurve)
                pos = new RobotPosition(((DriveCurve) AutonomousPage.selected).beginning.x, ((DriveCurve) AutonomousPage.selected).beginning.y, 0, 0);
            simulationField.overlay.add(pos);

            while(elapsedTime <= curveProfile.length()) {
                DifferentialTrajectory currTraj = curveProfile.getTrajectoryAt(elapsedTime);

                double rSpeed = currTraj.vr + pidJitter.getValue() * (rng.nextBoolean() ? -1 : 1) * rng.nextDouble();
                double lSpeed = currTraj.vl + pidJitter.getValue() * (rng.nextBoolean() ? -1 : 1) * rng.nextDouble();
                pr += rSpeed * dt;
                pl += lSpeed * dt;

                testGraph.addData("Sim - Right Speed", FeetPerSecond, rSpeed, elapsedTime);
                testGraph.addData("Sim - Left Speed", FeetPerSecond, lSpeed, elapsedTime);
                testGraph.addData("Sim - Right Position", MultiLineGraph.Units.Feet, pr, elapsedTime);
                testGraph.addData("Sim - Left Position", MultiLineGraph.Units.Feet, pl, elapsedTime);

                double speed = 12 * (rSpeed + lSpeed) / 2;
                double dtheta = (rSpeed - lSpeed) / (26.5 / 12.0);
                //TODO: we should start doing everything in radians, it makes stuff like this easier
                double angle = dt * dtheta + Math.toRadians(pos.angle);
                pos = new RobotPosition(pos.x + speed * dt * Math.cos(angle),
                                        pos.y + speed * dt * Math.sin(angle), elapsedTime, Math.toDegrees(angle));
                simulationField.overlay.add(pos);

                elapsedTime += dt;
            }
        });

        Main.redrawCallbacks.add(testGraph::draw);
    }

    public void setPageSelected(boolean selected) { }
    public Node getNode() { return node; }
}
