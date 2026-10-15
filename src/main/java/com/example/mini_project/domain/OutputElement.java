package com.example.mini_project.domain;

import javafx.scene.Node;

import java.util.List;

public class OutputElement extends CircuitElement {
    private SignalState value = SignalState.UNKNOWN;
    private final Port inputPort;

    public OutputElement(String id, double x, double y) {
        super(id, x, y);
        inputPort = new Port(id + "-in0", PortType.INPUT, this);
        applyPortOffsets();
    }

    private void applyPortOffsets() {
        SvgIconRegistry registry = new SvgIconRegistry();
        List<PortSpec> specs = registry.getPortSpecs(ComponentType.LED);
        for (PortSpec spec : specs) {
            if (inputPort.getId().endsWith("-" + spec.localId())) {
                inputPort.setRelOffset(spec.relX(), spec.relY());
            }
        }
    }

    public SignalState getValue() {
        return inputPort.getValue();
    }

    public Port getInputPort() {
        return inputPort;
    }

    @Override
    public Port findPort(String portId) {
        return inputPort.getId().equals(portId) ? inputPort : null;
    }

    @Override
    public Node getIconNode(SvgIconRegistry registry) {
        return registry.createLedIcon(getValue() == SignalState.ON);
    }

    @Override
    public ComponentType getType() {
        return ComponentType.LED;
    }
}
