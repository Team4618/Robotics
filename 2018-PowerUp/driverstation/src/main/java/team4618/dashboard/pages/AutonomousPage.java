package team4618.dashboard.pages;

import edu.wpi.first.networktables.NetworkTable;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import team4618.dashboard.Main;
import team4618.dashboard.components.FieldTopdown;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class AutonomousPage extends DashboardPage implements FieldTopdown.OnClick{
    ScrollPane node = new ScrollPane();
    VBox content = new VBox();
    HBox buttons = new HBox();
    FieldTopdown pathDrawer = new FieldTopdown(this);
    VBox editor = new VBox();

    public void uploadCommandsTo(List<AutonomousCommand> commands, String baseTable) {
        for(int i = 0; i < commands.size(); i++) {
            AutonomousCommand command = commands.get(i);
            NetworkTable currCommandTable = Main.network.getTable(baseTable + "/" + i);

            if(command.commands != null) {
                currCommandTable.getEntry("Conditional").setString(command.conditional);
                uploadCommandsTo(Arrays.asList(command.commands), baseTable + "/" + i + "/commands");
            } else {
                currCommandTable.getEntry("Subsystem Name").setString(command.getTemplate().subsystemName);
                currCommandTable.getEntry("Command Name").setString(command.getTemplate().commandName);
                currCommandTable.getEntry("Params").setDoubleArray(command.parameterValues);
            }
        }
    }

    public ArrayList<AutonomousCommand> downloadCommands(String baseTableName) {
        String[] commandIndicies = Main.network.getTable(baseTableName).getSubTables().toArray(new String[0]);
        Arrays.sort(commandIndicies);
        ArrayList<AutonomousCommand> commands = new ArrayList<>();
        for(String i : commandIndicies) {
            NetworkTable currCommandTable = Main.network.getTable(baseTableName + "/" + i);

            if(currCommandTable.containsKey("Conditional")) {
                commands.add(new AutonomousCommand(currCommandTable.getEntry("Conditional").getString(""),
                                                   downloadCommands(baseTableName + "/" + i + "/commands").toArray(new AutonomousCommand[0])));
            } else {
                String templateName = currCommandTable.getEntry("Subsystem Name").getString("") + ":" + currCommandTable.getEntry("Command Name").getString("");
                AutonomousCommand currCommand = new AutonomousCommand(commandTemplates.get(templateName));
                currCommand.parameterValues = currCommandTable.getEntry("Params").getDoubleArray(new double[0]);
                commands.add(currCommand);
            }
        }
        return commands;
    }

    public void setPageSelected(boolean selected) {
        FieldTopdown.fieldObjects.forEach(o -> {
            o.interactable = o instanceof FieldTopdown.StartingPosition;
            o.draggable = false;
        });
    }

    public void clearTable(String tableName) {
        NetworkTable table = Main.network.getTable(tableName);
        table.getKeys().forEach(k -> table.getEntry(k).delete());
        table.getSubTables().forEach(s -> clearTable(tableName + "/" + s));
    }

    public AutonomousPage() {
        Button upload = new Button("Upload");
        upload.setOnAction(evt -> {
            clearTable("Custom Dashboard/Autonomous");
            uploadCommandsTo(getCommandList(), "Custom Dashboard/Autonomous");
        });
        Button download = new Button("Download");
        download.setOnAction(evt -> {
            ArrayList<AutonomousCommand> downloadedCommands = downloadCommands("Custom Dashboard/Autonomous");
        });
        Button openFile = new Button("Open File");
        Button saveFile = new Button("Save File");
        buttons.getChildren().addAll(upload, download, openFile, saveFile);
        buttons.setBackground(new Background(new BackgroundFill(Color.BLACK, new CornerRadii(0), new Insets(0))));
        content.getChildren().add(buttons);

        pathDrawer.vboxSizing(content);
        editor.prefWidthProperty().bind(content.widthProperty());
        content.getChildren().addAll(pathDrawer, editor);

        content.prefWidthProperty().bind(node.widthProperty());
        content.setAlignment(Pos.TOP_CENTER);
        node.setContent(content);
    }
    public Node getNode() { return node; }

    public PathNode selected;
    public PathNode startingNode;

    public void deleteNodeOuts(PathNode node) {
        for(Drive outPath : node.outPaths) {
            deleteNodeOuts(outPath.end);
            pathDrawer.overlay.remove(outPath);
        }
        pathDrawer.overlay.remove(node);
    }

    public AutonomousCommandTemplate driveDistanceCommand() { return commandTemplates.get("Drive:driveDistance"); }
    public AutonomousCommandTemplate turnToAngleCommand() { return commandTemplates.get("Drive:turnToAngle"); }
    public static HashMap<String, AutonomousCommandTemplate> commandTemplates = new HashMap<>();
    public static ArrayList<String> conditionals = new ArrayList<>();

    public void addNodesCommands(ArrayList<AutonomousCommand> commands, PathNode node) {
        commands.addAll(node.commands);
        if(node.outPaths.size() == 1) {
            Drive drive = node.outPaths.get(0);
            double deltaX = (drive.end.x - drive.beginning.x);
            double deltaY = (drive.end.y - drive.beginning.y);
            double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
            double angle = Math.toDegrees(Math.atan2(deltaY, deltaX));
            AutonomousCommand driveCommand = new AutonomousCommand(driveDistanceCommand());
            driveCommand.parameterValues[0] = distance;
            driveCommand.parameterValues[1] = 4;
            driveCommand.parameterValues[2] = 1;
            driveCommand.parameterValues[3] = 1;
            AutonomousCommand turnCommand = new AutonomousCommand(turnToAngleCommand());
            turnCommand.parameterValues[0] = angle;
            turnCommand.parameterValues[1] = 2;
            turnCommand.parameterValues[2] = 1;
            turnCommand.parameterValues[3] = 10;
            commands.add(driveCommand);
            commands.add(turnCommand);

            addNodesCommands(commands, drive.end);
        } else {
            for (Drive outDrive : node.outPaths) {
                ArrayList<AutonomousCommand> branchCommands = new ArrayList<>();

                double deltaX = (outDrive.end.x - outDrive.beginning.x);
                double deltaY = (outDrive.end.y - outDrive.beginning.y);
                double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
                double angle = Math.toDegrees(Math.atan2(deltaY, deltaX));
                AutonomousCommand driveCommand = new AutonomousCommand(driveDistanceCommand());
                driveCommand.parameterValues[0] = distance;
                driveCommand.parameterValues[1] = 4;
                driveCommand.parameterValues[2] = 1;
                driveCommand.parameterValues[3] = 1;
                AutonomousCommand turnCommand = new AutonomousCommand(turnToAngleCommand());
                turnCommand.parameterValues[0] = angle;
                turnCommand.parameterValues[1] = 2;
                turnCommand.parameterValues[2] = 1;
                turnCommand.parameterValues[3] = 10;
                branchCommands.add(driveCommand);
                branchCommands.add(turnCommand);

                addNodesCommands(branchCommands, outDrive.end);
                AutonomousCommand branch = new AutonomousCommand(outDrive.conditional, branchCommands.toArray(new AutonomousCommand[branchCommands.size()]));
                commands.add(branch);
            }
        }
    }

    public ArrayList<AutonomousCommand> getCommandList() {
        ArrayList<AutonomousCommand> commands = new ArrayList<>();
        if(startingNode != null)
            addNodesCommands(commands, startingNode);

        return commands;
    }

    public void addTo(VBox currEditor, AutonomousCommand command) {
        if(command.commands != null) {
            VBox commandBlock = new VBox();
            commandBlock.setPadding(new Insets(10));
            commandBlock.setBackground(new Background(new BackgroundFill(Color.color(1, 1, 1, 1), new CornerRadii(10), new Insets(5))));
            commandBlock.setStyle("-fx-border-color : black");
            commandBlock.prefWidthProperty().bind(currEditor.widthProperty());
            commandBlock.getChildren().add(new Label("branch: " + command.conditional));

            for (AutonomousCommand branchCommand : command.commands) {
                addTo(commandBlock, branchCommand);
            }

            currEditor.getChildren().add(commandBlock);
        } else {
            AutonomousCommandTemplate template = commandTemplates.get(command.templateName);

            VBox commandBlock = new VBox();
            commandBlock.setPadding(new Insets(10));
            commandBlock.setBackground(new Background(new BackgroundFill(Color.color(1, 1, 1, 1), new CornerRadii(10), new Insets(5))));
            commandBlock.setStyle("-fx-border-color : black");
            commandBlock.prefWidthProperty().bind(currEditor.widthProperty());
            commandBlock.getChildren().add(new Label(template.subsystemName + " -> " + template.commandName));

            for(int i = 0; i < template.parameterNames.length; i++) {
                String paramText = template.parameterNames[i] + " " + command.parameterValues[i] + " " + template.parameterUnits[i];
                commandBlock.getChildren().addAll(new Label(paramText));
            }

            currEditor.getChildren().add(commandBlock);
        }

    }

    public void rebuildEditor() {
        editor.getChildren().clear();

        conditionals.clear();
        for(String conditional : Main.logicTable.getKeys()) {
            conditionals.add(conditional);
        }

        commandTemplates.clear();
        for(String subsystem : Main.subsystemTable.getSubTables()) {
            NetworkTable currSubsystemCommandsTable = Main.network.getTable("Custom Dashboard/Subsystem/" + subsystem + "/Commands");
            for (String key : currSubsystemCommandsTable.getKeys()) {
                if (key.endsWith("_ParamNames")) {
                    String commandName = key.replace("_ParamNames", "");
                    AutonomousCommandTemplate commandTemplate = new AutonomousCommandTemplate();
                    commandTemplate.commandName = commandName;
                    commandTemplate.subsystemName = subsystem;
                    commandTemplate.parameterNames = currSubsystemCommandsTable.getEntry(commandName + "_ParamNames").getStringArray(new String[0]);
                    commandTemplate.parameterUnits = currSubsystemCommandsTable.getEntry(commandName + "_ParamUnits").getStringArray(new String[0]);
                    commandTemplates.put(commandTemplate.hashName(), commandTemplate);
                }
            }
        }

        if(selected == null) {
            for(AutonomousCommand command : getCommandList()) {
                addTo(editor, command);
            }
        } else {
            HBox menu = new HBox();
            editor.getChildren().add(menu);

            Button delete = new Button("Delete");
            delete.setOnAction(evt -> {
                deleteNodeOuts(selected);
                if (selected.inPath != null) {
                    pathDrawer.overlay.remove(selected.inPath);
                    selected.inPath.beginning.outPaths.remove(selected.inPath);
                }
                if (selected == startingNode) { startingNode = null; }
                setSelected(null);
                pathDrawer.overlay.remove(selected);
            });
            menu.getChildren().add(delete);

            for(Drive drive : selected.outPaths) {
                ComboBox conditional = new ComboBox(FXCollections.observableArrayList(conditionals));
                conditional.setValue(drive.conditional);
                conditional.setOnAction(evt -> drive.conditional = (String) conditional.getValue());
                conditional.setOnMouseEntered(evt -> drive.color = Color.PURPLE);
                conditional.setOnMouseExited(evt -> drive.color = Color.BLUE);
                //TODO: make this stay highlighted while selecting an option from the dropdown, not just hovering over the box itself
                editor.getChildren().add(conditional);
            }

            for(AutonomousCommand command : selected.commands) {
                AutonomousCommandTemplate template = commandTemplates.get(command.templateName);

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
                titleRow.getChildren().addAll(new Label(template.subsystemName + " -> " + template.commandName), deleteBlock);
                commandBlock.getChildren().add(titleRow);

                for(int i = 0; i < template.parameterNames.length; i++) {
                    final int index = i;
                    HBox parameterRow = new HBox();
                    TextField parameterField = new TextField(Double.toString(command.parameterValues[i]));
                    parameterField.setOnAction(event -> {
                        try {
                            command.parameterValues[index] = Double.valueOf(parameterField.getText());
                        } catch (Exception e) { }
                    });
                    parameterRow.getChildren().addAll(new Label(template.parameterNames[i]), parameterField, new Label(template.parameterUnits[i]));
                    commandBlock.getChildren().add(parameterRow);
                }

                editor.getChildren().add(commandBlock);
            }

            FlowPane addCommand = new FlowPane();
            addCommand.setPadding(new Insets(10));
            addCommand.setBackground(new Background(new BackgroundFill(Color.color(0, 0, 0, 0.5), new CornerRadii(10), new Insets(5))));
            addCommand.prefWidthProperty().bind(editor.widthProperty());

            for(AutonomousCommandTemplate commandTemplate : commandTemplates.values()) {
                if(!commandTemplate.subsystemName.equals("Drive")) {
                    Button addCommandButton = new Button(commandTemplate.subsystemName + " -> " + commandTemplate.commandName);
                    addCommandButton.setOnAction(evt -> {
                        selected.commands.add(new AutonomousCommand(commandTemplate));
                        rebuildEditor();
                    });
                    addCommand.getChildren().add(addCommandButton);
                }
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

    public void onClickStartingLocation(FieldTopdown.StartingPosition pos) {
        PathNode newStartingNode = new PathNode(pos.x, pos.y);
        newStartingNode.draggable = false;
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

    public static class AutonomousCommandTemplate {
        String subsystemName;
        String commandName;
        String[] parameterUnits;
        String[] parameterNames;
        String formatString;

        public String hashName() { return subsystemName + ":" + commandName; }
    }

    public static class AutonomousCommand {
        String templateName;
        double[] parameterValues;

        String conditional;
        AutonomousCommand[] commands;

        public AutonomousCommand(String conditional, AutonomousCommand[] commands) {
            this.conditional = conditional;
            this.commands = commands;
        }

        public AutonomousCommand(AutonomousCommandTemplate t) {
            templateName = t.hashName();
            parameterValues = new double[t.parameterNames.length];
        }

        public AutonomousCommandTemplate getTemplate() { return commandTemplates.get(templateName); }
    }

    public class PathNode extends FieldTopdown.Drawable {
        public ArrayList<Drive> outPaths = new ArrayList<>();
        public Drive inPath;
        public ArrayList<AutonomousCommand> commands = new ArrayList<>();
        public boolean draggable = true;

        public PathNode(double nX, double nY) { x = nX; y = nY; }

        public void draw(GraphicsContext gc, FieldTopdown field) {
            gc.setFill(this == field.hot ? Color.RED : Color.GREEN);
            if(selected == this) gc.setFill(Color.LAVENDERBLUSH);
            gc.fillOval(-5, -5, field.getPixelPerInch() * 10, field.getPixelPerInch() * 10);
        }

        public boolean contains(double nX, double nY) {
            return Math.sqrt((x - nX) * (x - nX) + (y - nY) * (y - nY)) < 10;
        }

        public void drag(double nX, double nY) {
            if(draggable) {
                x = nX;
                y = nY;
                rebuildEditor();
            }
        }

        public void click(FieldTopdown fieldTopdown) {
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

        String conditional;
        Color color;

        public Drive(PathNode b, PathNode e) {
            beginning = b;
            beginning.outPaths.add(this);
            end = e;
            e.inPath = this;
            color = Color.BLUE;
        }

        public void draw(GraphicsContext gc, FieldTopdown field) {
            gc.setStroke(this == field.hot ? Color.RED : color);
            gc.setLineWidth(4);
            gc.strokeLine(beginning.x, beginning.y, end.x, end.y);
        }

        public boolean contains(double nX, double nY) {
            return new Line(beginning.x, beginning.y, end.x, end.y).contains(nX, nY);
        }

        public void click(FieldTopdown field) {

        }
    }
}
