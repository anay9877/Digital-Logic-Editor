package com.example.mini_project.domain;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.girod.javafx.svgimage.SVGImage;
import org.girod.javafx.svgimage.SVGLoader;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SvgIconRegistry {

    // ── Rendered sizes (canvas pixels) ────────────────────────────────────────
    // Change this one value to resize every component consistently.
    // Port offsets are scaled from the same SVG viewBox units below, so they remain
    // aligned with the artwork after resizing.
    private static final double COMPONENT_SCALE = 0.55;
    private static final double GATE_WIDTH   = 180 * COMPONENT_SCALE;  // 135 px
    private static final double SWITCH_WIDTH = 60 * COMPONENT_SCALE;  // 45 px
    private static final double LED_WIDTH    = 60 * COMPONENT_SCALE;  // 45 px

    // ── viewBox sizes of the source artwork (all square) ──────────────────────
    private static final double GATE_VIEWBOX   = 512;
    private static final double SWITCH_VIEWBOX = 24;
    private static final double LED_VIEWBOX    = 64;

    // pixels per viewBox unit
    private static final double GATE_SCALE   = GATE_WIDTH   / GATE_VIEWBOX;    // 135 / 512
    private static final double SWITCH_SCALE = SWITCH_WIDTH / SWITCH_VIEWBOX;  // 45 / 24
    private static final double LED_SCALE    = LED_WIDTH    / LED_VIEWBOX;     // 45 / 64

    // ── Gate port positions, in viewBox units ─────────────────────────────────
    // Verified against the path data: every gate draws its wire stubs as 18-unit-thick
    // rectangles, so the connection point is the MIDDLE of the stub, at its outer end.
    //
    //   input 0 stub : y 151..169  -> centre y = 160   (x starts at 16)
    //   input 1 stub : y 343..361  -> centre y = 352   (x starts at 16)
    //   NOT input    : y 247..265  -> centre y = 256   (x starts at 16)
    //   output stub  : y 247..265  -> centre y = 256   (x ends at 496)
    //
    // AND / OR / XOR / NAND / NOR all share identical stub positions. For NAND / NOR /
    // NOT the output bubble sits before the stub (x 393..456), the stub itself still
    // ends at x = 496, so the output port is at 496 for every gate.
    private static final double GATE_IN_X   = 16;
    private static final double GATE_OUT_X  = 496;
    private static final double GATE_IN0_Y  = 160;
    private static final double GATE_IN1_Y  = 352;
    private static final double GATE_MID_Y  = 256;

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

                    <path d="M16 256H87M456 256H496"
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

                <path d="M16 174H87M16 338H87M456 256H496"
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

            <path d="M16 174H135M16 338H135M456 256H496"
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
            <svg width="800px" height="800px" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg" fill="none">

            <g id="SVGRepo_bgCarrier" stroke-width="0"/>

            <g id="SVGRepo_tracerCarrier" stroke-linecap="round" stroke-linejoin="round"/>

            <g id="SVGRepo_iconCarrier"> <path stroke="#fff" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 6H9a6 6 0 1 0 0 12h6a6 6 0 0 0 0-12Z"/> <circle cx="15" cy="12" r="3" fill="#fff" stroke="#fff" stroke-width="2"/> </g>
            </svg>
            """;

    private static final String SWITCH_OFF_SVG = """
            <!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
            <!-- Uploaded to: SVG Repo, www.svgrepo.com, Transformed by: SVG Repo Mixer Tools -->
            <svg width="800px" height="800px" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg" fill="none">

            <g id="SVGRepo_bgCarrier" stroke-width="0"/>

            <g id="SVGRepo_tracerCarrier" stroke-linecap="round" stroke-linejoin="round"/>

            <g id="SVGRepo_iconCarrier"> <path stroke="#fff" stroke-linejoin="round" stroke-width="2" d="M12 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z"/> <path stroke="#fff" stroke-linejoin="round" stroke-width="2" d="M3 12a6 6 0 0 0 6 6h6a6 6 0 0 0 0-12H9a6 6 0 0 0-6 6Z"/> </g>
            </svg>
            """;

    private static final String LED_ON_SVG = """
            <!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
            <!-- Uploaded to: SVG Repo, www.svgrepo.com, Transformed by: SVG Repo Mixer Tools -->
            <svg version="1.0" id="Layer_1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="800px" height="800px" viewBox="0 0 64 64" enable-background="new 0 0 64 64" xml:space="preserve" fill="#ffffff">

            <g id="SVGRepo_bgCarrier" stroke-width="0"/>

            <g id="SVGRepo_tracerCarrier" stroke-linecap="round" stroke-linejoin="round"/>

            <g id="SVGRepo_iconCarrier"> <g> <g> <path fill="#ffffff" d="M40,56H24c-2.211,0-4,1.789-4,4s1.789,4,4,4h16c2.211,0,4-1.789,4-4S42.211,56,40,56z"/> <path fill="#ffffff" d="M32,0C16.539,0,4,12.539,4,28c0,9.445,4.695,17.781,11.863,22.844C16.438,51.25,17.375,52,20,52 s21.812,0,24,0s3.559-0.742,4.133-1.148C55.305,45.781,60,37.445,60,28C60,12.539,47.461,0,32,0z M43.891,44H36V28 c0-2.211-1.789-4-4-4s-4,1.789-4,4v16h-7.891C15.215,40.359,12,34.578,12,28c0-11.047,8.953-20,20-20s20,8.953,20,20 C52,34.578,48.785,40.359,43.891,44z"/> </g> <path fill="#e5a50a" d="M43.891,44H36V28c0-2.211-1.789-4-4-4s-4,1.789-4,4v16h-7.891C15.215,40.359,12,34.578,12,28 c0-11.047,8.953-20,20-20s20,8.953,20,20C52,34.578,48.785,40.359,43.891,44z"/> </g> </g>
            </svg>
            """;

    private static final String LED_OFF_SVG = """
            <!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
            <!-- Uploaded to: SVG Repo, www.svgrepo.com, Transformed by: SVG Repo Mixer Tools -->
            <svg version="1.0" id="Layer_1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="800px" height="800px" viewBox="0 0 64 64" enable-background="new 0 0 64 64" xml:space="preserve" fill="#ffffff" stroke="#ffffff">

            <g id="SVGRepo_bgCarrier" stroke-width="0"/>

            <g id="SVGRepo_tracerCarrier" stroke-linecap="round" stroke-linejoin="round"/>

            <g id="SVGRepo_iconCarrier"> <g> <g> <path fill="#ffffff" d="M40,56H24c-2.211,0-4,1.789-4,4s1.789,4,4,4h16c2.211,0,4-1.789,4-4S42.211,56,40,56z"/> <path fill="#fff00000ff" d="M32,0C16.539,0,4,12.539,4,28c0,9.445,4.695,17.781,11.863,22.844C16.438,51.25,17.375,52,20,52 s21.812,0,24,0s3.559-0.742,4.133-1.148C55.305,45.781,60,37.445,60,28C60,12.539,47.461,0,32,0z M43.891,44H36V28 c0-2.211-1.789-4-4-4s-4,1.789-4,4v16h-7.891C15.215,40.359,12,34.578,12,28c0-11.047,8.953-20,20-20s20,8.953,20,20 C52,34.578,48.785,40.359,43.891,44z"/> </g> <path fill="#fff00000" d="M43.891,44H36V28c0-2.211-1.789-4-4-4s-4,1.789-4,4v16h-7.891C15.215,40.359,12,34.578,12,28 c0-11.047,8.953-20,20-20s20,8.953,20,20C52,34.578,48.785,40.359,43.891,44z"/> </g> </g>
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
     * which pivots around the node's centre. The artwork ends up ~32px up/left of
     * (viewBoxCoord * scale). Without width/height the loader scales 1:1 from the viewBox, the
     * node scale is 1.0, and viewBoxCoord * (renderWidth / viewBoxWidth) is exact.
     */
    private static String stripRootSize(String svg) {
        Matcher m = SVG_TAG.matcher(svg);
        if (!m.find()) return svg;
        String tag = SIZE_ATTR.matcher(m.group()).replaceAll("");
        return svg.substring(0, m.start()) + tag + svg.substring(m.end());
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
     * layout bounds are exactly width x width. Port offsets in {@link #getPortSpecs} are measured
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
        Rectangle frame = new Rectangle(width, width, Color.TRANSPARENT);   // pins the bounds / origin
        frame.setMouseTransparent(true);                                    // clicks still hit only the artwork
        return new Group(frame, img);
    }

    public Node createIcon(ComponentType type) {
        switch (type) {
            case AND:
                return load(AND_GATE_SVG, "AND", GATE_WIDTH);
            case OR:
                return load(OR_GATE_SVG, "OR", GATE_WIDTH);
            case NOT:
                return load(NOT_GATE_SVG, "NOT", GATE_WIDTH);
            case XOR:
                return load(XOR_GATE_SVG, "XOR", GATE_WIDTH);
            case NAND:
                return load(NAND_GATE_SVG, "NAND", GATE_WIDTH);
            case NOR:
                return load(NOR_GATE_SVG, "NOR", GATE_WIDTH);
            default:
                throw new IllegalArgumentException("Not a gate type: " + type);
        }
    }

    public Node createSwitchIcon(boolean on) {
        return load(on ? SWITCH_ON_SVG : SWITCH_OFF_SVG, on ? "SW-ON" : "SW-OFF", SWITCH_WIDTH);
    }

    public Node createLedIcon(boolean on) {
        return load(on ? LED_ON_SVG : LED_OFF_SVG, on ? "LED-ON" : "LED-OFF", LED_WIDTH);
    }

    // ── Port specifications ───────────────────────────────────────────────────

    /**
     * Port positions in canvas pixels, relative to the icon node's top-left (its local origin).
     * The localId must match the suffix used when ports are named in Gate / InputElement /
     * OutputElement (e.g. "in0", "in1", "out").
     */
    public List<PortSpec> getPortSpecs(ComponentType type) {
        return switch (type) {
            case AND, OR, XOR, NAND, NOR -> List.of(
                    new PortSpec("in0", PortType.INPUT,  gx(GATE_IN_X),  gy(GATE_IN0_Y)),
                    new PortSpec("in1", PortType.INPUT,  gx(GATE_IN_X),  gy(GATE_IN1_Y)),
                    new PortSpec("out", PortType.OUTPUT, gx(GATE_OUT_X), gy(GATE_MID_Y))
            );
            case NOT -> List.of(
                    new PortSpec("in0", PortType.INPUT,  gx(GATE_IN_X),  gy(GATE_MID_Y)),
                    new PortSpec("out", PortType.OUTPUT, gx(GATE_OUT_X), gy(GATE_MID_Y))
            );
            // SWITCH viewBox 24x24. The pill outline spans x 3..21 (+1 for the 2-unit stroke = 22),
            // vertically centred at y = 12. The output wire leaves the right edge of the outline.
            case SWITCH -> List.of(
                    new PortSpec("out", PortType.OUTPUT, 22 * SWITCH_SCALE, 12 * SWITCH_SCALE)
            );
            // LED viewBox 64x64. The base pill spans y 56..64 and is centred on x = 32;
            // the input wire enters at the bottom of the base.
            case LED -> List.of(
                    new PortSpec("in0", PortType.INPUT, 32 * LED_SCALE, 64 * LED_SCALE)
            );
            default -> List.of();
        };
    }

    private static double gx(double viewBoxX) { return viewBoxX * GATE_SCALE; }
    private static double gy(double viewBoxY) { return viewBoxY * GATE_SCALE; }

    /** Convenience: rendered sizes (used by canvas to size hit-boxes). */
    public static double getGateWidth()   { return GATE_WIDTH;   }
    public static double getSwitchWidth() { return SWITCH_WIDTH; }
    public static double getLedWidth()    { return LED_WIDTH;    }
}