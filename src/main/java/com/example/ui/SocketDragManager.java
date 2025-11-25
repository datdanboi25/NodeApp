package com.example.ui;

import com.example.connections.ConnectionLine;
import com.example.nodes.BaseNode;
import com.example.nodes.Socket;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

import java.util.List;

public class SocketDragManager {
    private static Line tempLine;
    private static Socket startSocket;
    private static BaseNode highlightedNode;

    public static void startDrag(Socket socket, Circle circle, MouseEvent event) {
        startSocket = socket;
        System.out.println("DS");

        Pane canvas = (Pane) circle.getScene().lookup("#canvas");
        if (canvas == null) return;

        tempLine = new Line();

        Point2D startPoint = canvas.sceneToLocal(
            circle.localToScene(circle.getBoundsInLocal()).getCenterX(),
            circle.localToScene(circle.getBoundsInLocal()).getCenterY()
        );

        tempLine.setStartX(startPoint.getX());
        tempLine.setStartY(startPoint.getY());
        tempLine.setEndX(startPoint.getX());
        tempLine.setEndY(startPoint.getY());
        tempLine.setStrokeWidth(2);
        tempLine.setStroke(Color.YELLOW);
        tempLine.getStrokeDashArray().addAll(4.0, 4.0);

        canvas.getChildren().add(tempLine);
    }

    public static void updateDrag(Circle origin, MouseEvent e) {
        if (tempLine != null) {
            Pane canvas = (Pane) origin.getScene().lookup("#canvas");
            if (canvas == null) return;

            Point2D endPoint = canvas.sceneToLocal(e.getSceneX(), e.getSceneY());
            tempLine.setEndX(endPoint.getX());
            tempLine.setEndY(endPoint.getY());

            Socket hoverSocket = findSocketUnderMouse(canvas, e);
            BaseNode newHighlight = (hoverSocket != null && isValidConnection(startSocket, hoverSocket))
                ? hoverSocket.getParentNode() : null;

            if (highlightedNode != newHighlight) {
                if (highlightedNode != null) {
                    Node n = highlightedNode.getRenderRef();
                    if (n != null) {
                        Node found = n.lookup("#highlight");
                        if (found instanceof Rectangle) {
                            ((Rectangle) found).setStroke(Color.web("#3a3a3a"));
                        }
                    }
                }
                if (newHighlight != null) {
                    Node n = newHighlight.getRenderRef();
                    if (n != null) {
                        Node found = n.lookup("#highlight");
                        if (found instanceof Rectangle) {
                            ((Rectangle) found).setStroke(Color.WHITE);
                        }
                    }
                }
                highlightedNode = newHighlight;
            }
        
        }
    }

    public static void endDrag(Circle origin, MouseEvent e) {
        Pane canvas = (Pane) origin.getScene().lookup("#canvas");
        System.out.print("ENDDRAG");
        if (canvas == null) return;

        Socket endSocket = findSocketUnderMouse(canvas, e);
        if (endSocket != null && isValidConnection(startSocket, endSocket)) {
            ConnectionLine connection = new ConnectionLine(startSocket, endSocket, canvas);

            System.out.println("Connection created: " + startSocket.getName() + " -> " + endSocket.getName());
            
        }

        if (highlightedNode != null) {
            Node n = highlightedNode.getRenderRef();
            if (n != null) {
                Node found = n.lookup("#highlight");
                if (found instanceof Rectangle) {
                    ((Rectangle) found).setStroke(Color.web("#3a3a3a"));
                }
            }
        }
        highlightedNode = null;

        canvas.getChildren().remove(tempLine);

        reset(canvas);
    }

    private static boolean isValidConnection(Socket a, Socket b) {
        return a != b &&
               a.isOutput() &&
               b.isInput() &&
               a.isCompatibleWith(b);
    }

    private static Socket findSocketUnderMouse(Pane canvas, MouseEvent e) {
        List<BaseNode> allNodes = YourNodeManager.getAllNodes();
        Point2D mouseScreen = new Point2D(e.getScreenX(), e.getScreenY());

        //System.out.println("Mouse position in scene: " + mouseScene);


        for (BaseNode node : allNodes) {
            for (Socket s : node.getInputs()) {
                if (isSocketUnderMouse(s, mouseScreen)) return s;
            }
            for (Socket s : node.getOutputs()) {
                if (isSocketUnderMouse(s, mouseScreen)) return s;
            }
        }
        return null;
    }


    private static boolean isSocketUnderMouse(Socket s, Point2D mouseScene) {
        Circle circle = s.getRenderRef();
        if (circle != null) {
            Bounds bounds = circle.localToScreen(circle.getBoundsInLocal());
            //System.out.println("Checking socket: " + s.getName() + bounds.contains(mouseScene));
            return bounds.contains(mouseScene);


        }
        return false;
    }
    public static void cancelActiveDrag(Pane canvas) {
        if (tempLine != null && canvas.getChildren().contains(tempLine)) {
            canvas.getChildren().remove(tempLine);
        }
        tempLine = null;
        startSocket = null;

        if (highlightedNode != null) {
            Node n = highlightedNode.getRenderRef();
            if (n != null) {
                Node found = n.lookup("#highlight");
                if (found instanceof Rectangle) {
                    ((Rectangle) found).setStroke(Color.web("#3a3a3a"));
                }
            }
        }

        highlightedNode = null;
    }





    private static void reset(Pane canvas) {
        tempLine = null;
        startSocket = null;
        highlightedNode = null;
    }
}