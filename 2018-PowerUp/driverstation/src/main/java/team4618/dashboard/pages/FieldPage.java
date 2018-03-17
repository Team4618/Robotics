package team4618.dashboard.pages;

import javafx.scene.Node;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import team4618.dashboard.components.FieldTopdown;
import team4618.dashboard.components.FieldTopdown.StartingPosition;

import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Optional;

public class FieldPage extends DashboardPage {
    VBox node = new VBox();
    FieldTopdown field = new FieldTopdown();

    public static HashMap<String, StartingPosition> startingPositions = new HashMap<>();
    static {
        startingPositions.put("Left", new StartingPosition(18, 51, "Left"));
        startingPositions.put("Center", new StartingPosition(18, 200, "Center"));
        startingPositions.put("Right", new StartingPosition(18, 300, "Right"));
    }
    public static Switch ourSwitch = new Switch();

    public FieldPage() {
        node.getChildren().add(field);
        field.widthProperty().bind(node.widthProperty());

        FieldTopdown.fieldObjects.addAll(startingPositions.values());
        FieldTopdown.fieldObjects.add(ourSwitch);
    }

    public void setPageSelected(boolean selected) {
        FieldTopdown.fieldObjects.forEach(o -> {
            o.interactable = selected;
            o.draggable = selected;
        });
    }

    public Node getNode() { return node; }

    public static JSONObject fieldObjectData;

    public static void initFieldObjectData() {
        fieldObjectData = new JSONObject();
        try {
            FileReader jsonFile = new FileReader("fieldObjects.json");
            fieldObjectData = (JSONObject) JSONValue.parseWithException(jsonFile);
            jsonFile.close();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void setFieldObjectData(String name, JSONObject data) {
        if(fieldObjectData == null)
            initFieldObjectData();

        fieldObjectData.put(name, data);
        //TODO: save this file periodically when data gets changed instead of saving after every change
        try {
            FileWriter jsonFile = new FileWriter("fieldObjects.json");
            fieldObjectData.writeJSONString(jsonFile);
            jsonFile.close();
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static Optional<JSONObject> getFieldObjectData(String name) {
        if(fieldObjectData == null)
            initFieldObjectData();

        return Optional.ofNullable((JSONObject) fieldObjectData.getOrDefault(name, null));
    }

    public static class Switch extends FieldTopdown.Drawable {
        double width = 55;
        double height = 153;

        public Switch() {
            getFieldObjectData("Switch").ifPresent(j -> {
                x = (double) j.get("x");
                y = (double) j.get("y");
                width = (double) j.get("width");
                height = (double) j.get("height");
            });
        }

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

        public void click(FieldTopdown field) {
            JSONObject json = new JSONObject();
            json.put("x", x);
            json.put("y", y);
            json.put("width", width);
            json.put("height", height);

            FieldPage.setFieldObjectData("Switch", json);
        }

        public void drag(double nX, double nY) {
            x = nX;
            y = nY;
        }
    }
}
