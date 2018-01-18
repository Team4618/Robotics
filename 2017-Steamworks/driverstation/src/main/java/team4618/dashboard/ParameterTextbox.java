package team4618.dashboard;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.text.Font;

public class ParameterTextbox {
    Label label;
    TextField textbox;

    double value = 0;

    public ParameterTextbox(String name) {
        label = new Label(name);
        label.setFont(new Font("Ariel", 20));
        label.setAlignment(Pos.BASELINE_CENTER);

        textbox = new TextField(Double.toString(value));
        textbox.setStyle("-fx-border-color: green; -fx-border-width: 2px;");
        textbox.setOnKeyReleased(event -> {
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
                value = Double.valueOf(textbox.getText());
            } catch (Exception e) { }
        });
    }
}
