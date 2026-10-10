package com.example.mini_project.controller;

import com.example.mini_project.domain.*;
import com.example.mini_project.gui.CircuitCanvas;
import com.example.mini_project.gui.ToolbarOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditorController {

    private final Circuit circuit;
    private final Map<ComponentType, Integer> counters  = new HashMap<>();
    private final SelectionManager            selection = new SelectionManager();

    // ── Wire drawing state ────────────────────────────────────────────────────
    // These fields are managed by the wire-drawing state machine driven by
    // CircuitCanvas mouse events.  Only one field is set at a time for the
    // start endpoint; same for the end.

    /** True while the user is actively dragging a wire from a start point. */
    private boolean drawingWire = false;

    /** The port the drag started on (null if started on a PointNode). */
    private Port dragStartPort;

    /** The node the drag started on (null if started on a Port). */
    private PointNode dragStartNode;

    /** Canvas coordinates where the drag started. */
    private double dragStartX, dragStartY;

    /**
     * Once the user's mouse has moved enough to determine direction, this
     * is locked to either horizontal-first or vertical-first.
     */
    private boolean horizontalFirst       = true;
    private boolean bendDirectionLocked   = false;

    /** Canvas reference, set after construction via {@link #setCanvas}. */
    private CircuitCanvas canvas;

    // ── Constructor ───────────────────────────────────────────────────────────

    public EditorController(Circuit circuit) {
        this.circuit = circuit;
    }

    public void setCanvas(CircuitCanvas canvas) { this.canvas = canvas; }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public Circuit         getCircuit()   { return circuit;   }
    public SelectionManager getSelection() { return selection; }

    // ── Element placement ─────────────────────────────────────────────────────

    public void onElementPlaced(ComponentType type, double x, double y) {
        int n = counters.getOrDefault(type, 0) + 1;
        counters.put(type, n);
        String id = type.name() + "-" + n;
        circuit.addElement(type.create(id, x, y));
    }

    // ── Toolbar option dispatch ───────────────────────────────────────────────

    public void optionSelected(ToolbarOptions option, double x, double y) {
        switch (option) {
            case Select -> handleSelect(x, y);
            case Wire   -> { /* wire actions go through the mouse event methods below */ }
            default     -> { /* future options */ }
        }
    }

    // ── Select tool ───────────────────────────────────────────────────────────

    private void handleSelect(double x, double y) {
        String hitId = null;
        for (var entry : circuit.getElements().entrySet()) {
            var e = entry.getValue();
            if (x >= e.getX() && x <= e.getX() + 80   // rough icon width for now
                    && y >= e.getY() && y <= e.getY() + 40) {
                hitId = entry.getKey();
                break;
            }
        }
        if (hitId != null) {
            selection.selectElement(hitId, false);
        } else {
            selection.clear();
        }
    }

    // ── Wire drawing — called by CircuitCanvas mouse handlers ─────────────────

    private static final double PORT_HIT_RADIUS   = 8.0;  // px — snap distance to a port
    private static final double NODE_HIT_RADIUS   = 8.0;  // px — snap distance to a PointNode
    private static final double WIRE_HIT_THRESHOLD = 5.0; // px — snap distance to a wire
    private static final double DIRECTION_LOCK_DIST = 5.0; // px — min drag before locking bend direction

    /**
     * Called when the mouse is pressed while the Wire tool is active.
     * Attempts to find a Port or PointNode under the cursor to begin a wire drag.
     */
    public void onWireMousePressed(double x, double y) {
        // Try to hit a port first
        Port hitPort = findPortAt(x, y);
        if (hitPort != null) {
            dragStartPort  = hitPort;
            dragStartNode  = null;
            dragStartX     = hitPort.getAbsX();
            dragStartY     = hitPort.getAbsY();
            drawingWire    = true;
            bendDirectionLocked = false;
            return;
        }

        // Try to hit an existing PointNode (junction or free end)
        PointNode hitNode = findNodeAt(x, y);
        if (hitNode != null) {
            dragStartPort  = null;
            dragStartNode  = hitNode;
            dragStartX     = hitNode.getX();
            dragStartY     = hitNode.getY();
            drawingWire    = true;
            bendDirectionLocked = false;
            return;
        }

        // Try to hit the body of an existing WireSegment.
        // Clicking anywhere on a wire (not just at a node endpoint) creates a
        // junction at that exact point — the segment is split in two — and then
        // the drag starts from the newly created junction.
        WireSegment hitSeg = findWireSegmentAt(x, y);
        if (hitSeg != null) {
            // Split the wire at the click position and get the new junction node.
            PointNode junction = circuit.splitSegmentAtJunction(hitSeg, x, y);
            dragStartPort       = null;
            dragStartNode       = junction;
            dragStartX          = junction.getX();
            dragStartY          = junction.getY();
            drawingWire         = true;
            bendDirectionLocked = false;
            return;
        }

        // Nothing hit — don't start drawing
        drawingWire = false;
    }

    /**
     * Called on every mouse-drag event while Wire tool is active.
     * Updates the bend direction lock and notifies the canvas to redraw the preview.
     *
     * @return preview bend point as double[]{bendX, bendY} so the canvas can draw it,
     *         or null if not currently drawing.
     */
    public double[] onWireMouseDragged(double mouseX, double mouseY) {
        if (!drawingWire) return null;

        // Lock bend direction once the mouse has moved far enough
        if (!bendDirectionLocked) {
            double dx = Math.abs(mouseX - dragStartX);
            double dy = Math.abs(mouseY - dragStartY);
            if (dx > DIRECTION_LOCK_DIST || dy > DIRECTION_LOCK_DIST) {
                horizontalFirst     = dx >= dy;
                bendDirectionLocked = true;
            }
        }

        // Compute bend point
        double bendX = horizontalFirst ? mouseX : dragStartX;
        double bendY = horizontalFirst ? dragStartY : mouseY;
        return new double[]{dragStartX, dragStartY, bendX, bendY, mouseX, mouseY};
    }

    /**
     * Called when the mouse is released while Wire tool is active.
     * Determines what the wire ended on and creates the appropriate WireSegment(s).
     *
     * @param mouseX release X
     * @param mouseY release Y
     */
    public void onWireMouseReleased(double mouseX, double mouseY) {
        if (!drawingWire) return;
        drawingWire = false;

        // If the bend direction was never locked, just cancel (no meaningful drag)
        if (!bendDirectionLocked) return;

        // ── Case 1: Released on a Port ────────────────────────────────────────
        Port hitPort = findPortAt(mouseX, mouseY);
        if (hitPort != null && hitPort != dragStartPort) {
            double ex = hitPort.getAbsX(), ey = hitPort.getAbsY();
            WireSegment seg = buildSegmentToPort(hitPort, ex, ey);
            if (seg != null) circuit.addWireSegment(seg);
            notifyRender();
            return;
        }

        // ── Case 2: Released on an existing PointNode ─────────────────────────
        PointNode hitNode = findNodeAt(mouseX, mouseY);
        if (hitNode != null && hitNode != dragStartNode) {
            WireSegment seg = buildSegmentToNode(hitNode);
            if (seg != null) circuit.addWireSegment(seg);
            notifyRender();
            return;
        }

        // ── Case 3: Released on an existing WireSegment → create junction ─────
        WireSegment hitSeg = findWireSegmentAt(mouseX, mouseY);
        if (hitSeg != null) {
            // Split the existing segment and get the junction node
            PointNode junction = circuit.splitSegmentAtJunction(hitSeg, mouseX, mouseY);
            // Now connect our drag-start to the junction
            WireSegment seg = buildSegmentToNode(junction);
            if (seg != null) circuit.addWireSegment(seg);
            notifyRender();
            return;
        }

        // ── Case 4: Released in empty space → create a free PointNode ─────────
        PointNode freeEnd = new PointNode(mouseX, mouseY);
        circuit.addPointNode(freeEnd);
        WireSegment seg = buildSegmentToNode(freeEnd);
        if (seg != null) circuit.addWireSegment(seg);
        notifyRender();
    }

    // ── Wire-drawing helpers ──────────────────────────────────────────────────

    /** Builds a WireSegment from the current drag start to a Port. */
    private WireSegment buildSegmentToPort(Port endPort, double ex, double ey) {
        if (dragStartPort != null) {
            return WireSegment.portToPort(
                    dragStartPort, dragStartX, dragStartY,
                    endPort, ex, ey,
                    horizontalFirst);
        } else if (dragStartNode != null) {
            return WireSegment.nodeToPort(
                    dragStartNode, endPort, ex, ey, horizontalFirst);
        }
        return null;
    }

    /** Builds a WireSegment from the current drag start to a PointNode. */
    private WireSegment buildSegmentToNode(PointNode endNode) {
        if (dragStartPort != null) {
            return WireSegment.portToNode(
                    dragStartPort, dragStartX, dragStartY,
                    endNode, horizontalFirst);
        } else if (dragStartNode != null) {
            return WireSegment.nodeToNode(dragStartNode, endNode, horizontalFirst);
        }
        return null;
    }

    // ── Hit-testing ───────────────────────────────────────────────────────────

    /**
     * Returns the first Port whose absolute canvas position is within
     * PORT_HIT_RADIUS pixels of (x, y), or null if none.
     */
    public Port findPortAt(double x, double y) {
        SvgIconRegistry registry = new SvgIconRegistry();
        for (CircuitElement elem : circuit.getElements().values()) {
            List<Port> ports = getAllPorts(elem, registry);
            for (Port p : ports) {
                if (Math.hypot(x - p.getAbsX(), y - p.getAbsY()) <= PORT_HIT_RADIUS) {
                    return p;
                }
            }
        }
        return null;
    }

    /**
     * Returns the first PointNode within NODE_HIT_RADIUS of (x, y), or null.
     */
    public PointNode findNodeAt(double x, double y) {
        for (PointNode node : circuit.getPointNodes()) {
            if (Math.hypot(x - node.getX(), y - node.getY()) <= NODE_HIT_RADIUS) {
                return node;
            }
        }
        return null;
    }

    /**
     * Returns the first WireSegment whose polyline is within WIRE_HIT_THRESHOLD
     * pixels of (x, y), or null if none.
     */
    public WireSegment findWireSegmentAt(double x, double y) {
        for (WireSegment seg : circuit.getWireSegments()) {
            if (seg.distanceToPoint(x, y) <= WIRE_HIT_THRESHOLD) {
                return seg;
            }
        }
        return null;
    }

    /**
     * Collects all Port objects from a CircuitElement regardless of subtype.
     */
    private List<Port> getAllPorts(CircuitElement elem, SvgIconRegistry registry) {
        List<Port> ports = new ArrayList<>();
        if (elem instanceof Gate gate) {
            ports.addAll(gate.getAllPorts());
        } else if (elem instanceof InputElement ie) {
            ports.add(ie.getOutputPort());
        } else if (elem instanceof OutputElement oe) {
            ports.add(oe.getInputPort());
        }
        return ports;
    }

    // ── Component movement — stretch attached wire segments ───────────────────

    /**
     * Call this after a component has been moved to dx, dy.
     * Only the WireSegments whose start/end Port belongs to the moved element
     * are updated; everything else stays static.
     */
    public void onComponentMoved(CircuitElement element) {
        SvgIconRegistry registry = new SvgIconRegistry();
        List<Port> ports = getAllPorts(element, registry);

        for (WireSegment seg : circuit.getWireSegments()) {
            if (seg.getStartPort() != null && ports.contains(seg.getStartPort())) {
                seg.setX1(seg.getStartPort().getAbsX());
                seg.setY1(seg.getStartPort().getAbsY());
            }
            if (seg.getEndPort() != null && ports.contains(seg.getEndPort())) {
                seg.setX2(seg.getEndPort().getAbsX());
                seg.setY2(seg.getEndPort().getAbsY());
            }
        }
    }

    // ── State accessors (used by CircuitCanvas for preview line) ──────────────

    public boolean isDrawingWire()     { return drawingWire;      }
    public double  getDragStartX()     { return dragStartX;       }
    public double  getDragStartY()     { return dragStartY;       }
    public boolean isHorizontalFirst() { return horizontalFirst;  }

    // ── Undo / Redo (stubs) ───────────────────────────────────────────────────

    public void undo() { /* to be implemented */ }
    public void redo() { /* to be implemented */ }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void notifyRender() {
        if (canvas != null) canvas.render();
    }
}
