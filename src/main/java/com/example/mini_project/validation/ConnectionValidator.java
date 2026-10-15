package com.example.mini_project.validation;

import com.example.mini_project.domain.*;
import java.util.*;

public class ConnectionValidator {
    public String checkSingleDriver(Circuit circuit, Object start, Object end) {
        Map<Object, List<Object>> graph = new IdentityHashMap<>();
        for (WireSegment wire : circuit.getWireSegments()) {
            Object a = getStart(wire);
            Object b = getEnd(wire);
            if (a == null || b == null) continue;

            graph.computeIfAbsent(a, k -> new ArrayList<>()).add(b);
            graph.computeIfAbsent(b, k -> new ArrayList<>()).add(a);
        }
        Set<Port> drivers = Collections.newSetFromMap(new IdentityHashMap<>());
        drivers.addAll(findDrivers(start, graph));
        drivers.addAll(findDrivers(end, graph));
        if (drivers.size() > 1) {
            return "Multiple output ports are driving " + "the same wire network (" + drivers.size() + " sources).";
        }
        return null;
    }
    private Set<Port> findDrivers(Object endpoint, Map<Object, List<Object>> graph) {
        Set<Port> drivers = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        Queue<Object> queue = new ArrayDeque<>();
        if (endpoint instanceof WireSegment wire) {
            Object a = getStart(wire);
            Object b = getEnd(wire);
            if (a != null) queue.add(a);
            if (b != null) queue.add(b);
        } else if (endpoint != null) {
            queue.add(endpoint);
        }
        while (!queue.isEmpty()) {
            Object current = queue.remove();
            if (!visited.add(current)) continue;
            if (current instanceof Port port &&
                    port.getType() == PortType.OUTPUT) {
                drivers.add(port);
            }
            for (Object neighbour : graph.getOrDefault(current, List.of())) {
                if (!visited.contains(neighbour)) {
                    queue.add(neighbour);
                }
            }
        }
        return drivers;
    }
    private Object getStart(WireSegment wire) {
        return wire.getStartPort() != null
                ? wire.getStartPort()
                : wire.getStartNode();
    }
    private Object getEnd(WireSegment wire) {
        return wire.getEndPort() != null
                ? wire.getEndPort()
                : wire.getEndNode();
    }
    public String checkInputToInput(Object start, Object end) {
        if (start instanceof Port startPort && end instanceof Port endPort) {
            if (startPort.getType() == PortType.INPUT && endPort.getType() == PortType.INPUT) {
                return "Cannot connect two INPUT ports.";
            }
        }
        return null;
    }
    public String checkInputAlreadyConnected(Circuit circuit, Object start, Object end) {
        if (start instanceof Port port) {
            if (isOccupiedInput(circuit, port)) {
                return "Input port " + port.getId() + " already has a connection.";
            }
        }
        if (end instanceof Port port) {
            if (isOccupiedInput(circuit, port)) {
                return "Input port " + port.getId() + " already has a connection.";
            }
        }
        return null;
    }
    private boolean isOccupiedInput(Circuit circuit, Port port) {
        if (port.getType() != PortType.INPUT) return false;
        for (WireSegment segment : circuit.getWireSegments()) {
            if (segment.getStartPort() == port || segment.getEndPort() == port) return true;
        }
        return false;
    }
    public String checkCombinationalCycle(Circuit circuit, Object start, Object end) {
        Map<Object, Set<Object>> wireGraph = new IdentityHashMap<>();
        for (WireSegment segment : circuit.getWireSegments()) {
            connect(wireGraph, getStart(segment), getEnd(segment));
        }

        // Treat the proposed wire as already connected,
        // but only in this temporary graph.
        connect(wireGraph, resolveEndpoint(start), resolveEndpoint(end));
        Map<CircuitElement, Set<CircuitElement>> gateGraph = new IdentityHashMap<>();
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Object endpoint : wireGraph.keySet()) {
            if (!visited.add(endpoint)) continue;
            Queue<Object> queue = new ArrayDeque<>();
            queue.add(endpoint);
            Set<CircuitElement> sources = Collections.newSetFromMap(new IdentityHashMap<>());
            Set<CircuitElement> destinations = Collections.newSetFromMap(new IdentityHashMap<>());
            while (!queue.isEmpty()) {
                Object current = queue.remove();
                if (current instanceof Port port) {
                    CircuitElement owner = port.getOwner();
                    if (port.getType() == PortType.OUTPUT) {
                        sources.add(owner);
                    } else {
                        destinations.add(owner);
                    }
                }
                for (Object neighbour : wireGraph.getOrDefault(current, Set.of())) {
                    if (visited.add(neighbour)) {
                        queue.add(neighbour);
                    }
                }
            }
            for (CircuitElement source : sources) {
                if (!(source instanceof Gate)) continue;
                for (CircuitElement destination : destinations) {
                    if (!(destination instanceof Gate)) continue;
                    gateGraph.computeIfAbsent(
                            source, k -> new HashSet<>()
                    ).add(destination);
                }
            }
        }
        Map<CircuitElement, Integer> state =
                new IdentityHashMap<>();
        for (CircuitElement gate : gateGraph.keySet()) {
            if (hasCycle(gate, gateGraph, state)) {
                return "Combinational cycle detected.";
            }
        }
        return null;
    }
    private Object resolveEndpoint(Object endpoint) {
        if (endpoint instanceof WireSegment segment) {
            return getStart(segment);
        }
        return endpoint;
    }
    private void connect(
            Map<Object, Set<Object>> graph,
            Object a, Object b) {
        if (a == null || b == null) return;
        graph.computeIfAbsent(a, k -> Collections.newSetFromMap(new IdentityHashMap<>())).add(b);
        graph.computeIfAbsent(b, k -> Collections.newSetFromMap(new IdentityHashMap<>())).add(a);
    }

    private boolean hasCycle(CircuitElement current, Map<CircuitElement, Set<CircuitElement>> graph, Map<CircuitElement, Integer> state) {
        int status = state.getOrDefault(current, 0);
        if (status == 1) return true;
        if (status == 2) return false;
        state.put(current, 1);
        for (CircuitElement next : graph.getOrDefault(current, Set.of())) {
            if (hasCycle(next, graph, state)) {
                return true;
            }
        }
        state.put(current, 2);
        return false;
    }
    public String checkDuplicateConnection(Circuit circuit, Object start, Object end) {
        Map<Object, Set<Object>> graph = new IdentityHashMap<>();
        for (WireSegment wire : circuit.getWireSegments()) {
            Object a = getStart(wire);
            Object b = getEnd(wire);
            if (a == null || b == null) continue;
            graph.computeIfAbsent(a, k -> Collections.newSetFromMap(new IdentityHashMap<>())).add(b);
            graph.computeIfAbsent(b, k -> Collections.newSetFromMap(new IdentityHashMap<>())).add(a);
        }
        Set<Object> startPoints = getEndpoints(start);
        Set<Object> endPoints = getEndpoints(end);
        Queue<Object> queue = new ArrayDeque<>();
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Object point : startPoints) {
            queue.add(point);
        }
        while (!queue.isEmpty()) {
            Object current = queue.remove();
            if (!visited.add(current)) continue;
            if (endPoints.contains(current)) {
                return "These points are already electrically connected.";
            }
            for (Object neighbour : graph.getOrDefault(current, Set.of())) {
                if (!visited.contains(neighbour)) {
                    queue.add(neighbour);
                }
            }
        }
        return null;
    }
    private Set<Object> getEndpoints(Object endpoint) {
        Set<Object> points = Collections.newSetFromMap(new IdentityHashMap<>());
        if (endpoint instanceof WireSegment wire) {
            if (getStart(wire) != null) {
                points.add(getStart(wire));
            }
            if (getEnd(wire) != null) {
                points.add(getEnd(wire));
            }
        } else if (endpoint != null) {
            points.add(endpoint);
        }
        return points;
    }
    public String checkWireLength(double x1, double y1, double x2, double y2) {
        if (x1 == x2 && y1 == y2) {
            return "Wire cannot have zero length.";
        }
        return null;
    }
}
