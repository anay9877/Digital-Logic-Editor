package com.example.mini_project.controller;

import com.example.mini_project.domain.*;
import com.example.mini_project.gui.CircuitCanvas;
import com.example.mini_project.gui.ToolbarOptions;
import com.example.mini_project.gui.TruthTableWindow;
import com.example.mini_project.validation.*;
import com.example.mini_project.controller.command.*;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
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
    
    private boolean liveMoving =  false;

    private CompiledCircuit compiled;
    private final Map<Long, TruthTableData> tableCache = new HashMap<>();
    private boolean simulating = false;
    private boolean isValidated = false;
    //prajw : start
    private Consumer<List<String>> onValidationErrors;
    private final Set<String> invalidElementIds = new HashSet<>();
    private final Set<String> invalidWireIds = new HashSet<>();
    // highlight validation errored elems
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

    public Set<String> getInvalidElementIds() {
        return Set.copyOf(invalidElementIds);
    }

    public Set<String> getInvalidWireIds() {
        return Set.copyOf(invalidWireIds);
    }

    public void clearInvalidHighlights() {
        invalidElementIds.clear();
        invalidWireIds.clear();
        notifyRender();
    }

    //highlight validation errored elems
    private void markInvalidIds(Collection<String> elementIds, Collection<String> wireIds) {
        invalidElementIds.clear();
        invalidWireIds.clear();
        invalidElementIds.addAll(elementIds);
        invalidWireIds.addAll(wireIds);
        notifyRender();
    }

    //highlight validation errored elems
    private void markInvalidObjects(Object... targets) {
        invalidElementIds.clear();
        invalidWireIds.clear();
        for (Object target : targets) {
            if (target instanceof Port port) {
                invalidElementIds.add(port.getOwner().getId());
            } else if (target instanceof CircuitElement element) {
                invalidElementIds.add(element.getId());
            } else if (target instanceof WireSegment wire) {
                invalidWireIds.add(wire.getId());
            } else if (target instanceof PointNode node) {
                for (WireSegment wire : node.getConnectedSegments()) {
                    invalidWireIds.add(wire.getId());
                }
            }
        }
        notifyRender();
    }
    //prajw: end

    private double getElementSize(CircuitElement element) {
        if (element instanceof Gate) return 112;
        if (element instanceof InputElement) return 88;
        return 64;
    }

    private boolean isOverlapping(CircuitElement element1, double x1, double y1, 
                                CircuitElement element2, double x2, double y2) {
        double element1Size = getElementSize(element1);
        double element2Size = getElementSize(element2);
        return (x1 < x2 + element2Size) && (x1 + element1Size > x2) &&
                (y1 < y2 + element2Size) && (y1 + element1Size > y2);
    }

    private boolean isOverlappingExistingElement(CircuitElement newElement) {
        for (CircuitElement element: circuit.getElements().values()) {
            if (isOverlapping(element, element.getX(), element.getY(), newElement, newElement.getX(), newElement.getY())) {
                return true;
            }
        }
        return false;
    }
    // Used for deletion of entire circuit
    public void newCircuit() {
        circuit.clear();
        counters.clear();
        selection.clear();
        PointNode.resetId();
        WireSegment.resetId();
        drawingWire = false;
        clearInvalidHighlights();
        dragStartPort = null;
        dragStartNode = null;
        dragStartSegment = null;
        wireGestureBefore = null;
        history.clear();
        if (canvas != null && canvas.getViewportManager() != null) {
            canvas.getViewportManager().resetView();
        }
        compiled = null;
        tableCache.clear();
        stopSimulation();
        notifyRender();
    }

    // Used for generation of ids
    public void updateCounter(ComponentType type, int count) {
        counters.put(type, Math.max(counters.getOrDefault(type, 0), count));
    }

    public void onElementPlaced(ComponentType type, double x, double y) {
        double newX = CircuitCanvas.snap(x);
        double newY = CircuitCanvas.snap(y);

        int n = counters.getOrDefault(type, 0) + 1;
        String id = type.name() + "-" + n;
        CircuitElement newElement = type.create(id, newX, newY);
        if (isOverlappingExistingElement(newElement)) {
            LocalTime time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
            System.err.println(time + " - Component overlapping, cannot place it there.");
            return;
        }
        counters.put(type, n);
        CircuitElement element = newElement;
        circuit.addElement(element);
        stopSimulation();
        history.record(new PlaceElementCommand(circuit, element));
        selection.clear();
    }

    public void toggleInput(InputElement input) {
        SignalState before = input.getValue();
        input.toggle();
        if (compiled != null && compiled.matches(circuit)) compiled.update(input);
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

    /*
    public List<CircuitElement> validateAndGetOrder(Circuit targetCircuit) {
        CircuitValidator validator = new CircuitValidator();
        boolean wiresValid = validator.validateDanglingWires(targetCircuit);
        boolean inputsValid = validator.validateInputPorts(targetCircuit);
        boolean ioValid = validator.validateInputsAndOutputs(targetCircuit);
        boolean logicValid = validator.validateDeadLogic(targetCircuit);
        boolean referencesValid = validator.validateWireReferences(targetCircuit);
        boolean connectionsValid = validator.validateWireDrivers(targetCircuit);
        if (!(wiresValid && inputsValid && ioValid && logicValid && referencesValid && connectionsValid)) {
            //prajw : start
            List<String> errors = validator.getErrors();
            markInvalidIds(validator.getInvalidElementIds(), validator.getInvalidWireIds());
            publishValidationErrors(errors);
            throw new IllegalStateException(String.join(System.lineSeparator(), errors));
            //prajw: end
        }
        isValidated = true;
        clearInvalidHighlights();
        //prajw : start
        CircuitTopology topology = new CircuitTopology();
        try {
            return topology.sort(targetCircuit);
        } catch (IllegalStateException ex) {
            List<String> errors = List.of(ex.getMessage());
            markInvalidIds(topology.getInvalidElementIds(), topology.getInvalidWireIds());
            publishValidationErrors(errors);
            throw ex;
        }
        //prajw: end
    }
    */

    public List<CircuitElement> validateAndGetOrder(Circuit c) { return validateAndGetOrder(c, true); }

    private List<CircuitElement> validateAndGetOrder(Circuit c, boolean report) {
        CircuitValidator validator = new CircuitValidator();
        boolean ok = validator.validateDanglingWires(c);
        ok &= validator.validateInputPorts(c);
        ok &= validator.validateInputsAndOutputs(c);
        ok &= validator.validateDeadLogic(c);
        ok &= validator.validateWireReferences(c);
        ok &= validator.validateWireDrivers(c);
        if (!ok) {
            List<String> errors = validator.getErrors();
            if (report) {
                markInvalidIds(validator.getInvalidElementIds(), validator.getInvalidWireIds());
                publishValidationErrors(errors);
            }
            throw new IllegalStateException(String.join(System.lineSeparator(), errors));
        }
        CircuitTopology topology = new CircuitTopology();
        try {
            List<CircuitElement> order = topology.sort(c);
            isValidated = true;                       // moved here (see #5)
            if (report) clearInvalidHighlights();
            return order;
        } catch (IllegalStateException ex) {
            if (report) {
                markInvalidIds(topology.getInvalidElementIds(), topology.getInvalidWireIds());
                publishValidationErrors(List.of(ex.getMessage()));
            }
            throw ex;
        }
    }

    //prajw : start
    private void publishValidationErrors(List<String> errors) {
        if (onValidationErrors != null && !errors.isEmpty()) {
            onValidationErrors.accept(List.copyOf(errors));
        }
    }
    //prajw: end

    //prajw : start
    /** Publishes one non-validation error, such as a rejected wire connection. No highlighting. */
    private void publishError(String error) {
        publishValidationErrors(List.of(error));
    }
    //prajw: end

    public void runSimulation() {
        try {
            Circuit target = getCircuitToProcess();
            if (target == circuit) ensureCompiled();
            else CompiledCircuit.compile(target, validateAndGetOrder(target)).evaluateAll();
            simulating = true;
            notifyRender();
        } catch (IllegalStateException ex) {
            System.err.println("Cannot simulate: " + ex.getMessage());
            simulating = false;
        }
    }

    private void ensureCompiled() {
        if (compiled != null && compiled.matches(circuit)) return;   // nothing structural changed
        compiled = null;
        compiled = CompiledCircuit.compile(circuit, validateAndGetOrder(circuit));
        compiled.evaluateAll();
    }

    public void showTruthTable() {
        if (canvas == null) return;

        List<SubCircuitTable> tables = buildSubCircuitTables();
        if (tables.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "The canvas is empty.", ButtonType.OK).showAndWait();
            return;
        }

        // keep the canvas live (switch toggles update LEDs) when the whole canvas is valid
        try {
            ensureCompiled();
            simulating = true;
        } catch (IllegalStateException ex) {
            stopSimulation();      // some circuit is unfinished; press Simulate once it is complete
        }
        notifyRender();

        Window owner = canvas.getScene() != null ? canvas.getScene().getWindow() : null;
        TruthTableWindow.show(owner, tables, this::highlightSubCircuit);
    }

    /** One table per connected piece of the canvas, numbered top-left to bottom-right. */
    private List<SubCircuitTable> buildSubCircuitTables() {
        List<SubCircuit> subs = new ArrayList<>(circuit.splitIntoDisjointSubCircuits());
        subs.sort(Comparator
                .comparingDouble((SubCircuit s) -> elementsOf(s).stream().mapToDouble(CircuitElement::getY).min().orElse(0))
                .thenComparingDouble(s -> elementsOf(s).stream().mapToDouble(CircuitElement::getX).min().orElse(0)));

        List<SubCircuitTable> result = new ArrayList<>();
        Map<Long, TruthTableData> newCache = new HashMap<>();
        int n = 0;
        for (SubCircuit sub : subs) {
            n++;
            Circuit part = circuitOf(sub);
            List<String> elementIds = elementsOf(sub).stream().map(CircuitElement::getId).toList();
            List<String> wireIds = sub.getWireSegments().stream().map(WireSegment::getId).toList();

            long key = CompiledCircuit.fingerprint(part);
            TruthTableData data = tableCache.get(key);
            String error = null;
            if (data != null) {
                data = data.withCurrentRow(TruthTableGenerator.currentRow(part));   // unchanged circuit: reuse
            } else {
                try {
                    data = TruthTableGenerator.generate(part, validateAndGetOrder(part));
                } catch (IllegalStateException ex) {
                    error = ex.getMessage();
                }
            }
            if (data != null) newCache.put(key, data);
            result.add(new SubCircuitTable("Circuit " + n, sub.getInputs().size(), sub.getOutputs().size(),
                    elementIds, wireIds, data, error));
        }
        tableCache.clear();
        tableCache.putAll(newCache);
        return result;
    }

    private static List<CircuitElement> elementsOf(SubCircuit sub) {
        List<CircuitElement> all = new ArrayList<>(sub.getgates().values());
        all.addAll(sub.getInputs());
        all.addAll(sub.getOutputs());
        return all;
    }

    private static Circuit circuitOf(SubCircuit sub) {
        Circuit part = new Circuit();
        elementsOf(sub).forEach(part::addElement);
        sub.getPointNodes().forEach(part::addPointNode);
        sub.getWireSegments().forEach(part::addWireSegment);
        return part;
    }

    /** Called by the truth-table window: show which circuit is being viewed (empty lists = clear). */
    private void highlightSubCircuit(List<String> elementIds, List<String> wireIds) {
        selection.clear();
        selection.selectElementsAndWires(elementIds, wireIds, true);
        notifyRender();
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
    }

    private void handleValidate() {
        try {
            Circuit targeCircuit = getCircuitToProcess();
            List<CircuitElement> order = validateAndGetOrder(targeCircuit);
            System.out.println("Circuit validation passed.");
            System.out.println("Simulation order:");
            for (CircuitElement element : order) {
                System.out.println(element.getId());
            }
        } catch (IllegalStateException ex) {
            System.err.println(ex.getMessage());
        }
    }

    public void validateCircuit() {
        handleValidate();
    }

    public void beginLiveMove() {
        liveMoveOrigins.clear();
        liveMoveWiresBefore = WireChangeCommand.capture(circuit);
        liveMoving = true;
    }

    // Mouse released: final pose, one full redraw, one undo step
    public void endLiveMove(Collection<String> ids, double dx, double dy) {
        if (!liveMoving) return;
        liveMove(ids, dx, dy);
        liveMoving = false;

        List<MoveElementsCommand.Move> moves = new ArrayList<>();
        for (MoveOrigin origin : liveMoveOrigins.values()) {
            CircuitElement element = origin.element();
            if (element.getX() != origin.x() || element.getY() != origin.y()) {
                moves.add(new MoveElementsCommand.Move(
                        element, origin.x(), origin.y(), element.getX(), element.getY()));
            }
        }

        // Reject the move if a moved element ended up on top of a non-moved one
        boolean overlaps = false;
        for (MoveOrigin origin : liveMoveOrigins.values()) {
            CircuitElement m = origin.element();
            for (CircuitElement other : circuit.getElements().values()) {
                if (other == m || ids.contains(other.getId())) continue;
                if (isOverlapping(m, m.getX(), m.getY(), other, other.getX(), other.getY())) {
                    overlaps = true;
                    break;
                }
            }
            if (overlaps) break;
        }
        if (overlaps) {
            if (!moves.isEmpty()) new MoveElementsCommand(moves).undo();   // gates back
            WireChangeCommand back = WireChangeCommand.diff(
                    circuit, liveMoveWiresBefore, WireChangeCommand.capture(circuit));
            if (back != null) back.undo();                                  // wires back
            liveMoveWiresBefore = null;
            liveMoveOrigins.clear();
            stopSimulation();
            notifyRender();
            LocalTime time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
            System.err.println(time + " - Move failed: Components cannot overlap.");
            return;
        }

        WireChangeCommand.Image wiresBefore = liveMoveWiresBefore;
        liveMoveWiresBefore = null;
        liveMoveOrigins.clear();
        stopSimulation();
        notifyRender();
        if (wiresBefore != null) {
            if (moves.isEmpty()) {
                recordWithWires("Move elements", wiresBefore);
            } else {
                recordWithWires("Move elements", wiresBefore, new MoveElementsCommand(moves));
            }
        }
    }



    public void liveMove(Collection<String> ids, double dx, double dy) {
        if (!liveMoving) return;

        List<CircuitElement> movedElements = new ArrayList<>();
        for (String id : ids) {
            CircuitElement e = circuit.getElement(id);
            if (e != null) {
                MoveOrigin origin = liveMoveOrigins.computeIfAbsent(
                        id, ignored -> new MoveOrigin(e, e.getX(), e.getY()));
                double newX = CircuitCanvas.snap(origin.x() + dx);
                double newY = CircuitCanvas.snap(origin.y() + dy);
                if (newX == e.getX() && newY == e.getY()) continue;
                e.setPosition(newX, newY);
                movedElements.add(e);
            }
        }
        if (!movedElements.isEmpty()) {
            //prajw : start
            moveJunctionsWithMovedPorts(movedElements);
            //prajw: end
            for (CircuitElement element : movedElements) rerouteElementWires(element);
            stopSimulation();
            if (canvas != null) canvas.renderWiresOnly();
        }
    }
    private Circuit getCircuitToProcess() {
        Set<String> selectedElements = selection.getSelectedElements();
        Set<String> selectedWires = selection.getSelectedWires();
        if (selectedElements.isEmpty() && selectedWires.isEmpty())
            return circuit;             // validating/simulating entire circuit if nothing selected

        Circuit selectedCircuit = new Circuit();
        for (SubCircuit subCircuit : circuit.splitIntoDisjointSubCircuits()) {
            boolean selected = false;

            // check if selected subcircuit also contains a gate
            for (String id : subCircuit.getgates().keySet()) {
                if (selectedElements.contains(id)) {
                    selected = true;
                    break;
                }
            }
            // Check if switch selected
            for (InputElement input : subCircuit.getInputs()) {
                if (selectedElements.contains(input.getId())) {
                    selected = true;
                    break;
                }
            }
            // check if LEDs selected
            for (OutputElement led : subCircuit.getOutputs()) {
                if (selectedElements.contains(led.getId())) {
                    selected = true;
                    break;
                }
            }
            // check if wires selected
            for (WireSegment wire : subCircuit.getWireSegments()) {
                if (selectedWires.contains(wire.getId())) {
                    selected = true;
                    break;
                }
            }
            if (!selected) continue;
            for (Gate gate: subCircuit.getgates().values()) {
                selectedCircuit.addElement(gate);
            }
            for (InputElement input: subCircuit.getInputs()) {
                selectedCircuit.addElement(input);
            }
            for (OutputElement output: subCircuit.getOutputs()) {
                selectedCircuit.addElement(output);
            }
            for (PointNode node: subCircuit.getPointNodes()) {
                selectedCircuit.addPointNode(node);
            }
            for (WireSegment wire: subCircuit.getWireSegments()) {
                selectedCircuit.addWireSegment(wire);
            }
        }
        if (selectedCircuit.getElements().isEmpty()) {
            throw new IllegalStateException("No subcircuit found for the selection");
        }
        return selectedCircuit;
    }
    // Wire drawing — called by CircuitCanvas mouse handlers
    private static final double PORT_HIT_RADIUS = 8.0;
    private static final double NODE_HIT_RADIUS = 8.0;
    private static final double WIRE_HIT_THRESHOLD = 8.0;
    private static final double DIRECTION_LOCK_DIST = CircuitCanvas.GRID_SIZE;
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
        Object start = dragStartPort != null ? dragStartPort
                : dragStartNode != null ? dragStartNode : dragStartSegment;
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
            LocalTime time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
            //prajw : start
            String error = time + " - Connection Rejected: Wire cannot end without a node/port.";
            System.err.println(error);
            publishError(error);
            //prajw: end
            dragStartPort = null;
            dragStartNode = null;
            dragStartSegment = null;
            return;
        }
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
            LocalTime time = LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
            String message = time + " - Connection Rejected: " + error;
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

    //prajw : start
    private void moveJunctionsWithMovedPorts(List<CircuitElement> movedElements) {
        List<String> movedElementIds = new ArrayList<>();
        for (CircuitElement element : movedElements) {
            movedElementIds.add(element.getId());
        }

        for (PointNode junction : new ArrayList<>(circuit.getPointNodes())) {
            if (!junction.isJunction()) continue;

            List<Port> movedPorts = new ArrayList<>();
            double deltaX = 0;
            double deltaY = 0;
            boolean hasDelta = false;
            boolean consistentDelta = true;

            for (WireSegment segment : junction.getConnectedSegments()) {
                Port port = segment.getStartNode() == junction
                        ? segment.getEndPort()
                        : segment.getStartPort();
                if (port == null || !movedElementIds.contains(port.getOwner().getId())
                        || movedPorts.contains(port)) {
                    continue;
                }

                double oldX = segment.getStartPort() == port ? segment.getX1() : segment.getX2();
                double oldY = segment.getStartPort() == port ? segment.getY1() : segment.getY2();
                double portDeltaX = port.getAbsX() - oldX;
                double portDeltaY = port.getAbsY() - oldY;
                if (!hasDelta) {
                    deltaX = portDeltaX;
                    deltaY = portDeltaY;
                    hasDelta = true;
                } else if (Math.abs(deltaX - portDeltaX) >= 0.001
                        || Math.abs(deltaY - portDeltaY) >= 0.001) {
                    consistentDelta = false;
                    break;
                }
                movedPorts.add(port);
            }

            if (consistentDelta && movedPorts.size() >= 2
                    && (Math.abs(deltaX) >= 0.001 || Math.abs(deltaY) >= 0.001)) {
                movePointNodeAndReroute(junction, junction.getX() + deltaX, junction.getY() + deltaY);
            }
        }
    }

    private void movePointNodeAndReroute(PointNode node, double x, double y) {
        node.setX(x);
        node.setY(y);

        for (WireSegment segment : new ArrayList<>(node.getConnectedSegments())) {
            boolean nodeAtStart = segment.getStartNode() == node;
            Port farPort = nodeAtStart ? segment.getEndPort() : segment.getStartPort();
            PointNode farNode = nodeAtStart ? segment.getEndNode() : segment.getStartNode();
            double farX = farPort != null ? farPort.getAbsX() : farNode.getX();
            double farY = farPort != null ? farPort.getAbsY() : farNode.getY();

            if (Math.abs(x - farX) < 0.001 || Math.abs(y - farY) < 0.001) {
                if (nodeAtStart) {
                    segment.setX1(x);
                    segment.setY1(y);
                    segment.setX2(farX);
                    segment.setY2(farY);
                } else {
                    segment.setX1(farX);
                    segment.setY1(farY);
                    segment.setX2(x);
                    segment.setY2(y);
                }
                continue;
            }

            PointNode bend = new PointNode(farX, y);
            circuit.addPointNode(bend);
            if (farNode != null) {
                farNode.removeSegment(segment);
            }

            if (nodeAtStart) {
                segment.setX1(x);
                segment.setY1(y);
                segment.setX2(farX);
                segment.setY2(y);
                segment.setEndNode(bend);
                bend.addSegment(segment);

                WireSegment remainingLeg = farPort != null
                        ? WireSegment.nodeToPort(bend, farPort, farX, farY)
                        : WireSegment.nodeToNode(bend, farNode);
                circuit.addWireSegment(remainingLeg);
            } else {
                segment.setX1(farX);
                segment.setY1(y);
                segment.setX2(x);
                segment.setY2(y);
                segment.setStartNode(bend);
                bend.addSegment(segment);

                WireSegment remainingLeg = farPort != null
                        ? WireSegment.portToNode(farPort, farX, farY, bend)
                        : WireSegment.nodeToNode(farNode, bend);
                circuit.addWireSegment(remainingLeg);
            }
        }
    }
    //prajw: end

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
        compiled = null;
        stopSimulation();
        notifyRender();
    }

    public void notifyRender() {
        if (canvas != null) canvas.render();
    }
}