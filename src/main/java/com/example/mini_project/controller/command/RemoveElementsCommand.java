package com.example.mini_project.controller.command;

import com.example.mini_project.domain.Circuit;
import com.example.mini_project.domain.CircuitElement;
import java.util.List;

public final class RemoveElementsCommand implements Command {
    private final Circuit circuit;
    private final List<CircuitElement> elements;

    public RemoveElementsCommand(Circuit circuit, List<CircuitElement> elements) {
        this.circuit = circuit;
        this.elements = List.copyOf(elements);
    }

    @Override
    public void undo() {
        for (CircuitElement element : elements) circuit.addElement(element);
    }

    @Override
    public void redo() {
        for (CircuitElement element : elements) circuit.getElements().remove(element.getId());
    }

    @Override
    public String label() {
        return "Delete " + elements.size() + " element(s)";
    }
}
