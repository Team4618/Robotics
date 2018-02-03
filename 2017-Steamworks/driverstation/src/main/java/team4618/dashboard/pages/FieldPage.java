package team4618.dashboard.pages;

import javafx.scene.layout.VBox;
import team4618.dashboard.components.FieldTopdown;

public class FieldPage extends VBox implements FieldTopdown.FieldOverlayProvider {
    public FieldTopdown field = new FieldTopdown(this);

    //TODO: edit field parameters
    public FieldPage() {
        field.widthProperty().bind(this.widthProperty());
        this.getChildren().add(field);
    }

    public void drawOverlay() {
        field.gc.setLineWidth(0.1);
        field.gc.fillText("DistanceToTop: " + field.distanceToTop, 0, 40);
    }

    public void onDrag(double x, double y) {

    }

    public void onMouseRelease(double x, double y) {

    }

    public void onMousePressed(double x, double y) {

    }
}
