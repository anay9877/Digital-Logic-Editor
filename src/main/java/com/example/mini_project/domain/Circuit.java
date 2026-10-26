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

    // Removes segments by id. Unknown ids are skipped. Also removes any dangling stub the deletion leaves
    // behind. Returns how many segments were removed in total.
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

        List<PointNode> touched = new ArrayList<>();
        for (WireSegment seg : removed) {                   // keep each node's list + junction flag correct
            if (seg.getStartNode() != null) {
                seg.getStartNode().removeSegment(seg);
                touched.add(seg.getStartNode());
            }
            if (seg.getEndNode() != null) {
                seg.getEndNode().removeSegment(seg);
                touched.add(seg.getEndNode());
            }
        }

        int stubs = pruneStubsFrom(new ArrayDeque<>(touched));

        for (PointNode n : touched) {                       // tidy up pass-through nodes left behind
            if (pointNodes.contains(n)) mergeStraightThrough(n);
        }

        pointNodes.removeIf(n -> n.getConnectedSegments().isEmpty());   // prune orphan nodes
        return removed.size() + stubs;
    }

    /**
     * A node left with exactly ONE segment is a free end. Remove that segment, then check the node on its
     * far side, and keep going until the chain reaches a node that still has 2+ segments or a port.
     * Only chains next to the deleted segments are touched.
     */
    private int pruneStubsFrom(Deque<PointNode> pending) {
        int count = 0;
        while (!pending.isEmpty()) {
            PointNode n = pending.poll();
            if (n.getConnectedSegments().size() != 1) continue;   // 0 = orphan (cleaned by caller), 2+ = still a corner/junction

            WireSegment stub = n.getConnectedSegments().get(0);
            removeWireSegment(stub);                              // detaches from both end nodes
            count++;

            PointNode far = (stub.getStartNode() == n) ? stub.getEndNode() : stub.getStartNode();
            if (far != null) pending.add(far);                    // may have become a free end too
        }
        return count;
    }

    /**
     * If 'n' has exactly two segments that lie on the same straight line and leave n in OPPOSITE
     * directions, merge them into one segment and drop the node. Connectivity is unchanged.
     * Corners (perpendicular segments) are left alone.
     */
    private void mergeStraightThrough(PointNode n) {
        if (n.getConnectedSegments().size() != 2) return;
        WireSegment a = n.getConnectedSegments().get(0);
        WireSegment b = n.getConnectedSegments().get(1);

        boolean sameLine = (a.isHorizontal() && b.isHorizontal() && Math.abs(a.getY1() - b.getY1()) < 0.001)
                || (a.isVertical() && b.isVertical() && Math.abs(a.getX1() - b.getX1()) < 0.001);
        if (!sameLine) return;

        boolean aEndsAtN = (a.getEndNode() == n);
        boolean bEndsAtN = (b.getEndNode() == n);

        // far end of each segment (the end that is NOT n)
        double aFx = aEndsAtN ? a.getX1() : a.getX2();
        double aFy = aEndsAtN ? a.getY1() : a.getY2();
        double bFx = bEndsAtN ? b.getX1() : b.getX2();
        double bFy = bEndsAtN ? b.getY1() : b.getY2();

        // must leave n in opposite directions, otherwise the two wires overlap
        double dot = (aFx - n.getX()) * (bFx - n.getX()) + (aFy - n.getY()) * (bFy - n.getY());
        if (dot >= 0) return;

        Port farPort = bEndsAtN ? b.getStartPort() : b.getEndPort();
        PointNode farNode = bEndsAtN ? b.getStartNode() : b.getEndNode();

        removeWireSegment(b);                           // detaches b from n and from its far node

        // stretch 'a' so its n-end now ends where b's far end was
        if (aEndsAtN) {
            a.setEndNode(farNode);
            a.setEndPort(farPort);
            a.setX2(bFx);
            a.setY2(bFy);
        } else {
            a.setStartNode(farNode);
            a.setStartPort(farPort);
            a.setX1(bFx);
            a.setY1(bFy);
        }
        n.removeSegment(a);
        if (farNode != null) farNode.addSegment(a);
        pointNodes.remove(n);
    }

    // Point Nodes

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