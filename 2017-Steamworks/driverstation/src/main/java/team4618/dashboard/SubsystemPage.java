package team4618.dashboard;

import edu.wpi.first.networktables.*;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.HashMap;

import static team4618.dashboard.Main.*;

public class SubsystemPage extends ScrollPane implements TableEntryListener {
    VBox content = new VBox();
    HBox parameters = new HBox();
    VBox state_graphs = new VBox();
    Button menu_button = new Button();

    NumberAxis xAxis = new NumberAxis();
    NumberAxis yAxis = new NumberAxis();
    LineChart<Number, Number> chart = new LineChart(xAxis, yAxis);
    HashMap<String, XYChart.Series> series = new HashMap<>();
    NetworkTable stateTable;
    NetworkTable parameterTable;

    public SubsystemPage(double width, BorderPane root, String subtable) {
        this.setContent(content);
        this.setPrefWidth(width);
        this.setStyle("-fx-background-color: yellow");

        stateTable = network.getTable("Custom Dashboard/" + subtable + "/State");
        stateTable.addEntryListener(this, EntryListenerFlags.kLocal);
        parameterTable = network.getTable("Custom Dashboard/" + subtable + "/Parameters");

        menu_button.setText(subtable);
        menu_button.setOnAction(event -> root.setCenter(this));

        parameters.setPrefWidth(this.getPrefWidth());
        parameters.setStyle("-fx-background-color: lime green");
        parameters.setSpacing(10);
        state_graphs.setPrefWidth(this.getPrefWidth());
        state_graphs.setStyle("-fx-background-color: cyan");
        content.getChildren().addAll(parameters, state_graphs);

        for(String param : parameterTable.getKeys()) {
            ParameterTextbox param_textbox = new ParameterTextbox(param);
            parameters.getChildren().addAll(param_textbox.label, param_textbox.textbox);
        }

        yAxis.setLabel("Feet/Sec");
        state_graphs.getChildren().add(chart);
    }

    @Override
    public void valueChanged(NetworkTable table, String key, NetworkTableEntry entry, NetworkTableValue value, int flags) {
        if(!series.containsKey(key)) {
            XYChart.Series new_series = new XYChart.Series();
            new_series.setName(key);
            series.put(key, new_series);
        }

        XYChart.Data d = new XYChart.Data(1, value);
        series.get(key).getData().add(d);
        Tooltip.install(d.getNode(), new Tooltip(value + " Feet/Sec"));
    }
}
