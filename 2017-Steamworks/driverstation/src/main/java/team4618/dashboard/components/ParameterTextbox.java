package team4618.dashboard.components;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;

public class ParameterTextbox extends HBox{
    public Label label;
    public TextField textbox;

    public ParameterTextbox(NetworkTableEntry entry) {
        label = new Label(NetworkTable.basenameKey(entry.getName()));
        label.setFont(new Font("Ariel", 20));
        label.setAlignment(Pos.BASELINE_CENTER);

        textbox = new TextField(Double.toString(entry.getDouble(0)));
        textbox.setStyle("-fx-border-color: green; -fx-border-width: 2px;");
        textbox.setOnKeyReleased(event -> {
            double value = entry.getDouble(0);
            try {
                double new_value = Double.valueOf(textbox.getText());
                if(new_value == value) {
                    textbox.setStyle("-fx-border-color: green; -fx-border-width: 2px;");
                } else {
                    textbox.setStyle("-fx-border-color: yellow; -fx-border-width: 2px;");
                }
            } catch (Exception e) {
                textbox.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
            }
        });

        textbox.setOnAction(event -> {
            try {
                entry.setDouble(Double.valueOf(textbox.getText()));
            } catch (Exception e) { }
        });

        this.getChildren().addAll(label, textbox);
    }
}
