# Digital Logic Editor

A desktop application for designing and simulating digital logic circuits, built with **Java** and **JavaFX**.
Place gates, switches and LEDs on a canvas, wire them together, validate the design, run a live simulation and
generate truth tables, all from one editor.

## Features

- **Component palette**: AND, OR, NOT, XOR, NAND, NOR gates, input switches and output LEDs, drawn from SVG artwork.
- **Wiring**: drag from a port to another port to draw orthogonal wires. Drag from the middle of a wire to create a junction.
- **Selection**: click, Shift/Ctrl-click for multi-select, or drag a rubber band around several items.
- **Move**: drag selected components. Wires and junctions follow, and moves that would overlap another component are rejected.
- **Undo / Redo**: every edit (place, move, delete, wire, switch toggle) is recorded as a command.
- **Validation**: finds dangling wires, unconnected inputs, missing switches/LEDs, dead logic, multiple drivers on one wire and combinational loops. Problem items are highlighted in red and listed in an error panel.
- **Live simulation**: signals are pushed through the circuit in topological order. Click a switch and the LEDs and wire colours update.
- **Truth tables**: one table per disconnected sub-circuit, shown in a separate window. Selecting a table highlights that sub-circuit on the canvas.
- **Save / Load** circuits as JSON, and **Import / Export SVG**.
- **Zoom and pan** with the mouse, toolbar or keyboard.

## Tech stack

