package com.example.mini_project.domain;

public class NotGate extends Gate {
    public NotGate(String id, double x, double y) { super(id, x, y, 1); }

    @Override
    public SignalState evaluate() {
        return switch (inputPorts.get(0).getValue()) {
            case ON -> SignalState.OFF;
            case OFF -> SignalState.ON;
            case UNKNOWN -> SignalState.UNKNOWN;
        };
    }

    @Override
    protected ComponentType gateType() { return ComponentType.NOT; }
}
