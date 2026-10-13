package com.example.mini_project.gui;

import javafx.scene.Group;
import javafx.scene.transform.Scale;

/**
 * Owns the zoom / pan state of the world Group.
 *
 * screen = translate + zoom * world   (scale pivot is the world origin, so the maths
 * is the same no matter how big the group's bounds are).
 *
 * Every time the view changes, {@code onChange} is run so the screen-space grid can redraw.
 */
public class ViewportManager {
    private final Group view;
    private final Scale scale = new Scale(1, 1, 0, 0);
    private Runnable onChange = () -> {};

    private double zoom = 1.0;
    private static final double MIN_ZOOM = 0.25;
    private static final double MAX_ZOOM = 4.0;
    private static final double ZOOM_STEP = 0.05;

    private double translateX = 0.0;
    private double translateY = 0.0;

    // Screen point that zoom buttons zoom around (kept = centre of the visible canvas).
    private double anchorX = 0.0;
    private double anchorY = 0.0;

    public ViewportManager(Group view) {
        this.view = view;
        view.getTransforms().add(scale);
    }

    public void setOnChange(Runnable r) { this.onChange = r == null ? () -> {} : r; }

    public void setAnchor(double x, double y) { this.anchorX = x; this.anchorY = y; }

    private void apply() {
        scale.setX(zoom);
        scale.setY(zoom);
        view.setTranslateX(translateX);
        view.setTranslateY(translateY);
        onChange.run();
    }

    private void zoomTo(double newZoom) {
        newZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, newZoom));
        if (newZoom == zoom) return;
        // keep the world point under the anchor fixed on screen
        double wx = (anchorX - translateX) / zoom;
        double wy = (anchorY - translateY) / zoom;
        zoom = newZoom;
        translateX = anchorX - wx * zoom;
        translateY = anchorY - wy * zoom;
        apply();
    }

    public void zoomIn()  { zoomTo(zoom + ZOOM_STEP); }
    public void zoomOut() { zoomTo(zoom - ZOOM_STEP); }

    public double getZoom()       { return zoom; }
    public double getTranslateX() { return translateX; }
    public double getTranslateY() { return translateY; }

    public void translate(double dx, double dy) {
        translateX += dx;
        translateY += dy;
        apply();
    }
}