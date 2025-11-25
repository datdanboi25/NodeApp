package com.example.nodes;

import java.util.*;

import com.example.connections.ConnectionLine;
import com.example.ui.SocketDragManager;
import com.example.ui.YourNodeManager;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;

public abstract class BaseNode {
    protected String id;
    protected String title;
    protected double x, y;
    protected Color colour;
    protected StackPane renderRef;

    protected double nodeWidth = 150;
    protected double nodeHeight = 100;
    protected double nodeMinHeight = 100;
    protected double nodeMinWidth = 150;
    protected double headerHeight = 25;

    protected StackPane container;
    protected VBox controlsBox = new VBox(6);

    protected Rectangle bodyBox;
    protected Rectangle headerBar;
    protected Text titleText;
    protected BooleanProperty selected = new SimpleBooleanProperty(false);

    protected List<Socket> inputs = new ArrayList<>();
    protected List<Socket> outputs = new ArrayList<>();
    private static final Map<BaseNode, Point2D> dragStartPositions = new HashMap<>();
    private final double[] dragStartMouse = new double[2]; 
    private boolean wasDragged = false;
    private boolean startedOnSelected = false;
    private static final String name = "Base";


    

    public BaseNode(String title, Color colour, double x, double y) {
        this.id = UUID.randomUUID().toString();
        VBox.setVgrow(controlsBox, Priority.ALWAYS);
        //controlsBox.setAlignment(Pos.BOTTOM_CENTER);
        controlsBox.setAlignment(Pos.TOP_CENTER);
        //controlsBox.setStyle("-fx-border-color: red; -fx-border-width: 2;");
        this.title = title;
        this.x = x;
        this.y = y;
        this.colour = colour;
    }

    //protected abstract void defineSockets();
    public abstract void evaluate();
    protected abstract void customReconfigure();
    public abstract Object getValue();
    public abstract nodeType getType();
    //public abstract String getName();

    public List<Socket> getInputs() { return inputs; }
    public List<Socket> getOutputs() { return outputs; }

    public String getTitle() { return title; }
    public double getX() { return x; }
    public double getY() { return y; }

    public boolean isSelected() { return selected.get(); }
    public final void setSelected(boolean value) { selected.set(value); }
    public final BooleanProperty selectedProperty() { return selected; }

    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Node getRenderRef() {
        return renderRef;
    }

    public String getId() {
        return id;
    }

    public void modeChange(){
        reconfigureSockets(); // Reconfigure sockets based on the new mode
        rerenderSockets();     // Update visual representation
        evaluate();
        propagate();           // Trigger downstream updates
    }

    public void delete(Pane canvas) {
        for (ConnectionLine conn : new ArrayList<>(getConnections())) {
            canvas.getChildren().remove(conn.getCurve());
            conn.deleteConnection();
        }
        YourNodeManager.unregister(this);
        if (renderRef != null && renderRef.getParent() instanceof Pane parentPane) {
            parentPane.getChildren().remove(renderRef);
        }
    }

    public List<ConnectionLine> getInputConnections() {
        List<ConnectionLine> inputLines = new ArrayList<>();
        for (Socket input : inputs) {
            inputLines.addAll(input.getConnectionLines());
        }
        return inputLines;
    }

    public List<ConnectionLine> getOutputConnections() {
        List<ConnectionLine> outputLines = new ArrayList<>();
        for (Socket output : outputs) {
            outputLines.addAll(output.getConnectionLines());
        }
        return outputLines;
    }

    public List<ConnectionLine> getConnections() {
        List<ConnectionLine> connections = new ArrayList<>();
        connections.addAll(getInputConnections());
        connections.addAll(getOutputConnections());
        return connections;
    }

    protected void propagate() {
        for (Socket output : outputs) {
            for (ConnectionLine line : output.getConnectionLines()) {
                BaseNode target = line.getInput().getParentNode();
                target.evaluate();
            }
        }
    }

    

    protected String formatDigitToString(Object value) { 
        if (value == null) {
            return null;
        } else {
            Object val = value; // replace with actual value source
            if (val instanceof Number num) {
                double d = num.doubleValue();
                if (Math.abs(d - Math.round(d * 100000.0) / 100000.0) < 1e-9) {
                    // Trim trailing zeros but keep up to 5dp
                    String text = String.format("%.5f", d).replaceAll("0+$", "").replaceAll("\\.$", "");
                    return text;
                } else {
                    return String.format("%.5f", d);
                }
            } else {
                return value.toString();
            }
        }
    }

