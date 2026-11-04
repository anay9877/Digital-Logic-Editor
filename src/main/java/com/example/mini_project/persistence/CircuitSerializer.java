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

// Handles saving, loading, importing and exporting
public class CircuitSerializer {

    // Gson is a lib which is used to convert Java objects to JSON and vice versa
    // GsonBuilder allows that configuration to happen
    // CDATA_PATTERN is a regex to pattern finder, which finds the content enclosed in CDATA section in XML file
    // Eg - <![CDATA[{"zoom": 1.0}]]> is converted to {"zoom": 1.0}
    // Finds the content under metadata tag of an SVG (same as above but for SVG)
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Pattern CDATA_PATTERN = Pattern.compile("<!\\[CDATA\\[(.*?)\\]\\]>", Pattern.DOTALL);
    private static final Pattern METADATA_TAG_PATTERN = Pattern.compile("<metadata[^>]*>(.*?)</metadata>", Pattern.DOTALL);

    // JSON Serialization

    // Convert a normal circuit to DTO
    // Circuit
    // ├── elements → CircuitElement objects
    // │                ├── Ports
    // │                ├── Internal properties
    // │                └── Possibly JavaFX references
    // ├── wireSegments → WireSegment objects
    // └── pointNodes → PointNode objects
    //
    // CircuitDto
    // ├── elements → ElementDto (id, type, x, y, props)
    // ├── wires    → WireDto (id, endpoints)
    // ├── nodes    → NodeDto (id, x, y)
    // └── view      → ViewDto (zoom, panX, panY)
    // (Taken by AI)
    public static CircuitDto toDto(Circuit circuit, ViewportManager viewport) {
        CircuitDto dto = new CircuitDto();

        // Take the elements and then sort them by ID(using getId function)
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

        // Sort the nodes and add it to sortedNodes
        List<PointNode> sortedNodes = new ArrayList<>(circuit.getPointNodes());
        sortedNodes.sort(Comparator.comparing(PointNode::getId));
        for (PointNode node : sortedNodes) {
            dto.nodes.add(new CircuitDto.NodeDto(
                    node.getId(),
                    node.getX(),
                    node.getY()
            ));
        }

        // Sort the wires and add it to sortedWires
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

        // Merge into the current circuit (do NOT clear it).
        // If the incoming bounding box overlaps the existing circuit, shift it down.
        double dy = mergeOffset(dto, circuit);
        if (dy != 0) shiftDto(dto, dy);

        // Remap all IDs so they don't collide with the existing circuit.
        remapIds(dto, circuit);

        rebuildModel(dto, circuit, controller);

        if (viewport != null && dto.view != null) {
            viewport.setView(dto.view.zoom, dto.view.panX, dto.view.panY);
        }

        if (controller != null) {
            controller.notifyRender();
            controller.resetHistory();
        }
    }

    /**
     * Returns the Y offset to add to every incoming element/node so the loaded
     * circuit clears the existing one.
     * Returns 0 when the current circuit is empty or the bounding boxes do not clash.
     */
    private static double mergeOffset(CircuitDto dto, Circuit circuit) {
        if (circuit.getElements().isEmpty() && circuit.getPointNodes().isEmpty()) return 0;

        // Bottom of the existing circuit
        double existingBottom = Double.NEGATIVE_INFINITY;
        for (CircuitElement e : circuit.getElements().values()) {
            existingBottom = Math.max(existingBottom, e.getY());
        }
        for (PointNode n : circuit.getPointNodes()) {
            existingBottom = Math.max(existingBottom, n.getY());
        }
        if (existingBottom == Double.NEGATIVE_INFINITY) existingBottom = 0;

        // Top of the incoming circuit
        double incomingTop = Double.POSITIVE_INFINITY;
        if (dto.elements != null) {
            for (CircuitDto.ElementDto ed : dto.elements) incomingTop = Math.min(incomingTop, ed.y);
        }
        if (dto.nodes != null) {
            for (CircuitDto.NodeDto nd : dto.nodes) incomingTop = Math.min(incomingTop, nd.y);
        }
        if (incomingTop == Double.POSITIVE_INFINITY) incomingTop = 0;

        // No overlap: incoming already sits below the existing circuit
        final double BUFFER = 80.0; // 4 grid cells
        double needed = existingBottom + BUFFER - incomingTop;
        if (needed <= 0) return 0;

        // Snap to grid
        double gridSize = 20.0;
        return Math.ceil(needed / gridSize) * gridSize;
    }

