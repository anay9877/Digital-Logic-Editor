package com.example.mini_project.gui;

import com.example.mini_project.domain.Circuit;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

import java.util.ArrayList;
import java.util.List;

public class EditorToolbar extends HBox {
    private final Circuit circuit;
    private final List<ToolbarOptions> toolbarOptions = new ArrayList<>();
    private ToolbarOptions selected;
    private Runnable onUserSelect;
    private Runnable onZoomIn;
    private Runnable onZoomOut;

    public EditorToolbar(Circuit circuit){
        this.circuit = circuit;

        // this.selected = ToolbarOptions.Select;
        setSpacing(8);
        setStyle("-fx-padding: 10; -fx-background-color: #dddddd;");

        for(ToolbarOptions t : ToolbarOptions.values()){
            if(t == ToolbarOptions.Unknown){
                continue;
            }
            toolbarOptions.add(t);
            Button button = new Button(t.name());
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
                selectToolbarOption(t);
                if (onUserSelect != null) {
                    onUserSelect.run();
                }
            });
            getChildren().add(button);
        }
    }

    public void setOnUserSelect(Runnable r){
        this.onUserSelect = r;
    }

    public void selectToolbarOption(ToolbarOptions toolbarOption){
        this.selected = toolbarOption;
    }

    public ToolbarOptions getSelected(){
        return this.selected;
    }

    public void setOnZoomIn(Runnable action) {
        this.onZoomIn = action;
    }
    public void setOnZoomOut(Runnable action) {
        this.onZoomOut = action;
    }
}
