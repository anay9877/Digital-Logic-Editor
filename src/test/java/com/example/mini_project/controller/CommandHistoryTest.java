package com.example.mini_project.controller;

import com.example.mini_project.controller.command.WireChangeCommand;
import com.example.mini_project.domain.Circuit;
import com.example.mini_project.domain.ComponentType;
import com.example.mini_project.domain.InputElement;
import com.example.mini_project.domain.OutputElement;
import com.example.mini_project.domain.PointNode;
import com.example.mini_project.domain.SignalState;
import com.example.mini_project.domain.WireSegment;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CommandHistoryTest {

    @Test
    void undoAndRedoElementPlacement() {
        Circuit circuit = new Circuit();
        EditorController controller = new EditorController(circuit);

        controller.onElementPlaced(ComponentType.AND, 23, 37);
        var placed = circuit.getElement("AND-1");

        assertNotNull(placed);
        assertEquals(20, placed.getX());
        assertEquals(40, placed.getY());
        controller.undo();
        assertNull(circuit.getElement("AND-1"));
        controller.redo();
        assertSame(placed, circuit.getElement("AND-1"));
    }

    @Test
    void undoAndRedoInputTogglePreservesTheInputValue() {
        Circuit circuit = new Circuit();
        InputElement input = (InputElement) ComponentType.SWITCH.create("SWITCH-1", 0, 0);
        circuit.addElement(input);
        EditorController controller = new EditorController(circuit);

        controller.toggleInput(input);
        assertEquals(SignalState.ON, input.getValue());
        controller.undo();
        assertEquals(SignalState.OFF, input.getValue());
        controller.redo();
        assertEquals(SignalState.ON, input.getValue());
    }

    @Test
    void newActionAfterUndoDiscardsTheRedoBranch() {
        Circuit circuit = new Circuit();
        EditorController controller = new EditorController(circuit);

        controller.onElementPlaced(ComponentType.AND, 0, 0);
        controller.undo();
        assertTrue(controller.getHistory().canRedo());

        controller.onElementPlaced(ComponentType.OR, 40, 0);
        assertFalse(controller.getHistory().canRedo());
        controller.redo();
        assertNull(circuit.getElement("AND-1"));
        assertNotNull(circuit.getElement("OR-1"));
    }

    @Test
    void undoAndRedoWireAdditionRestoresNodeConnections() {
        Circuit circuit = new Circuit();
        WireChangeCommand.Image before = WireChangeCommand.capture(circuit);
        PointNode start = new PointNode(0, 0);
        PointNode end = new PointNode(40, 0);
        circuit.addPointNode(start);
        circuit.addPointNode(end);
        WireSegment segment = WireSegment.nodeToNode(start, end);
        circuit.addWireSegment(segment);

        WireChangeCommand command = WireChangeCommand.diff(
                circuit, before, WireChangeCommand.capture(circuit));
        HistoryManager history = new HistoryManager();
        history.record(command);

        assertTrue(history.undo());
        assertTrue(circuit.getPointNodes().isEmpty());
        assertTrue(circuit.getWireSegments().isEmpty());

        assertTrue(history.redo());
        assertEquals(1, circuit.getWireSegments().size());
        assertTrue(circuit.getWireSegments().contains(segment));
        assertSame(segment, start.getConnectedSegments().getFirst());
        assertSame(segment, end.getConnectedSegments().getFirst());
    }

    @Test
    void undoAndRedoWireDeletionRestoresMembershipAndConnections() {
        Circuit circuit = new Circuit();
        PointNode start = new PointNode(0, 0);
        PointNode end = new PointNode(40, 0);
        circuit.addPointNode(start);
        circuit.addPointNode(end);
        WireSegment segment = WireSegment.nodeToNode(start, end);
        circuit.addWireSegment(segment);
        WireChangeCommand.Image before = WireChangeCommand.capture(circuit);

        circuit.removeWireSegments(Set.of(segment.getId()));
        WireChangeCommand command = WireChangeCommand.diff(
                circuit, before, WireChangeCommand.capture(circuit));
        HistoryManager history = new HistoryManager();
        history.record(command);

        assertTrue(history.undo());
        assertEquals(1, circuit.getWireSegments().size());
        assertEquals(2, circuit.getPointNodes().size());
        assertTrue(start.getConnectedSegments().contains(segment));
        assertTrue(end.getConnectedSegments().contains(segment));

        assertTrue(history.redo());
        assertTrue(circuit.getWireSegments().isEmpty());
        assertTrue(circuit.getPointNodes().isEmpty());
    }

    @Test
    void undoAndRedoElementDeletionRestoresItsWires() {
        Circuit circuit = new Circuit();
        InputElement input = (InputElement) ComponentType.SWITCH.create("SWITCH-1", 0, 0);
        OutputElement led = (OutputElement) ComponentType.LED.create("LED-1", 160, 0);
        circuit.addElement(input);
        circuit.addElement(led);
        WireSegment segment = WireSegment.portToPort(
                input.getOutputPort(), input.getOutputPort().getAbsX(), input.getOutputPort().getAbsY(),
                led.getInputPort(), led.getInputPort().getAbsX(), led.getInputPort().getAbsY());
        circuit.addWireSegment(segment);
        EditorController controller = new EditorController(circuit);
        controller.getSelection().selectElement(input.getId(), false);

        controller.deleteSelected();
        assertNull(circuit.getElement(input.getId()));
        assertTrue(circuit.getWireSegments().isEmpty());

        assertTrue(controller.getHistory().undo());
        assertSame(input, circuit.getElement(input.getId()));
        assertSame(led, circuit.getElement(led.getId()));
        assertTrue(circuit.getWireSegments().contains(segment));

        assertTrue(controller.getHistory().redo());
        assertNull(circuit.getElement(input.getId()));
        assertTrue(circuit.getWireSegments().isEmpty());
    }

    @Test
    void undoAndRedoElementMovementRestoresItsWireGeometry() {
        Circuit circuit = new Circuit();
        InputElement input = (InputElement) ComponentType.SWITCH.create("SWITCH-1", 0, 0);
        OutputElement led = (OutputElement) ComponentType.LED.create("LED-1", 160, 0);
        circuit.addElement(input);
        circuit.addElement(led);
        WireSegment segment = WireSegment.portToPort(
                input.getOutputPort(), input.getOutputPort().getAbsX(), input.getOutputPort().getAbsY(),
                led.getInputPort(), led.getInputPort().getAbsX(), led.getInputPort().getAbsY());
        circuit.addWireSegment(segment);
        EditorController controller = new EditorController(circuit);

        controller.moveElements(List.of(input.getId()), 0, 40);
        assertEquals(40, input.getY());
        assertEquals(2, circuit.getWireSegments().size());

        assertTrue(controller.getHistory().undo());
        assertEquals(0, input.getY());
        assertEquals(1, circuit.getWireSegments().size());
        assertSame(segment, circuit.getWireSegments().getFirst());

        assertTrue(controller.getHistory().redo());
        assertEquals(40, input.getY());
        assertEquals(2, circuit.getWireSegments().size());
    }
}
