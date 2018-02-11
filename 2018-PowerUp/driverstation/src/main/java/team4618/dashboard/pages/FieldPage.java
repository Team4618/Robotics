package team4618.dashboard.pages;

import javafx.scene.layout.VBox;
import team4618.dashboard.components.FieldTopdown;

public class FieldPage extends VBox {
    public FieldTopdown field = new FieldTopdown();

    //TODO: edit field parameters
    public FieldPage() {
        this.getChildren().add(field);
        field.widthProperty().bind(this.widthProperty());
    }
}
