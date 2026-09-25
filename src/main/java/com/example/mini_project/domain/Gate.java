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
    }

    public abstract SignalState evaluate();

    @Override
    public Port findPort(String portId) {
        // finds port by portID
        return null;
    }

    public List<Port> getInputPorts() {
        return inputPorts;
    }
    public Port getOutputPort() {
        return outputPort;
    }

    protected boolean isOn(Port p) {
        return p.getValue() == SignalState.ON;
    }

    @Override
    public Node getIconNode(SvgIconRegistry registry) {
        return registry.createIcon(gateType());
    }

    protected abstract ComponentType gateType();
}
