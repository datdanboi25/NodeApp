package com.example.misc;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/** FastFrame — columnar, primitive-first dataframe with fast CSV/XLSX readers. */
public final class FastFrame {

    /* ===================== Column Types & APIs ===================== */

    public enum ColType { INT, LONG, DOUBLE, BOOL, DATETIME, STRING }

    public interface Column {
        String name();
        ColType type();
        int size();
        boolean isNull(int row);
        Column reindex(int[] index);
        Column selectMask(BitSet mask, int n);
    }

    /* ----- Primitive columns ----- */

    public static final class IntColumn implements Column {
        private final String name; private final int[] data; private final BitSet nulls;
        public IntColumn(String n, int[] d, BitSet z){name=n;data=d;nulls=z==null?new BitSet():cloneBS(z);}
        public String name(){return name;} public ColType type(){return ColType.INT;} public int size(){return data.length;}
        public boolean isNull(int r){return nulls.get(r);} public int get(int r){return data[r];}
        public IntColumn reindex(int[] idx){int[] o=new int[idx.length];BitSet z=new BitSet();for(int i=0;i<idx.length;i++){int r=idx[i];o[i]=data[r];if(nulls.get(r))z.set(i);}return new IntColumn(name,o,z);}
        public IntColumn selectMask(BitSet m,int n){int[] o=new int[n];BitSet z=new BitSet();int j=0;for(int i=m.nextSetBit(0);i>=0;i=m.nextSetBit(i+1)){o[j]=data[i];if(nulls.get(i))z.set(j);j++;}return new IntColumn(name,o,z);}
        IntColumn rename(String nn){return new IntColumn(nn,data,nulls);}
    }

    public static final class LongColumn implements Column {
        private final String name; private final long[] data; private final BitSet nulls;
        public LongColumn(String n,long[] d,BitSet z){name=n;data=d;nulls=z==null?new BitSet():cloneBS(z);}
        public String name(){return name;} public ColType type(){return ColType.LONG;} public int size(){return data.length;}
        public boolean isNull(int r){return nulls.get(r);} public long get(int r){return data[r];}
        public LongColumn reindex(int[] idx){long[] o=new long[idx.length];BitSet z=new BitSet();for(int i=0;i<idx.length;i++){int r=idx[i];o[i]=data[r];if(nulls.get(r))z.set(i);}return new LongColumn(name,o,z);}
        public LongColumn selectMask(BitSet m,int n){long[] o=new long[n];BitSet z=new BitSet();int j=0;for(int i=m.nextSetBit(0);i>=0;i=m.nextSetBit(i+1)){o[j]=data[i];if(nulls.get(i))z.set(j);j++;}return new LongColumn(name,o,z);}
        LongColumn rename(String nn){return new LongColumn(nn,data,nulls);}
    }

    public static final class DoubleColumn implements Column {
        private final String name; private final double[] data; private final BitSet nulls;
        public DoubleColumn(String n,double[] d,BitSet z){name=n;data=d;nulls=z==null?new BitSet():cloneBS(z);}
        public String name(){return name;} public ColType type(){return ColType.DOUBLE;} public int size(){return data.length;}
        public boolean isNull(int r){return nulls.get(r);} public double get(int r){return data[r];}
        public DoubleColumn reindex(int[] idx){double[] o=new double[idx.length];BitSet z=new BitSet();for(int i=0;i<idx.length;i++){int r=idx[i];o[i]=data[r];if(nulls.get(r))z.set(i);}return new DoubleColumn(name,o,z);}
        public DoubleColumn selectMask(BitSet m,int n){double[] o=new double[n];BitSet z=new BitSet();int j=0;for(int i=m.nextSetBit(0);i>=0;i=m.nextSetBit(i+1)){o[j]=data[i];if(nulls.get(i))z.set(j);j++;}return new DoubleColumn(name,o,z);}
        DoubleColumn rename(String nn){return new DoubleColumn(nn,data,nulls);}
    }

    public static final class BoolColumn implements Column {
        private final String name; private final boolean[] data; private final BitSet nulls;
        public BoolColumn(String n,boolean[] d,BitSet z){name=n;data=d;nulls=z==null?new BitSet():cloneBS(z);}
        public String name(){return name;} public ColType type(){return ColType.BOOL;} public int size(){return data.length;}
        public boolean isNull(int r){return nulls.get(r);} public boolean get(int r){return data[r];}
        public BoolColumn reindex(int[] idx){boolean[] o=new boolean[idx.length];BitSet z=new BitSet();for(int i=0;i<idx.length;i++){int r=idx[i];o[i]=data[r];if(nulls.get(r))z.set(i);}return new BoolColumn(name,o,z);}
        public BoolColumn selectMask(BitSet m,int n){boolean[] o=new boolean[n];BitSet z=new BitSet();int j=0;for(int i=m.nextSetBit(0);i>=0;i=m.nextSetBit(i+1)){o[j]=data[i];if(nulls.get(i))z.set(j);j++;}return new BoolColumn(name,o,z);}
        BoolColumn rename(String nn){return new BoolColumn(nn,data,nulls);}
    }

