package com.example.mini_project.controller;

import com.example.mini_project.domain.Circuit;
import com.example.mini_project.domain.CircuitElement;
import com.example.mini_project.domain.ComponentType;
import com.example.mini_project.gui.CircuitCanvas;
import com.example.mini_project.gui.ToolbarOptions;
import com.example.mini_project.domain.SvgIconRegistry;

import java.util.HashMap;
import java.util.Map;

public class EditorController {
    private final Circuit circuit;
    private final Map<ComponentType, Integer> counters = new HashMap<>();
    private final SelectionManager selection = new SelectionManager();

    public EditorController(Circuit circuit) {
        this.circuit = circuit;
    }

    public Circuit getCircuit() {
        return circuit;
    }

    public SelectionManager getSelection(){
        return selection;
    }

    // These below functions involve onElementPlaced and optionSelected which are called by CircuitCanvas
    // onElementPlaced takes the component type and tells the factory to create a element out of it and
    // place it at the given co-ordinates
    // optionSelected the toolbaroption which is currently selected and the co-ordinates
    // why co-ordinates
    public void onElementPlaced(ComponentType type, double x, double y) {
        int n = counters.getOrDefault(type, 0) + 1;
        counters.put(type, n);
        String id = type.name() + "-" + n;
        circuit.addElement(type.create(id, x, y));
    }

    public void optionSelected(ToolbarOptions option, double x, double y) {
        switch (option){
            case Select -> handleSelect(x, y);
            case Delete -> handleDelete();
        }
    }

    private void handleSelect(double x, double y) {
//        String hitId = null;
//        for (CircuitElement e : circuit.getElements().values()) {
//            if ((x >= e.getX() && x <= e.getX() + e.getLength()) && (y >= e.getY() && y <= e.getY() + e.getLength())) {
//                hitId = e.getId();
//                break;
//            }
//        }
//        if (hitId != null) {
//            selection.selectElement(hitId, false);
//            System.out.println("Selected: " + hitId);
//        } else {
//            selection.clear();
//            System.out.println("Selection Cleared");
//        }
        selection.clear();   // only reached when the click did NOT land on an icon
        System.out.println("Selection cleared");
    }

    private void handleDelete(){
        ;
    }

    public void undo(){
        // do undo
    }

    public void redo(){
        // do redo
    }
}
