package team4618.dashboard.autonomous;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import team4618.dashboard.components.FieldTopdown;
import team4618.dashboard.pages.AutonomousPage;

import java.util.ArrayList;
import java.util.List;

public class DriveCurve extends DriveManeuver {
    public ArrayList<ControlPoint> controlPoints = new ArrayList<>();
    public double time = 4;
    public double tHeading = 0;

    public static class Vector {
        public double x, y;
        public Vector(double x, double y) { this.x = x; this.y = y; }
        public double length() { return Math.sqrt(x * x + y * y); }
        public double angle() { return Math.atan2(y, x); }
    }

    //TODO: backwards curves
    public DriveCurve(PathNode s, PathNode e) {
        beginning = s;
        s.outPaths.add(this);
        end = e;
        e.inPath = this;
        color = Color.BLUE;
    }

    public static class QuadraticBezierCurve {
        Vector p0, p1, p2;

        public QuadraticBezierCurve(Vector p0, Vector p1, Vector p2) {
            this.p0 = p0;
            this.p1 = p1;
            this.p2 = p2;
        }

        public Vector position(double t) {
            Vector result = new Vector(0, 0);
            result.x = Math.pow(1 - t, 2) * p0.x + 2 * t * (1 - t) * p1.x + Math.pow(t, 2) * p2.x;
            result.y = Math.pow(1 - t, 2) * p0.y + 2 * t * (1 - t) * p1.y + Math.pow(t, 2) * p2.y;
            return result;
        }

        public Vector heading(double t) {
            Vector result = new Vector(0, 0);
            result.x = 2 * (1 - t) * (p1.x - p0.x) + 2 * t * (p2.x - p1.x);
            result.y = 2 * (1 - t) * (p1.y - p0.y) + 2 * t * (p2.y - p1.y);
            return result;
        }
    }

    public static class DifferentialTrajectory {
        public double t, l, r, angle;
        public DifferentialTrajectory (double t, double l, double r, double angle) {
            this.t = t;
            this.l = l;
            this.r = r;
            this.angle = angle;
        }
    }

    public static class BezierCurve {
        ArrayList<QuadraticBezierCurve> subcurves = new ArrayList<>();

        public BezierCurve(List<Vector> points) {
            int i;
            for (i = 0; i < points.size() - 3; i += 2) {
                Vector p0 = points.get(i);
                Vector p1 = points.get(i + 1);
                Vector p2 = points.get(i + 2);

                Vector mid = new Vector((p1.x + p2.x) / 2, (p1.y + p2.y) / 2);
                points.add(i + 2, mid);
                subcurves.add(new QuadraticBezierCurve(p0, p1, mid));
            }

            subcurves.add(new QuadraticBezierCurve(points.get(i), points.get(i + 1), points.get(i + 2)));
        }

        public Vector position(double t) {
            double scaledT = Math.max(Math.min(t, 1.0), 0) * subcurves.size();
            int i = Math.min((int) Math.floor(scaledT), subcurves.size() - 1);
            return subcurves.get(i).position(scaledT - i);
        }

        public Vector heading(double t) {
            double scaledT = Math.max(Math.min(t, 1.0), 0) * subcurves.size();
            int i = Math.min((int) Math.floor(scaledT), subcurves.size() - 1);
            return subcurves.get(i).heading(scaledT - i);
        }

        public ArrayList<DifferentialTrajectory> buildProfile(double time) {
            ArrayList<DifferentialTrajectory> result = new ArrayList<>();
            double wheelbase = 26.5 / 12.0;
            double dt = 0.01;

            for(double t = 0; t <= 1.0 * time; t += dt) {
                Vector p = position(t / time);
                Vector h = heading(t / time);
                double dtheta = 0;
                if(t > 0) {
                    Vector lasth = heading((t / time) - dt);
                    dtheta = (h.angle() - lasth.angle()) / dt;
                }
                double vl = (0.5) * (2 * h.length() - wheelbase * dtheta) * (1 / time);
                double vr = (0.5) * (2 * h.length() + wheelbase * dtheta) * (1 / time);
                result.add(new DifferentialTrajectory(t, vl, vr, Math.toDegrees(h.angle())));
            }
            return result;
        }
    }

