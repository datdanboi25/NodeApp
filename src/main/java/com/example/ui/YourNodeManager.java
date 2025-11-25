package com.example.ui;

import com.example.connections.ConnectionLine;
import com.example.nodes.BaseNode;

import java.util.ArrayList;
import java.util.List;

public class YourNodeManager {
    private static final List<BaseNode> allNodes = new ArrayList<>();
    private static final List<ConnectionLine> allConnections = new ArrayList<>();

    public static void register(ConnectionLine connection) {
        allConnections.add(connection);
    }

    public static void unregister(ConnectionLine connection) {
        allConnections.remove(connection);
    }

    public static void register(BaseNode node) {
        allNodes.add(node);
    }
    
    public static void unregister(BaseNode node) {
        allNodes.remove(node);
    }

    public static List<BaseNode> getAllNodes() {
        return allNodes;
    }

    public static List<ConnectionLine> getAllConnections() {
        return allConnections;
    }

    public static List<BaseNode> getSelectedNodes() {
        List<BaseNode> selectedNodes = new ArrayList<>();
        for (BaseNode node : allNodes) {
            if (node.isSelected()) {
                selectedNodes.add(node);
            }
        }
        return selectedNodes;
    }


    public static void clear() {
        allNodes.clear();
    }
}