    /** Shifts every element position and node position in the DTO by dy. */
    private static void shiftDto(CircuitDto dto, double dy) {
        if (dto.elements != null) {
            for (CircuitDto.ElementDto ed : dto.elements) ed.y += dy;
        }
        if (dto.nodes != null) {
            for (CircuitDto.NodeDto nd : dto.nodes) nd.y += dy;
        }
    }

    /**
     * Rewrites every id and cross-reference in the DTO so that none clash with
     * IDs already present in {@code circuit}.
     *
     * Strategy: for each prefix ("AND", "OR", "node", "wire", …) find the
     * highest N already used, then assign new IDs starting at max+1.
     */
    private static void remapIds(CircuitDto dto, Circuit circuit) {
        // 1. Build max-N map from the existing circuit.
        Map<String, Integer> maxByPrefix = new HashMap<>();
        for (String id : circuit.getElements().keySet()) {
            bumpMax(maxByPrefix, id);
        }
        for (PointNode n : circuit.getPointNodes()) {
            bumpMax(maxByPrefix, n.getId());
        }
        for (WireSegment w : circuit.getWireSegments()) {
            bumpMax(maxByPrefix, w.getId());
        }

        // 2. Assign new IDs to incoming elements / nodes / wires.
        Map<String, String> elemRemap = new HashMap<>();
        if (dto.elements != null) {
            for (CircuitDto.ElementDto ed : dto.elements) {
                String prefix = idPrefix(ed.id);
                int next = maxByPrefix.getOrDefault(prefix, 0) + 1;
                maxByPrefix.put(prefix, next);
                String newId = prefix + "-" + next;
                elemRemap.put(ed.id, newId);
                ed.id = newId;
            }
        }

        Map<String, String> nodeRemap = new HashMap<>();
        if (dto.nodes != null) {
            for (CircuitDto.NodeDto nd : dto.nodes) {
                String prefix = idPrefix(nd.id);
                int next = maxByPrefix.getOrDefault(prefix, 0) + 1;
                maxByPrefix.put(prefix, next);
                String newId = prefix + "-" + next;
                nodeRemap.put(nd.id, newId);
                nd.id = newId;
            }
        }

        if (dto.wires != null) {
            for (CircuitDto.WireDto wd : dto.wires) {
                // Remap wire id
                String prefix = idPrefix(wd.id);
                int next = maxByPrefix.getOrDefault(prefix, 0) + 1;
                maxByPrefix.put(prefix, next);
                wd.id = prefix + "-" + next;

                // Remap endpoint references
                if (wd.from != null) {
                    if (wd.from.element != null) wd.from.element = elemRemap.getOrDefault(wd.from.element, wd.from.element);
                    if (wd.from.node   != null) wd.from.node    = nodeRemap.getOrDefault(wd.from.node,   wd.from.node);
                }
                if (wd.to != null) {
                    if (wd.to.element != null) wd.to.element = elemRemap.getOrDefault(wd.to.element, wd.to.element);
                    if (wd.to.node   != null) wd.to.node    = nodeRemap.getOrDefault(wd.to.node,   wd.to.node);
                }
            }
        }
    }

    /** Extracts the prefix of an ID like "AND-3" → "AND", "node-5" → "node". */
    private static String idPrefix(String id) {
        int i = id.lastIndexOf('-');
        return i > 0 ? id.substring(0, i) : id;
    }

