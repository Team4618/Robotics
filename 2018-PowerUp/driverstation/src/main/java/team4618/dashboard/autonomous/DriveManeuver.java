package team4618.dashboard.autonomous;

import javafx.scene.paint.Color;
import team4618.dashboard.components.FieldTopdown;

import java.util.List;

public abstract class DriveManeuver extends FieldTopdown.Drawable {
    public PathNode beginning;
    public PathNode end;
    public String conditional = "alwaysTrue";
    public Color color;
    public boolean dashed = false;
    public boolean visible = true;

    public boolean backwards = false;

    public void remove(List<FieldTopdown.Drawable> overlay) {
        overlay.remove(this);
    }

    public abstract void addCommandsTo(List<AutonomousCommand> commands);
}
