package com.example.mini_project.domain;

public class Port {

    private final String id;
    private final PortType type;
    private final CircuitElement owner;
    private SignalState value = SignalState.UNKNOWN;
    private Wire connectedWire; // null if not connected

    /**
     * Pixel offset from the owner component's top-left corner (canvas space).
     * Set once during construction using PortSpec data from SvgIconRegistry.
     */
    private double relX;
    private double relY;

    public Port(String id, PortType type, CircuitElement owner) {
        this.id    = id;
        this.type  = type;
        this.owner = owner;
    }

    public Port(String id, PortType type, CircuitElement owner, double relX, double relY) {
        this(id, type, owner);
        this.relX = relX;
        this.relY = relY;
    }

    // ── Identity ──────────────────────────────────────────────────────────────

    public String getId() { return id; }

    public PortType getType() { return type; }

    public CircuitElement getOwner() { return owner; }

    // ── Signal state ──────────────────────────────────────────────────────────

    public SignalState getValue() { return value; }

    public void setValue(SignalState value) { this.value = value; }

    // ── Wire connection ───────────────────────────────────────────────────────

    public Wire getConnectedWire() { return connectedWire; }

    public void setConnectedWire(Wire connectedWire) { this.connectedWire = connectedWire; }

    public boolean isConnected() { return connectedWire != null; }

    public void propagate() {
        // Pushes this port's current value onto its connected wire, if any
    }

    // ── Visual position ───────────────────────────────────────────────────────

    /** Pixel offset from the owner's top-left corner. */
    public double getRelX() { return relX; }
    public double getRelY() { return relY; }

    public void setRelOffset(double relX, double relY) {
        this.relX = relX;
        this.relY = relY;
    }

    /**
     * Absolute canvas X of this port (owner position + relative offset).
     * Use this when drawing port dots or starting/ending a wire.
     */
    public double getAbsX() { return owner.getX() + relX; }

    /**
     * Absolute canvas Y of this port (owner position + relative offset).
     */
    public double getAbsY() { return owner.getY() + relY; }
}