    /** DATETIME = epoch millis in a long[] (fast comparisons/ranges). */
    public static final class DateTimeColumn implements Column {
        private final String name;
        private final long[] data;      // epoch millis
        private final BitSet nulls;

        public DateTimeColumn(String name, long[] data, BitSet nulls) {
            this.name = name;
            this.data = Objects.requireNonNull(data);
            this.nulls = (nulls == null) ? new BitSet() : (BitSet) nulls.clone();
        }

        @Override
        public String name() { return name; }

        @Override
        public ColType type() { return ColType.DATETIME; }

        @Override
        public int size() { return data.length; }

        @Override
        public boolean isNull(int row) { return nulls.get(row); }

        public long get(int row) { return data[row]; }

        @Override
        public DateTimeColumn reindex(int[] idx) {
            long[] out = new long[idx.length];
            BitSet nz = new BitSet();
            for (int i = 0; i < idx.length; i++) {
                int r = idx[i];
                out[i] = data[r];
                if (nulls.get(r)) nz.set(i);
            }
            return new DateTimeColumn(name, out, nz);
        }

        @Override
        public DateTimeColumn selectMask(BitSet mask, int n) {
            long[] out = new long[n];
            BitSet nz = new BitSet();
            int j = 0;
            for (int i = mask.nextSetBit(0); i >= 0; i = mask.nextSetBit(i + 1)) {
                out[j] = data[i];
                if (nulls.get(i)) nz.set(j);
                j++;
            }
            return new DateTimeColumn(name, out, nz);
        }
}


    /* ----- Dictionary-coded strings ----- */
    public static final class StringColumn implements Column {
        private final String name; private final String[] dict; private final int[] codes; private final BitSet nulls;
        public StringColumn(String n, String[] dict, int[] codes, BitSet z){name=n;this.dict=dict;this.codes=codes;nulls=z==null?new BitSet():cloneBS(z);}
        public String name(){return name;} public ColType type(){return ColType.STRING;} public int size(){return codes.length;}
        public boolean isNull(int r){return nulls.get(r);} public String get(int r){if(nulls.get(r))return null;int c=codes[r];return c<0?null:dict[c];}
        public StringColumn reindex(int[] idx){int[] o=new int[idx.length];BitSet z=new BitSet();for(int i=0;i<idx.length;i++){int r=idx[i];o[i]=codes[r];if(nulls.get(r))z.set(i);}return new StringColumn(name,dict,o,z);}
        public StringColumn selectMask(BitSet m,int n){int[] o=new int[n];BitSet z=new BitSet();int j=0;for(int i=m.nextSetBit(0);i>=0;i=m.nextSetBit(i+1)){o[j]=codes[i];if(nulls.get(i))z.set(j);j++;}return new StringColumn(name,dict,o,z);}
        StringColumn rename(String nn){return new StringColumn(nn,dict,codes,nulls);}
    }

    /* ===================== Frame Core ===================== */

    private final LinkedHashMap<String, Column> cols;
    private final int rows;

    private FastFrame(LinkedHashMap<String, Column> cols){
        if(cols.isEmpty()) throw new IllegalArgumentException("No columns");
        int n=-1; for(Column c:cols.values()){ if(n==-1) n=c.size(); else if(n!=c.size()) throw new IllegalArgumentException("All columns same length"); }
        this.cols=cols; this.rows=n;
    }

    public int rows(){return rows;}
    public int cols(){return cols.size();}
    public Set<String> colNames(){return Collections.unmodifiableSet(cols.keySet());}
    public Column col(String name){Column c=cols.get(name); if(c==null) throw new IllegalArgumentException("No such column: "+name); return c;}

    /* ----- Builder (fast path adds) ----- */
    public static final class Builder {
        private final LinkedHashMap<String, Column> cols = new LinkedHashMap<>();
        public Builder addInt(String name, int[] data, BitSet nulls){ cols.put(name, new IntColumn(name,data,nulls)); return this; }
        public Builder addLong(String name, long[] data, BitSet nulls){ cols.put(name, new LongColumn(name,data,nulls)); return this; }
        public Builder addDouble(String name, double[] data, BitSet nulls){ cols.put(name, new DoubleColumn(name,data,nulls)); return this; }
        public Builder addBool(String name, boolean[] data, BitSet nulls){ cols.put(name, new BoolColumn(name,data,nulls)); return this; }
        public Builder addDateTime(String name, long[] epochMillis, BitSet nulls) { cols.put(name, new DateTimeColumn(name, epochMillis, nulls));return this;}
        public Builder addString(String name, String[] dict, int[] codes, BitSet nulls){ cols.put(name, new StringColumn(name,dict,codes,nulls)); return this; }
        public FastFrame build(){ return new FastFrame(cols); }
    }

