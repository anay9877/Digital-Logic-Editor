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

    //prajw : start
    // The selected item remains in this set until another selection action replaces or removes it.
    public void selectElement(String id, boolean additive) {
        if (!additive)
            clearSilently();
        if (!selectedElements.add(id) && additive)
            selectedElements.remove(id); // shift-click toggles
        fire();
    }
    //prajw: end

    public void selectElements(Collection<String> ids, boolean additive) {
        if (!additive)
            clearSilently();
        selectedElements.addAll(ids);
        fire();
    }

    //prajw : start
    // Wire selection follows the same persistence and Shift/Control-toggle behavior as element selection.
    public void selectWire(String id, boolean additive) {
        if (!additive)
            clearSilently();
        if (!selectedWires.add(id) && additive)
            selectedWires.remove(id);
        fire();
    }
    //prajw: end

    public void selectWires(Collection<String> ids, boolean additive) {
        if (!additive) clearSilently();
        selectedWires.addAll(ids);
        fire();
    }

    //prajw : start
    // Explicit clearing is used when the user clicks empty canvas or presses Escape.
    public void clear() {
        clearSilently();
        fire();
    }
    //prajw: end

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

    public void selectElementsAndWires(Collection<String> elementIds, Collection<String> wireIds, boolean additive) {
        //prajw : start
        // Rubber-band selection replaces prior items unless Shift or Control asks to add to them.
        if (!additive) {
            selectedElements.clear();
            selectedWires.clear();
        }
        selectedElements.addAll(elementIds);
        selectedWires.addAll(wireIds);
        fire();
        //prajw: end
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