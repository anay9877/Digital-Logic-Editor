package com.example.mini_project.domain;

public class Port {

    private final String id;
    private final PortType type;
    private final CircuitElement owner;
    private SignalState value = SignalState.UNKNOWN;

    /**
     * Pixel offset from the owner component's top-left corner (canvas space).
     * Set once during construction using PortSpec data from SvgIconRegistry.
     */
    private double relX;
    private double relY;

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

    // Signal state
    public SignalState getValue() {
        return value;
    }

    public void setValue(SignalState value) {
        this.value = value;
    }

    public void setRelOffset(double relX, double relY) {
        this.relX = relX;
        this.relY = relY;
    }

    // Absolute canvas X and Y of this port
    // PortX = ComponentX + relX
    public double getAbsX() {
        return owner.getX() + relX;
    }

    // PortY = ComponentY + relY
    public double getAbsY() {
        return owner.getY() + relY;
    }
}