    /* ===================== Vectorized operations ===================== */

    public FastFrame whereDouble(String col, DoublePredicate p){
        DoubleColumn c=(DoubleColumn) require(col, ColType.DOUBLE);
        BitSet m=new BitSet(rows); for(int i=0;i<rows;i++) if(!c.isNull(i) && p.test(c.get(i))) m.set(i);
        return selectMask(m);
    }
    public FastFrame whereInt(String col, IntPredicate p){
        IntColumn c=(IntColumn) require(col, ColType.INT);
        BitSet m=new BitSet(rows); for(int i=0;i<rows;i++) if(!c.isNull(i) && p.test(c.get(i))) m.set(i);
        return selectMask(m);
    }
    public FastFrame whereStringEquals(String col, String value){
        StringColumn c=(StringColumn) require(col, ColType.STRING);
        BitSet m=new BitSet(rows);
        for(int i=0;i<rows;i++){ String v=c.get(i); if(Objects.equals(v,value)) m.set(i); }
        return selectMask(m);
    }

    public FastFrame select(String... names){
        LinkedHashMap<String, Column> out=new LinkedHashMap<>();
        for(String n:names) out.put(n, rename(cols.get(n), n));
        return new FastFrame(out);
    }

    public FastFrame drop(String... names){
        Set<String> d=Set.of(names);
        LinkedHashMap<String, Column> out=new LinkedHashMap<>();
        for(var e:cols.entrySet()) if(!d.contains(e.getKey())) out.put(e.getKey(), e.getValue());
        return new FastFrame(out);
    }

    public FastFrame sortByDouble(String name, boolean asc) {
        DoubleColumn c = (DoubleColumn) require(name, ColType.DOUBLE);
        int[] idx = IntStream.range(0, rows).toArray();
        sortIndexByDouble(idx, c, asc);           // custom primitive sort
        return reindex(idx);
    }

    private static void sortIndexByDouble(int[] idx, DoubleColumn c, boolean asc) {
        quickSort(idx, 0, idx.length - 1, c, asc);
    }

    private static void quickSort(int[] a, int lo, int hi, DoubleColumn c, boolean asc) {
        while (lo < hi) {
            int i = lo, j = hi;
            int p = a[(lo + hi) >>> 1];

            while (i <= j) {
                while (compareIndex(a[i], p, c, asc) < 0) i++;
                while (compareIndex(a[j], p, c, asc) > 0) j--;
                if (i <= j) {
                    int t = a[i]; a[i] = a[j]; a[j] = t;
                    i++; j--;
                }
            }
            if (j - lo < hi - i) { // tail recursion elimination
                if (lo < j) quickSort(a, lo, j, c, asc);
                lo = i;
            } else {
                if (i < hi) quickSort(a, i, hi, c, asc);
                hi = j;
            }
        }
    }

    private static int compareIndex(int ia, int ib, DoubleColumn c, boolean asc) {
        boolean na = c.isNull(ia), nb = c.isNull(ib);
        if (na && nb) return 0;
        if (na) return 1;               // nulls last
        if (nb) return -1;
        int cmp = Double.compare(c.get(ia), c.get(ib));
        return asc ? cmp : -cmp;
    }


    public FastFrame head(int n){ n=Math.max(0,Math.min(n,rows)); return reindex(IntStream.range(0,n).toArray()); }
    public FastFrame tail(int n){ n=Math.max(0,Math.min(n,rows)); return reindex(IntStream.range(rows-n, rows).toArray()); }

    /* ----- internal reindex/select ----- */
    private FastFrame selectMask(BitSet m){
        int n=m.cardinality(); LinkedHashMap<String, Column> out=new LinkedHashMap<>();
        for(var e:cols.entrySet()) out.put(e.getKey(), e.getValue().selectMask(m, n));
        return new FastFrame(out);
    }
    private FastFrame reindex(int[] idx){
        LinkedHashMap<String, Column> out=new LinkedHashMap<>();
        for(var e:cols.entrySet()) out.put(e.getKey(), e.getValue().reindex(idx));
        return new FastFrame(out);
    }
    private Column require(String name, ColType t){
        Column c=col(name); if(c.type()!=t) throw new IllegalArgumentException(name+" is not "+t); return c;
    }
    private static BitSet cloneBS(BitSet b){ return (BitSet) b.clone(); }
    private static Column rename(Column c, String nn){
        if(c instanceof IntColumn ic) return ic.rename(nn);
        if(c instanceof LongColumn lc) return lc.rename(nn);
        if(c instanceof DoubleColumn dc) return dc.rename(nn);
        if(c instanceof BoolColumn bc) return bc.rename(nn);
        if (c instanceof DateTimeColumn dt) { return new DateTimeColumn(nn, dt.data, dt.nulls);}
        if(c instanceof StringColumn sc) return sc.rename(nn);
        throw new IllegalStateException();
    }

