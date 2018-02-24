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
    public enum Units { Feet, FeetPerSecond, Degrees, DegreesPerSecond, Seconds, Unitless, Percent }

    public static class Entry {
        double value;
        double time;
        public Entry(double v, double t) { value = v; time = t; }
    }

    public class Graph {
        public ArrayList<Entry> data = new ArrayList<>();
        public Color color;
        public Units unit;
        public Button toggle;
        public boolean enabled = true;

        public double minValue = 0;
        public double maxValue = 0;

        public Graph(Color c, Units u, String name) {
            color = c;
            unit = u;
            toggle = new Button(name);
            toggle.setOnAction(event -> { enabled = !enabled; draw(); });
            toggle.setBackground(new Background(new BackgroundFill(color, new CornerRadii(0), new Insets(0))));
        }

        public void recalculateRange() {
            minValue = 0;
            maxValue = 0;

            for(Entry e : data) {
                if(inDomain(e)) {
                    minValue = Math.min(minValue, e.value);
                    maxValue = Math.max(maxValue, e.value);
                }
            }
        }

        public void add(Entry e) {
            data.add(e);
            recalculateRange();
        }
    }

    public void recalculateRanges() {
        double[] mins = new double[Units.values().length];
        for(int i = 0; i < mins.length; i++) { mins[i] = 0; }
        double[] maxs = new double[Units.values().length];
        for(int i = 0; i < maxs.length; i++) { maxs[i] = 0; }

        for(Graph g : graphs.values()) {
            g.recalculateRange();
            mins[g.unit.ordinal()] = Math.min(mins[g.unit.ordinal()], g.minValue);
            maxs[g.unit.ordinal()] = Math.max(maxs[g.unit.ordinal()], g.maxValue);
        }

        for(Graph g : graphs.values()) {
            if((g.unit != Units.Percent) && (g.unit != Units.Unitless)) {
                g.minValue = mins[g.unit.ordinal()];
                g.maxValue = maxs[g.unit.ordinal()];
            }
        }
    }

    public Canvas canvas = new Canvas();
    public GraphicsContext gc = canvas.getGraphicsContext2D();
    public HBox toggles = new HBox();
    public HashMap<String, Graph> graphs = new HashMap<>();
    Random rng = new Random();

    public boolean drawMouse = false;
    public double mouseX = 0, mouseY = 0;

    public double minTime = 0;
    public double maxTime = 0;

    boolean automaticMaxTime = true;

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

        draw();
    }

    public static double mapFromTo(double t, double a, double b, double x, double y) {
        return (x - y) * ((b - t) / (b - a)) + y;
    }

    public double getY(Graph g, double v) {
        double minY = canvas.getHeight() - 10;
        double maxY = 10;

        switch (g.unit) {
            case Percent:
                return mapFromTo(v, -1, 1, minY, maxY);
            case Unitless:
                return mapFromTo(v, g.minValue, g.maxValue, minY, maxY);
            default:
                return mapFromTo(v, g.minValue, g.maxValue, minY, maxY);
        }
    }

    public double getX(Entry entry) {
        return mapFromTo(entry.time, minTime, maxTime, 0, canvas.getWidth()); //canvas.getWidth() * (entry.time - minTime) / (maxTime - minTime);
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
                        gc.strokeLine(getX(e1), getY(g, e1.value), getX(e2), getY(g, e2.value));
                    }
                }
            }
        }

        if(drawMouse) {
            double mouseT = (mouseX / canvas.getWidth()) * (maxTime - minTime) + minTime;

            int y_count = 0;
            for(Map.Entry<String, Graph> e : graphs.entrySet()) {
                Graph graph = e.getValue();
                gc.setStroke(e.getValue().color);

                String unitSuffix = (graph.unit == Units.Unitless) ? "" : (" " + graph.unit.toString());
                if(graph.unit == Units.Percent) unitSuffix = "%";

                gc.strokeText((graph.enabled ? "+" : "-") + e.getKey() + ": " + getValueAt(graph, mouseT) + unitSuffix, 0, 20 + y_count * 20);
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

    public void addData(String graphName, Units unit, double data, double time) {
        if(graphs.values().size() == 0) {
            minTime = time;
            maxTime = minTime;
        }

        if(!graphs.containsKey(graphName)) {
            Graph newGraph = new Graph(Color.color(rng.nextDouble(), rng.nextDouble(), rng.nextDouble()), unit, graphName);
            Platform.runLater(() -> toggles.getChildren().add(newGraph.toggle));
            graphs.put(graphName, newGraph);
        }

        Graph graph = graphs.get(graphName);

        if(automaticMaxTime) {
            maxTime = Math.max(time, maxTime);
        }

        graph.add(new Entry(data, time));
        recalculateRanges();
    }
}
