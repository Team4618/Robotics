package team4618.dashboard.components;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Scale;
import javafx.stage.Screen;

import java.util.ArrayList;

public class FieldTopdown extends Canvas {
    public static class Point {
        public double x, y;
        public Point(double X, double Y) { x = X; y = Y; }
    }

    public interface FieldOverlayProvider {
        void drawOverlay();
        void onDrag(double x, double y);
        void onMouseRelease(double x, double y);
        void onMousePressed(double x, double y);
    }

    public static Image field = new Image(ClassLoader.getSystemClassLoader().getResourceAsStream("field.png"));

    public ArrayList<Point> startingPositions = new ArrayList<>();
    public Point startingPosition = null;

    public boolean drawMouse = false;
    public double mouseX = 0, mouseY = 0;

    public FieldOverlayProvider overlay;

    //TODO: Parameters here
    public static double distanceToTop = 95;

    public FieldTopdown() {
        this(null);
    }

    public FieldTopdown(FieldOverlayProvider ovl) {
        overlay = ovl;

        startingPositions.add(new Point(20, 40));
        startingPositions.add(new Point(20, 100));
        startingPositions.add(new Point(20, 160));

        this.widthProperty().addListener((observableValue, oldWidth, newWidth) -> { this.setHeight(newWidth.doubleValue() / 1.8); draw(); });
        this.setOnMouseEntered(event -> { drawMouse = true; draw(); });
        this.setOnMouseExited(event -> { drawMouse = false; draw(); });

        this.setOnMouseMoved(event -> {
            mouseX = event.getX();
            mouseY = event.getY();
            draw();
        });

        this.setOnMousePressed(event -> {
            double pixelsPerInch = (getWidth() / 648);

            System.out.println(event.getX() / pixelsPerInch + " : " + event.getY() / pixelsPerInch);

            if(overlay != null)
                overlay.onMousePressed(event.getX() / pixelsPerInch, event.getY() / pixelsPerInch);

            draw();
        });

        this.setOnMouseReleased(event -> {
            double pixelsPerInch = (getWidth() / 648);

            for(Point start : startingPositions) {
                Rectangle bounds = new Rectangle(start.x * pixelsPerInch - 10, start.y * pixelsPerInch - 10, 20, 20);
                if(bounds.contains(mouseX, mouseY)) {
                    startingPosition = start;
                    draw();
                    return;
                }
            }

            if(overlay != null)
                overlay.onMouseRelease(event.getX() / pixelsPerInch, event.getY() / pixelsPerInch);

            draw();
        });

        this.setOnMouseDragged(event -> {
            double pixelsPerInch = (getWidth() / 648);

            if(overlay != null)
                overlay.onDrag(event.getX() / pixelsPerInch, event.getY() / pixelsPerInch);

            mouseX = event.getX();
            mouseY = event.getY();
            draw();
        });
    }

    public void vboxSizing(VBox vb) {
        vb.widthProperty().addListener((observableValue, oldWidth, newWidth) -> this.setWidth(Math.min(newWidth.doubleValue(), Screen.getPrimary().getDpi() * 7)));
    }

    public boolean isResizable() { return true; }
    public double minWidth(double height) { return 0.0; }

    public GraphicsContext gc = this.getGraphicsContext2D();

    public void draw() {
        double pixelsPerInch = (getWidth() / 648);

        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, getWidth(), getHeight());

        gc.drawImage(field, 0, 0, getWidth(), getHeight());

        gc.setLineWidth(3);

        for(Point start : startingPositions) {
            Rectangle bounds = new Rectangle(start.x * pixelsPerInch - 10, start.y * pixelsPerInch - 10, 20, 20);
            gc.setStroke((drawMouse && bounds.contains(mouseX, mouseY)) ? Color.BLUEVIOLET : Color.AQUAMARINE);
            gc.strokeOval(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
        }

        gc.setTransform(new Affine(new Scale(pixelsPerInch, pixelsPerInch)));

        drawField();

        if(overlay != null)
            overlay.drawOverlay();
        gc.setTransform(new Affine());
    }

    //TODO: finish this then remove the field image
    public void drawField() {
        gc.setStroke(Color.IVORY);
        gc.strokeLine(143, distanceToTop, 192, distanceToTop);
    }
}
