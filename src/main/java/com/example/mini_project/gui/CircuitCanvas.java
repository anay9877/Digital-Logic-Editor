package com.example.mini_project.gui;

import com.example.mini_project.controller.EditorController;
import com.example.mini_project.domain.CircuitElement;
import com.example.mini_project.domain.SvgIconRegistry;
import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

public class CircuitCanvas extends Pane {
    private final EditorController controller;
    private final Canvas background;
    private final Group nodeLayer = new Group();
    private final SvgIconRegistry iconRegistry = new SvgIconRegistry();
    private CircuitElementPalette paletteRef;

    public CircuitCanvas(EditorController controller, double w, double h) {
        this.controller = controller;
        this.background = new Canvas(w, h);
        getChildren().addAll(background, nodeLayer);
        setOnMouseClicked(e -> {
            if (paletteRef != null && paletteRef.getSelected() != null) {
                controller.onElementPlaced(paletteRef.getSelected(), e.getX(), e.getY());
                render();
            }
        });
        render();
    }

    public void setPalette(CircuitElementPalette palette) {
        this.paletteRef = palette;
    }

    public void render() {
        GraphicsContext gc = background.getGraphicsContext2D();
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, background.getWidth(), background.getHeight());

        nodeLayer.getChildren().clear();
        for (CircuitElement e : controller.getCircuit().getElements().values()) {
            var icon = e.getIconNode(iconRegistry);
            icon.setLayoutX(e.getX());
            icon.setLayoutY(e.getY());
            nodeLayer.getChildren().add(icon);
        }
    }
}
