package com.example.mini_project.controller.command;

import java.util.List;

public final class CompositeCommand implements Command {
    private final String label;
    private final List<Command> parts;

    public CompositeCommand(String label, List<Command> parts) {
        this.label = label;
        this.parts = List.copyOf(parts);
    }

    @Override
    public void undo() {
        for (int i = parts.size() - 1; i >= 0; i--) parts.get(i).undo();
    }

    @Override
    public void redo() {
        for (Command part : parts) part.redo();
    }

    @Override
    public String label() {
        return label;
    }
}
