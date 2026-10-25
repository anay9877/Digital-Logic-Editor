package com.example.mini_project.validation;
import com.example.mini_project.domain.*;
import java.util.*;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
public class CircuitValidator {
    public boolean validateDanglingWires(Circuit circuit) {
        boolean valid = true;
        for (WireSegment wire : circuit.getWireSegments()) {
            if (wire.getStartPort() == null) {
                PointNode node = wire.getStartNode();
                if (node == null || countConnections(circuit, node) < 2) {
                    LocalTime time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
                    System.err.println(time + " - Validation error: Wire " + wire.getId() + " should have a valid starting node/port.");
                    valid = false;
                }
            }
            if (wire.getEndPort() == null) {
                PointNode node = wire.getEndNode();
                if (node == null || countConnections(circuit, node) < 2) {
                    LocalTime time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
                    System.err.println(time + " - Validation Error: Wire " + wire.getId() + " has a dangling end.");
                    valid = false;
                }
            }
        }
        return valid;
    }

    private int countConnections(Circuit circuit, PointNode node) {
        int count = 0;
        for (WireSegment wire : circuit.getWireSegments()) {
            if (wire.getStartNode() == node || wire.getEndNode() == node) {
                count++;
            }
        }
        return count;
    }

    public boolean validateInputPorts(Circuit circuit) {
        boolean valid = true;
        for (CircuitElement element :
                circuit.getElements().values()) {
            if (element instanceof Gate gate) {
                for (Port port : gate.getInputPorts()) {
                    if (!isPortConnected(circuit, port)) {
                        LocalTime time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
                        System.err.println(time + " - Validation Error: Gate " + gate.getId() + " is not connected at input: " + port.getId());
                        valid = false;
                    }
                }
            } else if (element instanceof OutputElement led) {
                Port port = led.getInputPort();
                if (!isPortConnected(circuit, port)) {
                    LocalTime time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
                    System.err.println(time + " - Validation Error: LED " + led.getId() + " is not connected to any wire.");
                    valid = false;
                }
            }
        }
        return valid;
    }
    private boolean isPortConnected(Circuit circuit, Port port) {
        for (WireSegment wire : circuit.getWireSegments()) {
            if (wire.getStartPort() == port || wire.getEndPort() == port) {
                return true;
            }
        }
        return false;
    }
    public boolean validateInputsAndOutputs(Circuit circuit) {
        boolean valid = true;
        if (circuit.getElements().isEmpty()) {
            LocalTime time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
            System.err.println(time + " - Validation Error: Circuit is empty.");
            return false;
        }
        for (SubCircuit subCircuit : circuit.splitIntoDisjointSubCircuits()) {
            if (subCircuit.getInputs().isEmpty()) {
                LocalTime time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
                System.err.println(time + " - Validation Error: " + subCircuit.getId() + " needs atleast one input switch.");
                valid = false;
            }
            if (subCircuit.getOutputs().isEmpty()) {
                LocalTime time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
                System.err.println(time + " - Validation Error: " + subCircuit.getId() + " needs atleast one LED.");
                valid = false;
            }
        }
        return valid;
    }
    public boolean validateDeadLogic(Circuit circuit) {
        Map<Object, List<Object>> wires = new IdentityHashMap<>();
        for (WireSegment segment : circuit.getWireSegments()) {
            Object a = segment.getStartPort() != null ? segment.getStartPort() : segment.getStartNode();
            Object b = segment.getEndPort() != null ? segment.getEndPort() : segment.getEndNode();
            if (a == null || b == null) continue;
            wires.computeIfAbsent(a, k -> new ArrayList<>()).add(b);
            wires.computeIfAbsent(b, k -> new ArrayList<>()).add(a);
        }
        // For each component, store the components that supply it.
        Map<CircuitElement, Set<CircuitElement>> predecessors = new IdentityHashMap<>();
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Object endpoint : wires.keySet()) {
            if (!visited.add(endpoint)) continue;
            Queue<Object> queue = new ArrayDeque<>();
            queue.add(endpoint);
            Set<CircuitElement> sources = Collections.newSetFromMap(new IdentityHashMap<>());
            Set<CircuitElement> destinations = Collections.newSetFromMap(new IdentityHashMap<>());
            while (!queue.isEmpty()) {
                Object current = queue.remove();
                if (current instanceof Port port) {
                    if (port.getType() == PortType.OUTPUT) {
                        sources.add(port.getOwner());
                    } else {
                        destinations.add(port.getOwner());
                    }
                }
                for (Object next : wires.getOrDefault(current, List.of())) {
                    if (visited.add(next)) {
                        queue.add(next);
                    }
                }
            }
            for (CircuitElement destination : destinations) {
                predecessors.computeIfAbsent(
                        destination, k -> new HashSet<>()
                ).addAll(sources);
            }
        }
        // Start from LEDs and move backward toward switches.
        Set<CircuitElement> useful = Collections.newSetFromMap(new IdentityHashMap<>());
        Queue<CircuitElement> queue = new ArrayDeque<>();
        for (CircuitElement element : circuit.getElements().values()) {
            if (element instanceof OutputElement) {
                useful.add(element);
                queue.add(element);
            }
        }
        while (!queue.isEmpty()) {
            CircuitElement current = queue.remove();
            for (CircuitElement previous :
                    predecessors.getOrDefault(current, Set.of())) {
                if (useful.add(previous)) {
                    queue.add(previous);
                }
            }
        }
        boolean valid = true;
        for (CircuitElement element : circuit.getElements().values()) {
            if ((element instanceof Gate || element instanceof InputElement) && !useful.contains(element)) {
                LocalTime time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
                System.err.println(time + " - Validation Error: " + element.getId() + " has no connection to any LED.");
                valid = false;
            }
        }
        return valid;
    }
    public boolean validateWireReferences(Circuit circuit) {
        boolean valid = true;
        Set<WireSegment> wires = new HashSet<>(circuit.getWireSegments());
        Set<PointNode> nodes = new HashSet<>(circuit.getPointNodes());
        for (WireSegment wire : circuit.getWireSegments()) {
            PointNode startNode = wire.getStartNode();
            PointNode endNode = wire.getEndNode();
            if (startNode != null) {
                if (!nodes.contains(startNode) || !startNode.getConnectedSegments().contains(wire)) {
                    LocalTime time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
                    System.err.println(time + " - Validation Error: Wire " + wire.getId() + " has an invalid start node/port.");
                    valid = false;
                }
            }
            if (endNode != null) {
                if (!nodes.contains(endNode) || !endNode.getConnectedSegments().contains(wire)) {
                    LocalTime time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
                    System.err.println(time + " - Validation Error: Wire " + wire.getId() + " has an invalid end node/port.");
                    valid = false;
                }
            }
            if (wire.getStartPort() != null &&
                circuit.getElement(wire.getStartPort().getOwner().getId()) != wire.getStartPort().getOwner()) {
                    LocalTime time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
                    System.err.println(time + " - Validation Error: Wire " + wire.getId() + " references a removed start component.");
                    valid = false;
            }
            if (wire.getEndPort() != null && circuit.getElement(wire.getEndPort().getOwner().getId()) != wire.getEndPort().getOwner()) {
                LocalTime time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
                System.err.println(time + " - Validation Error: Wire " + wire.getId() + " references a removed end component.");
                valid = false;
            }
        }
        for (PointNode node : circuit.getPointNodes()) {
            int actualConnections = 0;
            for (WireSegment wire : circuit.getWireSegments()) {
                if (wire.getStartNode() == node || wire.getEndNode() == node) {
                    actualConnections++;
                }
            }
            if (actualConnections != node.getConnectedSegments().size()) {
                LocalTime time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
                System.err.println(time + " - Validation Error: Node " + node.getId() + " has inconsistent wire connections.");
                valid = false;
            }
            for (WireSegment wire : node.getConnectedSegments()) {
                if (!wires.contains(wire) || (wire.getStartNode() != node && wire.getEndNode() != node)) {
                    LocalTime time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
                    System.err.println(time + " - Validation Error: Node " + node.getId() + " is linked to a wire that no longer exists or is disconnected.");
                    valid = false;
                }
            }
            if (node.isJunction() != (actualConnections >= 3)) {
                LocalTime time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
                System.err.println(time + " - Validation Error: Node " + node.getId() + "has incorrect junction status.");
                valid = false;
            }
        }
        return valid;
    }

    public boolean validateWireDrivers(Circuit circuit) {
        boolean valid = true;
        Set<WireSegment> visited = new HashSet<>();
        for (WireSegment start : circuit.getWireSegments()) {
            if (visited.contains(start)) continue;
            // Find all wire segments in this electrical network.
            Queue<WireSegment> queue = new ArrayDeque<>();
            Set<Port> drivers = Collections.newSetFromMap(new IdentityHashMap<>());
            queue.add(start);
            visited.add(start);
            while (!queue.isEmpty()) {
                WireSegment current = queue.remove();
                Port startPort = current.getStartPort();
                Port endPort = current.getEndPort();
                if (startPort != null && startPort.getType() == PortType.OUTPUT)
                    drivers.add(startPort);

                if (endPort != null && endPort.getType() == PortType.OUTPUT)
                    drivers.add(endPort);
                // Travel through the junctions and corners.
                PointNode[] nodes = {current.getStartNode(), current.getEndNode()};

                for (PointNode node : nodes) {
                    if (node == null) continue;
                    for (WireSegment next : circuit.getWireSegments()) {
                        if (next.getStartNode() == node || next.getEndNode() == node) {
                            if (visited.add(next)) {
                                queue.add(next);
                            }
                        }
                    }
                }

                // Also follow segments sharing the same port.
                for (WireSegment next : circuit.getWireSegments()) {
                    if (sharesPort(current, next)) {
                        if (visited.add(next)) {
                            queue.add(next);
                        }
                    }
                }
            }
            if (drivers.isEmpty()) {
                LocalTime time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
                System.err.println(time + " - Validation error: Wire network containing " + start.getId() + " has no signal source.");
                valid = false;
            } else if (drivers.size() > 1) {
                LocalTime time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
                System.err.println(time + " - Validation error: Wire network containing " + start.getId() + " has multiple signal sources.");
                valid = false;
            }
        }
        return valid;
    }

    private boolean sharesPort(WireSegment first, WireSegment second) {
        Port a = first.getStartPort();
        Port b = first.getEndPort();
        return (a != null && (a == second.getStartPort() || a == second.getEndPort())) ||
                (b != null &&(b == second.getStartPort() || b == second.getEndPort()));
    }
}