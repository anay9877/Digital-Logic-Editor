package com.example.mini_project.domain;

import javafx.geometry.Point2D;

/**
 * One manually drawn wire segment between exactly two endpoints.
 *
 * Each endpoint is EITHER a {@link Port} (attached to a component) OR a
 * {@link PointNode} (a free end or junction in empty canvas space).
 * Exactly one of the two endpoint fields is non-null for each end.
 *
 * The wire is always orthogonal with at most one 90° bend.
 * The bend direction (horizontal-first vs vertical-first) is determined
 * by the user's initial drag direction and stored here permanently.
 */
public class WireSegment {

    private static int nextId = 1;

    private final String id;

    // ── Start endpoint (exactly one of these is non-null) ─────────────────────
    private Port      startPort;   // null if start is a PointNode
    private PointNode startNode;   // null if start is a Port

    // ── End endpoint (exactly one of these is non-null) ───────────────────────
    private Port      endPort;     // null if end is a PointNode
    private PointNode endNode;     // null if end is a Port

    /**
     * Cached absolute canvas coordinates for both endpoints.
     * These must be kept in sync whenever the attached component or node moves.
     */
    private double x1, y1; // start
    private double x2, y2; // end

    /**
     * If true  → the wire goes horizontal first, then vertical  (bend at (x2, y1)).
     * If false → the wire goes vertical first, then horizontal  (bend at (x1, y2)).
     */
    private boolean horizontalFirst;

    // ── Constructor helpers ───────────────────────────────────────────────────

    private WireSegment(double x1, double y1, double x2, double y2, boolean horizontalFirst) {
        this.id = "wire-" + (nextId++);
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.horizontalFirst = horizontalFirst;
    }

    /** Segment from a Port to another Port. */
    public static WireSegment portToPort(Port start, double x1, double y1,
                                          Port end,   double x2, double y2,
                                          boolean horizontalFirst) {
        WireSegment s = new WireSegment(x1, y1, x2, y2, horizontalFirst);
        s.startPort = start;
        s.endPort   = end;
        return s;
    }

    /** Segment from a Port to a PointNode (free end or junction). */
    public static WireSegment portToNode(Port start, double x1, double y1,
                                          PointNode end, boolean horizontalFirst) {
        WireSegment s = new WireSegment(x1, y1, end.getX(), end.getY(), horizontalFirst);
        s.startPort = start;
        s.endNode   = end;
        return s;
    }

    /** Segment from a PointNode to another PointNode. */
    public static WireSegment nodeToNode(PointNode start, PointNode end, boolean horizontalFirst) {
        WireSegment s = new WireSegment(start.getX(), start.getY(),
                                         end.getX(),   end.getY(), horizontalFirst);
        s.startNode = start;
        s.endNode   = end;
        return s;
    }

    /** Segment from a PointNode to a Port. */
    public static WireSegment nodeToPort(PointNode start, Port end,
                                          double x2, double y2, boolean horizontalFirst) {
        WireSegment s = new WireSegment(start.getX(), start.getY(), x2, y2, horizontalFirst);
        s.startNode = start;
        s.endPort   = end;
        return s;
    }

    // ── Routing ───────────────────────────────────────────────────────────────

    /**
     * Returns the single bend point for the L-shape route.
     * horizontal-first → bend at (x2, y1)
     * vertical-first   → bend at (x1, y2)
     */
    public Point2D getBendPoint() {
        if (horizontalFirst) {
            return new Point2D(x2, y1);
        } else {
            return new Point2D(x1, y2);
        }
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String getId()  { return id; }

    public double getX1()  { return x1; }
    public double getY1()  { return y1; }
    public double getX2()  { return x2; }
    public double getY2()  { return y2; }

    public boolean isHorizontalFirst() { return horizontalFirst; }

    public Port      getStartPort() { return startPort; }
    public PointNode getStartNode() { return startNode; }
    public Port      getEndPort()   { return endPort; }
    public PointNode getEndNode()   { return endNode; }

    // ── Mutators (called when endpoints move) ─────────────────────────────────

    public void setX1(double x1) { this.x1 = x1; }
    public void setY1(double y1) { this.y1 = y1; }
    public void setX2(double x2) { this.x2 = x2; }
    public void setY2(double y2) { this.y2 = y2; }

    public void setHorizontalFirst(boolean horizontalFirst) {
        this.horizontalFirst = horizontalFirst;
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    /**
     * Returns the minimum distance from canvas point (px, py) to this segment's
     * rendered polyline (two sub-segments: start→bend and bend→end).
     */
    public double distanceToPoint(double px, double py) {
        Point2D bend = getBendPoint();
        double d1 = pointToSegmentDist(px, py, x1, y1, bend.getX(), bend.getY());
        double d2 = pointToSegmentDist(px, py, bend.getX(), bend.getY(), x2, y2);
        return Math.min(d1, d2);
    }

    private static double pointToSegmentDist(double px, double py,
                                              double ax, double ay,
                                              double bx, double by) {
        double dx = bx - ax, dy = by - ay;
        double lenSq = dx * dx + dy * dy;
        if (lenSq == 0) return Math.hypot(px - ax, py - ay);
        double t = Math.max(0, Math.min(1, ((px - ax) * dx + (py - ay) * dy) / lenSq));
        return Math.hypot(px - (ax + t * dx), py - (ay + t * dy));
    }
}
