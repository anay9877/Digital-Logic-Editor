package com.example.mini_project.controller.command;

import com.example.mini_project.domain.Circuit;
import com.example.mini_project.domain.CircuitElement;

public final class PlaceElementCommand implements Command {
    private final Circuit circuit;
    private final CircuitElement element;

    public PlaceElementCommand(Circuit circuit, CircuitElement element) {
        this.circuit = circuit;
        this.element = element;
    }

    @Override
    public void undo() {
        circuit.getElements().remove(element.getId());
    }

    @Override
    public void redo() {
        circuit.addElement(element);
    }

    @Override
    public String label() {
        return "Place " + element.getId();
    }
}
