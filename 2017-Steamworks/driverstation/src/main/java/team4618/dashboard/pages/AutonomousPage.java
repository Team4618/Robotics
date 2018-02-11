package team4618.dashboard.pages;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import team4618.dashboard.components.FieldTopdown;

import java.util.ArrayList;

public class AutonomousPage extends ScrollPane {
    VBox content = new VBox();
    HBox buttons = new HBox();
    FieldTopdown pathDrawer = new FieldTopdown(this::onClick);
    VBox editor = new VBox();

    public AutonomousPage() {
        Button upload = new Button("Upload");
        Button download = new Button("Download");
        Button openFile = new Button("Open File");
        Button saveFile = new Button("Save File");
        buttons.getChildren().addAll(upload, download, openFile, saveFile);
        buttons.setBackground(new Background(new BackgroundFill(Color.BLACK, new CornerRadii(0), new Insets(0))));
        content.getChildren().add(buttons);

        pathDrawer.overlay.add(new StartingPosition(12, 40));
        pathDrawer.overlay.add(new StartingPosition(12, 70));
        pathDrawer.overlay.add(new StartingPosition(12, 100));
        pathDrawer.vboxSizing(content);
        editor.prefWidthProperty().bind(content.widthProperty());
        content.getChildren().addAll(pathDrawer, editor);

        content.prefWidthProperty().bind(this.widthProperty());
        content.setAlignment(Pos.TOP_CENTER);
        this.setContent(content);
    }

    public PathNode selected;
    public PathNode startingNode;

    public void deleteNodeOuts(PathNode node) {
        for(Drive outPath : node.outPaths) {
            deleteNodeOuts(outPath.end);
            pathDrawer.overlay.remove(outPath);
        }
        pathDrawer.overlay.remove(node);
    }

    public static ArrayList<AutonomousCommandTemplate> commandTemplates = new ArrayList<>();
    static {
        AutonomousCommandTemplate elevGoToHeight = new AutonomousCommandTemplate();
        elevGoToHeight.commandName = "goToHeight";
        elevGoToHeight.subsystemName = "Elevator";
        elevGoToHeight.parameterNames = new String[] {"height"};
        elevGoToHeight.parameterUnits = new String[] {"Feet"};
        commandTemplates.add(elevGoToHeight);

        AutonomousCommandTemplate intakeIn = new AutonomousCommandTemplate();
        intakeIn.commandName = "in";
        intakeIn.subsystemName = "Intake";
        intakeIn.parameterNames = new String[] {};
        intakeIn.parameterUnits = new String[] {};
        commandTemplates.add(intakeIn);

        AutonomousCommandTemplate intakeOut = new AutonomousCommandTemplate();
        intakeOut.commandName = "out";
        intakeOut.subsystemName = "Intake";
        intakeOut.parameterNames = new String[] {};
        intakeOut.parameterUnits = new String[] {};
        commandTemplates.add(intakeOut);
    }

    public void setSelected(PathNode node) {
        selected = node;
        editor.getChildren().clear();

        if(node != null) {
            FlowPane addCommand = new FlowPane();
            addCommand.setPadding(new Insets(10));
            addCommand.setBackground(new Background(new BackgroundFill(Color.color(0, 0, 0, 0.5), new CornerRadii(10), new Insets(5))));
            addCommand.prefWidthProperty().bind(editor.widthProperty());

            for(AutonomousCommandTemplate commandTemplate : commandTemplates) {
                addCommand.getChildren().add(new Button(commandTemplate.subsystemName + " -> " + commandTemplate.commandName));
            }

            //TODO: add command blocks, update them whenever the commands array is changed
            {
                HBox command = new HBox();
                command.setPadding(new Insets(10));
                command.setBackground(new Background(new BackgroundFill(Color.color(0, 0, 0, 0.5), new CornerRadii(10), new Insets(5))));
                command.prefWidthProperty().bind(editor.widthProperty());
                command.getChildren().add(new Label("¯\\_(ツ)_/¯"));
            }

            Button delete = new Button("Delete");
            delete.setOnAction(evt -> {
                deleteNodeOuts(node);
                if (node.inPath != null) {
                    pathDrawer.overlay.remove(node.inPath);
                }
                if (node == startingNode) {
                    startingNode = null;
                }
                if (node == selected) {
                    setSelected(null);
                }
                pathDrawer.overlay.remove(node);
            });

            editor.getChildren().addAll(delete, /*command,*/ addCommand);
        }
    }

    public void onClick(double x, double y) {
        if(selected != null) {
            PathNode newTurn = new PathNode(x, y);
            Drive newDrive = new Drive(selected, newTurn);
            pathDrawer.overlay.add(newDrive);
            pathDrawer.overlay.add(newTurn);
            setSelected(newTurn);
        }
    }

    public static class AutonomousCommandTemplate {
        String subsystemName;
        String commandName;
        String[] parameterUnits;
        String[] parameterNames;
        String formatString;
    }

    public static class AutonomousCommand {
        AutonomousCommandTemplate template;
        double[] parameterValues;
    }

    public class PathNode extends FieldTopdown.Drawable {
        public ArrayList<Drive> outPaths = new ArrayList<>();
        public Drive inPath;
        public ArrayList<AutonomousCommand> commands = new ArrayList<>();

        public PathNode(double nX, double nY) { x = nX; y = nY; }

        public void draw(GraphicsContext gc) {
            gc.setFill(this == pathDrawer.hot ? Color.RED : Color.GREEN);
            if(selected == this) gc.setFill(Color.LAVENDERBLUSH);
            gc.fillOval(-5, -5, pathDrawer.getPixelPerInch() * 10, pathDrawer.getPixelPerInch() * 10);
        }

        public boolean contains(double nX, double nY) {
            return Math.sqrt((x - nX) * (x - nX) + (y - nY) * (y - nY)) < 10;
        }

        public void drag(double nX, double nY) {
            x = nX;
            y = nY;
        }

        public void click() {
            setSelected(this);
        }
    }

    public class Drive extends FieldTopdown.Drawable {
        PathNode beginning;
        PathNode end;

        public Drive(PathNode b, PathNode e) {
            beginning = b;
            beginning.outPaths.add(this);
            end = e;
            e.inPath = this;
        }

        public void draw(GraphicsContext gc) {
            gc.setStroke(Color.BLUE);
            gc.setLineWidth(4);
            gc.strokeLine(beginning.x, beginning.y, end.x, end.y);
        }

        public boolean contains(double nX, double nY) {
            return false; //TODO: this
        }
    }

    public class StartingPosition extends FieldTopdown.Drawable {
        public StartingPosition(double nX, double nY) { x = nX; y = nY; }

        public void draw(GraphicsContext gc) {
            gc.setStroke(this == pathDrawer.hot ? Color.RED : Color.GREEN);
            gc.setLineWidth(2);
            gc.strokeOval(-8, -8, pathDrawer.getPixelPerInch() * 16, pathDrawer.getPixelPerInch() * 16);
        }

        public boolean contains(double nX, double nY) {
            return Math.sqrt((x - nX) * (x - nX) + (y - nY) * (y - nY)) < 10;
        }

        public void click() {
            PathNode newStartingNode = new PathNode(x, y);
            pathDrawer.overlay.add(newStartingNode);

            if(startingNode != null) {
                newStartingNode.outPaths = startingNode.outPaths;
                for(Drive outPath : newStartingNode.outPaths) {
                    outPath.beginning = newStartingNode;
                }
                pathDrawer.overlay.remove(startingNode);
            }
            startingNode = newStartingNode;
            setSelected(startingNode);
        }
    }
}
