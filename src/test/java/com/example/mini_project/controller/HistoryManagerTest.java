package com.example.mini_project.controller;

import com.example.mini_project.controller.command.Command;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HistoryManagerTest {

    private static Command logging(List<String> log, String name) {
        return new Command() {
            @Override
            public void undo() {
                log.add("undo " + name);
            }

            @Override
            public void redo() {
                log.add("redo " + name);
            }

            @Override
            public String label() {
                return name;
            }
        };
    }

    @Test
    void undoAndRedoRunCommandsInOrder() {
        List<String> log = new ArrayList<>();
        HistoryManager history = new HistoryManager();
        history.record(logging(log, "A"));
        history.record(logging(log, "B"));

        assertTrue(history.undo());
        assertTrue(history.undo());
        assertFalse(history.undo());
        assertTrue(history.redo());
        assertEquals(List.of("undo B", "undo A", "redo A"), log);
    }

    @Test
    void newActionClearsRedoChain() {
        List<String> log = new ArrayList<>();
        HistoryManager history = new HistoryManager();
        history.record(logging(log, "A"));
        assertTrue(history.undo());
        assertTrue(history.canRedo());

        history.record(logging(log, "B"));
        assertFalse(history.canRedo());
    }

    @Test
    void historyIsCappedAndClearable() {
        HistoryManager history = new HistoryManager();
        for (int i = 0; i < 250; i++) {
            history.record(logging(new ArrayList<>(), "n" + i));
        }

        int steps = 0;
        while (history.undo()) steps++;
        assertEquals(100, steps);

        history.clear();
        assertFalse(history.canUndo());
        assertFalse(history.canRedo());
    }

    @Test
    void labelsTrackNextUndoAndRedoActions() {
        HistoryManager history = new HistoryManager();
        history.record(logging(new ArrayList<>(), "Place element"));
        history.record(logging(new ArrayList<>(), "Draw wire"));

        assertEquals("Draw wire", history.peekUndoLabel());
        assertTrue(history.undo());
        assertEquals("Draw wire", history.peekRedoLabel());
        assertEquals("Place element", history.peekUndoLabel());
    }
}
