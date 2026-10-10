package com.example.mini_project.domain;

import javafx.scene.Node;
import javafx.scene.canvas.GraphicsContext;

public abstract class CircuitElement {
    protected final String id;
    protected double x, y;

    protected CircuitElement(String id, double x, double y) {
        this.id = id;
        this.x = x;
        this.y = y;
    }

    public String getId() {
        return id;
    }
    public double getX() {
        return x;
    }
    public double getY() {
        return y;
    }

    public double getLength(){
        return SvgIconRegistry.GATE_WIDTH;
    }

    public void moveBy(double dx, double dy){
        this.x += dx;
        this.y += dy;
    }

    public boolean hitTest(double px, double py) {
        // to be implemented
        // takes 2 coordinates and detects which block is selected by rounding up or down
        return false;
    }

    public abstract Node getIconNode(SvgIconRegistry registry);

    public abstract Port findPort(String portId);
}
