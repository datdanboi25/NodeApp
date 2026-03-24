package com.example.nodes;

import java.util.List;

import com.example.connections.ConnectionLine;
import com.example.misc.tools;
import com.example.nodes.ValueNode.ValueMode;

import javafx.application.Platform;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.geometry.Insets;
import javafx.scene.Node;

public class ValueNode extends BaseNode {

    public enum ValueMode {
        OUT, IN
    }

    private ValueMode mode = ValueMode.OUT;
    private StackPane renderRef;
    private static String colour = "#96289A";
    private Object value;
    public static int priority = 1;
    private nodeType type;
    private static String name = "Value";
    private Label valueDisplayLabel = null;

    public ValueNode(double x, double y) {
        super("Value", Color.web(colour), x, y);
        this.mode = ValueMode.OUT;
        this.value = 0;
        this.type = nodeType.VALUE;
        
        reconfigureSockets();
    }

    public void setMode(ValueMode NewMode) {
        this.mode = NewMode;
        priority = mode == ValueMode.OUT ? 1 : 3;
        rerenderNode();
    }

    public static String getColor() {
        return colour;
    }

    public static String getName() {
        return name;
    }

    protected void customReconfigure() {
        switch (mode) {
            case OUT -> outputs.add(new Socket("Out", List.of(SocketType.DOUBLE), SocketDirection.OUTPUT, this));
            case IN -> inputs.add(new Socket("In", List.of(SocketType.DOUBLE), SocketDirection.INPUT, this));
        }
    }

    public ValueMode getMode() {
        return this.mode;
    }

    public void setValue(double value) {
        this.value = value;
        propagate();
    }

    public Object getValue() {
        return this.value;
    }

    public nodeType getType() {
        return this.type;
    }

    

    @Override
    public void evaluate() {
        
        if (mode == ValueMode.IN && !inputs.isEmpty() && !inputs.get(0).getConnectionLines().isEmpty()) {
            ConnectionLine line = inputs.get(0).getConnectionLines().get(0);
            Object val1 = line.getOutput().getParentNode().getValue();
            if (val1 instanceof Double) {
                this.value = (Double) val1;
            } else if (val1 instanceof List) {
                List<?> lst = (List<?>) val1;
                if (!lst.isEmpty() && lst.get(0) instanceof Double) {
                    this.value = (Double) lst.get(0);
                }
            }
            //this.value = (val1 instanceof Double) ? (Double) val1 : ;

            if (valueDisplayLabel != null) {
                valueDisplayLabel.setText("  " + formatDigitToString(this.value));
            }
        }

        propagate();
    }

    private void validateAndCommit(TextField field) {
        String text = field.getText();
        if (tools.isValidDouble(text)) {
            setValue(Double.parseDouble(text));
        } else {
            field.setText(String.valueOf(getValue())); // Revert to last good value
        }
    }

    private void validateAndCommit(String text) {
        if (tools.isValidDouble(text)) {
            setValue(Double.parseDouble(text));
        }
    }

    @Override
    public Node render() {
        renderRef = (StackPane) super.render();
        renderControls();
        return renderRef;
    }

    private void rerenderNode() {
        reconfigureSockets();
        renderControls();
        rerenderSockets();
    }

    private void renderControls() {
        controlsBox.getChildren().clear();
        Region spacer = new Region();
        spacer.setMaxHeight(headerHeight);
        spacer.setMinHeight(headerHeight);
        controlsBox.getChildren().add(spacer);
        valueDisplayLabel = null;
        
        ComboBox<ValueMode> modeSelector = new ComboBox<>();
        modeSelector.getItems().addAll(ValueMode.values());
        modeSelector.setValue(getMode());
        //modeSelector.setTranslateY(nodeHeight / 2 - 20);
        //modeSelector.setTranslateX(nodeWidth / 2 - 60);
        modeSelector.setMaxWidth(nodeWidth - 20);
        modeSelector.getStyleClass().add("node-combo");
        //modeSelector.setStyle("-fx-background-color: #3a3a3a; -fx-border-color: #555; -fx-border-radius: 3; -fx-background-radius: 3;");
        
        modeSelector.setCellFactory(list -> new ListCell<>() {
            { //setTextFill(Color.WHITE); setStyle("-fx-background-color: #3a3a3a;"); 
        }
        @Override protected void updateItem(ValueMode item, boolean empty) {
            super.updateItem(item, empty);
            setText(empty || item == null ? null : item.toString());
        }
    });
        modeSelector.setButtonCell(new ListCell<>() {
            { 
                //setTextFill(Color.WHITE); 
            }
            @Override protected void updateItem(ValueMode item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.toString());
            }
        });
        
        modeSelector.setOnAction(e -> {
            setMode(modeSelector.getValue());
            rerenderNode();
        });
        
        controlsBox.getChildren().add(modeSelector);
        
        if (getMode() == ValueMode.OUT) {
            TextField valueField = new TextField(String.valueOf(getValue()));
            //valueField.setTranslateY(nodeHeight / 2 - 25);
            //valueField.setTranslateX(nodeWidth / 2 - 60);
            valueField.setMaxWidth(nodeWidth - 20);
            valueField.getStyleClass().add("node-text-field");
            
            valueField.setOnAction(e -> {
                validateAndCommit(valueField);
                evaluate();
            });
            
            valueField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                if (!isNowFocused) {
                    validateAndCommit(valueField);
                    evaluate();
                }
            });
            
            controlsBox.getChildren().add(valueField);
            System.out.println("HEIGHT:" + valueField.getHeight());
        } else {
            valueDisplayLabel = new Label("  " + getValue());
            //valueDisplayLabel.setTranslateY(nodeHeight / 2 - 25);
            //valueDisplayLabel.setTranslateX(nodeWidth / 2 - 60);
            valueDisplayLabel.setMaxWidth(nodeWidth - 20);
            valueDisplayLabel.setPrefHeight(27);
            valueDisplayLabel.getStyleClass().add("node-text-field");
            valueDisplayLabel.setStyle("-fx-border-color: transparent;");
            VBox.setMargin(valueDisplayLabel, new Insets(0, 0, 6, 0));
            controlsBox.getChildren().add(valueDisplayLabel);
        }
        
    }
}
