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
import team4618.dashboard.Main;
import team4618.dashboard.pages.AutonomousPage;

import java.util.ArrayList;

public class FieldTopdown extends Canvas {
    public static final Image field = new Image(ClassLoader.getSystemClassLoader().getResourceAsStream("field.png"));

    public static abstract class Drawable {
        public double x;
        public double y;

        public void drag(double newX, double newY) {}
        public void click(FieldTopdown field) {}
        public abstract void draw(GraphicsContext gc, FieldTopdown field);
        public abstract boolean contains(double x, double y);
    }

    public interface OnClick {
        void onClick (double x, double y);
        void onClickStartingLocation(StartingPosition pos);
    }

    OnClick onClick;
    public Drawable hot;

    public static ArrayList<Drawable> fieldObjects = new ArrayList<>();
    public ArrayList<Drawable> overlay = new ArrayList<>();

    public FieldTopdown() { this(null); }

    public FieldTopdown(OnClick nOnClick) {
        onClick = nOnClick;
        this.widthProperty().addListener((observableValue, oldWidth, newWidth) -> this.setHeight(newWidth.doubleValue() / 1.8));

        this.setOnMouseMoved(event -> {
            double pixelsPerInch = (getWidth() / 648);
            double x = event.getX() / pixelsPerInch;
            double y = event.getY() / pixelsPerInch;

            hot = null;
            for (Drawable d : fieldObjects) { if(d.contains(x, y)) { hot = d; } }
            for (Drawable d : overlay) { if(d.contains(x, y)) { hot = d; }  }
        });

        this.setOnMouseReleased(event -> {
            double pixelsPerInch = (getWidth() / 648);
            double x = event.getX() / pixelsPerInch;
            double y = event.getY() / pixelsPerInch;

            if(hot != null) {
                hot.click(this);
            } else if(onClick != null) {
                onClick.onClick(x, y);
            }

            System.out.println("Field66 " + x + " : " + y);
        });

        this.setOnMouseDragged(event -> {
            double pixelsPerInch = (getWidth() / 648);
            double x = event.getX() / pixelsPerInch;
            double y = event.getY() / pixelsPerInch;

            if(hot != null) {
                hot.drag(x, y);
            }
        });

        Main.redrawCallbacks.add(this::draw);
    }

    public void vboxSizing(VBox vb) {
        vb.widthProperty().addListener((observableValue, oldWidth, newWidth) -> this.setWidth(Math.min(newWidth.doubleValue(), Screen.getPrimary().getDpi() * 7)));
    }

    public boolean isResizable() { return true; }
    public double minWidth(double height) { return 0.0; }
    public double getPixelPerInch() { return getWidth() / 648; }

    public GraphicsContext gc = this.getGraphicsContext2D();

    public void draw() {
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, getWidth(), getHeight());
        gc.drawImage(field, 0, 0, getWidth(), getHeight());

        double pixelsPerInch = (getWidth() / 648);
        gc.setTransform(new Affine(new Scale(pixelsPerInch, pixelsPerInch)));

        for (Drawable d : fieldObjects) { draw(d); }
        for (Drawable d : overlay) { draw(d); }

        gc.setTransform(new Affine());
    }

    void draw(Drawable d) {
        gc.translate(d.x, d.y);
        d.draw(gc, this);
        gc.translate(-d.x, -d.y);
    }

    public static class StartingPosition extends Drawable {
        public StartingPosition(double nX, double nY) { x = nX; y = nY; }

        public void draw(GraphicsContext gc, FieldTopdown field) {
            gc.setStroke(this == field.hot ? Color.RED : Color.GREEN);
            gc.setLineWidth(2);
            gc.strokeOval(-8, -8, field.getPixelPerInch() * 16, field.getPixelPerInch() * 16);
        }

        public boolean contains(double nX, double nY) {
            return Math.sqrt((x - nX) * (x - nX) + (y - nY) * (y - nY)) < 10;
        }

        public void click(FieldTopdown field) {
            field.onClick.onClickStartingLocation(this);
        }
    }
}
