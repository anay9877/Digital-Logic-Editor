package com.example.mini_project.domain;

import com.example.mini_project.gui.CircuitCanvas;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SvgIconRegistryTest {

    @Test
    void switchOutputPortStaysAttachedToBodyEdgeOnBothStates() {
        SvgIconRegistry registry = new SvgIconRegistry();
        PortSpec output = registry.getPortSpecs(ComponentType.SWITCH).getFirst();

        assertEquals(80.0, SvgIconRegistry.getSwitchWidth());
        assertEquals(60.0, output.relX());
        assertEquals(40.0, output.relY());
        assertEquals(output.relX(), CircuitCanvas.snap(output.relX()));
        assertEquals(output.relY(), CircuitCanvas.snap(output.relY()));
        assertTrue(SvgIconRegistry.getRawSvg(ComponentType.SWITCH, true)
                .contains("x=\"110\" y=\"40\" width=\"292\" height=\"432\""));
        assertTrue(SvgIconRegistry.getRawSvg(ComponentType.SWITCH, false)
                .contains("x=\"110\" y=\"40\" width=\"292\" height=\"432\""));
    }
}
