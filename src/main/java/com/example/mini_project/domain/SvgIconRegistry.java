package com.example.mini_project.domain;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.girod.javafx.svgimage.SVGImage;
import org.girod.javafx.svgimage.SVGLoader;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SvgIconRegistry {

    // ── Rendered sizes (canvas pixels) ────────────────────────────────────────
    // Gate viewBox is 512x512. At 128 px width, scale is exactly 0.25 px per viewBox unit (1/4).
    // The artwork is shifted by dx = -16, dy = -16 in viewBox units so that:
    //   - Gate inputs at x = 16 become (16 - 16) * 0.25 = 0.0 px (left edge)
    //   - Gate output at x = 496 becomes (496 - 16) * 0.25 = 120.0 px
    //   - Gate center line at y = 256 becomes (256 - 16) * 0.25 = 60.0 px
    //   - Gate in0 at y = 174 becomes (174 - 16) * 0.25 = 39.5 px -> lands on grid dot 40.0 px
    //   - Gate in1 at y = 338 becomes (338 - 16) * 0.25 = 80.5 px -> lands on grid dot 80.0 px
    // Every port offset is an EXACT multiple of GRID_SIZE (10.0)!
    private static final double GATE_WIDTH   = 128;
    private static final double GATE_SHIFT_X = -16;
    private static final double GATE_SHIFT_Y = -16;

    // Switch + LED render in a 64 px box over a 512-unit viewBox = exactly 0.125 px per unit,
    // so whole-unit shifts below map to whole-pixel (and grid-multiple) port offsets.
    private static final double SWITCH_WIDTH = 64;
    private static final double LED_WIDTH    = 64;

    // ── viewBox sizes of the source artwork (all square) ──────────────────────
    private static final double GATE_VIEWBOX   = 512;
    private static final double SWITCH_VIEWBOX = 512;
    private static final double LED_VIEWBOX    = 512;

    // pixels per viewBox unit
    private static final double GATE_SCALE   = GATE_WIDTH   / GATE_VIEWBOX;    // 128 / 512 = 0.25
    private static final double SWITCH_SCALE = SWITCH_WIDTH / SWITCH_VIEWBOX;  // 64 / 512 = 0.125
    private static final double LED_SCALE    = LED_WIDTH    / LED_VIEWBOX;     // 64 / 512 = 0.125

    // ── Gate port positions, in viewBox units ─────────────────────────────────
    // Measured from the artwork: every gate draws its wires as 12-unit strokes with round caps.
    // A port sits on the wire's centre line, at the wire's path endpoint (the round cap adds
    // another 6 units / ~1 px beyond it, so the connecting line overlaps the cap, no gap).
    //
    //   input 0 wire : "M16 174 ..."  -> port (16, 174)
    //   input 1 wire : "M16 338 ..."  -> port (16, 338)
    //   NOT input    : "M16 256 ..."  -> port (16, 256)
    //   output wire  : "... 496 256"  -> port (496, 256)
    //
    // AND / OR / XOR / NAND / NOR share identical wire positions (inputs are +-82 either
    // side of the centre line y = 256).
    private static final double GATE_IN_X   = 16;
    private static final double GATE_OUT_X  = 496;
    private static final double GATE_IN0_Y  = 174;
    private static final double GATE_IN1_Y  = 338;
    private static final double GATE_MID_Y  = 256;

    // ── Switch / LED connection points, in viewBox units ──────────────────────
    // The canvas puts the icon origin on a 10 px grid dot and a port's position is
    // origin + offset, so a port only sits on a grid dot if its pixel offset is a multiple
    // of GRID (10). The artwork is therefore shifted (whole viewBox units) so that its
    // connection point lands exactly on such an offset.
    //
    //   SWITCH output : right outer edge of the body (rect x 110 + w 292 + half stroke 6 = 408),
    //                   on the centre line y = 256.
    //   LED input     : bottom end of the long (anode) lead, path end (210, 491).
    //
    //   shifted units * 0.125 px  ->  switch out = (50, 30) px,  LED in = (30, 60) px
    private static final double SWITCH_OUT_X = 408, SWITCH_OUT_Y = 256;
    private static final double SWITCH_SHIFT_X = -8,  SWITCH_SHIFT_Y = -16;

    private static final double LED_IN_X = 210, LED_IN_Y = 491;
    private static final double LED_SHIFT_X = 30,  LED_SHIFT_Y = -11;

    // ── SVG source strings ────────────────────────────────────────────────────

    private static final String AND_GATE_SVG = """
            <!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
            <svg width="800px" height="800px" viewBox="0 0 512 512" xmlns="http://www.w3.org/2000/svg">

                <g id="SVGRepo_bgCarrier" stroke-width="0"/>

                <g id="SVGRepo_tracerCarrier"
                stroke-linecap="round"
                stroke-linejoin="round"/>

                <g id="SVGRepo_iconCarrier">

                    <path d="M16 174H105M16 338H105M407 256H496"
                        stroke="#1A73E8"
                        stroke-width="12"
                        stroke-linecap="round"
                        fill="none"/>

                    <path d="M105 105H256A151 151 0 0 1 256 407H105Z"
                        fill="#E8F0FE"
                        stroke="#1A73E8"
                        stroke-width="12"/>

                </g>
            </svg>
            """;

    private static final String OR_GATE_SVG = """
            <!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
            <!-- Uploaded to: SVG Repo, www.svgrepo.com, Transformed by: SVG Repo Mixer Tools -->
            <svg width="800px" height="800px" viewBox="0 0 512 512" xmlns="http://www.w3.org/2000/svg">

                <g id="SVGRepo_bgCarrier" stroke-width="0"/>

                <g id="SVGRepo_tracerCarrier" stroke-linecap="round" stroke-linejoin="round"/>

                <g id="SVGRepo_iconCarrier">

                    <path d="M16 174H135M16 338H135M407 256H496"
                        stroke="#1A73E8"
                        stroke-width="12"
                        stroke-linecap="round"
                        fill="none"/>

                    <path d="M105 105Q185 256 105 407Q285 407 407 256Q285 105 105 105Z"
                        fill="#E8F0FE"
                        stroke="#1A73E8"
                        stroke-width="12"/>

                </g>
            </svg>
            """;

    private static final String NOT_GATE_SVG = """
            <!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
            <!-- Uploaded to: SVG Repo, www.svgrepo.com, Transformed by: SVG Repo Mixer Tools -->
            <svg width="800px" height="800px" viewBox="0 0 512 512" xmlns="http://www.w3.org/2000/svg">

                <g id="SVGRepo_bgCarrier" stroke-width="0"/>

                <g id="SVGRepo_tracerCarrier"
                stroke-linecap="round"
                stroke-linejoin="round"/>

                <g id="SVGRepo_iconCarrier">

                    <path d="M16 256H105M439 256H496"
                        stroke="#1A73E8"
                        stroke-width="12"
                        stroke-linecap="round"
                        fill="none"/>

                    <path d="M105 111.3V400.7L365.5 256Z"
                        fill="#E8F0FE"
                        stroke="#1A73E8"
                        stroke-width="12"
                        stroke-linejoin="round"/>

                    <circle cx="416" cy="256" r="23"
                            fill="#E8F0FE"
                            stroke="#1A73E8"
                            stroke-width="12"/>

                </g>
            </svg>
            """;

    private static final String XOR_GATE_SVG = """
            <!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
            <!-- Uploaded to: SVG Repo, www.svgrepo.com, Transformed by: SVG Repo Mixer Tools -->
            <svg width="800px" height="800px" viewBox="0 0 512 512" xmlns="http://www.w3.org/2000/svg">

                <g id="SVGRepo_bgCarrier" stroke-width="0"/>

                <g id="SVGRepo_tracerCarrier" stroke-linecap="round" stroke-linejoin="round"/>

                <g id="SVGRepo_iconCarrier">

                    <path d="M16 174H135M16 338H135M407 256H496"
                        stroke="#1A73E8"
                        stroke-width="12"
                        stroke-linecap="round"
                        fill="none"/>

                    <path d="M105 105Q185 256 105 407Q285 407 407 256Q285 105 105 105Z"
                        fill="#E8F0FE"
                        stroke="#1A73E8"
                        stroke-width="12"/>

                    <path d="M65 105Q145 256 65 407"
                        fill="none"
                        stroke="#1A73E8"
                        stroke-width="12"
                        stroke-linecap="round"/>

                </g>
            </svg>
            """;

    private static final String NAND_GATE_SVG = """
            <!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
            <!-- Uploaded to: SVG Repo, www.svgrepo.com, Transformed by: SVG Repo Mixer Tools -->
            <svg width="800px" height="800px" viewBox="0 0 512 512" xmlns="http://www.w3.org/2000/svg" fill="#000000">

            <g id="SVGRepo_bgCarrier" stroke-width="0"/>

            <g id="SVGRepo_tracerCarrier" stroke-linecap="round" stroke-linejoin="round"/>

            <g id="SVGRepo_iconCarrier">

                <path d="M16 174H105M16 338H105M439 256H496"
                    stroke="#1A73E8"
                    stroke-width="12"
                    stroke-linecap="round"
                    fill="none"/>

                <path d="M105 105V407H256A151 151 0 0 0 256 105H105Z"
                    fill="#E8F0FE"
                    stroke="#1A73E8"
                    stroke-width="12"
                    stroke-linejoin="round"/>

                <circle cx="416" cy="256" r="23"
                    fill="#E8F0FE"
                    stroke="#1A73E8"
                    stroke-width="12"/>

            </g>
            </svg>
            """;

    private static final String NOR_GATE_SVG = """
            <!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
            <!-- Uploaded to: SVG Repo, www.svgrepo.com, Transformed by: SVG Repo Mixer Tools -->
            <svg width="800px" height="800px" viewBox="0 0 512 512" xmlns="http://www.w3.org/2000/svg" fill="#000000">

            <g id="SVGRepo_bgCarrier" stroke-width="0"/>

            <g id="SVGRepo_tracerCarrier" stroke-linecap="round" stroke-linejoin="round"/>

            <g id="SVGRepo_iconCarrier">

            <path d="M16 174H135M16 338H135M439 256H496"
                stroke="#1A73E8"
                stroke-width="12"
                stroke-linecap="round"
                fill="none"/>

            <path d="M105 105Q185 256 105 407Q285 407 375 256Q285 105 105 105Z"
                fill="#E8F0FE"
                stroke="#1A73E8"
                stroke-width="12"
                stroke-linejoin="round"/>

            <circle cx="416" cy="256" r="23"
                    fill="#E8F0FE"
                    stroke="#1A73E8"
                    stroke-width="12"/>

            </g>
            
            </svg>
            """;

    private static final String SWITCH_ON_SVG = """
            <!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
            <!-- Uploaded to: SVG Repo, www.svgrepo.com, Transformed by: SVG Repo Mixer Tools -->
            <svg width="800px" height="800px" viewBox="0 0 512 512" xmlns="http://www.w3.org/2000/svg" fill="#000000">
                <g id="SVGRepo_bgCarrier" stroke-width="0"/>
                <g id="SVGRepo_tracerCarrier" stroke-linecap="round" stroke-linejoin="round"/>
                <g id="SVGRepo_iconCarrier">
                    <rect x="110" y="40" width="292" height="432" rx="36"
                        fill="#1C1D1F"
                        stroke="#0F1011"
                        stroke-width="12"/>
                    <rect x="138" y="68" width="236" height="376" rx="20"
                        fill="#0A0B0C"
                        stroke="#0F1011"
                        stroke-width="8"/>
                    <rect x="150" y="80" width="212" height="176" rx="12"
                        fill="#2E3136"
                        stroke="#0F1011"
                        stroke-width="4"/>
                    <rect x="150" y="256" width="212" height="176" rx="12"
                        fill="#18191B"
                        stroke="#0F1011"
                        stroke-width="4"/>
                    <line x1="144" y1="256" x2="368" y2="256"
                        stroke="#0A0B0C"
                        stroke-width="12"
                        stroke-linecap="round"/>
                    <text x="256" y="195"
                        font-family="-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif"
                        font-size="88"
                        font-weight="700"
                        fill="#E0E0E0"
                        text-anchor="middle">1</text>
                </g>
            </svg>

            """;

    private static final String SWITCH_OFF_SVG = """
            <!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
            <!-- Uploaded to: SVG Repo, www.svgrepo.com, Transformed by: SVG Repo Mixer Tools -->
            <svg width="800px" height="800px" viewBox="0 0 512 512" xmlns="http://www.w3.org/2000/svg" fill="#000000">
                <g id="SVGRepo_bgCarrier" stroke-width="0"/>
                <g id="SVGRepo_tracerCarrier" stroke-linecap="round" stroke-linejoin="round"/>
                <g id="SVGRepo_iconCarrier">
                    <rect x="110" y="40" width="292" height="432" rx="36"
                        fill="#1C1D1F"
                        stroke="#0F1011"
                        stroke-width="12"/>
                    <rect x="138" y="68" width="236" height="376" rx="20"
                        fill="#0A0B0C"
                        stroke="#0F1011"
                        stroke-width="8"/>
                    <rect x="150" y="80" width="212" height="176" rx="12"
                        fill="#18191B"
                        stroke="#0F1011"
                        stroke-width="4"/>
                    <rect x="150" y="256" width="212" height="176" rx="12"
                        fill="#2E3136"
                        stroke="#0F1011"
                        stroke-width="4"/>
                    <line x1="144" y1="256" x2="368" y2="256"
                        stroke="#0A0B0C"
                        stroke-width="12"
                        stroke-linecap="round"/>
                    <text x="256" y="365"
                        font-family="-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif"
                        font-size="88"
                        font-weight="700"
                        fill="#8E9297"
                        text-anchor="middle">0</text>
                </g>
            </svg>

            """;

    private static final String LED_ON_SVG = """
            <!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
            <!-- Uploaded to: SVG Repo, www.svgrepo.com, Transformed by: SVG Repo Mixer Tools -->
            <svg version="1.0" id="Layer_1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="800px" height="800px" viewBox="0 0 64 64" enable-background="new 0 0 64 64" xml:space="preserve" fill="#ffffff">
            
            <g id="SVGRepo_bgCarrier" stroke-width="0"/>
            
            <g id="SVGRepo_tracerCarrier" stroke-linecap="round" stroke-linejoin="round"/>
            
            <g id="SVGRepo_iconCarrier"> <g> <g> <path fill="#000" d="M40,56H24c-2.211,0-4,1.789-4,4s1.789,4,4,4h16c2.211,0,4-1.789,4-4S42.211,56,40,56z"/> <path fill="#000" d="M32,0C16.539,0,4,12.539,4,28c0,9.445,4.695,17.781,11.863,22.844C16.438,51.25,17.375,52,20,52 s21.812,0,24,0s3.559-0.742,4.133-1.148C55.305,45.781,60,37.445,60,28C60,12.539,47.461,0,32,0z M43.891,44H36V28 c0-2.211-1.789-4-4-4s-4,1.789-4,4v16h-7.891C15.215,40.359,12,34.578,12,28c0-11.047,8.953-20,20-20s20,8.953,20,20 C52,34.578,48.785,40.359,43.891,44z"/> </g> <path fill="#e5a50a" d="M43.891,44H36V28c0-2.211-1.789-4-4-4s-4,1.789-4,4v16h-7.891C15.215,40.359,12,34.578,12,28 c0-11.047,8.953-20,20-20s20,8.953,20,20C52,34.578,48.785,40.359,43.891,44z"/> </g> </g>
            
            </svg>
            """;

    private static final String LED_OFF_SVG = """
            <!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
            <!-- Uploaded to: SVG Repo, www.svgrepo.com, Transformed by: SVG Repo Mixer Tools -->
            <svg version="1.0" id="Layer_1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="800px" height="800px" viewBox="0 0 64 64" enable-background="new 0 0 64 64" xml:space="preserve" fill="#ffffff" stroke="#ffffff">
            
            <g id="SVGRepo_bgCarrier" stroke-width="0"/>
            
            <g id="SVGRepo_tracerCarrier" stroke-linecap="round" stroke-linejoin="round"/>
            
            <g id="SVGRepo_iconCarrier"> <g> <g> <path fill="#000" d="M40,56H24c-2.211,0-4,1.789-4,4s1.789,4,4,4h16c2.211,0,4-1.789,4-4S42.211,56,40,56z"/> <path fill="#000" d="M32,0C16.539,0,4,12.539,4,28c0,9.445,4.695,17.781,11.863,22.844C16.438,51.25,17.375,52,20,52 s21.812,0,24,0s3.559-0.742,4.133-1.148C55.305,45.781,60,37.445,60,28C60,12.539,47.461,0,32,0z M43.891,44H36V28 c0-2.211-1.789-4-4-4s-4,1.789-4,4v16h-7.891C15.215,40.359,12,34.578,12,28c0-11.047,8.953-20,20-20s20,8.953,20,20 C52,34.578,48.785,40.359,43.891,44z"/> </g> <path fill="#fff00000" d="M43.891,44H36V28c0-2.211-1.789-4-4-4s-4,1.789-4,4v16h-7.891C15.215,40.359,12,34.578,12,28 c0-11.047,8.953-20,20-20s20,8.953,20,20C52,34.578,48.785,40.359,43.891,44z"/> </g> </g>
            
            </svg>
            """;

    // ── Icon loading ──────────────────────────────────────────────────────────

    // Matches the opening <svg ...> tag, and the width="..." / height="..." attributes inside it.
    private static final Pattern SVG_TAG   = Pattern.compile("<svg\\b[^>]*>", Pattern.DOTALL);
    private static final Pattern SIZE_ATTR = Pattern.compile("\\s(?:width|height)\\s*=\\s*\"[^\"]*\"");

    /**
     * Removes width/height from the root &lt;svg&gt; tag (SVG Repo always adds 800px/800px).
     *
     * Why: when width/height are present, SVGLoader computes its scale from that 800px and then
     * ALSO applies a viewBox->viewport scale (800/viewBox) to the node as a JavaFX scaleX/scaleY,
     * which pivots around the node's centre. The artwork ends up shifted away from
     * (viewBoxCoord * scale). Without width/height the loader scales 1:1 from the viewBox, the
     * node scale is 1.0, and viewBoxCoord * (renderWidth / viewBoxWidth) is exact.
     */
    private static String stripRootSize(String svg) {
        Matcher m = SVG_TAG.matcher(svg);
        if (!m.find()) return svg;
        String tag = SIZE_ATTR.matcher(m.group()).replaceAll("");
        return svg.substring(0, m.start()) + tag + svg.substring(m.end());
    }

    /** Wraps the artwork in a translate(dx dy) group (viewBox units). Art itself is untouched. */
    private static String shiftArt(String svg, double dx, double dy) {
        Matcher m = SVG_TAG.matcher(svg);
        int close = svg.lastIndexOf("</svg>");
        if (!m.find() || close < 0) return svg;
        return svg.substring(0, m.end())
                + "<g transform=\"translate(" + dx + " " + dy + ")\">"
                + svg.substring(m.end(), close)
                + "</g>"
                + svg.substring(close);
    }

    private Node placeholder(String label, double width) {
        Rectangle r = new Rectangle(width, 40, Color.LIGHTBLUE);
        r.setStroke(Color.BLACK);
        Label l = new Label(label);
        l.setLayoutX(5);
        l.setLayoutY(12);
        return new Group(r, l);
    }

    /**
     * Loads an icon so that its local origin (0,0) is the top-left of the viewBox and the node's
     * layout bounds are exactly width x width (a Pane). Port offsets in {@link #getPortSpecs} are measured
     * from that origin, so they stay correct whether the canvas positions the node with
     * setLayoutX/Y, setTranslateX/Y or relocate().
     */
    private Node load(String content, String label, double width) {
        if (content == null || content.isBlank()) {
            return placeholder(label, width);
        }
        SVGImage img = SVGLoader.load(stripRootSize(content), width);
        if (img == null) {
            return placeholder(label, width);
        }
        // A Pane has fixed layout bounds (0,0,width,width) no matter what its children do, so the
        // node origin stays the top-left of the viewBox even when the artwork overhangs it
        // (e.g. LED_OFF's white outline stroke sticks out ~0.3px). A Group would grow with it.
        Pane box = new Pane(img);
        box.setMinSize(width, width);
        box.setPrefSize(width, width);
        box.setMaxSize(width, width);
        box.setPickOnBounds(false);          // clicks still hit only the artwork, as before
        return box;
    }

    public Node createIcon(ComponentType type) {
        String svg = switch (type) {
            case AND -> AND_GATE_SVG;
            case OR -> OR_GATE_SVG;
            case NOT -> NOT_GATE_SVG;
            case XOR -> XOR_GATE_SVG;
            case NAND -> NAND_GATE_SVG;
            case NOR -> NOR_GATE_SVG;
            default -> throw new IllegalArgumentException("Not a gate type: " + type);
        };
        return load(shiftArt(svg, GATE_SHIFT_X, GATE_SHIFT_Y), type.name(), GATE_WIDTH);
    }

    public Node createSwitchIcon(boolean on) {
        String svg = on ? SWITCH_ON_SVG : SWITCH_OFF_SVG;
        return load(shiftArt(svg, SWITCH_SHIFT_X, SWITCH_SHIFT_Y), on ? "SW-ON" : "SW-OFF", SWITCH_WIDTH);
    }

    public Node createLedIcon(boolean on) {
        String svg = on ? LED_ON_SVG : LED_OFF_SVG;
        return load(shiftArt(svg, LED_SHIFT_X, LED_SHIFT_Y), on ? "LED-ON" : "LED-OFF", LED_WIDTH);
    }

    /**
     * Port positions in canvas pixels, relative to the icon node's top-left (its local origin).
     * The localId must match the suffix used when ports are named in Gate / InputElement /
     * OutputElement (e.g. "in0", "in1", "out").
     *
     * All offsets are exact multiples of GRID_SIZE (10.0 px) so ports always fall exactly
     * on the canvas grid dots.
     */
    public List<PortSpec> getPortSpecs(ComponentType type) {
        return switch (type) {
            case AND, OR, XOR, NAND, NOR -> List.of(
                    new PortSpec("in0", PortType.INPUT,    0.0, 40.0),
                    new PortSpec("in1", PortType.INPUT,    0.0, 80.0),
                    new PortSpec("out", PortType.OUTPUT, 120.0, 60.0)
            );
            case NOT -> List.of(
                    new PortSpec("in0", PortType.INPUT,    0.0, 60.0),
                    new PortSpec("out", PortType.OUTPUT, 120.0, 60.0)
            );
            // SWITCH: output on the right edge of the body, on the centre line -> (50, 30) px
            case SWITCH -> List.of(
                    new PortSpec("out", PortType.OUTPUT, 50.0, 30.0)
            );
            // LED: input on the tip of the long (anode) lead -> (30, 60) px
            case LED -> List.of(
                    new PortSpec("in0", PortType.INPUT, 61.5, 60.0)
            );
            default -> List.of();
        };
    }

    /** Convenience: rendered sizes (used by canvas to size hit-boxes). */
    public static double getGateWidth()   { return GATE_WIDTH;   }
    public static double getSwitchWidth() { return SWITCH_WIDTH; }
    public static double getLedWidth()    { return LED_WIDTH;    }

    public static double getGateShiftX()   { return GATE_SHIFT_X;   }
    public static double getGateShiftY()   { return GATE_SHIFT_Y;   }
    public static double getSwitchShiftX() { return SWITCH_SHIFT_X; }
    public static double getSwitchShiftY() { return SWITCH_SHIFT_Y; }
    public static double getLedShiftX()    { return LED_SHIFT_X;    }
    public static double getLedShiftY()    { return LED_SHIFT_Y;    }

    public static String getRawSvg(ComponentType type, boolean on) {
        return switch (type) {
            case AND -> AND_GATE_SVG;
            case OR -> OR_GATE_SVG;
            case NOT -> NOT_GATE_SVG;
            case XOR -> XOR_GATE_SVG;
            case NAND -> NAND_GATE_SVG;
            case NOR -> NOR_GATE_SVG;
            case SWITCH -> on ? SWITCH_ON_SVG : SWITCH_OFF_SVG;
            case LED -> on ? LED_ON_SVG : LED_OFF_SVG;
            default -> null;
        };
    }
}