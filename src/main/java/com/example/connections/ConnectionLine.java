package com.example.connections;

import javafx.geometry.Bounds;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.CubicCurve;

import java.util.List;

import com.example.nodes.Socket;
import com.example.ui.YourNodeManager;

public class ConnectionLine {
    private final CubicCurve curve;
    private final Circle startDot;
    private final Circle endDot;
    private final CubicCurve hitbox;

    private Socket from;
    private Socket to;
    private Object value;
    private Color color = Color.LIGHTGRAY;

    private boolean deleted = false;

    public ConnectionLine(Socket from, Socket to, Pane canvas) {
        this.from = from;
        this.to = to;

        // Register with a global manager if you use one
        YourNodeManager.register(this);

        // Register with sockets so capacity checks & UI state are correct
        if (!this.from.getConnectionLines().contains(this)) {
            this.from.getConnectionLines().add(this);
        }
        if (!this.to.getConnectionLines().contains(this)) {
            this.to.getConnectionLines().add(this);
        }
        to.getParentNode().evaluate();

        // Hitbox (wide invisible stroke for interactions)
        this.hitbox = new CubicCurve();
        hitbox.setStrokeWidth(12);
        hitbox.setStroke(Color.TRANSPARENT);
        hitbox.setFill(null);

        // Visible curve and endpoint dots
        this.curve = new CubicCurve();
        this.startDot = new Circle(3, color);
        this.endDot = new Circle(3, color);

        curve.setStrokeWidth(2);
        curve.setStroke(color);
        curve.setFill(null);

        // Add to canvas
        canvas.getChildren().addAll(hitbox, curve, startDot, endDot);

        // First layout pass
        update();

        // Right-click to delete this connection via the central delete method
        hitbox.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                deleteConnection();
                e.consume();
            }
        });
    }

    // ----- Public API -----

    public void setValue(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    /** Input side (the "to" socket) */
    public Socket getInput() {
        return to;
    }

    /** Output side (the "from" socket) */
    public Socket getOutput() {
        return from;
    }

    public CubicCurve getCurve() {
        return curve;
    }

    public Socket getFrom() {
        return from;
    }

    public Socket getTo() {
        return to;
    }

    /**
     * Safely retarget input; keeps socket connection lists consistent.
     */
    public void setInput(Socket newTo) {
        if (deleted) return;
        if (to != null) to.getConnectionLines().remove(this);
        to = newTo;
        if (to != null && !to.getConnectionLines().contains(this)) {
            to.getConnectionLines().add(this);
        }
        update();
    }

    /**
     * Safely retarget output; keeps socket connection lists consistent.
     */
    public void setOutput(Socket newFrom) {
        if (deleted) return;
        if (from != null) from.getConnectionLines().remove(this);
        from = newFrom;
        if (from != null && !from.getConnectionLines().contains(this)) {
            from.getConnectionLines().add(this);
        }
        update();
    }

    /**
     * Central deletion: detach from sockets, unregister, remove visuals.
     * After this, sockets are free to start new drags.
     */
    public void deleteConnection() {
        if (deleted) return;

        // Detach from sockets
        if (from != null) {
            from.getConnectionLines().remove(this);
        }
        if (to != null) {
            to.getConnectionLines().remove(this);
        }

        // Unregister globally if applicable
        YourNodeManager.unregister(this);

        // Remove visuals from the scene graph (defensive)
        if (hitbox.getParent() instanceof Pane p0) p0.getChildren().remove(hitbox);
        if (curve.getParent()  instanceof Pane p1) p1.getChildren().remove(curve);
        if (startDot.getParent() instanceof Pane p2) p2.getChildren().remove(startDot);
        if (endDot.getParent()   instanceof Pane p3) p3.getChildren().remove(endDot);

        // Break endpoint references to avoid stale checks elsewhere
        from = null;
        to = null;

        deleted = true;
    }

    /**
     * Repositions the curve/dots based on current socket locations.
     * Safe no-ops after deletion or before visuals are ready.
     */
    public void update() {
        if (deleted) return;
        if (from == null || to == null) return;
        if (from.getRenderRef() == null || to.getRenderRef() == null) return;
        if (curve.getParent() == null) return;

        updateColor();

        Bounds a = from.getRenderRef().localToScene(from.getRenderRef().getBoundsInLocal());
        Bounds b = to.getRenderRef().localToScene(to.getRenderRef().getBoundsInLocal());

        double startX = curve.getParent().sceneToLocal(a.getCenterX(), a.getCenterY()).getX();
        double startY = curve.getParent().sceneToLocal(a.getCenterX(), a.getCenterY()).getY();
        double endX   = curve.getParent().sceneToLocal(b.getCenterX(), b.getCenterY()).getX();
        double endY   = curve.getParent().sceneToLocal(b.getCenterX(), b.getCenterY()).getY();

        double controlOffset = Math.abs(endX - startX) * 0.5;

        curve.setStartX(startX);
        curve.setStartY(startY);
        curve.setEndX(endX);
        curve.setEndY(endY);
        curve.setControlX1(startX + controlOffset);
        curve.setControlY1(startY);
        curve.setControlX2(endX - controlOffset);
        curve.setControlY2(endY);

        startDot.setCenterX(startX);
        startDot.setCenterY(startY);
        endDot.setCenterX(endX);
        endDot.setCenterY(endY);

        hitbox.setStartX(startX);
        hitbox.setStartY(startY);
        hitbox.setEndX(endX);
        hitbox.setEndY(endY);
        hitbox.setControlX1(startX + controlOffset);
        hitbox.setControlY1(startY);
        hitbox.setControlX2(endX - controlOffset);
        hitbox.setControlY2(endY);
    }

    @Override
    public String toString() {
        if (from == null || to == null) return "(detached connection)";
        return from.getFullId() + " -----> " + to.getFullId();
    }

    // ----- Internals -----

    private void updateColor() {
        if (deleted || from == null) return;

        Object v = from.getParentNode().getValue();
        if (v instanceof Double) {
            color = Color.web("#63c763");     // numeric -> green
        } else if (v instanceof List) {
            color = Color.web("#6363c7");     // list -> purple
        } else {
            color = Color.LIGHTGRAY;          // default
        }
        curve.setStroke(color);
        startDot.setFill(color);
        endDot.setFill(color);
    }
}
