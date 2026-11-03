package com.example.mini_project.validation;
import com.example.mini_project.domain.*;
import java.util.*;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
public class CircuitValidator {
    //prajw : start
    private final List<String> errors = new ArrayList<>();
    private final Set<String> invalidElementIds = new HashSet<>();
    private final Set<String> invalidWireIds = new HashSet<>();
    private final Set<Port> invalidPorts = new HashSet<>();

    /** Returns a snapshot of every validation error found by this validator. */
    public List<String> getErrors() {
        return List.copyOf(errors);
    }

    public Set<String> getInvalidElementIds() {
        return Set.copyOf(invalidElementIds);
    }

    public Set<String> getInvalidWireIds() {
        return Set.copyOf(invalidWireIds);
    }

    public Set<Port> getInvalidPorts() {
        return Set.copyOf(invalidPorts);
    }

    // Centralizes validation messages so the editor can display them together.
    //highlight validation errored elems
    private void reportError(String message, Object... targets) {
        errors.add(message);
        for (Object target : targets) {
            if (target instanceof Port port) {
                invalidElementIds.add(port.getOwner().getId());
                invalidPorts.add(port);
            } else if (target instanceof CircuitElement element) {
                invalidElementIds.add(element.getId());
            } else if (target instanceof WireSegment wire) {
                invalidWireIds.add(wire.getId());
            } else if (target instanceof PointNode node) {
                for (WireSegment wire : node.getConnectedSegments()) {
                    invalidWireIds.add(wire.getId());
                }
            }
        }
    }

    private void reportSubCircuitError(String message, SubCircuit subCircuit) {
        reportError(message);
        /* invalidElementIds.addAll(subCircuit.getgates().keySet());
        for (InputElement input : subCircuit.getInputs()) {
            invalidElementIds.add(input.getId());
        }
        for (OutputElement output : subCircuit.getOutputs()) {
            invalidElementIds.add(output.getId());
        }
        for (WireSegment wire : subCircuit.getWireSegments()) {
            invalidWireIds.add(wire.getId());
        }*/
    }
    //prajw: end

