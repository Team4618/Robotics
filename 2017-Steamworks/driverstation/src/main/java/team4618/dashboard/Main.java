package team4618.dashboard;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.lang.reflect.Field;
import java.util.ArrayList;

public class Main extends Application {
    public static void main(String[] args) {
        launch(args);
        network.stopClient(); //TODO: this stays on, why?
    }

    public static NetworkTableInstance network;

    @Override
    public void start(Stage window) {
        network = NetworkTableInstance.getDefault();
        network.setServerTeam(4618);
        network.startClient();

        NetworkTable main_table = network.getTable("Custom Dashboard");

        BorderPane root = new BorderPane();
        window.setTitle("Dashboard");
        window.setScene(new Scene(root, 1280, 720));
        window.show();

        VBox menu = new VBox();
        menu.setStyle("-fx-background-color: red");
        menu.setPrefWidth(100);
        root.setLeft(menu);

        VBox home_pane = new VBox();
        home_pane.setStyle("-fx-background-color: blue");

        Button reconnect = new Button("Reconnect");
        reconnect.setOnAction(event -> {});
        home_pane.getChildren().add(reconnect);
        /*
        Image field_topdown = new Image("./hydrodynamics.png");
        ImageView field = new ImageView(field_topdown);
        field.setPreserveRatio(true);
        field.setSmooth(true);
        field.setCache(true);
        home_pane.getChildren().add(field);
        */

        ArrayList<SubsystemPage> pages = new ArrayList();
        for(String subtable : main_table.getSubTables()) {
            SubsystemPage page = new SubsystemPage(window.getWidth() - menu.getPrefWidth(), root, subtable);
            pages.add(page);
            menu.getChildren().add(page.menu_button);
        }

        VBox drive_content = new VBox();
        ScrollPane drive_pane = new ScrollPane(drive_content);
        drive_pane.setPrefWidth(window.getWidth() - menu.getPrefWidth() - 31);
        drive_pane.setStyle("-fx-background-color: yellow");

        HBox parameters = new HBox();
        parameters.setPrefWidth(drive_pane.getPrefWidth());
        parameters.setStyle("-fx-background-color: lime green");
        parameters.setSpacing(10);
        drive_content.getChildren().add(parameters);

        VBox state_graphs = new VBox();
        state_graphs.setPrefWidth(drive_pane.getPrefWidth());
        state_graphs.setStyle("-fx-background-color: cyan");
        drive_content.getChildren().add(state_graphs);

        Button home = new Button("Home");
        home.setOnAction(event -> root.setCenter(home_pane));
        menu.getChildren().add(home);

        Button drive = new Button("Drive");
        drive.setOnAction(event -> root.setCenter(drive_pane));
        menu.getChildren().add(drive);

        Tooltip dummytooltip = new Tooltip();
        try {
            Field fieldBehavior = Tooltip.class.getDeclaredField("BEHAVIOR");
            fieldBehavior.setAccessible(true);
            Object objBehavior = fieldBehavior.get(dummytooltip);

            Field fieldTimer = objBehavior.getClass().getDeclaredField("activationTimer");
            fieldTimer.setAccessible(true);
            Timeline objTimer = (Timeline) fieldTimer.get(objBehavior);

            objTimer.getKeyFrames().clear();
            objTimer.getKeyFrames().add(new KeyFrame(new Duration(0)));
        } catch (Exception e) {
            e.printStackTrace();
        }

        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Feet/Sec");
        LineChart<Number, Number> chart = new LineChart<Number, Number>(xAxis, yAxis);

        XYChart.Series leftSpeed = new XYChart.Series();
        leftSpeed.setName("Left Speed");
        leftSpeed.getData().add(new XYChart.Data(1, 10));
        leftSpeed.getData().add(new XYChart.Data(2, 6));
        leftSpeed.getData().add(new XYChart.Data(4, 12));

        XYChart.Series rightSpeed = new XYChart.Series();
        rightSpeed.setName("Right Speed");
        rightSpeed.getData().add(new XYChart.Data(1, 11));
        rightSpeed.getData().add(new XYChart.Data(2, 7));
        rightSpeed.getData().add(new XYChart.Data(4, 9));

        chart.getData().addAll(leftSpeed, rightSpeed);
        state_graphs.getChildren().add(chart);

        for (XYChart.Series<Number, Number> s : chart.getData()) {
            for (XYChart.Data<Number, Number> d : s.getData()) {
                Tooltip.install(d.getNode(), new Tooltip(d.getYValue().toString() + " Feet/Sec"));
            }
        }

        root.setCenter(home_pane);

        Compass compass = new Compass();
        state_graphs.getChildren().add(compass);

        Compass compass2 = new Compass();
        state_graphs.getChildren().add(compass2);

        ParameterTextbox angle = new ParameterTextbox("Angle");
        angle.textbox.setOnAction(event -> {
            try {
                angle.value = Double.valueOf(angle.textbox.getText());
                compass.setAngle(angle.value);
            } catch (Exception e) { }
        });
        parameters.getChildren().addAll(angle.label, angle.textbox);

        ParameterTextbox P = new ParameterTextbox("P");
        ParameterTextbox I = new ParameterTextbox("I");
        ParameterTextbox D = new ParameterTextbox("D");
        parameters.getChildren().addAll(P.label, P.textbox, I.label, I.textbox, D.label, D.textbox);

        //window.setFullScreen(true);
    }
}
