package com.example.nodes;

import com.example.nodes.MathNode.MathMode;

import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class TestNode extends BaseNode {
    private StackPane renderRef;
    private Object value;
    public static String name = "Test";
    private static final String color = "#d3d3d3";
    private nodeType type;

    public TestNode(double x, double y) {
        super("Test", Color.web(color), x, y);
        this.type = nodeType.TEST;
        this.value = new ArrayList<>(List.of(1.0, 2.0, 3.0, 4.0, 5.0));
        reconfigureSockets();
    }

    public static String getColor() {
        return color;
    }
    public nodeType getType() {
        return this.type;
    }

    public Object getValue() {
        return this.value;
    }

    public static String getName() {
        return name;
    }

    @Override
    protected void customReconfigure(){
        outputs.add(new Socket("List", List.of(SocketType.VECTOR), SocketDirection.OUTPUT, this));
    }
/* 
    protected void defineSockets() {
        reconfigureSockets();
    }
 */
    @Override
    public void evaluate() {
        this.value = List.of(1.0, 2.0, 3.0, 4.0, 5.0);
    }

    @Override
    public Node render() {
        renderRef = (StackPane) super.render();
        renderControls();
        return renderRef;
    }
/*
    private void rerenderNode() {
        reconfigureSockets();
        renderControls();
        rerenderSockets();
    }
*/
    private void renderControls() {}
}
