package com.example.mini_project.gui;

import com.example.mini_project.controller.EditorController;
import com.example.mini_project.domain.*;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.control.Label;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Mouse's left button  --> select / rubber band / drag elements / draw wires
 * Mouse's right button --> pan the view
 * <p>
 * The main drawing surface for the circuit editor.
 */
public class CircuitCanvas extends Pane {

    // Grid Constants & Snapping
    public static final double GRID_SIZE = 20.0;

    public static double snap(double v) {
        return Math.round(v / GRID_SIZE) * GRID_SIZE;
    }

    // Constants
    private static final double PORT_RADIUS = 5.0;
    private static final double JUNCTION_RADIUS = 6.0;
    private static final double WIRE_WIDTH = 2.0;

    private static final Color COLOR_PORT_IDLE = Color.web("#4caf50");
    private static final Color COLOR_PORT_HOVER = Color.web("#00e5ff");
    private static final Color COLOR_JUNCTION = Color.web("#000");
    private static final Color COLOR_PREVIEW = Color.web("#000");

    // Fields
    private final EditorController controller;

    // Viewport grouping: contentGroup holds the circuit nodes and is zoomed / panned as one unit.
    // The grid is NOT inside it: it lives on a screen-sized Canvas behind it and is redrawn
    // whenever the view (size / pan / zoom) changes.

    private final Group contentGroup = new Group();
    private final Canvas gridCanvas = new Canvas();
    private final Group nodeLayer = new Group();
    private final Label hoverLabel = new Label();

    private final ViewportManager viewportManager;
    private final SvgIconRegistry iconRegistry = new SvgIconRegistry();
    private final Rectangle rubberBand = new Rectangle();
    private CircuitElementPalette paletteRef;
    private EditorToolbar toolbarRef;

    // Pan
    private double lastMousePosition_X;
    private double lastMousePosition_Y;

    // The live preview line shown while the user drags a wire.
    private Polyline previewLine = null;

    // Selection (rubber band)
    private double startX, startY;
    private boolean pressedOnEmpty = false;
    private boolean dragging = false;

    // Move (dragging selected elements)
    private Set<String> dragIds = Set.of();
    private double dragStartX, dragStartY;
    private boolean moving = false;

