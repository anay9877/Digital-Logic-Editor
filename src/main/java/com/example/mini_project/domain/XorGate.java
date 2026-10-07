package com.example.mini_project.domain;

public class XorGate extends Gate {
    public XorGate(String id, double x, double y) { super(id, x, y, 2); }

    @Override
    public SignalState evaluate() {
        return (isOn(inputPorts.get(0)) ^ isOn(inputPorts.get(1))) ? SignalState.ON : SignalState.OFF;
    }

    @Override
    protected ComponentType gateType() { return ComponentType.XOR; }
}
