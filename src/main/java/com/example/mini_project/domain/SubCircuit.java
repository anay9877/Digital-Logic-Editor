package com.example.mini_project.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A connected component of the circuit built from the segment-based connectivity graph.
 * <p>
 * This class intentionally does not use the legacy Wire model. A circuit is represented by
 * Gates, Port connections, PointNodes, and WireSegments instead of a separate Wire
 * abstraction. This keeps sub-circuit detection and routing consistent with the current model.
 */
public class SubCircuit {
    private final String id;
    private final Map<String, Gate> gates = new HashMap<>();
    private final List<WireSegment> wireSegments = new ArrayList<>();
    private final List<PointNode> pointNodes = new ArrayList<>();
    private final List<InputElement> inputs = new ArrayList<>();
    private final List<OutputElement> outputs = new ArrayList<>();

    public SubCircuit(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void addElement(CircuitElement e) {
        if (e == null) return;
        if (gates.containsKey(e.getId())) {
            return;
        }
        if (e instanceof InputElement input && !inputs.contains(input)) {
            inputs.add(input);
        } else if (e instanceof OutputElement output && !outputs.contains(output)) {
            outputs.add(output);
        } else if (e instanceof Gate gate) {
            gates.put(e.getId(), gate);
        }
    }

    public void removeElement(String id) {
        Gate removed = gates.remove(id);
        if (removed == null) {
            return;
        }
        inputs.removeIf(el -> el.getId().equals(id));
        outputs.removeIf(el -> el.getId().equals(id));
        wireSegments.removeIf(seg -> {
            boolean startBelongs = seg.getStartPort() != null
                    && seg.getStartPort().getOwner().getId().equals(id);
            boolean endBelongs = seg.getEndPort() != null
                    && seg.getEndPort().getOwner().getId().equals(id);
            if (startBelongs || endBelongs) {
                if (seg.getStartNode() != null) seg.getStartNode().removeSegment(seg);
                if (seg.getEndNode() != null) seg.getEndNode().removeSegment(seg);
                return true;
            }
            return false;
        });
        pointNodes.removeIf(node -> node.getConnectedSegments().isEmpty());
    }

    public void addWireSegment(WireSegment seg) {
        if (seg == null || wireSegments.contains(seg)) {
            return;
        }
        wireSegments.add(seg);
        if (seg.getStartNode() != null) {
            seg.getStartNode().addSegment(seg);
            if (!pointNodes.contains(seg.getStartNode())) {
                pointNodes.add(seg.getStartNode());
            }
        }
        if (seg.getEndNode() != null) {
            seg.getEndNode().addSegment(seg);
            if (!pointNodes.contains(seg.getEndNode())) {
                pointNodes.add(seg.getEndNode());
            }
        }
    }

    public void removeWireSegment(WireSegment seg) {
        if (seg == null) {
            return;
        }
        wireSegments.remove(seg);
        if (seg.getStartNode() != null) seg.getStartNode().removeSegment(seg);
        if (seg.getEndNode() != null) seg.getEndNode().removeSegment(seg);
        pointNodes.removeIf(node -> node.getConnectedSegments().isEmpty());
    }

    public void addPointNode(PointNode node) {
        if (node != null && !pointNodes.contains(node)) {
            pointNodes.add(node);
        }
    }

    public void removePointNode(PointNode node) {
        pointNodes.remove(node);
    }

    public boolean containsElement(String id) {
        return gates.containsKey(id);
    }

    public boolean containsElement(CircuitElement element) {
        return element != null && gates.containsKey(element.getId());
    }

    public Gate getElement(String id) {
        return gates.get(id);
    }

    public Map<String, Gate> getgates() {
        return gates;
    }

    public List<WireSegment> getWireSegments() {
        return wireSegments;
    }

    public List<PointNode> getPointNodes() {
        return pointNodes;
    }

    public List<InputElement> getInputs() {
        return inputs;
    }

    public List<OutputElement> getOutputs() {
        return outputs;
    }

    public PointNode splitSegmentAtJunction(WireSegment existingSeg, double jx, double jy) {
        PointNode junction = new PointNode(jx, jy);
        addPointNode(junction);

        WireSegment segA;
        WireSegment segB;

        if (existingSeg.getStartPort() != null) {
            segA = WireSegment.portToNode(
                    existingSeg.getStartPort(),
                    existingSeg.getX1(), existingSeg.getY1(),
                    junction);
        } else {
            segA = WireSegment.nodeToNode(existingSeg.getStartNode(), junction);
        }

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
