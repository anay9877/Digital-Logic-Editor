package com.example.mini_project.validation;

import com.example.mini_project.domain.*;
import java.util.*;

/** Builds the dependency order used by the simulator. */
public class CircuitTopology {
    private final Set<String> invalidElementIds = new HashSet<>();
    private final Set<String> invalidWireIds = new HashSet<>();

    public Set<String> getInvalidElementIds() {
        return Set.copyOf(invalidElementIds);
    }

    public Set<String> getInvalidWireIds() {
        return Set.copyOf(invalidWireIds);
    }

    public List<CircuitElement> sort(Circuit circuit) {
        invalidElementIds.clear();
        invalidWireIds.clear();

        // Every element must appear, including switches, LEDs and isolated gates.
        Map<CircuitElement, Set<CircuitElement>> next = new IdentityHashMap<>();
        Map<CircuitElement, Integer> indegree = new IdentityHashMap<>();
        for (CircuitElement element : circuit.getElements().values()) {
            next.put(element, Collections.newSetFromMap(new IdentityHashMap<>()));
            indegree.put(element, 0);
        }

        // Physical wires are undirected: a port or junction is adjacent to its neighbor.
        Map<Object, Set<Object>> wires = new IdentityHashMap<>();
        Map<Object, Set<WireSegment>> adjacentSegments = new IdentityHashMap<>();
        for (WireSegment segment : circuit.getWireSegments()) {
            Object a = segment.getStartPort() != null
                    ? segment.getStartPort() : segment.getStartNode();
            Object b = segment.getEndPort() != null
                    ? segment.getEndPort() : segment.getEndNode();
            if (a == null || b == null) {
                //highlight validation errored elems
                invalidWireIds.add(segment.getId());
                throw new IllegalStateException("Wire has a missing endpoint: " + segment.getId());
            }
            wires.computeIfAbsent(a, k -> Collections.newSetFromMap(new IdentityHashMap<>())).add(b);
            wires.computeIfAbsent(b, k -> Collections.newSetFromMap(new IdentityHashMap<>())).add(a);
            adjacentSegments.computeIfAbsent(a, k -> new HashSet<>()).add(segment);
            adjacentSegments.computeIfAbsent(b, k -> new HashSet<>()).add(segment);
        }

        // Explore each electrical network and translate it to directed element dependencies.
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Object start : wires.keySet()) {
            if (!visited.add(start)) continue;
            Queue<Object> queue = new ArrayDeque<>();
            queue.add(start);
            Set<CircuitElement> sources = Collections.newSetFromMap(new IdentityHashMap<>());
            Set<CircuitElement> targets = Collections.newSetFromMap(new IdentityHashMap<>());
            Set<WireSegment> networkSegments = Collections.newSetFromMap(new IdentityHashMap<>());
            while (!queue.isEmpty()) {
                Object current = queue.remove();
                networkSegments.addAll(adjacentSegments.getOrDefault(current, Set.of()));
                if (current instanceof Port port) {
                    if (port.getType() == PortType.OUTPUT) {
                        sources.add(port.getOwner());
                    } else {
                        targets.add(port.getOwner());
                    }
                }
                for (Object neighbor : wires.getOrDefault(current, Collections.emptySet())) {
                    if (visited.add(neighbor)) queue.add(neighbor);
                }
            }
            if (sources.size() > 1) {
                //highlight validation errored elems
                sources.forEach(element -> invalidElementIds.add(element.getId()));
                targets.forEach(element -> invalidElementIds.add(element.getId()));
                networkSegments.forEach(segment -> invalidWireIds.add(segment.getId()));
                throw new IllegalStateException("An electrical network has multiple drivers.");
            }
            for (CircuitElement source : sources) {
                for (CircuitElement target : targets) {
                    if (!next.containsKey(source) || !next.containsKey(target)) {
                        //highlight validation errored elems
                        invalidElementIds.add(source.getId());
                        invalidElementIds.add(target.getId());
                        networkSegments.forEach(segment -> invalidWireIds.add(segment.getId()));
                        throw new IllegalStateException("A wire references a component outside this circuit.");
                    }
                    if (next.get(source).add(target)) {
                        indegree.put(target, indegree.get(target) + 1);
                    }
                }
            }
        }

        // Kahn's algorithm: evaluate each element only after all upstream elements.
        Queue<CircuitElement> ready = new ArrayDeque<>();
        for (CircuitElement element : circuit.getElements().values()) {
            if (indegree.get(element) == 0) ready.add(element);
        }
        List<CircuitElement> order = new ArrayList<>();
        while (!ready.isEmpty()) {
            CircuitElement current = ready.remove();
            order.add(current);
            for (CircuitElement target : next.get(current)) {
                int remaining = indegree.get(target) - 1;
                indegree.put(target, remaining);
                if (remaining == 0) ready.add(target);
            }
        }
        if (order.size() != circuit.getElements().size()) {
            //highlight validation errored elems
            for (CircuitElement element : circuit.getElements().values()) {
                if (indegree.get(element) > 0) {
                    invalidElementIds.add(element.getId());
                }
            }
            throw new IllegalStateException("Combinational cycle detected; cannot topologically sort.");
        }
        return order;
    }
}
