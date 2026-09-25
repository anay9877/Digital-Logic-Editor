package com.example.mini_project.domain;

public class Wire {

    private final String id;
    private final String sourcePortId;
    private final String destPortId;
    private SignalState value = SignalState.UNKNOWN;

    public Wire(String id, String sourcePortId, String destPortId) {
        this.id = id;
        this.sourcePortId = sourcePortId;
        this.destPortId = destPortId;
    }

    public String getId() {
        return id;
    }

    public String getSourcePortId() {
        return sourcePortId;
    }

    public String getDestPortId() {
        return destPortId;
    }

    public SignalState getValue() {
        return value;
    }

    public void setValue(SignalState value) {
        this.value = value;
    }
}
