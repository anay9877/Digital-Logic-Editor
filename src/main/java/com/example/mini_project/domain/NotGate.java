package com.example.mini_project.domain;

public class NotGate extends Gate {
    public NotGate(String id, double x, double y) { super(id, x, y, 1); }

    @Override
    public SignalState evaluate() {
        return isOn(inputPorts.get(0)) ? SignalState.OFF : SignalState.ON;
    }

    @Override
    protected ComponentType gateType() { return ComponentType.NOT; }
}
