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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Mouse's left button  --> select / rubber band / drag elements / draw wires
 * Mouse's right button --> pan the view
 *
 * The main drawing surface for the circuit editor.
 *
 * Rendering layers (bottom -> top):
 *   1. Black background (Canvas)
 *   2. Wire segments   (Polylines in nodeLayer)
 *   3. Component icons (SVG nodes in nodeLayer)
 *   4. Port dots       (Circles in nodeLayer, hover-highlighted)
 *   5. Junction dots   (filled Circles in nodeLayer)
 *   6. Wire preview    (Polyline in nodeLayer, only while dragging)
 *   7. Rubber band     (Rectangle above everything, only while selecting)
 */
public class CircuitCanvas extends Pane {

    // ── Constants ─────────────────────────────────────────────────────────────
    private static final double PORT_RADIUS     = 5.0;
    private static final double JUNCTION_RADIUS = 6.0;
    private static final double WIRE_WIDTH      = 2.0;

    private static final Color COLOR_WIRE        = Color.web("#000");
    private static final Color COLOR_PORT_IDLE   = Color.web("#4caf50");
    private static final Color COLOR_PORT_HOVER  = Color.web("#00e5ff");
    private static final Color COLOR_JUNCTION    = Color.web("#000");
    private static final Color COLOR_PREVIEW     = Color.web("#000");

    // ── Fields ────────────────────────────────────────────────────────────────
    private final EditorController controller;
    private final Canvas background;
    private final Group nodeLayer = new Group();
    private final ViewportManager viewportManager;
    private final SvgIconRegistry iconRegistry = new SvgIconRegistry();
    private final Rectangle rubberBand = new Rectangle();
    private CircuitElementPalette paletteRef;
    private EditorToolbar toolbarRef;

    // Pan
    private double lastMousePosition_X;
    private double lastMousePosition_Y;

    /** The live preview line shown while the user drags a wire. */
    private Polyline previewLine = null;

    // Selection (rubber band)
    private double startX, startY;
    private boolean pressedOnEmpty = false;
    private boolean dragging = false;

    // Move (dragging selected elements)
    private Set<String> dragIds = Set.of();
    private double dragStartX, dragStartY;   // in nodeLayer coordinates, so zoom/pan are handled
    private boolean moving = false;

