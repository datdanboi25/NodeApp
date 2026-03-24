package com.example.nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import com.example.connections.ConnectionLine;
import com.example.nodes.MathNode.MathMode;

import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import java.lang.reflect.Array;

public class SplitNode extends BaseNode {
    public enum SplitMode {
        SPLIT, COMBINE
    }

    private int splitcount;
    private StackPane rendeRef;
    public static final int priority = 2;
    private static final String color = "#246283";
    private nodeType type;
    private SplitMode mode;
    private Object[] values;
    public static String name = "Split";

    public SplitNode(double x, double y) {
        super("Split", Color.web(color), x, y);
        this.type = nodeType.SPLIT;
        this.splitcount = 2;
        this.mode = SplitMode.SPLIT;

        reconfigureSockets();
    }

    public void setSplitCount(int count) {
        this.splitcount += count;
        reconfigureSockets();
    }

    public static String getColor() {
        return color;
    }

    public void setMode(SplitMode NewMode) {
        this.mode = NewMode;
        modeChange();
    }

    public nodeType getType() {
        return this.type;
    }

    public static String getName() {
        return name;
    }

    public SplitMode getMode() {
        return this.mode;
    }

    public Object getValue() {
        return values;
    }

    @Override
    protected void customReconfigure() {
        switch (mode) {
            case SPLIT -> {
                inputs.add(new Socket("List", List.of(SocketType.VECTOR, SocketType.VECTOR_ARRAY),
                        SocketDirection.INPUT, this));
                for (int i = 0; i < splitcount; i++) {
                    outputs.add(new Socket("Result" + i, List.of(SocketType.DOUBLE, SocketType.VECTOR),
                            SocketDirection.OUTPUT, this));
                }
            }
            case COMBINE -> {
                for (int i = 0; i < splitcount; i++) {
                    inputs.add(new Socket("Element" + i, List.of(SocketType.DOUBLE, SocketType.VECTOR),
                            SocketDirection.INPUT, this));
                }
                outputs.add(new Socket("Result", List.of(SocketType.DOUBLE, SocketType.VECTOR), SocketDirection.OUTPUT,
                        this));
            }
        }
    }

    @Override
    public void evaluate() {
        switch (mode) {
            case SPLIT -> {
                ConnectionLine line = inputs.get(0).getConnectionLines().get(0);
                Object val1 = line.getOutput().getParentNode().getValue();
                if (val1 != null && val1.getClass().isArray()) {
                    int length = Array.getLength(val1);
                    for (int i = 0; i < length; i++) {
                        Object element = Array.get(val1, i);
                        values[i] = element;
                    }
                }
            }
            case COMBINE -> {
                for (int i = 0; i < inputs.size(); i++) {
                    ConnectionLine line = inputs.get(i).getConnectionLines().get(0);
                    Object val1 = line.getOutput().getParentNode().getValue();
                    values[i] = val1;
                }
            }
        }
    }

    private void renderControls() {
        controlsBox.getChildren().clear();

        Region spacer = new Region();
        spacer.setMaxHeight(headerHeight);
        spacer.setMinHeight(headerHeight);
        controlsBox.getChildren().add(spacer);

        ComboBox<SplitMode> modeSelector = new ComboBox<>();
        modeSelector.getItems().addAll(SplitMode.values());
        modeSelector.setValue(getMode());
        // modeSelector.setTranslateY(nodeHeight / 2 - 20); // bottom-aligned
        // modeSelector.setTranslateX(nodeWidth / 2 - 60); // center horizontally
        modeSelector.setMaxWidth(120);
        modeSelector.getStyleClass().add("node-combo");
        VBox.setMargin(modeSelector, new Insets(0, 0, 6, 0));

        modeSelector.setCellFactory(list -> new ListCell<>() {
            {
                // setTextFill(Color.WHITE);
                // setStyle("-fx-background-color: #3a3a3a;");
            }

            @Override
            protected void updateItem(SplitMode item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.toString());
            }
        });

        modeSelector.setButtonCell(new ListCell<>() {
            {
                // setTextFill(Color.WHITE);
            }

            @Override
            protected void updateItem(SplitMode item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.toString());
            }
        });

        modeSelector.setOnAction(e -> {
            setMode(modeSelector.getValue());
            rerenderNode();
        });

        controlsBox.getChildren().add(modeSelector);
    }

}
