package com.example.mini_project.domain;

import javafx.scene.Node;

public class InputElement extends CircuitElement {
    private SignalState value = SignalState.OFF;
    private final Port outputPort;

    public InputElement(String id, double x, double y) {
        super(id, x, y);
        outputPort = new Port(id + "-out", PortType.OUTPUT, this);
    }

    @Override
    public double getLength(){
        return SvgIconRegistry.SWITCH_WIDTH;
    }

    public void toggle() {
        // for toggoling on and off
    }

    public SignalState getValue() {
        return value;
    }
    public Port getOutputPort() {
        return outputPort;
    }

    @Override
    public Port findPort(String portId) {
        // logic not implemented
        return outputPort;
    }

    @Override
    public Node getIconNode(SvgIconRegistry registry) {
        return registry.createSwitchIcon(value == SignalState.ON);
    }
}
