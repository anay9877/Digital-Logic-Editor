package com.example.mini_project.validation;

import com.example.mini_project.domain.Circuit;
import com.example.mini_project.domain.CircuitElement;
import com.example.mini_project.domain.InputElement;
import com.example.mini_project.domain.OutputElement;
import com.example.mini_project.domain.SignalState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Drives every combination of the circuit's switches through the simulator and records
 * what every LED does. The switches are restored to their original positions afterwards.
 */
public class TruthTableGenerator {

    /** Above this many switches the table (2^n rows) would be unreadable and slow. */
    public static final int MAX_INPUTS = 10;

    private static final Pattern TRAILING_NUMBER = Pattern.compile("^(.*?)(\\d+)$");

    /** @throws IllegalStateException with a user-friendly message if no table can be built. */
    public static TruthTableData generate(Circuit circuit, List<CircuitElement> topoOrder) {
        return CompiledCircuit.compile(circuit, topoOrder).truthTable();   // bit-parallel, leaves switches untouched
    }

    /** Row number matching the switches' current positions (bit i = i-th switch in natural order). */
    public static int currentRow(Circuit circuit) {
        List<InputElement> inputs = circuit.getElements().values().stream()
                .filter(InputElement.class::isInstance).map(InputElement.class::cast)
                .sorted(Comparator.comparing(CircuitElement::getId, TruthTableGenerator::naturalCompare)).toList();
        return CompiledCircuit.currentRow(inputs);
    }

    /** "SWITCH-2" sorts before "SWITCH-10". */
    static int naturalCompare(String a, String b) {
        Matcher ma = TRAILING_NUMBER.matcher(a), mb = TRAILING_NUMBER.matcher(b);
        if (ma.matches() && mb.matches()) {
            int c = ma.group(1).compareTo(mb.group(1));
            if (c != 0) return c;
            return Long.compare(Long.parseLong(ma.group(2)), Long.parseLong(mb.group(2)));
        }
        return a.compareTo(b);
    }
}