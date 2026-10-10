package com.example.mini_project.gui;

import com.example.mini_project.controller.EditorController;
import com.example.mini_project.domain.*;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polyline;

import java.util.ArrayList;
import java.util.List;

/**
 * The main drawing surface for the circuit editor.
 *
 * Rendering layers (bottom → top):
 *   1. Black background (Canvas)
 *   2. Wire segments   (Polylines in nodeLayer)
 *   3. Component icons (SVG nodes in nodeLayer)
 *   4. Port dots       (Circles in nodeLayer — hover-highlighted)
 *   5. Junction dots   (filled Circles in nodeLayer)
 *   6. Wire preview    (Polyline in nodeLayer, only while dragging)
 */
public class CircuitCanvas extends Pane {

    // ── Constants ─────────────────────────────────────────────────────────────
    private static final double PORT_RADIUS     = 5.0;   // visual radius of port dot
    private static final double JUNCTION_RADIUS = 6.0;   // visual radius of junction dot
    private static final double WIRE_WIDTH      = 2.0;   // stroke width of wires

    private static final Color COLOR_BACKGROUND  = Color.web("#1a1a2e");
    private static final Color COLOR_WIRE        = Color.web("#00c896");   // teal-green
    private static final Color COLOR_PORT_IDLE   = Color.web("#4caf50");   // green
    private static final Color COLOR_PORT_HOVER  = Color.web("#00e5ff");   // cyan
    private static final Color COLOR_PORT_ACTIVE = Color.web("#ff9800");   // orange — currently drawing
    private static final Color COLOR_JUNCTION    = Color.web("#00c896");   // same as wire
    private static final Color COLOR_PREVIEW     = Color.web("#ffffff");   // white dotted preview line

    // ── Fields ────────────────────────────────────────────────────────────────
    private final EditorController controller;
    private final Canvas            background;
    private final Group             nodeLayer = new Group();
    private final SvgIconRegistry   iconRegistry = new SvgIconRegistry();

    private CircuitElementPalette paletteRef;
    private EditorToolbar         toolbarRef;

    /** The live preview line shown while the user drags a wire. */
    private Polyline previewLine = null;

    // ── Constructor ───────────────────────────────────────────────────────────

    public CircuitCanvas(EditorController controller, double w, double h) {
        this.controller = controller;
        controller.setCanvas(this);

        this.background = new Canvas(w, h);
        getChildren().addAll(background, nodeLayer);

        hookMouseEvents();
        render();
    }

    // ── Palette / Toolbar wiring ───────────────────────────────────────────────

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

    // ── Mouse Events ──────────────────────────────────────────────────────────

    private void hookMouseEvents() {

        // ── Click — place component or dispatch toolbar action ─────────────────
        setOnMouseClicked(e -> {
            if (paletteRef == null || toolbarRef == null) return;

            ComponentType comp = paletteRef.getSelected();
            ToolbarOptions tool = toolbarRef.getSelected();

            if (comp != null && comp != ComponentType.UNKNOWN) {
                controller.onElementPlaced(comp, e.getX(), e.getY());
                render();
            } else if (tool != null && tool != ToolbarOptions.Unknown
                    && tool != ToolbarOptions.Wire) {
                // Non-wire toolbar actions (Select, Delete, etc.)
                controller.optionSelected(tool, e.getX(), e.getY());
            }
        });

        // ── Mouse Pressed — begin wire drag if Wire tool is active ─────────────
        setOnMousePressed(e -> {
            if (!isWireToolActive()) return;
            controller.onWireMousePressed(e.getX(), e.getY());

            if (controller.isDrawingWire()) {
                // Put up an empty preview line now; it will be updated on drag
                previewLine = new Polyline();
                previewLine.setStroke(COLOR_PREVIEW);
                previewLine.setStrokeWidth(WIRE_WIDTH);
                previewLine.getStrokeDashArray().addAll(6.0, 4.0);
                previewLine.setMouseTransparent(true);
                nodeLayer.getChildren().add(previewLine);
            }
        });

        // ── Mouse Dragged — update preview line ────────────────────────────────
        setOnMouseDragged(e -> {
            if (!controller.isDrawingWire()) return;

            double[] pts = controller.onWireMouseDragged(e.getX(), e.getY());
            if (pts != null && previewLine != null) {
                // pts = [startX, startY, bendX, bendY, mouseX, mouseY]
                previewLine.getPoints().setAll(pts[0], pts[1], pts[2], pts[3], pts[4], pts[5]);
            }
        });

        // ── Mouse Released — finalise wire segment ─────────────────────────────
        setOnMouseReleased(e -> {
            if (!isWireToolActive()) return;

            // Remove preview line
            if (previewLine != null) {
                nodeLayer.getChildren().remove(previewLine);
                previewLine = null;
            }

            controller.onWireMouseReleased(e.getX(), e.getY());
            // render() is called inside onWireMouseReleased via notifyRender()
        });
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    /**
     * Full redraw.  Called after every state change (element placed, wire added, etc.).
     */
    public void render() {
        // 1. Background
        GraphicsContext gc = background.getGraphicsContext2D();
        gc.setFill(COLOR_BACKGROUND);
        gc.fillRect(0, 0, background.getWidth(), background.getHeight());

        nodeLayer.getChildren().clear();

        // 2. Wire segments
        renderWireSegments();

        // 3. Component icons
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

                // Hover highlight
                dot.setOnMouseEntered(e -> dot.setFill(COLOR_PORT_HOVER));
                dot.setOnMouseExited(e  -> dot.setFill(COLOR_PORT_IDLE));

                // Tooltip-style cursor change when hovering over a port
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

    // ── Helper — collect all ports from any element type ─────────────────────

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

    private boolean isWireToolActive() {
        return toolbarRef != null && toolbarRef.getSelected() == ToolbarOptions.Wire;
    }
}
