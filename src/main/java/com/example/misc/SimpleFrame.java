package com.example.misc;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;
import java.nio.file.*;

import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;

/**
 * SimpleFrame — ultra-straightforward, readable DataFrame-like table.
 * - Columns: List<String>
 * - Data:    List<Object[]> (each row same length as columns)
 * - Nulls:   use null directly
 * - Types:   you decide; cast when reading
 *
 * This is meant to be EASY TO MODIFY, not fast.
 */
public class SimpleFrame {

    /* ===== Schema ===== */

    private final List<String> columns = new ArrayList<>();
    private final Map<String, Integer> nameToIndex = new LinkedHashMap<>();
    private final List<Object[]> rows = new ArrayList<>();

    public SimpleFrame() {}

    public SimpleFrame(List<String> columnNames) {
        setColumns(columnNames);
    }

    public static SimpleFrame of(String... columnNames) {
        return new SimpleFrame(Arrays.asList(columnNames));
    }

    public void setColumns(List<String> columnNames) {
        columns.clear();
        nameToIndex.clear();
        for (int i = 0; i < columnNames.size(); i++) {
            String n = Objects.requireNonNull(columnNames.get(i), "Column name cannot be null");
            if (nameToIndex.containsKey(n)) throw new IllegalArgumentException("Duplicate column: " + n);
            columns.add(n);
            nameToIndex.put(n, i);
        }
        // If schema shrinks or grows, you decide how to handle existing rows. Here we clear for simplicity.
        rows.clear();
    }

    public List<String> columns() { return Collections.unmodifiableList(columns); }
    public int colIndex(String name) { Integer i = nameToIndex.get(name); if (i == null) throw new IllegalArgumentException("No such column: " + name); return i; }
    public int rowCount() { return rows.size(); }
    public int colCount() { return columns.size(); }

    /* ===== Row add / set / get ===== */

    public void addRow(Object... values) {
        if (values.length != colCount())
            throw new IllegalArgumentException("Expected " + colCount() + " values but got " + values.length);
        rows.add(Arrays.copyOf(values, values.length));
    }

    public void addRows(List<Object[]> manyRows) {
        for (Object[] r : manyRows) addRow(r);
    }

    public Object get(int row, String col) {
        return rows.get(row)[colIndex(col)];
    }

    @SuppressWarnings("unchecked")
    public <T> T getAs(int row, String col, Class<T> type) {
        Object v = get(row, col);
        return (T) v; // caller ensures type is correct
    }

    public void set(int row, String col, Object value) {
        rows.get(row)[colIndex(col)] = value;
    }

    /* ===== Column add / drop / select ===== */

    public void addColumn(String name, List<?> values) {
        if (nameToIndex.containsKey(name)) throw new IllegalArgumentException("Column exists: " + name);
        int newIdx = colCount();
        columns.add(name);
        nameToIndex.put(name, newIdx);

        if (rows.isEmpty()) {
            for (Object v : values) rows.add(new Object[] { v });
        } else {
            if (values.size() != rowCount())
                throw new IllegalArgumentException("New column length " + values.size() + " != row count " + rowCount());
            for (int r = 0; r < rowCount(); r++) {
                Object[] old = rows.get(r);
                Object[] neu = Arrays.copyOf(old, old.length + 1);
                neu[old.length] = values.get(r);
                rows.set(r, neu);
            }
        }
    }

    public void dropColumns(String... names) {
        Set<String> toDrop = new HashSet<>(Arrays.asList(names));
        List<String> kept = columns.stream().filter(c -> !toDrop.contains(c)).toList();
        reproject(kept);
    }

    public SimpleFrame select(String... names) {
        SimpleFrame out = new SimpleFrame(Arrays.asList(names));
        for (Object[] row : rows) {
            Object[] vals = new Object[names.length];
            for (int i = 0; i < names.length; i++) vals[i] = row[colIndex(names[i])];
            out.rows.add(vals);
        }
        return out;
    }

    private void reproject(List<String> names) {
        List<Object[]> newRows = new ArrayList<>(rowCount());
        for (Object[] row : rows) {
            Object[] v = new Object[names.size()];
            for (int i = 0; i < names.size(); i++) v[i] = row[colIndex(names.get(i))];
            newRows.add(v);
        }
        setColumns(names);
        rows.addAll(newRows);
    }

