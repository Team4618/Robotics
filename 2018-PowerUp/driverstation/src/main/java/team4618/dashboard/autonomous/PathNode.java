package team4618.dashboard.autonomous;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import team4618.dashboard.components.FieldTopdown;
import team4618.dashboard.pages.AutonomousPage;

import java.util.ArrayList;

public class PathNode extends FieldTopdown.Drawable {
    public ArrayList<Drive> outPaths = new ArrayList<>();
    public Drive inPath;
    public ArrayList<AutonomousCommand> commands = new ArrayList<>();
    public boolean draggable = true;

    public PathNode(double nX, double nY) { x = nX; y = nY; }

    public void draw(GraphicsContext gc, FieldTopdown field) {
        gc.setFill(this == field.hot ? Color.RED : Color.GREEN);
        if(AutonomousPage.selected == this) gc.setFill(Color.LAVENDERBLUSH);
        gc.fillOval(-5, -5, field.getPixelPerInch() * 10, field.getPixelPerInch() * 10);
        Integer conditionsSet = 0;
        for(Drive d : outPaths) {
            if(!d.conditional.equals("alwaysTrue"))
                conditionsSet++;
        }
        if(conditionsSet + 1 < outPaths.size()) {
            gc.setStroke(Color.RED);
            gc.strokeOval(-8, -8, field.getPixelPerInch() * 16, field.getPixelPerInch() * 16);
        }
    }

    public boolean contains(double nX, double nY) {
        return Math.sqrt((x - nX) * (x - nX) + (y - nY) * (y - nY)) < 10;
    }

    public void drag(double nX, double nY) {
        if(draggable) {
            x = nX;
            y = nY;
        }
    }
}
