package team4618.dashboard.autonomous;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import team4618.dashboard.components.FieldTopdown;
import team4618.dashboard.pages.AutonomousPage;

import java.util.ArrayList;
import java.util.List;

public class DriveCurve extends DriveManeuver {
    public ArrayList<ControlPoint> controlPoints = new ArrayList<>();
    public double accelTime = 1;
    public double deccelTime = 1;
    public double speed = 4;

    public DriveCurve(PathNode s, PathNode e) {
        beginning = s;
        s.outPaths.add(this);
        end = e;
        e.inPath = this;
        color = Color.BLUE;
    }

    public static class Vector {
        public double x, y;
        public Vector(double x, double y) { this.x = x; this.y = y; }
        public double length() { return Math.sqrt(x * x + y * y); }
        public double angle() { return Math.atan2(y, x); }
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

        public List<LineSegment> toSegments() {
            ArrayList<LineSegment> result = new ArrayList<>();
            double ds = 0.0001; //this creates approx 10000 segments, thats too many

            for(double s = 0; s <= 1.0 - ds; s += ds) {
                Vector a = position(s);
                Vector b = position(s + ds);

                result.add(new LineSegment(a, b));
            }

            return result;
        }
    }

    public static class DifferentialTrajectory {
        public double t, vl, pl, vr, pr, angle;
        public DifferentialTrajectory (double t, double vell, double posl, double velr, double posr, double angle) {
            this.t = t;
            this.vl = vell;
            this.pl = posl;
            this.vr = velr;
            this.pr = posr;
            this.angle = angle;
        }
    }

    public static class LineSegment {
        public Vector a, b;
        public LineSegment(Vector start, Vector end) { a = start; b = end; }

        public double length() { return Math.sqrt((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y)); }
        public Vector lerp(double s) { return new Vector((1 - s) * a.x + s * b.x, (1 - s) * a.y + s * b.y); }
    }

    public static double lerp(double a, double t, double b) { return (1 - t) * a + t * b; }

    public static double profileTime(double tAccel, double tDeccel, double distance, double speed) {
        return (distance / speed) + 0.5 * (tAccel + tDeccel);
    }
    public static double trapazoidalProfile(double t, double tAccel, double tDeccel, double distance, double speed) {
        double tTotal = profileTime(tAccel, tDeccel, distance, speed);
        double deccelTime = tTotal - tDeccel;

        //TODO: use smoothstep instead of lerp, rewrite profileTime when we change lerp out
        if(t >= deccelTime) {
            return lerp(speed, (t - deccelTime) / tDeccel, 0);
        } else if(t <= tAccel) {
            return lerp(0, t / tAccel, speed);
        }

        return speed;
    }

    public static class SegmentedPath {
        public ArrayList<LineSegment> segments = new ArrayList<>();
        public double length = 0;

        public SegmentedPath(List<Vector> points) {
            int i;
            for (i = 0; i < points.size() - 3; i += 2) {
                Vector p0 = points.get(i);
                Vector p1 = points.get(i + 1);
                Vector p2 = points.get(i + 2);

                Vector mid = new Vector((p1.x + p2.x) / 2, (p1.y + p2.y) / 2);
                points.add(i + 2, mid);
                segments.addAll(new QuadraticBezierCurve(p0, p1, mid).toSegments());
            }

            segments.addAll(new QuadraticBezierCurve(points.get(i), points.get(i + 1), points.get(i + 2)).toSegments());

            for(LineSegment segment : segments) {
                length += segment.length();
            }
        }

        //TODO: fix this lookup, it creates artifacts at the beginning and end of paths
        public Vector positionAt(double d) {
            d = Math.max(0, Math.min(d, length));

            double currLength = 0;
            Vector currResult = segments.get(0).a;
            for(int i = 0; i < segments.size(); i++) {
                LineSegment segment = segments.get(i);

                if((currLength <= d) && (d < (currLength + segment.length()))) {
                    double remainingDistance = d - currLength;
                    return segment.lerp(remainingDistance / segment.length());
                }

                currResult = segment.b;
                currLength += segment.length();
            }

            return currResult;
        }

        public ArrayList<DifferentialTrajectory> buildProfile(double tAccel, double tDeccel, double nominalSpeed, boolean backwards) {
            ArrayList<DifferentialTrajectory> result = new ArrayList<>();
            double wheelbase = 26.5 / 12.0;
            double dt = 0.01;

            double pl = 0;
            double pr = 0;
            double distance = 0;

            double tTotal = profileTime(tAccel, tDeccel, length, nominalSpeed);
            Vector lp = null;
            for(double t = 0; distance <= length; t += dt) {
                double speed = trapazoidalProfile(t, tAccel, tDeccel, length, nominalSpeed);
                Vector p = positionAt(distance);
                Vector pn = positionAt(distance + speed * dt);
                Vector h = new Vector((pn.x - p.x) / dt, (pn.y - p.y) / dt);

                double dtheta = 0;
                if(lp != null) {
                    Vector lasth = new Vector((p.x - lp.x) / dt, (p.y - lp.y) / dt);
                    dtheta = (h.angle() - lasth.angle()) / dt;
                }

                System.out.println("s " + h.length() + " a =" + dtheta + " @ t%=" + (t/tTotal) + " d%=" + (distance/length));

                double vl = (backwards ? -1 : 1) * (0.5) * (2 * h.length() - wheelbase * dtheta);
                pl += vl * dt;
                double vr = (backwards ? -1 : 1) * (0.5) * (2 * h.length() + wheelbase * dtheta);
                pr += vr * dt;
                distance += speed * dt;
                if(speed != 0)
                    lp = p;
                result.add(new DifferentialTrajectory(t, vl, pl, vr, pr, Math.toDegrees(h.angle())));
            }

            return result;
        }
    }

    //TODO: remove this or use it for rendering only
    public static class BezierCurve {
        public ArrayList<QuadraticBezierCurve> subcurves = new ArrayList<>();

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
    }

    public BezierCurve generateCurve() {
        ArrayList<Vector> points = new ArrayList<>();
        points.add(new Vector(beginning.x / 12.0, beginning.y / 12.0));
        controlPoints.forEach(c -> points.add(new Vector(c.x / 12.0, c.y / 12.0)));
        points.add(new Vector(end.x / 12.0, end.y / 12.0));

        return new BezierCurve(points);
    }

    public SegmentedPath generateSegmentedPath() {
        ArrayList<Vector> points = new ArrayList<>();
        points.add(new Vector(beginning.x / 12.0, beginning.y / 12.0));
        controlPoints.forEach(c -> points.add(new Vector(c.x / 12.0, c.y / 12.0)));
        points.add(new Vector(end.x / 12.0, end.y / 12.0));

        return new SegmentedPath(points);
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

            /*
            SegmentedPath segmentedPath = generateSegmentedPath();
            for(int i = 0; i < segmentedPath.segments.size(); i++) {
                LineSegment segment = segmentedPath.segments.get(i);
                gc.strokeLine(12 * segment.a.x, 12 * segment.a.y, 12 * segment.b.x, 12 * segment.b.y);
            }
            */
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
        paramValues.add(accelTime);
        paramValues.add(deccelTime);
        paramValues.add((backwards ? -1 : 1) * speed);

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