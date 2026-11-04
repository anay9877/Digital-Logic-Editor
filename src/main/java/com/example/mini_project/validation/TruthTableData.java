package com.example.mini_project.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Structured result of a truth-table run: which inputs / outputs exist and the value
 * of every output for every input combination. The GUI renders this directly.
 *
 * Row numbering: bit i of the row index is the value of input i, so the table is
 * in ordinary binary counting order only if you read inputs right-to-left. For display
 * we show the inputs in their natural order (first switch first) and keep the
 * "bit i = input i" convention for minterm numbers.
 */
public final class TruthTableData {

    public record Row(int index, boolean[] inputs, boolean[] outputs) {
        public boolean allOutputsOn() {
            if (outputs.length == 0) return false;
            for (boolean b : outputs) if (!b) return false;
            return true;
        }
    }

    private final List<String> inputIds;
    private final List<String> outputIds;
    private final List<Row> rows;

    public TruthTableData(List<String> inputIds, List<String> outputIds, List<Row> rows, int currentRow) {
        this.inputIds = List.copyOf(inputIds);
        this.outputIds = List.copyOf(outputIds);
        this.rows = List.copyOf(rows);
    }

    public List<String> getInputIds() { return inputIds; }

    public List<String> getOutputIds() { return outputIds; }

    public List<Row> getRows() { return rows; }

    /** Rows where every output is ON at the same time. */
    public List<Row> allGlowRows() {
        List<Row> result = new ArrayList<>();
        for (Row r : rows) if (r.allOutputsOn()) result.add(r);
        return result;
    }

    // Naming helpers

    private static final Pattern ID = Pattern.compile("^(SWITCH|LED)-(\\d+)$", Pattern.CASE_INSENSITIVE);

    /** SWITCH-3 -> S3, LED-2 -> L2; any other id is returned unchanged. */
    public static String shortName(String id) {
        Matcher m = ID.matcher(id);
        if (!m.matches()) return id;
        return (m.group(1).equalsIgnoreCase("SWITCH") ? "S" : "L") + m.group(2);
    }

    public TruthTableData withCurrentRow(int row) {
        return new TruthTableData(inputIds, outputIds, rows, row);
    }
}
