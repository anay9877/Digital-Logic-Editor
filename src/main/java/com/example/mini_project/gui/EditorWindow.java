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
        canvas.setPalette(palette);
        canvas.setToolbar(toolbar);

        BorderPane root = new BorderPane();
        root.setTop(toolbar);
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

    public static void main(String[] args) {
        launch(args);
    }
}
