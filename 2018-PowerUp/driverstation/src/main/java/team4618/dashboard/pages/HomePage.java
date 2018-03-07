package team4618.dashboard.pages;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import team4618.dashboard.Main;
import team4618.dashboard.components.FieldTopdown;

public class HomePage extends DashboardPage implements FieldTopdown.OnClick {
    VBox node = new VBox();
    FieldTopdown liveFieldView =  new FieldTopdown(this);
    VBox currentlyExecuting = new VBox();

    RobotPosition currentPosition;

    public HomePage() {
        node.setAlignment(Pos.TOP_CENTER);

        liveFieldView.vboxSizing(node);
        Main.redrawCallbacks.add(this::updateLiveView);
        node.getChildren().add(liveFieldView);

        currentlyExecuting.setPadding(new Insets(10));
        currentlyExecuting.setBackground(new Background(new BackgroundFill(Color.color(0, 0, 0, 0.5), new CornerRadii(10), new Insets(5))));
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

    public void onClickStartingLocation(FieldTopdown.StartingPosition pos) {
        liveFieldView.overlay.clear();
        currentPosition = new RobotPosition(pos.x, pos.y, System.currentTimeMillis() / 1000.0);
        //TODO: bring up "select auto" menu
    }

    public class RobotPosition extends FieldTopdown.Drawable {
        double time;

        public RobotPosition(double x, double y, double time) {
            this.x = x;
            this.y = y;
            this.time = time;
            currentPosition = this;
            liveFieldView.overlay.add(this);
        }

        public void draw(GraphicsContext gc, FieldTopdown field) {
            gc.setFill((this == currentPosition) ? Color.AZURE : Color.BLUEVIOLET);
            gc.fillOval(-3, -3, field.getPixelPerInch() * 6, field.getPixelPerInch() * 6);
        }

        public boolean contains(double x, double y) { return false; }
    }

    public void updateLiveView() {
        if(currentPosition != null) {
            double speed = 12 * Main.subsystems.get("Drive").stateTable.getEntry("Speed_Value").getDouble(0);
            double angle = Main.subsystems.get("Drive").stateTable.getEntry("Angle_Value").getDouble(0);
            double time = System.currentTimeMillis() / 1000.0;
            double deltat = time - currentPosition.time;
            System.out.println("Speed " + speed + " Angle " + angle);
            //System.out.println("Cos(" + angle + ")=" + Math.cos(Math.toRadians(angle)) + " Sin(" + angle + ")=" + Math.sin(Math.toRadians(angle)));
            new RobotPosition(currentPosition.x + speed * deltat * Math.cos(Math.toRadians(angle)),
                              currentPosition.y + speed * deltat * Math.sin(Math.toRadians(angle)), time);
        }
    }

    public void rebuildCurrentlyExecuting() {
        currentlyExecuting.getChildren().clear();

        if(Main.currentlyExecutingTable.containsKey("Command Name") && Main.currentlyExecutingTable.containsKey("Subsystem Name")) {
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