    protected void reconfigureSockets(){
        // Step 1: Save connection lines
        List<ConnectionLine> oldInputs = new ArrayList<>();
        for (Socket s : inputs) oldInputs.addAll(s.getConnectionLines());

        List<ConnectionLine> oldOutputs = new ArrayList<>();
        for (Socket s : outputs) oldOutputs.addAll(s.getConnectionLines());

        // Step 2: Clear + recreate sockets based on mode
        inputs.clear();
        outputs.clear();

        this.customReconfigure();
    

        // Step 3: Try to reconnect old inputs
        for (ConnectionLine line : oldInputs) {
            Socket match = findBestMatchingSocket(inputs, line.getInput());
            if (match != null) {
                line.setInput(match);
            } else {
                line.deleteConnection(); // fallback
            }
        }

        // Step 4: Try to reconnect old outputs
        for (ConnectionLine line : oldOutputs) {
            Socket match = findBestMatchingSocket(outputs, line.getOutput());
            if (match != null) {
                line.setOutput(match);
            } else {
                line.deleteConnection(); // fallback
            }
        }
    }


    // ✅ New version
    private Socket findBestMatchingSocket(List<Socket> candidates, Socket original) {
        for (Socket s : candidates) {
            if (Objects.equals(s.getName(), original.getName())) {// check by name
                for (SocketType t : s.getTypeList()) {
                    if (original.getTypeList().contains(t)) {
                        return s;
                    }
            }
            }
        }
        // Fallback to type-based match
        /* 
        for (Socket s : candidates) {
            for (SocketType t : s.getTypeList()) {
                if (original.getTypeList().contains(t)) {
                    return s;
                }
            }
        }
         */
        return null;
    }


