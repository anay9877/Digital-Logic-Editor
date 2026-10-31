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
        List<InputElement> inputs = circuit.getElements().values().stream()
                .filter(InputElement.class::isInstance)
                .map(InputElement.class::cast)
                .sorted(Comparator.comparing(CircuitElement::getId, TruthTableGenerator::naturalCompare))
                .toList();

        List<OutputElement> outputs = circuit.getElements().values().stream()
                .filter(OutputElement.class::isInstance)
                .map(OutputElement.class::cast)
                .sorted(Comparator.comparing(CircuitElement::getId, TruthTableGenerator::naturalCompare))
                .toList();

        if (inputs.isEmpty()) {
            throw new IllegalStateException("Truth table unavailable: no input switches were found in the circuit.");
        }
        if (outputs.isEmpty()) {
            throw new IllegalStateException("Truth table unavailable: no output LEDs were found in the circuit.");
        }
        if (inputs.size() > MAX_INPUTS) {
            throw new IllegalStateException("Truth table unavailable: " + inputs.size()
                    + " switches would need " + (1L << inputs.size()) + " rows (limit is " + MAX_INPUTS + " switches).");
        }

        // remember the switch positions so we can put them back
        List<SignalState> originalStates = new ArrayList<>();
        int currentRow = 0;
        for (int bit = 0; bit < inputs.size(); bit++) {
            SignalState s = inputs.get(bit).getValue();
            originalStates.add(s);
            if (s == SignalState.ON) currentRow |= (1 << bit);
        }

        CircuitSimulator simulator = new CircuitSimulator(circuit, topoOrder);
        List<TruthTableData.Row> rows = new ArrayList<>();
        int rowCount = 1 << inputs.size();

        try {
            for (int row = 0; row < rowCount; row++) {
                boolean[] in = new boolean[inputs.size()];
                for (int bit = 0; bit < inputs.size(); bit++) {
                    in[bit] = ((row >> bit) & 1) == 1;
                    inputs.get(bit).setValue(in[bit] ? SignalState.ON : SignalState.OFF);
                }

                simulator.simulate();

                boolean[] out = new boolean[outputs.size()];
                for (int i = 0; i < outputs.size(); i++) {
                    out[i] = outputs.get(i).getValue() == SignalState.ON;
                }
                rows.add(new TruthTableData.Row(row, in, out));
            }
        } finally {
            for (int i = 0; i < inputs.size(); i++) {
                inputs.get(i).setValue(originalStates.get(i));
            }
            simulator.simulate();
        }

        return new TruthTableData(
                inputs.stream().map(CircuitElement::getId).toList(),
                outputs.stream().map(CircuitElement::getId).toList(),
                rows,
                currentRow);
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