    /** Bumps the max counter for the prefix of {@code id} in {@code map}. */
    private static void bumpMax(Map<String, Integer> map, String id) {
        String prefix = idPrefix(id);
        int n = parseIdNumber(id);
        map.put(prefix, Math.max(map.getOrDefault(prefix, 0), n));
    }

    private static void rebuildModel(CircuitDto dto, Circuit circuit, EditorController controller) {
        if (dto.elements != null) {
            for (CircuitDto.ElementDto ed : dto.elements) {
                ComponentType type = ComponentType.valueOf(ed.type);
                CircuitElement elem = type.create(ed.id, ed.x, ed.y);
                if (elem instanceof InputElement ie && ed.props != null) {
                    String stateStr = ed.props.get("state");
                    if (stateStr != null) ie.setValue(SignalState.valueOf(stateStr));
                }
                circuit.addElement(elem);
                if (controller != null) controller.updateCounter(type, parseIdNumber(ed.id));
            }
        }
        if (dto.nodes != null) {
            for (CircuitDto.NodeDto nd : dto.nodes) {
                circuit.addPointNode(new PointNode(nd.id, nd.x, nd.y));
                PointNode.updateNextId(parseIdNumber(nd.id));
            }
        }
        if (dto.wires != null) {
            for (CircuitDto.WireDto wd : dto.wires) {
                Port sp = portOf(wd.from, circuit), tp = portOf(wd.to, circuit);
                PointNode sn = nodeOf(wd.from, circuit), tn = nodeOf(wd.to, circuit);
                double x1 = sp != null ? sp.getAbsX() : sn != null ? sn.getX() : 0;
                double y1 = sp != null ? sp.getAbsY() : sn != null ? sn.getY() : 0;
                double x2 = tp != null ? tp.getAbsX() : tn != null ? tn.getX() : 0;
                double y2 = tp != null ? tp.getAbsY() : tn != null ? tn.getY() : 0;

                WireSegment seg = new WireSegment(wd.id, x1, y1, x2, y2);
                if (sp != null) seg.setStartPort(sp);
                else if (sn != null) seg.setStartNode(sn);
                if (tp != null) seg.setEndPort(tp);
                else if (tn != null) seg.setEndNode(tn);
                circuit.addWireSegment(seg);
                WireSegment.updateNextId(parseIdNumber(wd.id));
            }
        }
    }

    private static Port portOf(CircuitDto.EndpointDto ep, Circuit c) {
        if (ep == null || ep.element == null) return null;
        CircuitElement owner = c.getElement(ep.element);
        return owner == null ? null : findPort(owner, ep.port);
    }

    private static PointNode nodeOf(CircuitDto.EndpointDto ep, Circuit c) {
        if (ep == null || ep.element != null || ep.node == null) return null;
        return findNode(c, ep.node);
    }

    // SVG Export & Import

    // We need to add JSON Script of circuit and individual wires, junction dotes and components
    // The Json script is needed to reconstruct the circuit while the data of others are needed to
    // show the actual data visually in svg file when opened!!
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
//        Matcher metaMatcher = METADATA_TAG_PATTERN.matcher(svgContent);
//        if (metaMatcher.find()) {
//            String json = metaMatcher.group(1).trim();
//            fromJson(json, circuit, viewport, controller);
//            return;
//        }

        throw new IllegalArgumentException("No circuit metadata found in the SVG file.");
    }

    // File Operations
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

    // Helper functions

    // AND-1-in0 -> in0
    private static String extractLocalPortId(String fullPortId) {
        int idx = fullPortId.lastIndexOf('-');
        return idx >= 0 ? fullPortId.substring(idx + 1) : fullPortId;
    }

    private static Port findPort(CircuitElement elem, String localPortId) {
        if (elem == null || localPortId == null) return null;
        Port p = elem.findPort(elem.getId() + "-" + localPortId);
        if (p != null) return p;

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
            } catch (NumberFormatException ignored) {
            }
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
