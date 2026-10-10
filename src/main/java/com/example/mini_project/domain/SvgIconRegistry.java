package com.example.mini_project.domain;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.girod.javafx.svgimage.SVGLoader;

public class SvgIconRegistry {

    private static final double GATE_WIDTH = 180;
    private static final double SWITCH_WIDTH = 1000;
    private static final double LED_WIDTH = 500;

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

    private Node load(String content, String label, double width) {
        if (content == null || content.isBlank()) {
            Rectangle r = new Rectangle(width, 40, Color.LIGHTBLUE);
            r.setStroke(Color.BLACK);
            Label l = new Label(label);
            l.setLayoutX(5);
            l.setLayoutY(12);
            return new Group(r, l); // TEMP placeholder until real SVG is pasted above
        }
        return SVGLoader.load(content, width);
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
        String svgContent;
        String label;
        if (on) {
            svgContent = SWITCH_ON_SVG;
            label = "SW-ON";
        } else {
            svgContent = SWITCH_OFF_SVG;
            label = "SW-OFF";
        }
        return load(svgContent, label, SWITCH_WIDTH);
    }

    public Node createLedIcon(boolean on) {
        String svgContent;
        String label;
        if (on) {
            svgContent = LED_ON_SVG;
            label = "LED-ON";
        } else {
            svgContent = LED_OFF_SVG;
            label = "LED-OFF";
        }
        return load(svgContent, label, LED_WIDTH);
    }
}