    public CircuitCanvas(EditorController controller) {
        this.controller = controller;
        setStyle("-fx-background-color: white;");

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        setClip(clip);

        gridCanvas.setMouseTransparent(true);
        // The grid canvas always matches the visible size of this pane (never larger).
        gridCanvas.widthProperty().bind(widthProperty());
        gridCanvas.heightProperty().bind(heightProperty());

        contentGroup.getChildren().add(nodeLayer);
        hoverLabel.setStyle("-fx-background-color: #e9e9e5;" + "-fx-font-size: 12px;" +
                            "-fx-padding: 5 10 5 10;" + "fx-text-fill: white;");
        hoverLabel.setVisible(false);
        hoverLabel.setMouseTransparent(true);
        contentGroup.getChildren().add(hoverLabel);

        rubberBand.setFill(Color.rgb(0, 200, 255, 0.15));
        rubberBand.setStroke(Color.CYAN);
        rubberBand.setVisible(false);
        rubberBand.setMouseTransparent(true);

        controller.getSelection().setOnChange(this::render);

        this.viewportManager = new ViewportManager(contentGroup);
        viewportManager.setOnChange(this::drawGrid);
        getChildren().addAll(gridCanvas, contentGroup, rubberBand);   // grid at the back, rubberBand on top

        // Redraw the grid when the pane is resized; zoom buttons zoom around the visible centre.
        widthProperty().addListener((o, a, b) -> onViewResized());
        heightProperty().addListener((o, a, b) -> onViewResized());

        // ONE handler per event type. Each one branches on button / active tool.

        setOnMousePressed(e -> {
            pressedOnEmpty = false;

            if (e.getButton() == MouseButton.SECONDARY) {          // pan
                lastMousePosition_X = e.getX();
                lastMousePosition_Y = e.getY();
                return;
            }
            if (e.getButton() != MouseButton.PRIMARY) return;

            if (isWireToolActive()) {                              // wire tool
                Point2D p = contentGroup.sceneToLocal(e.getSceneX(), e.getSceneY());
                controller.onWireMousePressed(p.getX(), p.getY());
                if (controller.isDrawingWire()) {
                    previewLine = new Polyline();
                    previewLine.setStroke(COLOR_PREVIEW);
                    previewLine.setStrokeWidth(WIRE_WIDTH);
                    previewLine.setFill(Color.TRANSPARENT);
                    previewLine.getStrokeDashArray().addAll(6.0, 4.0);
                    previewLine.setMouseTransparent(true);
                    nodeLayer.getChildren().add(previewLine);
                }
                return;
            }

            // rubber band: Select tool + press on empty canvas
            pressedOnEmpty = isSelectToolActive() && (e.getTarget() == this || e.getTarget() == contentGroup || e.getTarget() == gridCanvas);
            dragging = false;
            startX = e.getX();
            startY = e.getY();
        });
        setOnScroll(e -> {
            if (!e.isControlDown()) return;
            if (e.getDeltaY() > 0)
                viewportManager.zoomIn();
            else if (e.getDeltaY() < 0)
                viewportManager.zoomOut();
            e.consume();
        });
        setOnMouseDragged(e -> {
            if (e.isSecondaryButtonDown()) {                       // pan
                double dx = e.getX() - lastMousePosition_X;
                double dy = e.getY() - lastMousePosition_Y;
                viewportManager.translate(dx, dy);
                lastMousePosition_X = e.getX();
                lastMousePosition_Y = e.getY();
                return;
            }

            if (controller.isDrawingWire()) {                      // wire preview
                Point2D p = contentGroup.sceneToLocal(e.getSceneX(), e.getSceneY());
                double[] pts = controller.onWireMouseDragged(p.getX(), p.getY());
                if (pts != null && previewLine != null) {
                    previewLine.getPoints().clear();
                    for (double pt : pts) {
                        previewLine.getPoints().add(pt);
                    }
                }
                return;
            }

            if (!pressedOnEmpty) return;                           // rubber band
            dragging = true;
            rubberBand.setX(Math.min(startX, e.getX()));
            rubberBand.setY(Math.min(startY, e.getY()));
            rubberBand.setWidth(Math.abs(e.getX() - startX));
            rubberBand.setHeight(Math.abs(e.getY() - startY));
            rubberBand.setVisible(true);
        });

        setOnMouseReleased(e -> {
            if (e.getButton() == MouseButton.PRIMARY && isWireToolActive()) {   // finish wire
                if (previewLine != null) {
                    nodeLayer.getChildren().remove(previewLine);
                    previewLine = null;
                }
                Point2D p = contentGroup.sceneToLocal(e.getSceneX(), e.getSceneY());
                controller.onWireMouseReleased(p.getX(), p.getY());
                return;
            }

                if (!pressedOnEmpty) return;                           // finish rubber band
            pressedOnEmpty = false;
            rubberBand.setVisible(false);
            if (!dragging) return;

            Bounds box = rubberBand.localToScene(rubberBand.getBoundsInLocal());
            List<String> elementHits = new ArrayList<>();
            List<String> wireHits = new ArrayList<>();
            for (Node n : nodeLayer.getChildren()) {
                // Ignore nodes outside the rubber band
                if (!n.localToScene(n.getBoundsInLocal()).intersects(box)) {
                    continue;
                }
                // Check whether the node represents a wire or an element
                if (n instanceof Polyline && n.getUserData() instanceof String id) {
                    wireHits.add(id);
                } else if (n.getUserData() instanceof String id) {
                    elementHits.add(id);
                }
            }
            boolean additive = e.isShiftDown() || e.isControlDown();
            controller.getSelection().selectElementsAndWires(elementHits, wireHits, additive);
        });

        // plain click on Canvas
        // If Palette is selected then place the element
        // If controller is selected then
        setOnMouseClicked(e -> {
            if (!e.isStillSincePress() || e.getButton() != MouseButton.PRIMARY) return;
            if (paletteRef == null || toolbarRef == null) return;

            ComponentType comp = paletteRef.getSelected();
            ToolbarOptions tool = toolbarRef.getSelected();

            if (comp != null && comp != ComponentType.UNKNOWN) {
                Point2D p = contentGroup.sceneToLocal(e.getSceneX(), e.getSceneY());
                controller.onElementPlaced(comp, p.getX(), p.getY());
            } else if (tool == ToolbarOptions.Select) {
                Point2D p = contentGroup.sceneToLocal(e.getSceneX(), e.getSceneY());   // wires use world coordinates
                boolean additive = e.isShiftDown() || e.isControlDown();
                controller.onSelectPoint(p.getX(), p.getY(), additive, e.getClickCount() >= 2);
            } else if (tool != null && tool != ToolbarOptions.Unknown && tool != ToolbarOptions.Wire) {
                controller.optionSelected(tool);
            }
            render();
        });

        render();
    }

