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

    public static AutonomousCommandTemplate driveDistance;
    public static AutonomousCommandTemplate turnToAngle;
    public static ArrayList<AutonomousCommandTemplate> commandTemplates = new ArrayList<>();
    static {
        driveDistance = new AutonomousCommandTemplate();
        driveDistance.commandName = "driveDistance";
        driveDistance.subsystemName = "Drive";
        driveDistance.parameterNames = new String[] {"distance"};
        driveDistance.parameterUnits = new String[] {"Feet"};
        commandTemplates.add(driveDistance);

        turnToAngle = new AutonomousCommandTemplate();
        turnToAngle.commandName = "turnToAngle";
        turnToAngle.subsystemName = "Drive";
        turnToAngle.parameterNames = new String[] {"angle"};
        turnToAngle.parameterUnits = new String[] {"Degrees"};
        commandTemplates.add(turnToAngle);

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

    public void addNodesCommands(ArrayList<AutonomousCommand> commands, PathNode node) {
        commands.addAll(node.commands);
        if(node.outPaths.size() == 1) {
            Drive drive = node.outPaths.get(0);
            double deltaX = (drive.end.x - drive.beginning.x);
            double deltaY = (drive.end.y - drive.beginning.y);
            double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
            double angle = Math.toDegrees(Math.atan2(deltaY, deltaX));
            AutonomousCommand driveCommand = new AutonomousCommand(driveDistance);
            driveCommand.parameterValues[0] = distance;
            AutonomousCommand turnCommand = new AutonomousCommand(turnToAngle);
            turnCommand.parameterValues[0] = angle;
            commands.add(driveCommand);
            commands.add(turnCommand);

            addNodesCommands(commands, drive.end);
        } else {
            for (Drive outDrive : node.outPaths) {

            }
        }
    }

    public ArrayList<AutonomousCommand> getCommandList() {
        ArrayList<AutonomousCommand> commands = new ArrayList<>();
        if(startingNode != null)
            addNodesCommands(commands, startingNode);

        return commands;
    }

    public void rebuildEditor() {
        editor.getChildren().clear();
        if(selected == null) {
            for(AutonomousCommand command : getCommandList()) {
                VBox commandBlock = new VBox();
                commandBlock.setPadding(new Insets(10));
                commandBlock.setBackground(new Background(new BackgroundFill(Color.color(0, 0, 0, 0.5), new CornerRadii(10), new Insets(5))));
                commandBlock.prefWidthProperty().bind(editor.widthProperty());
                commandBlock.getChildren().add(new Label(command.template.subsystemName + " -> " + command.template.commandName));

                for(int i = 0; i < command.template.parameterNames.length; i++) {
                    String paramText = command.template.parameterNames[i] + " " + command.parameterValues[i] + " " + command.template.parameterUnits[i];
                    commandBlock.getChildren().addAll(new Label(paramText));
                }

                editor.getChildren().add(commandBlock);
            }
        } else {
            HBox menu = new HBox();
            editor.getChildren().add(menu);

            Button delete = new Button("Delete");
            delete.setOnAction(evt -> {
                deleteNodeOuts(selected);
                if (selected.inPath != null) { pathDrawer.overlay.remove(selected.inPath); }
                if (selected == startingNode) { startingNode = null; }
                setSelected(null);
                pathDrawer.overlay.remove(selected);
            });
            menu.getChildren().add(delete);

            for(AutonomousCommand command : selected.commands) {
                VBox commandBlock = new VBox();
                commandBlock.setPadding(new Insets(10));
                commandBlock.setBackground(new Background(new BackgroundFill(Color.color(0, 0, 0, 0.5), new CornerRadii(10), new Insets(5))));
                commandBlock.prefWidthProperty().bind(editor.widthProperty());

                HBox titleRow = new HBox();
                Button deleteBlock = new Button("Delete");
                deleteBlock.setOnAction(evt -> {
                    selected.commands.remove(command);
                    rebuildEditor();
                });
                titleRow.getChildren().addAll(new Label(command.template.subsystemName + " -> " + command.template.commandName), deleteBlock);
                commandBlock.getChildren().add(titleRow);

                for(int i = 0; i < command.template.parameterNames.length; i++) {
                    final int index = i;
                    HBox parameterRow = new HBox();
                    TextField parameterField = new TextField(Double.toString(command.parameterValues[i]));
                    parameterField.setOnAction(event -> {
                        try {
                            command.parameterValues[index] = Double.valueOf(parameterField.getText());
                        } catch (Exception e) { }
                    });
                    parameterRow.getChildren().addAll(new Label(command.template.parameterNames[i]), parameterField, new Label(command.template.parameterUnits[i]));
                    commandBlock.getChildren().add(parameterRow);
                }

                editor.getChildren().add(commandBlock);
            }

            FlowPane addCommand = new FlowPane();
            addCommand.setPadding(new Insets(10));
            addCommand.setBackground(new Background(new BackgroundFill(Color.color(0, 0, 0, 0.5), new CornerRadii(10), new Insets(5))));
            addCommand.prefWidthProperty().bind(editor.widthProperty());

            for(AutonomousCommandTemplate commandTemplate : commandTemplates) {
                Button addCommandButton = new Button(commandTemplate.subsystemName + " -> " + commandTemplate.commandName);
                addCommandButton.setOnAction(evt -> {
                    selected.commands.add(new AutonomousCommand(commandTemplate));
                    rebuildEditor();
                });
                addCommand.getChildren().add(addCommandButton);
            }
            editor.getChildren().add(addCommand);
        }
    }

    public void setSelected(PathNode node) {
        selected = node;
        rebuildEditor();
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

        public AutonomousCommand(AutonomousCommandTemplate t) {
            template = t;
            parameterValues = new double[t.parameterNames.length];
        }
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
            rebuildEditor();
        }

        public void click() {
            if(this == selected) {
                setSelected(null);
            } else {
                setSelected(this);
            }
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
            return false;
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
