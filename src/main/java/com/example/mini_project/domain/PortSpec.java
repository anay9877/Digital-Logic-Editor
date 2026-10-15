package com.example.mini_project.domain;

/**
 * Static blueprint for a port's visual position on a rendered component icon.
 *
 * @param localId suffix matching how the port is named in Gate/InputElement/OutputElement
 *                e.g. "in0", "in1", "out"
 * @param type    INPUT or OUTPUT
 * @param relX    pixel offset from the component's top-left corner (canvas space)
 * @param relY    pixel offset from the component's top-left corner (canvas space)
 */
public record PortSpec(String localId, PortType type, double relX, double relY) {
}
