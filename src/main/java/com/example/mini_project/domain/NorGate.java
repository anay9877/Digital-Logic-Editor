package com.example.mini_project.domain;

public class NorGate extends Gate {
    public NorGate(String id, double x, double y) { super(id, x, y, 2); }

    @Override
    public SignalState evaluate() {
        SignalState left = inputPorts.get(0).getValue();
        SignalState right = inputPorts.get(1).getValue();
        if (left == SignalState.ON || right == SignalState.ON) return SignalState.OFF;
        if (left == SignalState.OFF && right == SignalState.OFF) return SignalState.ON;
        return SignalState.UNKNOWN;
    }

    @Override
    protected ComponentType gateType() { return ComponentType.NOR; }
}
