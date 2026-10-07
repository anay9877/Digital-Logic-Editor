package com.example.mini_project.domain;

public enum ComponentType {
    AND{
        @Override
        public CircuitElement create(String id, double x, double y){
            return new AndGate(id, x, y);
        }
    },
    OR{
        @Override
        public CircuitElement create(String id, double x, double y){
            return new OrGate(id, x, y);
        }
    },
    NOT{
        @Override
        public CircuitElement create(String id, double x, double y){
            return new NotGate(id, x, y);
        }
    },
    XOR{
        @Override
        public CircuitElement create(String id, double x, double y){
            return new XorGate(id, x, y);
        }
    },
    NAND{
        @Override
        public CircuitElement create(String id, double x, double y){
            return new NandGate(id, x, y);
        }
    },
    NOR{
        @Override
        public CircuitElement create(String id, double x, double y){
            return new NorGate(id, x, y);
        }
    },
    SWITCH{
        @Override
        public CircuitElement create(String id, double x, double y){
            return new InputElement(id, x, y);
        }
    },
    LED{
        @Override
        public CircuitElement create(String id, double x, double y){
            return new OutputElement(id, x, y);
        }
    },
    UNKNOWN{
        @Override
        public CircuitElement create(String id, double x, double y){
            return new AndGate(id, x, y);
        }
    };

    public abstract CircuitElement create(String id, double x, double y);
}
