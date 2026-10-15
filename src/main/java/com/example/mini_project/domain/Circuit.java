package com.example.mini_project.domain;

import com.example.mini_project.gui.CircuitCanvas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// This is main Circuit class, but it may contain disjoints circuits as well!!
// Contains -> CircuitElements, WireSegments, PointNodes
public class Circuit {

    private final Map<String, CircuitElement> elements = new HashMap<>();
    private final List<WireSegment> wireSegments = new ArrayList<>();
    private final List<PointNode> pointNodes = new ArrayList<>();

    public void addElement(CircuitElement e) {
        elements.put(e.getId(), e);
    }

    public CircuitElement getElement(String id) {
        return elements.get(id);
    }

    public Map<String, CircuitElement> getElements() {
        return elements;
    }

    // Used by Ctrl+N that is New button in File MenuBar
    public void clear() {
        elements.clear();
        wireSegments.clear();
        pointNodes.clear();
    }

    public void removeElement(String id) {
        // Remove the element
        elements.remove(id);
        // Cascade-delete any wire segment whose start or end port belongs to
        // the removed element, so dangling wires never exist.
        wireSegments.removeIf(seg -> {
            boolean startBelongs = seg.getStartPort() != null
                    && seg.getStartPort().getOwner().getId().equals(id);
            boolean endBelongs = seg.getEndPort() != null
                    && seg.getEndPort().getOwner().getId().equals(id);
            if (startBelongs || endBelongs) {
                // Disconnect the surviving PointNode end so it knows the segment is gone
                if (seg.getStartNode() != null) seg.getStartNode().removeSegment(seg);
                if (seg.getEndNode() != null) seg.getEndNode().removeSegment(seg);
                return true; // remove this segment
            }
            return false;
        });
        // Prune any PointNodes that now have no segments
        pointNodes.removeIf(n -> n.getConnectedSegments().isEmpty());
    }

    // ── Wire Segments ─────────────────────────────────────────────────────────

    public List<WireSegment> getWireSegments() {
        return wireSegments;
    }

    public void addWireSegment(WireSegment seg) {
        wireSegments.add(seg);
        // Register this segment with any PointNode endpoints
        if (seg.getStartNode() != null) seg.getStartNode().addSegment(seg);
        if (seg.getEndNode() != null) seg.getEndNode().addSegment(seg);
    }

    public void removeWireSegment(WireSegment seg) {
        wireSegments.remove(seg);
        if (seg.getStartNode() != null) seg.getStartNode().removeSegment(seg);
        if (seg.getEndNode() != null) seg.getEndNode().removeSegment(seg);
    }

    // ── Point Nodes ───────────────────────────────────────────────────────────

    public List<PointNode> getPointNodes() {
        return pointNodes;
    }

    public void addPointNode(PointNode node) {
        pointNodes.add(node);
    }

    public void removePointNode(PointNode node) {
        pointNodes.remove(node);
    }

    /**
     * Splits an existing WireSegment at a junction point.
     * <p>
     * Given:
     * existingSeg: A → B
     * junction:    a new PointNode at the click position
     * <p>
     * Result:
     * Remove existingSeg
     * Add  segA : A → junction  (horizontalFirst same as existingSeg)
     * Add  segB : junction → B  (horizontalFirst = true, routes simply)
     * <p>
     * The caller is responsible for also adding the new incoming segment
     * that triggered the junction.
     */
    //
    public PointNode splitSegmentAtJunction(WireSegment existingSeg, double jx, double jy) {
        // Project (jx, jy) onto the straight wire segment
        if (existingSeg.isHorizontal()) {
            jy = existingSeg.getY1();
            jx = Math.clamp(jx, Math.min(existingSeg.getX1(), existingSeg.getX2()), Math.max(existingSeg.getX1(), existingSeg.getX2()));
        } else {
            jx = existingSeg.getX1();
            jy = Math.clamp(jy, Math.min(existingSeg.getY1(), existingSeg.getY2()), Math.max(existingSeg.getY1(), existingSeg.getY2()));
        }
        jx = CircuitCanvas.snap(jx);
        jy = CircuitCanvas.snap(jy);

        PointNode junction = new PointNode(jx, jy);
        addPointNode(junction);

        // Build the two straight halves
        WireSegment segA;
        if (existingSeg.getStartPort() != null) {
            segA = WireSegment.portToNode(
                    existingSeg.getStartPort(),
                    existingSeg.getX1(), existingSeg.getY1(),
                    junction);
        } else {
            segA = WireSegment.nodeToNode(existingSeg.getStartNode(), junction);
        }

        WireSegment segB;
        if (existingSeg.getEndPort() != null) {
            segB = WireSegment.nodeToPort(
                    junction,
                    existingSeg.getEndPort(),
                    existingSeg.getX2(), existingSeg.getY2());
        } else {
            segB = WireSegment.nodeToNode(junction, existingSeg.getEndNode());
        }

        removeWireSegment(existingSeg);
        addWireSegment(segA);
        addWireSegment(segB);

        return junction;
    }
}
