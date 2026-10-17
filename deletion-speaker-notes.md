# Speaker Notes: Deleting Elements and Wires

## 1. Selection

“Before deletion, the editor keeps two selections: element IDs and wire IDs. A component click in the Select tool reaches `SelectionManager.selectElement()`. For wires, `CircuitCanvas` calls `EditorController.onSelectPoint()`: a single click selects the hit segment, and a double-click gathers the connected wire group with `Circuit.getConnectedWires()` before selecting those IDs. `SelectionManager` fires its change callback so the canvas can redraw the selection highlight.”

Code locations:
- `CircuitCanvas.renderComponents()` — component click handler calls `selectElement()`.
- `CircuitCanvas` canvas click handler — calls `onSelectPoint()`.
- `EditorController.onSelectPoint()` — single-segment vs. connected-wire selection.
- `SelectionManager.selectElement()`, `selectWire()`, `selectWires()`, `fire()` — store IDs and notify the canvas.

## 2. Delete entry points and dispatch

“Delete can come from the toolbar or the Delete/Backspace key. Both routes reach `EditorController.deleteSelected()`. That method does nothing while a wire is being drawn, snapshots the selected element and wire IDs, and processes elements before explicitly selected wires. This order matters because deleting an element may already cascade-delete some of those selected wires.”

Code locations:
- `CircuitCanvas.setToolbar()` — connects `EditorToolbar` Delete to `controller::deleteSelected`.
- `EditorToolbar` button action and `setOnDelete()` — invokes the registered callback.
- `EditorWindow.start()` — Delete/Backspace key handler calls `controller.deleteSelected()`.
- `EditorController.deleteSelected()` — guards, reads selections, dispatches both removal paths.
- `EditorController.onElementDeleted()` — delegates to `Circuit.removeElement()`.

## 3. Element deletion and cascade

“`Circuit.removeElement(id)` first removes the element. It then scans the circuit for seed segments whose start or end Port belongs to that element. `belongsTo()` makes that owner check. Those segments enter a queue and a set. The method performs breadth-first traversal through their `PointNode` endpoints: each node provides its adjacent segments through `getConnectedSegments()`. The set prevents repeated visits. A Port endpoint stops the traversal, so the search does not continue through another component.”

“After the traversal, the method removes every discovered segment, removes each segment from its endpoint nodes using `PointNode.removeSegment()`, then prunes nodes that have no remaining segments. If the element ID was unknown, removal returns false.”

Code locations:
- `Circuit.removeElement()` — seed scan, BFS queue, removal, and orphan-node pruning.
- `Circuit.belongsTo()` — verifies `port.getOwner().getId()`.
- `PointNode.getConnectedSegments()` and `PointNode.removeSegment()` — adjacency and bookkeeping.
- `WireSegment.getStartPort()`, `getEndPort()`, `getStartNode()`, `getEndNode()` — endpoint relationships used by the traversal.

## 4. Explicit wire deletion and completion

“After element removal, `deleteSelected()` calls `Circuit.removeWireSegments(wireIds)` for wires selected directly. This removes matching IDs, updates connected nodes, and prunes orphan nodes. IDs already removed by the element cascade are simply absent and do not count as removed again.”

“Finally, the controller clears the selection. If anything actually changed, it calls `notifyRender()` and `recordChange()`: the canvas redraws, and a circuit snapshot enters undo history.”

Code locations:
- `Circuit.removeWireSegments()` — selected-ID removal and node cleanup.
- `EditorController.deleteSelected()` — selection clearing and changed check.
- `EditorController.notifyRender()` — delegates redraw to `CircuitCanvas.render()`.
- `EditorController.recordChange()` — snapshots the circuit into `HistoryManager` via `CircuitSerializer`.

## Closing line

“The important distinction is that deleting an element removes the wire network reachable from its ports through shared PointNodes, while deleting a selected wire removes only those selected segment IDs. Both paths repair PointNode bookkeeping before the canvas is refreshed.”