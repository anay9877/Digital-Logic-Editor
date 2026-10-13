package com.example.mini_project.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CircuitDto {
    public String format = "digital-logic-editor";
    public List<ElementDto> elements = new ArrayList<>();
    public List<NodeDto> nodes = new ArrayList<>();
    public List<WireDto> wires = new ArrayList<>();
    public ViewDto view;

    public static class ElementDto {
        public String id;
        public String type;
        public double x;
        public double y;
        public Map<String, String> props;

        public ElementDto() {}

        public ElementDto(String id, String type, double x, double y, Map<String, String> props) {
            this.id = id;
            this.type = type;
            this.x = x;
            this.y = y;
            this.props = props;
        }
    }

    public static class NodeDto {
        public String id;
        public double x;
        public double y;

        public NodeDto() {}

        public NodeDto(String id, double x, double y) {
            this.id = id;
            this.x = x;
            this.y = y;
        }
    }

    public static class WireDto {
        public String id;
        public EndpointDto from;
        public EndpointDto to;

        public WireDto() {}

        public WireDto(String id, EndpointDto from, EndpointDto to) {
            this.id = id;
            this.from = from;
            this.to = to;
        }
    }

    public static class EndpointDto {
        public String element;
        public String port;
        public String node;

        public EndpointDto() {}

        public static EndpointDto port(String elementId, String portId) {
            EndpointDto ep = new EndpointDto();
            ep.element = elementId;
            ep.port = portId;
            return ep;
        }

        public static EndpointDto node(String nodeId) {
            EndpointDto ep = new EndpointDto();
            ep.node = nodeId;
            return ep;
        }
    }

    public static class ViewDto {
        public double zoom = 1.0;
        public double panX = 0.0;
        public double panY = 0.0;

        public ViewDto() {}

        public ViewDto(double zoom, double panX, double panY) {
            this.zoom = zoom;
            this.panX = panX;
            this.panY = panY;
        }
    }
}
