package team4618.dashboard.components;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class MultiLineGraph extends VBox {
    public enum Units { Feet, FeetPerSecond, Degrees, DegreesPerSecond, Seconds, Unitless }

    public static class Entry {
        double value;
        long time;
        public Entry(double v, long t) { value = v; time = t; }
    }

    public class Graph {
        public ArrayList<Entry> data = new ArrayList<>();
        public Color color;
        public Units unit;
        public Button toggle;
        public boolean enabled = true;

        public Graph(Color c, Units u, String name) {
            color = c;
            unit = u;
            toggle = new Button(name);
            toggle.setOnAction(event -> { enabled = !enabled; draw(); });
            toggle.setBackground(new Background(new BackgroundFill(color, new CornerRadii(0), new Insets(0))));
        }
    }

    public Canvas canvas = new Canvas();
    public GraphicsContext gc = canvas.getGraphicsContext2D();
    public HBox toggles = new HBox();
    public HashMap<String, Graph> graphs = new HashMap<>();
    Random rng = new Random();
    public boolean[] drawAxisForUnit = new boolean[Units.values().length];
    public double[] unitMin = new double[Units.values().length]; //TODO: we probably wanna recalculate the min & max once the old entry fall out of the domain
    public double[] unitMax = new double[Units.values().length];

    public boolean drawMouse = false;
    public double mouseX = 0, mouseY = 0;

    long timeRange = 1000;

    long minTime = 0;
    long maxTime = timeRange;
    long maxEntryTime = 0;

    public MultiLineGraph() {
        canvas.setWidth(600);
        canvas.setHeight(300);
        this.getChildren().addAll(canvas, toggles);

        canvas.setOnMouseEntered(event -> { drawMouse = true; draw(); });
        canvas.setOnMouseExited(event -> { drawMouse = false; draw(); });

        canvas.setOnMouseMoved(event -> {
            mouseX = event.getX();
            mouseY = event.getY();
            draw();
        });

        TextField timeRangeBox = new TextField(Long.toString(timeRange));
        timeRangeBox.setOnAction(event -> {
            try {
                timeRange = Long.valueOf(timeRangeBox.getText());
                maxTime = Math.max(timeRange, maxEntryTime);
                minTime = maxTime - timeRange;
                draw();
            } catch (Exception e) {}
        });
        this.getChildren().add(timeRangeBox);

        draw();
    }

    public double getY(Units unit, Entry entry) {
        return getY(unit, entry.value);
    }

    public double getY(Units unit, double v) {
        switch (unit) {
            case Unitless:
                return canvas.getHeight() / 2 - v * (canvas.getHeight() / 2);
            default:
                return (canvas.getHeight() / 2) - v * (canvas.getHeight() / (unitMax[unit.ordinal()] - unitMin[unit.ordinal()]));
        }
    }

    public double getX(Entry entry) {
        return canvas.getWidth() * (entry.time - minTime) / (maxTime - minTime);
    }

    public boolean inDomain(Entry e) {
        return (minTime <= e.time) && (e.time <= maxTime);
    }

    public void draw() {
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        for(Graph g : graphs.values()) {
            if (g.enabled) {
                gc.setStroke(g.color);
                for(int i = 0; i < g.data.size() - 1; i++) {
                    Entry e1 = g.data.get(i);
                    Entry e2 = g.data.get(i + 1);

                    if(inDomain(e1) && inDomain(e2)) {
                        gc.strokeLine(getX(e1), getY(g.unit, e1), getX(e2), getY(g.unit, e2));
                    }
                }
            }
        }

        gc.setStroke(Color.BLACK);
        for(Units unit : Units.values()) {
            if(drawAxisForUnit[unit.ordinal()]) {
                double maxy = getY(unit, unitMax[unit.ordinal()]);
                gc.strokeLine(canvas.getWidth() - 10, maxy, canvas.getWidth(), maxy);

                double miny = getY(unit, unitMin[unit.ordinal()]);
                gc.strokeLine(canvas.getWidth() - 10, miny, canvas.getWidth(), miny);
            }
        }

        if(drawMouse) {
            double mouseT = (mouseX / canvas.getWidth()) * (maxTime - minTime) + minTime;

            int y_count = 0;
            for(Map.Entry<String, Graph> e : graphs.entrySet()) {
                Graph graph = e.getValue();
                gc.setStroke(e.getValue().color);
                gc.strokeText(e.getKey() + ": " + getValueAt(graph, mouseT) + " " + (graph.unit == Units.Unitless ? "" : graph.unit.toString()), 0, 20 + y_count * 20);
                y_count++;
            }

            gc.strokeText("Mouse " + mouseT, 0, 20 + y_count * 20);
        }
    }

    public double Lerp(double a, double t, double b) {
        return (1 - t) * a + t * b;
    }

    public double getValueAt(Graph g, double t) {
        for(int i = 0; i < g.data.size() - 1; i++) {
            Entry e1 = g.data.get(i);
            Entry e2 = g.data.get(i + 1);

            if(inDomain(e1) && inDomain(e2)) {
                if((e1.time <= t) && (t <= e2.time)) {
                    return Lerp(e1.value, (t - e1.time) / (e2.time - e1.time), e2.value);
                }
            }
        }

        return 0;
    }

    public void addData(String graphName, Units unit, double data, long time) {
        if(!graphs.containsKey(graphName)) {
            Graph newGraph = new Graph(Color.color(rng.nextDouble(), rng.nextDouble(), rng.nextDouble()), unit, graphName);
            Platform.runLater(() -> toggles.getChildren().add(newGraph.toggle));
            drawAxisForUnit[newGraph.unit.ordinal()] = true;
            graphs.put(graphName, newGraph);
        }

        Graph graph = graphs.get(graphName);

        unitMax[graph.unit.ordinal()] = Math.max(data, unitMax[graph.unit.ordinal()]);
        unitMin[graph.unit.ordinal()] = Math.min(data, unitMin[graph.unit.ordinal()]);

        maxEntryTime = Math.max(time, maxEntryTime);
        maxTime = Math.max(time, maxTime);
        minTime = maxTime - timeRange;

        graph.data.add(new Entry(data, time));
        //draw();
    }
}
