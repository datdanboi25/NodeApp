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

        YourNodeManager.register(this);

        if (!this.from.getConnectionLines().contains(this)) {
            this.from.getConnectionLines().add(this);
        }
        if (!this.to.getConnectionLines().contains(this)) {
            this.to.getConnectionLines().add(this);
        }
        to.getParentNode().evaluate();

        this.hitbox = new CubicCurve();
        hitbox.setStrokeWidth(12);
        hitbox.setStroke(Color.TRANSPARENT);
        hitbox.setFill(null);

        this.curve = new CubicCurve();
        this.startDot = new Circle(3, color);
        this.endDot = new Circle(3, color);

        curve.setStrokeWidth(2);
        curve.setStroke(color);
        curve.setFill(null);

        canvas.getChildren().addAll(hitbox, curve, startDot, endDot);
        update();

        hitbox.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                deleteConnection();
                e.consume();
            }
        });
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    public Socket getInput() {
        return to;
    }

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

    public void setInput(Socket newTo) {
        if (deleted) return;
        if (to != null) to.getConnectionLines().remove(this);
        to = newTo;
        if (to != null && !to.getConnectionLines().contains(this)) {
            to.getConnectionLines().add(this);
        }
        update();
    }

    public void setOutput(Socket newFrom) {
        if (deleted) return;
        if (from != null) from.getConnectionLines().remove(this);
        from = newFrom;
        if (from != null && !from.getConnectionLines().contains(this)) {
            from.getConnectionLines().add(this);
        }
        update();
    }

    public void deleteConnection() {
        if (deleted) return;

        if (from != null) {
            from.getConnectionLines().remove(this);
        }
        if (to != null) {
            to.getConnectionLines().remove(this);
        }

        YourNodeManager.unregister(this);

        if (hitbox.getParent() instanceof Pane p0) p0.getChildren().remove(hitbox);
        if (curve.getParent()  instanceof Pane p1) p1.getChildren().remove(curve);
        if (startDot.getParent() instanceof Pane p2) p2.getChildren().remove(startDot);
        if (endDot.getParent()   instanceof Pane p3) p3.getChildren().remove(endDot);

        from = null;
        to = null;

        deleted = true;
    }

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

    private void updateColor() {
        if (deleted || from == null) return;

        Object v = from.getParentNode().getValue();
        if (v instanceof Double) {
            color = Color.web("#63c763");
        } else if (v instanceof List) {
            color = Color.web("#6363c7");
        } else {
            color = Color.LIGHTGRAY;
        }
        curve.setStroke(color);
        startDot.setFill(color);
        endDot.setFill(color);
    }
}
