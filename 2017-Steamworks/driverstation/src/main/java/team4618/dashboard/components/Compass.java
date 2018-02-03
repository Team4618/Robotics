package team4618.dashboard.components;

import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Compass extends Canvas {
    public GraphicsContext gc = this.getGraphicsContext2D();
    public double angle = 0;

    public Compass() {
        super(250, 250);
        draw();
    }

    public void draw() {
        gc.setFill(Color.GREY);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(3);
        gc.fillOval(0, 0, 250, 250);

        Point2D center = new Point2D(125, 125);
        Point2D angle_offset = new Point2D(Math.sin((Math.PI / 180) * angle), -Math.cos((Math.PI / 180) * angle));
        angle_offset = angle_offset.multiply(125).add(center);
        gc.strokeLine(center.getX(), center.getY(), angle_offset.getX(), angle_offset.getY());
    }

    public void setAngle(double angle) {
        this.angle = angle;
        draw();
    }
}
