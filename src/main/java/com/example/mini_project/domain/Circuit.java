package com.example.mini_project.domain;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    // **********************************************************
    public void removeElement(String id) {
        // to be implemented
    }

    private boolean belongsTo(Wire w, String elementId) {
        // to be implemented
        return false;
    }

    public boolean addWire(Port source, Port dest) {
        // to be implemented
        return false;
    }

    public void removeWire(String id) {
        // to be implemented
    }
    // ***************************************************
}
