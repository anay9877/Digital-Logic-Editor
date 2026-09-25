package com.example.mini_project.domain;

import javafx.scene.Node;

public class OutputElement extends CircuitElement {
    private SignalState value = SignalState.UNKNOWN;
    private final Port inputPort;

    public OutputElement(String id, double x, double y) {
        super(id, x, y);
        inputPort = new Port(id + "-in", PortType.INPUT, this);
    }

    public SignalState getValue() {
        return inputPort.getValue();
    }
    public Port getInputPort() {
        return inputPort;
    }

    @Override
    public Port findPort(String portId) {
        // logic not implemented
        return inputPort;
    }

    @Override
    public Node getIconNode(SvgIconRegistry registry) {
        return registry.createLedIcon(getValue() == SignalState.ON);
    }
}