    /* ===================== FAST READER ===================== */

    /** Options for reading CSV/XLSX. */
    public static final class ReadOptions {
        public final boolean header;
        public final Character delimiter;   // null => auto
        public final boolean trimCells;
        public final boolean emptyAsNull;
        public final int inferSampleRows;
        public final int maxRows;           // <=0 => unlimited
        public final String sheetName;      // xlsx
        public final int sheetIndex;        // xlsx (if no sheetName)
        public final boolean evaluateFormulas;
        public final boolean inferTypes;
        private ReadOptions(Builder b){header=b.header;delimiter=b.delimiter;trimCells=b.trimCells;emptyAsNull=b.emptyAsNull;inferSampleRows=b.inferSampleRows;maxRows=b.maxRows;sheetName=b.sheetName;sheetIndex=b.sheetIndex;evaluateFormulas=b.evaluateFormulas;inferTypes=b.inferTypes;}
        public static Builder builder(){return new Builder();}
        public static final class Builder{
            boolean header=true; Character delimiter=null; boolean trimCells=true; boolean emptyAsNull=true;
            int inferSampleRows=200; int maxRows=0; String sheetName=null; int sheetIndex=0; boolean evaluateFormulas=true; boolean inferTypes=true;
            public Builder header(boolean v){header=v;return this;} public Builder delimiter(Character v){delimiter=v;return this;}
            public Builder trimCells(boolean v){trimCells=v;return this;} public Builder emptyAsNull(boolean v){emptyAsNull=v;return this;}
            public Builder inferSampleRows(int v){inferSampleRows=v;return this;} public Builder maxRows(int v){maxRows=v;return this;}
            public Builder sheet(String n){sheetName=n;return this;} public Builder sheetIndex(int v){sheetIndex=v;return this;}
            public Builder evaluateFormulas(boolean v){evaluateFormulas=v;return this;} public Builder inferTypes(boolean v){inferTypes=v;return this;}
            public ReadOptions build(){return new ReadOptions(this);}
        }
    }

    public static FastFrame readAuto(Path path, ReadOptions opts) throws IOException {
        String n=path.getFileName().toString().toLowerCase();
        if(n.endsWith(".csv"))  return readCSV(path, opts);
        if(n.endsWith(".xlsx")) return readExcel(path, opts);
        throw new IllegalArgumentException("Unsupported file: "+n);
    }

    /* ----- CSV (fast, robust, auto-delim, quoted newlines) ----- */
    public static FastFrame readCSV(Path path, ReadOptions opts) throws IOException {
        String content = Files.readString(path, StandardCharsets.UTF_8);
        if(content.length()>0 && content.charAt(0)=='\uFEFF') content=content.substring(1);

        // logical records (respect quoted newlines)
        List<String> records = csvSplitRecords(content);
        if(records.isEmpty()) throw new IllegalArgumentException("Empty CSV: "+path);

        char delim = (opts.delimiter!=null)?opts.delimiter:detectDelimiter(records);

        // parse first record for header/width
        List<String> first = csvParseFields(records.get(0), delim);
        List<String> headers;
        int dataStart = 0;
        if(opts.header){
            headers = sanitizeHeaders(first);
            dataStart = 1;
        } else {
            headers = new ArrayList<>(first.size());
            for(int i=0;i<first.size();i++) headers.add("c"+i);
        }
        int cols = headers.size();

        // infer types from sample
        List<List<String>> sample = new ArrayList<>();
        int sampleEnd = Math.min(records.size(), dataStart + Math.max(0, opts.inferSampleRows));
        for(int i=dataStart;i<sampleEnd;i++) sample.add(fixWidth(csvParseFields(records.get(i), delim), cols));
        List<ColType> types = opts.inferTypes ? inferTypes(sample, opts) : defaultTypes(cols);

        // allocate final arrays + null masks
        ColumnBuilder[] builders = allocBuilders(headers, types, estimateRows(records.size(), dataStart, opts.maxRows));

        // fill
        int limit = opts.maxRows>0 ? Math.min(records.size(), dataStart+opts.maxRows) : records.size();
        for(int r=dataStart; r<limit; r++){
            List<String> fields = fixWidth(csvParseFields(records.get(r), delim), cols);
            appendRow(builders, fields, types, opts);
        }
        return buildFrame(headers, builders);
    }

