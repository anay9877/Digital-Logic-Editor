package com.example.mini_project.gui;

import com.example.mini_project.domain.ComponentType;
import com.example.mini_project.domain.SvgIconRegistry;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class CircuitElementPalette extends VBox {
    private static final double ICON_SIZE = 48;
    private static final String STYLE_SELECTED = "-fx-border-color: #1A73E8; -fx-border-width: 2; -fx-background-color: #E8F0FE;";

    private final SvgIconRegistry iconRegistry = new SvgIconRegistry();
    private final Map<ComponentType, Button> buttons = new EnumMap<>(ComponentType.class);
    private ComponentType selected;
    private Runnable onUserSelect;

    public CircuitElementPalette() {
        for (ComponentType t : ComponentType.values()) {
            if (t == ComponentType.UNKNOWN) continue;

            Button b = new Button();
            b.setGraphic(iconRegistry.createPaletteIcon(t, ICON_SIZE));
            b.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            b.setTooltip(new Tooltip(t.name()));          // name still available on hover
            b.setPrefSize(ICON_SIZE + 16, ICON_SIZE + 16);
            b.setOnAction(e -> {
                selectComponentType(t);
                if (onUserSelect != null) onUserSelect.run();
            });
            buttons.put(t, b);
            getChildren().add(b);
        }
    }

    public void setOnUserSelect(Runnable r) { this.onUserSelect = r; }

    public void selectComponentType(ComponentType type) {
        this.selected = type;
        buttons.forEach((t, b) -> b.setStyle(t == selected ? STYLE_SELECTED : ""));
    }

    public ComponentType getSelected() { return selected; }
}