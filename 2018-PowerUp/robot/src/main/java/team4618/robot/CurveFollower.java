package team4618.robot;

import java.util.ArrayList;
import java.util.List;

public class CurveFollower {

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
            double ds = 0.001; //this creates approx 10000 segments, thats too many

            for(double s = 0; s <= 1.0 - ds; s += ds) {
                Vector a = position(s);
                Vector b = position(s + ds);

                result.add(new LineSegment(a, b));
            }

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
            for(Vector p : points) {
                System.out.println("Bezier Point " + p.x + " : " + p.y);
            }

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
                //TODO: fix this, it doesnt generate working profiles right now
                double vl = (0.5) * (2 * h.length() - wheelbase * dtheta) * (1 / time) * subcurves.size();
                double vr = (0.5) * (2 * h.length() + wheelbase * dtheta) * (1 / time) * subcurves.size();
                result.add(new DifferentialTrajectory(t, vl, vr, Math.toDegrees(h.angle())));
            }
            return result;
        }
    }


    public static class LineSegment {
        public Vector a, b;
        public LineSegment(Vector start, Vector end) { a = start; b = end; }

        public double length() { return Math.sqrt((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y)); }
        public Vector lerp(double s) { return new Vector((1 - s) * a.x + s * b.x, (1 - s) * a.y + s * b.y); }
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

        public Vector positionAt(double d) {
            double currLength = 0;
            for(int i = 0; i < segments.size(); i++) {
                LineSegment segment = segments.get(i);

                if((currLength < d) && (d < (currLength + segment.length()))) {
                    double remainingDistance = d - currLength;
                    return segment.lerp(remainingDistance / segment.length());
                }

                currLength += segment.length();
            }

            return segments.get(0).a;
        }

        public ArrayList<DifferentialTrajectory> buildProfile(double nominalSpeed) {
            ArrayList<DifferentialTrajectory> result = new ArrayList<>();
            double wheelbase = 26.5 / 12.0;
            double dt = 0.01;

            for(double t = 0; t <= (length / nominalSpeed) - dt; t += dt) {
                Vector p = positionAt(nominalSpeed * t);
                Vector pn = positionAt(nominalSpeed * (t + dt));
                Vector h = new Vector((pn.x - p.x) / dt, (pn.y - p.y) / dt);

                double dtheta = 0;
                if(t > 0) {
                    Vector lp = positionAt(nominalSpeed * (t - dt));
                    Vector lasth = new Vector((p.x - lp.x) / dt, (p.y - lp.y) / dt);

                    dtheta = (h.angle() - lasth.angle()) / dt;
                    System.out.println(lasth.angle() + " to " + h.angle() + " dtheta:" + dtheta);
                }

                double vl = (0.5) * (2 * h.length() - wheelbase * dtheta);
                double vr = (0.5) * (2 * h.length() + wheelbase * dtheta);
                result.add(new DifferentialTrajectory(t, vl, vr, Math.toDegrees(h.angle())));
            }

            return result;
        }
    }
}
