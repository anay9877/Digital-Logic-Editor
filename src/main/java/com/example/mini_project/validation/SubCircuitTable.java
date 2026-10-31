package com.example.mini_project.validation;

import java.util.List;

/**
 * Truth-table result for one sub-circuit (one connected piece of the canvas).
 * Exactly one of {@code data} / {@code error} is set.
 */
public record SubCircuitTable(
        String name,                 // "Circuit 1", "Circuit 2", ... (top-left to bottom-right)
        int switches,
        int leds,
        List<String> elementIds,     // used to highlight the circuit on the canvas
        List<String> wireIds,
        TruthTableData data,         // null when the table could not be built
        String error) {              // null when the table was built

    public boolean ok() {
        return data != null;
    }
}