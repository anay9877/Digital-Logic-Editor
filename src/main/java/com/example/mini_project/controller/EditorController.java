package com.example.mini_project.controller;

import com.example.mini_project.domain.Circuit;
import com.example.mini_project.domain.ComponentFactory;
import com.example.mini_project.domain.ComponentType;
import com.example.mini_project.gui.CircuitCanvas;
import com.example.mini_project.gui.ToolbarOptions;

public class EditorController {
    private final Circuit circuit;
    private final ComponentFactory factory = new ComponentFactory();

    public EditorController(Circuit circuit) {
        this.circuit = circuit;
    }

    public Circuit getCircuit() {
        return circuit;
    }

    public void onElementPlaced(ComponentType type, double x, double y) {
        circuit.addElement(factory.create(type, x, y));
    }

    public void optionSelected(ToolbarOptions option, double x, double y) {
        // nothing
    }

    public void undo(){
        // do undo
    }

    public void redo(){
        // do redo
    }
}
