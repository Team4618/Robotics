package team4618.dashboard.pages;

import edu.wpi.first.networktables.NetworkTable;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import team4618.dashboard.Main;
import team4618.dashboard.autonomous.AutonomousCommand;
import team4618.dashboard.autonomous.AutonomousCommandTemplate;
import team4618.dashboard.autonomous.Drive;
import team4618.dashboard.autonomous.PathNode;
import team4618.dashboard.components.FieldTopdown;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AutonomousPage extends DashboardPage implements FieldTopdown.OnClick{
    ScrollPane node = new ScrollPane();
    VBox content = new VBox();
    HBox buttons = new HBox();
    public static FieldTopdown pathDrawer;
    public static VBox editor;

    public static void uploadCommands(List<AutonomousCommand> commands) {
        clearTable("Custom Dashboard/Autonomous");
        uploadCommandsTo(commands, "Custom Dashboard/Autonomous");
        HomePage.resetAutoView();
    }

    public static void uploadCommandsTo(List<AutonomousCommand> commands, String baseTable) {
        for(int i = 0; i < commands.size(); i++) {
            AutonomousCommand command = commands.get(i);
            NetworkTable currCommandTable = Main.network.getTable(baseTable + "/" + i);

            if(command.isBranchedCommand()) {
                currCommandTable.getEntry("Conditional").setString(command.conditional);
                uploadCommandsTo(Arrays.asList(command.commands), baseTable + "/" + i + "/commands");
            } else {
                currCommandTable.getEntry("Subsystem Name").setString(command.getTemplate().subsystemName);
                currCommandTable.getEntry("Command Name").setString(command.getTemplate().commandName);
                currCommandTable.getEntry("Params").setDoubleArray(command.parameterValues);
            }
        }
    }

    public static ArrayList<AutonomousCommand> downloadCommandsFrom(String baseTableName) {
        String[] commandIndicies = Main.network.getTable(baseTableName).getSubTables().toArray(new String[0]);
        Arrays.sort(commandIndicies);
        ArrayList<AutonomousCommand> commands = new ArrayList<>();
        for(String i : commandIndicies) {
            NetworkTable currCommandTable = Main.network.getTable(baseTableName + "/" + i);

            if(currCommandTable.containsKey("Conditional") && currCommandTable.containsSubTable("commands")) {
                commands.add(new AutonomousCommand(currCommandTable.getEntry("Conditional").getString(""),
                                                   downloadCommandsFrom(baseTableName + "/" + i + "/commands").toArray(new AutonomousCommand[0])));
            } else if(currCommandTable.containsKey("Subsystem Name") &&
                      currCommandTable.containsKey("Command Name") &&
                      currCommandTable.containsKey("Params")) {
                String templateName = currCommandTable.getEntry("Subsystem Name").getString("") + ":" + currCommandTable.getEntry("Command Name").getString("");
                AutonomousCommand currCommand = new AutonomousCommand(AutonomousCommandTemplate.templates.get(templateName));
                currCommand.parameterValues = currCommandTable.getEntry("Params").getDoubleArray(new double[0]);
                commands.add(currCommand);
            }
        }
        return commands;
    }

    public static void commandsToPath(List<AutonomousCommand> commands, PathNode startingNode, double initialAngle, FieldTopdown field) {
        PathNode currNode = startingNode;

        double angle = initialAngle;
        for(AutonomousCommand command : commands) {
            if(command.commands == null) {
                if(command.templateName.equals("Drive:turnToAngle")) {
                    angle = command.parameterValues[0];
                    currNode.commands.add(command);
                } else if(command.templateName.equals("Drive:driveDistance")) {
                    currNode.commands.remove(currNode.commands.size() - 1);
                    double distance = command.parameterValues[0] * 12;
                    PathNode newNode = new PathNode(currNode.x + distance * Math.cos(Math.toRadians(angle)),
                                                    currNode.y + distance * Math.sin(Math.toRadians(angle)));
                    Drive newDrive = new Drive(currNode, newNode);
                    field.overlay.add(newDrive);
                    field.overlay.add(newNode);
                    currNode = newNode;
                } else {
                    currNode.commands.add(command);
                }
            } else {
                List<AutonomousCommand> branchCommands = Arrays.asList(command.commands);
                assert branchCommands.get(0).templateName.equals("Drive:turnToAngle") && branchCommands.get(1).templateName.equals("Drive:driveDistance");
                double newAngle = branchCommands.get(0).parameterValues[0];
                double distance = branchCommands.get(1).parameterValues[0] * 12;
                PathNode newNode = new PathNode(currNode.x + distance * Math.cos(Math.toRadians(newAngle)),
                                                currNode.y + distance * Math.sin(Math.toRadians(newAngle)));
                Drive newDrive = new Drive(currNode, newNode);
                newDrive.conditional = command.conditional;
                field.overlay.add(newDrive);
                field.overlay.add(newNode);
                commandsToPath(branchCommands.subList(2, branchCommands.size()), newNode, newAngle, field);
            }
        }
    }

    public void setPageSelected(boolean selected) {
        FieldTopdown.fieldObjects.forEach(o -> {
            o.interactable = o instanceof FieldTopdown.StartingPosition;
            o.draggable = false;
        });
    }

    public static void clearTable(String tableName) {
        NetworkTable table = Main.network.getTable(tableName);
        table.getKeys().forEach(k -> table.getEntry(k).delete());
        table.getSubTables().forEach(s -> clearTable(tableName + "/" + s));
    }

    public void resetPathDrawer() {
        pathDrawer.overlay.clear();
        selected = null;
        startingNode = null;
        rebuildEditor();
    }

    public AutonomousPage() {
        pathDrawer = new FieldTopdown(this);
        editor = new VBox();

        Button upload = new Button("Upload");
        upload.setOnAction(evt -> uploadCommands(getCommandList()));
        Button download = new Button("Download");
        download.setOnAction(evt -> {
            PathNode currStartingNode = startingNode;
            resetPathDrawer();
            if(currStartingNode != null) {
                startingNode = currStartingNode;
                pathDrawer.overlay.add(currStartingNode);
                commandsToPath(downloadCommandsFrom("Custom Dashboard/Autonomous"), currStartingNode, 0, pathDrawer);
                rebuildEditor();
            }
        });
        Button openFile = new Button("Open File");
        openFile.setOnAction(evt -> {
            resetPathDrawer();
            try {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Open Auto File");

                FileReader reader = new FileReader(fileChooser.showOpenDialog(new Stage()));
                JSONObject rootObject = (JSONObject) JSONValue.parseWithException(reader);
                reader.close();

                startingPos = FieldPage.startingPositions.get(rootObject.get("Starting Position"));
                startingNode = new PathNode(startingPos.x, startingPos.y);
                pathDrawer.overlay.add(startingNode);
                ArrayList<AutonomousCommand> commandList = new ArrayList<>();
                ((JSONArray) rootObject.get("Commands")).forEach(j -> commandList.add(new AutonomousCommand((JSONObject) j)));
                commandsToPath(commandList, startingNode, 0, pathDrawer);
            } catch (Exception e) { e.printStackTrace(); }
            rebuildEditor();
        });

        Button saveFile = new Button("Save File");
        saveFile.setOnAction(evt -> {
            if(startingPos != null) {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Save Auto File");

                JSONObject rootObject = new JSONObject();
                rootObject.put("Starting Position", startingPos.name);
                JSONArray rootCommandArray = new JSONArray();
                rootObject.put("Commands", rootCommandArray);
                getCommandList().forEach(c -> rootCommandArray.add(c.toJSON()));
                try {
                    FileWriter writer = new FileWriter(fileChooser.showSaveDialog(new Stage()));
                    rootObject.writeJSONString(writer);
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        Button clear = new Button("Clear");
        clear.setOnAction(evt -> resetPathDrawer());

        buttons.getChildren().addAll(upload, download, openFile, saveFile, clear);
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

    public static Object selected;
    public static PathNode startingNode;
    public static FieldTopdown.StartingPosition startingPos;

    public void setSelected(FieldTopdown.Drawable newSelected) {
        if(!(newSelected instanceof FieldTopdown.StartingPosition)) {
            selected = selected == newSelected ? null : newSelected;
        }

        rebuildEditor();
    }

    public static void propagateAndDash(PathNode startingNode) {
        //TODO
    }

    public void onClick(double x, double y) {
        if(selected instanceof PathNode) {
            PathNode newTurn = new PathNode(x, y);
            pathDrawer.overlay.add(new Drive((PathNode) selected, newTurn));
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
            newStartingNode.outPaths.forEach(o -> o.beginning = newStartingNode);
            pathDrawer.overlay.remove(startingNode);
        }

        startingPos = pos;
        startingNode = newStartingNode;
        setSelected(startingNode);
    }

    public static void deleteNodeOuts(PathNode node) {
        for(Drive outPath : node.outPaths) {
            deleteNodeOuts(outPath.end);
            pathDrawer.overlay.remove(outPath);
        }
        pathDrawer.overlay.remove(node);
    }

    public static double canonicalizeAngle(double rawAngle) {
        int revolutions = (int) (rawAngle / 360);
        double mod360 = (rawAngle - revolutions * 360);
        return mod360 < 0 ? 360 + mod360 : mod360;
    }

    public static void addNodesCommands(ArrayList<AutonomousCommand> commands, PathNode node) {
        commands.addAll(node.commands);
        if(node.outPaths.size() == 1) {
            Drive drive = node.outPaths.get(0);
            drive.addCommandsTo(commands);
            addNodesCommands(commands, drive.end);
        } else {
            for (Drive outDrive : node.outPaths) {
                ArrayList<AutonomousCommand> branchCommands = new ArrayList<>();
                outDrive.addCommandsTo(branchCommands);
                addNodesCommands(branchCommands, outDrive.end);
                commands.add(new AutonomousCommand(outDrive.conditional, branchCommands.toArray(new AutonomousCommand[0])));
            }
        }
    }

    public static ArrayList<AutonomousCommand> getCommandList() {
        ArrayList<AutonomousCommand> commands = new ArrayList<>();
        if(startingNode != null)
            addNodesCommands(commands, startingNode);

        return commands;
    }

    public static VBox commandBlock(VBox parent, Node titleBar) {
        DropShadow dropShadow = new DropShadow();
        dropShadow.setRadius(5.0);
        dropShadow.setColor(Color.BLACK);

        VBox commandBlock = new VBox();
        commandBlock.setPadding(new Insets(10));
        commandBlock.setBackground(new Background(new BackgroundFill(Color.color(0.67, 0.67, 0.67, 1), new CornerRadii(10), new Insets(5))));
        commandBlock.setEffect(dropShadow);
        commandBlock.prefWidthProperty().bind(parent.widthProperty());
        commandBlock.getChildren().add(titleBar);
        return commandBlock;
    }
    public static VBox commandBlock(VBox parent, String title) { return commandBlock(parent, new Label(title)); }

    public static void rebuildEditor() {
        editor.getChildren().clear();
        AutonomousCommandTemplate.refreshCommandsAndLogic();

        if(selected == null) {
            getCommandList().forEach(c -> editor.getChildren().add(c.commandBlock(editor)));
        } else if(selected instanceof PathNode) {
            PathNode selectedNode = (PathNode) selected;
            HBox menu = new HBox();
            editor.getChildren().add(menu);

            Button delete = new Button("Delete");
            delete.setOnAction(evt -> {
                deleteNodeOuts(selectedNode);
                if (selectedNode.inPath != null) {
                    pathDrawer.overlay.remove(selectedNode.inPath);
                    selectedNode.inPath.beginning.outPaths.remove(selectedNode.inPath);
                }
                if (selected == startingNode) { startingNode = null; }
                pathDrawer.overlay.remove(selected);
                selected = null;
                rebuildEditor();
            });
            menu.getChildren().add(delete);

            for(Drive drive : selectedNode.outPaths) {
                VBox conditionalBlock = commandBlock(editor, new Pane());
                ComboBox conditional = new ComboBox(FXCollections.observableArrayList(AutonomousCommandTemplate.conditionals.keySet()));
                conditional.setValue(drive.conditional);
                conditional.setOnAction(evt -> drive.conditional = (String) conditional.getValue());
                conditionalBlock.setOnMouseEntered(evt -> drive.color = Color.PURPLE);
                conditionalBlock.setOnMouseExited(evt -> drive.color = Color.BLUE);
                conditionalBlock.getChildren().add(conditional);
                editor.getChildren().add(conditionalBlock);
            }

            selectedNode.commands.forEach(c -> editor.getChildren().add(c.editorBlock(editor, selectedNode)));

            FlowPane addCommand = new FlowPane();
            addCommand.setPadding(new Insets(10));
            addCommand.setBackground(new Background(new BackgroundFill(Color.color(0, 0, 0, 0.5), new CornerRadii(10), new Insets(5))));
            addCommand.prefWidthProperty().bind(editor.widthProperty());

            for(AutonomousCommandTemplate commandTemplate : AutonomousCommandTemplate.templates.values()) {
                if(!blacklistedCommands.contains(commandTemplate.hashName())) {
                    Button addCommandButton = new Button(commandTemplate.hashName());
                    addCommandButton.setOnAction(evt -> {
                        selectedNode.commands.add(new AutonomousCommand(commandTemplate));
                        rebuildEditor();
                    });
                    addCommand.getChildren().add(addCommandButton);
                }
            }
            editor.getChildren().add(addCommand);
        } else if(selected instanceof Drive) {
            Drive selectedDrive = (Drive) selected;

            addParameterSlider(0, 15, () -> selectedDrive.driveMaxSpeed,
                                s -> selectedDrive.driveMaxSpeed = s, "Max Drive Speed", "Feet/Sec");
            addParameterSlider(0, 15, () -> selectedDrive.turnMaxSpeed,
                    s -> selectedDrive.turnMaxSpeed = s, "Max Turn Speed", "Feet/Sec");
            addParameterSlider(0, selectedDrive.getDistance(), () -> selectedDrive.distanceToSlowdown,
                    s -> selectedDrive.distanceToSlowdown = s, "Begin Slowdown Distance", "Feet");
            addParameterSlider(0, 180, () -> selectedDrive.angleToSlowdown,
                    s -> selectedDrive.angleToSlowdown = s, "Begin Slowdown Angle", "Degrees");
            addParameterSlider(0, 5, () -> selectedDrive.driveTimeUntilMaxSpeed,
                    s -> selectedDrive.driveTimeUntilMaxSpeed = s, "Drive Acceleration Time", "Seconds");
            addParameterSlider(0, 5, () -> selectedDrive.turnTimeUntilMaxSpeed,
                    s -> selectedDrive.turnTimeUntilMaxSpeed = s, "Turn Acceleration Time", "Seconds");
        }
    }

    public static void addParameterSlider(double min, double max, Supplier<Double> getValue, Consumer<Double> setValue,
                                          String name, String unit) {
        HBox row = new HBox();
        Slider slider = new Slider();
        Label valueLabel = new Label(String.valueOf(Math.round(getValue.get() * 100) / 100.0) + " " + unit);
        slider.setMin(min);
        slider.setMax(max);
        slider.setValue(getValue.get());
        slider.valueProperty().addListener(evt -> {
            setValue.accept(slider.getValue());
            valueLabel.setText(String.valueOf(Math.round(getValue.get() * 100) / 100.0) + " " + unit);
        });
        row.getChildren().addAll(new Label(name), slider, valueLabel);
        editor.getChildren().add(row);
    }

    public static ArrayList<String> blacklistedCommands = new ArrayList<>();
    static {
        blacklistedCommands.add("Drive:driveDistance");
        blacklistedCommands.add("Drive:driveCurve");
    }
}
