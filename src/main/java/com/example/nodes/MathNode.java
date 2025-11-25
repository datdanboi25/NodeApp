package com.example.nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import com.example.connections.ConnectionLine;

import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.Region;

public class MathNode extends BaseNode {
    public enum MathMode {
        ADD, SUBTRACT, MULTIPLY, DIVIDE, AVERAGE, DOT_PRODUCT, CROSS_PRODUCT, MAX, MIN, MAGNITUDE, COS, SIN, TAN, INV_COS, INV_SIN, INV_TAN
    }

    private MathMode mode = MathMode.ADD;
    private StackPane renderRef;
    public static final int priority = 2;
    private Object value; // Placeholder for computed value
    private static final String colour = "#246283";
    private nodeType type;
    public static String name = "Math";

    public MathNode(double x, double y) {
        super("Math", Color.web(colour), x, y);
        this.type = nodeType.MATH;
        this.mode = MathMode.ADD;

        reconfigureSockets();
    }

    public void setMode(MathMode NewMode) {
        this.mode = NewMode;
        modeChange();
    }

    public static String getColor() {
        return colour;
    }

    public MathMode getMode() {
        return this.mode;
    }

    public nodeType getType() {
        return this.type;
    }

    public static String getName() {
        return name;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }


@Override
protected void customReconfigure(){
    switch (mode) {
            case ADD, SUBTRACT, MULTIPLY, DIVIDE -> {
                inputs.add(new Socket("A", List.of(SocketType.DOUBLE, SocketType.VECTOR, SocketType.VECTOR_ARRAY), SocketDirection.INPUT, this));
                inputs.add(new Socket("B", List.of(SocketType.DOUBLE, SocketType.VECTOR, SocketType.VECTOR_ARRAY), SocketDirection.INPUT, this));
                outputs.add(new Socket("Result", List.of(SocketType.DOUBLE, SocketType.VECTOR, SocketType.VECTOR_ARRAY), SocketDirection.OUTPUT, this));
            }
            case AVERAGE -> {
                inputs.add(new Socket("List", List.of(SocketType.VECTOR, SocketType.VECTOR_ARRAY), SocketDirection.INPUT, this));
                outputs.add(new Socket("Result", List.of(SocketType.DOUBLE, SocketType.VECTOR), SocketDirection.OUTPUT, this));
            }
            case DOT_PRODUCT-> {
                inputs.add(new Socket("VectorA", List.of(SocketType.VECTOR, SocketType.VECTOR_ARRAY), SocketDirection.INPUT, this));
                inputs.add(new Socket("VectorB", List.of(SocketType.VECTOR, SocketType.VECTOR_ARRAY), SocketDirection.INPUT, this));
                outputs.add(new Socket("Result", List.of(SocketType.DOUBLE, SocketType.VECTOR), SocketDirection.OUTPUT, this));
            }
            case CROSS_PRODUCT -> {
                inputs.add(new Socket("VectorA", List.of(SocketType.VECTOR, SocketType.VECTOR_ARRAY), SocketDirection.INPUT, this));
                inputs.add(new Socket("VectorB", List.of(SocketType.VECTOR, SocketType.VECTOR_ARRAY), SocketDirection.INPUT, this));
                outputs.add(new Socket("Result", List.of(SocketType.VECTOR, SocketType.VECTOR_ARRAY), SocketDirection.OUTPUT, this));
            }
            case MAX, MIN -> {
                inputs.add(new Socket("List", List.of(SocketType.VECTOR), SocketDirection.INPUT, this));
                outputs.add(new Socket("Result", List.of(SocketType.DOUBLE), SocketDirection.OUTPUT, this));
            }
            case MAGNITUDE -> {
                inputs.add(new Socket("VectorA", List.of(SocketType.VECTOR, SocketType.VECTOR_ARRAY), SocketDirection.INPUT, this));
                outputs.add(new Socket("Result", List.of(SocketType.DOUBLE, SocketType.VECTOR), SocketDirection.OUTPUT, this));
            }
            case COS, SIN, TAN -> {
                inputs.add(new Socket("Angle", List.of(SocketType.DOUBLE, SocketType.VECTOR), SocketDirection.INPUT, this));
                outputs.add(new Socket("Result", List.of(SocketType.DOUBLE, SocketType.VECTOR), SocketDirection.OUTPUT, this));
            }
            case INV_COS, INV_SIN, INV_TAN -> {
                inputs.add(new Socket("Value", List.of(SocketType.DOUBLE, SocketType.VECTOR), SocketDirection.INPUT, this));
                outputs.add(new Socket("Angle", List.of(SocketType.DOUBLE, SocketType.VECTOR), SocketDirection.OUTPUT, this));
            }
        }
}