| Area | Technology |
|---|---|
| Language | Java 17 or newer |
| UI | JavaFX (controls, graphics) |
| SVG rendering | [FXSVGImage](https://github.com/hervegirod/fxsvgimage) (`org.girod.javafx.svgimage`) |
| JSON persistence | Gson |
| Build tool | Maven (`javafx-maven-plugin`) |

## Prerequisites

You need three things: a **JDK 17+**, **Maven 3.8+** and **Git**.
JavaFX does **not** need a separate installation. Maven downloads it automatically the first time you run the project.

Check what you already have:

```bash
java -version     # should print 17 or higher
mvn -version      # should print Maven 3.8 or higher, and the same Java version
git --version
```

If all three work, skip to [Running the project](#running-the-project).

### Installing the prerequisites

**Ubuntu / Debian**

```bash
sudo apt update
sudo apt install -y openjdk-17-jdk maven git
```

**Fedora**

```bash
sudo dnf install -y java-17-openjdk-devel maven git
```

**Arch Linux**

```bash
sudo pacman -S jdk17-openjdk maven git
```

**macOS (Homebrew)**

```bash
brew install openjdk@17 maven git
sudo ln -sfn "$(brew --prefix openjdk@17)/libexec/openjdk.jdk" /Library/Java/JavaVirtualMachines/openjdk-17.jdk
```

**Windows**

Using `winget` for the JDK and Git, and Chocolatey for Maven (run in an administrator terminal):

```powershell
winget install EclipseAdoptium.Temurin.17.JDK
winget install Git.Git
choco install maven
```

If you do not use Chocolatey, download the Maven binary zip from <https://maven.apache.org/download.cgi>, unzip it, and add its `bin` folder to your `PATH`.

Open a **new** terminal afterwards so the new `PATH` is picked up.

**If `java` or `mvn` is not found after installing**, set `JAVA_HOME`:

```bash
# Linux example (adjust the path to your JDK)
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
```

#### Optional: installing JavaFX manually

Not required for `mvn javafx:run`. Only do this if you want a system-wide JavaFX SDK for your IDE.

```bash
# Ubuntu / Debian
sudo apt install -y openjfx

# macOS
brew install openjfx
```

On Windows, or for any other platform, download the SDK from <https://gluonhq.com/products/javafx/> and unzip it.

## Running the project

1. **Fork** this repository using the **Fork** button at the top right of the GitHub page.
2. **Clone your fork** and go into the project folder:

   ```bash
   git clone https://github.com/<your-username>/<repository-name>.git
   cd <repository-name>
   ```

   Make sure you are in the folder that contains `pom.xml`.

3. **Build and run:**

   ```bash
   mvn clean javafx:run
   ```

The first run downloads dependencies, so it takes a little longer. The **Digital Logic Editor** window opens when the build finishes.

> The command is `javafx:run`, written with a **colon** and no spaces.
> `mvn javafx run` fails with `Unknown lifecycle phase "javafx"`.

## How to use the editor

### Building a circuit

1. Pick a component from the **palette** on the left (AND, OR, SWITCH, LED, and so on), then click the canvas to place it.
2. Choose **Select** in the toolbar to go back to selecting and moving things.
3. **Draw a wire** by dragging from a port dot to another port. Output ports connect to input ports.
4. **Create a junction** by dragging from the middle of an existing wire.
5. Click **Validate** to check the circuit, then **Simulate** to run it. Click a switch to toggle it.
6. Click **TruthTable** to see the truth table of every complete sub-circuit.

### Toolbar

| Button | Action |
|---|---|
| Select | Select, move and wire components |
| Delete | Delete the selected items |
| Undo / Redo | Step backward or forward through your edits |
| Zoom_In / Zoom_Out | Zoom the canvas |
| Simulate | Start or stop the live simulation (only starts if the circuit is valid) |
| Validate | Run all validation checks |
| TruthTable | Open the truth-table window |
| Home | Reset the view |

### Mouse and keyboard

| Input | Action |
|---|---|
| Left click | Select, or place the chosen component |
| Shift / Ctrl + click | Add to or remove from the selection |
| Drag on empty canvas | Rubber-band selection |
| Drag a selected component | Move the selection |
| Click a switch | Toggle it ON / OFF |
| Right mouse drag | Pan the canvas |
| Ctrl + scroll wheel | Zoom |
| Ctrl + `+` / Ctrl + `-` | Zoom in / out |
| `Esc` | Clear the selection |
| `Delete` / `Backspace` | Delete the selection |
| Ctrl + Z | Undo |
| Ctrl + Shift + Z or Ctrl + Y | Redo |
| Ctrl + N / O / S | New / Open / Save |

### File menu

| Item | Description |
|---|---|
| New | Start an empty circuit |
| Open / Load... | Open a circuit saved as `.json` |
| Save / Save As... | Save the circuit as `.json` |
| Import SVG... / Export SVG... | Exchange the circuit through an `.svg` file |
| Exit | Close the editor |

## How it works

### Validation

`CircuitValidator` and `ConnectionValidator` check a circuit before it is simulated, and a wire is also checked at the moment you draw it:

- dangling wires and unconnected gate or LED inputs
- missing switch or LED in a sub-circuit
- dead logic (gates that do not contribute to any LED)
- more than one driver on the same wire network
- combinational loops, which are rejected when you draw the wire and detected again during sorting

### Simulation

1. `CircuitTopology` groups wires and junctions into electrical networks and builds a dependency graph between elements.
2. **Kahn's algorithm** produces a topological order. If some elements cannot be ordered, the circuit contains a loop.
3. `CompiledCircuit` / the simulator walks the elements in that order. Switches push their value onto the wires, gates call `evaluate()`, and LEDs latch their input. Every gate is therefore evaluated only after every gate that feeds it.

### Sub-circuits and truth tables

`Circuit.splitIntoDisjointSubCircuits()` runs a BFS over the wire graph and returns each connected piece as a `SubCircuit`.
For each piece, `TruthTableGenerator` validates it, sorts it, and drives every combination of its switches through the simulator
(2<sup>n</sup> rows for n switches, up to **10 switches** per sub-circuit). The switches are restored to their original state afterwards.
Unfinished sub-circuits show an error message instead of a table, and do not block the others.

### Design

| Concept | Where |
|---|---|
| Layered architecture (GUI / controller / domain / validation / persistence) | package layout below |
| Command pattern for undo / redo | `controller.command`, `HistoryManager` |
| Observer / callbacks for selection changes | `SelectionManager` |
| Polymorphic creation (each `ComponentType` builds its own element) | `ComponentType` |
| DTOs for saving and loading | `persistence.CircuitDto`, `CircuitSerializer` |

## Project structure

```
src/main/java/com/example/mini_project/
├── controller/          EditorController, SelectionManager, HistoryManager
│   └── command/         Command pattern: place, move, remove, wire change, toggle input
├── domain/              Circuit, gates, Port, WireSegment, PointNode, SubCircuit, SvgIconRegistry
├── gui/                 EditorWindow, CircuitCanvas, EditorToolbar, CircuitElementPalette,
│                        TruthTableWindow, ValidationErrorPanel, ViewportManager
├── persistence/         CircuitDto, CircuitSerializer (JSON / SVG)
└── validation/          CircuitValidator, ConnectionValidator, CircuitTopology,
                         CompiledCircuit, TruthTableGenerator, TruthTableData, SubCircuitTable
```

## Troubleshooting

| Problem | Fix |
|---|---|
| `Unknown lifecycle phase "javafx"` | You typed `mvn javafx run`. Use `mvn clean javafx:run` (colon, no space). |
| `No plugin found for prefix 'javafx'` or `there is no POM in this directory` | Run the command from the folder that contains `pom.xml`. |
| `release version 17 not supported` or `invalid target release` | Your JDK is too old. Install JDK 17+ and check `java -version` and `mvn -version` both report it. |
| `JAVA_HOME is not defined correctly` | Set `JAVA_HOME` to your JDK folder (see the installation section). |
| `JavaFX runtime components are missing` | Start the app with `mvn javafx:run` and not by running the class directly. In an IDE, add the JavaFX modules as VM options. |
| Window does not open on Linux over SSH or WSL | JavaFX needs a display. Use a local desktop session, WSLg, or an X server. |
| Dependencies fail to download | Check your network or proxy settings, then retry. `mvn clean javafx:run -U` forces an update. |

## Authors

Piyush Deshpande, Prajwal More, Niranjan Gavali, Anay Mulay
