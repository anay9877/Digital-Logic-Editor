package com.example.mini_project.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubCircuit {
    private final String id;
    private final Map<String, CircuitElement> elements = new HashMap<>();
    private final List<Wire> wires = new ArrayList<>();
    private final List<InputElement> inputs = new ArrayList<>();
    private final List<OutputElement> outputs = new ArrayList<>();

    public SubCircuit(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void addElement(CircuitElement e) {
        elements.put(e.getId(), e);
        if (e instanceof InputElement input) {
            inputs.add(input);
        } else if (e instanceof OutputElement output) {
            outputs.add(output);
        }
    }

    public void addWire(Wire wire) {
        wires.add(wire);
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

    public List<InputElement> getInputs() {
        return inputs;
    }

    public List<OutputElement> getOutputs() {
        return outputs;
    }
}
