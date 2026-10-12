package com.example.mini_project.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * A fixed point on the canvas that wire segments can attach to.
 * Used for two purposes:
 *   1. Free endpoint — a wire that ends in empty space (isJunction = false, connectedSegments.size() == 1)
 *   2. Junction      — where three or more wire segments meet (isJunction = true)
 *
 * Unlike Port (which moves with its parent component), a PointNode has absolute
 * canvas coordinates that never change unless the user explicitly moves it.
 */
public class PointNode {

    private static int nextId = 1;

    private final String id;
    private double x;
    private double y;

    /** True when 3 or more WireSegments are attached here — renders as a filled dot. */
    private boolean junction;

    /** All WireSegments that have this node as either their start or end point. */
    private final List<WireSegment> connectedSegments = new ArrayList<>();

    public PointNode(double x, double y) {
        this.id = "node-" + (nextId++);
        this.x  = x;
        this.y  = y;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String getId() { return id; }

    public double getX() { return x; }
    public double getY() { return y; }

    public boolean isJunction() { return junction; }

    public List<WireSegment> getConnectedSegments() { return connectedSegments; }

    // ── Mutators ──────────────────────────────────────────────────────────────

    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }

    public void addSegment(WireSegment seg) {
        if (!connectedSegments.contains(seg)) {
            connectedSegments.add(seg);
        }
        // A junction requires 3+ segments meeting here
        junction = connectedSegments.size() >= 3;
    }

    public void removeSegment(WireSegment seg) {
        connectedSegments.remove(seg);
        junction = connectedSegments.size() >= 3;
    }
}
