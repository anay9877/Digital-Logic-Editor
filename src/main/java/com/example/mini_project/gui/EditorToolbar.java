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

    public EditorToolbar(Circuit circuit) {
        this.circuit = circuit;

        // this.selected = ToolbarOptions.Select;
        setSpacing(8);
        setStyle("-fx-padding: 10; -fx-background-color: #dddddd;");

        for (ToolbarOptions t : ToolbarOptions.values()) {
            if (t == ToolbarOptions.Unknown) {
                continue;
            }
            toolbarOptions.add(t);
            Button b = new Button(t.name());
            b.setOnAction(e -> {
                selectToolbarOption(t);
                if (onUserSelect != null)
                    onUserSelect.run();
            });
            getChildren().add(b);
        }
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
}
