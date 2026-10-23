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
                    System.err.println(time + " - VALIDATION ERROR: Wire " + wire.getId() + " has a dangling start.");
                    valid = false;
                }
            }
            if (wire.getEndPort() == null) {
                PointNode node = wire.getEndNode();
                if (node == null || countConnections(circuit, node) < 2) {
                    LocalTime time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
                    System.err.println(time + " - VALIDATION ERROR: Wire " + wire.getId() + " has a dangling end.");
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
                        System.err.println(time + " - VALIDATION ERROR: Gate " + gate.getId() + " has an unconnected input: " + port.getId());
                        valid = false;
                    }
                }
            } else if (element instanceof OutputElement led) {
                Port port = led.getInputPort();
                if (!isPortConnected(circuit, port)) {
                    LocalTime time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
                    System.err.println(time + " - VALIDATION ERROR: LED " + led.getId() + " has an unconnected input");
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
            System.err.println(time + " - VALIDATION ERROR: Circuit is empty.");
            return false;
        }
        for (SubCircuit subCircuit : circuit.splitIntoDisjointSubCircuits()) {
            if (subCircuit.getInputs().isEmpty()) {
                LocalTime time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
                System.err.println(time + " - VALIDATION ERROR: " + subCircuit.getId() + " has no input switch.");
                valid = false;
            }
            if (subCircuit.getOutputs().isEmpty()) {
                LocalTime time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
                System.err.println(time + " - VALIDATION ERROR: " + subCircuit.getId() + " has no output LED.");
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
                System.err.println(time + " - VALIDATION ERROR: " + element.getId() + " does not contribute to any LED.");
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
                    System.err.println(time + " - VALIDATION ERROR: Wire " + wire.getId() + " has an invalid start node.");
                    valid = false;
                }
            }
            if (endNode != null) {
                if (!nodes.contains(endNode) || !endNode.getConnectedSegments().contains(wire)) {
                    LocalTime time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
                    System.err.println(time + " - VALIDATION ERROR: Wire " + wire.getId() + " has an invalid end node.");
                    valid = false;
                }
            }
            if (wire.getStartPort() != null &&
                circuit.getElement(wire.getStartPort().getOwner().getId()) != wire.getStartPort().getOwner()) {
                    LocalTime time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
                    System.err.println(time + " - VALIDATION ERROR: Wire " + wire.getId() + " references a removed start component.");
                    valid = false;
            }
            if (wire.getEndPort() != null && circuit.getElement(wire.getEndPort().getOwner().getId()) != wire.getEndPort().getOwner()) {
                LocalTime time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
                System.err.println(time + " - VALIDATION ERROR: Wire " + wire.getId() + " references a removed end component.");
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
                System.err.println(time + " - VALIDATION ERROR: Node " + node.getId() + " has inconsistent wire connections.");
                valid = false;
            }
            for (WireSegment wire : node.getConnectedSegments()) {
                if (!wires.contains(wire) || (wire.getStartNode() != node && wire.getEndNode() != node)) {
                    LocalTime time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
                    System.err.println(time + " - VALIDATION ERROR: Node " + node.getId() + " references an invalid wire.");
                    valid = false;
                }
            }
            if (node.isJunction() != (actualConnections >= 3)) {
                LocalTime time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
                System.err.println(time + " - VALIDATION ERROR: Node " + node.getId() + "has incorrect junction status.");
                valid = false;
            }
        }
        return valid;
    }

}