    /* ----- Excel (fast path, primitives, formulas, dates) ----- */
    public static FastFrame readExcel(Path path, ReadOptions opts) throws IOException {
        try(InputStream is = Files.newInputStream(path); Workbook wb = new XSSFWorkbook(is)){
            Sheet sheet = (opts.sheetName!=null) ? wb.getSheet(opts.sheetName) : wb.getSheetAt(opts.sheetIndex);
            if(sheet==null) throw new IllegalArgumentException("Sheet not found");
            FormulaEvaluator eval = opts.evaluateFormulas ? wb.getCreationHelper().createFormulaEvaluator() : null;

            int firstRow=sheet.getFirstRowNum();
            Row headerRow = sheet.getRow(firstRow);
            if(headerRow==null) throw new IllegalArgumentException("No rows");

            List<String> headers;
            int dataStart;
            short width = headerRow.getLastCellNum();
            if(opts.header){
                headers = new ArrayList<>(width);
                for(int c=0;c<width;c++) headers.add(sanitizeOne(asString(cellValue(headerRow.getCell(c), eval), opts), "c"+c));
                headers = dedupeHeaders(headers);
                dataStart = firstRow+1;
            }else{
                headers = new ArrayList<>(width);
                for(int c=0;c<width;c++) headers.add("c"+c);
                dataStart = firstRow;
            }
            int cols = headers.size();

            // sample for types
            List<List<String>> sample = new ArrayList<>();
            int rEnd = Math.min(sheet.getLastRowNum(), dataStart + Math.max(0, opts.inferSampleRows));
            for(int r=dataStart; r<=rEnd; r++){
                Row row = sheet.getRow(r); if(row==null) continue;
                sample.add(excelRowAsStrings(row, cols, eval, opts));
            }
            List<ColType> types = opts.inferTypes ? inferTypes(sample, opts) : defaultTypes(cols);

            ColumnBuilder[] builders = allocBuilders(headers, types, estimateRows(sheet.getLastRowNum()-dataStart+1, 0, opts.maxRows));

            int count=0;
            for(int r=dataStart; r<=sheet.getLastRowNum(); r++){
                if(opts.maxRows>0 && count>=opts.maxRows) break;
                Row row = sheet.getRow(r); if(row==null){appendNullRow(builders); count++; continue;}
                appendRow(builders, excelRowAsStrings(row, cols, eval, opts), types, opts);
                count++;
            }
            return buildFrame(headers, builders);
        }
    }

    /* ===================== Reader internals ===================== */

    private static int estimateRows(int totalRecords, int dataStart, int maxRows){
        int n = totalRecords - dataStart;
        if(maxRows>0) n = Math.min(n, maxRows);
        return Math.max(n, 0);
    }

