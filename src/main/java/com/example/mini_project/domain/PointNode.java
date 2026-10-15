package com.example.mini_project.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * A fixed point on the canvas that wire segments can attach to.
 * Used for two purposes:
 * 1. Free endpoint — a wire that ends in empty space (isJunction = false, connectedSegments.size() == 1)
 * 2. Junction      — where three or more wire segments meet (isJunction = true)
 * <p>
 * UNLIKE PORT (WHICH MOVES WITH ITS PARENT COMPONENT), A "POINTNODE" HAS ABSOLUTE
 * CANVAS COORDINATES THAT NEVER CHANGE UNLESS THE USER EXPLICITLY MOVES IT.
 */
public class PointNode {

    private static int nextId = 1;

    private final String id;
    private double x;
    private double y;

    // True when 3 or more WireSegments are attached here(renders as a filled dot)
    private boolean junction;

    // List of all wire segments connected to this pointnode
    // For these wire segments this could act as input or output (or both) node or port
    private final List<WireSegment> connectedSegments = new ArrayList<>();

    // Why 2 constructors?
    // First -> when PointNode is going to be created
    // Second -> when we know the id and want to create a node out of it
    public PointNode(double x, double y) {
        this("node-" + (nextId++), x, y);
    }

    public PointNode(String id, double x, double y) {
        this.id = id;
        this.x = x;
        this.y = y;
    }

    public static void resetId() {
        nextId = 1;
    }

    public static void updateNextId(int id) {
        if (id >= nextId) nextId = id + 1;
    }

    public String getId() {
        return id;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public boolean isJunction() {
        return junction;
    }

    public List<WireSegment> getConnectedSegments() {
        return connectedSegments;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    // Adding a wire segment into connectedSegments and updating junction(if needed)
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
