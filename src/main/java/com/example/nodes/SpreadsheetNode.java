package com.example.nodes;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import java.io.File;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.example.misc.FastFrame;
import com.example.misc.FastFrame.Column;
import com.example.misc.FastFrame.ColType;

import javafx.stage.FileChooser;
import javafx.stage.Window;

public class SpreadsheetNode extends BaseNode {
    private StackPane renderRef;

    // Data & state
    private Object value;                    // Node output: selected column(s) data
    public static String name = "Spreadsheet";
    private static final String color = "#207245";
    private nodeType type;

    private FastFrame dataFrame;             // Loaded data table
    public String file;                      // path (optional for your UI)
    public List<String> loadedColumns = new ArrayList<>();

    // UI bits
    private ComboBox<String> columnCombo;
    private Label statusLabel;
    

    public SpreadsheetNode(double x, double y) {
        super("Spreadsheet", Color.web(color), x, y);
        this.type = nodeType.SPREADSHEET;
        this.value = List.of(); // empty until loaded
        
        reconfigureSockets();
    }

    public static String getColor() { return color; }
    public nodeType getType() { return this.type; }
    public Object getValue() { return this.value; }
    public static String getName() { return name; }

    @Override
    protected void customReconfigure() {
        // One vector-like output; your engine can interpret the shape at runtime.
        //outputs.add(new Socket("List", List.of(SocketType.VECTOR), SocketDirection.OUTPUT, this));
        for (String col : loadedColumns) {
            outputs.add(new Socket(col, List.of(SocketType.VECTOR), SocketDirection.OUTPUT, this));
        }
    }

    protected void defineSockets() {
        reconfigureSockets();
    }

    /** Generic reader for a cell, returning boxed Java types for downstream nodes. */
    private static Object getCell(FastFrame df, int row, String colName) {
        Column c = df.col(colName);
        switch (c.type()) {
            case INT    -> { var ic = (FastFrame.IntColumn) c; return c.isNull(row)?null:(double)ic.get(row); }
            case LONG   -> { var lc = (FastFrame.LongColumn) c; return c.isNull(row)?null:(double)lc.get(row); }
            case DOUBLE -> { var dc = (FastFrame.DoubleColumn) c; return c.isNull(row)?null:dc.get(row); }
            case BOOL   -> { var bc = (FastFrame.BoolColumn) c; return c.isNull(row)?null:bc.get(row); }
            case DATETIME -> { var dt = (FastFrame.DateTimeColumn) c; return c.isNull(row)?null:Instant.ofEpochMilli(dt.get(row)).toString(); }
            case STRING -> { var sc = (FastFrame.StringColumn) c; return sc.get(row); }
            default -> {return null;}
        }
    }

    

    private void updateValueFromSelection() {
        
        if (dataFrame==null||loadedColumns.isEmpty()){ this.value=List.of(); return; }
        if (loadedColumns.size()==1) {
            this.value=buildColumnList(loadedColumns.get(0));
        } else {
            List<Object[]> rows=new ArrayList<>(dataFrame.rows());
            for (int r=0;r<dataFrame.rows();r++) {
                Object[] out=new Object[loadedColumns.size()];
                for (int i=0;i<loadedColumns.size();i++) out[i]=getCell(dataFrame,r,loadedColumns.get(i));
                rows.add(out);
            }
            this.value=rows;
            evaluate();
        }
    }

    /** File picker + load logic. Accepts CSV or XLSX via FastFrame. */
    private void openDialogAndLoad() {
        Window owner = (renderRef != null && renderRef.getScene() != null) ? renderRef.getScene().getWindow() : null;

        FileChooser fc = new FileChooser();
        fc.setTitle("Open Data File");
        fc.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Data files", "*.csv", "*.xlsx"),
            new FileChooser.ExtensionFilter("CSV files", "*.csv"),
            new FileChooser.ExtensionFilter("Excel files", "*.xlsx")
        );

        File f = fc.showOpenDialog(owner);
        if (f == null) return;

        this.file = f.getAbsolutePath();

        try {
            var opts = FastFrame.ReadOptions.builder()
                    .header(true)
                    .inferTypes(true)
                    .inferSampleRows(500)
                    .build();

            this.dataFrame = FastFrame.readAuto(Path.of(file), opts);

            loadedColumns.clear();
            refreshOutputSockets();
            renderControls(); // so the combo shows the new headers


            // Update status
            if (statusLabel != null) {
                statusLabel.setText("Loaded: " + f.getName() + " (" + dataFrame.rows() + " rows, " + dataFrame.cols() + " cols)");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            if (statusLabel != null) statusLabel.setText("Failed to load: " + ex.getMessage());
            this.dataFrame = null;
            loadedColumns.clear();
            this.value = List.of();
        }
        Platform.runLater(this::rerenderNodeSize);
    }

    private void addSelectedColumn() {
        if (dataFrame == null || columnCombo == null) return;
        String col = columnCombo.getValue();
        if (col == null || loadedColumns.contains(col)) return;
        loadedColumns.add(col);
        refreshOutputSockets();
        if (statusLabel != null) statusLabel.setText("Selected columns: " + loadedColumns);
        evaluate(); // auto-trigger downstream updates
    }