    protected void defineSockets() {
        reconfigureSockets();
    }

    @Override
    public void evaluate() {
        if (inputs.stream().anyMatch(socket -> !socket.isConnected())) {
            System.out.println(getTitle() + ": One or more inputs not connected.");
            return;
        }

        ConnectionLine line = inputs.get(0).getConnectionLines().get(0);
        Object val1 = line.getOutput().getParentNode().getValue();

        ConnectionLine line2 = inputs.size() > 1 ? inputs.get(1).getConnectionLines().get(0) : null;
        Object val2 = line2 != null ? line2.getOutput().getParentNode().getValue() : null;


        this.value = switch (mode) {
            case ADD -> performElementwise(val1, val2, (a, b) -> a + b);
            case SUBTRACT -> performElementwise(val1, val2, (a, b) -> a - b);
            case MULTIPLY -> performElementwise(val1, val2, (a, b) -> a * b);
            case DIVIDE -> performElementwise(val1, val2, (a, b) -> b != 0 ? a / b : null);
            case AVERAGE -> (val1 instanceof List<?> list && allAreDoubles(list))
                            ? average(toDoubleList(list)) : null;
            case DOT_PRODUCT -> (val1 instanceof List<?> l1 && val2 instanceof List<?> l2)
                                ? dotProduct(toDoubleList(l1), toDoubleList(l2)) : null;
            case CROSS_PRODUCT -> (val1 instanceof List<?> l1 && val2 instanceof List<?> l2)
                                ? crossProduct(toDoubleList(l1), toDoubleList(l2)) : null;
            case MIN -> (val1 instanceof List<?> list && allAreDoubles(list))
                        ? toDoubleList(list).stream().min(Double::compareTo).orElse(null) : null;
            case MAX -> (val1 instanceof List<?> list && allAreDoubles(list))
                        ? toDoubleList(list).stream().max(Double::compareTo).orElse(null) : null;
            case MAGNITUDE -> computeMagnitudes(val1);
            case COS -> (val1 instanceof Double angle) ? Math.cos(Math.toRadians(angle)) : null;
            case SIN -> (val1 instanceof Double angle) ? Math.sin(Math.toRadians(angle)) : null;
            case TAN -> (val1 instanceof Double angle) ? Math.tan(Math.toRadians(angle)) : null;
            case INV_COS -> (val1 instanceof Double value) ? Math.toDegrees(Math.acos(value)) : null;
            case INV_SIN -> (val1 instanceof Double value) ? Math.toDegrees(Math.asin(value)) : null;
            case INV_TAN -> (val1 instanceof Double value) ? Math.toDegrees(Math.atan(value)) : null;
        };
        System.out.print(val1 + " + " + val2 + " = " + value);
        propagate();
    }



    private List<Double> toDoubleList(List<?> list) {
        return list.stream().map(o -> (Double) o).toList();
    }

    private boolean allAreDoubles(List<?> list) {
        return list.stream().allMatch(o -> o instanceof Double);
    }

    private Double average(List<Double> list) {
        return list.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
    }

