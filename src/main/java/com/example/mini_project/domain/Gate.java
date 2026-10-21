package com.example.mini_project.domain;

import javafx.scene.Node;

import java.util.ArrayList;
import java.util.List;

public abstract class Gate extends CircuitElement {
    protected final List<Port> inputPorts = new ArrayList<>();
    protected final Port outputPort;

    protected Gate(String id, double x, double y, int inputCount) {
        super(id, x, y);
        for (int i = 0; i < inputCount; i++) {
            inputPorts.add(new Port(id + "-in" + i, PortType.INPUT, this));
        }
        outputPort = new Port(id + "-out", PortType.OUTPUT, this);

        // Build ports without offsets first
        // Offsets are assigned below, once the registry can tell us where each
        // port sits visually.
        // Assign pixel offsets from the SVG icon registry so that every port
        // knows its exact on-screen position relative to the component's top-left corner.
        applyPortOffsets(new SvgIconRegistry());
    }

    /**
     * Reads PortSpec entries for this gate type from the registry and
     * copies the relX/relY offsets onto the matching Port objects.
     * <p>
     * Matching is done by comparing the PortSpec localId to the port ID
     * suffix (the part after the last "-").
     */
    private void applyPortOffsets(SvgIconRegistry registry) {
        List<PortSpec> specs = registry.getPortSpecs(gateType());
        for (PortSpec spec : specs) {
            Port match = findPortByLocalId(spec.localId());
            if (match != null) {
                match.setRelOffset(spec.relX(), spec.relY());
            }
        }
    }

    /**
     * Finds a Port by the local suffix of its ID.
     * E.g. localId "in0" matches the Port whose full id ends with "-in0".
     */
    private Port findPortByLocalId(String localId) {
        for (Port p : inputPorts) {
            if (p.getId().endsWith("-" + localId)) return p;
        }
        if (outputPort.getId().endsWith("-" + localId)) return outputPort;
        return null;
    }

    public abstract SignalState evaluate();

    @Override
    public Port findPort(String portId) {
        for (Port p : inputPorts) {
            if (p.getId().equals(portId)) return p;
        }
        if (outputPort.getId().equals(portId)) return outputPort;
        return null;
    }

    public List<Port> getInputPorts() {
        return inputPorts;
    }

    public Port getOutputPort() {
        return outputPort;
    }

    /**
     * Returns ALL ports (inputs + output) as a flat list for convenience.
     */
    public List<Port> getAllPorts() {
        List<Port> all = new ArrayList<>(inputPorts);
        all.add(outputPort);
        return all;
    }

    @Override
    public Node getIconNode(SvgIconRegistry registry) {
        return registry.createIcon(gateType());
    }

    @Override
    public ComponentType getType() {
        return gateType();
    }

    protected abstract ComponentType gateType();
}
