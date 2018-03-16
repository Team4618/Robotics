package team4618.dashboard.autonomous;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import team4618.dashboard.components.FieldTopdown;
import team4618.dashboard.pages.AutonomousPage;

import java.util.List;

public class Drive extends FieldTopdown.Drawable {
    public PathNode beginning;
    public PathNode end;
    public String conditional = "alwaysTrue";
    public Color color;
    public boolean dashed = false;
    public boolean visible = true;

    public double turnMaxSpeed = 2;
    public double turnTimeUntilMaxSpeed = 1;
    public double angleToSlowdown = 10;

    public double driveMaxSpeed = 4;
    public double driveTimeUntilMaxSpeed = 1;
    public double distanceToSlowdown = 1;

    public boolean backwards = false;

    public Drive(PathNode b, PathNode e) {
        beginning = b;
        beginning.outPaths.add(this);
        end = e;
        e.inPath = this;
        color = Color.BLUE;
    }

    //TODO: draw backward drives in a different colour
    public void draw(GraphicsContext gc, FieldTopdown field) {
        if(visible) {
            gc.setStroke(this == field.hot ? Color.RED : color);
            if (AutonomousPage.selected == this) gc.setStroke(Color.LAVENDERBLUSH);
            gc.setLineWidth(2);
            gc.setLineDashes(dashed ? 6 : 0);
            gc.strokeLine(beginning.x, beginning.y, end.x, end.y);
            gc.setLineDashes();
        }
    }

    public static double distancePointToLine(double x1, double y1, double x2, double y2, double x3, double y3) {
        double px = x2 - x1;
        double py = y2 - y1;
        double temp = (px * px) + (py * py);
        double u= ((x3 - x1) * px + (y3 - y1) * py) / (temp);
        if(u > 1) {
            u = 1;
        } else if(u < 0) {
            u = 0;
        }
        double x = x1 + u * px;
        double y = y1 + u * py;

        double dx = x - x3;
        double dy = y - y3;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public boolean contains(double nX, double nY) {
        return (distancePointToLine(beginning.x, beginning.y, end.x, end.y, nX, nY) < 2) && visible;
    }

    public double getDistance() {
        double deltaX = (end.x - beginning.x);
        double deltaY = (end.y - beginning.y);
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY) / 12.0;
    }

    public void addCommandsTo(List<AutonomousCommand> commands) {
        double distance = getDistance();
        double angle = Math.toDegrees(Math.atan2(end.y - beginning.y, end.x - beginning.x));

        AutonomousCommand turnCommand = new AutonomousCommand(AutonomousCommandTemplate.templates.get("Drive:turnToAngle"));
        turnCommand.parameterValues[0] = AutonomousPage.canonicalizeAngle(angle + (backwards ? 180 : 0));
        turnCommand.parameterValues[1] = turnMaxSpeed;
        turnCommand.parameterValues[2] = turnTimeUntilMaxSpeed;
        turnCommand.parameterValues[3] = angleToSlowdown;

        AutonomousCommand driveCommand = new AutonomousCommand(AutonomousCommandTemplate.templates.get("Drive:driveDistance"));
        driveCommand.parameterValues[0] = (backwards ? -1 : 1) * distance;
        driveCommand.parameterValues[1] = driveMaxSpeed;
        driveCommand.parameterValues[2] = driveTimeUntilMaxSpeed;
        driveCommand.parameterValues[3] = distanceToSlowdown;

        commands.add(turnCommand);
        commands.add(driveCommand);
    }
}