    /** Builder per column to append quickly then compact to primitive array. */
    private static abstract class ColumnBuilder {
        final BitSet nulls = new BitSet();
        int row=0;
        abstract void append(String raw, ReadOptions opts);
        abstract Column finish(String name);
        void appendNull(){ nulls.set(row++); }
        void ensureCapacity(int cap){}
    }
    private static final class IntBuilder extends ColumnBuilder {
        int[] buf; IntBuilder(int cap){buf=new int[Math.max(8,cap)];}
        void append(String s, ReadOptions o){ if(s==null){appendNull();return;} try{buf[row]=Integer.parseInt(s.trim()); row++;}catch(Exception e){appendNull();}}
        Column finish(String n){ return new IntColumn(n, Arrays.copyOf(buf,row), nulls); }
        void ensureCapacity(int cap){ if(buf.length<cap) buf=Arrays.copyOf(buf, cap); }
    }
    private static final class LongBuilder extends ColumnBuilder {
        long[] buf; LongBuilder(int cap){buf=new long[Math.max(8,cap)];}
        void append(String s, ReadOptions o){ if(s==null){appendNull();return;} try{buf[row]=Long.parseLong(s.trim()); row++;}catch(Exception e){appendNull();}}
        Column finish(String n){ return new LongColumn(n, Arrays.copyOf(buf,row), nulls); }
        void ensureCapacity(int cap){ if(buf.length<cap) buf=Arrays.copyOf(buf, cap); }
    }
    private static final class DoubleBuilder extends ColumnBuilder {
        double[] buf; DoubleBuilder(int cap){buf=new double[Math.max(8,cap)];}
        void append(String s, ReadOptions o){ if(s==null){appendNull();return;} try{buf[row]=Double.parseDouble(s.trim()); row++;}catch(Exception e){appendNull();}}
        Column finish(String n){ return new DoubleColumn(n, Arrays.copyOf(buf,row), nulls); }
        void ensureCapacity(int cap){ if(buf.length<cap) buf=Arrays.copyOf(buf, cap); }
    }
    private static final class BoolBuilder extends ColumnBuilder {
        boolean[] buf; BoolBuilder(int cap){buf=new boolean[Math.max(8,cap)];}
        void append(String s, ReadOptions o){ if(s==null){appendNull();return;} String t=s.trim().toLowerCase(Locale.ROOT); boolean v = t.equals("true")||t.equals("t")||t.equals("yes")||t.equals("1"); buf[row]=v; row++; }
        Column finish(String n){ return new BoolColumn(n, Arrays.copyOf(buf,row), nulls); }
        void ensureCapacity(int cap){ if(buf.length<cap) buf=Arrays.copyOf(buf, cap); }
    }
    private static final class DateTimeBuilder extends ColumnBuilder {
        long[] buf; DateTimeBuilder(int cap){buf=new long[Math.max(8,cap)];}
        void append(String s, ReadOptions o){ if(s==null){appendNull();return;} Long v=parseEpochMillis(s.trim()); if(v==null){appendNull();} else {buf[row]=v; row++;} }
        Column finish(String n){ return new DateTimeColumn(n, Arrays.copyOf(buf,row), nulls); }
        void ensureCapacity(int cap){ if(buf.length<cap) buf=Arrays.copyOf(buf, cap); }
    }
    private static final class StringDictBuilder extends ColumnBuilder {
        final Map<String,Integer> map=new LinkedHashMap<>(); final List<String> dict=new ArrayList<>();
        int[] codes; StringDictBuilder(int cap){codes=new int[Math.max(8,cap)];}
        void append(String s, ReadOptions o){
            if(o!=null && o.trimCells && s!=null) s=s.trim();
            if(o!=null && o.emptyAsNull && (s==null || s.isBlank())) s=null;
            if(s==null){appendNull();return;}
            Integer code=map.get(s);
            if(code==null){code=dict.size(); map.put(s,code); dict.add(s);}
            codes[row]=code; row++;
        }
        Column finish(String n){ return new StringColumn(n, dict.toArray(new String[0]), Arrays.copyOf(codes,row), nulls); }
        void ensureCapacity(int cap){ if(codes.length<cap) codes=Arrays.copyOf(codes, cap); }
    }

    private static ColumnBuilder[] allocBuilders(List<String> headers, List<ColType> types, int capacity){
        ColumnBuilder[] b = new ColumnBuilder[headers.size()];
        for(int i=0;i<headers.size();i++){
            switch(types.get(i)){
                case INT -> b[i]=new IntBuilder(capacity);
                case LONG -> b[i]=new LongBuilder(capacity);
                case DOUBLE -> b[i]=new DoubleBuilder(capacity);
                case BOOL -> b[i]=new BoolBuilder(capacity);
                case DATETIME -> b[i]=new DateTimeBuilder(capacity);
                case STRING -> b[i]=new StringDictBuilder(capacity);
            }
        }
        return b;
    }

    private static void appendRow(ColumnBuilder[] b, List<String> fields, List<ColType> types, ReadOptions opts){
        for(int c=0;c<b.length;c++){
            String raw = c<fields.size()?fields.get(c):null;
            if(opts.trimCells && raw!=null) raw=raw.trim();
            if(opts.emptyAsNull && (raw==null || raw.isBlank())) raw=null;
            b[c].append(raw, opts);
        }
    }
    private static void appendNullRow(ColumnBuilder[] b){ for(ColumnBuilder cb:b) cb.append(null, new ReadOptions.Builder().build()); }

    private static FastFrame buildFrame(List<String> headers, ColumnBuilder[] b){
        LinkedHashMap<String, Column> out = new LinkedHashMap<>();
        for(int i=0;i<headers.size();i++) out.put(headers.get(i), b[i].finish(headers.get(i)));
        return new FastFrame(out);
    }

