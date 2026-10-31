package com.example.mini_project.gui;

import com.example.mini_project.controller.EditorController;
import com.example.mini_project.domain.Circuit;
import com.example.mini_project.persistence.*;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Alert;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.scene.input.KeyCodeCombination;
import javafx.stage.FileChooser;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
        //prajw : start
        ValidationErrorPanel validationErrorPanel = new ValidationErrorPanel();
        controller.setOnValidationErrors(validationErrorPanel::appendErrors);
        //prajw: end
        controller.setCanvas(canvas);
        canvas.setPalette(palette);
        canvas.setToolbar(toolbar);

        BorderPane root = new BorderPane();
        MenuBar menuBar = createMenuBar(stage, circuit, controller, canvas);
        VBox topContainer = new VBox(menuBar, toolbar);

        root.setTop(topContainer);
        //prajw : start
        StackPane canvasArea = new StackPane(canvas, validationErrorPanel);
        StackPane.setAlignment(validationErrorPanel, Pos.TOP_RIGHT);
        StackPane.setMargin(validationErrorPanel, new Insets(10));
        root.setLeft(palette);
        root.setCenter(canvasArea);
        //prajw: end
        root.setStyle("-fx-font-size: 13px;");
        palette.setStyle("-fx-padding: 10; -fx-spacing: 6; -fx-background-color: #eeeeee;");

        Scene scene = new Scene(root, 1200, 700);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (!e.isControlDown()) return;
            if (e.getCode() == KeyCode.PLUS || e.getCode() == KeyCode.ADD)
                canvas.zoomIn();
            else if (e.getCode() == KeyCode.SUBTRACT || e.getCode() == KeyCode.MINUS)
                canvas.zoomOut();
            e.consume();
        });
        // Removing the elements selected from the list when ESC key is pressed -> basically calls clear() method from SelectionManager
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                controller.getSelection().clear();
            } else if (e.getCode() == KeyCode.DELETE || e.getCode() == KeyCode.BACK_SPACE) {
                controller.deleteSelected();
                e.consume();
            } else if (e.isShortcutDown() && e.getCode() == KeyCode.Z) {
                if (e.isShiftDown()) controller.redo();
                else controller.undo();
                e.consume();
            } else if (e.isShortcutDown() && e.getCode() == KeyCode.Y) {
                controller.redo();
                e.consume();
            }
        });
        stage.setScene(scene);
        stage.setTitle("Digital Logic Editor");
        stage.setOnCloseRequest(e -> TruthTableWindow.closeIfOpen());
        stage.show();
    }

    // The below code is only for Persistence Layer

    private java.io.File currentFile = null;

    // Creating Menubar on javafx.scene
    // Adding a Menu named File to Menubar
    private MenuBar createMenuBar(Stage stage, Circuit circuit, EditorController controller, CircuitCanvas canvas) {
        MenuBar menuBar = new MenuBar();
        Menu fileMenu = new Menu("File");

        // Defining New
        // On action it would delete all the elements on the canvas by calling newCircuit()
        // NOTE -> These are the permanent changes to actual canvas.
        //         Even undo and redo cannot bring it back!!
        // A very new circuit canvas is generated
        MenuItem newItem = new MenuItem("New");
        newItem.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN));
        newItem.setOnAction(e -> {
            controller.newCircuit();
            currentFile = null;
            // stage.setTitle("Digital Logic Editor");
        });

        // This is for opening a file which is saved with json extension
        // Select the file using FileChooser (this is a javafx.stage class)
        // After choosing a file pass it to CircuitSerializer.loadJson
        // If any exception catch and throw it by showError
        MenuItem openItem = new MenuItem("Open / Load...");
        openItem.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN));
        openItem.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Open Circuit JSON");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Circuit Files (*.json)", "*.json"));
            java.io.File file = fc.showOpenDialog(stage);
            if (file != null) {
                try {
                    CircuitSerializer.loadJson(file, circuit, canvas.getViewportManager(), controller);
                    currentFile = file;
                    stage.setTitle("Digital Logic Editor - " + file.getName());
                } catch (Exception ex) {
                    showError("Failed to load circuit", ex.getMessage());
                }
            }
        });

        // To Save a file
        // You have never saved your file before then call saveAs
        // If you have saved the file before then overwrite in the same file
        MenuItem saveItem = new MenuItem("Save");
        saveItem.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));
        saveItem.setOnAction(e -> {
            if (currentFile != null) {
                try {
                    CircuitSerializer.saveJson(currentFile, circuit, canvas.getViewportManager());
                } catch (Exception ex) {
                    showError("Failed to save circuit", ex.getMessage());
                }
            } else {
                saveAs(stage, circuit, canvas);
            }
        });

        // Create a complete different copy of the current file
        // It acts like commiting changes in a file so we can revisit it
        MenuItem saveAsItem = new MenuItem("Save As...");
        saveAsItem.setOnAction(e -> saveAs(stage, circuit, canvas));

        // Imports SVG
        // Calls importSVG function from CircuitSerializer
        MenuItem importSvgItem = new MenuItem("Import SVG...");
        importSvgItem.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Import Circuit from SVG");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("SVG Files (*.svg)", "*.svg"));
            java.io.File file = fc.showOpenDialog(stage);
            if (file != null) {
                try {
                    CircuitSerializer.importSvg(file, circuit, canvas.getViewportManager(), controller);
                    currentFile = null;
                    stage.setTitle("Digital Logic Editor - " + file.getName());
                } catch (Exception ex) {
                    showError("Failed to import SVG", ex.getMessage());
                }
            }
        });

        // Exports SVG
        // Calls exportSVG function from CircuitSerializer
        MenuItem exportSvgItem = new MenuItem("Export SVG...");
        exportSvgItem.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Export Circuit as SVG");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("SVG Files (*.svg)", "*.svg"));
            java.io.File file = fc.showSaveDialog(stage);
            if (file != null) {
                try {
                    CircuitSerializer.exportSvg(file, circuit, canvas.getViewportManager());
                } catch (Exception ex) {
                    showError("Failed to export SVG", ex.getMessage());
                }
            }
        });

        // Interesting option which closes the stage itself!!
        // Alternative option for closing the current circuit window
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> stage.close());

        // SeparatorMenuItem adds a horizontal line
        fileMenu.getItems().addAll(
                newItem, openItem, saveItem, saveAsItem,
                new SeparatorMenuItem(),
                importSvgItem, exportSvgItem,
                new SeparatorMenuItem(),
                exitItem
        );
        menuBar.getMenus().add(fileMenu);
        return menuBar;
    }

    // Only for saving Json files
    private void saveAs(Stage stage, Circuit circuit, CircuitCanvas canvas) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save Circuit JSON");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Circuit Files (*.json)", "*.json"));
        java.io.File file = fc.showSaveDialog(stage);
        if (file != null) {
            try {
                CircuitSerializer.saveJson(file, circuit, canvas.getViewportManager());
                currentFile = file;
                stage.setTitle("Digital Logic Editor - " + file.getName());
            } catch (Exception ex) {
                showError("Failed to save circuit", ex.getMessage());
            }
        }
    }

    // The exception thrown is caught and showError function is called
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