    private void clearSelection() {
        loadedColumns.clear();
        //refreshOutputSockets();
        
        if (statusLabel != null) statusLabel.setText("Selected columns: []");
    }


    public void evaluate() {
        // Maintain 'value' live from selection.
        updateValueFromSelection();
        propagate();
    }

    @Override
    public Node render() {
        renderRef = (StackPane) super.render(); // initializes container + controlsBox
        renderControls();
        return renderRef;
    }

    /** Build/refresh the node’s controls (file button + column picker once data is loaded). */
    private void renderControls() {
        
        controlsBox.getChildren().clear();
        rerenderSockets();
        Region spacer = new Region();
        spacer.setPrefHeight(headerHeight);
        spacer.setMinHeight(headerHeight);
        controlsBox.getChildren().add(spacer);
        //spacer.setStyle("-fx-border-color: red; -fx-border-width: 2;");


        // --- Status
        statusLabel = new Label(
            (dataFrame == null)
            ? "No file loaded"
            : "Loaded: " + new File(Objects.toString(file, "")).getName() + " (" + dataFrame.rows() + " rows)"
        );
        statusLabel.setStyle("-fx-text-fill: white;");
        controlsBox.getChildren().add(statusLabel);
        
        // --- Column picker (only after a file is loaded)
        if (dataFrame != null) {
            VBox pickBox = new VBox(6   );
            
            columnCombo = new ComboBox<>();
            // FastFrame: colNames() returns a Set; make it a list for the combo
            columnCombo.getItems().setAll(new ArrayList<>(dataFrame.colNames()));
            columnCombo.setPromptText("Choose a column");
            columnCombo.setMaxWidth(nodeWidth - 20);
            columnCombo.getStyleClass().add("node-combo");
            
            Button addColBtn = new Button("Add Column");
            addColBtn.setMaxWidth(nodeWidth - 20);
            addColBtn.setOnAction(e -> addSelectedColumn());
            addColBtn.getStyleClass().add("node-button");
            
            Button clearBtn = new Button("Clear Selection");
            clearBtn.setMaxWidth(nodeWidth - 20);
            clearBtn.getStyleClass().add("node-button");
            clearBtn.setOnAction(e -> {
                loadedColumns.clear();
                updateValueFromSelection();
                if (statusLabel != null) statusLabel.setText("Selected columns: []");
                columnCombo.setValue(null);
            });
            
            pickBox.getChildren().addAll(columnCombo, addColBtn, clearBtn);
            pickBox.setAlignment(Pos.CENTER);
            controlsBox.getChildren().add(pickBox);
        }

        // --- File button
        Button fileSelectButton = new Button("Select File");
        fileSelectButton.getStyleClass().add("node-button");
        fileSelectButton.setMaxWidth(nodeWidth - 20);
        VBox.setMargin(fileSelectButton, new Insets(0, 0, 6, 0));
        //fileSelectButton.setTranslateY(nodeHeight / 2 - 20); // bottom-aligned
        //fileSelectButton.setTranslateX(nodeWidth / 2 - 60);
        //fileSelectButton.setMaxWidth(120);
        fileSelectButton.setOnAction(e -> {
            openDialogAndLoad();
            fileSelectButton.setText("File Selected");
        });
        controlsBox.getChildren().add(fileSelectButton);
        rerenderNodeSize();
    }

    private void refreshOutputSockets() {
        // keep non-data outputs if you have them; here we clear all and rebuild
        outputs.removeIf(s -> s.getDirection() == SocketDirection.OUTPUT);
        for (String col : loadedColumns) {
            outputs.add(new Socket(
                col,                       // socket name = column name
                List.of(SocketType.VECTOR),
                SocketDirection.OUTPUT,
                this
            ));
        }
    }

    public Object getValueForSocket(Socket s) {
        if (dataFrame == null) return List.of();
        String col = s.getName();
        if (!loadedColumns.contains(col)) return List.of();
        return buildColumnList(col);   // compute now; no extra state kept
    }


    private List<Object> buildColumnList(String colName) {
        List<Object> list = new ArrayList<>(dataFrame.rows());
        FastFrame.Column c = dataFrame.col(colName);
        for (int r = 0; r < dataFrame.rows(); r++) {
            if (c.isNull(r)) { list.add(null); continue; }
            switch (c.type()) {
                case INT      -> list.add((double) ((FastFrame.IntColumn) c).get(r));   // <-- changed
                case LONG     -> list.add((double) ((FastFrame.LongColumn) c).get(r));  // (optional) keep as Double too
                case DOUBLE   -> list.add(((FastFrame.DoubleColumn) c).get(r));
                case BOOL     -> list.add(((FastFrame.BoolColumn) c).get(r));
                case DATETIME -> {
                    long ms = ((FastFrame.DateTimeColumn) c).get(r);
                    list.add(java.time.Instant.ofEpochMilli(ms).toString());
                }
                case STRING   -> list.add(((FastFrame.StringColumn) c).get(r));
            }
        }
        return list;
    }
}
