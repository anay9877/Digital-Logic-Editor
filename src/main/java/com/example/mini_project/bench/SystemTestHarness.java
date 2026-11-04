package com.example.mini_project.bench;

import com.example.mini_project.domain.*;
import com.example.mini_project.validation.*;

import java.util.*;
import java.util.function.Function;

/**
 * Headless system-test harness: correctness, performance and memory/scale tests for the
 * simulation engine (CompiledCircuit) and truth-table generator.
 *
 * Run:  java -Xmx512m <javafx + gson + svgimage on classpath> com.example.mini_project.bench.SystemTestHarness <mode> [args]
 *   correct                      functional/correctness cases (gates, truth tables, sim-vs-table cross-check)
 *   perf   [sizes...]            per-stage timing for each gate count (default list below)
 *   scale  [startGates] [maxMs]  doubles gate count until OOM or one full evaluation exceeds maxMs
 *   tt                           truth-table timing/memory for 1..10 switches (+ the 11-switch rejection)
 *   export <width> <depth> <file.json>   write a layered circuit you can load via File > Open in the GUI
 */
public class SystemTestHarness {

    // ------------------------------------------------------------------ circuit builders

    /** A generated circuit plus an independent reference model (the "oracle") for its outputs. */
    static final class Built {
        final Circuit circuit = new Circuit();
        final List<InputElement> inputs = new ArrayList<>();      // SWITCH-1..k (natural order == creation order)
        final List<OutputElement> outputs = new ArrayList<>();    // LED-1..m
        int gates;
        Function<boolean[], boolean[]> oracle;                    // switch values -> expected LED values
    }

    private static final ComponentType[] MIX = {
            ComponentType.AND, ComponentType.OR, ComponentType.XOR, ComponentType.NAND, ComponentType.NOR};

    private static Gate newGate(ComponentType t, String id, double x, double y) {
        return switch (t) {
            case AND -> new AndGate(id, x, y);
            case OR -> new OrGate(id, x, y);
            case XOR -> new XorGate(id, x, y);
            case NAND -> new NandGate(id, x, y);
            case NOR -> new NorGate(id, x, y);
            case NOT -> new NotGate(id, x, y);
            default -> throw new IllegalArgumentException(t.name());
        };
    }

    private static boolean apply(ComponentType t, boolean a, boolean b) {
        return switch (t) {
            case AND -> a & b;
            case OR -> a | b;
            case XOR -> a ^ b;
            case NAND -> !(a & b);
            case NOR -> !(a | b);
            default -> throw new IllegalArgumentException(t.name());
        };
    }

    private static void wire(Circuit c, Port from, Port to) {
        c.addWireSegment(WireSegment.portToPort(from, from.getAbsX(), from.getAbsY(), to, to.getAbsX(), to.getAbsY()));
    }

    private static InputElement addSwitch(Built b, int n, double x, double y) {
        InputElement s = new InputElement("SWITCH-" + n, x, y);
        b.circuit.addElement(s);
        b.inputs.add(s);
        return s;
    }

    private static OutputElement addLed(Built b, int n, double x, double y) {
        OutputElement l = new OutputElement("LED-" + n, x, y);
        b.circuit.addElement(l);
        b.outputs.add(l);
        return l;
    }

    /** width switches -> depth layers of width mixed 2-input gates -> width LEDs. Gates = width*depth. Fan-out 2. */
    static Built layered(int width, int depth) {
        Built b = new Built();
        List<Port> prev = new ArrayList<>();
        for (int j = 0; j < width; j++) prev.add(addSwitch(b, j + 1, 0, j * 100).getOutputPort());
        Map<ComponentType, Integer> counter = new EnumMap<>(ComponentType.class);
        for (int layer = 0; layer < depth; layer++) {
            List<Port> next = new ArrayList<>();
            for (int j = 0; j < width; j++) {
                ComponentType t = MIX[(j + layer) % MIX.length];
                int n = counter.merge(t, 1, Integer::sum);
                Gate g = newGate(t, t.name() + "-" + n, (layer + 1) * 140, j * 100);
                b.circuit.addElement(g);
                wire(b.circuit, prev.get(j), g.getInputPorts().get(0));
                wire(b.circuit, prev.get((j + 1) % width), g.getInputPorts().get(1));
                next.add(g.getOutputPort());
                b.gates++;
            }
            prev = next;
        }
        for (int j = 0; j < width; j++) {
            OutputElement led = addLed(b, j + 1, (depth + 1) * 140, j * 100);
            wire(b.circuit, prev.get(j), led.getInputPort());
        }
        final int w = width, d = depth;
        b.oracle = in -> {
            boolean[] cur = in.clone();
            for (int layer = 0; layer < d; layer++) {
                boolean[] nxt = new boolean[w];
                for (int j = 0; j < w; j++) nxt[j] = apply(MIX[(j + layer) % MIX.length], cur[j], cur[(j + 1) % w]);
                cur = nxt;
            }
            return cur;
        };
        return b;
    }

