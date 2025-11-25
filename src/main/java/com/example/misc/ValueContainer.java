package com.example.misc;
import java.util.List;

public class ValueContainer {
    private Integer singleValue;
    private List<Integer> listValue;

    public void set(int value) {
        this.singleValue = value;
        this.listValue = null;
    }

    public void set(List<Integer> values) {
        this.listValue = values;
        this.singleValue = null;
    }

    public boolean isSingle() {
        return singleValue != null;
    }

    public boolean isList() {
        return listValue != null;
    }

    public Integer getSingle() {
        return singleValue;
    }

    public List<Integer> getList() {
        return listValue;
    }
}

