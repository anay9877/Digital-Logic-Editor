package com.example.mini_project.controller.command;

import com.example.mini_project.domain.InputElement;
import com.example.mini_project.domain.SignalState;

public final class ToggleInputCommand implements Command {
    private final InputElement input;
    private final SignalState before;
    private final SignalState after;

    public ToggleInputCommand(InputElement input, SignalState before, SignalState after) {
        this.input = input;
        this.before = before;
        this.after = after;
    }

    @Override
    public void undo() {
        input.setValue(before);
    }

    @Override
    public void redo() {
        input.setValue(after);
    }

    @Override
    public String label() {
        return "Toggle " + input.getId();
    }
}
