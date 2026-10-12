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
            <svg width="800px" height="800px" viewBox="0 0 512 512" xmlns="http://www.w3.org/2000/svg" fill="#000000">
                <g id="SVGRepo_bgCarrier" stroke-width="0"/>
                <g id="SVGRepo_tracerCarrier" stroke-linecap="round" stroke-linejoin="round"/>
                <g id="SVGRepo_iconCarrier">
                    <defs>
                    <radialGradient id="rGlow" cx="256" cy="171" r="171" gradientUnits="userSpaceOnUse">
                        <stop offset="0%" stop-color="#FF3B30" stop-opacity="0.6"/>
                        <stop offset="60%" stop-color="#FF3B30" stop-opacity="0.2"/>
                        <stop offset="100%" stop-color="#FF3B30" stop-opacity="0"/>
                    </radialGradient>
                    <linearGradient id="rDome" x1="0" y1="0" x2="1" y2="0">
                        <stop offset="0%" stop-color="#FF453A"/>
                        <stop offset="30%" stop-color="#FF9F9A"/>
                        <stop offset="70%" stop-color="#FF3B30"/>
                        <stop offset="100%" stop-color="#D70015"/>
                    </linearGradient>
                    </defs>
                    <circle cx="256" cy="171" r="171" fill="url(#rGlow)"/>
                    <line x1="210" y1="363" x2="210" y2="491" stroke="#A6B1B9" stroke-width="13" stroke-linecap="round"/>
                    <line x1="302" y1="363" x2="302" y2="469" stroke="#A6B1B9" stroke-width="13" stroke-linecap="round"/>
                    <path d="M142 213 C142 94 370 94 370 213 L370 333 L142 333 Z" fill="url(#rDome)"/>
                    <rect x="125" y="333" width="262" height="30" rx="9" fill="#D70015"/>
                    <path d="M216 222 L216 179 L245 179 L256 205 Z" fill="#FFE5E5" opacity="0.8"/>
                    <path d="M285 222 L285 196 L262 196" stroke="#FFE5E5" stroke-width="6.4" fill="none" opacity="0.8"/>
                    <ellipse cx="188" cy="149" rx="13" ry="43" transform="rotate(-25 188 149)" fill="#FFFFFF" opacity="0.6"/>
                </g>
            </svg>

            """;

    private static final String LED_OFF_SVG = """
            <!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
            <!-- Uploaded to: SVG Repo, www.svgrepo.com, Transformed by: SVG Repo Mixer Tools -->
            <svg width="800px" height="800px" viewBox="0 0 512 512" xmlns="http://www.w3.org/2000/svg" fill="#000000">
                <g id="SVGRepo_bgCarrier" stroke-width="0"/>
                <g id="SVGRepo_tracerCarrier" stroke-linecap="round" stroke-linejoin="round"/>
                <g id="SVGRepo_iconCarrier">
                    <defs>
                    <linearGradient id="rDomeOff" x1="0" y1="0" x2="1" y2="0">
                        <stop offset="0%" stop-color="#5B1617"/>
                        <stop offset="30%" stop-color="#8E2325"/>
                        <stop offset="70%" stop-color="#5B1617"/>
                        <stop offset="100%" stop-color="#3D0C0D"/>
                    </linearGradient>
                    </defs>
                    <line x1="210" y1="363" x2="210" y2="491" stroke="#7A8288" stroke-width="13" stroke-linecap="round"/>
                    <line x1="302" y1="363" x2="302" y2="469" stroke="#7A8288" stroke-width="13" stroke-linecap="round"/>
                    <path d="M142 213 C142 94 370 94 370 213 L370 333 L142 333 Z" fill="url(#rDomeOff)"/>
                    <rect x="125" y="333" width="262" height="30" rx="9" fill="#3D0C0D"/>
                    <path d="M216 222 L216 179 L245 179 L256 205 Z" fill="#401011"/>
                    <path d="M285 222 L285 196 L262 196" stroke="#401011" stroke-width="6.4" fill="none"/>
                    <ellipse cx="188" cy="149" rx="13" ry="43" transform="rotate(-25 188 149)" fill="#FFFFFF" opacity="0.2"/>
                </g>
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