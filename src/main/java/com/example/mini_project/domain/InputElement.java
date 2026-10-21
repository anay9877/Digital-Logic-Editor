package com.example.mini_project.domain;

import javafx.scene.Node;

import java.util.List;

public class InputElement extends CircuitElement {
    private SignalState value = SignalState.OFF;
    private final Port outputPort;

    public InputElement(String id, double x, double y) {
        super(id, x, y);
        outputPort = new Port(id + "-out", PortType.OUTPUT, this);
        outputPort.setValue(value);
        applyPortOffsets();
    }

    private void applyPortOffsets() {
        SvgIconRegistry registry = new SvgIconRegistry();
        List<PortSpec> specs = registry.getPortSpecs(ComponentType.SWITCH);
        for (PortSpec spec : specs) {
            if (outputPort.getId().endsWith("-" + spec.localId())) {
                outputPort.setRelOffset(spec.relX(), spec.relY());
            }
        }
    }

    public void toggle() {
        setValue((value == SignalState.ON) ? SignalState.OFF : SignalState.ON);
    }

    public SignalState getValue() {
        return value;
    }

    public void setValue(SignalState value) {
        this.value = value;
        outputPort.setValue(value);
    }

    public Port getOutputPort() {
        return outputPort;
    }

    @Override
    public Port findPort(String portId) {
        return outputPort.getId().equals(portId) ? outputPort : null;
    }

    @Override
    public Node getIconNode(SvgIconRegistry registry) {
        return registry.createSwitchIcon(value == SignalState.ON);
    }

    @Override
    public ComponentType getType() {
        return ComponentType.SWITCH;
    }
}
