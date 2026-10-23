package com.example.mini_project.controller.command;

import com.example.mini_project.domain.CircuitElement;
import java.util.List;

public final class MoveElementsCommand implements Command {
    public record Move(CircuitElement element, double oldX, double oldY, double newX, double newY) {}

    private final List<Move> moves;

    public MoveElementsCommand(List<Move> moves) {
        this.moves = List.copyOf(moves);
    }

    @Override
    public void undo() {
        for (Move move : moves) move.element().setPosition(move.oldX(), move.oldY());
    }

    @Override
    public void redo() {
        for (Move move : moves) move.element().setPosition(move.newX(), move.newY());
    }

    @Override
    public String label() {
        return "Move " + moves.size() + " element(s)";
    }
}
