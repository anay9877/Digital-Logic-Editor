package com.example.mini_project.controller.command;

public interface Command {
    void undo();
    void redo();
    String label();
}
