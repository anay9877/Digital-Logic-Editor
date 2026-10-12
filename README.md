Mini_Project

Make the branches name according to you names and start working upon project
Also make sure once the code from your side is completed please upload it on the *main branch
Update the README accordingly also

THIS IS HOW JAVAFX WORKS:

Background: How a JavaFX Application is Structured

STAGE is the actual application window — the frame with a title bar, close button, and border that the operating system displays. Every JavaFX application has one main Stage, created automatically when the application starts.

SCENE is the container for everything visible inside that window. A Stage can only display one Scene at a time (though it can be swapped out, e.g. switching from a "menu screen" to an "editor screen"). The Scene holds the actual content — buttons, canvases, toolbars — and defines things like the window's width and height.

NODE is the general term for anything that can be placed inside a Scene — a button, a shape, an image, a text label, or *a container that groups other nodes together*. Every visible element in a JavaFX application, no matter how simple or complex, is a Node.

THE SCENE GRAPH is the tree structure formed by nesting nodes inside other nodes. For example, a Scene might contain a BorderPane (a layout container), which contains a Toollbar at the top and a drawing area in the center; that drawing area might contain individual shapes representing each logic gate. This entire nested structure — from the Scene down to the smallest shape — is called the scene graph, and JavaFX automatically handles drawing it, detecting mouse clicks on the correct node, and redrawing it when something changes.

Why this matters for the project: every interactive feature already implemented — clicking to place a gate, clicking a port to draw a wire, dragging a component, toggling a switch — works because these elements are all genuine Node objects living inside the same scene graph. JavaFX's built-in mouse-event system (onMouseClicked, onMouseDragged, etc.) automatically knows which node was clicked, based on its position in this tree, and the project's Circuit/Port/Wire classes are simply attached to these nodes to track the underlying logic.

This is the reason the choice between the two SVG rendering approaches matters so much: one approach (fxsvgimage) produces genuine nodes that slot into this same scene graph, keeping the entire application's interaction logic consistent in one place. The other (WebView) renders content inside an embedded web browser, which is technically also a Node in the scene graph, but everything drawn inside it is invisible to JavaFX's own node system — it exists in a separate browser document with its own internal structure that JavaFX cannot see into or directly interact with. Any click, drag, or selection happening inside a WebView needs a completely separate piece of code (written in JavaScript, not Java) to detect and then relay back to the rest of the application.

We were having conflict on which idea to be used for reading -> parsing -> selection(drag and drop) of svg.
JavaFX has no built-in SVG image loader (unlike PNG/JPEG). SVG files contain vector elements (<path>, <circle>, <rect>, <g>) that JavaFX's Image class cannot interpret directly — parsing is required. Two viable approaches were evaluated for rendering gate, switch, and LED graphics from SVG source files.

2 ideas were finalized
1. fxsvgimage - SVG to Node Parsing
This library parses SVG markup and converts each element into a native JavaFX Shape node (SVGPath, Circle, Rectangle, etc.), grouped into a javafx.scene.Group. The resulting group is inserted directly into the application's existing scene graph, alongside wires, ports, and selection overlays.
2. WebView - Embedded Browser Rendering
JavaFX bundles a WebKit-based WebView component capable of rendering any SVG with full fidelity, since it uses a complete browser rendering engine. The SVG is loaded as web content inside an embedded browser viewport.

Decision - fxsvgimage to be opted for this project
The project's entire interaction model — component selection, dragging, port-to-port wiring, grid snapping — is built around JavaFX's native scene graph and mouse-event system operating on the Circuit/CircuitElement/Port domain model. fxsvgimage converts SVG graphics into real JavaFX nodes that plug directly into this existing model, requiring no parallel interaction system. WebView, while offering perfect rendering fidelity, isolates content inside a browser document that cannot be hit-tested or manipulated through the existing Java object model without significant bridging code — effectively duplicating the interaction layer in JavaScript. Given the project's non-functional requirement of supporting up to 5,000 logic components and 10,000 connections, fxsvgimage's lightweight node-based rendering is also better suited to scale than instantiating a full browser engine per component.

Right now, CircuitCanvasView draws everything on a single Canvas using GraphicsContext — this is called immediate-mode rendering: every frame, you manually recalculate every pixel position and repaint the whole thing from scratch. Nothing "remembers" itself between frames.

Once you switch to fxsvgimage, each gate becomes a real JavaFX Node living permanently in the scene graph — this is retained-mode rendering: nodes persist, and you move/scale them by changing their properties, not by redrawing pixels.

