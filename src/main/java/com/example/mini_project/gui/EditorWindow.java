package com.example.mini_project.gui;

import com.example.mini_project.controller.EditorController;
import com.example.mini_project.domain.Circuit;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class EditorWindow extends Application {

    @Override
    public void start(Stage stage) {
        Circuit circuit = new Circuit();
        EditorController controller = new EditorController(circuit);
        EditorToolbar toolbar = new EditorToolbar(circuit);

        CircuitElementPalette palette = new CircuitElementPalette();
        CircuitCanvas canvas = new CircuitCanvas(controller);
        controller.setCanvas(canvas);
        canvas.setPalette(palette);
        canvas.setToolbar(toolbar);

        BorderPane root = new BorderPane();
        javafx.scene.control.MenuBar menuBar = createMenuBar(stage, circuit, controller, canvas);
        javafx.scene.layout.VBox topContainer = new javafx.scene.layout.VBox(menuBar, toolbar);

        root.setTop(topContainer);
        root.setLeft(palette);
        root.setCenter(canvas);
        root.setStyle("-fx-font-size: 13px;");
        palette.setStyle("-fx-padding: 10; -fx-spacing: 6; -fx-background-color: #eeeeee;");

        Scene scene = new Scene(root, 1200, 700);
        // Removing the elements selected from the list when ESC key is pressed -> basically calls clear() method from SelectionManager
        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                controller.getSelection().clear();
            }
        });
        stage.setScene(scene);
        stage.setTitle("Digital Logic Editor");
        stage.show();
    }

    private java.io.File currentFile = null;

    private javafx.scene.control.MenuBar createMenuBar(Stage stage, Circuit circuit, EditorController controller, CircuitCanvas canvas) {
        javafx.scene.control.MenuBar menuBar = new javafx.scene.control.MenuBar();
        javafx.scene.control.Menu fileMenu = new javafx.scene.control.Menu("File");

        javafx.scene.control.MenuItem newItem = new javafx.scene.control.MenuItem("New");
        newItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.N, javafx.scene.input.KeyCombination.CONTROL_DOWN));
        newItem.setOnAction(e -> {
            controller.newCircuit();
            currentFile = null;
            stage.setTitle("Digital Logic Editor");
        });

        javafx.scene.control.MenuItem openItem = new javafx.scene.control.MenuItem("Open / Load...");
        openItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.O, javafx.scene.input.KeyCombination.CONTROL_DOWN));
        openItem.setOnAction(e -> {
            javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
            fc.setTitle("Open Circuit JSON");
            fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Circuit Files (*.json)", "*.json"));
            java.io.File file = fc.showOpenDialog(stage);
            if (file != null) {
                try {
                    com.example.mini_project.persistence.CircuitSerializer.loadJson(file, circuit, canvas.getViewportManager(), controller);
                    currentFile = file;
                    stage.setTitle("Digital Logic Editor - " + file.getName());
                } catch (Exception ex) {
                    showError("Failed to load circuit", ex.getMessage());
                }
            }
        });

        javafx.scene.control.MenuItem saveItem = new javafx.scene.control.MenuItem("Save");
        saveItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.S, javafx.scene.input.KeyCombination.CONTROL_DOWN));
        saveItem.setOnAction(e -> {
            if (currentFile != null) {
                try {
                    com.example.mini_project.persistence.CircuitSerializer.saveJson(currentFile, circuit, canvas.getViewportManager());
                } catch (Exception ex) {
                    showError("Failed to save circuit", ex.getMessage());
                }
            } else {
                saveAs(stage, circuit, canvas);
            }
        });

        javafx.scene.control.MenuItem saveAsItem = new javafx.scene.control.MenuItem("Save As...");
        saveAsItem.setOnAction(e -> saveAs(stage, circuit, canvas));

        javafx.scene.control.MenuItem importSvgItem = new javafx.scene.control.MenuItem("Import SVG...");
        importSvgItem.setOnAction(e -> {
            javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
            fc.setTitle("Import Circuit from SVG");
            fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("SVG Files (*.svg)", "*.svg"));
            java.io.File file = fc.showOpenDialog(stage);
            if (file != null) {
                try {
                    com.example.mini_project.persistence.CircuitSerializer.importSvg(file, circuit, canvas.getViewportManager(), controller);
                    currentFile = null;
                    stage.setTitle("Digital Logic Editor - " + file.getName());
                } catch (Exception ex) {
                    showError("Failed to import SVG", ex.getMessage());
                }
            }
        });

        javafx.scene.control.MenuItem exportSvgItem = new javafx.scene.control.MenuItem("Export SVG...");
        exportSvgItem.setOnAction(e -> {
            javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
            fc.setTitle("Export Circuit as SVG");
            fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("SVG Files (*.svg)", "*.svg"));
            java.io.File file = fc.showSaveDialog(stage);
            if (file != null) {
                try {
                    com.example.mini_project.persistence.CircuitSerializer.exportSvg(file, circuit, canvas.getViewportManager());
                } catch (Exception ex) {
                    showError("Failed to export SVG", ex.getMessage());
                }
            }
        });

        javafx.scene.control.MenuItem exitItem = new javafx.scene.control.MenuItem("Exit");
        exitItem.setOnAction(e -> stage.close());

        fileMenu.getItems().addAll(
                newItem, openItem, saveItem, saveAsItem,
                new javafx.scene.control.SeparatorMenuItem(),
                importSvgItem, exportSvgItem,
                new javafx.scene.control.SeparatorMenuItem(),
                exitItem
        );
        menuBar.getMenus().add(fileMenu);
        return menuBar;
    }

    private void saveAs(Stage stage, Circuit circuit, CircuitCanvas canvas) {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Save Circuit JSON");
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Circuit Files (*.json)", "*.json"));
        java.io.File file = fc.showSaveDialog(stage);
        if (file != null) {
            try {
                com.example.mini_project.persistence.CircuitSerializer.saveJson(file, circuit, canvas.getViewportManager());
                currentFile = file;
                stage.setTitle("Digital Logic Editor - " + file.getName());
            } catch (Exception ex) {
                showError("Failed to save circuit", ex.getMessage());
            }
        }
    }

    private void showError(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
