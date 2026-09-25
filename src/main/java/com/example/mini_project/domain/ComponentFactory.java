package com.example.mini_project.domain;

import java.util.HashMap;
import java.util.Map;

public class ComponentFactory {
    private final Map<String, Integer> counters = new HashMap<>();

    private String nextId(ComponentType type) {
        int n = counters.getOrDefault(type.name(), 0) + 1;
        counters.put(type.name(), n);
        return type.name() + "-" + n;
    }

    public CircuitElement create(ComponentType type, double x, double y) {
        String id = nextId(type);
        switch (type) {
            case AND: return new AndGate(id, x, y);
            case OR: return new OrGate(id, x, y);
            case NOT: return new NotGate(id, x, y);
            case XOR: return new XorGate(id, x, y);
            case NAND: return new NandGate(id, x, y);
            case NOR: return new NorGate(id, x, y);
            case SWITCH: return new InputElement(id, x, y);
            case LED: return new OutputElement(id, x, y);
            default: throw new IllegalArgumentException("Unknown type: " + type);
        }
    }
}