    protected void rerenderSockets() {
        renderRef.getChildren().removeIf(n -> n instanceof Circle);

        int numInputs = inputs.size();
        for (int i = 0; i < numInputs; i++) { // for input
            double y = (-nodeHeight / 2.0) + (nodeHeight / (numInputs + 1)) * (i + 1);
            Socket socket = inputs.get(i);

            Circle socketCircle = new Circle(5, Color.DARKRED);
            socket.setRenderRef(socketCircle);
            socketCircle.setTranslateX(-nodeWidth / 2.0);
            socketCircle.setTranslateY(y);
            socketCircle.setMouseTransparent(false);
            socketCircle.setPickOnBounds(true);
            
            renderRef.getChildren().add(socketCircle);
            
            socketCircle.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.SECONDARY) {
                    System.out.println("Destroying connections for socket: " + socket.getName());
                    socket.destroyConnections();
                    e.consume();
                }
            });
        }
        
        int numOutputs = outputs.size();
        for (int i = 0; i < numOutputs; i++) {
            double y = (-nodeHeight / 2.0) + (nodeHeight / (numOutputs + 1)) * (i + 1);
            Socket socket = outputs.get(i);
            
            Circle socketCircle = new Circle(5, Color.DARKGREEN);
            socket.setRenderRef(socketCircle);
            socketCircle.setTranslateX(nodeWidth / 2.0);
            socketCircle.setTranslateY(y);

            final boolean[] dragging = {false};
            socketCircle.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.SECONDARY) {
                    socket.destroyConnections();
                    e.consume();
                }
            });
            
            socketCircle.setOnMousePressed(e -> {
                if (e.isPrimaryButtonDown()) {
                    dragging[0] = true;
                    SocketDragManager.startDrag(socket, socketCircle, e);
                    e.consume();
                }
            });

            socketCircle.setOnMouseDragged(e -> {
                if (dragging[0]) {
                    SocketDragManager.updateDrag(socketCircle, e);
                    e.consume();
                }
            });

            socketCircle.setOnMouseReleased(e -> {
                dragging[0] = false;
                SocketDragManager.endDrag(socketCircle, e);
                e.consume();
            });


            renderRef.getChildren().add(socketCircle);
            System.out.println("Rendering socket " + i + ": " + socket.getName() + " -> Y = " + y);

        }
    }


    public void rerenderNodeSize() {
        // Use layout bounds so we get the laid-out height of the VBox
        double contentHeight = controlsBox.getLayoutBounds().getHeight();
        System.out.println("Content height: " + contentHeight);

        double totalHeight = contentHeight; // + extra padding if you want
        this.nodeHeight = Math.max(totalHeight, nodeMinHeight);

        // bodyBox is a Rectangle
        bodyBox.setHeight(nodeHeight);
        bodyBox.setWidth(nodeWidth);
        headerBar.setWidth(nodeWidth);

        // renderRef is presumably a Region backing the control
        renderRef.setPrefHeight(nodeHeight);

        // keep header/title positioned relative to the node's center
        headerBar.setTranslateY(-nodeHeight / 2 + headerHeight / 2);
        titleText.setTranslateY(-nodeHeight / 2 + headerHeight / 2);
        //titleText.setTranslateX(nodeWidth/2);

        System.out.println("Node " + this.title + " resized to height: " + this.nodeHeight);
        rerenderSockets();
    }


    public Node render() {
        final double[] dragOffset = new double[2];

        if (renderRef != null) {
            return renderRef;
        }

        Region spacer = new Region();
        spacer.setPrefHeight(headerHeight);
        spacer.setMinHeight(headerHeight);
        spacer.setStyle("-fx-border-color: red; -fx-border-width: 2;");
        controlsBox.getChildren().add(spacer);

        System.out.println("Rendering node: " + this.title + " with height: " + this.nodeHeight);

        bodyBox = new Rectangle(nodeWidth, nodeHeight);
        bodyBox.setArcWidth(12);
        bodyBox.setArcHeight(12);
        bodyBox.setFill(Color.web("#303030"));
        bodyBox.setStroke(Color.web("#3a3a3a"));
        bodyBox.setStrokeWidth(1);
        bodyBox.setId("highlight");

        headerBar = new Rectangle(nodeWidth, headerHeight);
        headerBar.setArcWidth(12);
        headerBar.setArcHeight(12);
        headerBar.setFill(this.colour);
        headerBar.setTranslateY(-nodeHeight / 2 + headerHeight / 2);

        titleText = new Text(this.title);
        titleText.setFill(Color.WHITE);
        titleText.setTranslateY(-nodeHeight / 2 + headerHeight / 2);

        container = new StackPane(bodyBox, headerBar, titleText, controlsBox);
        container.setPrefSize(nodeWidth, nodeHeight);
        container.setLayoutX(x);
        container.setLayoutY(y);
        this.renderRef = container;


        selected.addListener((obs, oldVal, isSelected) -> {
            bodyBox.setStroke(isSelected ? Color.WHITESMOKE : Color.web("#3a3a3a"));
        });

        /*controlsBox.heightProperty().addListener((obs, oldHeight, newHeight) -> {
            rerenderNodeSize();
        });*/
        

        container.setOnMouseClicked(e -> {
            if (wasDragged) {
                e.consume();
                return;
            }

            if (!e.isShiftDown()) {
                for (BaseNode node : YourNodeManager.getAllNodes()) {
                    node.setSelected(false);
                }
            }
            setSelected(true);
            e.consume();
        });

        container.setOnMousePressed((MouseEvent e) -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                wasDragged = false;
                startedOnSelected = isSelected();

                // If not shift-clicking and this wasn't already selected, select only this one
                if (!e.isShiftDown() && !isSelected()) {
                    for (BaseNode node : YourNodeManager.getAllNodes()) {
                        node.setSelected(false);
                    }
                    setSelected(true);
                }

                Point2D mouseInParent = container.getParent().sceneToLocal(e.getSceneX(), e.getSceneY());
                dragStartMouse[0] = mouseInParent.getX();
                dragStartMouse[1] = mouseInParent.getY();

                dragStartPositions.clear();
                for (BaseNode node : YourNodeManager.getSelectedNodes()) {
                    dragStartPositions.put(node, new Point2D(
                        node.getRenderRef().getLayoutX(),
                        node.getRenderRef().getLayoutY()
                    ));
                }

                e.consume();
            }
        });


        container.setOnMouseDragged((MouseEvent e) -> {
            if (e.isPrimaryButtonDown()) {
                wasDragged = true;

                Point2D mouseInParent = container.getParent().sceneToLocal(e.getSceneX(), e.getSceneY());
                double deltaX = mouseInParent.getX() - dragStartMouse[0];
                double deltaY = mouseInParent.getY() - dragStartMouse[1];

                for (BaseNode node : YourNodeManager.getSelectedNodes()) {
                    Point2D start = dragStartPositions.get(node);
                    if (start != null) {
                        Node ref = node.getRenderRef();
                        ref.setLayoutX(start.getX() + deltaX);
                        ref.setLayoutY(start.getY() + deltaY);
                        node.setPosition(ref.getLayoutX(), ref.getLayoutY());

                        for (ConnectionLine conn : node.getConnections()) {
                            conn.update();
                        }
                    }
                }

                e.consume();
            }
        });

        rerenderSockets();
        return renderRef;
    }
}

enum nodeType {
    MATH, VALUE, TEST, SPREADSHEET
}
