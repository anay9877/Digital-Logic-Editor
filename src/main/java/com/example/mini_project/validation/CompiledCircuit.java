package com.example.mini_project.validation;

import com.example.mini_project.domain.*;

import java.util.*;

/**
 * A validated circuit "compiled" once into nets (wire groups) and nodes (switch / gate / LED)
 * in topological order. Compiling is the expensive part; after that:
 *
 *  - update(switch)  event-driven: only the part of the circuit downstream of the toggled switch is
 *                    re-evaluated, and propagation stops at any gate whose output did not change.
 *                    Other sub-circuits are never touched.
 *  - evaluateAll()   one linear pass over every node (used for the first run).
 *  - truthTable()    bit-parallel: every signal is a bit-set over all input rows, so a gate is
 *                    evaluated for 64 rows with ONE machine instruction. Does not touch switch values.
 *
 * Moving components does not change connectivity, so the compiled form stays valid; check with matches().
 */
public final class CompiledCircuit {

    private static final class Net {
        final List<Port> sinks = new ArrayList<>();
        final List<WireSegment> wires = new ArrayList<>();
        final List<Node> sinkNodes = new ArrayList<>();
    }

    private static final class Node {
        final CircuitElement element;
        final int index;                 // position in topological order
        Net[] in = new Net[0];
        Net out;
        Node(CircuitElement element, int index) { this.element = element; this.index = index; }
    }

    private static final long[] PATTERN = {            // bit i of the row number, for rows 0..63
            0xAAAAAAAAAAAAAAAAL, 0xCCCCCCCCCCCCCCCCL, 0xF0F0F0F0F0F0F0F0L,
            0xFF00FF00FF00FF00L, 0xFFFF0000FFFF0000L, 0xFFFFFFFF00000000L};

    private final Node[] nodes;
    private final Map<CircuitElement, Node> nodeOf = new IdentityHashMap<>();
    private final long fingerprint;
    private Node[] queue;                // scratch heap for update()

    private CompiledCircuit(Circuit circuit, List<CircuitElement> topoOrder) {
        nodes = new Node[topoOrder.size()];
        for (int i = 0; i < nodes.length; i++) {
            Node n = new Node(topoOrder.get(i), i);
            if (n.element instanceof Gate g) n.in = new Net[g.getInputPorts().size()];
            else if (n.element instanceof OutputElement) n.in = new Net[1];
            nodes[i] = n;
            nodeOf.put(n.element, n);
        }

        // union-find over wire endpoints (Port or PointNode) -> one Net per electrical group
        Map<Object, Object> parent = new IdentityHashMap<>();
        for (WireSegment w : circuit.getWireSegments()) union(parent, startOf(w), endOf(w));

        Map<Object, Net> netOf = new IdentityHashMap<>();
        Set<Port> seenPorts = Collections.newSetFromMap(new IdentityHashMap<>());
        for (WireSegment w : circuit.getWireSegments()) {
            Net net = netOf.computeIfAbsent(find(parent, startOf(w)), k -> new Net());
            net.wires.add(w);
            for (Object end : new Object[]{startOf(w), endOf(w)}) {
                if (!(end instanceof Port p) || !seenPorts.add(p)) continue;
                Node owner = nodeOf.get(p.getOwner());
                if (owner == null) continue;
                if (p.getType() == PortType.OUTPUT) {
                    owner.out = net;
                } else {
                    int idx = p.getOwner() instanceof Gate g ? g.getInputPorts().indexOf(p) : 0;
                    if (idx >= 0 && idx < owner.in.length) owner.in[idx] = net;
                    net.sinks.add(p);
                    net.sinkNodes.add(owner);
                }
            }
        }
        fingerprint = fingerprint(circuit);
    }

    /** @param topoOrder the order returned by CircuitTopology.sort for an already validated circuit */
    public static CompiledCircuit compile(Circuit circuit, List<CircuitElement> topoOrder) {
        return new CompiledCircuit(circuit, topoOrder);
    }

    /** True while the circuit's elements / wires / connections are the ones this was compiled from. */
    public boolean matches(Circuit circuit) {
        return fingerprint == fingerprint(circuit);
    }

    // Live simulation

    public void evaluateAll() {
        for (Node n : nodes) process(n, true, null);
    }