    /** 1 switch -> n NOT gates in series -> 1 LED. Maximum logic depth for the gate count. */
    static Built notChain(int n) {
        Built b = new Built();
        Port prev = addSwitch(b, 1, 0, 0).getOutputPort();
        for (int i = 0; i < n; i++) {
            Gate g = new NotGate("NOT-" + (i + 1), (i + 1) * 140, 0);
            b.circuit.addElement(g);
            wire(b.circuit, prev, g.getInputPorts().get(0));
            prev = g.getOutputPort();
            b.gates++;
        }
        OutputElement led = addLed(b, 1, (n + 1) * 140, 0);
        wire(b.circuit, prev, led.getInputPort());
        b.oracle = in -> new boolean[]{(n % 2 == 0) == in[0]};
        return b;
    }

    /** k switches folded through k-1 gates of one type into 1 LED (k=1: switch wired straight to the LED). */
    static Built reduce(int k, ComponentType t) {
        Built b = new Built();
        Port acc = addSwitch(b, 1, 0, 0).getOutputPort();
        for (int i = 1; i < k; i++) {
            Port s = addSwitch(b, i + 1, 0, i * 100).getOutputPort();
            Gate g = newGate(t, t.name() + "-" + i, i * 140, i * 100);
            b.circuit.addElement(g);
            wire(b.circuit, acc, g.getInputPorts().get(0));
            wire(b.circuit, s, g.getInputPorts().get(1));
            acc = g.getOutputPort();
            b.gates++;
        }
        OutputElement led = addLed(b, 1, (k + 1) * 140, 0);
        wire(b.circuit, acc, led.getInputPort());
        b.oracle = in -> {
            boolean r = in[0];
            for (int i = 1; i < in.length; i++) r = apply(t, r, in[i]);
            return new boolean[]{r};
        };
        return b;
    }

    /** One 2-input gate, two switches, one LED. */
    static Built single(ComponentType t) { return reduce(2, t); }

    // ------------------------------------------------------------------ pipeline stages (mirrors EditorController)

    static void validate(Circuit c) {
        CircuitValidator v = new CircuitValidator();
        boolean ok = v.validateDanglingWires(c);
        ok &= v.validateInputPorts(c);
        ok &= v.validateOutputPorts(c);
        ok &= v.validateInputsAndOutputs(c);
        ok &= v.validateWireReferences(c);
        ok &= v.validateWireDrivers(c);
        if (!ok) throw new IllegalStateException(String.join(" | ", v.getErrors()));
    }

    static List<CircuitElement> order(Circuit c) { return new CircuitTopology().sort(c); }

    static void setSwitches(Built b, boolean[] v) {
        for (int i = 0; i < v.length; i++) b.inputs.get(i).setValue(v[i] ? SignalState.ON : SignalState.OFF);
    }

    static boolean[] ledValues(Built b) {
        boolean[] r = new boolean[b.outputs.size()];
        for (int i = 0; i < r.length; i++) r[i] = b.outputs.get(i).getValue() == SignalState.ON;
        return r;
    }

    static boolean[] bits(int row, int n) {
        boolean[] r = new boolean[n];
        for (int i = 0; i < n; i++) r[i] = ((row >>> i) & 1) == 1;
        return r;
    }

