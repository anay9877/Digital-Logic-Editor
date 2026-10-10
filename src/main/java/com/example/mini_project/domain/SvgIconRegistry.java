package com.example.mini_project.domain;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.girod.javafx.svgimage.SVGLoader;

public class SvgIconRegistry {

    public static final double GATE_WIDTH = 180;
    public static final double SWITCH_WIDTH = 1000;
    public static final double LED_WIDTH = 500;

    private static final String AND_GATE_SVG = """
            <!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
            <svg width="800px" height="800px" viewBox="0 0 512 512" xmlns="http://www.w3.org/2000/svg" fill="#000000">
            
            <g id="SVGRepo_bgCarrier" stroke-width="0"/>
            
            <g id="SVGRepo_tracerCarrier" stroke-linecap="round" stroke-linejoin="round"/>
            
            <g id="SVGRepo_iconCarrier">
            
            <path fill="#fff" d="M105 105v302h151c148 0 148-302 0-302H105zm-89 46v18h71v-18H16zm368.8 96c.2 6 .2 12 0 18H496v-18H384.8zM16 343v18h71v-18H16z"/>
            
            </g>
            
            </svg>
            """;

    private static final String OR_GATE_SVG = """
            <!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
            <!-- Uploaded to: SVG Repo, www.svgrepo.com, Transformed by: SVG Repo Mixer Tools -->
            <svg width="800px" height="800px" viewBox="0 0 512 512" xmlns="http://www.w3.org/2000/svg" fill="#000000">
            
            <g id="SVGRepo_bgCarrier" stroke-width="0"/>
            
            <g id="SVGRepo_tracerCarrier" stroke-linecap="round" stroke-linejoin="round"/>
            
            <g id="SVGRepo_iconCarrier">
            
            <path fill="#fff" d="M116.6 407c40-45.9 60.4-98.4 60.4-151 0-52.6-20.4-105.1-60.4-151H192c34.1 0 81.9 34 119.3 71.4 18.7 18.6 35.1 37.9 46.6 53.3 5.8 7.6 10.4 14.4 13.4 19.4 1.4 2.5 2.5 4.7 3.2 6.1.1.4.2.5.2.8 0 .3-.1.5-.2.9-.6 1.4-1.7 3.5-3.2 6-3 5.1-7.5 11.8-13.2 19.5-11.3 15.4-27.5 34.6-46.1 53.2C274.8 373 227.1 407 192 407zM16 361v-18h122.2c-3 6.1-6.3 12.1-9.9 18zm374.5-96c.2-.3.4-.7.5-1 1.1-2.4 2-4.4 2-8 0-3.6-1-5.6-2-8-.1-.3-.3-.7-.5-1H496v18zM16 169v-18h112.3c3.6 5.9 6.9 11.9 9.9 18z"/>
            
            </g>
            
            </svg>
            """;

    private static final String NOT_GATE_SVG = """
            <!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
            <!-- Uploaded to: SVG Repo, www.svgrepo.com, Transformed by: SVG Repo Mixer Tools -->
            <svg width="800px" height="800px" viewBox="0 0 512 512" xmlns="http://www.w3.org/2000/svg" fill="#000000">
            
            <g id="SVGRepo_bgCarrier" stroke-width="0"/>
            
            <g id="SVGRepo_tracerCarrier" stroke-linecap="round" stroke-linejoin="round"/>
            
            <g id="SVGRepo_iconCarrier">
            
            <path fill="#fff" d="M 105,111.3 V 400.7 L 365.5,256 Z M 16,247 v 18 h 71 v -18 z m 400,-14 c -12.8,0 -23,10.2 -23,23 0,12.8 10.2,23 23,23 12.8,0 23,-10.2 23,-23 0,-12.8 -10.2,-23 -23,-23 z m 40,14 c 0.6,2.9 1,5.9 1,9 0,3.1 -0.4,6.1 -1,9 h 40 v -18 z"/>
            
            </g>
            
            </svg>
            """;

