package com.example.mini_project.controller;

import com.example.mini_project.domain.Circuit;
import com.example.mini_project.domain.CircuitElement;
import com.example.mini_project.domain.ComponentFactory;
import com.example.mini_project.domain.ComponentType;
import com.example.mini_project.gui.CircuitCanvas;
import com.example.mini_project.gui.ToolbarOptions;

public class EditorController {
    private final Circuit circuit;
    private final ComponentFactory factory = new ComponentFactory();
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
        circuit.addElement(factory.create(type, x, y));
    }

    public void optionSelected(ToolbarOptions option, double x, double y) {
        switch(option){
            case Select:
                handleSelect(x, y);
                break;
//            case Delete:
//                // later;
//                break;
            case Wire:
                // later;
                break;
            default:
                // later;
                break;
        }
    }

    private void handleSelect(double x, double y) {
        String hitId = null;
        for (var entry : circuit.getElements().entrySet()) {
            var e = entry.getValue();
            if (x >= e.getX() && x <= e.getX() + 80   // 80 = rough icon width for now
                    && y >= e.getY() && y <= e.getY() + 40) {
                hitId = entry.getKey();
                break;
            }
        }
        if (hitId != null) {
            selection.selectElement(hitId, false); // additive wiring comes in step 3/4
        } else {
            selection.clear();
        }
    }

    public void undo(){
        // do undo
    }

    public void redo(){
        // do redo
    }
}