    public static SimpleFrame openFileDialogAndLoad(Window owner) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Data File");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Data files", "*.csv", "*.xlsx"),
            new FileChooser.ExtensionFilter("CSV files", "*.csv"),
            new FileChooser.ExtensionFilter("Excel files", "*.xlsx")
        );

        File file = fileChooser.showOpenDialog(owner);
        if (file == null) return null; // user cancelled

        String name = file.getName().toLowerCase();
        try {
            if (name.endsWith(".csv")) {
                return SimpleFrame.readCSV(file.toPath(), true);
            } else if (name.endsWith(".xlsx")) {
                return readExcel(file.toPath());
            } else {
                throw new IllegalArgumentException("Unsupported file: " + file);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /** Very basic Excel loader: first row = header, rest = data. */
    public static SimpleFrame readExcel(Path path) throws IOException {
        try (InputStream is = new FileInputStream(path.toFile());
            Workbook wb = new XSSFWorkbook(is)) {
            Sheet sheet = wb.getSheetAt(0);

            // header
            Row headerRow = sheet.getRow(0);
            int cols = headerRow.getLastCellNum();
            List<String> headers = new ArrayList<>();
            for (int c = 0; c < cols; c++) {
                Cell cell = headerRow.getCell(c);
                headers.add(cell == null ? "c" + c : cell.toString());
            }

            SimpleFrame df = new SimpleFrame(headers);

            // data
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                Object[] vals = new Object[cols];
                for (int c = 0; c < cols; c++) {
                    Cell cell = row.getCell(c);
                    if (cell == null) {
                        vals[c] = null;
                    } else {
                        switch (cell.getCellType()) {
                            case STRING -> vals[c] = cell.getStringCellValue();
                            case NUMERIC -> vals[c] = cell.getNumericCellValue();
                            case BOOLEAN -> vals[c] = cell.getBooleanCellValue();
                            case FORMULA -> vals[c] = cell.getCellFormula();
                            default -> vals[c] = null;
                        }
                    }
                }
                df.addRow(vals);
            }
            return df;
        }
    }

    /* ===== Filtering ===== */

    public interface RowView {
        int index();
        Object get(String col);
        default <T> T getAs(String col, Class<T> cls) { @SuppressWarnings("unchecked") T v = (T) get(col); return v; }
    }

    public SimpleFrame filter(Predicate<RowView> p) {
        SimpleFrame out = new SimpleFrame(columns);
        RowViewImpl rv = new RowViewImpl();
        for (int i = 0; i < rowCount(); i++) {
            rv.i = i;
            if (p.test(rv)) out.rows.add(Arrays.copyOf(rows.get(i), colCount()));
        }
        return out;
    }

    private final class RowViewImpl implements RowView {
        int i;
        public int index() { return i; }
        public Object get(String col) { return rows.get(i)[colIndex(col)]; }
    }

    /** Convenience: where column value satisfies a predicate */
    public SimpleFrame where(String col, Predicate<Object> p) {
        int idx = colIndex(col);
        SimpleFrame out = new SimpleFrame(columns);
        for (Object[] row : rows) {
            if (p.test(row[idx])) out.rows.add(Arrays.copyOf(row, row.length));
        }
        return out;
    }

    /* ===== Sorting ===== */

    public SimpleFrame sortBy(String col, Comparator<Object> cmp, boolean ascending) {
        int idx = colIndex(col);
        List<Object[]> copy = new ArrayList<>(rows);
        Comparator<Object[]> rowCmp = (a, b) -> {
            Object va = a[idx], vb = b[idx];
            if (va == null && vb == null) return 0;
            if (va == null) return 1; // nulls last
            if (vb == null) return -1;
            int c = cmp.compare(va, vb);
            return ascending ? c : -c;
        };
        copy.sort(rowCmp);
        SimpleFrame out = new SimpleFrame(columns);
        out.rows.addAll(copy.stream().map(r -> Arrays.copyOf(r, r.length)).toList());
        return out;
    }

    /* ===== Head / Tail ===== */

    public SimpleFrame head(int n) {
        n = Math.max(0, Math.min(n, rowCount()));
        SimpleFrame out = new SimpleFrame(columns);
        for (int i = 0; i < n; i++) out.rows.add(Arrays.copyOf(rows.get(i), colCount()));
        return out;
    }

    public SimpleFrame tail(int n) {
        n = Math.max(0, Math.min(n, rowCount()));
        SimpleFrame out = new SimpleFrame(columns);
        for (int i = rowCount() - n; i < rowCount(); i++) out.rows.add(Arrays.copyOf(rows.get(i), colCount()));
        return out;
    }

    /* ===== CSV (very basic) ===== */

    public static SimpleFrame readCSV(Path path, boolean header) throws IOException {
        List<String> lines = Files.readAllLines(path);
        if (lines.isEmpty()) throw new IllegalArgumentException("Empty file");
        List<String> cols;
        int startRow = 0;

        if (header) {
            cols = parseCsvLine(lines.get(0));
            startRow = 1;
        } else {
            int fields = parseCsvLine(lines.get(0)).size();
            cols = IntStream.range(0, fields).mapToObj(i -> "c" + i).toList();
        }

        SimpleFrame df = new SimpleFrame(cols);
        for (int i = startRow; i < lines.size(); i++) {
            List<String> parts = parseCsvLine(lines.get(i));
            Object[] row = new Object[cols.size()];
            for (int c = 0; c < cols.size(); c++) {
                row[c] = c < parts.size() ? parts.get(c) : null;
            }
            df.rows.add(row);
        }
        return df;
    }

    // super-naive CSV splitter (no quotes/escapes). Replace with a real parser if needed.
    private static List<String> parseCsvLine(String line) {
        return Arrays.asList(line.split(",", -1));
    }

    /* ===== Pretty print ===== */

    @Override public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SimpleFrame[").append(rowCount()).append("x").append(colCount()).append("]\n");
        sb.append(String.join(" | ", columns)).append("\n");
        sb.append("-".repeat(Math.max(8, columns.stream().mapToInt(String::length).sum() + 3*(columns.size()-1)))).append("\n");
        int show = Math.min(rowCount(), 10);
        for (int r = 0; r < show; r++) {
            Object[] row = rows.get(r);
            List<String> cells = new ArrayList<>(row.length);
            for (Object v : row) cells.add(Objects.toString(v, "null"));
            sb.append(String.join(" | ", cells)).append("\n");
        }
        if (rowCount() > show) sb.append("... (").append(rowCount() - show).append(" more rows)\n");
        return sb.toString();
    }

    /* ===== Demo ===== */

    public static void main(String[] args) {
        SimpleFrame df = SimpleFrame.of("price", "qty", "cat");
        df.addRow(10.0, 5,  "A");
        df.addRow(12.5, 2,  "B");
        df.addRow(9.9,  10, "A");
        df.addRow(11.1, 0,  "C");
        df.addRow(15.2, 1,  "B");

        System.out.println(df);

        // where on a column
        var pricey = df.where("price", v -> v != null && ((Double)v) > 12.0);
        System.out.println("price > 12\n" + pricey);

        // filter with RowView
        var nonZeroRevenue = df.filter(r -> {
            Integer q = r.getAs("qty", Integer.class);
            Double p  = r.getAs("price", Double.class);
            return q != null && q > 0 && p != null && p * q >= 50.0;
        });
        System.out.println("rev>=50\n" + nonZeroRevenue);

        // select and sort
        var sorted = df.select("price", "cat", "qty")
                       .sortBy("price", Comparator.comparingDouble(o -> (Double)o), true)
                       .head(3);
        System.out.println("sorted/head\n" + sorted);

        // add a computed column
        List<Double> revenue = new ArrayList<>();
        for (int i = 0; i < df.rowCount(); i++) {
            Double p = df.getAs(i, "price", Double.class);
            Integer q = df.getAs(i, "qty", Integer.class);
            revenue.add( (p == null || q == null) ? null : p * q );
        }
        df.addColumn("revenue", revenue);
        System.out.println("with revenue\n" + df);
    }
}

