package team4618.dashboard.pages;

import edu.wpi.first.networktables.NetworkTable;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import team4618.dashboard.Main;
import team4618.dashboard.autonomous.*;
import team4618.dashboard.autonomous.DriveCurve.Vector;
import team4618.dashboard.components.FieldTopdown;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AutonomousPage extends DashboardPage implements FieldTopdown.OnClick {
    ScrollPane node = new ScrollPane();
    VBox content = new VBox();
    HBox buttons = new HBox();
    public static FieldTopdown pathDrawer;
    public static VBox editor;

    public static void uploadCommands(List<AutonomousCommand> commands) {
        AutonomousCommandTemplate.refreshCommandsAndLogic();
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
        int[] commandIndicies = Main.network.getTable(baseTableName).getSubTables().stream().mapToInt(Integer::valueOf).toArray();
        Arrays.sort(commandIndicies);

        ArrayList<AutonomousCommand> commands = new ArrayList<>();
        for(int i : commandIndicies) {
            NetworkTable currCommandTable = Main.network.getTable(baseTableName + "/" + i);

            if(currCommandTable.containsKey("Conditional") && currCommandTable.containsSubTable("commands")) {
                String conditional = currCommandTable.getEntry("Conditional").getString("");
                commands.add(new AutonomousCommand(conditional,
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

    public static void commandsToPath(List<AutonomousCommand> commands, PathNode startingNode, FieldTopdown field) {
        PathNode currNode = startingNode;

        AutonomousCommand lastAngle = null;
        for(AutonomousCommand command : commands) {
            if(!command.isBranchedCommand()) {
                if(command.templateName.equals("Drive:turnToAngle")) {
                    lastAngle = command;
                    currNode.commands.add(command);
                } else if(command.templateName.equals("Drive:driveDistance")) {
                    if(lastAngle != null) {
                        double angle = lastAngle.parameterValues[0];
                        double distance = command.parameterValues[0] * 12;
                        PathNode newNode = new PathNode(currNode.x + distance * Math.cos(Math.toRadians(angle)),
                                                        currNode.y + distance * Math.sin(Math.toRadians(angle)));
                        DriveStraight newDrive = new DriveStraight(currNode, newNode);
                        newDrive.turnMaxSpeed = lastAngle.parameterValues[1];
                        newDrive.turnTimeUntilMaxSpeed = lastAngle.parameterValues[2];
                        newDrive.angleToSlowdown = lastAngle.parameterValues[3];
                        newDrive.driveMaxSpeed = command.parameterValues[1];
                        newDrive.driveTimeUntilMaxSpeed = command.parameterValues[2];
                        newDrive.distanceToSlowdown = command.parameterValues[3];
                        newDrive.backwards = distance < 0;
                        field.overlay.add(newDrive);
                        field.overlay.add(newNode);

                        currNode.commands.remove(lastAngle);
                        lastAngle = null;
                        currNode = newNode;
                    } else {
                        HomePage.errorMessage("Cannot Convert Commands To Path", "Drive distance specified without an angle");
                    }
                } else if(command.templateName.equals("Drive:driveCurve")) {
                    if(lastAngle != null) {
                        double time = command.parameterValues[0];

                        double endX = currNode.x + command.parameterValues[command.parameterValues.length - 2] * 12;
                        double endY = currNode.y + command.parameterValues[command.parameterValues.length - 1] * 12;

                        PathNode newNode = new PathNode(endX, endY);

                        //TODO: is backwards
                        DriveCurve newDrive = new DriveCurve(currNode, newNode);
                        newDrive.time = time;

                        for(int i = 1; i < command.parameterValues.length - 2; i += 2) {
                            DriveCurve.ControlPoint c = new DriveCurve.ControlPoint(currNode.x + command.parameterValues[i] * 12, currNode.y + command.parameterValues[i + 1] * 12);
                            newDrive.controlPoints.add(c);
                            field.overlay.add(c);
                        }

                        field.overlay.add(newDrive);
                        field.overlay.add(newNode);

                        currNode.commands.remove(lastAngle);
                        lastAngle = null;
                        currNode = newNode;
                    } else {
                        HomePage.errorMessage("Cannot Convert Commands To Path", "Drive curve specified without an angle");
                    }
                } else {
                    currNode.commands.add(command);
                }
            } else {
                List<AutonomousCommand> branchCommands = Arrays.asList(command.commands);
                assert branchCommands.get(0).templateName.equals("Drive:turnToAngle") && branchCommands.get(1).templateName.equals("Drive:driveDistance");
                AutonomousCommand turnCommand = branchCommands.get(0);
                AutonomousCommand driveCommand = branchCommands.get(1);
                double newAngle = branchCommands.get(0).parameterValues[0];
                double distance = branchCommands.get(1).parameterValues[0] * 12;
                PathNode newNode = new PathNode(currNode.x + distance * Math.cos(Math.toRadians(newAngle)),
                                                currNode.y + distance * Math.sin(Math.toRadians(newAngle)));
                DriveStraight newDrive = new DriveStraight(currNode, newNode);
                newDrive.turnMaxSpeed = turnCommand.parameterValues[1];
                newDrive.turnTimeUntilMaxSpeed = turnCommand.parameterValues[2];
                newDrive.angleToSlowdown = turnCommand.parameterValues[3];
                newDrive.driveMaxSpeed = driveCommand.parameterValues[1];
                newDrive.driveTimeUntilMaxSpeed = driveCommand.parameterValues[2];
                newDrive.distanceToSlowdown = driveCommand.parameterValues[3];
                newDrive.backwards = distance < 0;
                newDrive.conditional = command.conditional;
                field.overlay.add(newDrive);
                field.overlay.add(newNode);
                commandsToPath(branchCommands.subList(2, branchCommands.size()), newNode, field);
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

    public File currentAutoFile = null;
    public Button saveCurrentFile = new Button();
    public boolean drawCurves = false;

    public AutonomousPage() {
        pathDrawer = new FieldTopdown(this);
        editor = new VBox();

        Label pathModeLabel = new Label("Straight Lines");
        pathModeLabel.setTooltip(new Tooltip("S for straight lines, C for curves"));

        node.setOnKeyReleased(evt -> {
            if(evt.getCode().equals(KeyCode.S)) {
                drawCurves = false;
            } else if(evt.getCode().equals(KeyCode.C)) {
                drawCurves = true;
            }
            pathModeLabel.setText(drawCurves ? "Curves" : "Straight Lines");
        });

        Button upload = new Button("Upload");
        upload.setOnAction(evt -> uploadCommands(getCommandList()));
        Button download = new Button("Download");
        download.setOnAction(evt -> {
            resetPathDrawer();
            if(startingPos != null) {
                startingNode = new PathNode(startingPos.x, startingPos.y);
                pathDrawer.overlay.add(startingNode);
                commandsToPath(downloadCommandsFrom("Custom Dashboard/Autonomous"), startingNode, pathDrawer);
                rebuildEditor();
            }
        });
        Button openFile = new Button("Open File");
        openFile.setOnAction(evt -> {
            resetPathDrawer();
            try {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Open Auto File");

                currentAutoFile = fileChooser.showOpenDialog(new Stage());
                saveCurrentFile.setVisible(true);
                saveCurrentFile.setText("Save " + currentAutoFile.getName());

                FileReader reader = new FileReader(currentAutoFile);
                JSONObject rootObject = (JSONObject) JSONValue.parseWithException(reader);
                reader.close();

                startingPos = FieldPage.startingPositions.get(rootObject.get("Starting Position"));
                startingNode = new PathNode(startingPos.x, startingPos.y);
                pathDrawer.overlay.add(startingNode);
                ArrayList<AutonomousCommand> commandList = new ArrayList<>();
                ((JSONArray) rootObject.get("Commands")).forEach(j -> commandList.add(new AutonomousCommand((JSONObject) j)));
                commandsToPath(commandList, startingNode, pathDrawer);
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
                    currentAutoFile = fileChooser.showSaveDialog(new Stage());
                    saveCurrentFile.setVisible(true);
                    saveCurrentFile.setText("Save " + currentAutoFile.getName());

                    FileWriter writer = new FileWriter(currentAutoFile);
                    rootObject.writeJSONString(writer);
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        Button clear = new Button("Clear");
        clear.setOnAction(evt -> resetPathDrawer());

        saveCurrentFile.setVisible(false);
        saveCurrentFile.setOnAction(evt -> {
            if((startingPos != null) && (currentAutoFile != null)) {
                JSONObject rootObject = new JSONObject();
                rootObject.put("Starting Position", startingPos.name);
                JSONArray rootCommandArray = new JSONArray();
                rootObject.put("Commands", rootCommandArray);
                getCommandList().forEach(c -> rootCommandArray.add(c.toJSON()));
                try {
                    FileWriter writer = new FileWriter(currentAutoFile);
                    rootObject.writeJSONString(writer);
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        Button showAll = new Button("Show All");
        showAll.setOnAction(evt -> {
            if(startingNode != null) {
                startingNode.outPaths.forEach(d -> propagateVisibility(d, true));
            }
        });

        buttons.getChildren().addAll(pathModeLabel, upload, download, openFile, saveFile, clear, showAll, saveCurrentFile);
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
        if(!(newSelected instanceof FieldTopdown.StartingPosition) &&
                !(newSelected instanceof DriveCurve.ControlPoint)) {
            selected = selected == newSelected ? null : newSelected;
        }

        rebuildEditor();
    }

    public static void propagateAndDash(PathNode startingNode, boolean certain) {
        boolean chosePath = false;
        for(DriveManeuver path : startingNode.outPaths) {
            boolean pathCertain = certain && (AutonomousCommandTemplate.conditionals.get(path.conditional) == AutonomousCommandTemplate.ConditionalState.True) && !chosePath;
            if(pathCertain)
                chosePath = true;

            path.dashed = !pathCertain;
            propagateAndDash(path.end, pathCertain);
        }
    }

    public static void propagateVisibility(DriveManeuver root, boolean visible) {
        root.visible = visible;
        root.end.visible = visible;
        root.end.outPaths.forEach(d -> propagateVisibility(d, visible));
    }

    public void onClick(double x, double y) {
        if(selected instanceof PathNode) {
            PathNode newTurn = new PathNode(x, y);
            pathDrawer.overlay.add(newTurn);

            if(drawCurves) {
                DriveCurve curve = new DriveCurve((PathNode) selected, newTurn);
                pathDrawer.overlay.add(curve);
                DriveCurve.ControlPoint c = new DriveCurve.ControlPoint((curve.beginning.x + curve.end.x) / 2, (curve.beginning.y + curve.end.y) / 2);
                curve.controlPoints.add(c);
                pathDrawer.overlay.add(c);
                setSelected(curve);
            } else {
                pathDrawer.overlay.add(new DriveStraight((PathNode) selected, newTurn));
                setSelected(newTurn);
            }
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
        for(DriveManeuver outPath : node.outPaths) {
            deleteNodeOuts(outPath.end);
            outPath.remove(pathDrawer.overlay);
        }
        pathDrawer.overlay.remove(node);
    }

    public static double canonicalizeAngle(double rawAngle) {
        int revolutions = (int) (rawAngle / 360);
        double mod360 = (rawAngle - revolutions * 360);
        return mod360 < 0 ? 360 + mod360 : mod360;
    }

    public static boolean addNodesCommands(ArrayList<AutonomousCommand> commands, PathNode node) {
        commands.addAll(node.commands);
        if(node.outPaths.size() == 1) {
            DriveManeuver drive = node.outPaths.get(0);
            drive.addCommandsTo(commands);
            if(addNodesCommands(commands, drive.end))
                return true;
        } else {
            if(!node.isComplete())
                return true;

            for (DriveManeuver outDrive : node.outPaths) {
                ArrayList<AutonomousCommand> branchCommands = new ArrayList<>();
                outDrive.addCommandsTo(branchCommands);
                if(addNodesCommands(branchCommands, outDrive.end))
                    return true;

                commands.add(new AutonomousCommand(outDrive.conditional, branchCommands.toArray(new AutonomousCommand[0])));
            }
        }

        return false;
    }

    public static ArrayList<AutonomousCommand> getCommandList() {
        ArrayList<AutonomousCommand> commands = new ArrayList<>();
        if(startingNode != null) {
            if(addNodesCommands(commands, startingNode)) {
                HomePage.errorMessage("Cannot Generate Commands", "Incomplete Nodes");
                commands.clear();
            }
        }

        return commands;
    }

    public static Pane commandBlock(VBox parent, Pane commandBlock, Node titleBar) {
        DropShadow dropShadow = new DropShadow();
        dropShadow.setRadius(5.0);
        dropShadow.setColor(Color.BLACK);

        commandBlock.setPadding(new Insets(10));
        commandBlock.setBackground(new Background(new BackgroundFill(Color.color(0.67, 0.67, 0.67, 1), new CornerRadii(10), new Insets(5))));
        commandBlock.setEffect(dropShadow);
        commandBlock.prefWidthProperty().bind(parent.widthProperty());
        commandBlock.getChildren().add(titleBar);
        return commandBlock;
    }

    public static VBox commandBlock(VBox parent, Node titleBar) { return (VBox) commandBlock(parent, new VBox(), titleBar); }
    public static VBox commandBlock(VBox parent, String title) { return commandBlock(parent, new Label(title)); }

    public static class OutPathBlock {
        public HBox uiBlock;
        public DriveManeuver path;
        public OutPathBlock(HBox b, DriveManeuver p) {
            uiBlock = b;
            path = p;
        }
    }

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
                    selectedNode.inPath.remove(pathDrawer.overlay);
                    selectedNode.inPath.beginning.outPaths.remove(selectedNode.inPath);
                }
                if (selected == startingNode) { startingNode = null; }
                pathDrawer.overlay.remove(selected);
                selected = null;
                rebuildEditor();
            });
            menu.getChildren().add(delete);

            ArrayList<OutPathBlock> outPathBlocks = new ArrayList<>();
            for(DriveManeuver drive : selectedNode.outPaths) {
                HBox pathBlock = (HBox) commandBlock(editor, new HBox(), new Pane());
                pathBlock.setOnMouseEntered(evt -> drive.color = Color.PURPLE);
                pathBlock.setOnMouseExited(evt -> drive.color = Color.BLUE);

                pathBlock.setOnMouseReleased(evt -> {
                    outPathBlocks.forEach(a -> System.out.println(a.path.conditional + " " + a.uiBlock.getTranslateY()));
                    outPathBlocks.sort((a, b) -> {
                        double aY = a.uiBlock.getLayoutY() + a.uiBlock.getTranslateY();
                        double bY = b.uiBlock.getLayoutY() + b.uiBlock.getTranslateY();
                        return (int)(aY - bY);
                    });
                    selectedNode.outPaths = new ArrayList<>();
                    outPathBlocks.forEach(a -> selectedNode.outPaths.add(a.path));
                    rebuildEditor();
                });

                Vector nodeStartingPosition = new Vector(0, 0);
                Vector lastMousePosition = new Vector(0, 0);

                pathBlock.setOnMousePressed(evt -> {
                    lastMousePosition.x = evt.getSceneX();
                    lastMousePosition.y = evt.getSceneY();

                    // get the current coordinates of the draggable node.
                    nodeStartingPosition.x = pathBlock.getTranslateX();
                    nodeStartingPosition.y = pathBlock.getTranslateY();
                });

                pathBlock.setOnMouseDragged(evt -> {
                    double deltaX = evt.getSceneX() - lastMousePosition.x;
                    double deltaY = evt.getSceneY() - lastMousePosition.y;

                    // add the delta coordinates to the node coordinates.
                    nodeStartingPosition.y += deltaY;

                    // set the layout for the draggable node.
                    pathBlock.setTranslateY(nodeStartingPosition.y);

                    // get the latest mouse coordinate.
                    lastMousePosition.x = evt.getSceneX();
                    lastMousePosition.y = evt.getSceneY();
                });

                ComboBox conditional = new ComboBox(FXCollections.observableArrayList(AutonomousCommandTemplate.conditionals.keySet()));
                conditional.setValue(drive.conditional);
                conditional.setOnAction(evt -> drive.conditional = (String) conditional.getValue());

                ToggleButton toggleVisibility = new ToggleButton("Show");
                toggleVisibility.setSelected(drive.visible);
                toggleVisibility.selectedProperty().addListener(evt -> propagateVisibility(drive, toggleVisibility.isSelected()));

                pathBlock.getChildren().addAll(conditional, toggleVisibility);
                editor.getChildren().add(pathBlock);
                outPathBlocks.add(new OutPathBlock(pathBlock, drive));
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
        } else if(selected instanceof DriveStraight) {
            DriveStraight selectedDrive = (DriveStraight) selected;

            ToggleButton backwardsToggle = new ToggleButton("Backwards");
            backwardsToggle.setSelected(selectedDrive.backwards);
            backwardsToggle.selectedProperty().addListener(evt -> {
                selectedDrive.backwards = backwardsToggle.isSelected();
            });
            editor.getChildren().add(backwardsToggle);

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
        } else if(selected instanceof DriveCurve) {
            DriveCurve curve = (DriveCurve) selected;
            Button addControlPoint = new Button("Add Control Point");
            addControlPoint.setOnAction(evt -> {
                DriveCurve.ControlPoint lastC = curve.controlPoints.get(curve.controlPoints.size() - 1);
                DriveCurve.ControlPoint c = new DriveCurve.ControlPoint((curve.end.x + lastC.x) / 2, (curve.end.y + lastC.y) / 2);
                curve.controlPoints.add(c);
                pathDrawer.overlay.add(c);
            });

            Slider timeSlider = new Slider(0, 40, curve.time);
            Label timeLabel = new Label(curve.time + " seconds");
            timeSlider.valueProperty().addListener(l -> {
                curve.time = timeSlider.getValue();
                timeLabel.setText(curve.time + " seconds");
            });


            Slider tHeadingSlider = new Slider(0, 1, curve.tHeading);
            tHeadingSlider.valueProperty().addListener(l -> {
                curve.tHeading = tHeadingSlider.getValue();
            });

            editor.getChildren().addAll(addControlPoint, timeSlider, timeLabel, tHeadingSlider);
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