    // ------------------------------------------------------------------ tiny test framework

    static int pass, fail;

    static void check(String id, String what, boolean ok, String detail) {
        if (ok) pass++; else fail++;
        System.out.printf("%-6s %-4s %s%s%n", id, ok ? "PASS" : "FAIL", what, ok ? "" : "  -> " + detail);
    }

    // ------------------------------------------------------------------ MODE: correct

    static void correct() {
        // CT-01..06 each gate type, exhaustive 2-input truth table (table AND live simulation)
        int id = 1;
        for (ComponentType t : new ComponentType[]{ComponentType.AND, ComponentType.OR, ComponentType.XOR,
                ComponentType.NAND, ComponentType.NOR}) {
            Built b = single(t);
            validate(b.circuit);
            List<CircuitElement> ord = order(b.circuit);
            TruthTableData tt = TruthTableGenerator.generate(b.circuit, ord);
            CompiledCircuit cc = CompiledCircuit.compile(b.circuit, ord);
            boolean ok = tt.getRows().size() == 4;
            StringBuilder why = new StringBuilder();
            for (int r = 0; r < 4; r++) {
                boolean[] in = bits(r, 2);
                boolean exp = apply(t, in[0], in[1]);
                if (tt.getRows().get(r).outputs()[0] != exp) { ok = false; why.append("table row ").append(r).append(' '); }
                setSwitches(b, in);
                cc.evaluateAll();
                if (ledValues(b)[0] != exp) { ok = false; why.append("sim row ").append(r).append(' '); }
            }
            check(String.format("CT-%02d", id++), t + " gate: 4-row table + simulation", ok, why.toString());
        }
        // CT-06 NOT gate
        {
            Built b = notChain(1);
            validate(b.circuit);
            List<CircuitElement> ord = order(b.circuit);
            TruthTableData tt = TruthTableGenerator.generate(b.circuit, ord);
            boolean ok = tt.getRows().size() == 2 && tt.getRows().get(0).outputs()[0] && !tt.getRows().get(1).outputs()[0];
            check("CT-06", "NOT gate: 2-row table (0->1, 1->0)", ok, "unexpected rows");
        }

        // TT-01..10  rows = 2^n for n = 1..10, every row vs oracle; row r <-> bit i of r is switch i
        for (int n = 1; n <= 10; n++) {
            Built b = layered(Math.max(n, 2), 3);          // layered needs >=2 wide; extra switch still counts
            if (n == 1) b = reduce(1, ComponentType.AND);  // 1 switch -> LED
            validate(b.circuit);
            TruthTableData tt = TruthTableGenerator.generate(b.circuit, order(b.circuit));
            boolean ok = tt.getRows().size() == (1 << b.inputs.size()) && tt.getInputIds().size() == b.inputs.size();
            String why = "";
            for (TruthTableData.Row row : tt.getRows()) {
                boolean[] exp = b.oracle.apply(bits(row.index(), b.inputs.size()));
                if (!Arrays.equals(row.inputs(), bits(row.index(), b.inputs.size()))
                        || !Arrays.equals(row.outputs(), exp)) { ok = false; why = "row " + row.index(); break; }
            }
            check(String.format("TT-%02d", n), n + " switch(es): " + tt.getRows().size() + " rows all match oracle", ok, why);
        }

        // TT-11 / 12 / 13  known glow counts
        for (ComponentType t : new ComponentType[]{ComponentType.AND, ComponentType.OR, ComponentType.XOR}) {
            Built b = reduce(10, t);
            validate(b.circuit);
            TruthTableData tt = TruthTableGenerator.generate(b.circuit, order(b.circuit));
            int expect = t == ComponentType.AND ? 1 : t == ComponentType.OR ? 1023 : 512;
            check(t == ComponentType.AND ? "TT-11" : t == ComponentType.OR ? "TT-12" : "TT-13",
                    "10-input " + t + " chain: glow rows = " + expect, tt.allGlowRows().size() == expect,
                    "got " + tt.allGlowRows().size());
        }

        // TT-14 natural ID ordering SWITCH-2 before SWITCH-10
        {
            Built b = reduce(10, ComponentType.AND);
            TruthTableData tt = TruthTableGenerator.generate(b.circuit, order(b.circuit));
            boolean ok = tt.getInputIds().get(1).equals("SWITCH-2") && tt.getInputIds().get(9).equals("SWITCH-10");
            check("TT-14", "natural ordering: SWITCH-2 before SWITCH-10", ok, tt.getInputIds().toString());
        }

        // TT-15 rejection beyond MAX_INPUTS
        {
            Built b = reduce(TruthTableGenerator.MAX_INPUTS + 1, ComponentType.AND);
            validate(b.circuit);
            String msg = null;
            try { TruthTableGenerator.generate(b.circuit, order(b.circuit)); } catch (IllegalStateException e) { msg = e.getMessage(); }
            check("TT-15", "11 switches rejected with clear message", msg != null && msg.contains("limit"), String.valueOf(msg));
        }

        // TT-16 switches are not disturbed by table generation; currentRow matches
        {
            Built b = reduce(4, ComponentType.OR);
            setSwitches(b, new boolean[]{true, false, true, false});         // row 5
            TruthTableData tt = TruthTableGenerator.generate(b.circuit, order(b.circuit));
            boolean untouched = b.inputs.get(0).getValue() == SignalState.ON && b.inputs.get(1).getValue() == SignalState.OFF
                    && b.inputs.get(2).getValue() == SignalState.ON && b.inputs.get(3).getValue() == SignalState.OFF;
            check("TT-16", "switch positions preserved; current row = 5", untouched && TruthTableGenerator.currentRow(b.circuit) == 5,
                    "currentRow=" + TruthTableGenerator.currentRow(b.circuit));
        }

        // TT-17 / 18  circuit without LEDs / without switches
        {
            Built noLed = new Built();
            addSwitch(noLed, 1, 0, 0);
            String m1 = tryTable(noLed.circuit);
            check("TT-17", "no LED -> error, no table", m1 != null, "table was produced");
            Built noSw = new Built();
            addLed(noSw, 1, 0, 0);
            String m2 = tryTable(noSw.circuit);
            check("TT-18", "no switch -> error, no table", m2 != null, "table was produced");
        }

        // SM-01..03  simulation vs oracle on larger circuits: full evaluation AND incremental update
        int[][] shapes = {{4, 5}, {8, 20}, {10, 50}};
        for (int s = 0; s < shapes.length; s++) {
            Built b = layered(shapes[s][0], shapes[s][1]);
            validate(b.circuit);
            CompiledCircuit cc = CompiledCircuit.compile(b.circuit, order(b.circuit));
            Random rnd = new Random(42 + s);
            int n = b.inputs.size();
            boolean[] cur = new boolean[n];
            setSwitches(b, cur);
            cc.evaluateAll();
            boolean ok = Arrays.equals(ledValues(b), b.oracle.apply(cur));
            String why = ok ? "" : "initial state";
            for (int step = 0; ok && step < 500; step++) {
                int k = rnd.nextInt(n);                                  // flip one switch -> incremental path
                cur[k] = !cur[k];
                b.inputs.get(k).setValue(cur[k] ? SignalState.ON : SignalState.OFF);
                cc.update(b.inputs.get(k));
                if (!Arrays.equals(ledValues(b), b.oracle.apply(cur))) { ok = false; why = "incremental step " + step; }
            }
            check(String.format("SM-%02d", s + 1), shapes[s][0] + "x" + shapes[s][1] + " layered: 500 random toggles match oracle",
                    ok, why);
        }

        // SM-04 deep chain, odd/even
        for (int n : new int[]{1000, 1001}) {
            Built b = notChain(n);
            validate(b.circuit);
            CompiledCircuit cc = CompiledCircuit.compile(b.circuit, order(b.circuit));
            setSwitches(b, new boolean[]{true});
            cc.evaluateAll();
            check("SM-04", n + " NOT chain, switch ON -> LED " + (n % 2 == 0), ledValues(b)[0] == (n % 2 == 0), "wrong LED");
        }

        // CS-01  simulation cross-checked against truth table for EVERY row (sim and table must agree)
        {
            Built b = layered(10, 6);
            validate(b.circuit);
            List<CircuitElement> ord = order(b.circuit);
            TruthTableData tt = TruthTableGenerator.generate(b.circuit, ord);
            CompiledCircuit cc = CompiledCircuit.compile(b.circuit, ord);
            boolean ok = true; String why = "";
            for (TruthTableData.Row row : tt.getRows()) {
                setSwitches(b, row.inputs());
                cc.evaluateAll();
                if (!Arrays.equals(ledValues(b), row.outputs())) { ok = false; why = "row " + row.index(); break; }
            }
            check("CS-01", "10 switches x 1024 rows: simulation == truth table on every row", ok, why);
        }

        // VL-01.. invalid circuits must be rejected
        {
            Built b = single(ComponentType.AND);
            b.circuit.removeWireSegment(b.circuit.getWireSegments().get(0));
            check("VL-01", "missing wire -> validation fails", fails(b.circuit), "validation passed");
            Built e = new Built();
            check("VL-02", "empty circuit -> validation fails", fails(e.circuit), "validation passed");
        }

        System.out.printf("%nSUMMARY: %d passed, %d failed%n", pass, fail);
    }

