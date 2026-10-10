package com.example.mini_project.gui;

import com.example.mini_project.controller.EditorController;
import com.example.mini_project.domain.CircuitBoard;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class EditorWindow extends Application {

    @Override
    public void start(Stage stage) {
        CircuitBoard circuitBoard = new CircuitBoard();
        EditorController controller = new EditorController(circuitBoard);
        EditorToolbar toolbar = new EditorToolbar(circuitBoard);

        CircuitElementPalette palette = new CircuitElementPalette();
        CircuitCanvas canvas = new CircuitCanvas(controller, 1200, 700);
        canvas.setPalette(palette);
        canvas.setToolbar(toolbar);

        BorderPane root = new BorderPane();
        root.setTop(toolbar);
        root.setLeft(palette);
        root.setCenter(canvas);
        root.setStyle("-fx-font-size: 13px;");
        palette.setStyle("-fx-padding: 10; -fx-spacing: 6; -fx-background-color: #eeeeee;");

        Scene scene = new Scene(root, 1100, 700);
        stage.setScene(scene);
        stage.setTitle("Digital Logic Editor");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
