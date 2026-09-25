package com.example.mini_project.gui;

import com.example.mini_project.domain.ComponentType;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class CircuitElementPalette extends VBox {
    private final List<ComponentType> componentTypes = new ArrayList<>();
    private ComponentType selected;

    public CircuitElementPalette() {
        for (ComponentType t : ComponentType.values()) {
            componentTypes.add(t);
            Button b = new Button(t.name());
            b.setOnAction(e -> selectComponentType(t));
            getChildren().add(b);
        }
    }

    public void selectComponentType(ComponentType type) {
        this.selected = type;
    }

    public ComponentType getSelected() {
        return selected;
    }
}