    // ── Palette / Toolbar wiring ───────────────────────────────────────────────

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
            if (paletteRef != null) {
                paletteRef.selectComponentType(ComponentType.UNKNOWN);
            }
        });

        toolbar.setOnZoomIn(() -> viewportManager.zoomIn());
        toolbar.setOnZoomOut(() -> viewportManager.zoomOut());
        toolbar.setOnHome(() -> viewportManager.resetView());
        toolbar.setOnDelete(controller::deleteSelected); //deletion
        toolbar.setOnValidate(() -> controller.optionSelected(ToolbarOptions.Validate));
        toolbar.setOnSimulate(controller::runSimulation);
        toolbar.setOnUndo(controller::undo); //prajwal undo redo
        toolbar.setOnRedo(controller::redo);
        controller.getHistory().setOnChange(() -> toolbar.setHistoryState(
                controller.getHistory().canUndo(), controller.getHistory().canRedo()));
        toolbar.setHistoryState(controller.getHistory().canUndo(), controller.getHistory().canRedo());
    }

    private void onViewResized() {
        viewportManager.setAnchor(getWidth() / 2, getHeight() / 2);
        drawGrid();
    }

    /**
     * Draws the dot grid in screen space for the part of the world that is currently visible.
     * Only ever as big as the pane, so it can't exceed GPU texture limits.
     */
    private void drawGrid() {
        double w = gridCanvas.getWidth();
        double h = gridCanvas.getHeight();
        GraphicsContext gc = gridCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);
        if (w <= 0 || h <= 0) return;

        double zoom = viewportManager.getZoom();
        double tx = viewportManager.getTranslateX();
        double ty = viewportManager.getTranslateY();

        // When zoomed far out, skip grid lines so dots never get denser than ~6 screen px.
        int step = 1;
        while (GRID_SIZE * step * zoom < 6) step *= 2;
        double stepWorld = GRID_SIZE * step;

        // visible world rectangle
        double minWX = (0 - tx) / zoom, maxWX = (w - tx) / zoom;
        double minWY = (0 - ty) / zoom, maxWY = (h - ty) / zoom;

        gc.setFill(Color.GRAY);
        double startX = Math.floor(minWX / stepWorld) * stepWorld;
        double startY = Math.floor(minWY / stepWorld) * stepWorld;
        for (double wx = startX; wx <= maxWX; wx += stepWorld) {
            double sx = wx * zoom + tx;
            for (double wy = startY; wy <= maxWY; wy += stepWorld) {
                double sy = wy * zoom + ty;
                gc.fillOval(sx - 1, sy - 1, 2, 2);
            }
        }
    }

    // Rendering
    // Full redraw of circuit elements. Called after any state change.
    public void render() {
        hoverLabel.setVisible(false);
        nodeLayer.getChildren().clear();

        // 1. Wire segments (straight orthogonal lines)
        renderWireSegments();

        // 2. Component icons (selection glow + drag handlers)
        renderComponents();

        // 3. Port dots (on top of icons)
        renderPorts();

        // 4. Junction dots
        renderJunctions();

        // 5. Re-add preview line if active
        if (previewLine != null) {
            nodeLayer.getChildren().add(previewLine);
        }
    }

    // Layer 1: Wire segments
    private void renderWireSegments() {
        for (WireSegment seg : controller.getCircuit().getWireSegments()) {
            Polyline wire = new Polyline(
                    seg.getX1(), seg.getY1(),
                    seg.getX2(), seg.getY2());
            wire.setUserData(seg.getId());

            if (controller.getSelection().isWireSelected(seg.getId())) {
                wire.setEffect(new DropShadow(15, Color.CYAN));
            }

            Color wireColor = switch (seg.getState()) {
                case ON -> Color.web("#00c800");   // bright green — active
                case OFF -> Color.web("#404040");   // dark gray    — inactive
                case UNKNOWN -> Color.web("#000000");   // black        — unknown / undriven
            };
            wire.setStroke(wireColor);
            wire.setStrokeWidth(WIRE_WIDTH);
            wire.setFill(Color.TRANSPARENT);
            wire.setMouseTransparent(true);
            nodeLayer.getChildren().add(wire);
        }
    }

    // ── Layer 2: Component icons ───────────────────────────────────────────────

    private void renderComponents() {
        for (CircuitElement elem : controller.getCircuit().getElements().values()) {
            var icon = elem.getIconNode(iconRegistry);
            icon.setLayoutX(elem.getX());
            icon.setLayoutY(elem.getY());
            icon.setUserData(elem.getId());
            icon.setOnMouseEntered(e -> {
                hoverLabel.setText(elem.getId());
                hoverLabel.applyCss();
                hoverLabel.autosize();
                double componentWidth = elem instanceof Gate ? 128 : 64;
                double labelX = elem.getX() + (componentWidth - hoverLabel.getWidth()) / 2;
                double labelY = elem.getY() - hoverLabel.getHeight() / 3;
                hoverLabel.relocate(labelX, labelY);
                hoverLabel.setVisible(true);
                hoverLabel.toFront();
            });
            icon.setOnMouseExited(e -> {
                hoverLabel.setVisible(false);
            });
            if (controller.getSelection().isElementSelected(elem.getId())) {
                icon.setEffect(new DropShadow(15, Color.CYAN));
            }

            icon.setOnMousePressed(ev -> {
                if (ev.getButton() != MouseButton.PRIMARY || !isSelectToolActive()) return;
                dragIds = controller.getSelection().isElementSelected(elem.getId())
                        ? controller.getSelection().getSelectedElements()
                        : Set.of(elem.getId());
                Point2D p = contentGroup.sceneToLocal(ev.getSceneX(), ev.getSceneY());
                dragStartX = p.getX();
                dragStartY = p.getY();
                moving = false;
            });

            icon.setOnMouseDragged(ev -> {
                if (!ev.isPrimaryButtonDown() || !isSelectToolActive() || dragIds.isEmpty()) return;
                Point2D p = contentGroup.sceneToLocal(ev.getSceneX(), ev.getSceneY());
                double dx = p.getX() - dragStartX;
                double dy = p.getY() - dragStartY;
                if (!moving && Math.hypot(dx, dy) < 3) return;
                moving = true;
                for (Node n : nodeLayer.getChildren()) {
                    if (n.getUserData() instanceof String id && dragIds.contains(id)) {
                        n.setTranslateX(dx);
                        n.setTranslateY(dy);
                    }
                }
                ev.consume();
            });

            icon.setOnMouseReleased(ev -> {
                if (!moving) return;
                Point2D p = contentGroup.sceneToLocal(ev.getSceneX(), ev.getSceneY());
                double dx = p.getX() - dragStartX;
                double dy = p.getY() - dragStartY;
                controller.moveElements(dragIds, dx, dy);
                if (!controller.getSelection().isElementSelected(elem.getId())) {
                    controller.getSelection().selectElement(elem.getId(), false);
                }
                moving = false;
                dragIds = Set.of();
                render();
                ev.consume();
            });

            icon.setOnMouseClicked(ev -> {
                if (ev.getButton() != MouseButton.PRIMARY) return;
                if (!ev.isStillSincePress()) return;
                if (elem instanceof InputElement input) {
                    input.toggle();
                    ev.consume();
                    if (isSelectToolActive()) {
                        boolean additive = ev.isShiftDown() || ev.isControlDown();
                        controller.getSelection().selectElement(elem.getId(), additive);
                    } else {
                        render();
                    }
                    return;
                }
                // Normal component selection
                if (!isSelectToolActive()) return;
                ev.consume();
                boolean additive = ev.isShiftDown() || ev.isControlDown();
                controller.getSelection().selectElement(elem.getId(), additive);
            });
            nodeLayer.getChildren().add(icon);
        }
    }

    // ── Layer 3: Port dots ─────────────────────────────────────────────────────

    private void renderPorts() {
        Circuit circuit = controller.getCircuit();
        for (CircuitElement elem : circuit.getElements().values()) {
            List<Port> ports = getAllPorts(elem);
            for (Port port : ports) {
                double cx = port.getAbsX();
                double cy = port.getAbsY();

                Circle dot = new Circle(cx, cy, PORT_RADIUS);
                dot.setFill(COLOR_PORT_IDLE);
                dot.setStroke(Color.BLACK);
                dot.setStrokeWidth(0.5);
                dot.setUserData(elem.getId());

                dot.setOnMouseEntered(e -> {
                    dot.setFill(COLOR_PORT_HOVER);
                    dot.setRadius(PORT_RADIUS + 2);
                });
                dot.setOnMouseExited(e -> {
                    dot.setFill(COLOR_PORT_IDLE);
                    dot.setRadius(PORT_RADIUS);
                });

                nodeLayer.getChildren().add(dot);
            }
        }
    }

    // ── Layer 4: Junction dots ─────────────────────────────────────────────────

    private void renderJunctions() {
        for (PointNode node : controller.getCircuit().getPointNodes()) {
            if (node.isJunction()) {
                Circle jDot = new Circle(node.getX(), node.getY(), JUNCTION_RADIUS);
                jDot.setFill(COLOR_JUNCTION);
                jDot.setMouseTransparent(true);
                nodeLayer.getChildren().add(jDot);
            }
        }
    }

    // ── Helper: collect all ports from any element type ───────────────────────

    private List<Port> getAllPorts(CircuitElement elem) {
        List<Port> ports = new ArrayList<>();
        if (elem instanceof Gate gate) {
            ports.addAll(gate.getAllPorts());
        } else if (elem instanceof InputElement ie) {
            ports.add(ie.getOutputPort());
        } else if (elem instanceof OutputElement oe) {
            ports.add(oe.getInputPort());
        }
        return ports;
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private boolean isSelectToolActive() {
        return toolbarRef != null && toolbarRef.getSelected() == ToolbarOptions.Select;
    }

    private boolean isWireToolActive() {
        return toolbarRef != null && toolbarRef.getSelected() == ToolbarOptions.Wire;
    }

    public ViewportManager getViewportManager() {
        return viewportManager;
    }

    public void zoomIn() {
        viewportManager.zoomIn();
    }

    public void zoomOut() {
        viewportManager.zoomOut();
    }
}