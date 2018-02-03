package team4618.dashboard.pages;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import team4618.dashboard.Main;
import team4618.dashboard.components.FieldTopdown;

import java.util.ArrayList;

public class AutonomousPage extends ScrollPane implements FieldTopdown.FieldOverlayProvider {
    VBox content = new VBox();
    HBox buttons = new HBox();
    FieldTopdown pathDrawer = new FieldTopdown(this);

    Node currentEditor = null;

    public static class AutonomousCommand {
        public String subsystemName;
        public String commandName;
        public double[] params;

        public AutonomousCommand(String sName, String cName) {
            subsystemName = sName;
            commandName = cName;
        }
    }

    public static class PathEvent {
        FieldTopdown.Point pos;
        ArrayList<AutonomousCommand> commands = new ArrayList<>();
    }

    public ArrayList<PathEvent> path = new ArrayList<>();

    public PathEvent draggingEvent = null;
    public PathEvent selectedEvent = null;

    public class AutonomousCommandBlock extends VBox {
        ComboBox<String> subsystemBox = new ComboBox<>();
        ComboBox<String> commandBox = new ComboBox<>();
        VBox parameters = new VBox();

        public AutonomousCommandBlock() {
            Button up = new Button("Up");
            Button down = new Button("Down");
            Button delete = new Button("Delete");

            commandBox.setDisable(true);
            commandBox.valueProperty().addListener((obsV, oldV, newV) -> {
                Main.Subsystem.CommandParam[] cParams = Main.subsystems.get(subsystemBox.getValue()).commands.get(newV);

                parameters.getChildren().clear();
                for(Main.Subsystem.CommandParam cParam : cParams) {
                    TextField paramBox = new TextField();
                    Tooltip.install(paramBox, new Tooltip(cParam.name + " " + cParam.unit));
                    parameters.getChildren().add(paramBox);
                }
            });

            subsystemBox.getItems().addAll(Main.subsystems.keySet());
            subsystemBox.valueProperty().addListener((obsV, oldV, newV) -> {
                commandBox.getItems().clear();
                commandBox.getItems().addAll(Main.subsystems.get(newV).commands.keySet());
                commandBox.setDisable(commandBox.getItems().size() == 0);
            });

            this.getChildren().addAll(up, down, delete, subsystemBox, commandBox);
            this.setPadding(new Insets(10));
            this.setBackground(new Background(new BackgroundFill(Color.color(0, 0, 0, 0.5), new CornerRadii(10), new Insets(5))));
        }

        public AutonomousCommand toCommand() {
            AutonomousCommand command = new AutonomousCommand(subsystemBox.getValue(), commandBox.getValue());
            Main.Subsystem.CommandParam[] cParams = Main.subsystems.get(subsystemBox.getValue()).commands.get(commandBox.getValue());
            command.params = new double[cParams.length];
            //TODO: get params from text boxes
            return command;
        }
    }

    public AutonomousPage() {
        Button upload = new Button("Upload");
        Button openFile = new Button("Open File");
        Button saveFile = new Button("Save File");
        buttons.getChildren().addAll(upload, openFile, saveFile);
        buttons.setBackground(new Background(new BackgroundFill(Color.BLACK, new CornerRadii(0), new Insets(0))));
        content.getChildren().add(buttons);

        pathDrawer.vboxSizing(content);
        content.getChildren().add(pathDrawer);

        VBox testEditor = new VBox();

        //TODO: Drag to reorder
        AutonomousCommandBlock commandBlock = new AutonomousCommandBlock();
        testEditor.getChildren().add(commandBlock);

        //TODO: add command button
        VBox addCommand = new VBox();
        addCommand.setPadding(new Insets(10));
        addCommand.setBackground(new Background(new BackgroundFill(Color.color(0, 0, 0, 0.5), new CornerRadii(10), new Insets(5))));

        addCommand.getChildren().add(new Button("Elevator -> goToHeight"));
        addCommand.getChildren().add(new Button("Intake -> in"));
        addCommand.getChildren().add(new Button("Intake -> eject"));

        testEditor.getChildren().add(addCommand);
        setCurrentEditor(testEditor);

        //TODO: get this to properly size without a horizontal scroll bar
        content.prefWidthProperty().bind(this.widthProperty());
        content.setAlignment(Pos.TOP_CENTER);
        this.setContent(content);
    }

    public void setCurrentEditor(Node newEditor) {
        if(currentEditor != null) {
            content.getChildren().remove(currentEditor);
        }

        content.getChildren().add(newEditor);
        currentEditor = newEditor;
    }

    public void drawOverlay() {
        GraphicsContext gc = pathDrawer.gc;

        gc.setStroke(Color.BLACK);
        for (int i = 0; i < path.size() - 1; i++) {
            gc.strokeLine(path.get(i).pos.x, path.get(i).pos.y, path.get(i + 1).pos.x, path.get(i + 1).pos.y);
        }

        if(pathDrawer.startingPosition != null) {
            if (path.size() != 0) {
                gc.setStroke(Color.BLACK);
                gc.strokeLine(path.get(0).pos.x, path.get(0).pos.y, pathDrawer.startingPosition.x, pathDrawer.startingPosition.y);
            }

            gc.setFill(Color.AQUAMARINE);
            gc.fillOval(pathDrawer.startingPosition.x - 3, pathDrawer.startingPosition.y - 3, 6, 6);
        }

        for(PathEvent point : path) {
            Rectangle bounds = new Rectangle(point.pos.x - 3, point.pos.y - 3, 6, 6);
            gc.setFill(Color.GREENYELLOW);
            gc.setFill(selectedEvent == point ? Color.CRIMSON : gc.getFill());
            gc.fillRect(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
        }

        if(pathDrawer.drawMouse) {
            //TODO: draw closest place on path to cursor, right click to add event there, left click to select
            //TODO: profile editor for selected path
        }
    }

    public void onDrag(double x, double y) {
        if(draggingEvent != null) {
            draggingEvent.pos.x = x;
            draggingEvent.pos.y = y;
        }
    }

    public void onMouseRelease(double x, double y) {
        if(draggingEvent != null) {
            selectedEvent = draggingEvent;
            //TODO: display this event's command list
            //setCurrentEditor();
            draggingEvent = null;
        } else {
            PathEvent pathEvent = new PathEvent();
            pathEvent.pos = new FieldTopdown.Point(x, y);
            path.add(pathEvent);
        }
    }

    public void onMousePressed(double x, double y) {
        for(PathEvent point : path) {
            Rectangle bounds = new Rectangle(point.pos.x - 3, point.pos.y - 3, 6, 6);
            if(bounds.contains(x, y)) {
                draggingEvent = point;
            }
        }
    }

    public AutonomousCommand[] getCommands() {
        //TODO: generate command list from turns, drives & event commands
        return null;
    }

    //TODO: Event creator (command editor & Profile editor)

    //TODO: path finding?
    //TODO: decision list?
}