    /* ----- Type inference ----- */
    private static final DateTimeFormatter[] DATE_FMTS = new DateTimeFormatter[]{
        DateTimeFormatter.ISO_LOCAL_DATE_TIME,
        DateTimeFormatter.ISO_OFFSET_DATE_TIME,
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("d/M/uuuu[ H[:mm][:ss]]"),
        DateTimeFormatter.ofPattern("M/d/uuuu[ H[:mm][:ss]]")
    };
    private static List<ColType> inferTypes(List<List<String>> sample, ReadOptions opts){
        if(sample.isEmpty()) return List.of();
        int cols = sample.get(0).size();
        List<ColType> t = new ArrayList<>(Collections.nCopies(cols, ColType.STRING));
        for(int c=0;c<cols;c++){
            boolean any=false, allInt=true, allLong=true, allDbl=true, allBool=true, allDate=true;
            for(List<String> row: sample){
                String s = c<row.size()?row.get(c):null;
                if(opts.trimCells && s!=null) s=s.trim();
                if(opts.emptyAsNull && (s==null || s.isBlank())) continue;
                any=true;
                if(allInt && !isInt(s)) allInt=false;
                if(allLong && !isLong(s)) allLong=false;
                if(allDbl && !isDouble(s)) allDbl=false;
                if(allBool && !isBool(s)) allBool=false;
                if(allDate && parseEpochMillis(s)==null) allDate=false;
                if(!allInt && !allLong && !allDbl && !allBool && !allDate) break;
            }
            if(!any) { t.set(c, ColType.STRING); continue; }
            if(allInt) t.set(c, ColType.INT);
            else if(allLong) t.set(c, ColType.LONG);
            else if(allBool) t.set(c, ColType.BOOL);
            else if(allDate) t.set(c, ColType.DATETIME);
            else if(allDbl) t.set(c, ColType.DOUBLE);
            else t.set(c, ColType.STRING);
        }
        return t;
    }
    private static List<ColType> defaultTypes(int cols){ List<ColType> t=new ArrayList<>(cols); for(int i=0;i<cols;i++) t.add(ColType.STRING); return t; }

    private static boolean isInt(String s){ try{ Integer.parseInt(s); return true; }catch(Exception e){return false;} }
    private static boolean isLong(String s){ try{ Long.parseLong(s); return true; }catch(Exception e){return false;} }
    private static boolean isDouble(String s){ try{ Double.parseDouble(s); return true; }catch(Exception e){return false;} }
    private static boolean isBool(String s){ String t=s.toLowerCase(Locale.ROOT); return t.equals("true")||t.equals("false")||t.equals("t")||t.equals("f")||t.equals("yes")||t.equals("no")||t.equals("1")||t.equals("0"); }
    private static Long parseEpochMillis(String s){
        if(s==null) return null;
        // accept epoch seconds/millis numeric
        if(isLong(s)){ long v=Long.parseLong(s); if(v<3_000_000_000L) return v*1000; return v; }
        for(DateTimeFormatter f: DATE_FMTS){
            try{
                if(f==DateTimeFormatter.ISO_LOCAL_DATE) return LocalDate.parse(s,f).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
                if(f==DateTimeFormatter.ISO_LOCAL_DATE_TIME) return LocalDateTime.parse(s,f).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                if(f==DateTimeFormatter.ISO_OFFSET_DATE_TIME) return OffsetDateTime.parse(s,f).toInstant().toEpochMilli();
                // custom patterns use LocalDateTime by default
                try { return LocalDateTime.parse(s,f).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(); }
                catch(Exception ignored){}
            }catch(Exception ignored){}
        }
        return null;
    }

    /* ----- CSV helpers ----- */
    private static List<String> csvSplitRecords(String text){
        List<String> recs=new ArrayList<>(); StringBuilder cur=new StringBuilder(); boolean inQ=false;
        for(int i=0;i<text.length();i++){
            char ch=text.charAt(i);
            if(ch=='"'){
                if(inQ && i+1<text.length() && text.charAt(i+1)=='"'){ cur.append('"'); i++; }
                else inQ=!inQ;
            }else if((ch=='\n'||ch=='\r')){
                if(inQ) cur.append(ch);
                else{
                    recs.add(cur.toString()); cur.setLength(0);
                    if(ch=='\r' && i+1<text.length() && text.charAt(i+1)=='\n') i++;
                }
            }else cur.append(ch);
        }
        if(cur.length()>0 || text.endsWith("\n") || text.endsWith("\r")) recs.add(cur.toString());
        return recs;
    }
    private static List<String> csvParseFields(String rec, char d){
        List<String> out=new ArrayList<>(); StringBuilder cur=new StringBuilder(); boolean inQ=false;
        for(int i=0;i<rec.length();i++){
            char ch=rec.charAt(i);
            if(ch=='"'){
                if(inQ && i+1<rec.length() && rec.charAt(i+1)=='"'){ cur.append('"'); i++; }
                else inQ=!inQ;
            }else if(ch==d && !inQ){ out.add(cur.toString()); cur.setLength(0); }
            else cur.append(ch);
        }
        out.add(cur.toString()); return out;
    }
    private static char detectDelimiter(List<String> records){
        char[] cands={',',';','\t','|'}; int best=-1; char pick=',';
        int probe=Math.min(10, records.size());
        for(char d:cands){
            int hits=0; for(int i=0;i<probe;i++) hits+=csvParseFields(records.get(i), d).size();
            if(hits>best){best=hits; pick=d;}
        }
        return pick;
    }
    private static List<String> fixWidth(List<String> fields, int width){
        if(fields.size()==width) return fields;
        List<String> out=new ArrayList<>(width); out.addAll(fields);
        while(out.size()<width) out.add(null);
        if(out.size()>width) return out.subList(0,width);
        return out;
    }