    /** Call after a switch changed value. */
    public void update(InputElement changed) {
        Node start = nodeOf.get(changed);
        if (start == null) return;
        PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingInt((Node n) -> n.index));
        Set<Node> queued = Collections.newSetFromMap(new IdentityHashMap<>());
        pq.add(start);
        queued.add(start);
        Node n;
        while ((n = pq.poll()) != null) {
            queued.remove(n);
            process(n, n == start, node -> {
                if (queued.add(node)) pq.add(node);
            });
        }
    }

    private void process(Node node, boolean force, java.util.function.Consumer<Node> enqueue) {
        CircuitElement el = node.element;
        SignalState out;
        Port outPort;
        if (el instanceof InputElement in) {
            out = in.getValue();
            outPort = in.getOutputPort();
        } else if (el instanceof Gate g) {
            out = g.evaluate();
            outPort = g.getOutputPort();
        } else if (el instanceof OutputElement led) {
            led.setValue();              // latch what reached its input port
            return;
        } else {
            return;
        }
        if (!force && outPort.getValue() == out) return;   // unchanged -> nothing downstream changes
        outPort.setValue(out);
        Net net = node.out;
        if (net == null) return;
        for (WireSegment w : net.wires) w.setState(out);
        for (Port p : net.sinks) p.setValue(out);
        if (enqueue != null) net.sinkNodes.forEach(enqueue);
    }

    // Bit-parallel truth table

    public TruthTableData truthTable() {
        List<InputElement> inputs = new ArrayList<>();
        List<OutputElement> outputs = new ArrayList<>();
        for (Node n : nodes) {
            if (n.element instanceof InputElement i) inputs.add(i);
            else if (n.element instanceof OutputElement o) outputs.add(o);
        }
        Comparator<CircuitElement> byId = Comparator.comparing(CircuitElement::getId, TruthTableGenerator::naturalCompare);
        inputs.sort(byId);
        outputs.sort(byId);

        if (inputs.isEmpty())
            throw new IllegalStateException("Truth table unavailable: no input switches were found in the circuit.");
        if (outputs.isEmpty())
            throw new IllegalStateException("Truth table unavailable: no output LEDs were found in the circuit.");
        if (inputs.size() > TruthTableGenerator.MAX_INPUTS)
            throw new IllegalStateException("Truth table unavailable: " + inputs.size() + " switches would need "
                    + (1L << inputs.size()) + " rows (limit is " + TruthTableGenerator.MAX_INPUTS + " switches).");

        int n = inputs.size(), rows = 1 << n, words = Math.max(1, rows >>> 6);
        long[] zeros = new long[words];
        Map<Net, long[]> val = new IdentityHashMap<>();

        for (int i = 0; i < n; i++) {
            Net net = nodeOf.get(inputs.get(i)).out;
            if (net == null) continue;
            long[] w = new long[words];
            for (int k = 0; k < words; k++) w[k] = i < 6 ? PATTERN[i] : (((k >>> (i - 6)) & 1) == 1 ? -1L : 0L);
            val.put(net, w);
        }
        for (Node node : nodes) {
            if (!(node.element instanceof Gate g)) continue;
            long[][] a = new long[node.in.length][];
            for (int j = 0; j < a.length; j++) a[j] = node.in[j] == null ? zeros : val.getOrDefault(node.in[j], zeros);
            long[] r = new long[words];
            ComponentType t = g.getType();
            for (int k = 0; k < words; k++) {
                long x = a[0][k];
                for (int j = 1; j < a.length; j++) {
                    long y = a[j][k];
                    x = switch (t) {
                        case AND, NAND -> x & y;
                        case OR, NOR -> x | y;
                        case XOR -> x ^ y;
                        default -> throw new IllegalStateException("Unsupported gate type: " + t);
                    };
                }
                r[k] = (t == ComponentType.NAND || t == ComponentType.NOR || t == ComponentType.NOT) ? ~x : x;
            }
            if (node.out != null) val.put(node.out, r);
        }

        long[][] outWords = new long[outputs.size()][];
        for (int o = 0; o < outWords.length; o++) {
            Net net = nodeOf.get(outputs.get(o)).in[0];
            outWords[o] = net == null ? zeros : val.getOrDefault(net, zeros);
        }

        List<TruthTableData.Row> result = new ArrayList<>(rows);
        for (int r = 0; r < rows; r++) {
            boolean[] in = new boolean[n];
            for (int b = 0; b < n; b++) in[b] = ((r >>> b) & 1) == 1;
            boolean[] out = new boolean[outWords.length];
            for (int o = 0; o < out.length; o++) out[o] = ((outWords[o][r >>> 6] >>> (r & 63)) & 1L) == 1L;
            result.add(new TruthTableData.Row(r, in, out));
        }
        return new TruthTableData(
                inputs.stream().map(CircuitElement::getId).toList(),
                outputs.stream().map(CircuitElement::getId).toList(),
                result, currentRow(inputs));
    }

    static int currentRow(List<InputElement> sortedInputs) {
        int row = 0;
        for (int b = 0; b < sortedInputs.size(); b++)
            if (sortedInputs.get(b).getValue() == SignalState.ON) row |= 1 << b;
        return row;
    }

    // Helpers

    /** Order-independent hash of which objects exist and how they are wired (positions are ignored). */
    public static long fingerprint(Circuit c) {
        long h = 0;
        for (CircuitElement e : c.getElements().values()) h += mix(System.identityHashCode(e));
        for (WireSegment w : c.getWireSegments()) {
            long s = mix(System.identityHashCode(w));
            s ^= Long.rotateLeft(mix(System.identityHashCode(startOf(w))), 17);
            s ^= Long.rotateLeft(mix(System.identityHashCode(endOf(w))), 31);
            h += mix(s);
        }
        return h;
    }

    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    private static Object startOf(WireSegment w) { return w.getStartPort() != null ? w.getStartPort() : w.getStartNode(); }

    private static Object endOf(WireSegment w) { return w.getEndPort() != null ? w.getEndPort() : w.getEndNode(); }

    private static Object find(Map<Object, Object> parent, Object x) {
        Object p = parent.getOrDefault(x, x);
        while (p != x) {
            Object gp = parent.getOrDefault(p, p);
            parent.put(x, gp);
            x = gp;
            p = parent.getOrDefault(x, x);
        }
        return x;
    }

    private static void union(Map<Object, Object> parent, Object a, Object b) {
        if (a == null || b == null) return;
        Object ra = find(parent, a), rb = find(parent, b);
        if (ra != rb) parent.put(ra, rb);
    }
}