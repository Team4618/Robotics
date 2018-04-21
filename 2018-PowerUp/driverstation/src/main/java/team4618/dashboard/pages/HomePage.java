package team4618.dashboard.pages;

import edu.wpi.first.networktables.EntryListenerFlags;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import team4618.dashboard.Main;
import team4618.dashboard.autonomous.AutonomousCommand;
import team4618.dashboard.autonomous.AutonomousCommandTemplate;
import team4618.dashboard.autonomous.DriveCurve;
import team4618.dashboard.autonomous.DriveCurve.DifferentialTrajectory;
import team4618.dashboard.autonomous.PathNode;
import team4618.dashboard.components.FieldTopdown;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.Iterator;

public class HomePage extends DashboardPage implements FieldTopdown.OnClick {
    VBox node = new VBox();
    public static FieldTopdown liveFieldView;
    VBox currentlyExecuting = new VBox();

    public static FieldTopdown.StartingPosition startingPos;
    public static RobotPosition currentPosition;

    public HomePage() {
        liveFieldView = new FieldTopdown(this);
        node.setAlignment(Pos.TOP_CENTER);

        liveFieldView.vboxSizing(node);
        Main.redrawCallbacks.add(this::updateLiveView);
        node.getChildren().add(liveFieldView);

        Main.logicTable.addEntryListener((table, key, entry, value, flags) -> resetAutoView(), EntryListenerFlags.kUpdate | EntryListenerFlags.kLocal | EntryListenerFlags.kNew);

        currentlyExecuting.setPadding(new Insets(10));
        currentlyExecuting.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(10), new Insets(5))));
        Main.redrawCallbacks.add(this::rebuildCurrentlyExecuting);
        node.getChildren().add(currentlyExecuting);
    }

    public void setPageSelected(boolean selected) {
        FieldTopdown.fieldObjects.forEach(o -> {
            o.interactable = o instanceof FieldTopdown.StartingPosition;
            o.draggable = false;
        });
    }
    public Node getNode() { return node; }
    public void onClick(double x, double y) { }

    public static void resetAutoView() {
        liveFieldView.overlay.removeIf(currDrawable -> !(currDrawable instanceof RobotPosition));
        AutonomousCommandTemplate.refreshCommandsAndLogic();
        if(startingPos != null) {
            PathNode startingNode = new PathNode(startingPos.x, startingPos.y);
            liveFieldView.overlay.add(startingNode);
            AutonomousPage.commandsToPath(AutonomousPage.downloadCommandsFrom("Custom Dashboard/Autonomous"), startingNode, liveFieldView);
            AutonomousPage.propagateAndDash(startingNode, true);
            liveFieldView.overlay.forEach(x -> x.interactable = false);
        }
    }

    public static void errorMessage(String title, String text) {
        Alert errorMessage = new Alert(Alert.AlertType.ERROR);
        errorMessage.setTitle(title);
        errorMessage.setContentText(text);
        errorMessage.show();
    }

    public void onClickStartingLocation(FieldTopdown.StartingPosition pos) {
        liveFieldView.overlay.clear();
        startingPos = pos;

        //TODO: instead of setting current position, send the position to the robot as it handles the position tracking
        currentPosition = new RobotPosition(pos.x, pos.y, System.currentTimeMillis() / 1000.0, 0);

        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Open Auto File");

            FileReader reader = new FileReader(fileChooser.showOpenDialog(new Stage()));
            JSONObject rootObject = (JSONObject) JSONValue.parseWithException(reader);
            reader.close();

            ArrayList<AutonomousCommand> commandList = new ArrayList<>();
            ((JSONArray) rootObject.get("Commands")).forEach(j -> commandList.add(new AutonomousCommand((JSONObject) j)));

            FieldTopdown.StartingPosition loadedStartingPos = FieldPage.startingPositions.get(rootObject.get("Starting Position"));
            if(startingPos == loadedStartingPos) {
                AutonomousPage.uploadCommands(commandList);
            } else {
                errorMessage("Selected Autonomous Not Compatible",
                        "Starting Position: " + startingPos.name + "\nLoaded Auto: " + loadedStartingPos.name);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static class RobotPosition extends FieldTopdown.Drawable {
        double time;
        double angle;

        public RobotPosition(double x, double y, double time, double angle) {
            this.x = x;
            this.y = y;
            this.time = time;
            this.angle = angle;
        }

        Rectangle rect = new Rectangle(-29 / 2, -28 / 2, 29, 28);
        public void draw(GraphicsContext gc, FieldTopdown field) {
            gc.setFill((this == currentPosition) ? Color.AZURE : Color.BLUEVIOLET);
            gc.fillOval(-3, -3, field.getPixelPerInch() * 6, field.getPixelPerInch() * 6);

            if(this == currentPosition) {
                gc.rotate(angle);
                Paint prevFill = gc.getFill();
                gc.setFill(Color.rgb(0, 0, 0, 0.5));
                gc.fillRect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
                gc.setFill(prevFill);
                gc.rotate(-angle);
            }
        }

        public boolean contains(double x, double y) { return false; }
    }

    public void updateLiveView() {
        double time = System.currentTimeMillis() / 1000.0;

        /*
        if(Main.connected) {
            double x = 12 * Main.mainTable.getEntry("x").getDouble(0);
            double y = 12 * Main.mainTable.getEntry("y").getDouble(0);
            double angle = Main.subsystems.get("Drive").stateTable.getEntry("Angle_Value").getDouble(0);

            RobotPosition newPos = new RobotPosition(x, y, time, angle);
            if (((currentPosition.x - x) * (currentPosition.x - x) + (currentPosition.y - y) * (currentPosition.y - y)) > 2) {
                currentPosition = newPos;
                liveFieldView.overlay.add(newPos);
            }
        }
*/

        if(currentPosition != null) {
            double speed = 12 * Main.subsystems.get("Drive").stateTable.getEntry("Speed_Value").getDouble(0);
            double angle = Main.subsystems.get("Drive").stateTable.getEntry("Angle_Value").getDouble(0);
            double deltat = time - currentPosition.time;
            RobotPosition newPos = new RobotPosition(currentPosition.x + speed * deltat * Math.cos(Math.toRadians(angle)),
                                                     currentPosition.y + speed * deltat * Math.sin(Math.toRadians(angle)), time, angle);
            currentPosition = newPos;
            liveFieldView.overlay.add(newPos);
        }

        if(AutonomousPage.recording) {
            double leftSpeed = Main.subsystems.get("Drive").stateTable.getEntry("Left Speed_Value").getDouble(0);
            double rightSpeed = Main.subsystems.get("Drive").stateTable.getEntry("Right Speed_Value").getDouble(0);
            boolean moving = (Math.abs(leftSpeed) > 0.1) || (Math.abs(leftSpeed) > 0.1);

            if(moving || (AutonomousPage.recordedProfile.size() > 0)) {
                //TODO: rewrite or remove
                AutonomousPage.recordedProfile.add(new DifferentialTrajectory(time - AutonomousPage.recordingStartTime, leftSpeed, 0, rightSpeed, 0, 0));

                if(moving) {
                    AutonomousPage.lastMovingTraj = AutonomousPage.recordedProfile.size();
                }
            }
        }
    }

    public void rebuildCurrentlyExecuting() {
        currentlyExecuting.getChildren().clear();

        String mode = Main.mainTable.getEntry("mode").getString("");
        if(mode.equals("Teleop")) {
            for(String key : Main.teleopTable.getKeys()) {
                String value = Main.teleopTable.getEntry(key).getString("");
                Label label;

                if(key.equals("Gear")) {
                    boolean high = value.equals("High");
                    label = new Label(high ? "High Gear" : "Low Gear");
                    label.setTextFill(high ? Color.BLUE : Color.RED);
                    label.setFont(new Font("Arial", 38));
                } else if(key.equals("Intake Spinning")) {
                    boolean spinning = value.equals("true");
                    label = new Label(spinning ? "Intake Spinning" : "Intake NOT Spinning");
                    label.setTextFill(spinning ? Color.RED : Color.BLACK);
                    label.setFont(new Font("Arial", 18));
                } else {
                    label = new Label(key + ": " + value);
                    Color c = Color.BLACK;

                    if(value.equals("Automatic") || value.equals("Enabled")) {
                        c = Color.BLUE;
                    } else if(value.equals("Manual") || value.equals("Disabled")) {
                        c = Color.RED;
                    }

                    label.setFont(new Font("Arial", 18));
                    label.setTextFill(c);
                }

                currentlyExecuting.getChildren().add(label);
            }
        } else if(mode.equals("Autonomous")) {
            if (Main.currentlyExecutingTable.containsKey("Command Name") && Main.currentlyExecutingTable.containsKey("Subsystem Name")) {
                currentlyExecuting.getChildren().add(new Label("Currently Executing: " + Main.currentlyExecutingTable.getEntry("Command Name").getString("") + " -> " + Main.currentlyExecutingTable.getEntry("Subsystem Name").getString("")));
                for (String key : Main.currentlyExecutingTable.getKeys()) {
                    if (key.endsWith("_Value")) {
                        String stateName = key.replace("_Value", "");
                        currentlyExecuting.getChildren().add(new Label(stateName + ": " + Main.currentlyExecutingTable.getEntry(stateName + "_Value").getString("") + " " + Main.currentlyExecutingTable.getEntry(stateName + "_Unit").getString("")));
                    }
                }
            }
        }
    }
}
