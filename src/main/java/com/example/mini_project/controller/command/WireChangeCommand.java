package com.example.mini_project.controller.command;

import com.example.mini_project.domain.Circuit;
import com.example.mini_project.domain.PointNode;
import com.example.mini_project.domain.Port;
import com.example.mini_project.domain.WireSegment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class WireChangeCommand implements Command {
    private record SegmentState(double x1, double y1, double x2, double y2,
                                Port startPort, PointNode startNode,
                                Port endPort, PointNode endNode) {
        static SegmentState capture(WireSegment segment) {
            return new SegmentState(segment.getX1(), segment.getY1(), segment.getX2(), segment.getY2(),
                    segment.getStartPort(), segment.getStartNode(), segment.getEndPort(), segment.getEndNode());
        }

        void applyTo(WireSegment segment) {
            segment.setX1(x1);
            segment.setY1(y1);
            segment.setX2(x2);
            segment.setY2(y2);
            if (startPort != null) segment.setStartPort(startPort);
            else segment.setStartNode(startNode);
            if (endPort != null) segment.setEndPort(endPort);
            else segment.setEndNode(endNode);
        }
    }

    private record NodeState(double x, double y, List<WireSegment> segments) {
        static NodeState capture(PointNode node) {
            return new NodeState(node.getX(), node.getY(), List.copyOf(node.getConnectedSegments()));
        }

        void applyTo(PointNode node) {
            node.setX(x);
            node.setY(y);
            node.restoreConnections(segments);
        }
    }

    private record Change<T>(T before, T after) {}

    public static final class Image {
        private final Map<WireSegment, SegmentState> segments = new LinkedHashMap<>();
        private final Map<PointNode, NodeState> nodes = new LinkedHashMap<>();
    }

    private final Circuit circuit;
    private final Map<WireSegment, Change<SegmentState>> segmentChanges;
    private final Map<PointNode, Change<NodeState>> nodeChanges;

    private WireChangeCommand(Circuit circuit,
                              Map<WireSegment, Change<SegmentState>> segmentChanges,
                              Map<PointNode, Change<NodeState>> nodeChanges) {
        this.circuit = circuit;
        this.segmentChanges = Map.copyOf(segmentChanges);
        this.nodeChanges = Map.copyOf(nodeChanges);
    }

    public static Image capture(Circuit circuit) {
        Image image = new Image();
        for (WireSegment segment : circuit.getWireSegments()) {
            image.segments.put(segment, SegmentState.capture(segment));
        }
        for (PointNode node : circuit.getPointNodes()) image.nodes.put(node, NodeState.capture(node));
        return image;
    }

    public static WireChangeCommand diff(Circuit circuit, Image before, Image after) {
        Map<WireSegment, Change<SegmentState>> segmentChanges =
                diffMaps(before.segments, after.segments);
        Map<PointNode, Change<NodeState>> nodeChanges = diffMaps(before.nodes, after.nodes);
        if (segmentChanges.isEmpty() && nodeChanges.isEmpty()) return null;
        return new WireChangeCommand(circuit, segmentChanges, nodeChanges);
    }

    private static <K, V> Map<K, Change<V>> diffMaps(Map<K, V> before, Map<K, V> after) {
        Map<K, Change<V>> changes = new LinkedHashMap<>();
        Set<K> keys = new LinkedHashSet<>(before.keySet());
        keys.addAll(after.keySet());
        for (K key : keys) {
            V oldState = before.get(key);
            V newState = after.get(key);
            if (oldState == null || newState == null || !oldState.equals(newState)) {
                changes.put(key, new Change<>(oldState, newState));
            }
        }
        return changes;
    }

    @Override
    public void undo() {
        apply(true);
    }

    @Override
    public void redo() {
        apply(false);
    }

    private void apply(boolean undo) {
        List<WireSegment> segmentsToRemove = new ArrayList<>();
        for (var entry : segmentChanges.entrySet()) {
            SegmentState from = undo ? entry.getValue().after() : entry.getValue().before();
            SegmentState to = undo ? entry.getValue().before() : entry.getValue().after();
            if (from != null && to == null) segmentsToRemove.add(entry.getKey());
            else if (from == null && to != null && !containsIdentity(circuit.getWireSegments(), entry.getKey())) {
                circuit.getWireSegments().add(entry.getKey());
            }
        }
        removeAllByIdentity(circuit.getWireSegments(), segmentsToRemove);

        List<PointNode> nodesToRemove = new ArrayList<>();
        for (var entry : nodeChanges.entrySet()) {
            NodeState from = undo ? entry.getValue().after() : entry.getValue().before();
            NodeState to = undo ? entry.getValue().before() : entry.getValue().after();
            if (from != null && to == null) nodesToRemove.add(entry.getKey());
            else if (from == null && to != null && !containsIdentity(circuit.getPointNodes(), entry.getKey())) {
                circuit.getPointNodes().add(entry.getKey());
            }
        }
        removeAllByIdentity(circuit.getPointNodes(), nodesToRemove);

        for (var entry : segmentChanges.entrySet()) {
            SegmentState target = undo ? entry.getValue().before() : entry.getValue().after();
            if (target != null) target.applyTo(entry.getKey());
        }
        for (var entry : nodeChanges.entrySet()) {
            NodeState target = undo ? entry.getValue().before() : entry.getValue().after();
            if (target != null) target.applyTo(entry.getKey());
        }
    }

    private static <T> boolean containsIdentity(List<T> list, T target) {
        for (T item : list) if (item == target) return true;
        return false;
    }

    private static <T> void removeAllByIdentity(List<T> list, List<T> doomed) {
        if (doomed.isEmpty()) return;
        Set<T> identities = Collections.newSetFromMap(new IdentityHashMap<>());
        identities.addAll(doomed);
        list.removeIf(identities::contains);
    }

    @Override
    public String label() {
        return "Wire change (" + segmentChanges.size() + " segment(s), "
                + nodeChanges.size() + " node(s))";
    }
}
