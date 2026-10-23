package com.example.mini_project.controller;

import com.example.mini_project.controller.command.Command;

import java.util.ArrayDeque;
import java.util.Deque;

/** Manages undo and redo actions that have already been applied to the circuit. */
public class HistoryManager {
    private static final int MAX_STEPS = 100;

    private final Deque<Command> undoStack = new ArrayDeque<>();
    private final Deque<Command> redoStack = new ArrayDeque<>();
    private Runnable onChange;

    public void setOnChange(Runnable onChange) {
        this.onChange = onChange;
    }

    /** Clears history when a new circuit is created or a file is loaded. */
    public void clear() {
        undoStack.clear();
        redoStack.clear();
        fire();
    }

    /** Registers an action that has already been applied to the circuit. */
    public void record(Command command) {
        if (command == null) return;
        undoStack.addFirst(command);
        redoStack.clear();
        while (undoStack.size() > MAX_STEPS) {
            undoStack.removeLast();
        }
        fire();
    }

    /** Reverts the latest action, returning false when no action is available. */
    public boolean undo() {
        if (undoStack.isEmpty()) return false;
        Command command = undoStack.removeFirst();
        try {
            command.undo();
        } catch (RuntimeException ex) {
            clear();
            throw ex;
        }
        redoStack.addFirst(command);
        fire();
        return true;
    }

    /** Reapplies the latest undone action, returning false when none is available. */
    public boolean redo() {
        if (redoStack.isEmpty()) return false;
        Command command = redoStack.removeFirst();
        try {
            command.redo();
        } catch (RuntimeException ex) {
            clear();
            throw ex;
        }
        undoStack.addFirst(command);
        fire();
        return true;
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    /** Returns the label of the action that undo would revert, or null. */
    public String peekUndoLabel() {
        return undoStack.isEmpty() ? null : undoStack.peekFirst().label();
    }

    /** Returns the label of the action that redo would reapply, or null. */
    public String peekRedoLabel() {
        return redoStack.isEmpty() ? null : redoStack.peekFirst().label();
    }

    private void fire() {
        if (onChange != null) onChange.run();
    }
}
