package com.example.mini_project.gui;

import com.example.mini_project.controller.EditorController;
import com.example.mini_project.domain.CircuitElement;
import com.example.mini_project.domain.ComponentType;
import com.example.mini_project.domain.SvgIconRegistry;
// import com.example.mini_project.gui.ViewportManager;
import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.input.MouseButton;

/**Mouse's right key --> to select 
 * Mouse's left key --> drag/translate
*/
public class CircuitCanvas extends Pane {
    private final EditorController controller;
    private final Canvas background;
    private final Group nodeLayer = new Group();
    private final ViewportManager viewportManager;
    private final SvgIconRegistry iconRegistry = new SvgIconRegistry();
    private CircuitElementPalette paletteRef;
    private EditorToolbar toolbarRef;
    private double lastMousePosition_X;
    private double lastMousePosition_Y;

    public CircuitCanvas(EditorController controller) {
        this.controller = controller;
        this.background = new Canvas();
        // To make the canvas size adaptive
        background.widthProperty().bind(widthProperty());
        background.heightProperty().bind(heightProperty());
        widthProperty().addListener((obs, oldVal, newVal) -> {
            makeBackground();
        });
        heightProperty().addListener((obs, oldVal, newVal) -> {
            makeBackground();
        });

        this.viewportManager = new ViewportManager(nodeLayer);
        getChildren().addAll(background, nodeLayer);
        setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                lastMousePosition_X = e.getX();
                lastMousePosition_Y = e.getY();
            }
        });
        setOnMouseDragged(e -> {
            if (e.isSecondaryButtonDown()) {
                double dx = e.getX() - lastMousePosition_X;
                double dy = e.getY() - lastMousePosition_Y;
                viewportManager.translate(dx, dy);

                lastMousePosition_X = e.getX();
                lastMousePosition_Y = e.getY();
            }
        });
        setOnMouseClicked(e -> {
            if (e.getButton() != MouseButton.PRIMARY) return;
            if(paletteRef == null || toolbarRef == null) return;

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
            if (toolbarRef != null) toolbarRef.selectToolbarOption(ToolbarOptions.Unknown);
        });
    }

    public void setToolbar(EditorToolbar toolbar) {
        this.toolbarRef = toolbar;
        toolbar.setOnUserSelect(() -> {
            if (paletteRef != null) {
                paletteRef.selectComponentType(ComponentType.UNKNOWN);
            }
        });

        toolbar.setOnZoomIn(() -> {
            viewportManager.zoomIn();
        });
        toolbar.setOnZoomOut(() -> {
            viewportManager.zoomOut();
        });
    }

    public void makeBackground() {
        GraphicsContext gc = background.getGraphicsContext2D();
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, background.getWidth(), background.getHeight());    
    }

    public void render() {
        makeBackground();
        nodeLayer.getChildren().clear();
        for (CircuitElement e : controller.getCircuit().getElements().values()) {
            var icon = e.getIconNode(iconRegistry);
            icon.setLayoutX(e.getX());
            icon.setLayoutY(e.getY());
            nodeLayer.getChildren().add(icon);
        }
    }
}
