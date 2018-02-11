package team4618.dashboard.pages;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import team4618.dashboard.Main;
import team4618.dashboard.components.FieldTopdown;

public class HomePage extends VBox {
    FieldTopdown liveFieldView;
    VBox currentlyExecuting;

    RobotPosition currentPosition;

    public HomePage() {
        this.setAlignment(Pos.TOP_CENTER);

        liveFieldView = new FieldTopdown();
        liveFieldView.vboxSizing(this);
        Main.redrawCallbacks.add(this::updateLiveView);
        this.getChildren().add(liveFieldView);

        currentlyExecuting = new VBox();
        currentlyExecuting.setPadding(new Insets(10));
        currentlyExecuting.setBackground(new Background(new BackgroundFill(Color.color(0, 0, 0, 0.5), new CornerRadii(10), new Insets(5))));
        Main.redrawCallbacks.add(this::rebuildCurrentlyExecuting);
        this.getChildren().add(currentlyExecuting);
    }

    public class RobotPosition extends FieldTopdown.Drawable {
        //TODO: track time
        double time;

        public RobotPosition(double x, double y) {
            this.x = x;
            this.y = y;
            currentPosition = this;
            liveFieldView.overlay.add(this);
        }

        public void draw(GraphicsContext gc) { gc.fillOval(-3, -3, liveFieldView.getPixelPerInch() * 6, liveFieldView.getPixelPerInch() * 6); }
        public boolean contains(double x, double y) { return false; }
    }

    public void updateLiveView() {
        //TODO: implement this
        new RobotPosition(0, 0);
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
