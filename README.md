# Semantic Node GUI

A node-based visual editor built with JavaFX. Drag and drop nodes onto a canvas, connect them together, and watch values propagate through the graph in real time.

![Java 17](https://img.shields.io/badge/Java-17-orange) ![JavaFX](https://img.shields.io/badge/JavaFX-17-blue)

## What it does

- Drag nodes from the sidebar onto an infinite pannable/zoomable canvas
- Connect output sockets to input sockets to build a data flow graph
- Nodes evaluate and propagate values downstream automatically
- Select, multi-select (drag box), and delete nodes

### Node types

| Node | Description |
|------|-------------|
| **Value** | Outputs a number, or displays an incoming value |
| **Math** | Arithmetic, trig, dot/cross product, magnitude, etc. Supports scalars and vectors |
| **Spreadsheet** | Load a CSV/XLSX file and output columns as vectors |
| **Split** | Split a vector into components or combine components into a vector |
| **Test** | Outputs a hardcoded list `[1, 2, 3, 4, 5]` for testing |

New node types are picked up automatically via reflection -- just extend `BaseNode` and they show up in the sidebar.

## Running

Requires Java 17+ and Maven.

```bash
cd semantic-app
mvn javafx:run
```

## Controls

- **Middle mouse drag** -- pan the canvas
- **Scroll wheel** -- zoom in/out
- **Left click drag** on canvas -- selection box
- **Shift+click** -- add to selection
- **Delete / Backspace** -- remove selected nodes
- **Right click** a connection or socket -- delete it
- **Drag** from an output socket to an input socket -- create a connection

## Project structure

```
semantic-app/src/main/java/com/example/
  SemanticApp.java          -- main app (reflection-based node discovery)
  nodes/
    BaseNode.java           -- abstract base, handles rendering/dragging/sockets
    MathNode.java           -- math operations
    ValueNode.java          -- numeric input/output
    SpreadsheetNode.java    -- CSV/XLSX loader
    SplitNode.java          -- split/combine vectors
    Socket.java             -- input/output socket logic
  connections/
    ConnectionLine.java     -- bezier curve connections between sockets
  ui/
    SocketDragManager.java  -- drag-to-connect interaction
    YourNodeManager.java    -- global node/connection registry
    NodeRegistry.java       -- class registry for node types
  misc/
    FastFrame.java          -- columnar data table (used by SpreadsheetNode)
    SimpleFrame.java        -- simpler row-based data table
```

## Built with

- JavaFX 17
- Apache POI (Excel support)
- org.reflections (runtime node discovery)
