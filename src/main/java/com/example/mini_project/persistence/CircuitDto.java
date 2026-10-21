package com.example.mini_project.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// DTO -> Data Transfer Object
// CircuitDto converts the circuit's complex object structure into a simple data representation by
// keeping only the information needed for saving(it deletes the references)
// Defining structure of circuit for when saving or loading it from JSON file
// While saving a circuit -> The application converts circuit into circuitDto object, then this object
// is converted to JSON by serializer and then written to a file
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
        // These are the circuit element properties(State!!)
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

    // This is a Node where junction is defined
    // It has 2 possibilities -> Either a junction or a free endpoint but # connected Segments = 1
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

    // Just like wire segment
    // Considering wire segment can be connected to Endpoints
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

    // Identifies where the endpoint of wire segment is connected
    // It can be component's port or a node!!
    // Component ->
    // {
    //  "element": "gate1",
    //  "port": "output",
    //  "node": null
    // }
    // Junction Node ->
    // {
    //  "element": null,
    //  "port": null,
    //  "node": "node1"
    // }
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

    // Nothing but viewport settings like thing
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
