package com.example.ui;

import com.example.nodes.BaseNode;
import java.lang.reflect.Constructor;
import java.util.*;

public class NodeRegistry {

    private static final Map<String, Class<? extends BaseNode>> registry = new HashMap<>();

    public static void register(Class<? extends BaseNode> clazz) {
        registry.put(clazz.getSimpleName(), clazz);
    }

    public static BaseNode create(String name, double x, double y) {
        Class<? extends BaseNode> clazz = registry.get(name);
        if (clazz == null) return null;

        try {
            Constructor<? extends BaseNode> ctor = clazz.getConstructor(double.class, double.class);
            return ctor.newInstance(x, y);
        } catch (Exception e) {
            System.err.println("Failed to instantiate node: " + name);
            e.printStackTrace();
            return null;
        }
    }

    public static Set<String> getRegisteredNames() {
        return registry.keySet();
    }
}
