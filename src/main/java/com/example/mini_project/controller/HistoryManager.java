package com.example.mini_project.controller;

import java.util.ArrayDeque;
import java.util.Deque;

public class HistoryManager {
    private static final int  MAX_STEPS = 100;
    private static final long MAX_CHARS = 50_000_000L;

    private final Deque<String> undoStack = new ArrayDeque<>();
    private final Deque<String> redoStack = new ArrayDeque<>();
    private String current;                 // snapshot of what is on screen now
    private long undoChars;
    private Runnable onChange;

    public void setOnChange(Runnable r) { this.onChange = r; }

    public void reset(String baseline) {
        undoStack.clear(); redoStack.clear(); undoChars = 0;
        current = baseline;
        fire();
    }

    public void record(String after) {
        if (after == null || after.equals(current)) return;   // no-op: nothing changed
        if (current != null) pushUndo(current);
        redoStack.clear();                                    // a new action ends the redo chain
        current = after;
        trim();
        fire();
    }

    public String undo() {
        if (undoStack.isEmpty() || current == null) return null;
        redoStack.addFirst(current);
        current = popUndo();
        fire();
        return current;
    }

    public String redo() {
        if (redoStack.isEmpty() || current == null) return null;
        pushUndo(current);
        current = redoStack.removeFirst();
        trim();
        fire();
        return current;
    }

    public boolean canUndo() { return !undoStack.isEmpty(); }
    public boolean canRedo() { return !redoStack.isEmpty(); }

    private void pushUndo(String s) { undoStack.addFirst(s); undoChars += s.length(); }
    private String popUndo() { String s = undoStack.removeFirst(); undoChars -= s.length(); return s; }

    private void trim() {
        while (undoStack.size() > MAX_STEPS
                || (undoChars > MAX_CHARS && undoStack.size() > 1)) {
            undoChars -= undoStack.removeLast().length();     // drop the oldest step
        }
    }

    private void fire() { if (onChange != null) onChange.run(); }
}