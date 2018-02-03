package team4618.dashboard.pages;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import team4618.dashboard.components.FieldTopdown;

public class HomePage extends VBox {

    public HomePage() {
        this.setAlignment(Pos.TOP_CENTER);

        //TODO: get the live position view working
        FieldTopdown liveFieldView = new FieldTopdown();
        liveFieldView.vboxSizing(this);
        this.getChildren().add(liveFieldView);

        VBox currentlyExecuting = new VBox();
        currentlyExecuting.setPadding(new Insets(10));
        currentlyExecuting.setBackground(new Background(new BackgroundFill(Color.color(0, 0, 0, 0.5), new CornerRadii(10), new Insets(5))));

        //TODO: actually implement this
        currentlyExecuting.getChildren().add(new Label("Currently Executing: Drive -> driveToDistance"));
        currentlyExecuting.getChildren().add(new Label("Left Remaining: 1 feet"));
        currentlyExecuting.getChildren().add(new Label("Right Remaining: 1.01 feet"));

        this.getChildren().add(currentlyExecuting);
    }
}