    public BezierCurve generateCurve() {
        ArrayList<Vector> points = new ArrayList<>();
        points.add(new Vector(beginning.x / 12.0, beginning.y / 12.0));
        controlPoints.forEach(c -> points.add(new Vector(c.x / 12.0, c.y / 12.0)));
        points.add(new Vector(end.x / 12.0, end.y / 12.0));

        return new BezierCurve(points);
    }

    public void draw(GraphicsContext gc, FieldTopdown field) {
        if(visible) {
            gc.setStroke(this == field.hot ? Color.RED : color);
            if (AutonomousPage.selected == this) gc.setStroke(Color.LAVENDERBLUSH);

            BezierCurve curve = generateCurve();
            for(double t = 0; t <= 1.0; t += 0.01) {
                Vector a = curve.position(t);
                Vector b = curve.position(t + 0.01);
                gc.strokeLine(12 * a.x, 12 * a.y, 12 * b.x, 12 * b.y);
            }

            {
                Vector p = curve.position(tHeading);
                Vector heading = curve.heading(tHeading);

                gc.setFill(Color.BLACK);
                gc.fillOval(-2.5 + 12 * p.x, -2.5 + 12 * p.y, field.getPixelPerInch() * 5, field.getPixelPerInch() * 5);

                gc.setStroke(Color.BLACK);
                gc.strokeLine(12 * p.x, 12 * p.y, 12 * p.x + 12*0.5*heading.x, 12 * p.y + 12*0.5*heading.y);
            }
        }
    }

    public boolean contains(double x, double y) {
        Vector mouse = new Vector(x, y);

        BezierCurve curve = generateCurve();
        for(double t = 0; t <= 1.0; t += 0.01) {
            Vector p = curve.position(t);
            if(Math.sqrt((12*p.x - mouse.x) * (12*p.x - mouse.x) + (12*p.y - mouse.y) * (12*p.y - mouse.y)) < 2)
                return true;
        }
        return false;
    }

    public void remove(List<FieldTopdown.Drawable> overlay) {
        overlay.remove(this);
        for (ControlPoint c : controlPoints) {
            overlay.remove(c);
        }
    }

    public void addCommandsTo(List<AutonomousCommand> commands) {
        BezierCurve curve = generateCurve();
        Vector initialHeading = curve.heading(0);
        AutonomousCommand turnCommand = new AutonomousCommand(AutonomousCommandTemplate.templates.get("Drive:turnToAngle"));
        turnCommand.parameterValues[0] = AutonomousPage.canonicalizeAngle(Math.toDegrees(initialHeading.angle()) + (backwards ? 180 : 0));
        turnCommand.parameterValues[1] = 2;
        turnCommand.parameterValues[2] = 1;
        turnCommand.parameterValues[3] = 10;
        commands.add(turnCommand);

        ArrayList<Double> paramValues = new ArrayList<>();
        paramValues.add(time);

        for(ControlPoint c : controlPoints) {
            paramValues.add((c.x - beginning.x) / 12.0);
            paramValues.add((c.y - beginning.y) / 12.0);
        }

        paramValues.add((end.x - beginning.x) / 12.0);
        paramValues.add((end.y - beginning.y) / 12.0);

        AutonomousCommand curveCommand = new AutonomousCommand(AutonomousCommandTemplate.templates.get("Drive:driveCurve"));
        curveCommand.parameterValues = new double[paramValues.size()];
        for(int i = 0; i < paramValues.size(); i++)
            curveCommand.parameterValues[i] = paramValues.get(i);
        commands.add(curveCommand);
    }

    public static class ControlPoint extends FieldTopdown.Drawable {
        public ControlPoint(double nX, double nY) { x = nX; y = nY; }

        public void draw(GraphicsContext gc, FieldTopdown field) {
            gc.setFill(this == field.hot ? Color.RED : Color.DARKRED);
            gc.fillOval(-5, -5, field.getPixelPerInch() * 10, field.getPixelPerInch() * 10);
        }

        public boolean contains(double nX, double nY) { return Math.sqrt((x - nX) * (x - nX) + (y - nY) * (y - nY)) < 10; }
        public void drag(double nX, double nY) { x = nX; y = nY; }
    }
}