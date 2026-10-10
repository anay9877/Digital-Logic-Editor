package com.example.mini_project.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class Circuit {
    private final Map<String, CircuitElement> elements = new HashMap<>();
    private final List<Wire> wires = new ArrayList<>();

    public void addElement(CircuitElement e) {
        elements.put(e.getId(), e);
    }

    public CircuitElement getElement(String id) {
        return elements.get(id);
    }

    public Map<String, CircuitElement> getElements() {
        return elements;
    }

    public List<Wire> getWires() {
        return wires;
    }

    public void removeElement(String id) {
        elements.remove(id);
        wires.removeIf(w -> belongsTo(w, id));
    }

    private boolean belongsTo(Wire w, String elementId) {
        return w.getSourcePortId().startsWith(elementId + "-") ||
                w.getDestPortId().startsWith(elementId + "-");
    }

    public boolean addWire(Port source, Port dest) {
        String wireId = "W-" + (wires.size() + 1);
        Wire w = new Wire(wireId, source.getId(), dest.getId());
        wires.add(w);
        source.setConnectedWire(w);
        dest.setConnectedWire(w);
        return true;
    }

    public void removeWire(String id) {
        wires.removeIf(w -> w.getId().equals(id));
    }

    /**
     * Partitions all elements and wires on the canvas into independent disjoint
     * sub-circuits using BFS.
     */
    public List<SubCircuit> getDisjointSubCircuits() {
        List<SubCircuit> subCircuits = new ArrayList<>();
        Set<String> visitedElements = new HashSet<>();
        int subCircuitCounter = 1;

        for (CircuitElement element : elements.values()) {
            if (!visitedElements.contains(element.getId())) {
                SubCircuit sub = extractConnectedComponent(element, visitedElements,
                        "SubCircuit-" + subCircuitCounter++);
                subCircuits.add(sub);
            }
        }
        return subCircuits;
    }

    private SubCircuit extractConnectedComponent(CircuitElement start, Set<String> visitedElements,
            String subCircuitId) {
        SubCircuit subCircuit = new SubCircuit(subCircuitId);
        Queue<CircuitElement> queue = new LinkedList<>();
        Set<String> subCircuitWireIds = new HashSet<>();

        queue.add(start);
        visitedElements.add(start.getId());

        while (!queue.isEmpty()) {
            CircuitElement curr = queue.poll();
            subCircuit.addElement(curr);

            // Find all wires attached to curr (either as source or destination)
            for (Wire w : wires) {
                boolean isSource = w.getSourcePortId().startsWith(curr.getId() + "-");
                boolean isDest = w.getDestPortId().startsWith(curr.getId() + "-");

                if (isSource || isDest) {
                    if (subCircuitWireIds.add(w.getId())) {
                        subCircuit.addWire(w);
                    }

                    // Find neighbor element on the other side of this wire
                    String otherPortId = isSource ? w.getDestPortId() : w.getSourcePortId();
                    String neighborId = extractElementIdFromPort(otherPortId);
                    CircuitElement neighbor = elements.get(neighborId);

                    if (neighbor != null && visitedElements.add(neighbor.getId())) {
                        queue.add(neighbor);
                    }
                }
            }
        }

        return subCircuit;
    }

    private String extractElementIdFromPort(String portId) {
        int lastDashIndex = portId.lastIndexOf('-');
        return (lastDashIndex != -1) ? portId.substring(0, lastDashIndex) : portId;
    }
}
