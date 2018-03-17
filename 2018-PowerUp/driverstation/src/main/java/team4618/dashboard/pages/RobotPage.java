package team4618.dashboard.pages;

import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Slider;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import team4618.dashboard.Main;
import team4618.dashboard.components.MultiLineGraph;

public class RobotPage extends DashboardPage {
    MultiLineGraph testGraph = new MultiLineGraph();
    Canvas canvas = new Canvas(400, 300);
    Slider tSlider = new Slider(0.0, 1.0, 0.5);
    VBox node = new VBox();

    public RobotPage() {
        testGraph.prefWidthProperty().bind(node.widthProperty());
        node.getChildren().addAll(testGraph, canvas, tSlider);

        for(int i = 0; i < 110; i++) {
            testGraph.addData("Test1", MultiLineGraph.Units.Feet, i * Math.sin(i), i*10);
        }

        for(int i = 0; i < 100; i++) {
            testGraph.addData("Test2", MultiLineGraph.Units.Percent, 0.7 * Math.sin(i), i*10);
        }

        for(int i = 0; i < 10; i++) {
            testGraph.addData("Test3", MultiLineGraph.Units.Degrees, i * 10, i);
        }

        Main.redrawCallbacks.add(this::draw);
    }

    double p0x = 20, p0y = 10;
    double p1x = 200, p1y = 250;
    double p2x = 380, p2y = 10;

    public void draw() {
        testGraph.draw();

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        gc.setStroke(Color.BLACK);
        gc.strokeRect(0, 0, canvas.getWidth(), canvas.getHeight());

        gc.setFill(Color.BLUE);
        gc.fillOval(p0x - 4, p0y - 4, 8, 8);
        gc.fillOval(p1x - 4, p1y - 4, 8, 8);
        gc.fillOval(p2x - 4, p2y - 4, 8, 8);
        for(double t = 0; t <= 1.0; t += 0.01) {
            double x = Math.pow(1 - t, 2) * p0x + 2 * t * (1 - t) * p1x + Math.pow(t, 2) * p2x;
            double y = Math.pow(1 - t, 2) * p0y + 2 * t * (1 - t) * p1y + Math.pow(t, 2) * p2y;
            gc.fillOval(x - 2, y - 2, 4, 4);
        }

        {
            double prevWidth = gc.getLineWidth();
            gc.setLineWidth(2);
            double t = tSlider.getValue();
            double x = Math.pow(1 - t, 2) * p0x + 2 * t * (1 - t) * p1x + Math.pow(t, 2) * p2x;
            double y = Math.pow(1 - t, 2) * p0y + 2 * t * (1 - t) * p1y + Math.pow(t, 2) * p2y;
            double dx = 2 * (1 - t) * (p1x - p0x) + 2 * t * (p2x - p1x);
            double dy = 2 * (1 - t) * (p1y - p0y) + 2 * t * (p2y - p1y);

            gc.fillOval(x - 4, y - 4, 8, 8);
            gc.strokeLine(x, y, x + 0.1 * dx, y + 0.1 * dy);
            gc.setLineWidth(prevWidth);
        }
    }

    public void setPageSelected(boolean selected) { }
    public Node getNode() { return node; }
}
