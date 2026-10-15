package com.example.mini_project.controller;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class SelectionManager {
    private final Set<String> selectedElements = new LinkedHashSet<>();
    private final Set<String> selectedWires = new LinkedHashSet<>();
    private Runnable onChange;   // canvas sets this to call render()

    public void setOnChange(Runnable r) {
        this.onChange = r;
    }

    public void selectElement(String id, boolean additive) {
        if (!additive)
            clearSilently();
        if (!selectedElements.add(id) && additive)
            selectedElements.remove(id); // shift-click toggles
        fire();
    }

    public void selectElements(Collection<String> ids, boolean additive){
        if(!additive) clearSilently();
        selectedElements.addAll(ids);
        fire();
    }

    public void selectWire(String id, boolean additive) {
        if(!additive)
            clearSilently();
        if(!selectedWires.add(id) && additive)
            selectedWires.remove(id);
        fire();
    }

    public void selectWires(Collection<String> ids, boolean additive) {
        if (!additive) clearSilently();
        selectedWires.addAll(ids);
        fire();
    }

    public void clear() {
        clearSilently();
        fire();
    }

    public boolean isElementSelected(String id) {
        return selectedElements.contains(id);
    }

    public boolean isWireSelected(String id) {
        return selectedWires.contains(id);
    }

    public Set<String> getSelectedElements() {
        return Set.copyOf(selectedElements);
    }

    public Set<String> getSelectedWires() {
        return Set.copyOf(selectedWires);     // snapshot, safe to loop over while deleting
    }


    private void clearSilently() {
        selectedElements.clear();
        selectedWires.clear();
    }

    private void fire() {
        if (onChange != null)
            onChange.run();
    }
}