    public boolean validateDanglingWires(Circuit circuit) {
        boolean valid = true;
        for (WireSegment wire : circuit.getWireSegments()) {
            if (wire.getStartPort() == null) {
                PointNode node = wire.getStartNode();
                if (node == null ||
                    countConnections(circuit, node) < 2) {
                    System.err.println("VALIDATION ERROR: Wire " + wire.getId() + " has a dangling start.");
                    reportError("VALIDATION ERROR: Wire " + wire.getId() + " has a dangling start.", wire);
                    valid = false;
                }
            }
            if (wire.getEndPort() == null) {
                PointNode node = wire.getEndNode();
                if (node == null ||
                    countConnections(circuit, node) < 2) {
                    System.err.println("VALIDATION ERROR: Wire " + wire.getId() + " has a dangling end.");
                    reportError("VALIDATION ERROR: Wire " + wire.getId() + " has a dangling end.", wire);
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
                        reportError("Validation Error: Gate " + gate.getId() + " is not connected at input: " + port.getId(), port);
                        valid = false;
                    }
                }
            } else if (element instanceof OutputElement led) {
                Port port = led.getInputPort();
                if (!isPortConnected(circuit, port)) {
                    LocalTime time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
                    System.err.println("VALIDATION ERROR: LED " + led.getId() + " has an unconnected input");
                    reportError("VALIDATION ERROR: LED " + led.getId() + " has an unconnected input", port);
                    valid = false;
                }
            }
        }
        return valid;
    }

    public boolean validateOutputPorts(Circuit circuit) {
        boolean valid = true;
        for (CircuitElement element : circuit.getElements().values()) {
            if (!(element instanceof Gate gate)) continue;

            Port output = gate.getOutputPort();
            if (!isPortConnected(circuit, output)) {
                LocalTime time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
                String message = "VALIDATION ERROR: Gate " + gate.getId()
                        + " has an unconnected output: " + output.getId();
                System.err.println(time + " - " + message);
                reportError(message, output);
                valid = false;
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
        if (circuit.getElements().isEmpty()) {
            LocalTime time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
            System.err.println(time + " - Validation Error: Circuit is empty.");
            reportError("VALIDATION ERROR: Circuit is empty.");
            return false;
        }

        boolean hasInput = false;
        boolean hasOutput = false;
        for (CircuitElement element : circuit.getElements().values()) {
            hasInput |= element instanceof InputElement;
            hasOutput |= element instanceof OutputElement;
        }

        boolean valid = true;
        if (!hasInput) {
            LocalTime time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
            String message = "VALIDATION ERROR: Circuit has no input switch.";
            System.err.println(time + " - " + message);
            reportError(message);
            valid = false;
        }
        if (!hasOutput) {
            LocalTime time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
            String message = "VALIDATION ERROR: Circuit has no output LED.";
            System.err.println(time + " - " + message);
            reportError(message);
            valid = false;
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
                
        // prajwal : start -> each of their gates and switches as dead logic.
        Set<String> ledlessIds = new HashSet<>();
        for (SubCircuit sub : circuit.splitIntoDisjointSubCircuits()) {
            if (sub.getOutputs().isEmpty()) {
                ledlessIds.addAll(sub.getgates().keySet());
                for (InputElement in : sub.getInputs()) {
                    ledlessIds.add(in.getId());
                }
            }
        }
        //prajw : end -> 
        // Subcircuits with no LED already get a "no LED" error. Don't also flag
        boolean valid = true;
        for (CircuitElement element : circuit.getElements().values()) {
            if ((element instanceof Gate || element instanceof InputElement)
                    && !useful.contains(element)
                    && !ledlessIds.contains(element.getId())) {
                LocalTime time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
                System.err.println(time + " - Validation Error: " + element.getId() + " has no connection to any LED.");
                reportError("VALIDATION ERROR: " + element.getId() + " does not contribute to any LED.", element);
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
                    reportError("VALIDATION ERROR: Wire " + wire.getId() + " has an invalid start node.", wire);
                    valid = false;
                }
            }
            if (endNode != null) {
                if (!nodes.contains(endNode) || !endNode.getConnectedSegments().contains(wire)) {
                    LocalTime time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
                    System.err.println(time + " - Validation Error: Wire " + wire.getId() + " has an invalid end node.");
                    reportError("VALIDATION ERROR: Wire " + wire.getId() + " has an invalid end node.", wire);
                    valid = false;
                }
            }
            if (wire.getStartPort() != null &&
                circuit.getElement(wire.getStartPort().getOwner().getId()) != wire.getStartPort().getOwner()) {
                    LocalTime time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
                    System.err.println(time + " - Validation Error: Wire " + wire.getId() + " references a removed start component.");
                    reportError("VALIDATION ERROR: Wire " + wire.getId() + " references a removed start component.", wire, wire.getStartPort());
                    valid = false;
            }
            if (wire.getEndPort() != null && circuit.getElement(wire.getEndPort().getOwner().getId()) != wire.getEndPort().getOwner()) {
                LocalTime time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
                System.err.println(time + " - Validation Error: Wire " + wire.getId() + " references a removed end component.");
                reportError("VALIDATION ERROR: Wire " + wire.getId() + " references a removed end component.", wire, wire.getEndPort());
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
                reportError("VALIDATION ERROR: Node " + node.getId() + " has inconsistent wire connections.", node, null);
                valid = false;
            }
            for (WireSegment wire : node.getConnectedSegments()) {
                if (!wires.contains(wire) || (wire.getStartNode() != node && wire.getEndNode() != node)) {
                    LocalTime time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
                    System.err.println(time + " - Validation Error: Node " + node.getId() + " references an invalid wire.");
                    reportError("VALIDATION ERROR: Node " + node.getId() + " references an invalid wire.", wire, null);
                    valid = false;
                }
            }
            if (node.isJunction() != (actualConnections >= 3)) {
                LocalTime time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
                System.err.println(time + " - Validation Error: Node " + node.getId() + "has incorrect junction status.");
                reportError("VALIDATION ERROR: Node " + node.getId() + " has incorrect junction status.", node);
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
            Set<WireSegment> networkSegments = new HashSet<>();
            queue.add(start);
            visited.add(start);
            while (!queue.isEmpty()) {
                WireSegment current = queue.remove();
                networkSegments.add(current);
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
                String message = "VALIDATION ERROR: Wire network containing " + start.getId() + " has no signal source.";
                System.err.println(time + " - " + message);
                reportError(message, networkSegments.toArray());
                valid = false;
            } else if (drivers.size() > 1) {
                LocalTime time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
                String message = "VALIDATION ERROR: Wire network containing " + start.getId() + " has multiple signal sources.";
                System.err.println(time + " - " + message);
                List<Object> targets = new ArrayList<>(networkSegments);
                targets.addAll(drivers);
                reportError(message, targets.toArray());
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