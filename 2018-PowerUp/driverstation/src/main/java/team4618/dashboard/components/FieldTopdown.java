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

import java.util.ArrayList;

public class FieldTopdown extends Canvas {
    public static final Image field = new Image(ClassLoader.getSystemClassLoader().getResourceAsStream("field.png"));

    public static abstract class Drawable {
        public double x;
        public double y;

        public void drag(double newX, double newY) {}
        public void click() {}
        public abstract void draw(GraphicsContext gc);
        public abstract boolean contains(double x, double y);
    }

    public interface OnClick {
        void onClick (double x, double y);
    }

    OnClick onClick;
    public Drawable hot;

    public ArrayList<Drawable> fieldObjects = new ArrayList<>();
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
                hot.click();
            } else if(onClick != null) {
                onClick.onClick(x, y);
            }

            System.out.println(x + " : " + y);
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
        d.draw(gc);
        gc.translate(-d.x, -d.y);
    }
}
