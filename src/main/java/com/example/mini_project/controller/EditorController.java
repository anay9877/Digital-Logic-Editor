package com.example.mini_project.controller;

import com.example.mini_project.domain.*;
import com.example.mini_project.gui.CircuitCanvas;
import com.example.mini_project.gui.ToolbarOptions;
import com.example.mini_project.validation.*;
import com.example.mini_project.controller.command.*;
import com.example.mini_project.validation.CircuitSimulator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class EditorController {

    private final Circuit circuit;
    private final Map<ComponentType, Integer> counters = new HashMap<>();
    private final SelectionManager selection = new SelectionManager();
    private final HistoryManager history = new HistoryManager();

    // Wire drawing state
    private boolean drawingWire = false;
    private Port dragStartPort;
    private PointNode dragStartNode;
    private WireSegment dragStartSegment;
    private final ConnectionValidator connectionValidator = new ConnectionValidator();
    private double dragStartX, dragStartY;
    private boolean horizontalFirst = true;
    private boolean bendDirectionLocked = false;
    private WireChangeCommand.Image wireGestureBefore;
    private WireChangeCommand.Image liveMoveWiresBefore;
    private final Map<String, MoveOrigin> liveMoveOrigins = new HashMap<>();
    private boolean liveMoving;
    private CircuitSimulator simulator;
    private boolean simulating = false;
    private boolean isValidated = false;
    //prajw : start
    private Consumer<List<String>> onValidationErrors;
    //prajw: end

    private CircuitCanvas canvas;

    private record MoveOrigin(CircuitElement element, double x, double y) {}

    public EditorController(Circuit circuit) {
        this.circuit = circuit;
    }

    public void setCanvas(CircuitCanvas canvas) {
        this.canvas = canvas;
    }

    public Circuit getCircuit() {
        return circuit;
    }

    public SelectionManager getSelection() {
        return selection;
    }

    public CircuitCanvas getCanvas() {
        return canvas;
    }

    //prajw : start
    /** Sets the UI callback that receives a batch of validation errors. */
    public void setOnValidationErrors(Consumer<List<String>> listener) {
        onValidationErrors = listener;
    }
    //prajw: end

    // Used for deletion of entire circuit
    public void newCircuit() {
        circuit.clear();
        counters.clear();
        selection.clear();
        PointNode.resetId();
        WireSegment.resetId();
        drawingWire = false;
        dragStartPort = null;
        dragStartNode = null;
        dragStartSegment = null;
        wireGestureBefore = null;
        history.clear();
        if (canvas != null && canvas.getViewportManager() != null) {
            canvas.getViewportManager().resetView();
        }
        stopSimulation();
        notifyRender();
    }

    // Used for generation of ids
    public void updateCounter(ComponentType type, int count) {
        counters.put(type, Math.max(counters.getOrDefault(type, 0), count));
    }

    public void onElementPlaced(ComponentType type, double x, double y) {
        int n = counters.getOrDefault(type, 0) + 1;
        counters.put(type, n);
        String id = type.name() + "-" + n;
        CircuitElement element = type.create(id, CircuitCanvas.snap(x), CircuitCanvas.snap(y));
        circuit.addElement(element);
        stopSimulation();
        history.record(new PlaceElementCommand(circuit, element));
        selection.clear();
    }

    public void toggleInput(InputElement input) {
        SignalState before = input.getValue();
        input.toggle();
        notifyRender();
        history.record(new ToggleInputCommand(input, before, input.getValue()));
    }

    // Tells which option is selected from toolbar(implemented just for select and delete)
    public void optionSelected(ToolbarOptions option) {
        switch (option) {
            //prajw : start
            // Explicitly choosing Select clears the old highlight; ordinary clicks keep the current selection.
            case Select -> selection.clear();
            //prajw: end
            case Delete -> handleDelete();
            case Validate -> handleValidate();
            case Simulate -> runSimulation();
        }
    }

    private void handleDelete() {
        deleteSelected();
    }

    public void deleteSelected() {
        if (drawingWire) return;
        Collection<String> elementIds = selection.getSelectedElements();
        Set<String> wireIds = selection.getSelectedWires();
        if (elementIds.isEmpty() && wireIds.isEmpty()) return;

        WireChangeCommand.Image wiresBefore = WireChangeCommand.capture(circuit);
        List<CircuitElement> removedElements = new ArrayList<>();
        for (String id : elementIds) {
            CircuitElement element = circuit.getElement(id);
            if (onElementDeleted(id) && element != null) removedElements.add(element);
        }
        boolean wiresRemoved = circuit.removeWireSegments(wireIds) > 0;

        selection.clear();
        if (!removedElements.isEmpty() || wiresRemoved) {
            stopSimulation();
            notifyRender();
            if (removedElements.isEmpty()) {
                recordWithWires("Delete wires", wiresBefore);
            } else {
                recordWithWires("Delete selection", wiresBefore,
                        new RemoveElementsCommand(circuit, removedElements));
            }
        }
    }

    public boolean onElementDeleted(String id) {
        return circuit.removeElement(id);   // counterpart of onElementPlaced
    }

    public List<CircuitElement> validateAndGetOrder() {
        CircuitValidator validator = new CircuitValidator();
        boolean wiresValid = validator.validateDanglingWires(circuit);
        boolean inputsValid = validator.validateInputPorts(circuit);
        boolean ioValid = validator.validateInputsAndOutputs(circuit);
        boolean logicValid = validator.validateDeadLogic(circuit);
        boolean referencesValid = validator.validateWireReferences(circuit);
        if (!(wiresValid && inputsValid && ioValid && logicValid && referencesValid)) {
            //prajw : start
            List<String> errors = validator.getErrors();
            publishValidationErrors(errors);
            throw new IllegalStateException(String.join(System.lineSeparator(), errors));
            //prajw: end
        }
        isValidated = true;
        //prajw : start
        try {
            return new CircuitTopology().sort(circuit);
        } catch (IllegalStateException ex) {
            publishValidationErrors(List.of(ex.getMessage()));
            throw ex;
        }
        //prajw: end
    }

    //prajw : start
    private void publishValidationErrors(List<String> errors) {
        if (onValidationErrors != null && !errors.isEmpty()) {
            onValidationErrors.accept(List.copyOf(errors));
        }
    }
    //prajw: end

    //prajw : start
    /** Publishes one non-validation error, such as a rejected wire connection. */
    private void publishError(String error) {
        publishValidationErrors(List.of(error));
    }
    //prajw: end

    public void runSimulation() {
        try {
            List<CircuitElement> order = validateAndGetOrder();
            simulator = new CircuitSimulator(circuit, order);
            simulating = true;
            printLedOutputs(order);
            notifyRender();            // redraws LEDs: ON -> LED_ON_SVG, OFF -> LED_OFF_SVG
        } catch (IllegalStateException ex) {
            System.err.println("Cannot simulate: " + ex.getMessage());
            simulating = false;
            simulator = null;
        }
    }

    private void printLedOutputs(List<CircuitElement> order) {
        System.out.println("---- Simulation result ----");
        for (CircuitElement element : order) {
            if (element instanceof OutputElement led) {
                System.out.println(led.getId() + " is " + led.getValue());
            }
        }
    }

    private void stopSimulation() {
        simulating = false;
        simulator = null;
    }

    private void handleValidate() {
        try {
            List<CircuitElement> order = validateAndGetOrder();
            System.out.println("Circuit validation passed.");
            System.out.println("Simulation order:");
            for (CircuitElement element : order) {
                System.out.println(element.getId());
            }
        } catch (IllegalStateException ex) {
            System.err.println("Circuit validation failed: " + ex.getMessage());
        }
    }

    public void validateCircuit() {
        handleValidate();
    }

    public void moveElements(Collection<String> ids, double dx, double dy) {
        WireChangeCommand.Image wiresBefore = WireChangeCommand.capture(circuit);
        List<MoveElementsCommand.Move> moves = new ArrayList<>();
        List<CircuitElement> movedElements = new ArrayList<>();
        for (String id : ids) {
            CircuitElement e = circuit.getElement(id);
            if (e != null) {
                double oldX = e.getX();
                double oldY = e.getY();
                double newX = CircuitCanvas.snap(e.getX() + dx);
                double newY = CircuitCanvas.snap(e.getY() + dy);
                if (newX == e.getX() && newY == e.getY()) continue;
                e.setPosition(newX, newY);
                movedElements.add(e);
                moves.add(new MoveElementsCommand.Move(e, oldX, oldY, newX, newY));
            }
        }
        if (!moves.isEmpty()) {
            for (CircuitElement element : movedElements) rerouteElementWires(element);
            stopSimulation();
            notifyRender();
            recordWithWires("Move elements", wiresBefore, new MoveElementsCommand(moves));
        }
    }

    public void beginLiveMove() {
        liveMoveOrigins.clear();
        for (CircuitElement element : circuit.getElements().values()) {
            liveMoveOrigins.put(element.getId(),
                    new MoveOrigin(element, element.getX(), element.getY()));
        }
        liveMoveWiresBefore = WireChangeCommand.capture(circuit);
        liveMoving = true;
        stopSimulation();
    }

    public void liveMove(Collection<String> ids, double dx, double dy) {
        if (!liveMoving || liveMoveWiresBefore == null) return;

        WireChangeCommand currentChanges =
                WireChangeCommand.diff(circuit, liveMoveWiresBefore, WireChangeCommand.capture(circuit));
        if (currentChanges != null) currentChanges.undo();

        List<CircuitElement> moved = new ArrayList<>();
        for (String id : ids) {
            MoveOrigin origin = liveMoveOrigins.get(id);
            if (origin == null) continue;
            CircuitElement element = origin.element();
            element.setPosition(CircuitCanvas.snap(origin.x() + dx), CircuitCanvas.snap(origin.y() + dy));
            moved.add(element);
        }
        for (CircuitElement element : moved) rerouteElementWires(element);
        if (canvas != null) canvas.renderWiresOnly();
    }

    public void endLiveMove(Collection<String> ids, double dx, double dy) {
        if (!liveMoving) return;
        liveMove(ids, dx, dy);
        liveMoving = false;

        List<MoveElementsCommand.Move> moves = new ArrayList<>();
        for (String id : ids) {
            MoveOrigin origin = liveMoveOrigins.get(id);
            if (origin == null) continue;
            CircuitElement element = origin.element();
            if (origin.x() != element.getX() || origin.y() != element.getY()) {
                moves.add(new MoveElementsCommand.Move(
                        element, origin.x(), origin.y(), element.getX(), element.getY()));
            }
        }

        WireChangeCommand.Image wiresBefore = liveMoveWiresBefore;
        liveMoveWiresBefore = null;
        liveMoveOrigins.clear();
        if (!moves.isEmpty()) {
            recordWithWires("Move elements", wiresBefore, new MoveElementsCommand(moves));
        }
        notifyRender();
    }

    // Wire drawing — called by CircuitCanvas mouse handlers
    private static final double PORT_HIT_RADIUS = CircuitCanvas.GRID_SIZE * 1.5;  // 15px
    private static final double NODE_HIT_RADIUS = CircuitCanvas.GRID_SIZE * 1.5;  // 15px
    private static final double WIRE_HIT_THRESHOLD = CircuitCanvas.GRID_SIZE * 0.6;  // 6px
    private static final double DIRECTION_LOCK_DIST = CircuitCanvas.GRID_SIZE;        // 10px
    private static final double WIRE_SELECT_THRESHOLD = 8.0;

    public void onWireMousePressed(double x, double y) {
        Port port = findPortAt(x, y);
        if (port != null) {
            onWireStartFromPort(port);
        } else {
            onWireStartFromCanvas(x, y);
        }
    }

    public void onWireStartFromPort(Port port) {
        wireGestureBefore = WireChangeCommand.capture(circuit);
        dragStartPort = port;
        dragStartNode = null;
        dragStartSegment = null;
        dragStartX = CircuitCanvas.snap(port.getAbsX());
        dragStartY = CircuitCanvas.snap(port.getAbsY());
        drawingWire = true;
        bendDirectionLocked = false;
    }

    public boolean onWireStartFromCanvas(double x, double y) {
        wireGestureBefore = WireChangeCommand.capture(circuit);
        x = CircuitCanvas.snap(x);
        y = CircuitCanvas.snap(y);
        dragStartNode = null;
        dragStartPort = null;
        dragStartSegment = null;

        PointNode hitNode = findNodeAt(x, y);
        if (hitNode != null) {
            dragStartNode = hitNode;
            dragStartX = CircuitCanvas.snap(hitNode.getX());
            dragStartY = CircuitCanvas.snap(hitNode.getY());
            drawingWire = true;
            bendDirectionLocked = false;
            return true;
        }

        WireSegment hitSeg = findWireSegmentAt(x, y);
        if (hitSeg != null) {
            dragStartSegment = hitSeg;
            double sx = x;
            double sy = y;
            if (hitSeg.isHorizontal()) {
                sy = hitSeg.getY1();
                sx = Math.clamp(x, Math.min(hitSeg.getX1(), hitSeg.getX2()),
                        Math.max(hitSeg.getX1(), hitSeg.getX2()));
            } else {
                sx = hitSeg.getX1();
                sy = Math.clamp(y, Math.min(hitSeg.getY1(), hitSeg.getY2()),
                        Math.max(hitSeg.getY1(), hitSeg.getY2()));
            }
            dragStartX = CircuitCanvas.snap(sx);
            dragStartY = CircuitCanvas.snap(sy);
            drawingWire = true;
            bendDirectionLocked = false;
            return true;
        }

        drawingWire = false;
        wireGestureBefore = null;
        return false;
    }

    public double[] onWireMouseDragged(double mouseX, double mouseY) {
        if (!drawingWire) return null;

        mouseX = CircuitCanvas.snap(mouseX);
        mouseY = CircuitCanvas.snap(mouseY);

        if (!bendDirectionLocked) {
            double dx = Math.abs(mouseX - dragStartX);
            double dy = Math.abs(mouseY - dragStartY);
            if (dx >= DIRECTION_LOCK_DIST || dy >= DIRECTION_LOCK_DIST) {
                horizontalFirst = dx >= dy;
                bendDirectionLocked = true;
            }
        }

        if (mouseX == dragStartX && mouseY == dragStartY) {
            return null;
        }

        if (mouseX == dragStartX || mouseY == dragStartY) {
            // Straight line preview: 2 points
            return new double[]{dragStartX, dragStartY, mouseX, mouseY};
        } else {
            // L-bend preview: 3 points (2 straight segments)
            double bendX = horizontalFirst ? mouseX : dragStartX;
            double bendY = horizontalFirst ? dragStartY : mouseY;
            return new double[]{dragStartX, dragStartY, bendX, bendY, mouseX, mouseY};
        }
    }

    public void onWireMouseReleased(double mouseX, double mouseY) {
        try {
            handleWireRelease(mouseX, mouseY);
        } finally {
            stopSimulation();
            commitWireGesture();
        }
    }

    private void handleWireRelease(double mouseX, double mouseY) {
        if (!drawingWire) return;
        drawingWire = false;

        mouseX = CircuitCanvas.snap(mouseX);
        mouseY = CircuitCanvas.snap(mouseY);
        if (mouseX == dragStartX && mouseY == dragStartY) {
            dragStartSegment = null;
            return;
        }
        // Identify what the user is trying to connect.
        Port targetPort = findPortAt(mouseX, mouseY);
        PointNode targetNode = null;
        WireSegment targetSegment = null;
        if (targetPort == null) {
            targetNode = findNodeAt(mouseX, mouseY);
        }
        if (targetPort == null && targetNode == null) {
            targetSegment = findWireSegmentAt(mouseX, mouseY);
        }
        if (targetPort == null && targetNode == null && targetSegment == null) {
            //prajw : start
            String error = "CONNECTION REJECTED: Wire cannot end in empty space";
            System.err.println(error);
            publishError(error);
            //prajw: end
            dragStartPort = null;
            dragStartNode = null;
            dragStartSegment = null;
            return;
        }
        Object start = dragStartPort != null ? dragStartPort : dragStartNode != null ? dragStartNode : dragStartSegment;
        Object end = targetPort != null ? targetPort : targetNode != null ? targetNode : targetSegment;

        // Validate before modifying the circuit.
        String error = connectionValidator.checkWireLength(dragStartX, dragStartY, mouseX, mouseY);
        if (error == null)
            error = connectionValidator.checkInputToInput(start, end);
        if (error == null) {
            error = connectionValidator.checkInputAlreadyConnected(circuit, start, end);
        }
        if (error == null) {
            error = connectionValidator.checkSingleDriver(circuit, start, end);
        }
        if (error == null) {
            error = connectionValidator.checkCombinationalCycle(circuit, start, end);
        }
        if (error == null) {
            error = connectionValidator.checkDuplicateConnection(circuit, start, end);
        }
        if (error != null) {
            //prajw : start
            String message = "CONNECTION REJECTED: " + error;
            System.err.println(message);
            publishError(message);
            //prajw: end
            dragStartPort = null;
            dragStartNode = null;
            dragStartSegment = null;
            return;
        }

        // Connection passed this validation.
        // Only now may we split an existing starting wire.
        if (dragStartSegment != null) {
            dragStartNode = circuit.splitSegmentAtJunction(dragStartSegment, dragStartX, dragStartY);
            dragStartPort = null;
            dragStartSegment = null;
            dragStartX = dragStartNode.getX();
            dragStartY = dragStartNode.getY();
        }
        boolean hasBend = (mouseX != dragStartX) && (mouseY != dragStartY);
        double bendX = horizontalFirst ? mouseX : dragStartX;
        double bendY = horizontalFirst ? dragStartY : mouseY;

        double leg1EndX = hasBend ? bendX : mouseX;
        double leg1EndY = hasBend ? bendY : mouseY;
        boolean leg1IsHorizontal = Math.abs(dragStartY - leg1EndY) < 0.001;

        // Check if we can EXTEND an existing segment at dragStartNode
        WireSegment segToExtend = null;
        if (dragStartNode != null && dragStartNode.getConnectedSegments().size() == 1) {
            WireSegment cand = dragStartNode.getConnectedSegments().get(0);
            if ((cand.isHorizontal() && leg1IsHorizontal) || (cand.isVertical() && !leg1IsHorizontal)) {
                segToExtend = cand;
            }
        }

        if (segToExtend != null) {
            if (!hasBend) {
                // Entire drag is collinear: extend segToExtend to (mouseX, mouseY)
                attachSegmentEnd(segToExtend, dragStartNode, mouseX, mouseY);
            } else {
                // Leg 1 extends segToExtend to (bendX, bendY)
                updateSegmentNode(segToExtend, dragStartNode, bendX, bendY);
                // Leg 2 is a new segment from bendX, bendY to mouseX, mouseY
                createAndAddSegment(null, dragStartNode, bendX, bendY, mouseX, mouseY);
            }
        } else {
            if (!hasBend) {
                // Straight: 1 new segment
                createAndAddSegment(dragStartPort, dragStartNode, dragStartX, dragStartY, mouseX, mouseY);
            } else {
                // Bend: 2 new segments meeting at cornerNode
                PointNode cornerNode = new PointNode(bendX, bendY);
                circuit.addPointNode(cornerNode);

                // Leg 1: start -> cornerNode
                WireSegment leg1;
                if (dragStartPort != null) {
                    leg1 = WireSegment.portToNode(dragStartPort, dragStartX, dragStartY, cornerNode);
                } else {
                    leg1 = WireSegment.nodeToNode(dragStartNode, cornerNode);
                }
                circuit.addWireSegment(leg1);

                // Leg 2: cornerNode -> (mouseX, mouseY)
                createAndAddSegment(null, cornerNode, bendX, bendY, mouseX, mouseY);
            }
        }
        notifyRender();
    }

    public void onSelectPoint(double x, double y, boolean additive, boolean wholeWire) {
        //prajw : start
        // A hit changes the selected wire; an empty click clears selection only when it is not additive.
        WireSegment hit = findNearestWire(x, y);
        if (hit == null) {
            if (!additive) {
                selection.clear();          // same behaviour as the old handleSelect
            }
            return;
        }
        if (wholeWire) {
            List<String> ids = new ArrayList<>();
            for (WireSegment s : circuit.getConnectedWires(hit)) {
                ids.add(s.getId());
            }
            selection.selectWires(ids, additive);
        } else {
            selection.selectWire(hit.getId(), additive);
        }
        //prajw: end
    }

    public WireSegment findNearestWire(double x, double y) {
        WireSegment best = null;
        double bestDist = WIRE_SELECT_THRESHOLD;
        for (WireSegment seg : circuit.getWireSegments()) {
            double d = seg.distanceToPoint(x, y);
            if (d <= bestDist) {
                bestDist = d;
                best = seg;
            }   // nearest wins near corners and junctions
        }
        return best;
    }

    private void attachSegmentEnd(WireSegment seg, PointNode currentNode, double targetX, double targetY) {
        boolean isStart = (seg.getStartNode() == currentNode);

        Port hitPort = findPortAt(targetX, targetY);
        if (hitPort != null) {
            double hx = hitPort.getAbsX();
            double hy = hitPort.getAbsY();
            if (isStart) {
                seg.setStartPort(hitPort);
                seg.setX1(hx);
                seg.setY1(hy);
            } else {
                seg.setEndPort(hitPort);
                seg.setX2(hx);
                seg.setY2(hy);
            }
            circuit.removePointNode(currentNode);
            return;
        }

        PointNode hitNode = findNodeAt(targetX, targetY);
        if (hitNode != null && hitNode != currentNode) {
            double nx = hitNode.getX();
            double ny = hitNode.getY();
            if (isStart) {
                seg.setStartNode(hitNode);
                seg.setX1(nx);
                seg.setY1(ny);
                hitNode.addSegment(seg);
            } else {
                seg.setEndNode(hitNode);
                seg.setX2(nx);
                seg.setY2(ny);
                hitNode.addSegment(seg);
            }
            circuit.removePointNode(currentNode);
            return;
        }

        WireSegment hitSeg = findWireSegmentAt(targetX, targetY);
        if (hitSeg != null && hitSeg != seg) {
            PointNode junction = circuit.splitSegmentAtJunction(hitSeg, targetX, targetY);
            double jx = junction.getX();
            double jy = junction.getY();
            if (isStart) {
                seg.setStartNode(junction);
                seg.setX1(jx);
                seg.setY1(jy);
                junction.addSegment(seg);
            } else {
                seg.setEndNode(junction);
                seg.setX2(jx);
                seg.setY2(jy);
                junction.addSegment(seg);
            }
            circuit.removePointNode(currentNode);
            return;
        }

        // Empty space: update node and segment coordinates
        currentNode.setX(targetX);
        currentNode.setY(targetY);
        if (isStart) {
            seg.setX1(targetX);
            seg.setY1(targetY);
        } else {
            seg.setX2(targetX);
            seg.setY2(targetY);
        }
    }

    private void updateSegmentNode(WireSegment seg, PointNode currentNode, double nx, double ny) {
        currentNode.setX(nx);
        currentNode.setY(ny);
        if (seg.getStartNode() == currentNode) {
            seg.setX1(nx);
            seg.setY1(ny);
        } else {
            seg.setX2(nx);
            seg.setY2(ny);
        }
    }

    private void createAndAddSegment(Port startPort, PointNode startNode, double x1, double y1, double targetX, double targetY) {
        Port hitPort = findPortAt(targetX, targetY);
        if (hitPort != null && hitPort != startPort) {
            double hx = hitPort.getAbsX();
            double hy = hitPort.getAbsY();
            WireSegment seg = (startPort != null)
                    ? WireSegment.portToPort(startPort, x1, y1, hitPort, hx, hy)
                    : WireSegment.nodeToPort(startNode, hitPort, hx, hy);
            circuit.addWireSegment(seg);
            return;
        }

        PointNode hitNode = findNodeAt(targetX, targetY);
        if (hitNode != null && hitNode != startNode) {
            WireSegment seg = (startPort != null)
                    ? WireSegment.portToNode(startPort, x1, y1, hitNode)
                    : WireSegment.nodeToNode(startNode, hitNode);
            circuit.addWireSegment(seg);
            return;
        }

        WireSegment hitSeg = findWireSegmentAt(targetX, targetY);
        if (hitSeg != null) {
            PointNode junction = circuit.splitSegmentAtJunction(hitSeg, targetX, targetY);
            WireSegment seg = (startPort != null)
                    ? WireSegment.portToNode(startPort, x1, y1, junction)
                    : WireSegment.nodeToNode(startNode, junction);
            circuit.addWireSegment(seg);
            return;
        }

        PointNode freeEnd = new PointNode(targetX, targetY);
        circuit.addPointNode(freeEnd);
        WireSegment seg = (startPort != null)
                ? WireSegment.portToNode(startPort, x1, y1, freeEnd)
                : WireSegment.nodeToNode(startNode, freeEnd);
        circuit.addWireSegment(seg);
    }

    // ── Hit-testing ───────────────────────────────────────────────────────────

    public Port findPortAt(double x, double y) {
        for (CircuitElement elem : circuit.getElements().values()) {
            List<Port> ports = getAllPorts(elem);
            for (Port p : ports) {
                if (Math.hypot(x - p.getAbsX(), y - p.getAbsY()) <= PORT_HIT_RADIUS) {
                    return p;
                }
            }
        }
        return null;
    }

    public PointNode findNodeAt(double x, double y) {
        for (PointNode node : circuit.getPointNodes()) {
            if (Math.hypot(x - node.getX(), y - node.getY()) <= NODE_HIT_RADIUS) {
                return node;
            }
        }
        return null;
    }

    public WireSegment findWireSegmentAt(double x, double y) {
        for (WireSegment seg : circuit.getWireSegments()) {
            if (seg.distanceToPoint(x, y) <= WIRE_HIT_THRESHOLD) {
                return seg;
            }
        }
        return null;
    }

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

    // ── Component movement — stretch attached wire segments ───────────────────

    public void onComponentMoved(CircuitElement element) {
        rerouteElementWires(element);
        notifyRender();
    }

    private void rerouteElementWires(CircuitElement element) {
        List<Port> ports = getAllPorts(element);

        for (Port p : ports) {
            double px = p.getAbsX();
            double py = p.getAbsY();

            List<WireSegment> attached = new ArrayList<>();
            for (WireSegment seg : circuit.getWireSegments()) {
                if (seg.getStartPort() == p || seg.getEndPort() == p) {
                    attached.add(seg);
                }
            }

            for (WireSegment seg : attached) {
                boolean isStart = (seg.getStartPort() == p);
                Port otherPort = isStart ? seg.getEndPort() : seg.getStartPort();
                PointNode otherNode = isStart ? seg.getEndNode() : seg.getStartNode();
                double otherX = otherPort != null ? otherPort.getAbsX()
                        : otherNode != null ? otherNode.getX() : isStart ? seg.getX2() : seg.getX1();
                double otherY = otherPort != null ? otherPort.getAbsY()
                        : otherNode != null ? otherNode.getY() : isStart ? seg.getY2() : seg.getY1();

                // If other end is a corner PointNode that connects exactly 2 segments (bend corner):
                if (otherNode != null && otherNode.getConnectedSegments().size() == 2) {
                    WireSegment otherSeg = null;
                    for (WireSegment s : otherNode.getConnectedSegments()) {
                        if (s != seg) {
                            otherSeg = s;
                            break;
                        }
                    }

                    if (otherSeg != null) {
                        if (seg.isHorizontal() && otherSeg.isVertical()) {
                            otherNode.setY(py);
                            seg.setY1(py);
                            seg.setY2(py);
                            if (isStart) seg.setX1(px);
                            else seg.setX2(px);
                            if (otherSeg.getStartNode() == otherNode) otherSeg.setY1(py);
                            else otherSeg.setY2(py);
                            continue;
                        } else if (seg.isVertical() && otherSeg.isHorizontal()) {
                            otherNode.setX(px);
                            seg.setX1(px);
                            seg.setX2(px);
                            if (isStart) seg.setY1(py);
                            else seg.setY2(py);
                            if (otherSeg.getStartNode() == otherNode) otherSeg.setX1(px);
                            else otherSeg.setX2(px);
                            continue;
                        }
                    }
                }

                // If already aligned on an axis:
                if (Math.abs(px - otherX) < 0.001 || Math.abs(py - otherY) < 0.001) {
                    if (isStart) {
                        seg.setX1(px);
                        seg.setY1(py);
                    } else {
                        seg.setX2(px);
                        seg.setY2(py);
                    }
                } else {
                    // Not aligned: create an orthogonal L-bend between (px, py) and (otherX, otherY)
                    double bendX = otherX;
                    double bendY = py;
                    PointNode bend = new PointNode(bendX, bendY);
                    circuit.addPointNode(bend);

                    if (isStart) {
                        Port oldEndPort = seg.getEndPort();
                        PointNode oldEndNode = seg.getEndNode();
                        if (oldEndNode != null) {
                            oldEndNode.removeSegment(seg);
                        }

                        seg.setX1(px);
                        seg.setY1(py);
                        seg.setX2(bendX);
                        seg.setY2(bendY);
                        seg.setEndNode(bend);
                        bend.addSegment(seg);

                        WireSegment leg2 = (oldEndPort != null)
                                ? WireSegment.nodeToPort(bend, oldEndPort, otherX, otherY)
                                : WireSegment.nodeToNode(bend, oldEndNode);
                        circuit.addWireSegment(leg2);
                    } else {
                        Port oldStartPort = seg.getStartPort();
                        PointNode oldStartNode = seg.getStartNode();
                        if (oldStartNode != null) {
                            oldStartNode.removeSegment(seg);
                        }

                        seg.setX1(bendX);
                        seg.setY1(bendY);
                        seg.setX2(px);
                        seg.setY2(py);
                        seg.setStartNode(bend);
                        bend.addSegment(seg);

                        WireSegment leg2 = (oldStartPort != null)
                                ? WireSegment.portToNode(oldStartPort, otherX, otherY, bend)
                                : WireSegment.nodeToNode(oldStartNode, bend);
                        circuit.addWireSegment(leg2);
                    }
                }
            }
        }
    }

    public boolean isDrawingWire() {
        return drawingWire;
    }
//    public double  getDragStartX()     { return dragStartX;       }
//    public double  getDragStartY()     { return dragStartY;       }
//    public boolean isHorizontalFirst() { return horizontalFirst;  }

    public HistoryManager getHistory() {
        return history;
    }

    /**
     * Records the already-applied command together with any wiring changes made by the same action.
     */
    private void recordWithWires(String label, WireChangeCommand.Image wiresBefore, Command... commands) {
        List<Command> parts = new ArrayList<>(Arrays.asList(commands));
        WireChangeCommand wires = WireChangeCommand.diff(circuit, wiresBefore, WireChangeCommand.capture(circuit));
        if (wires != null) parts.add(wires);
        if (parts.isEmpty()) return;
        history.record(parts.size() == 1 ? parts.get(0) : new CompositeCommand(label, parts));
    }

    private void commitWireGesture() {
        if (wireGestureBefore == null) return;
        WireChangeCommand.Image before = wireGestureBefore;
        wireGestureBefore = null;
        WireChangeCommand wires = WireChangeCommand.diff(circuit, before, WireChangeCommand.capture(circuit));
        if (wires == null) return;
        history.record(wires);
        notifyRender();
    }

    public void resetHistory() {
        history.clear();
    }

    public void undo() {
        if (drawingWire) return;
        commitWireGesture();
        if (history.undo()) afterHistoryStep();
    }

    public void redo() {
        if (drawingWire) return;
        commitWireGesture();
        if (history.redo()) afterHistoryStep();
    }

    private void afterHistoryStep() {
        drawingWire = false;
        dragStartPort = null;
        dragStartNode = null;
        dragStartSegment = null;
        selection.clear();
        circuit.resetSignals();
        stopSimulation();
        notifyRender();
    }

    public void notifyRender() {
        if (canvas != null) canvas.render();
    }
}