    /* ----- Header sanitize/dedupe ----- */
    private static List<String> sanitizeHeaders(List<String> raw){
        List<String> out=new ArrayList<>(raw.size()); Set<String> used=new HashSet<>();
        for(int i=0;i<raw.size();i++){ out.add(sanitizeOne(raw.get(i), "c"+i)); }
        return dedupeHeaders(out);
    }
    private static String sanitizeOne(String s, String def){
        if(s==null || s.isBlank()) return def;
        String t=s.trim().replaceAll("[^A-Za-z0-9_]+", "_");
        if(t.isBlank()) t=def; return t;
    }
    private static List<String> dedupeHeaders(List<String> headers){
        Map<String,Integer> seen=new HashMap<>();
        List<String> out=new ArrayList<>(headers.size());
        for(String h:headers){
            int k=seen.getOrDefault(h,0);
            if(k==0){ out.add(h); seen.put(h,1); }
            else { String nh=h+"_"+(k+1); out.add(nh); seen.put(h,k+1); }
        }
        return out;
    }

    /* ----- Excel helpers ----- */
    private static List<String> excelRowAsStrings(Row row, int cols, FormulaEvaluator ev, ReadOptions opts){
        List<String> out=new ArrayList<>(cols);
        for(int c=0;c<cols;c++) out.add(asString(cellValue(row.getCell(c), ev), opts));
        return out;
    }
    private static Object cellValue(Cell cell, FormulaEvaluator ev){
        if(cell==null) return null;
        CellType t = (ev!=null && cell.getCellType()==CellType.FORMULA) ? ev.evaluateFormulaCell(cell) : cell.getCellType();
        return switch(t){
            case STRING -> cell.getRichStringCellValue().getString();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell) ? cell.getLocalDateTimeCellValue() : cell.getNumericCellValue();
            case BOOLEAN -> cell.getBooleanCellValue();
            case BLANK -> null;
            default -> null;
        };
    }
    private static String asString(Object v, ReadOptions opts){
        if(v==null) return null;
        if(v instanceof LocalDateTime ldt) return ldt.toString();
        if(v instanceof Double d){ // keep integers pretty in CSV-like sheets
            if(d == Math.rint(d)) return Long.toString(d.longValue());
            return d.toString();
        }
        return v.toString();
    }

    /* ===================== Pretty print (debug) ===================== */

    public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("FastFrame[").append(rows()).append("x").append(cols()).append("]\n");

    List<String> names = new ArrayList<>(cols.keySet());
    sb.append(String.join(" | ", names)).append("\n");
    sb.append("-".repeat(Math.max(8,
            names.stream().mapToInt(String::length).sum() + 3 * (names.size() - 1)))).append("\n");

    int show = Math.min(rows(), 10);
    for (int r = 0; r < show; r++) {
        List<String> cells = new ArrayList<>(names.size());
        for (String n : names) {
            Column c = cols.get(n);
            switch (c.type()) {
                case INT -> {
                    var ic = (IntColumn) c;
                    cells.add(c.isNull(r) ? "null" : Integer.toString(ic.get(r)));
                }
                case LONG -> {
                    var lc = (LongColumn) c;
                    cells.add(c.isNull(r) ? "null" : Long.toString(lc.get(r)));
                }
                case DOUBLE -> {
                    var dc = (DoubleColumn) c;
                    cells.add(c.isNull(r) ? "null" : Double.toString(dc.get(r)));
                }
                case BOOL -> {
                    var bc = (BoolColumn) c;
                    cells.add(c.isNull(r) ? "null" : Boolean.toString(bc.get(r)));
                }
                case DATETIME -> {
                    var dt = (DateTimeColumn) c;                 // <-- no cast to LongColumn
                    cells.add(c.isNull(r)
                            ? "null"
                            : java.time.Instant.ofEpochMilli(dt.get(r)).toString());
                }
                case STRING -> {
                    var sc = (StringColumn) c;
                    cells.add(java.util.Objects.toString(sc.get(r), "null"));
                }
            }
        }
        sb.append(String.join(" | ", cells)).append("\n");
    }
    if (rows() > show) sb.append("... (").append(rows() - show).append(" more rows)\n");
    return sb.toString();
}

    /* ===================== Demo ===================== */
    public static void main(String[] args) throws Exception {
        var opts = ReadOptions.builder().header(true).inferTypes(true).build();
        // FastFrame df = FastFrame.readAuto(Path.of("data.csv"), opts);
        // System.out.println(df);
        // System.out.println(df.whereDouble("price", p -> p > 12).head(5));
    }
}