    private static boolean fails(Circuit c) {
        try { validate(c); return false; } catch (IllegalStateException e) { return true; }
    }

    private static String tryTable(Circuit c) {
        try { TruthTableGenerator.generate(c, order(c)); return null; } catch (RuntimeException e) { return e.getMessage(); }
    }

    // ------------------------------------------------------------------ MODE: perf / scale / tt

    static long usedBytes() {
        Runtime rt = Runtime.getRuntime();
        for (int i = 0; i < 3; i++) { System.gc(); try { Thread.sleep(50); } catch (InterruptedException ignored) { } }
        return rt.totalMemory() - rt.freeMemory();
    }

    static double ms(long ns) { return ns / 1e6; }

    static double median(long[] a) { long[] c = a.clone(); Arrays.sort(c); return c[c.length / 2]; }

    /** Returns a one-line result; throws OutOfMemoryError to the caller. */
    static double[] measure(int width, int depth, int reps) {
        long mem0 = usedBytes();
        long t = System.nanoTime();
        Built b = layered(width, depth);
        double build = ms(System.nanoTime() - t);

        t = System.nanoTime();   validate(b.circuit);                         double vMs = ms(System.nanoTime() - t);
        t = System.nanoTime();   List<CircuitElement> ord = order(b.circuit); double oMs = ms(System.nanoTime() - t);
        t = System.nanoTime();   CompiledCircuit cc = CompiledCircuit.compile(b.circuit, ord); double cMs = ms(System.nanoTime() - t);

        setSwitches(b, new boolean[width]);
        for (int i = 0; i < 2; i++) cc.evaluateAll();                         // JIT warm-up
        long[] full = new long[reps];
        for (int i = 0; i < reps; i++) { t = System.nanoTime(); cc.evaluateAll(); full[i] = System.nanoTime() - t; }

        long[] inc = new long[reps * 20];
        for (int i = 0; i < inc.length; i++) {
            InputElement s = b.inputs.get(i % width);
            s.toggle();
            t = System.nanoTime(); cc.update(s); inc[i] = System.nanoTime() - t;
        }
        long mem1 = usedBytes();
        double total = vMs + oMs + cMs + ms((long) median(full));
        return new double[]{b.gates, build, vMs, oMs, cMs, ms((long) median(full)), median(inc) / 1e3, total,
                (mem1 - mem0) / 1048576.0};
    }

