package com.example.mini_project.gui;

import com.example.mini_project.controller.EditorController;
import com.example.mini_project.domain.CircuitElement;
import com.example.mini_project.domain.ComponentType;
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
    private EditorToolbar toolbarRef;

    public CircuitCanvas(EditorController controller, double w, double h) {
        this.controller = controller;
        this.background = new Canvas(w, h);
        getChildren().addAll(background, nodeLayer);
        setOnMouseClicked(e -> {
            if (paletteRef == null || toolbarRef == null)
                return;

            ComponentType comp = paletteRef.getSelected();
            ToolbarOptions tool = toolbarRef.getSelected();

            if (comp != null && comp != ComponentType.UNKNOWN) {
                controller.onElementPlaced(comp, e.getX(), e.getY());
                render();
            } else if (tool != null && tool != ToolbarOptions.Unknown) {
                System.out.println("Toolbar action: " + tool); // temporary, to confirm this branch runs
                controller.optionSelected(tool, e.getX(), e.getY());
            }
        });
        render();
    }

    public void setPalette(CircuitElementPalette palette) {
        this.paletteRef = palette;
        palette.setOnUserSelect(() -> {
            if (toolbarRef != null)
                toolbarRef.selectToolbarOption(ToolbarOptions.Unknown);
        });
    }

    public void setToolbar(EditorToolbar toolbar) {
        this.toolbarRef = toolbar;
        toolbar.setOnUserSelect(() -> {
            if (paletteRef != null)
                paletteRef.selectComponentType(ComponentType.UNKNOWN);
        });
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
