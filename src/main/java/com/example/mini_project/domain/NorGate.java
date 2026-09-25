package com.example.mini_project.domain;

public class NorGate extends Gate {
    public NorGate(String id, double x, double y) { super(id, x, y, 2); }

    @Override
    public SignalState evaluate() {
        return (isOn(inputPorts.get(0)) || isOn(inputPorts.get(1))) ? SignalState.OFF : SignalState.ON;
    }

    @Override
    protected ComponentType gateType() { return ComponentType.NOR; }
}
