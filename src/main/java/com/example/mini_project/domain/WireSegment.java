package com.example.mini_project.domain;

/**
 * One straight orthogonal wire segment between exactly two endpoints.
 * A segment is always a single straight line: either purely horizontal (y1 == y2)
 * or purely vertical (x1 == x2).
 * <p>
 * Each endpoint is EITHER a {@link Port} (attached to a component) OR a
 * {@link PointNode} (a free end, corner bend, or junction in empty canvas space).
 * Exactly one of the two endpoint fields is non-null for each end.
 */
public class WireSegment {

    private static int nextId = 1;

    private final String id;

    // Start Endpoint
    // Exactly only one has a non null value
    private Port startPort;   // null if start is a PointNode
    private PointNode startNode;   // null if start is a Port

    // End Endpoint
    // Exactly only one has a non null value
    private Port endPort;     // null if end is a PointNode
    private PointNode endNode;     // null if end is a Port

    // Absolute coordinates
    private double x1, y1; // start
    private double x2, y2; // end

    private SignalState state = SignalState.UNKNOWN;

    // Why 2 constructors?
    // Cause there are times when only want to create a wire segment and use it for small purpose
    // The first constructor allows you to do it
    // The second constructor used strictly when you have an id and want to create a wire segment
    public WireSegment(double x1, double y1, double x2, double y2) {
        this("wire-" + (nextId++), x1, y1, x2, y2);
    }

    public WireSegment(String id, double x1, double y1, double x2, double y2) {
        this.id = id != null ? id : "wire-" + (nextId++);
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    public static void resetId() {
        nextId = 1;
    }

    public static void updateNextId(int id) {
        if (id >= nextId) nextId = id + 1;
    }

    // Wire segment from PORT to PORT
    public static WireSegment portToPort(Port start, double x1, double y1, Port end, double x2, double y2) {
        WireSegment s = new WireSegment(x1, y1, x2, y2);
        s.startPort = start;
        s.endPort = end;
        return s;
    }

    // Wire segment from port to pointnode
    public static WireSegment portToNode(Port start, double x1, double y1, PointNode end) {
        WireSegment s = new WireSegment(x1, y1, end.getX(), end.getY());
        s.startPort = start;
        s.endNode = end;
        return s;
    }

    // Wire segment from pointnode to pointnode
    public static WireSegment nodeToNode(PointNode start, PointNode end) {
        WireSegment s = new WireSegment(start.getX(), start.getY(), end.getX(), end.getY());
        s.startNode = start;
        s.endNode = end;
        return s;
    }

    // Wire segment from pointnode to port
    public static WireSegment nodeToPort(PointNode start, Port end, double x2, double y2) {
        WireSegment s = new WireSegment(start.getX(), start.getY(), x2, y2);
        s.startNode = start;
        s.endPort = end;
        return s;
    }

    // Orientation
    public boolean isHorizontal() {
        return Math.abs(y1 - y2) < 0.001;
    }

    public boolean isVertical() {
        return Math.abs(x1 - x2) < 0.001;
    }

    // Getters
    public String getId() {
        return id;
    }

    public double getX1() {
        return x1;
    }

    public double getY1() {
        return y1;
    }

    public double getX2() {
        return x2;
    }

    public double getY2() {
        return y2;
    }

    public Port getStartPort() {
        return startPort;
    }

    public PointNode getStartNode() {
        return startNode;
    }

    public Port getEndPort() {
        return endPort;
    }

    public PointNode getEndNode() {
        return endNode;
    }

    public void setStartPort(Port p) {
        this.startPort = p;
        this.startNode = null;
    }

    public void setStartNode(PointNode n) {
        this.startNode = n;
        this.startPort = null;
    }

    public void setEndPort(Port p) {
        this.endPort = p;
        this.endNode = null;
    }

    public void setEndNode(PointNode n) {
        this.endNode = n;
        this.endPort = null;
    }

    // Signal state
    public SignalState getState() {
        return state;
    }

    public void setState(SignalState state) {
        this.state = state;
    }

    // Setters -> used when we set or extend a wire segment
    public void setX1(double x1) {
        this.x1 = x1;
    }

    public void setY1(double y1) {
        this.y1 = y1;
    }

    public void setX2(double x2) {
        this.x2 = x2;
    }

    public void setY2(double y2) {
        this.y2 = y2;
    }

    // Returns the minimum distance from canvas point (px, py) to this straight segment
    public double distanceToPoint(double px, double py) {
        return pointToSegmentDist(px, py, x1, y1, x2, y2);
    }

    public static double pointToSegmentDist(double px, double py,
                                            double ax, double ay,
                                            double bx, double by) {
        double dx = bx - ax, dy = by - ay;
        double lenSq = dx * dx + dy * dy;
        if (lenSq == 0) return Math.hypot(px - ax, py - ay);
        double t = Math.max(0, Math.min(1, ((px - ax) * dx + (py - ay) * dy) / lenSq));
        return Math.hypot(px - (ax + t * dx), py - (ay + t * dy));
    }
}