    private static final String XOR_GATE_SVG = """
            <!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
            <!-- Uploaded to: SVG Repo, www.svgrepo.com, Transformed by: SVG Repo Mixer Tools -->
            <svg width="800px" height="800px" viewBox="0 0 512 512" xmlns="http://www.w3.org/2000/svg" fill="#000000">
            
            <g id="SVGRepo_bgCarrier" stroke-width="0"/>
            
            <g id="SVGRepo_tracerCarrier" stroke-linecap="round" stroke-linejoin="round"/>
            
            <g id="SVGRepo_iconCarrier">
            
            <path fill="#fff" d="M53.86 89.17L42.14 102.8c17.99 15.4 32.89 31.6 44.81 48.2H16v18h82.58C114.9 197.3 123 226.7 123 256c0 29.3-8.1 58.7-24.42 87H16v18h70.95c-11.92 16.6-26.82 32.8-44.81 48.2l11.72 13.6c22.59-19.4 40.85-40.1 54.74-61.8h19.7c3.6-5.9 6.9-11.9 9.9-18H119c14.6-28.2 22-57.5 22-87s-7.4-58.8-22-87h19.2c-3-6.1-6.3-12.1-9.9-18h-19.7c-13.88-21.7-32.15-42.5-54.74-61.83zM116.6 105c40 45.9 60.4 98.4 60.4 151 0 52.6-20.4 105.1-60.4 151H192c34.1 0 81.9-34 119.3-71.4 18.7-18.6 35.1-37.9 46.6-53.3 5.8-7.6 10.4-14.4 13.4-19.4 1.4-2.5 2.5-4.7 3.2-6.1.1-.4.2-.5.2-.8 0-.3-.1-.5-.2-.9-.6-1.4-1.7-3.5-3.2-6-3-5.1-7.5-11.8-13.2-19.5-11.3-15.4-27.5-34.6-46.1-53.2C274.8 139 227.1 105 192 105h-75.4zm273.9 142c.2.3.4.7.5 1 1.1 2.4 2 4.4 2 8 0 3.6-1 5.6-2 8-.1.3-.3.7-.5 1H496v-18H390.5z"/>
            
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
            
            <path fill="#fff" d="M105 105v302h151c148 0 148-302 0-302H105zm-89 46v18h71v-18H16zm400 82c-12.8 0-23 10.2-23 23s10.2 23 23 23 23-10.2 23-23-10.2-23-23-23zm40 14c.6 2.9 1 5.9 1 9 0 3.1-.4 6.1-1 9h40v-18h-40zM16 343v18h71v-18H16z"/>
            
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
            
            <path fill="#fff" d="M116.6 105c40 45.9 60.4 98.4 60.4 151 0 52.6-20.4 105.1-60.4 151H192c34.1 0 81.9-34 119.3-71.4 18.7-18.6 35.1-37.9 46.6-53.3 5.8-7.6 10.4-14.4 13.4-19.4 1.4-2.5 2.5-4.7 3.2-6.1.1-.4.2-.5.2-.8 0-.3-.1-.5-.2-.9-.6-1.4-1.7-3.5-3.2-6-3-5.1-7.5-11.8-13.2-19.5-11.3-15.4-27.5-34.6-46.1-53.2C274.8 139 227.1 105 192 105h-75.4zM16 151v18h122.2c-3-6.1-6.3-12.1-9.9-18H16zm400 82c-12.8 0-23 10.2-23 23s10.2 23 23 23 23-10.2 23-23-10.2-23-23-23zm40 14c.6 2.9 1 5.9 1 9 0 3.1-.4 6.1-1 9h40v-18h-40zM16 343v18h112.3c3.6-5.9 6.9-11.9 9.9-18H16z"/>
            
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
            
            <g id="SVGRepo_iconCarrier"> <g> <g> <path fill="#ffffff" d="M40,56H24c-2.211,0-4,1.789-4,4s1.789,4,4,4h16c2.211,0,4-1.789,4-4S42.211,56,40,56z"/> <path fill="#ffffff" d="M32,0C16.539,0,4,12.539,4,28c0,9.445,4.695,17.781,11.863,22.844C16.438,51.25,17.375,52,20,52 s21.812,0,24,0s3.559-0.742,4.133-1.148C55.305,45.781,60,37.445,60,28C60,12.539,47.461,0,32,0z M43.891,44H36V28 c0-2.211-1.789-4-4-4s-4,1.789-4,4v16h-7.891C15.215,40.359,12,34.578,12,28c0-11.047,8.953-20,20-20s20,8.953,20,20 C52,34.578,48.785,40.359,43.891,44z"/> </g> <path fill="#555555" d="M43.891,44H36V28c0-2.211-1.789-4-4-4s-4,1.789-4,4v16h-7.891C15.215,40.359,12,34.578,12,28 c0-11.047,8.953-20,20-20s20,8.953,20,20C52,34.578,48.785,40.359,43.891,44z"/> </g> </g>
            
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
            case AND: return load(AND_GATE_SVG, "AND", GATE_WIDTH);
            case OR: return load(OR_GATE_SVG, "OR", GATE_WIDTH);
            case NOT: return load(NOT_GATE_SVG, "NOT", GATE_WIDTH);
            case XOR: return load(XOR_GATE_SVG, "XOR", GATE_WIDTH);
            case NAND: return load(NAND_GATE_SVG, "NAND", GATE_WIDTH);
            case NOR: return load(NOR_GATE_SVG, "NOR", GATE_WIDTH);
            default: throw new IllegalArgumentException("Not a gate type: " + type);
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