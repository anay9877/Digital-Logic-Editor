package com.example.mini_project.controller;

import com.example.mini_project.domain.*;
import com.example.mini_project.gui.CircuitCanvas;
import com.example.mini_project.gui.ToolbarOptions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EditorController {

    private final Circuit circuit;
    private final Map<ComponentType, Integer> counters  = new HashMap<>();
    private final SelectionManager            selection = new SelectionManager();

    // ── Wire drawing state ────────────────────────────────────────────────────
    private boolean drawingWire = false;
    private Port dragStartPort;
    private PointNode dragStartNode;
    private double dragStartX, dragStartY;
    private boolean horizontalFirst = true;
    private boolean bendDirectionLocked = false;

    /** Canvas reference, set after construction via {@link #setCanvas}. */
    private CircuitCanvas canvas;

    // ── Constructor ───────────────────────────────────────────────────────────

    public EditorController(Circuit circuit) {
        this.circuit = circuit;
    }

    public void setCanvas(CircuitCanvas canvas) { this.canvas = canvas; }

    public Circuit         getCircuit()   { return circuit;   }
    public SelectionManager getSelection() { return selection; }
    public CircuitCanvas   getCanvas()    { return canvas;    }

    public void newCircuit() {
        circuit.clear();
        counters.clear();
        selection.clear();
        PointNode.resetId();
        WireSegment.resetId();
        drawingWire = false;
        dragStartPort = null;
        dragStartNode = null;
        if (canvas != null && canvas.getViewportManager() != null) {
            canvas.getViewportManager().resetView();
        }
        notifyRender();
    }

    public void updateCounter(ComponentType type, int count) {
        counters.put(type, Math.max(counters.getOrDefault(type, 0), count));
    }

    public void clearCounters() {
        counters.clear();
    }

    // ── Element placement ─────────────────────────────────────────────────────

    public void onElementPlaced(ComponentType type, double x, double y) {
        int n = counters.getOrDefault(type, 0) + 1;
        counters.put(type, n);
        String id = type.name() + "-" + n;
        circuit.addElement(type.create(id, CircuitCanvas.snap(x), CircuitCanvas.snap(y)));
        notifyRender();
    }

    public void toggleInput(InputElement input) {
        input.toggle();
        notifyRender();
    }

    // ── Toolbar option dispatch ───────────────────────────────────────────────

    public void optionSelected(ToolbarOptions option, double x, double y) {
        switch (option){
            case Select -> handleSelect(x, y);
            case Delete -> handleDelete();
        }
    }

    private void handleSelect(double x, double y) {
        selection.clear();
        System.out.println("Selection cleared");
    }

    private void handleDelete() {
        deleteSelected();
    }

    public void deleteSelected() {
        if (drawingWire) return;
        Collection<String> elementIds = selection.getSelectedElements();
        Set<String> wireIds = selection.getSelectedWires();
        if (elementIds.isEmpty() && wireIds.isEmpty()) return;

        boolean changed = false;
        for (String id : elementIds) {
            changed |= onElementDeleted(id);                       // its cascade may already remove some selected wires
        }
        changed |= circuit.removeWireSegments(wireIds) > 0;        // already-removed ids are skipped

        selection.clear();
        if (changed) {
            notifyRender();
            //recordChange();                                        // undo covers wire deletion too
        }
    }

    public boolean onElementDeleted(String id) {
        return circuit.removeElement(id);   // counterpart of onElementPlaced
    }

    public void moveElements(Collection<String> ids, double dx, double dy){
        for(String id : ids){
            CircuitElement e = circuit.getElement(id);
            if(e != null) {
                double newX = CircuitCanvas.snap(e.getX() + dx);
                double newY = CircuitCanvas.snap(e.getY() + dy);
                e.setPosition(newX, newY);
                onComponentMoved(e);
            }
        }
        notifyRender();
    }

    // ── Wire drawing — called by CircuitCanvas mouse handlers ─────────────────

    private static final double PORT_HIT_RADIUS    = CircuitCanvas.GRID_SIZE * 1.5;  // 15px
    private static final double NODE_HIT_RADIUS    = CircuitCanvas.GRID_SIZE * 1.5;  // 15px
    private static final double WIRE_HIT_THRESHOLD = CircuitCanvas.GRID_SIZE * 0.6;  // 6px
    private static final double DIRECTION_LOCK_DIST = CircuitCanvas.GRID_SIZE;        // 10px
    private static final double WIRE_SELECT_THRESHOLD = 8.0; 

    public void onWireMousePressed(double x, double y) {
        x = CircuitCanvas.snap(x);
        y = CircuitCanvas.snap(y);

        // Try to hit a port first
        Port hitPort = findPortAt(x, y);
        if (hitPort != null) {
            dragStartPort  = hitPort;
            dragStartNode  = null;
            // Snap so the wire start is always on a grid dot.
            // Port.getAbsX() = element.getX() + relX. element.getX() is snapped but relX is a
            // raw scaled value that may not be a multiple of GRID_SIZE. Snapping here ensures
            // the start coordinate matches any snapped mouse position during drag, preventing
            // spurious L-bends when the user drags perfectly vertical or horizontal.
            dragStartX     = CircuitCanvas.snap(hitPort.getAbsX());
            dragStartY     = CircuitCanvas.snap(hitPort.getAbsY());
            drawingWire    = true;
            bendDirectionLocked = false;
            return;
        }

        // Try to hit an existing PointNode (junction, corner, or free end)
        PointNode hitNode = findNodeAt(x, y);
        if (hitNode != null) {
            dragStartPort  = null;
            dragStartNode  = hitNode;
            dragStartX     = CircuitCanvas.snap(hitNode.getX());
            dragStartY     = CircuitCanvas.snap(hitNode.getY());
            drawingWire    = true;
            bendDirectionLocked = false;
            return;
        }

        // Try to hit the body of an existing WireSegment
        WireSegment hitSeg = findWireSegmentAt(x, y);
        if (hitSeg != null) {
            PointNode junction = circuit.splitSegmentAtJunction(hitSeg, x, y);
            dragStartPort       = null;
            dragStartNode       = junction;
            dragStartX          = CircuitCanvas.snap(junction.getX());
            dragStartY          = CircuitCanvas.snap(junction.getY());
            drawingWire         = true;
            bendDirectionLocked = false;
            return;
        }

        drawingWire = false;
    }

    public double[] onWireMouseDragged(double mouseX, double mouseY) {
        if (!drawingWire) return null;

        mouseX = CircuitCanvas.snap(mouseX);
        mouseY = CircuitCanvas.snap(mouseY);

        if (!bendDirectionLocked) {
            double dx = Math.abs(mouseX - dragStartX);
            double dy = Math.abs(mouseY - dragStartY);
            if (dx >= DIRECTION_LOCK_DIST || dy >= DIRECTION_LOCK_DIST) {
                horizontalFirst     = dx >= dy;
                bendDirectionLocked = true;
            }
        }

        if (mouseX == dragStartX && mouseY == dragStartY) {
            return null;
        }

        if (mouseX == dragStartX || mouseY == dragStartY) {
            // Straight line preview: 2 points
            return new double[]{dragStartX, dragStartY, mouseX, mouseY};
        } else {
            // L-bend preview: 3 points (2 straight segments)
            double bendX = horizontalFirst ? mouseX : dragStartX;
            double bendY = horizontalFirst ? dragStartY : mouseY;
            return new double[]{dragStartX, dragStartY, bendX, bendY, mouseX, mouseY};
        }
    }

    public void onWireMouseReleased(double mouseX, double mouseY) {
        if (!drawingWire) return;
        drawingWire = false;

        mouseX = CircuitCanvas.snap(mouseX);
        mouseY = CircuitCanvas.snap(mouseY);

        if (mouseX == dragStartX && mouseY == dragStartY) return;

        boolean hasBend = (mouseX != dragStartX) && (mouseY != dragStartY);
        double bendX = horizontalFirst ? mouseX : dragStartX;
        double bendY = horizontalFirst ? dragStartY : mouseY;

        double leg1EndX = hasBend ? bendX : mouseX;
        double leg1EndY = hasBend ? bendY : mouseY;
        boolean leg1IsHorizontal = Math.abs(dragStartY - leg1EndY) < 0.001;

        // Check if we can EXTEND an existing segment at dragStartNode
        WireSegment segToExtend = null;
        if (dragStartNode != null && dragStartNode.getConnectedSegments().size() == 1) {
            WireSegment cand = dragStartNode.getConnectedSegments().get(0);
            if ((cand.isHorizontal() && leg1IsHorizontal) || (cand.isVertical() && !leg1IsHorizontal)) {
                segToExtend = cand;
            }
        }

        if (segToExtend != null) {
            if (!hasBend) {
                // Entire drag is collinear: extend segToExtend to (mouseX, mouseY)
                attachSegmentEnd(segToExtend, dragStartNode, mouseX, mouseY);
            } else {
                // Leg 1 extends segToExtend to (bendX, bendY)
                updateSegmentNode(segToExtend, dragStartNode, bendX, bendY);
                // Leg 2 is a new segment from bendX, bendY to mouseX, mouseY
                createAndAddSegment(null, dragStartNode, bendX, bendY, mouseX, mouseY);
            }
        } else {
            if (!hasBend) {
                // Straight: 1 new segment
                createAndAddSegment(dragStartPort, dragStartNode, dragStartX, dragStartY, mouseX, mouseY);
            } else {
                // Bend: 2 new segments meeting at cornerNode
                PointNode cornerNode = new PointNode(bendX, bendY);
                circuit.addPointNode(cornerNode);

                // Leg 1: start -> cornerNode
                WireSegment leg1;
                if (dragStartPort != null) {
                    leg1 = WireSegment.portToNode(dragStartPort, dragStartX, dragStartY, cornerNode);
                } else {
                    leg1 = WireSegment.nodeToNode(dragStartNode, cornerNode);
                }
                circuit.addWireSegment(leg1);

                // Leg 2: cornerNode -> (mouseX, mouseY)
                createAndAddSegment(null, cornerNode, bendX, bendY, mouseX, mouseY);
            }
        }
        notifyRender();
    }

    public void onSelectPoint(double x, double y, boolean additive, boolean wholeWire) {
        WireSegment hit = findNearestWire(x, y);
        if (hit == null) {
            if (!additive) selection.clear();          // same behaviour as the old handleSelect
            return;
        }
        if (wholeWire) {
            List<String> ids = new ArrayList<>();
            for (WireSegment s : circuit.getConnectedWires(hit)) ids.add(s.getId());
            selection.selectWires(ids, additive);
        } else {
            selection.selectWire(hit.getId(), additive);
        }
    }

    public WireSegment findNearestWire(double x, double y) {
        WireSegment best = null;
        double bestDist = WIRE_SELECT_THRESHOLD;
        for (WireSegment seg : circuit.getWireSegments()) {
            double d = seg.distanceToPoint(x, y);
            if (d <= bestDist) { bestDist = d; best = seg; }   // nearest wins near corners and junctions
        }
        return best;
    }    

    private void attachSegmentEnd(WireSegment seg, PointNode currentNode, double targetX, double targetY) {
        boolean isStart = (seg.getStartNode() == currentNode);

        Port hitPort = findPortAt(targetX, targetY);
        if (hitPort != null) {
            double hx = hitPort.getAbsX();
            double hy = hitPort.getAbsY();
            if (isStart) {
                seg.setStartPort(hitPort);
                seg.setX1(hx);
                seg.setY1(hy);
            } else {
                seg.setEndPort(hitPort);
                seg.setX2(hx);
                seg.setY2(hy);
            }
            circuit.removePointNode(currentNode);
            return;
        }

        PointNode hitNode = findNodeAt(targetX, targetY);
        if (hitNode != null && hitNode != currentNode) {
            double nx = hitNode.getX();
            double ny = hitNode.getY();
            if (isStart) {
                seg.setStartNode(hitNode);
                seg.setX1(nx);
                seg.setY1(ny);
                hitNode.addSegment(seg);
            } else {
                seg.setEndNode(hitNode);
                seg.setX2(nx);
                seg.setY2(ny);
                hitNode.addSegment(seg);
            }
            circuit.removePointNode(currentNode);
            return;
        }

        WireSegment hitSeg = findWireSegmentAt(targetX, targetY);
        if (hitSeg != null && hitSeg != seg) {
            PointNode junction = circuit.splitSegmentAtJunction(hitSeg, targetX, targetY);
            double jx = junction.getX();
            double jy = junction.getY();
            if (isStart) {
                seg.setStartNode(junction);
                seg.setX1(jx);
                seg.setY1(jy);
                junction.addSegment(seg);
            } else {
                seg.setEndNode(junction);
                seg.setX2(jx);
                seg.setY2(jy);
                junction.addSegment(seg);
            }
            circuit.removePointNode(currentNode);
            return;
        }

        // Empty space: update node and segment coordinates
        currentNode.setX(targetX);
        currentNode.setY(targetY);
        if (isStart) {
            seg.setX1(targetX);
            seg.setY1(targetY);
        } else {
            seg.setX2(targetX);
            seg.setY2(targetY);
        }
    }

    private void updateSegmentNode(WireSegment seg, PointNode currentNode, double nx, double ny) {
        currentNode.setX(nx);
        currentNode.setY(ny);
        if (seg.getStartNode() == currentNode) {
            seg.setX1(nx);
            seg.setY1(ny);
        } else {
            seg.setX2(nx);
            seg.setY2(ny);
        }
    }

    private void createAndAddSegment(Port startPort, PointNode startNode, double x1, double y1, double targetX, double targetY) {
        Port hitPort = findPortAt(targetX, targetY);
        if (hitPort != null && hitPort != startPort) {
            double hx = hitPort.getAbsX();
            double hy = hitPort.getAbsY();
            WireSegment seg = (startPort != null)
                    ? WireSegment.portToPort(startPort, x1, y1, hitPort, hx, hy)
                    : WireSegment.nodeToPort(startNode, hitPort, hx, hy);
            circuit.addWireSegment(seg);
            return;
        }

        PointNode hitNode = findNodeAt(targetX, targetY);
        if (hitNode != null && hitNode != startNode) {
            WireSegment seg = (startPort != null)
                    ? WireSegment.portToNode(startPort, x1, y1, hitNode)
                    : WireSegment.nodeToNode(startNode, hitNode);
            circuit.addWireSegment(seg);
            return;
        }

        WireSegment hitSeg = findWireSegmentAt(targetX, targetY);
        if (hitSeg != null) {
            PointNode junction = circuit.splitSegmentAtJunction(hitSeg, targetX, targetY);
            WireSegment seg = (startPort != null)
                    ? WireSegment.portToNode(startPort, x1, y1, junction)
                    : WireSegment.nodeToNode(startNode, junction);
            circuit.addWireSegment(seg);
            return;
        }

        PointNode freeEnd = new PointNode(targetX, targetY);
        circuit.addPointNode(freeEnd);
        WireSegment seg = (startPort != null)
                ? WireSegment.portToNode(startPort, x1, y1, freeEnd)
                : WireSegment.nodeToNode(startNode, freeEnd);
        circuit.addWireSegment(seg);
    }

    // ── Hit-testing ───────────────────────────────────────────────────────────

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

    public PointNode findNodeAt(double x, double y) {
        for (PointNode node : circuit.getPointNodes()) {
            if (Math.hypot(x - node.getX(), y - node.getY()) <= NODE_HIT_RADIUS) {
                return node;
            }
        }
        return null;
    }

    public WireSegment findWireSegmentAt(double x, double y) {
        for (WireSegment seg : circuit.getWireSegments()) {
            if (seg.distanceToPoint(x, y) <= WIRE_HIT_THRESHOLD) {
                return seg;
            }
        }
        return null;
    }

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

    public void onComponentMoved(CircuitElement element) {
        SvgIconRegistry registry = new SvgIconRegistry();
        List<Port> ports = getAllPorts(element, registry);

        for (Port p : ports) {
            double px = p.getAbsX();
            double py = p.getAbsY();

            List<WireSegment> attached = new ArrayList<>();
            for (WireSegment seg : circuit.getWireSegments()) {
                if (seg.getStartPort() == p || seg.getEndPort() == p) {
                    attached.add(seg);
                }
            }

            for (WireSegment seg : attached) {
                boolean isStart = (seg.getStartPort() == p);
                double otherX = isStart ? seg.getX2() : seg.getX1();
                double otherY = isStart ? seg.getY2() : seg.getY1();
                PointNode otherNode = isStart ? seg.getEndNode() : seg.getStartNode();

                // If other end is a corner PointNode that connects exactly 2 segments (bend corner):
                if (otherNode != null && otherNode.getConnectedSegments().size() == 2) {
                    WireSegment otherSeg = null;
                    for (WireSegment s : otherNode.getConnectedSegments()) {
                        if (s != seg) { otherSeg = s; break; }
                    }

                    if (otherSeg != null) {
                        if (seg.isHorizontal() && otherSeg.isVertical()) {
                            otherNode.setY(py);
                            seg.setY1(py);
                            seg.setY2(py);
                            if (isStart) seg.setX1(px); else seg.setX2(px);
                            if (otherSeg.getStartNode() == otherNode) otherSeg.setY1(py);
                            else otherSeg.setY2(py);
                            continue;
                        } else if (seg.isVertical() && otherSeg.isHorizontal()) {
                            otherNode.setX(px);
                            seg.setX1(px);
                            seg.setX2(px);
                            if (isStart) seg.setY1(py); else seg.setY2(py);
                            if (otherSeg.getStartNode() == otherNode) otherSeg.setX1(px);
                            else otherSeg.setX2(px);
                            continue;
                        }
                    }
                }

                // If already aligned on an axis:
                if (Math.abs(px - otherX) < 0.001 || Math.abs(py - otherY) < 0.001) {
                    if (isStart) { seg.setX1(px); seg.setY1(py); }
                    else         { seg.setX2(px); seg.setY2(py); }
                } else {
                    // Not aligned: create an orthogonal L-bend between (px, py) and (otherX, otherY)
                    double bendX = otherX;
                    double bendY = py;
                    PointNode bend = new PointNode(bendX, bendY);
                    circuit.addPointNode(bend);

                    if (isStart) {
                        Port oldEndPort = seg.getEndPort();
                        PointNode oldEndNode = seg.getEndNode();
                        if (oldEndNode != null) {
                            oldEndNode.removeSegment(seg);
                        }

                        seg.setX1(px); seg.setY1(py);
                        seg.setX2(bendX); seg.setY2(bendY);
                        seg.setEndNode(bend);
                        bend.addSegment(seg);

                        WireSegment leg2 = (oldEndPort != null)
                                ? WireSegment.nodeToPort(bend, oldEndPort, otherX, otherY)
                                : WireSegment.nodeToNode(bend, oldEndNode);
                        circuit.addWireSegment(leg2);
                    } else {
                        Port oldStartPort = seg.getStartPort();
                        PointNode oldStartNode = seg.getStartNode();
                        if (oldStartNode != null) {
                            oldStartNode.removeSegment(seg);
                        }

                        seg.setX1(bendX); seg.setY1(bendY);
                        seg.setX2(px); seg.setY2(py);
                        seg.setStartNode(bend);
                        bend.addSegment(seg);

                        WireSegment leg2 = (oldStartPort != null)
                                ? WireSegment.portToNode(oldStartPort, otherX, otherY, bend)
                                : WireSegment.nodeToNode(oldStartNode, bend);
                        circuit.addWireSegment(leg2);
                    }
                }
            }
        }
        notifyRender();
    }

    public boolean isDrawingWire()     { return drawingWire;      }
    public double  getDragStartX()     { return dragStartX;       }
    public double  getDragStartY()     { return dragStartY;       }
    public boolean isHorizontalFirst() { return horizontalFirst;  }

    public void undo() { }
    public void redo() { }

    public void notifyRender() {
        if (canvas != null) canvas.render();
    }
}
