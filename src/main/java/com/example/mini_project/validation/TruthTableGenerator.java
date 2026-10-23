package com.example.mini_project.validation;

import com.example.mini_project.domain.Circuit;
import com.example.mini_project.domain.CircuitElement;
import com.example.mini_project.domain.InputElement;
import com.example.mini_project.domain.OutputElement;
import com.example.mini_project.domain.SignalState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class TruthTableGenerator {

    public static String generate(Circuit circuit, List<CircuitElement> topoOrder) {
        List<InputElement> inputs = circuit.getElements().values().stream()
                .filter(InputElement.class::isInstance)
                .map(InputElement.class::cast)
                .sorted(Comparator.comparing(CircuitElement::getId))
                .toList();

        List<OutputElement> outputs = circuit.getElements().values().stream()
                .filter(OutputElement.class::isInstance)
                .map(OutputElement.class::cast)
                .sorted(Comparator.comparing(CircuitElement::getId))
                .toList();

        if (inputs.isEmpty()) {
            return "Truth table unavailable: no input switches were found in the circuit.";
        }
        if (outputs.isEmpty()) {
            return "Truth table unavailable: no output LEDs were found in the circuit.";
        }

        Map<String, CircuitElement> elements = circuit.getElements();
        List<InputElement> originalInputs = new ArrayList<>(inputs);
        List<SignalState> originalStates = new ArrayList<>();
        for (InputElement input : originalInputs) {
            originalStates.add(input.getValue());
        }

        int rowCount = 1 << inputs.size();
        StringBuilder table = new StringBuilder();
        table.append("Truth table\n");

        table.append(String.join(" ", getInputHeaders(inputs)))
                .append(" | ")
                .append(String.join(" ", getOutputHeaders(outputs)))
                .append(System.lineSeparator());

        for (int row = 0; row < rowCount; row++) {
            for (int bit = 0; bit < inputs.size(); bit++) {
                SignalState value = ((row >> bit) & 1) == 1 ? SignalState.ON : SignalState.OFF;
                inputs.get(bit).setValue(value);
            }

            new CircuitSimulator(circuit, topoOrder).simulate();

            List<String> values = new ArrayList<>();
            for (InputElement input : inputs) {
                values.add(input.getValue() == SignalState.ON ? "1" : "0");
            }
            for (OutputElement output : outputs) {
                values.add(output.getValue() == SignalState.ON ? "1" : "0");
            }

            table.append(String.join(" ", values))
                    .append(System.lineSeparator());
        }

        for (int i = 0; i < originalInputs.size(); i++) {
            originalInputs.get(i).setValue(originalStates.get(i));
        }

        new CircuitSimulator(circuit, topoOrder).simulate();
        return table.toString();
    }

    private static List<String> getInputHeaders(List<InputElement> inputs) {
        List<String> headers = new ArrayList<>();
        for (InputElement input : inputs) {
            headers.add(input.getId());
        }
        return headers;
    }

    private static List<String> getOutputHeaders(List<OutputElement> outputs) {
        List<String> headers = new ArrayList<>();
        for (OutputElement output : outputs) {
            headers.add(output.getId());
        }
        return headers;
    }
}
