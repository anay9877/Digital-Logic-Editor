package com.example.mini_project.validation;

import com.example.mini_project.domain.Circuit;
import com.example.mini_project.domain.CircuitElement;
import com.example.mini_project.domain.Gate;
import com.example.mini_project.domain.InputElement;
import com.example.mini_project.domain.OutputElement;
import com.example.mini_project.domain.PointNode;
import com.example.mini_project.domain.Port;
import com.example.mini_project.domain.PortType;
import com.example.mini_project.domain.SignalState;
import com.example.mini_project.domain.WireSegment;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Propagates signals through a circuit whose elements are already
 * validated and topologically sorted (e.g. SWITCH-1, SWITCH-2, AND-1, LED-1, LED-2).
 *
 * For each element, in topo order:
 *   SWITCH -> its output port already holds ON/OFF; push it along the wires.
 *   GATE   -> evaluate() from its (already filled) input ports, set the output
 *             port, push it along the wires.
 *   LED    -> its input port was filled by an upstream element; just latch it.
 *
 * "Pushing along the wires" is a DFS where the explicit stack acts as the
 * junction checkpoints: when a PointNode junction is reached, every branch is
 * pushed on the stack; we follow one branch until it hits an INPUT port
 * (gate input / LED), then pop back to the checkpoint and take the next branch.
 */
public class CircuitSimulator {

    private final List<CircuitElement> topoOrder;
    private final List<WireSegment> wireSegments;

    // Endpoint (Port or PointNode) -> wire segments attached to it.
    private final Map<Object, List<WireSegment>> attached = new IdentityHashMap<>();

    // Wires already traversed during ONE propagation (guards against loops).
    private final Set<WireSegment> visited = Collections.newSetFromMap(new IdentityHashMap<>());

    /** One pending DFS step: walk 'wire', having entered it from endpoint 'from'. */
    private record Step(WireSegment wire, Object from) {
    }

    public CircuitSimulator(Circuit circuit, List<CircuitElement> topoOrder) {
        this.topoOrder = topoOrder;
        this.wireSegments = circuit.getWireSegments();
        buildAdjacency();
        simulate();
    }

    /** Re-run the whole simulation (call again after toggling a switch). */
    public void simulate() {
        reset();

        for (CircuitElement element : topoOrder) {
            if (element instanceof InputElement input) {
                propagate(input.getOutputPort());

            } else if (element instanceof Gate gate) {
                gate.getOutputPort().setValue(gate.evaluate());
                propagate(gate.getOutputPort());

            } else if (element instanceof OutputElement led) {
                led.setValue(); // latch whatever reached its input port
            }
        }
    }

    // DFS from one output port over wires / junctions

    private void propagate(Port source) {
        SignalState signal = source.getValue();
        visited.clear();

        Deque<Step> stack = new ArrayDeque<>();
        pushBranches(source, null, stack);

        while (!stack.isEmpty()) {
            Step step = stack.pop();
            WireSegment wire = step.wire();
            if (!visited.add(wire)) continue;

            wire.setState(signal); // wire carries the signal

            Object far = farEnd(wire, step.from());

            if (far instanceof Port port) {
                // Reached a gate input / LED: deliver the signal and stop this branch.
                // (stack pop = go back to the last junction checkpoint)
                if (port.getType() == PortType.INPUT) {
                    port.setValue(signal);
                }
            } else if (far instanceof PointNode node) {
                // Plain bend -> single continuation.
                // Junction (3+ wires)   -> several branches become checkpoints.
                pushBranches(node, wire, stack);
            }
        }
    }

    /** Push every wire attached to 'endpoint' except the one we arrived by. */
    private void pushBranches(Object endpoint, WireSegment arrivedBy, Deque<Step> stack) {
        for (WireSegment wire : attached.getOrDefault(endpoint, List.of())) {
            if (wire != arrivedBy && !visited.contains(wire)) {
                stack.push(new Step(wire, endpoint));
            }
        }
    }

    // Helpers

    private static Object startOf(WireSegment w) {
        return w.getStartPort() != null ? w.getStartPort() : w.getStartNode();
    }

    private static Object endOf(WireSegment w) {
        return w.getEndPort() != null ? w.getEndPort() : w.getEndNode();
    }

    /** The endpoint of 'wire' opposite to 'from'. */
    private static Object farEnd(WireSegment wire, Object from) {
        return startOf(wire) == from ? endOf(wire) : startOf(wire);
    }

    private void buildAdjacency() {
        for (WireSegment w : wireSegments) {
            Object a = startOf(w);
            Object b = endOf(w);
            if (a != null) attached.computeIfAbsent(a, k -> new ArrayList<>()).add(w);
            if (b != null) attached.computeIfAbsent(b, k -> new ArrayList<>()).add(w);
        }
    }

    /** Clear everything derived from a previous run; switches keep their own value. */
    private void reset() {
        for (WireSegment w : wireSegments) {
            w.setState(SignalState.UNKNOWN);
        }
        for (CircuitElement element : topoOrder) {
            if (element instanceof InputElement input) {
                input.getOutputPort().setValue(input.getValue());
            } else if (element instanceof Gate gate) {
                for (Port p : gate.getAllPorts()) p.setValue(SignalState.UNKNOWN);
            } else if (element instanceof OutputElement led) {
                led.getInputPort().setValue(SignalState.UNKNOWN);
                led.setValue();
            }
        }
    }
}