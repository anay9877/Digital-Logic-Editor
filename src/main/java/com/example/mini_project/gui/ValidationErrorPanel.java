package com.example.mini_project.gui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

//prajw : start
/** Fixed-size dropdown history of timestamped validation and connection errors. */
public final class ValidationErrorPanel extends VBox {
    private static final double OPEN_WIDTH = 340;
    private static final double OPEN_HEIGHT = 440;
    private static final double COLLAPSED_SIZE = 40;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Button toggleButton = new Button("⌄");
    //prajw : start
    // Adds an explicit way to remove the accumulated error history from the panel.
    private final Button clearButton = new Button("Clear");
    //prajw: end
    private final Label title = new Label("Errors");
    private final VBox errorList = new VBox(8);
    private final ScrollPane scrollPane = new ScrollPane(errorList);
    private int errorCount;
    private boolean expanded;
    private Runnable onCleared;

    public ValidationErrorPanel() {
        setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #c8c8c8; -fx-border-width: 0 0 0 1;");
        setMinHeight(0);
        setMaxHeight(Double.MAX_VALUE);

        //prajw : start
        // Keep Clear beside the count so the accumulated history can be reset without closing the panel.
        HBox header = new HBox(8, title, new Region(), clearButton, toggleButton);
        //prajw: end
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);
        header.setPadding(new Insets(4));
        header.setStyle("-fx-background-color: #e6e6e6;");
        toggleButton.setFocusTraversable(false);
        toggleButton.setOnAction(event -> setExpanded(!expanded));
        toggleButton.setTooltip(new Tooltip("Open error history"));
        //prajw : start
        // Clear starts disabled and becomes available only when the panel contains errors.
        clearButton.setFocusTraversable(false);
        clearButton.setOnAction(event -> clearErrors());
        clearButton.setDisable(true);
        //prajw: end
        header.setAlignment(Pos.CENTER_LEFT);

        errorList.setPadding(new Insets(10));
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background: #f4f4f4; -fx-background-color: #f4f4f4;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        getChildren().addAll(header, scrollPane);
        setExpanded(false);
    }

    /** Appends the full validation batch and reveals the panel when new errors arrive. */
    public void appendErrors(List<String> errors) {
        if (errors.isEmpty()) return;
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> appendErrors(errors));
            return;
        }

        String timestamp = LocalTime.now().format(TIME_FORMAT);
        for (String error : errors) {
            Label time = new Label(timestamp);
            time.setStyle("-fx-text-fill: #666666; -fx-font-size: 10px;");

            Label message = new Label(error);
            message.setWrapText(true);
            message.setMaxWidth(Double.MAX_VALUE);

            VBox entry = new VBox(3, time, message);
            entry.setPadding(new Insets(8));
            entry.setStyle("-fx-background-color: white; -fx-border-color: #dddddd; -fx-border-radius: 3; -fx-background-radius: 3;");
            errorList.getChildren().add(entry);
            errorCount++;
        }

        //prajw : start
        // Keep the count accurate and enable clearing after adding a new batch of errors.
        title.setText("Errors (" + errorCount + ")");
        clearButton.setDisable(false);
        //prajw: end
        setExpanded(true);
        Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }

    //prajw : start
    // Clear removes only the displayed history, resets its count and disables itself until new errors arrive.
    private void clearErrors() {
        errorList.getChildren().clear();
        errorCount = 0;
        title.setText("Errors (0)");
        clearButton.setDisable(true);
        setExpanded(false);                         // close the box
        if (onCleared != null) onCleared.run();     // remove the highlights
    }
    //prajw: end

    // Resizes the top-right overlay so opening it extends left and downward.
    private void setExpanded(boolean expanded) {
        this.expanded = expanded;
        title.setVisible(expanded);
        title.setManaged(expanded);
        clearButton.setVisible(expanded);
        clearButton.setManaged(expanded);
        scrollPane.setVisible(expanded);
        scrollPane.setManaged(expanded);
        toggleButton.setText(expanded ? "⌃" : (errorCount > 0 ? "!" : "⌄"));
        toggleButton.setAccessibleText(expanded ? "Collapse validation errors" : "Open validation errors");
        toggleButton.setTooltip(new Tooltip(expanded
                ? "Collapse error history"
                : "Open error history (" + errorCount + " errors)"));

        double width = expanded ? OPEN_WIDTH : COLLAPSED_SIZE;
        double height = expanded ? OPEN_HEIGHT : COLLAPSED_SIZE;
        setMinSize(width, height);
        setPrefSize(width, height);
        setMaxSize(width, height);
    }

    public void setOnCleared(Runnable onCleared) {
        this.onCleared = onCleared;
    }
}
//prajw: end