    static void perf(String[] args) {
        int[] sizes = args.length > 1 ? Arrays.stream(args, 1, args.length).mapToInt(Integer::parseInt).toArray()
                : new int[]{10, 100, 1_000, 5_000, 10_000, 50_000, 100_000};
        System.out.println("gates,build_ms,validate_ms,toposort_ms,compile_ms,full_eval_ms(median),toggle_update_us(median),SIMULATE_total_ms,heap_MB");
        for (int g : sizes) {
            int width = 10, depth = Math.max(1, g / width);
            double[] r = measure(width, depth, 5);
            System.out.printf(Locale.ROOT, "%.0f,%.1f,%.1f,%.1f,%.1f,%.3f,%.1f,%.1f,%.1f%n", r[0], r[1], r[2], r[3], r[4], r[5], r[6], r[7], r[8]);
        }
    }

    static void scale(String[] args) {
        int gates = args.length > 1 ? Integer.parseInt(args[1]) : 10_000;
        double maxMs = args.length > 2 ? Double.parseDouble(args[2]) : 2000;
        System.out.println("max heap = " + Runtime.getRuntime().maxMemory() / 1048576 + " MB;  stop when SIMULATE_total_ms > " + maxMs);
        System.out.println("gates,SIMULATE_total_ms,full_eval_ms,heap_MB");
        int last = 0;
        while (true) {
            try {
                double[] r = measure(10, Math.max(1, gates / 10), 3);
                System.out.printf(Locale.ROOT, "%.0f,%.1f,%.3f,%.1f%n", r[0], r[7], r[5], r[8]);
                last = (int) r[0];
                if (r[7] > maxMs) { System.out.println("STOP: time budget exceeded. Max practical gates = " + last); return; }
                gates *= 2;
            } catch (OutOfMemoryError oom) {
                System.out.println("STOP: OutOfMemoryError at " + gates + " gates. Max gates that fit in this heap = " + last);
                return;
            }
        }
    }

