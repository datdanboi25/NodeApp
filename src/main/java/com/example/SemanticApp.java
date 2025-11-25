package com.example;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.stream.Collectors;

import com.example.nodes.BaseNode;

import com.example.ui.*;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
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
import javafx.scene.Node;

import org.reflections.Reflections;

public class SemanticApp extends Application {

    private final Scale scaleTransform = new Scale(1, 1);
    private final Translate translateTransform = new Translate();
    private final Rectangle selectionRect = new Rectangle();
    private final double[] startX = new double[1];
    private final double[] startY = new double[1];

    private Pane nodePane;
    private TextField searchField;

    @Override
    public void start(Stage primaryStage) {
        Canvas backgroundCanvas = new Canvas(5000, 5000);
        drawDotGrid(backgroundCanvas, 20, Color.web("#444"));

        nodePane = new Pane();
        nodePane.setPrefSize(5000, 5000);

        Pane canvas = new Pane(backgroundCanvas, nodePane);
        canvas.setId("canvas");
        canvas.setPrefSize(5000, 5000);

        Group scalableContent = new Group(canvas);
        scalableContent.getTransforms().addAll(translateTransform, scaleTransform);

        Pane zoomPane = new Pane(scalableContent);

        VBox nodeBar = new VBox(10);
        nodeBar.setPrefWidth(140);
        nodeBar.setId("node-bar");


        searchField = new TextField();
        searchField.setPromptText("Search nodes");
        searchField.getStyleClass().add("text-field");
        searchField.setFocusTraversable(false);

        VBox iconContainer = new VBox(5);
        iconContainer.setId("icon-container");

        List<Label> allIcons = new ArrayList<>();

        Reflections reflections = new Reflections("com.example.nodes");
        Set<Class<? extends BaseNode>> nodeClasses = reflections.getSubTypesOf(BaseNode.class);

        for (Class<? extends BaseNode> nodeClass : nodeClasses) {
            try {
                Constructor<? extends BaseNode> constructor = nodeClass.getConstructor(double.class, double.class);
                BaseNode tempNode = constructor.newInstance(0, 0);
                String nodeName = nodeClass.getMethod("getName").invoke(null).toString();

                Label nodeIcon = new Label(nodeName);
                nodeIcon.getStyleClass().add("node-icon");
                nodeIcon.prefWidthProperty().bind(nodeBar.widthProperty().subtract(20));
                nodeIcon.setAlignment(Pos.CENTER);
                nodeIcon.setStyle("-fx-border-color: " + nodeClass.getMethod("getColor").invoke(null).toString() +  ";");
                nodeIcon.setOnDragDetected(event -> {
                    Dragboard db = nodeIcon.startDragAndDrop(TransferMode.COPY);
                    ClipboardContent content = new ClipboardContent();
                    content.putString(nodeClass.getName());
                    db.setContent(content);
                    event.consume();
                });
                allIcons.add(nodeIcon);
            } catch (Exception e) {
                System.err.println("Skipping " + nodeClass.getSimpleName() + ": " + e.getMessage());
            }
        }

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

        MenuBar menuBar = new MenuBar();
        menuBar.getStyleClass().add("menu-bar");
        menuBar.setFocusTraversable(false);


        Menu fileMenu = new Menu("File");
        MenuItem exitItem = new MenuItem("Exit");

        
        exitItem.setOnAction(e -> Platform.exit());
        fileMenu.getItems().add(exitItem);
        
        Menu helpMenu = new Menu("Help");
        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction(e -> new Alert(Alert.AlertType.INFORMATION, "Made by Daniel.").show());
        helpMenu.getItems().add(aboutItem);
        
        fileMenu.setId("file-menu");
        helpMenu.setId("help-menu");
        exitItem.setId("exit-item");
        aboutItem.setId("about-item");

        menuBar.getMenus().addAll(fileMenu, helpMenu);

        VBox rootLayout = new VBox(menuBar, root);
        Scene scene = new Scene(rootLayout, 1000, 600);
        scene.getStylesheets().add(
            java.util.Objects.requireNonNull(
                getClass().getResource("/styles.css")
            ).toExternalForm()
        );

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
                try {
                    Class<?> clazz = Class.forName(db.getString());
                    if (BaseNode.class.isAssignableFrom(clazz)) {
                        Constructor<?> ctor = clazz.getConstructor(double.class, double.class);
                        BaseNode newNode = (BaseNode) ctor.newInstance(dropPoint.getX(), dropPoint.getY());
                        YourNodeManager.register(newNode);
                        nodePane.getChildren().add(newNode.render());
                        event.setDropCompleted(true);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    event.setDropCompleted(false);
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
        });

        canvas.setOnMousePressed(e -> {
            if (!(e.getTarget() instanceof TextField)) {
                canvas.requestFocus();  // steals focus from the node
            }
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

        zoomPane.setOnScroll(event -> {
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
                List<BaseNode> toRemove = YourNodeManager.getAllNodes().stream()
                        .filter(BaseNode::isSelected)
                        .toList();
                for (BaseNode node : toRemove) {
                    node.delete(nodePane);
                }
            }
        });

        selectionRect.getStyleClass().add("selection-rect");
        selectionRect.setVisible(false);
        selectionRect.setMouseTransparent(true);
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
