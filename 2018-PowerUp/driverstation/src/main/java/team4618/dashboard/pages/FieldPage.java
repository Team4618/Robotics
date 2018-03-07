package team4618.dashboard.pages;

import javafx.scene.Node;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import team4618.dashboard.components.FieldTopdown;
import team4618.dashboard.components.FieldTopdown.StartingPosition;

public class FieldPage extends DashboardPage {
    VBox node = new VBox();
    FieldTopdown field = new FieldTopdown();

    public FieldPage() {
        node.getChildren().add(field);
        field.widthProperty().bind(node.widthProperty());

        FieldTopdown.fieldObjects.add(new StartingPosition(18, 51));
        FieldTopdown.fieldObjects.add(new StartingPosition(18, 100));
        FieldTopdown.fieldObjects.add(new StartingPosition(18, 150));

        FieldTopdown.fieldObjects.add(new Switch());
    }

    public void setPageSelected(boolean selected) {
        FieldTopdown.fieldObjects.forEach(o -> {
            o.interactable = selected;
            o.draggable = selected;
        });
    }

    public Node getNode() { return node; }

    public static class Switch extends FieldTopdown.Drawable {
        double width = 55;
        double height = 153;

        public void draw(GraphicsContext gc, FieldTopdown field) {
            gc.setStroke(Color.RED);
            gc.strokeRect(0, 0, width, height);
            if(this == field.hot) {
                gc.strokeLine(0, 0, 0, -gc.getTransform().getTy());
                gc.strokeLine(0, 0, -gc.getTransform().getTx(), 0);
            }
        }

        public boolean contains(double nX, double nY) {
            return new Rectangle(x, y, width, height).contains(nX, nY);
        }

        public void drag(double nX, double nY) {
            x = nX;
            y = nY;
        }
    }
}