    static void tt() {
        System.out.println("switches,rows,gates,truth_table_ms(median of 5),table_heap_KB");
        for (int n = 1; n <= 10; n++) {
            Built b = reduce(n, ComponentType.XOR);
            Built heavy = n >= 2 ? layered(n, 100) : b;     // heavy: n switches, 100 layers (100*n gates)
            for (Built x : new Built[]{b, heavy}) {
                validate(x.circuit);
                List<CircuitElement> ord = order(x.circuit);
                TruthTableGenerator.generate(x.circuit, ord);                      // warm-up
                long[] t = new long[5];
                TruthTableData data = null;
                for (int i = 0; i < 5; i++) { long s = System.nanoTime(); data = TruthTableGenerator.generate(x.circuit, ord); t[i] = System.nanoTime() - s; }
                long m0 = usedBytes();
                TruthTableData keep = TruthTableGenerator.generate(x.circuit, ord);
                long m1 = usedBytes();
                System.out.printf(Locale.ROOT, "%d,%d,%d,%.3f,%d%n", n, data.getRows().size(), x.gates, median(t) / 1e6, Math.max(0, (m1 - m0) / 1024));
                if (keep.getRows().isEmpty()) System.out.println();                 // keep 'keep' alive
            }
        }
        Built b = reduce(11, ComponentType.AND);
        validate(b.circuit);
        System.out.println("11 switches -> " + tryTable(b.circuit));
    }

    // ------------------------------------------------------------------ MODE: export (for GUI timing)

    static void export(String[] args) throws Exception {
        int width = Integer.parseInt(args[1]), depth = Integer.parseInt(args[2]);
        Built b = layered(width, depth);
        javafx.scene.Group g = new javafx.scene.Group();
        String json = com.example.mini_project.persistence.CircuitSerializer.toJson(b.circuit, new com.example.mini_project.gui.ViewportManager(g));
        java.nio.file.Files.writeString(java.nio.file.Path.of(args[3]), json);
        System.out.println("wrote " + args[3] + " (" + b.gates + " gates, " + width + " switches, " + width + " LEDs)");
    }

    // ------------------------------------------------------------------ main

    public static void main(String[] args) throws Exception {
        try { javafx.application.Platform.startup(() -> { }); } catch (IllegalStateException ignored) { }  // gates load SVG icons
        String mode = args.length == 0 ? "correct" : args[0];
        switch (mode) {
            case "correct" -> correct();
            case "perf" -> perf(args);
            case "scale" -> scale(args);
            case "tt" -> tt();
            case "export" -> export(args);
            default -> System.out.println("modes: correct | perf [sizes...] | scale [start] [maxMs] | tt | export <w> <d> <file>");
        }
        javafx.application.Platform.exit();
    }
}
