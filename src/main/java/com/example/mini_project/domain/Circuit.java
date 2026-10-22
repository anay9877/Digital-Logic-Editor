package com.example.mini_project.domain;

import com.example.mini_project.gui.CircuitCanvas;

import com.example.mini_project.gui.CircuitCanvas;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    public boolean removeElement(String id) {
        if (elements.remove(id) == null) return false;        // unknown or already deleted

        // 1. Seeds: wires whose start/end PORT belongs to the removed element
        Set<WireSegment> doomed = new HashSet<>();
        Deque<WireSegment> queue = new ArrayDeque<>();
        for (WireSegment seg : wireSegments) {
            if (belongsTo(seg.getStartPort(), id) || belongsTo(seg.getEndPort(), id)) {
                if (doomed.add(seg)) queue.add(seg);
            }
        }

        // 2. BFS: spread through PointNodes to every connected segment
        while (!queue.isEmpty()) {
            WireSegment s = queue.poll();
            for (PointNode n : new PointNode[]{s.getStartNode(), s.getEndNode()}) {
                if (n == null) continue;                       // that end is a Port, so the search stops there
                // if (n.isJunction()) continue;               // optional: see "Choice to confirm" below
                for (WireSegment next : n.getConnectedSegments()) {
                    if (doomed.add(next)) queue.add(next);     // add() is false if already visited
                }
            }
        }

        // 3. Delete everything found, then fix the nodes' bookkeeping
        wireSegments.removeAll(doomed);
        for (WireSegment s : doomed) {
            if (s.getStartNode() != null) s.getStartNode().removeSegment(s);
            if (s.getEndNode() != null) s.getEndNode().removeSegment(s);
        }
        pointNodes.removeIf(n -> n.getConnectedSegments().isEmpty());
        return true;
    }

    private static boolean belongsTo(Port p, String elementId) {
        return p != null && p.getOwner().getId().equals(elementId);
    }

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

     // All wire segments reachable from 'start' through shared PointNodes (BFS).
    public Set<WireSegment> getConnectedWires(WireSegment start) {
        Set<WireSegment> found = new HashSet<>();
        Deque<WireSegment> queue = new ArrayDeque<>();
        found.add(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            WireSegment s = queue.poll();
            for (PointNode n : new PointNode[]{
                    s.getStartNode(), s.getEndNode()
            }) {
                if (n == null) continue;                    // that end is a port: stop
                for (WireSegment next : n.getConnectedSegments()) {
                    if (found.add(next)) queue.add(next);
                }
            }
        }
        return found;
    }

    // Removes segments by id. Unknown ids are skipped. Returns how many were removed.
    public int removeWireSegments(Set<String> ids) {
        if (ids.isEmpty()) return 0;
        List<WireSegment> removed = new ArrayList<>();
        wireSegments.removeIf(seg -> {
            if (ids.contains(seg.getId())) {
                removed.add(seg);
                return true;
            }
            return false;
        });
        for (WireSegment seg : removed) {                   // keep each node's list + junction flag correct
            if (seg.getStartNode() != null) {
                seg.getStartNode().removeSegment(seg);
            }
            if (seg.getEndNode() != null) {
                seg.getEndNode().removeSegment(seg);
            }
        }
        pointNodes.removeIf(n -> n.getConnectedSegments().isEmpty());   // prune orphan nodes
        return removed.size();
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

    /**
     * Builds a list of connected sub-circuits by traversing the graph formed by
     * circuit elements and point nodes connected through wire segments.
     */
    public List<SubCircuit> splitIntoDisjointSubCircuits() {
        List<SubCircuit> subCircuits = new ArrayList<>();
        Map<String, Set<String>> adjacency = new HashMap<>();
        Set<String> visitedVertices = new HashSet<>();

        for (CircuitElement element : elements.values()) {
            adjacency.computeIfAbsent(element.getId(), key -> new HashSet<>());
        }
        for (PointNode node : pointNodes) {
            adjacency.computeIfAbsent(node.getId(), key -> new HashSet<>());
        }
        for (WireSegment segment : wireSegments) {
            String startVertex = null;
            String endVertex = null;

            if (segment.getStartPort() != null) {
                startVertex = segment.getStartPort().getOwner().getId();
            } else if (segment.getStartNode() != null) {
                startVertex = segment.getStartNode().getId();
            }

            if (segment.getEndPort() != null) {
                endVertex = segment.getEndPort().getOwner().getId();
            } else if (segment.getEndNode() != null) {
                endVertex = segment.getEndNode().getId();
            }

            if (startVertex != null && endVertex != null) {
                adjacency.computeIfAbsent(startVertex, key -> new HashSet<>()).add(endVertex);
                adjacency.computeIfAbsent(endVertex, key -> new HashSet<>()).add(startVertex);
            }
        }

        for (String elementId : elements.keySet()) {
            if (visitedVertices.contains(elementId)) {
                continue;
            }

            SubCircuit subCircuit = new SubCircuit("subcircuit-" + (subCircuits.size() + 1));
            Deque<String> queue = new ArrayDeque<>();
            Set<String> componentVertices = new HashSet<>();

            queue.add(elementId);
            visitedVertices.add(elementId);

            while (!queue.isEmpty()) {
                String current = queue.removeFirst();
                componentVertices.add(current);
                for (String neighbor : adjacency.getOrDefault(current, Set.of())) {
                    if (!visitedVertices.contains(neighbor)) {
                        visitedVertices.add(neighbor);
                        queue.addLast(neighbor);
                    }
                }
            }

            for (String currentId : componentVertices) {
                if (elements.containsKey(currentId)) {
                    subCircuit.addElement(elements.get(currentId));
                }
            }

            for (WireSegment segment : wireSegments) {
                String startVertex = null;
                String endVertex = null;

                if (segment.getStartPort() != null) {
                    startVertex = segment.getStartPort().getOwner().getId();
                } else if (segment.getStartNode() != null) {
                    startVertex = segment.getStartNode().getId();
                }

                if (segment.getEndPort() != null) {
                    endVertex = segment.getEndPort().getOwner().getId();
                } else if (segment.getEndNode() != null) {
                    endVertex = segment.getEndNode().getId();
                }

                if (startVertex != null && endVertex != null) {
                    boolean touchesComponent = componentVertices.contains(startVertex)
                            || componentVertices.contains(endVertex);
                    if (touchesComponent) {
                        subCircuit.addWireSegment(segment);
                    }
                }
            }

            for (PointNode node : pointNodes) {
                if (componentVertices.contains(node.getId())) {
                    subCircuit.addPointNode(node);
                }
            }

            subCircuits.add(subCircuit);
        }

        return subCircuits;
    }
}