    // ── Constructor ───────────────────────────────────────────────────────────
    public CircuitCanvas(EditorController controller) {
        this.controller = controller;
        this.background = new Canvas();
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        setClip(clip);
        // make the canvas size adaptive
        background.widthProperty().bind(widthProperty());
        background.heightProperty().bind(heightProperty());

        rubberBand.setFill(Color.rgb(0, 200, 255, 0.15));
        rubberBand.setStroke(Color.CYAN);
        rubberBand.setVisible(false);
        rubberBand.setMouseTransparent(true);

        controller.getSelection().setOnChange(this::render);

        widthProperty().addListener((obs, oldVal, newVal) -> makeBackground());
        heightProperty().addListener((obs, oldVal, newVal) -> makeBackground());

        this.viewportManager = new ViewportManager(nodeLayer);
        getChildren().addAll(background, nodeLayer, rubberBand);   // rubberBand last so it draws on top

        // ── ONE handler per event type. Each one branches on button / active tool. ──

        setOnMousePressed(e -> {
            pressedOnEmpty = false;

            if (e.getButton() == MouseButton.SECONDARY) {          // pan
                lastMousePosition_X = e.getX();
                lastMousePosition_Y = e.getY();
                return;
            }
            if (e.getButton() != MouseButton.PRIMARY) return;

            if (isWireToolActive()) {                              // wire tool
                Point2D p = nodeLayer.sceneToLocal(e.getSceneX(), e.getSceneY());
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
            pressedOnEmpty = isSelectToolActive() && e.getTarget() == background;
            dragging = false;
            startX = e.getX();
            startY = e.getY();
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
                Point2D p = nodeLayer.sceneToLocal(e.getSceneX(), e.getSceneY());
                double[] pts = controller.onWireMouseDragged(p.getX(), p.getY());
                if (pts != null && previewLine != null) {
                    // pts = [startX, startY, bendX, bendY, mouseX, mouseY]
                    previewLine.getPoints().setAll(pts[0], pts[1], pts[2], pts[3], pts[4], pts[5]);
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
                Point2D p = nodeLayer.sceneToLocal(e.getSceneX(), e.getSceneY());
                controller.onWireMouseReleased(p.getX(), p.getY());
                // render() is called inside onWireMouseReleased via notifyRender()
                return;
            }

            if (!pressedOnEmpty) return;                           // finish rubber band
            pressedOnEmpty = false;
            rubberBand.setVisible(false);
            if (!dragging) return;

            // compare in scene space so pan/zoom on nodeLayer does not break the overlap test
            Bounds box = rubberBand.localToScene(rubberBand.getBoundsInLocal());
            List<String> hits = new ArrayList<>();
            for (Node n : nodeLayer.getChildren()) {
                if (n.getUserData() instanceof String id            // skips wires and junctions (no id)
                        && n.localToScene(n.getBoundsInLocal()).intersects(box)) {
                    hits.add(id);
                }
            }
            controller.getSelection().selectElements(hits, e.isShiftDown() || e.isControlDown());
        });

        // plain click
        setOnMouseClicked(e -> {
            if (!e.isStillSincePress() || e.getButton() != MouseButton.PRIMARY) return;   // a drag also produces a click
            if (paletteRef == null || toolbarRef == null) return;

            ComponentType comp = paletteRef.getSelected();
            ToolbarOptions tool = toolbarRef.getSelected();

            if (comp != null && comp != ComponentType.UNKNOWN) {
                Point2D p = nodeLayer.sceneToLocal(e.getSceneX(), e.getSceneY());
                controller.onElementPlaced(comp, p.getX(), p.getY());
            } else if (tool != null && tool != ToolbarOptions.Unknown && tool != ToolbarOptions.Wire) {
                controller.optionSelected(tool, e.getX(), e.getY());
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
    }

    public void makeBackground() {
        GraphicsContext gc = background.getGraphicsContext2D();
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, background.getWidth(), background.getHeight());
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    /**
     * Full redraw. Called after every state change (element placed, wire added, selection changed, etc.).
     */
    public void render() {
        makeBackground();
        nodeLayer.getChildren().clear();

        // 2. Wire segments
        renderWireSegments();

        // 3. Component icons (selection glow + drag handlers live here)
        renderComponents();

        // 4. Port dots (on top of icons so they are always visible)
        renderPorts();

        // 5. Junction dots
        renderJunctions();

        // 6. Re-add the preview line if one is active
        if (previewLine != null) {
            nodeLayer.getChildren().add(previewLine);
        }
    }

    // ── Layer 2: Wire segments ─────────────────────────────────────────────────

    private void renderWireSegments() {
        for (WireSegment seg : controller.getCircuit().getWireSegments()) {
            Point2D bend = seg.getBendPoint();
            Polyline wire = new Polyline(
                    seg.getX1(), seg.getY1(),
                    bend.getX(), bend.getY(),
                    seg.getX2(), seg.getY2());
            wire.setStroke(COLOR_WIRE);
            wire.setStrokeWidth(WIRE_WIDTH);
            wire.setFill(Color.TRANSPARENT);
            wire.setMouseTransparent(true); // hit-testing done manually in controller
            nodeLayer.getChildren().add(wire);
        }
    }

    // ── Layer 3: Component icons ───────────────────────────────────────────────

    private void renderComponents() {
        for (CircuitElement elem : controller.getCircuit().getElements().values()) {
            var icon = elem.getIconNode(iconRegistry);
            icon.setLayoutX(elem.getX());
            icon.setLayoutY(elem.getY());
            icon.setUserData(elem.getId());   // maps a node back to its element (rubber band + drag)

            if (controller.getSelection().isElementSelected(elem.getId())) {
                icon.setEffect(new DropShadow(15, Color.CYAN));
            }

            icon.setOnMousePressed(ev -> {
                if (ev.getButton() != MouseButton.PRIMARY || !isSelectToolActive()) return;
                // pressing an already-selected icon drags the whole selection;
                // pressing an unselected one drags just that icon
                dragIds = controller.getSelection().isElementSelected(elem.getId())
                        ? controller.getSelection().getSelectedElements()
                        : Set.of(elem.getId());
                Point2D p = nodeLayer.sceneToLocal(ev.getSceneX(), ev.getSceneY());
                dragStartX = p.getX();
                dragStartY = p.getY();
                moving = false;
            });

            icon.setOnMouseDragged(ev -> {
                if (!ev.isPrimaryButtonDown() || !isSelectToolActive() || dragIds.isEmpty()) return;
                Point2D p = nodeLayer.sceneToLocal(ev.getSceneX(), ev.getSceneY());
                double dx = p.getX() - dragStartX;
                double dy = p.getY() - dragStartY;
                if (!moving && Math.hypot(dx, dy) < 3) return;   // tiny jitter still counts as a click
                moving = true;
                for (Node n : nodeLayer.getChildren()) {
                    if (n.getUserData() instanceof String id && dragIds.contains(id)) {
                        n.setTranslateX(dx);   // visual only, the model is untouched
                        n.setTranslateY(dy);
                    }
                }
                ev.consume();
            });

            icon.setOnMouseReleased(ev -> {
                if (!moving) return;
                Point2D p = nodeLayer.sceneToLocal(ev.getSceneX(), ev.getSceneY());
                double dx = p.getX() - dragStartX;
                double dy = p.getY() - dragStartY;
                controller.moveElements(dragIds, dx, dy);                    // commit once
                if (!controller.getSelection().isElementSelected(elem.getId())) {
                    controller.getSelection().selectElement(elem.getId(), false);  // dragged an unselected icon: select it
                }
                moving = false;
                dragIds = Set.of();
                render();                                                     // rebuild once, at the new positions
                ev.consume();
            });

            icon.setOnMouseClicked(ev -> {
                if (ev.getButton() != MouseButton.PRIMARY || !isSelectToolActive()) return;
                ev.consume();
                if (!ev.isStillSincePress()) return;   // tail end of a drag, not a real click
                boolean additive = ev.isShiftDown() || ev.isControlDown();
                controller.getSelection().selectElement(elem.getId(), additive);
            });

            nodeLayer.getChildren().add(icon);
        }
    }

    // ── Layer 4: Port dots ─────────────────────────────────────────────────────

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
                dot.setUserData(elem.getId());   // so the dot moves along with its element while dragging

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

    // ── Layer 5: Junction dots ─────────────────────────────────────────────────

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
}