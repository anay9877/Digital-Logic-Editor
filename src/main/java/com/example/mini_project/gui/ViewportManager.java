package com.example.mini_project.gui;

import javafx.scene.Group;

public class ViewportManager {
    private final Group view;
    private double zoom = 1.0;
    private static final double MIN_ZOOM = 0.25;
    private static final double MAX_ZOOM = 4.0;
    private static final double ZOOM_STEP = 0.05;

    private double translateX = 0.0;
    private double translateY = 0.0;
    public ViewportManager(Group view) {
        this.view = view;
    }

    private void applyZoom() {
        view.setScaleX(zoom);
        view.setScaleY(zoom);
    }

    public void zoomIn() {
        if (zoom < MAX_ZOOM) {
            zoom += ZOOM_STEP;
            applyZoom();
        }
    }
    
    public void zoomOut() {
        if (zoom > MIN_ZOOM) {
            zoom -= ZOOM_STEP;
            applyZoom();
        }
    }

    public double getZoom() {
        return this.zoom;
    }
    public void translate(double dx, double dy) {
        translateX += dx;
        translateY += dy;
        view.setTranslateX(translateX);
        view.setTranslateY(translateY);
    }
}