This actually makes zoom/pan easier, not harder.

This would be diagram for Node Graph
Pane (root)
 ├── Canvas (grid only — redrawn on pan/zoom, screen-space)
 └── Group "worldGroup" (scaled/translated as one unit)
      ├── Gate nodes (from fxsvgimage)
      ├── Switch/LED nodes
      └── Wire shapes

**Data Structures to be used**
Data Structure - Which we are going to use for this evaluation
1. ArrayList -> Circuit.elements, Circuit.wires - Ordered storage of all gates/switches/LEDs and all wires currently on the canvas
2. HashMap	-> Circuit.elementIndex (ID -> CircuitElement), Circuit.portIndex (ID -> Port) - O(1) lookup of any component/port by its unique ID, instead of scanning the list
(ID->CircuitElement) this is going to be the mapping and also same for ID and Port
3. HashMap -> ComponentFactory.componentRegistry (String -> ComponentType)	- Maps a type name to its enum constant for creation
4. HashMap ->	IdGenerator.counters (prefix -> running count) - Tracks the next free number per ID prefix (GATE-1, PORT-7, etc.)
5. Enum -> ComponentType, PortType, SignalState -	Fixed, closed sets of values — gate kinds, port direction, signal level
6. (IMPORTANT) JavaFX Scene Graph (Tree) -> *worldGroup (new, from the fxsvgimage pivot)*	Each parsed SVG gate becomes a node subtree; nesting them under one Group lets zoom/pan transform everything as a unit
7. List (bendPoints) -> Wire.bendPoints - Ordered path points for wire routing (currently empty/straight-line; will hold the 1-bend point later)
(This 7th point to be implemented later as it has orthogonal routing)

Data Structure - Which we have planned of implementing for further evaluations
1. HashSet -> SelectionManager.selectedIds - Fast add/remove/contains for multi-select, no duplicates
2. 2 Stacks -> CommandManager — undo stack + redo stack - Command-pattern undo/redo
3. Directed Graph (adjacency list via HashMap<Node, List<Edge>>) - Circuit Model implementation, that is, this would be the structure for storing the Circuit instead of having a GraphBuilder class
3. Queue -> Topological sort (Kahn's algorithm) for evaluation order - Determines the order gates must be evaluated in
4. Stack (or recursion) -> Cycle detection (DFS with visiting/visited marking) - Flags illegal feedback loops in a combinational only circuit

**Class Connectivity**
EditorWindow  (Application entry point)
 │
 ├─ creates → Circuit                         (the model — holds elements + wires)
 │
 ├─ creates → CircuitCanvasView                (extends Pane; renders via worldGroup + grid Canvas)
 │              └─ uses → ComponentFactory       (creates Gate/Switch/LedProbe instances)
 │              └─ uses → GridManager           (drawn as separate background Canvas layer)
 │              └─ (each gate/switch/LED node)  → parsed from SVG via fxsvgimage
 │
 ├─ creates → EditorController                 (orchestrator)
 │              ├─ holds → Circuit
 │              ├─ holds → CircuitCanvasView
 │              ├─ holds → ViewportManager     (drives worldGroup's scale/translate transform)
 │              └─ holds → GridManager         (redrawn on every pan/zoom change)
 │
 ├─ creates → EditorToolbar                    (buttons → call EditorController methods)
 │              └─ calls → EditorController.{toggleGrid, zoomIn, zoomOut,
 │                          activateSelectTool*, activateWireTool*, activatePanTool*,
 │                          undo*, redo*, stopSimulation*}   (* = currently stubbed)
 │
 └─ creates → CircuitElementPalette            (buttons per ComponentType)
                └─ calls → EditorController.selectComponentType(type)
                              └─ forwards to → CircuitCanvasView.setPendingType(type)


Circuit  (model, framework-independent)
 ├─ List<CircuitElement> elements
 ├─ List<Wire> wires
 ├─ HashMap elementIndex, portIndex
 └─ CircuitElement (abstract)
      ├─ Gate (AND/OR/NOT/NAND/NOR/XOR/XNOR — port count set by type)
      ├─ Switch (1 output port, toggleable)
      └─ LedProbe (1 input port, displays state)
           each holds → List<Port>
                          Port → SignalState, PortType, owner reference, connectedWire reference

Wire
 ├─ Port source, Port target
 └─ List<Point2D> bendPoints
