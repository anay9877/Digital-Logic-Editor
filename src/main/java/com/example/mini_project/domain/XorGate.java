package com.example.mini_project.domain;

public class XorGate extends Gate {
    public XorGate(String id, double x, double y) { super(id, x, y, 2); }

    @Override
    public SignalState evaluate() {
        SignalState left = inputPorts.get(0).getValue();
        SignalState right = inputPorts.get(1).getValue();
        if (left == SignalState.UNKNOWN || right == SignalState.UNKNOWN) return SignalState.UNKNOWN;
        return left != right ? SignalState.ON : SignalState.OFF;
    }

    @Override
    protected ComponentType gateType() { return ComponentType.XOR; }
}
