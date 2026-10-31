package com.example.mini_project.gui;

import com.example.mini_project.validation.SubCircuitTable;
import com.example.mini_project.validation.TruthTableData;
import com.example.mini_project.validation.TruthTableData.Row;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.Modality;
import javafx.util.Callback;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * Truth tables, one per sub-circuit, in the editor's own look.
 *
 * Left: list of circuits (selecting one highlights it on the canvas).
 * Right: that circuit's table, plus the switch combinations that light every LED.
 */
public final class TruthTableWindow {

    private static final String BAR = "#dddddd";     // same grey as the toolbar
    private static final String BLUE = "#1A73E8";    // gate outline colour
    private static final String GREEN = "#2e9e44";   // darker shade of the port green
    private static final String ROW_ON = "#e8f5e9";
    private static final String MUTED = "#9a9a9a";
    private static final double LIST_WIDTH = 170;

    private static Stage openStage;   // keep a single table window open

    private TruthTableWindow() {
    }

    /**
     * @param onPick called with (elementIds, wireIds) of the circuit being viewed so the
     *               editor can highlight it; called with two empty lists when the window closes
     */
    public static void show(Window owner, List<SubCircuitTable> tables,
                            BiConsumer<List<String>, List<String>> onPick) {
        if (openStage != null) openStage.close();

        Stage stage = new Stage();
        stage.initModality(Modality.NONE);   // never block the editor
        // no initOwner: an owned window is forced above its owner, which trapped the canvas
        stage.setTitle("Truth Tables");

        BorderPane root = new BorderPane();

        ListView<SubCircuitTable> list = new ListView<>(FXCollections.observableArrayList(tables));
        list.setPrefWidth(LIST_WIDTH);
        list.setStyle("-fx-background-insets: 0; -fx-border-color: transparent #cccccc transparent transparent;");
        list.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(SubCircuitTable t, boolean empty) {
                super.updateItem(t, empty);
                if (empty || t == null) {
                    setText(null);
                    return;
                }
                String info = t.ok() ? t.switches() + " in  >  " + t.leds() + " out" : "incomplete";
                setText(t.name() + "\n" + info);
            }
        });
        list.getSelectionModel().selectedItemProperty().addListener((obs, old, t) -> {
            if (t == null) return;
            root.setCenter(buildDetail(t));
            onPick.accept(t.elementIds(), t.wireIds());
        });
        root.setLeft(list);
        list.getSelectionModel().selectFirst();

        int widest = tables.stream().filter(SubCircuitTable::ok)
                .mapToInt(t -> 1 + t.data().getInputIds().size() + t.data().getOutputIds().size())
                .max().orElse(3);
        double width = LIST_WIDTH + Math.max(380, Math.min(widest * 80 + 40, 820));
        stage.setScene(new Scene(root, width, 520));
        if (owner != null) {
            stage.setX(owner.getX() + owner.getWidth() - width - 30);
            stage.setY(owner.getY() + 60);
        }

        stage.setOnHidden(e -> {
            onPick.accept(List.of(), List.of());          // remove the highlight
            if (openStage == stage) openStage = null;
        });
        openStage = stage;
        stage.show();
    }

    // Right-hand side

    private static Node buildDetail(SubCircuitTable t) {
        if (!t.ok()) {
            Label message = new Label(t.name() + " cannot be tabulated yet.\n\n" + t.error());
            message.setWrapText(true);
            message.setStyle("-fx-text-fill: " + MUTED + ";");
            StackPane box = new StackPane(message);
            box.setPadding(new Insets(20));
            box.setAlignment(Pos.CENTER);
            return box;
        }
        BorderPane detail = new BorderPane();
        detail.setTop(buildHeader(t));
        detail.setCenter(buildTable(t.data()));
        detail.setBottom(buildGlowList(t.data()));
        return detail;
    }

    private static VBox buildHeader(SubCircuitTable t) {
        TruthTableData data = t.data();
        int nIn = data.getInputIds().size();
        int nOut = data.getOutputIds().size();

        Label title = new Label(t.name());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label summary = new Label(nIn + (nIn == 1 ? " switch" : " switches") + "  |  "
                + nOut + (nOut == 1 ? " LED" : " LEDs") + "  |  "
                + data.getRows().size() + " combinations");

        Label contains = new Label("Contains: " + String.join(", ", t.elementIds()));
        contains.setWrapText(true);
        contains.setStyle("-fx-text-fill: #666666;");

        Label legend = new Label("Green rows: every LED glows");
        legend.setStyle("-fx-text-fill: " + GREEN + ";");

        VBox header = new VBox(4, title, summary, contains, legend);
        header.setStyle("-fx-background-color: " + BAR + "; -fx-padding: 10;");
        return header;
    }

    // Table

    private static TableView<Row> buildTable(TruthTableData data) {
        TableView<Row> table = new TableView<>(FXCollections.observableArrayList(data.getRows()));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setFocusTraversable(false);

        TableColumn<Row, String> index = new TableColumn<>("#");
        index.setCellValueFactory(c -> new ReadOnlyStringWrapper(String.valueOf(c.getValue().index())));
        index.setCellFactory(valueCells(MUTED, false, false));
        index.setMaxWidth(60);
        styleColumn(index);
        table.getColumns().add(index);

        List<String> inputs = data.getInputIds();
        for (int i = 0; i < inputs.size(); i++) {
            final int bit = i;
            TableColumn<Row, String> col = new TableColumn<>(TruthTableData.shortName(inputs.get(i)));
            col.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().inputs()[bit] ? "1" : "0"));
            col.setCellFactory(valueCells(BLUE, true, false));
            styleColumn(col);
            table.getColumns().add(col);
        }

        List<String> outputs = data.getOutputIds();
        for (int i = 0; i < outputs.size(); i++) {
            final int pos = i;
            TableColumn<Row, String> col = new TableColumn<>(TruthTableData.shortName(outputs.get(i)));
            col.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().outputs()[pos] ? "1" : "0"));
            col.setCellFactory(valueCells(GREEN, true, i == 0));   // thin divider before the first LED
            styleColumn(col);
            table.getColumns().add(col);
        }

        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Row row, boolean empty) {
                super.updateItem(row, empty);
                setStyle(!empty && row != null && row.allOutputsOn()
                        ? "-fx-background-color: " + ROW_ON + ";" : "");
            }
        });
        return table;
    }

    private static void styleColumn(TableColumn<Row, String> col) {
        col.setSortable(false);
        col.setReorderable(false);
        col.setStyle("-fx-alignment: CENTER;");
    }

    /** Centered cell: a "1" is bold and coloured, a "0" is grey. */
    private static Callback<TableColumn<Row, String>, TableCell<Row, String>> valueCells(
            String onColor, boolean highlightOne, boolean dividerLeft) {
        return col -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(value);
                String style = "-fx-alignment: CENTER;";
                if (!highlightOne) {
                    style += "-fx-text-fill: " + onColor + ";";
                } else if ("1".equals(value)) {
                    style += "-fx-font-weight: bold; -fx-text-fill: " + onColor + ";";
                } else {
                    style += "-fx-text-fill: " + MUTED + ";";
                }
                if (dividerLeft) {
                    style += "-fx-border-color: transparent transparent transparent #c8c8c8;";
                }
                setStyle(style);
            }
        };
    }

    // Combinations that light every LED

    private static VBox buildGlowList(TruthTableData data) {
        List<String> inNames = data.getInputIds().stream().map(TruthTableData::shortName).toList();
        List<Row> rows = data.allGlowRows();
        boolean single = data.getOutputIds().size() == 1;

        Label title = new Label(single ? "The LED glows when" : "Every LED glows when");
        title.setStyle("-fx-font-weight: bold;");

        VBox box = new VBox(6, title);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-border-color: #cccccc transparent transparent transparent;");

        if (rows.isEmpty()) {
            Label none = new Label(single ? "No switch combination lights the LED."
                    : "No switch combination lights every LED.");
            none.setStyle("-fx-text-fill: " + MUTED + ";");
            box.getChildren().add(none);
            return box;
        }

        ListView<String> list = new ListView<>();
        for (Row r : rows) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < inNames.size(); i++) {
                if (i > 0) sb.append(",   ");
                sb.append(inNames.get(i)).append(" = ").append(r.inputs()[i] ? 1 : 0);
            }
            list.getItems().add(sb.toString());
        }
        list.setFocusTraversable(false);
        list.setPrefHeight(Math.min(rows.size(), 5) * 26 + 4);
        box.getChildren().add(list);
        return box;
    }

    public static void closeIfOpen() {
        if (openStage != null) openStage.close();
    }
}