    private Double dotProduct(List<Double> v1, List<Double> v2) {
        if (v1.size() != v2.size()) return null;
        double sum = 0;
        for (int i = 0; i < v1.size(); i++) sum += v1.get(i) * v2.get(i);
        return sum;
    }

    private List<Double> crossProduct(List<Double> a, List<Double> b) {
        if (a.size() != 3 || b.size() != 3) return null;
        return List.of(
            a.get(1) * b.get(2) - a.get(2) * b.get(1),
            a.get(2) * b.get(0) - a.get(0) * b.get(2),
            a.get(0) * b.get(1) - a.get(1) * b.get(0)
        );
    }

    private Object computeMagnitudes(Object val) {
        if (val instanceof List<?> list) {
            if (list.isEmpty()) return null;
            if (list.get(0) instanceof Double) {
                return magnitude(toDoubleList(list));
            } else if (list.get(0) instanceof List<?>) {
                return list.stream()
                        .map(v -> magnitude(toDoubleList((List<?>) v)))
                        .toList();
            }
        }
        return null;
    }

    private Double magnitude(List<Double> vec) {
        return Math.sqrt(vec.stream().mapToDouble(x -> x * x).sum());
    }


    private Object performElementwise(Object a, Object b, BiFunction<Double, Double, Double> op) {
        // Case 1: list and scalar
        if (a instanceof List<?> list && allAreDoubles(list) && b instanceof Double scalar) {
            return toDoubleList(list).stream().map(x -> op.apply(x, scalar)).toList();
        }
        if (b instanceof List<?> list && allAreDoubles(list) && a instanceof Double scalar) {
            return toDoubleList(list).stream().map(x -> op.apply(scalar, x)).toList();
        }

        // Case 2: two lists
        if (a instanceof List<?> listA && b instanceof List<?> listB &&
            allAreDoubles(listA) && allAreDoubles(listB)) {

            List<Double> l1 = toDoubleList(listA);
            List<Double> l2 = toDoubleList(listB);
            if (l1.size() != l2.size()) return null;

            List<Double> result = new ArrayList<>();
            for (int i = 0; i < l1.size(); i++) {
                Double res = op.apply(l1.get(i), l2.get(i));
                result.add(res);
            }
            return result;
        }

        // Case 3: two scalars
        if (a instanceof Double da && b instanceof Double db) {
            return op.apply(da, db);
        }

        return null;
    }




    @Override
    public Node render() {
        renderRef = (StackPane) super.render(); // initializes container + controlsBox
        renderControls();
        return renderRef;
    }

    private void rerenderNode() {
        reconfigureSockets();     // updates internal socket lists
        renderControls();         // updates controlsBox

        // Update socket visuals (optional if your framework handles it elsewhere)
        rerenderSockets();          // ⬅️ you might need to call this if sockets are re-added
    }

    private void renderControls() {
        controlsBox.getChildren().clear();

        Region spacer = new Region();
        spacer.setMaxHeight(headerHeight);
        spacer.setMinHeight(headerHeight);
        controlsBox.getChildren().add(spacer);

        ComboBox<MathMode> modeSelector = new ComboBox<>();
        modeSelector.getItems().addAll(MathMode.values());
        modeSelector.setValue(getMode());
        //modeSelector.setTranslateY(nodeHeight / 2 - 20); // bottom-aligned
        //modeSelector.setTranslateX(nodeWidth / 2 - 60);  // center horizontally
        modeSelector.setMaxWidth(120);
        modeSelector.getStyleClass().add("node-combo");
        VBox.setMargin(modeSelector, new Insets(0, 0, 6, 0));

        modeSelector.setCellFactory(list -> new ListCell<>() {
            {
                //setTextFill(Color.WHITE);
                //setStyle("-fx-background-color: #3a3a3a;");
            }

            @Override
            protected void updateItem(MathMode item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.toString());
            }
        });

        modeSelector.setButtonCell(new ListCell<>() {
            {
                //setTextFill(Color.WHITE);
            }

            @Override
            protected void updateItem(MathMode item, boolean empty) {
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
