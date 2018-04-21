package team4618.dashboard;

import edu.wpi.first.networktables.ConnectionNotification;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import team4618.dashboard.components.MultiLineGraph;
import team4618.dashboard.pages.*;

import java.net.DatagramSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Consumer;

public class Main extends Application implements Consumer<ConnectionNotification> {
    public static void main(String[] args) {
        launch(args);
    }

    public static NetworkTableInstance network;
    public static NetworkTable mainTable;
    public static NetworkTable subsystemTable;
    public static NetworkTable autoTable;
    public static NetworkTable currentlyExecutingTable;
    public static NetworkTable logicTable;
    public static NetworkTable teleopTable;

    public static class Subsystem {
        public static class CommandParam { public String name; public String unit; }

        public String name;
        public NetworkTable table;
        public NetworkTable parameterTable;
        public NetworkTable stateTable;
        public NetworkTable commandTable;
        public HashMap<String, CommandParam[]> commands = new HashMap<>();

        public Subsystem(String name) {
            this.name = name;
            table = network.getTable("Custom Dashboard/Subsystem/" + name);
            parameterTable = network.getTable("Custom Dashboard/Subsystem/" + name + "/Parameters");
            stateTable = network.getTable("Custom Dashboard/Subsystem/" + name + "/State");
            commandTable = network.getTable("Custom Dashboard/Subsystem/" + name + "/Commands");

            for(String key : commandTable.getKeys()) {
                String command = key.replace("_ParamNames", "").replace("_ParamUnits", "");
                String[] values = commandTable.getEntry(key).getStringArray(new String[0]);

                if(!commands.containsKey(command)) {
                    commands.put(command, new CommandParam[values.length]);
                    for(int i = 0; i < commands.get(command).length; i++) {
                        commands.get(command)[i] = new CommandParam();
                    }
                }

                CommandParam[] params = commands.get(command);
                for(int i = 0; i < params.length; i++) {
                    if(key.endsWith("_ParamNames")) {
                        params[i].name = values[i];
                    } else if(key.endsWith("_ParamUnits")) {
                        params[i].unit = values[i];
                    }
                }
            }
        }
    }

    public static Socket commPort;
    public static DatagramSocket statePort;

    public static HashMap<String, Subsystem> subsystems = new HashMap<>();

    public static ArrayList<Runnable> redrawCallbacks = new ArrayList<>();
    public Timeline redrawTask = new Timeline(new KeyFrame(Duration.millis(33), e -> { for (Runnable r : redrawCallbacks) { r.run(); }} ));

    public Rectangle connectionStatus = new Rectangle();
    public Text connectionName = new Text("No Connection");

    public BorderPane root = new BorderPane();
    public VBox menu = new VBox();

    public static AutonomousPage autonomousPage;

    @Override
    public void start(Stage window) {
        //TODO: new networking
        /*
        try {
            commPort = new Socket("roboRIO-4618-FRC.lan", 5082);
            statePort = new DatagramSocket();

            statePort.getPort();


        } catch(Exception e) { e.printStackTrace(); }
        */

        network = NetworkTableInstance.getDefault();
        network.setServerTeam(4618);
        //network.setServer("localhost");
        network.startClient();
        mainTable = network.getTable("Custom Dashboard");
        subsystemTable = network.getTable("Custom Dashboard/Subsystem");
        autoTable = network.getTable("Custom Dashboard/Autonomous");
        currentlyExecutingTable = network.getTable("Custom Dashboard/Executing");
        logicTable = network.getTable("Custom Dashboard/Logic");
        teleopTable = network.getTable("Custom Dashboard/Teleop");

        Image logo = new Image(ClassLoader.getSystemClassLoader().getResourceAsStream("logo.png"));
        BackgroundImage[] backgrounds = new BackgroundImage[2];
        backgrounds[0] = new BackgroundImage(logo /*new Image(ClassLoader.getSystemClassLoader().getResourceAsStream("background.png"))*/, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, BackgroundSize.DEFAULT);
        backgrounds[1] = new BackgroundImage(logo, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, BackgroundSize.DEFAULT);
        root.setBackground(new Background(backgrounds));
        window.setTitle("Dashboard");
        window.setScene(new Scene(root, 1280, 720));
        window.getIcons().add(logo);
        window.show();

        HBox statusBar = new HBox();
        statusBar.setPrefHeight(20);
        statusBar.setBackground(new Background(new BackgroundFill(Color.BLACK, new CornerRadii(0), new Insets(0))));
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setPadding(new Insets(2));
        root.setTop(statusBar);

        connectionStatus.widthProperty().bind(connectionStatus.heightProperty());
        connectionStatus.heightProperty().bind(statusBar.heightProperty().subtract(4));
        connectionStatus.setFill(Color.RED);
        connectionStatus.setArcHeight(7);
        connectionStatus.setArcWidth(7);

        connectionName.setStroke(Color.WHITE);
        connectionName.setFill(Color.WHITE);
        statusBar.getChildren().addAll(connectionStatus, connectionName);

        menu.setBackground(new Background(new BackgroundFill(Color.color(0, 0, 0, 0.1), new CornerRadii(0), new Insets(0))));
        menu.setPrefWidth(100);
        root.setLeft(menu);

        HomePage homePage = new HomePage();
        addMenuButton("Home", homePage);

        autonomousPage = new AutonomousPage();
        addMenuButton("Autonomous", autonomousPage);

        RobotPage robotPage = new RobotPage();
        addMenuButton("Robot", robotPage);

        FieldPage fieldPage = new FieldPage();
        addMenuButton("Field", fieldPage);

        root.setCenter(autonomousPage.getNode());
        DashboardPage.setSelectedPage(autonomousPage);

        redrawTask.setCycleCount(Timeline.INDEFINITE);
        redrawTask.play();
        network.addConnectionListener(this, true);
    }

    public void addMenuButton(String name, DashboardPage page) {
        Button robot = new Button(name);
        robot.setOnAction(event -> {
            root.setCenter(page.getNode());
            DashboardPage.setSelectedPage(page);
        });
        robot.prefWidthProperty().bind(menu.widthProperty());
        menu.getChildren().add(robot);
    }

    public static boolean connected = false;

    @Override
    public void accept(ConnectionNotification connectionNotification) {
        for(Node n : menu.getChildren()) {
            Button b = (Button) n;
            if(subsystems.containsKey(b.getText())) {
                menu.getChildren().remove(n);
            }
        }

        subsystems.clear();

        connected = connectionNotification.connected;
        if(connectionNotification.connected) {
            connectionStatus.setFill(Color.GREEN);
            connectionName.setText(mainTable.getEntry("name").getString(""));

            for(String subtable : subsystemTable.getSubTables()) {
                Subsystem subsystem = new Subsystem(subtable);
                subsystems.put(subtable, subsystem );
                SubsystemPage page = new SubsystemPage(subsystem);
                Platform.runLater(() -> addMenuButton(subtable, page));
            }
        } else {
            connectionStatus.setFill(Color.RED);
            connectionName.setText("No Connection");
        }
    }
}
