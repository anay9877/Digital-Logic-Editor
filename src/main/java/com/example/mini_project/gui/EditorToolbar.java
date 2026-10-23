package com.example.mini_project.gui;

import com.example.mini_project.domain.Circuit;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

import java.util.ArrayList;
import java.util.List;

public class EditorToolbar extends HBox {
    private final Circuit circuit;
    private final List<ToolbarOptions> toolbarOptions = new ArrayList<>();
    private ToolbarOptions selected = ToolbarOptions.Select;
    private Runnable onUserSelect;
    private Runnable onZoomIn;
    private Runnable onZoomOut;
    //prajwal
    private Runnable onDelete;
    //undo redo
    private Runnable onUndo;
    private Runnable onRedo;
    private Button undoButton;
    private Button redoButton;
    private Runnable onSimulate;

    public EditorToolbar(Circuit circuit) {
        this.circuit = circuit;

        setSpacing(8);
        setStyle("-fx-padding: 10; -fx-background-color: #dddddd;");

        for (ToolbarOptions t : ToolbarOptions.values()) {
            if (t == ToolbarOptions.Unknown) {
                continue;
            }
            toolbarOptions.add(t);
            Button button = new Button(t.name());
            if (t == ToolbarOptions.Undo) undoButton = button; //prajwal "undo"
            if (t == ToolbarOptions.Redo) redoButton = button; //prajwal "redo"
            button.setOnAction(e -> {
                if (t == ToolbarOptions.Zoom_In) {
                    if(onZoomIn != null) {
                        onZoomIn.run();
                    }
                    return;
                }
                if (t == ToolbarOptions.Zoom_Out) {
                    if(onZoomOut != null) {
                        onZoomOut.run();
                    }
                    return;
                }
                if (t == ToolbarOptions.Delete) {
                    if (onDelete != null) onDelete.run();
                    return;                       // keep the current tool and palette state
                }
                //prajwal undo redo
                if (t == ToolbarOptions.Undo) {
                    if (onUndo != null) onUndo.run();
                    return;
                }
                if (t == ToolbarOptions.Redo) {
                    if (onRedo != null) onRedo.run();
                    return;
                }
                if(t == ToolbarOptions.Simulate){
                    if(onSimulate != null) onSimulate.run();
                    return;
                }
                selectToolbarOption(t);
                if (onUserSelect != null) {
                    onUserSelect.run();
                }
            });
            getChildren().add(button);
        }
    }

    //setters getters for undo redo
    public void setOnUndo(Runnable action) { 
        this.onUndo = action; 
    }
    public void setOnRedo(Runnable action) { 
        this.onRedo = action; 
    }

    public void setHistoryState(boolean canUndo, boolean canRedo) {
        if (undoButton != null) undoButton.setDisable(!canUndo);
        if (redoButton != null) redoButton.setDisable(!canRedo);
    }

    public void setOnDelete(Runnable action) {
        this.onDelete = action;
    }

    public void setOnSimulate(Runnable action){
        this.onSimulate = action;
    }

    public void setOnUserSelect(Runnable r) {
        this.onUserSelect = r;
    }

    public void selectToolbarOption(ToolbarOptions toolbarOption) {
        this.selected = toolbarOption;
    }

    public ToolbarOptions getSelected() {
        return this.selected;
    }

    public void setOnZoomIn(Runnable action) {
        this.onZoomIn = action;
    }
    public void setOnZoomOut(Runnable action) {
        this.onZoomOut = action;
    }

    
}
