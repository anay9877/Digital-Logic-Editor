package com.example.mini_project.persistence;

import com.example.mini_project.controller.EditorController;
import com.example.mini_project.domain.*;
import com.example.mini_project.gui.ViewportManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CircuitSerializer {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Pattern CDATA_PATTERN = Pattern.compile("<!\\[CDATA\\[(.*?)\\]\\]>", Pattern.DOTALL);
    private static final Pattern METADATA_TAG_PATTERN = Pattern.compile("<metadata[^>]*>(.*?)</metadata>", Pattern.DOTALL);

    // ── JSON Serialization ───────────────────────────────────────────────────

    public static CircuitDto toDto(Circuit circuit, ViewportManager viewport) {
        CircuitDto dto = new CircuitDto();

        // 1. Elements (sorted by ID for deterministic output)
        List<CircuitElement> sortedElements = new ArrayList<>(circuit.getElements().values());
        sortedElements.sort(Comparator.comparing(CircuitElement::getId));
        for (CircuitElement elem : sortedElements) {
            Map<String, String> props = null;
            if (elem instanceof InputElement ie) {
                props = Map.of("state", ie.getValue().name());
            }
            dto.elements.add(new CircuitDto.ElementDto(
                    elem.getId(),
                    elem.getType().name(),
                    elem.getX(),
                    elem.getY(),
                    props
            ));
        }

        // 2. Nodes (sorted by ID)
        List<PointNode> sortedNodes = new ArrayList<>(circuit.getPointNodes());
        sortedNodes.sort(Comparator.comparing(PointNode::getId));
        for (PointNode node : sortedNodes) {
            dto.nodes.add(new CircuitDto.NodeDto(
                    node.getId(),
                    node.getX(),
                    node.getY()
            ));
        }

        // 3. Wires (sorted by ID)
        List<WireSegment> sortedWires = new ArrayList<>(circuit.getWireSegments());
        sortedWires.sort(Comparator.comparing(WireSegment::getId));
        for (WireSegment seg : sortedWires) {
            CircuitDto.EndpointDto fromEp = new CircuitDto.EndpointDto();
            if (seg.getStartPort() != null) {
                fromEp.element = seg.getStartPort().getOwner().getId();
                fromEp.port = extractLocalPortId(seg.getStartPort().getId());
            } else if (seg.getStartNode() != null) {
                fromEp.node = seg.getStartNode().getId();
            }

            CircuitDto.EndpointDto toEp = new CircuitDto.EndpointDto();
            if (seg.getEndPort() != null) {
                toEp.element = seg.getEndPort().getOwner().getId();
                toEp.port = extractLocalPortId(seg.getEndPort().getId());
            } else if (seg.getEndNode() != null) {
                toEp.node = seg.getEndNode().getId();
            }

            dto.wires.add(new CircuitDto.WireDto(seg.getId(), fromEp, toEp));
        }

        // 4. Viewport
        if (viewport != null) {
            dto.view = new CircuitDto.ViewDto(viewport.getZoom(), viewport.getTranslateX(), viewport.getTranslateY());
        } else {
            dto.view = new CircuitDto.ViewDto(1.0, 0.0, 0.0);
        }

        return dto;
    }

    public static String toJson(Circuit circuit, ViewportManager viewport) {
        CircuitDto dto = toDto(circuit, viewport);
        return GSON.toJson(dto);
    }

    public static void fromJson(String json, Circuit circuit, ViewportManager viewport, EditorController controller) {
        if (json == null || json.isBlank()) return;

        CircuitDto dto = GSON.fromJson(json, CircuitDto.class);
        if (dto == null) return;

        // Clear existing circuit and state
        if (controller != null) {
            controller.newCircuit();
        } else {
            circuit.clear();
            PointNode.resetId();
            WireSegment.resetId();
        }

        // 1. Rebuild elements
        if (dto.elements != null) {
            for (CircuitDto.ElementDto ed : dto.elements) {
                ComponentType type = ComponentType.valueOf(ed.type);
                CircuitElement elem = type.create(ed.id, ed.x, ed.y);

                if (elem instanceof InputElement ie && ed.props != null) {
                    String stateStr = ed.props.get("state");
                    if (stateStr != null) {
                        ie.setValue(SignalState.valueOf(stateStr));
                    }
                }

                circuit.addElement(elem);

                if (controller != null) {
                    int num = parseIdNumber(ed.id);
                    controller.updateCounter(type, num);
                }
            }
        }

        // 2. Rebuild nodes
        if (dto.nodes != null) {
            for (CircuitDto.NodeDto nd : dto.nodes) {
                PointNode node = new PointNode(nd.id, nd.x, nd.y);
                circuit.addPointNode(node);
                int num = parseIdNumber(nd.id);
                PointNode.updateNextId(num);
            }
        }

        // 3. Rebuild wires
        if (dto.wires != null) {
            for (CircuitDto.WireDto wd : dto.wires) {
                Port startPort = null;
                PointNode startNode = null;
                double x1 = 0, y1 = 0;

                if (wd.from != null) {
                    if (wd.from.element != null) {
                        CircuitElement owner = circuit.getElement(wd.from.element);
                        if (owner != null) {
                            startPort = findPort(owner, wd.from.port);
                            if (startPort != null) {
                                x1 = startPort.getAbsX();
                                y1 = startPort.getAbsY();
                            }
                        }
                    } else if (wd.from.node != null) {
                        startNode = findNode(circuit, wd.from.node);
                        if (startNode != null) {
                            x1 = startNode.getX();
                            y1 = startNode.getY();
                        }
                    }
                }

                Port endPort = null;
                PointNode endNode = null;
                double x2 = 0, y2 = 0;

                if (wd.to != null) {
                    if (wd.to.element != null) {
                        CircuitElement owner = circuit.getElement(wd.to.element);
                        if (owner != null) {
                            endPort = findPort(owner, wd.to.port);
                            if (endPort != null) {
                                x2 = endPort.getAbsX();
                                y2 = endPort.getAbsY();
                            }
                        }
                    } else if (wd.to.node != null) {
                        endNode = findNode(circuit, wd.to.node);
                        if (endNode != null) {
                            x2 = endNode.getX();
                            y2 = endNode.getY();
                        }
                    }
                }

                WireSegment seg = new WireSegment(wd.id, x1, y1, x2, y2);
                if (startPort != null) seg.setStartPort(startPort);
                else if (startNode != null) seg.setStartNode(startNode);

                if (endPort != null) seg.setEndPort(endPort);
                else if (endNode != null) seg.setEndNode(endNode);

                circuit.addWireSegment(seg);

                int num = parseIdNumber(wd.id);
                WireSegment.updateNextId(num);
            }
        }

        // 4. Restore viewport
        if (viewport != null && dto.view != null) {
            viewport.setView(dto.view.zoom, dto.view.panX, dto.view.panY);
        }

        if (controller != null) {
            controller.notifyRender();
        }
    }

    // ── SVG Export & Import ───────────────────────────────────────────────────

    public static String toSvg(Circuit circuit, ViewportManager viewport) {
        String json = toJson(circuit, viewport);

        double zoom = viewport != null ? viewport.getZoom() : 1.0;
        double panX = viewport != null ? viewport.getTranslateX() : 0.0;
        double panY = viewport != null ? viewport.getTranslateY() : 0.0;

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"2000\" height=\"1500\" viewBox=\"0 0 2000 1500\">\n");

        // Embedded JSON in metadata for lossless import
        sb.append("  <metadata id=\"circuit-data\">\n");
        sb.append("    <![CDATA[\n");
        sb.append(json).append("\n");
        sb.append("    ]]>\n");
        sb.append("  </metadata>\n");

        // Visual group
        sb.append(String.format(Locale.US, "  <g id=\"circuit-root\" transform=\"translate(%.2f, %.2f) scale(%.4f)\">\n", panX, panY, zoom));

        // 1. Wires
        sb.append("    <g id=\"wires\" stroke=\"#000000\" stroke-width=\"2.5\" stroke-linecap=\"round\">\n");
        for (WireSegment seg : circuit.getWireSegments()) {
            sb.append(String.format(Locale.US, "      <line x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\"/>\n",
                    seg.getX1(), seg.getY1(), seg.getX2(), seg.getY2()));
        }
        sb.append("    </g>\n");

        // 2. Junction dots
        sb.append("    <g id=\"junctions\" fill=\"#000000\">\n");
        for (PointNode node : circuit.getPointNodes()) {
            if (node.isJunction()) {
                sb.append(String.format(Locale.US, "      <circle cx=\"%.1f\" cy=\"%.1f\" r=\"4.0\"/>\n",
                        node.getX(), node.getY()));
            }
        }
        sb.append("    </g>\n");

        // 3. Components
        sb.append("    <g id=\"components\">\n");
        for (CircuitElement elem : circuit.getElements().values()) {
            ComponentType type = elem.getType();
            boolean isOn = false;
            if (elem instanceof InputElement ie) isOn = (ie.getValue() == SignalState.ON);
            else if (elem instanceof OutputElement oe) isOn = (oe.getValue() == SignalState.ON);

            double w = (type == ComponentType.SWITCH) ? SvgIconRegistry.getSwitchWidth()
                    : (type == ComponentType.LED) ? SvgIconRegistry.getLedWidth()
                    : SvgIconRegistry.getGateWidth();

            double shiftX = (type == ComponentType.SWITCH) ? SvgIconRegistry.getSwitchShiftX()
                    : (type == ComponentType.LED) ? SvgIconRegistry.getLedShiftX()
                    : SvgIconRegistry.getGateShiftX();

            double shiftY = (type == ComponentType.SWITCH) ? SvgIconRegistry.getSwitchShiftY()
                    : (type == ComponentType.LED) ? SvgIconRegistry.getLedShiftY()
                    : SvgIconRegistry.getGateShiftY();

            double vx = -shiftX;
            double vy = -shiftY;

            String rawSvg = SvgIconRegistry.getRawSvg(type, isOn);
            String innerContent = extractSvgInner(rawSvg);

            sb.append(String.format(Locale.US, "      <g id=\"%s\" transform=\"translate(%.1f, %.1f)\">\n", elem.getId(), elem.getX(), elem.getY()));
            sb.append(String.format(Locale.US, "        <svg width=\"%.1f\" height=\"%.1f\" viewBox=\"%.1f %.1f 512 512\">\n", w, w, vx, vy));
            sb.append("          ").append(innerContent).append("\n");
            sb.append("        </svg>\n");
            sb.append("      </g>\n");
        }
        sb.append("    </g>\n");

        sb.append("  </g>\n");
        sb.append("</svg>\n");

        return sb.toString();
    }

    public static void fromSvg(String svgContent, Circuit circuit, ViewportManager viewport, EditorController controller) {
        if (svgContent == null || svgContent.isBlank()) return;

        // Try extracting JSON from CDATA first
        Matcher cdataMatcher = CDATA_PATTERN.matcher(svgContent);
        if (cdataMatcher.find()) {
            String json = cdataMatcher.group(1).trim();
            fromJson(json, circuit, viewport, controller);
            return;
        }

        // Try extracting from <metadata>
        Matcher metaMatcher = METADATA_TAG_PATTERN.matcher(svgContent);
        if (metaMatcher.find()) {
            String json = metaMatcher.group(1).trim();
            fromJson(json, circuit, viewport, controller);
            return;
        }

        throw new IllegalArgumentException("No circuit metadata found in the SVG file.");
    }

    // ── File Operations ───────────────────────────────────────────────────────
    // saveJson similar to importSVG
    // loadJson somewhat similar to exportSVG

    public static void saveJson(File file, Circuit circuit, ViewportManager viewport) throws IOException {
        String json = toJson(circuit, viewport);
        Files.writeString(file.toPath(), json, StandardCharsets.UTF_8);
    }

    public static void loadJson(File file, Circuit circuit, ViewportManager viewport, EditorController controller) throws IOException {
        String json = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        fromJson(json, circuit, viewport, controller);
    }

    public static void exportSvg(File file, Circuit circuit, ViewportManager viewport) throws IOException {
        String svg = toSvg(circuit, viewport);
        Files.writeString(file.toPath(), svg, StandardCharsets.UTF_8);
    }

    public static void importSvg(File file, Circuit circuit, ViewportManager viewport, EditorController controller) throws IOException {
        String svg = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        fromSvg(svg, circuit, viewport, controller);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String extractLocalPortId(String fullPortId) {
        int idx = fullPortId.lastIndexOf('-');
        return idx >= 0 ? fullPortId.substring(idx + 1) : fullPortId;
    }

    private static Port findPort(CircuitElement elem, String localPortId) {
        if (elem == null || localPortId == null) return null;
        Port p = elem.findPort(elem.getId() + "-" + localPortId);
        if (p != null) return p;

        if (elem instanceof Gate gate) {
            for (Port port : gate.getAllPorts()) {
                if (port.getId().endsWith("-" + localPortId)) return port;
            }
        } else if (elem instanceof InputElement ie) {
            if (ie.getOutputPort().getId().endsWith("-" + localPortId)) return ie.getOutputPort();
        } else if (elem instanceof OutputElement oe) {
            if (oe.getInputPort().getId().endsWith("-" + localPortId)) return oe.getInputPort();
        }
        return null;
    }

    private static PointNode findNode(Circuit circuit, String nodeId) {
        for (PointNode n : circuit.getPointNodes()) {
            if (n.getId().equals(nodeId)) return n;
        }
        return null;
    }

    private static int parseIdNumber(String id) {
        if (id == null) return 0;
        int idx = id.lastIndexOf('-');
        if (idx >= 0 && idx + 1 < id.length()) {
            try {
                return Integer.parseInt(id.substring(idx + 1));
            } catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    private static String extractSvgInner(String svg) {
        if (svg == null) return "";
        int start = svg.indexOf("<svg");
        if (start < 0) return "";
        int closeTag = svg.indexOf('>', start);
        if (closeTag < 0) return "";
        int end = svg.lastIndexOf("</svg>");
        if (end < closeTag) return "";
        return svg.substring(closeTag + 1, end).trim();
    }
}
