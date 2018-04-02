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

        public int startIndex = 0;
        public int endIndex = 0;

        public Graph(Color c, Units u, String name) {
            color = c;
            unit = u;
            toggle = new Button(name);
            toggle.setOnAction(event -> enabled = !enabled);
            toggle.setBackground(new Background(new BackgroundFill(color, new CornerRadii(0), new Insets(0))));
        }

        public void recalculateRange() {
            minValue = Float.MAX_VALUE;
            maxValue = Float.MIN_VALUE;
            startIndex = data.size() - 1;
            endIndex = Integer.MIN_VALUE;

            for(int i = 0; i < data.size(); i++) {
                Entry e = data.get(i);
                if(inDomain(e)) {
                    minValue = Math.min(minValue, e.value);
                    maxValue = Math.max(maxValue, e.value);
                    startIndex = Math.min(startIndex, i);
                    endIndex = Math.max(endIndex, i);
                }
            }

            System.out.println(toggle.getText() + " " + startIndex + " to " + endIndex);
        }

        public double getMinTime() {
            return data.get(0).time;
        }

        public double getMaxTime() {
            return data.get(data.size() - 1).time;
        }

        public boolean isOnLine(Entry e, Entry a, Entry b) {
            double atob = Math.sqrt((b.value - a.value) * (b.value - a.value) + (b.time - a.time) * (b.time - a.time));
            double atoe = Math.sqrt((e.value - a.value) * (e.value - a.value) + (e.time - a.time) * (e.time - a.time));
            double etob = Math.sqrt((b.value - e.value) * (b.value - e.value) + (b.time - e.time) * (b.time - e.time));
            return false; //Math.abs(atoe + etob - atob) == 0.0;
        }

        public void add(Entry e) {
            if(data.isEmpty() || (e.time > getMaxTime())) {
                if((data.size() > 1) && isOnLine(data.get(data.size() - 1), data.get(data.size() - 1), e)) {
                    data.set(data.size() - 1, e);
                } else {
                    data.add(e);
                }

                if(automaticMaxTime) {
                    endIndex = data.size() - 1;
                    minValue = Math.min(minValue, e.value);
                    maxValue = Math.max(maxValue, e.value);
                }
            }
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
    public HBox controls = new HBox();
    public HashMap<String, Graph> graphs = new HashMap<>();
    Random rng = new Random();

    public boolean drawMouse = false;
    public double mouseX = 0, mouseY = 0;

    public long startTime;
    public double minTime = 0;
    public double maxTime = 0;

    boolean automaticMaxTime = true;

    boolean dragging = false;
    public double dragBeginT = 0;

    public MultiLineGraph() {
        canvas.setWidth(600);
        canvas.setHeight(300);

        canvas.setOnMouseEntered(event -> drawMouse = true);
        canvas.setOnMouseExited(event -> drawMouse = false);

        canvas.setOnMouseMoved(event -> {
            mouseX = event.getX();
            mouseY = event.getY();
        });

        canvas.setOnMouseDragged(event -> {
            mouseX = event.getX();
            mouseY = event.getY();
        });

        canvas.setOnMousePressed(evt -> {
            dragging = true;
            dragBeginT = (evt.getX() / canvas.getWidth()) * (maxTime - minTime) + minTime;
            System.out.println("Click " + dragBeginT);
        });

        canvas.setOnMouseReleased(evt -> {
            dragging = false;
            minTime = dragBeginT;
            maxTime = (evt.getX() / canvas.getWidth()) * (maxTime - minTime) + minTime;
            automaticMaxTime = false;
            recalculateRanges();
            System.out.println("Released");
        });

        Button clear_graph = new Button("Clear Graph");
        clear_graph.setOnAction(event -> {
            graphs.clear();
            toggles.getChildren().clear();
        });

        Button resetGraphMinTime = new Button("Reset Min Time");
        resetGraphMinTime.setOnAction(event -> {
            minTime = (System.currentTimeMillis() - startTime) / 1000.0;
            maxTime = minTime;
            automaticMaxTime = true;
            recalculateRanges();
        });

        Button expandGraphTime = new Button("Full Time");
        expandGraphTime.setOnAction(event -> {
            minTime = 0;
            maxTime = 0;
            automaticMaxTime = true;

            for(Graph g : graphs.values()) {
                minTime = Math.min(g.getMinTime(), minTime);
                maxTime = Math.max(g.getMaxTime(), maxTime);
            }

            recalculateRanges();
        });
        controls.getChildren().addAll(clear_graph, resetGraphMinTime, expandGraphTime);
        this.getChildren().addAll(canvas, toggles, controls);

        startTime = System.currentTimeMillis();
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
            if (g.enabled && (g.endIndex > g.startIndex)) {
                gc.setStroke(g.color);
                for(int i = g.startIndex; i <= g.endIndex - 1; i++) {
                    Entry e1 = g.data.get(i);
                    Entry e2 = g.data.get(i + 1);

                    /*
                    gc.setFill(Color.BLACK);
                    gc.fillOval(getX(e1) - 2, getY(g, e1.value) - 2, 4, 4);
                    gc.fillOval(getX(e2) - 2, getY(g, e2.value) - 2, 4, 4);
                    */

                    if(inDomain(e1) && inDomain(e2)) {
                        gc.strokeLine(getX(e1), getY(g, e1.value), getX(e2), getY(g, e2.value));
                    }
                }
            }
        }

        if(drawMouse) {
            double mouseT = (mouseX / canvas.getWidth()) * (maxTime - minTime) + minTime;

            if(dragging) {
                //TODO: fix this
                gc.setFill(Color.rgb(0, 0, 0, 0.2));
                double dragBeginX = mapFromTo(dragBeginT, minTime, maxTime, 0, canvas.getWidth());

                gc.fillRect(dragBeginX, 0, 10, canvas.getHeight());
                gc.fillRect(mouseX, 0, 10, canvas.getHeight());
            }

            int y_count = 0;
            for(Map.Entry<String, Graph> e : graphs.entrySet()) {
                Graph graph = e.getValue();
                gc.setStroke(e.getValue().color);

                String unitSuffix = (graph.unit == Units.Unitless) ? "" : (" " + graph.unit.toString());
                if(graph.unit == Units.Percent) unitSuffix = "%";

                double value = 0;
                for(int i = graph.startIndex; i <= graph.endIndex - 1; i++) {
                    Entry e1 = graph.data.get(i);
                    Entry e2 = graph.data.get(i + 1);

                    if(inDomain(e1) && inDomain(e2)) {
                        if((e1.time <= mouseT) && (mouseT <= e2.time)) {
                            value = Lerp(e1.value, (mouseT - e1.time) / (e2.time - e1.time), e2.value);
                        }
                    }
                }

                gc.strokeText((graph.enabled ? "+" : "-") + e.getKey() + ": " + value + unitSuffix, 0, 20 + y_count * 20);
                y_count++;
            }

            gc.strokeText("Mouse " + mouseT, 0, 20 + y_count * 20);
        }
    }

    public double Lerp(double a, double t, double b) {
        return (1 - t) * a + t * b;
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
    }
}
