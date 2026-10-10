package com.example.mini_project.gui;

import com.example.mini_project.controller.EditorController;
import com.example.mini_project.domain.CircuitElement;
import com.example.mini_project.domain.ComponentType;
import com.example.mini_project.domain.SvgIconRegistry;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;

public class CircuitCanvas extends Pane {
    private final EditorController controller;
    private final Canvas background;
    private final Group nodeLayer = new Group();
    private final SvgIconRegistry iconRegistry = new SvgIconRegistry();
    private final Rectangle rubberBand = new Rectangle();
    private CircuitElementPalette paletteRef;
    private EditorToolbar toolbarRef;

    private double startX, startY;
    private boolean pressedOnEmpty = false;
    private boolean dragging = false;

    public CircuitCanvas(EditorController controller, double w, double h) {
        this.controller = controller;
        this.background = new Canvas(w, h);

        rubberBand.setFill(Color.rgb(0, 200, 255, 0.15));
        rubberBand.setStroke(Color.CYAN);
        rubberBand.setVisible(false);
        rubberBand.setMouseTransparent(true);

        getChildren().addAll(background, nodeLayer, rubberBand);
        controller.getSelection().setOnChange(this::render);

        // rubberBand : press -> drag -> release => we get the selected elements
        setOnMousePressed(e -> {
            // only start when the select tool is active AND press is on empty canvas
            pressedOnEmpty = isSelectToolActive() && e.getTarget() == background;
            dragging = false;
            startX = e.getX();
            startY = e.getY();
        });

        setOnMouseDragged(e -> {
            if (!pressedOnEmpty) return;
            dragging = true;
            rubberBand.setX(Math.min(startX, e.getX()));
            rubberBand.setY(Math.min(startY, e.getY()));
            rubberBand.setWidth(Math.abs(e.getX() - startX));
            rubberBand.setHeight(Math.abs(e.getY() - startY));
            rubberBand.setVisible(true);
        });

        setOnMouseReleased(e -> {
            if (!pressedOnEmpty) return;
            pressedOnEmpty = false;
            rubberBand.setVisible(false);
            if (!dragging) return;

            Bounds box = rubberBand.getBoundsInParent();
            List<String> hits = new ArrayList<>();
            for (Node n : nodeLayer.getChildren()) {
                if (n.getBoundsInParent().intersects(box)) {
                    hits.add((String) n.getUserData());   // id stored in render()
                }
            }
            controller.getSelection().selectElements(hits, e.isShiftDown() || e.isControlDown());
        });

        // plain click
        setOnMouseClicked(e -> {
            if (!e.isStillSincePress()) return;   // a drag also produces a click; ignore it
            if (paletteRef == null || toolbarRef == null) return;

            ComponentType comp = paletteRef.getSelected();
            ToolbarOptions tool = toolbarRef.getSelected();

            if (comp != null && comp != ComponentType.UNKNOWN) {
                controller.onElementPlaced(comp, e.getX(), e.getY());
                render();
            } else if (tool != null && tool != ToolbarOptions.Unknown) {
                controller.optionSelected(tool, e.getX(), e.getY());
            }
        });

        render();
    }

    private boolean isSelectToolActive(){
        return toolbarRef != null && toolbarRef.getSelected() == ToolbarOptions.Select;
    }

    public void setPalette(CircuitElementPalette palette) {
        this.paletteRef = palette;
        palette.setOnUserSelect(() -> {
            if (toolbarRef != null) toolbarRef.selectToolbarOption(ToolbarOptions.Unknown);
        });
    }

    public void setToolbar(EditorToolbar toolbar) {
        this.toolbarRef = toolbar;
        toolbar.setOnUserSelect(() -> {
            if (paletteRef != null) paletteRef.selectComponentType(ComponentType.UNKNOWN);
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
            icon.setUserData(e.getId());   // lets the rubber band map a node back to its element

            if (controller.getSelection().isElementSelected(e.getId())) {
                icon.setEffect(new DropShadow(15, Color.CYAN));
            }

            icon.setOnMouseClicked(ev -> {
                if (isSelectToolActive()) {
                    ev.consume();
                    boolean additive = ev.isShiftDown() || ev.isControlDown();
                    controller.getSelection().selectElement(e.getId(), additive);
                }
            });

            nodeLayer.getChildren().add(icon);
        }
    }
}