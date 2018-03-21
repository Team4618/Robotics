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
}
