package team4618.dashboard.autonomous;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import team4618.dashboard.components.FieldTopdown;
import team4618.dashboard.pages.AutonomousPage;

import java.util.List;

public class Drive extends FieldTopdown.Drawable {
    public PathNode beginning;
    public PathNode end;
    public String conditional = "alwaysTrue";
    public Color color;
    public boolean dashed = false;

    public double turnMaxSpeed = 2;
    public double turnTimeUntilMaxSpeed = 1;
    public double angleToSlowdown = 10;

    public double driveMaxSpeed = 4;
    public double driveTimeUntilMaxSpeed = 1;
    public double distanceToSlowdown = 1;

    public Drive(PathNode b, PathNode e) {
        beginning = b;
        beginning.outPaths.add(this);
        end = e;
        e.inPath = this;
        color = Color.BLUE;
    }

    public void draw(GraphicsContext gc, FieldTopdown field) {
        gc.setStroke(this == field.hot ? Color.RED : color);
        if(AutonomousPage.selected == this) gc.setStroke(Color.LAVENDERBLUSH);
        gc.setLineWidth(2);
        gc.setLineDashes(dashed ? 6 : null);
        gc.strokeLine(beginning.x, beginning.y, end.x, end.y);
        gc.setLineDashes();
    }

    public boolean contains(double nX, double nY) {
        //TODO: make it easier to click on the line
        return new Line(beginning.x, beginning.y, end.x, end.y).contains(nX, nY);
    }

    public double getDistance() {
        double deltaX = (end.x - beginning.x);
        double deltaY = (end.y - beginning.y);
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY) / 12.0;
    }

    public void addCommandsTo(List<AutonomousCommand> commands) {
        double distance = getDistance();
        double angle = AutonomousPage.canonicalizeAngle(Math.toDegrees(Math.atan2(end.y - beginning.y, end.x - beginning.x)));

        AutonomousCommand turnCommand = new AutonomousCommand(AutonomousCommandTemplate.templates.get("Drive:turnToAngle"));
        turnCommand.parameterValues[0] = angle;
        turnCommand.parameterValues[1] = turnMaxSpeed;
        turnCommand.parameterValues[2] = turnTimeUntilMaxSpeed;
        turnCommand.parameterValues[3] = angleToSlowdown;

        AutonomousCommand driveCommand = new AutonomousCommand(AutonomousCommandTemplate.templates.get("Drive:driveDistance"));
        driveCommand.parameterValues[0] = distance;
        driveCommand.parameterValues[1] = driveMaxSpeed;
        driveCommand.parameterValues[2] = driveTimeUntilMaxSpeed;
        driveCommand.parameterValues[3] = distanceToSlowdown;

        commands.add(turnCommand);
        commands.add(driveCommand);
    }
}
