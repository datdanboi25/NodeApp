
package com.example;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.example.nodes.*;
import com.example.ui.YourNodeManager;

import javafx.application.Application;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;
import javafx.scene.Node;

public class SemanticApp2 extends Application {

    private final Scale scaleTransform = new Scale(1, 1);
    private final Translate translateTransform = new Translate();
    private final List<BaseNode> allNodes = new ArrayList<>();
    private Pane nodePane;
    private TextField searchField;
    private final Rectangle selectionRect = new Rectangle();
    private final double[] startX = new double[1];
    private final double[] startY = new double[1];


    @Override
    public void start(Stage primaryStage) {
        Canvas backgroundCanvas = new Canvas(5000, 5000);
        drawDotGrid(backgroundCanvas, 20, Color.web("#444"));

        nodePane = new Pane();
        nodePane.setPrefSize(5000, 5000);

        Pane canvas = new Pane(backgroundCanvas, nodePane);
        canvas.setId("canvas");
        canvas.setStyle("-fx-background-color: #1e1e1e;");
        canvas.setPrefSize(5000, 5000);

        Group scalableContent = new Group(canvas);
        scalableContent.getTransforms().addAll(translateTransform, scaleTransform);

        Pane zoomPane = new Pane(scalableContent);

        VBox nodeBar = new VBox(10);
        nodeBar.setStyle("-fx-background-color: #2c2c2c; -fx-padding: 10;");
        nodeBar.setPrefWidth(140);
        

        searchField = new TextField();
        searchField.setPromptText("Search nodes");
        searchField.setStyle("-fx-background-color: #3a3a3a; -fx-text-fill: white; -fx-border-color: #555; -fx-border-radius: 3; -fx-background-radius: 3;");
        searchField.setFocusTraversable(false);

        VBox iconContainer = new VBox(5);

        Label mathNodeIcon = new Label("MathNode");
        
        mathNodeIcon.setStyle("-fx-background-color: " + MathNode.getColor() + "; -fx-text-fill: white; -fx-padding: 5; -fx-alignment: center;");
        mathNodeIcon.setOnDragDetected(event -> {
            Dragboard db = mathNodeIcon.startDragAndDrop(TransferMode.COPY);
            ClipboardContent content = new ClipboardContent();
            content.putString("MathNode");
            db.setContent(content);
            event.consume();
        });

        Label valueNodeIcon = new Label("ValueNode");

        valueNodeIcon.setStyle("-fx-background-color: " + ValueNode.getColor() + "; -fx-text-fill: white; -fx-padding: 5; -fx-alignment: center;");
        valueNodeIcon.setOnDragDetected(event -> {
            Dragboard db = valueNodeIcon.startDragAndDrop(TransferMode.COPY);
            ClipboardContent content = new ClipboardContent();
            content.putString("ValueNode");
            db.setContent(content);
            event.consume();
        });

        Label testNodeIcon = new Label("TestNode");
        
        testNodeIcon.setStyle("-fx-background-color: " + TestNode.getColor() + "; -fx-text-fill: white; -fx-padding: 5; -fx-alignment: center;");
        testNodeIcon.setOnDragDetected(event -> {
            Dragboard db = testNodeIcon.startDragAndDrop(TransferMode.COPY);
            ClipboardContent content = new ClipboardContent();
            content.putString("TestNode");
            db.setContent(content);
            event.consume();
        });

        List<Label> allIcons = List.of(mathNodeIcon, valueNodeIcon, testNodeIcon);
        iconContainer.getChildren().addAll(allIcons);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            iconContainer.getChildren().setAll(
                allIcons.stream()
                        .filter(label -> label.getText().toLowerCase().contains(newVal.toLowerCase()))
                        .collect(Collectors.toList())
            );
        });

        nodeBar.getChildren().addAll(searchField, iconContainer);

        BorderPane root = new BorderPane();
        root.setCenter(zoomPane);
        root.setRight(nodeBar);

        Scene scene = new Scene(root, 1000, 600);

        nodePane.setOnDragOver(event -> {
            if (event.getGestureSource() != nodePane && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        nodePane.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasString()) {
                Point2D dropPoint = nodePane.sceneToLocal(event.getSceneX(), event.getSceneY());
                BaseNode newNode = null;

                switch (db.getString()) {
                    case "MathNode":
                        newNode = new MathNode(dropPoint.getX(), dropPoint.getY());
                        System.out.println("Math");
                        break;
                    case "ValueNode":
                        System.out.println("Value");
                        newNode = new ValueNode(dropPoint.getX(), dropPoint.getY());
                        break;

                    case "TestNode":
                        System.out.println("Test");
                        newNode = new TestNode(dropPoint.getX(), dropPoint.getY());
                        break;

                    default:
                        newNode = new MathNode(dropPoint.getX(), dropPoint.getY());
                        System.out.println("default");
                }

                if (newNode != null) {
                    YourNodeManager.register(newNode);
                    nodePane.getChildren().add(newNode.render());
                    //allNodes.add(newNode);
                    event.setDropCompleted(true);
                }
            } else {
                event.setDropCompleted(false);
            }
            event.consume();
        });

        final double[] lastMouse = new double[2];
        zoomPane.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.MIDDLE) {
                
                lastMouse[0] = e.getSceneX();
                lastMouse[1] = e.getSceneY();
                e.consume();
            }
            if (!e.isShiftDown()) {
                for (BaseNode node : allNodes) {
                    node.setSelected(false);
                }
                zoomPane.requestFocus(); // remove focus from search bar
            }
        });

        canvas.setOnMousePressed(e -> {
            if (e.isPrimaryButtonDown()) {
                startX[0] = e.getX();
                startY[0] = e.getY();

                selectionRect.setX(startX[0]);
                selectionRect.setY(startY[0]);
                selectionRect.setWidth(0);
                selectionRect.setHeight(0);
                selectionRect.setVisible(true);
            }
        });

        canvas.setOnMouseDragged(e -> {
            if (selectionRect.isVisible()) {
                double x = Math.min(startX[0], e.getX());
                double y = Math.min(startY[0], e.getY());
                double width = Math.abs(e.getX() - startX[0]);
                double height = Math.abs(e.getY() - startY[0]);

                selectionRect.setX(x);
                selectionRect.setY(y);
                selectionRect.setWidth(width);
                selectionRect.setHeight(height);
            }
        });

        canvas.setOnMouseReleased(e -> {
            if (selectionRect.isVisible()) {
                Bounds selectionBounds = selectionRect.getBoundsInParent();

                for (BaseNode node : YourNodeManager.getAllNodes()) {
                    Node render = node.getRenderRef();
                    if (render.getBoundsInParent().intersects(selectionBounds)) {
                        node.setSelected(true);
                    } else {
                        node.setSelected(false);
                    }
                }

                selectionRect.setVisible(false);
            }
        });

        zoomPane.setOnMouseDragged(e -> {
            if (e.isMiddleButtonDown()) {
                double dx = e.getSceneX() - lastMouse[0];
                double dy = e.getSceneY() - lastMouse[1];
                translateTransform.setX(translateTransform.getX() + dx);
                translateTransform.setY(translateTransform.getY() + dy);
                lastMouse[0] = e.getSceneX();
                lastMouse[1] = e.getSceneY();
                e.consume();
            }
        });

        zoomPane.setOnScroll((ScrollEvent event) -> {
            double oldScale = scaleTransform.getX();
            double zoomFactor = Math.exp(event.getDeltaY() * 0.0015);
            double newScale = clamp(oldScale * zoomFactor, 0.2, 4);

            Point2D mouseScene = new Point2D(event.getSceneX(), event.getSceneY());
            Point2D mouseInContentBefore = scalableContent.sceneToLocal(mouseScene);

            scaleTransform.setX(newScale);
            scaleTransform.setY(newScale);

            Point2D mouseInSceneAfter = scalableContent.localToScene(mouseInContentBefore);
            double dx = mouseInSceneAfter.getX() - event.getSceneX();
            double dy = mouseInSceneAfter.getY() - event.getSceneY();
            translateTransform.setX(translateTransform.getX() - dx);
            translateTransform.setY(translateTransform.getY() - dy);

            event.consume();
        });

        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DELETE || e.getCode() == KeyCode.BACK_SPACE) {
                System.out.println("Delete key pressed");
                System.out.println(YourNodeManager.getAllNodes().size() + " nodes before removal");
                List<BaseNode> toRemove = new ArrayList<>(YourNodeManager.getAllNodes())//allNodes.stream()
                        .stream()
                        .filter(BaseNode::isSelected)
                        .toList();

                for (BaseNode node : toRemove) {
                    System.out.println("Removing node: " + node.getTitle() + " " + node.getId());
                    node.delete(nodePane);
                }

                //allNodes.removeAll(toRemove);
            }
        });

        selectionRect.setFill(Color.web("#d6d6d622"));
        selectionRect.setStroke(Color.web("#d6d6d6"));
        selectionRect.setVisible(false);
        selectionRect.setMouseTransparent(true);
        selectionRect.getStrokeDashArray().addAll(4.0, 4.0);

        canvas.getChildren().add(selectionRect);



        primaryStage.setScene(scene);
        primaryStage.setTitle("Semantic Node GUI");
        primaryStage.show();
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private void drawDotGrid(Canvas canvas, double spacing, Color color) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(color);
        for (double y = 0; y < canvas.getHeight(); y += spacing) {
            for (double x = 0; x < canvas.getWidth(); x += spacing) {
                gc.fillOval(x, y, 1.5, 1.5);
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
} 
/*package com.example;

import com.example.nodes.*;
import com.example.ui.YourNodeManager;
import javafx.application.Application;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;
import org.reflections.Reflections;
import com.example.ui.NodeRegistry;
import javafx.scene.Node;

import java.util.*;
import java.util.stream.Collectors;

public class SemanticApp extends Application {

    private final Scale scaleTransform = new Scale(1, 1);
    private final Translate translateTransform = new Translate();
    private final List<BaseNode> allNodes = new ArrayList<>();
    private Pane nodePane;
    private Canvas backgroundCanvas;
    private final Rectangle selectionRect = new Rectangle();
    private final double[] startX = new double[1];
    private final double[] startY = new double[1];
    private Point2D contextClickLocation = new Point2D(0, 0);
    private final ContextMenu nodeContextMenu = new ContextMenu();
    private boolean draggingSelect = false;

    @Override
    public void start(Stage primaryStage) {
        preloadNodes();

        backgroundCanvas = new Canvas(5000, 5000);
        drawDotGrid(backgroundCanvas, 20, Color.web("#444"));

        nodePane = new Pane();
        nodePane.setPrefSize(5000, 5000);

        Pane canvas = new Pane(backgroundCanvas, nodePane);
        canvas.setStyle("-fx-background-color: #1e1e1e;");
        canvas.setPrefSize(5000, 5000);

        Group scalableContent = new Group(canvas);
        scalableContent.getTransforms().addAll(translateTransform, scaleTransform);

        Pane zoomPane = new Pane(scalableContent);

        BorderPane root = new BorderPane();
        root.setCenter(zoomPane);

        Scene scene = new Scene(root, 1000, 600);

        updateNodeMenu();

        canvas.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                Node target = e.getPickResult().getIntersectedNode();
                if (target instanceof javafx.scene.shape.CubicCurve || target instanceof javafx.scene.shape.Circle) {
                    System.out.println("Suppressed context menu from connection line");
                    return;
                }

                contextClickLocation = nodePane.sceneToLocal(e.getSceneX(), e.getSceneY());
                nodeContextMenu.show(canvas, e.getScreenX(), e.getScreenY());
            } else if (e.getButton() == MouseButton.PRIMARY) {
                Node clicked = e.getPickResult().getIntersectedNode();
                if (clicked == backgroundCanvas || backgroundCanvas.equals(clicked.getParent())) {
                    draggingSelect = true;
                    nodeContextMenu.hide();
                    startX[0] = e.getX();
                    startY[0] = e.getY();

                    selectionRect.setX(startX[0]);
                    selectionRect.setY(startY[0]);
                    selectionRect.setWidth(0);
                    selectionRect.setHeight(0);
                    selectionRect.setVisible(true);
                }
            }
        });

        canvas.setOnMouseDragged(e -> {
            if (!draggingSelect) return;

            double x = Math.min(startX[0], e.getX());
            double y = Math.min(startY[0], e.getY());
            double width = Math.abs(e.getX() - startX[0]);
            double height = Math.abs(e.getY() - startY[0]);

            selectionRect.setX(x);
            selectionRect.setY(y);
            selectionRect.setWidth(width);
            selectionRect.setHeight(height);
        });

        canvas.setOnMouseReleased(e -> {
            if (draggingSelect) {
                draggingSelect = false;
                Bounds selectionBounds = selectionRect.getBoundsInParent();

                for (BaseNode node : YourNodeManager.getAllNodes()) {
                    Node render = node.getRenderRef();
                    if (render.getBoundsInParent().intersects(selectionBounds)) {
                        node.setSelected(true);
                    } else {
                        node.setSelected(false);
                    }
                }

                selectionRect.setVisible(false);
            }
        });

        nodePane.setOnDragOver(event -> {
            if (event.getGestureSource() != nodePane && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        nodePane.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasString()) {
                Point2D dropPoint = nodePane.sceneToLocal(event.getSceneX(), event.getSceneY());
                spawnNodeAt(db.getString(), dropPoint);
                event.setDropCompleted(true);
            } else {
                event.setDropCompleted(false);
            }
            event.consume();
        });

        final double[] lastMouse = new double[2];
        zoomPane.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.MIDDLE) {
                lastMouse[0] = e.getSceneX();
                lastMouse[1] = e.getSceneY();
                e.consume();
            }
        });

        zoomPane.setOnMouseDragged(e -> {
            if (e.isMiddleButtonDown()) {
                double dx = e.getSceneX() - lastMouse[0];
                double dy = e.getSceneY() - lastMouse[1];
                translateTransform.setX(translateTransform.getX() + dx);
                translateTransform.setY(translateTransform.getY() + dy);
                lastMouse[0] = e.getSceneX();
                lastMouse[1] = e.getSceneY();
                e.consume();
            }
        });

        zoomPane.setOnScroll((ScrollEvent event) -> {
            double oldScale = scaleTransform.getX();
            double zoomFactor = Math.exp(event.getDeltaY() * 0.0015);
            double newScale = clamp(oldScale * zoomFactor, 0.2, 4);

            Point2D mouseScene = new Point2D(event.getSceneX(), event.getSceneY());
            Point2D mouseInContentBefore = scalableContent.sceneToLocal(mouseScene);

            scaleTransform.setX(newScale);
            scaleTransform.setY(newScale);

            Point2D mouseInSceneAfter = scalableContent.localToScene(mouseInContentBefore);
            double dx = mouseInSceneAfter.getX() - event.getSceneX();
            double dy = mouseInSceneAfter.getY() - event.getSceneY();
            translateTransform.setX(translateTransform.getX() - dx);
            translateTransform.setY(translateTransform.getY() - dy);

            event.consume();
        });

        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DELETE || e.getCode() == KeyCode.BACK_SPACE) {
                List<BaseNode> toRemove = new ArrayList<>(YourNodeManager.getAllNodes())
                        .stream()
                        .filter(BaseNode::isSelected)
                        .toList();

                for (BaseNode node : toRemove) {
                    node.delete(nodePane);
                }
            }

            if (e.getCode() == KeyCode.A && e.isShiftDown()) {
                Point2D center = new Point2D(
                        nodePane.getWidth() / 2,
                        nodePane.getHeight() / 2
                );
                contextClickLocation = center;
                nodeContextMenu.show(canvas, 300, 200);
            }
        });

        selectionRect.setFill(Color.web("#d6d6d622"));
        selectionRect.setStroke(Color.web("#d6d6d6"));
        selectionRect.setVisible(false);
        selectionRect.setMouseTransparent(true);
        selectionRect.getStrokeDashArray().addAll(4.0, 4.0);
        canvas.getChildren().add(selectionRect);

        primaryStage.setScene(scene);
        primaryStage.setTitle("Semantic Node GUI");
        primaryStage.show();
    }

    private void preloadNodes() {
        Reflections reflections = new Reflections("com.example.nodes");
        Set<Class<? extends BaseNode>> nodeClasses = reflections.getSubTypesOf(BaseNode.class);

        for (Class<? extends BaseNode> clazz : nodeClasses) {
            try {
                NodeRegistry.register(clazz);
                System.out.println("Registered dynamically: " + clazz.getSimpleName());
            } catch (Exception e) {
                System.err.println("Failed to register: " + clazz.getName());
                e.printStackTrace();
            }
        }
    }

    private void updateNodeMenu() {
        nodeContextMenu.getItems().clear();

        Set<String> nodeNames = NodeRegistry.getRegisteredNames();
        if (nodeNames.isEmpty()) {
            MenuItem empty = new MenuItem("No nodes found");
            empty.setDisable(true);
            nodeContextMenu.getItems().add(empty);
        } else {
            nodeNames.stream().sorted().forEach(key -> {
                BaseNode tempNode = NodeRegistry.create(key, 0, 0);
                String label = (tempNode != null && tempNode.getTitle() != null) ? tempNode.getTitle() : key;
                MenuItem item = new MenuItem("Add " + label);
                item.setOnAction(e -> {
                    spawnNodeAt(key, contextClickLocation);
                    nodeContextMenu.hide();
                });
                nodeContextMenu.getItems().add(item);
            });
        }
    }

    private void spawnNodeAt(String type, Point2D position) {
        BaseNode newNode = NodeRegistry.create(type, position.getX(), position.getY());
        if (newNode != null) {
            YourNodeManager.register(newNode);
            nodePane.getChildren().add(newNode.render());
        }
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private void drawDotGrid(Canvas canvas, double spacing, Color color) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(color);
        for (double y = 0; y < canvas.getHeight(); y += spacing) {
            for (double x = 0; x < canvas.getWidth(); x += spacing) {
                gc.fillOval(x, y, 1.5, 1.5);
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
*/






