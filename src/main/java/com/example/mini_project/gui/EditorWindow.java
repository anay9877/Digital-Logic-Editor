package com.example.mini_project.gui;

import com.example.mini_project.controller.EditorController;
import com.example.mini_project.domain.Circuit;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
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
        scene.addEventFilter(KeyEvent.KEY_PRESSED,e -> {
            if (!e.isControlDown()) return;
            if (e.getCode() == KeyCode.PLUS || e.getCode() == KeyCode.ADD)
                canvas.zoomIn();
            else if (e.getCode() == KeyCode.SUBTRACT || e.getCode() == KeyCode.MINUS)
                canvas.zoomOut();
            e.consume();
        });
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

    // The below code is only for Persistence Layer

    private java.io.File currentFile = null;

    // Creating Menubar on javafx.scene
    // Adding a Menu named File to Menubar
    private javafx.scene.control.MenuBar createMenuBar(Stage stage, Circuit circuit, EditorController controller, CircuitCanvas canvas) {
        javafx.scene.control.MenuBar menuBar = new javafx.scene.control.MenuBar();
        javafx.scene.control.Menu fileMenu = new javafx.scene.control.Menu("File");

        // Defining New
        // On action it would delete all the elements on the canvas by calling newCircuit()
        // NOTE -> These are the permanent changes to actual canvas.
        //         Even undo and redo cannot bring it back!!
        // A very new circuit canvas is generated
        javafx.scene.control.MenuItem newItem = new javafx.scene.control.MenuItem("New");
        newItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.N, javafx.scene.input.KeyCombination.CONTROL_DOWN));
        newItem.setOnAction(e -> {
            controller.newCircuit();
            currentFile = null;
            // stage.setTitle("Digital Logic Editor");
        });

        // This is for opening a file which is saved with json extension
        // Select the file using FileChooser (this is a javafx.stage class)
        // After choosing a file pass it to CircuitSerializer.loadJson
        // If any exception catch and throw it by showError
        javafx.scene.control.MenuItem openItem = new javafx.scene.control.MenuItem("Open / Load...");
        openItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.O, javafx.scene.input.KeyCombination.CONTROL_DOWN));
        openItem.setOnAction(e -> {
            FileChooser fc = new FileChooser();
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

        // To Save a file
        // You have never saved your file before then call savaAs
        // If you have saved the file before then overwrite in the same file
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

        // Create a complete different copy of the current file
        // It acts like commiting changes in a file so we can revisit it
        javafx.scene.control.MenuItem saveAsItem = new javafx.scene.control.MenuItem("Save As...");
        saveAsItem.setOnAction(e -> saveAs(stage, circuit, canvas));

        // Imports SVG
        // Calls importSVG function from CircuitSerializer
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

        // Exports SVG
        // Calls exportSVG function from CircuitSerializer
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

        // Interesting option which closes the stage itself!!
        // Alternative option for closing the current circuit window
        javafx.scene.control.MenuItem exitItem = new javafx.scene.control.MenuItem("Exit");
        exitItem.setOnAction(e -> stage.close());

        // SeparatorMenuItem adds a horizontal line
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

    // Only for saving Json files
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

    // The exception thrown is caught and showError function is called
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
