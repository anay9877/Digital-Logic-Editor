package com.example.mini_project.domain;

public class Port {

    private final String id;
    private final PortType type;
    private final CircuitElement owner;
    private SignalState value = SignalState.UNKNOWN;
    private Wire connectedWire; // null if not connected

    public Port(String id, PortType type, CircuitElement owner) {
        this.id = id;
        this.type = type;
        this.owner = owner;
    }

    public String getId() {
        return id;
    }

    public PortType getType() {
        return type;
    }

    public CircuitElement getOwner() {
        return owner;
    }

    public SignalState getValue() {
        return value;
    }

    public void setValue(SignalState value) {
        this.value = value;
    }

    public Wire getConnectedWire() {
        return connectedWire;
    }

    public void setConnectedWire(Wire connectedWire) {
        this.connectedWire = connectedWire;
    }

    public boolean isConnected() {
        return connectedWire != null;
    }

    public void propagate() {
        // Pushes this port's current value onto its connected wire, if any
    }
}
