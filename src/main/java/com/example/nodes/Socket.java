package com.example.nodes;

import java.util.ArrayList;
import java.util.List;

import com.example.connections.ConnectionLine;
import com.example.ui.YourNodeManager;

import javafx.scene.shape.Circle;


public class Socket {
    private String name;
    private SocketType type;
    private int index;
    private SocketDirection direction;
    private BaseNode parentNode;
    private List<SocketType> acceptedTypes;
    private Circle renderRef;

    public Socket(String name, List<SocketType> acceptedTypes, SocketDirection direction, BaseNode parentNode) {
        //System.out.println(parentNode.inputs.size() - 1);
        //System.out.println(parentNode.outputs.size() - 1);
        this.name = name;
        this.acceptedTypes = acceptedTypes;
        this.direction = direction;
        this.parentNode = parentNode;
        if (this.direction == SocketDirection.INPUT) {
            this.index = parentNode.inputs.size(); // Default to first type for input
        } else {
            this.index = parentNode.outputs.size(); // Default to first type for output
        }
    }

    public boolean isInput() {
        return direction == SocketDirection.INPUT;
    }

    public boolean isOutput() {
        return direction == SocketDirection.OUTPUT;
    }

    public void destroyConnections() {
        List<ConnectionLine> lines = getConnectionLines();
        for (ConnectionLine line : lines) {
            line.deleteConnection();
        }
    }
    

    public boolean isConnected() {
        if (isInput()) {
            return getConnectedOutput() != null;
        } else {
            return getConnectedInputs() != null;
        }
    }

    public List<SocketType> getTypeList() {
        return acceptedTypes;
    }


    public Socket getConnectedOutput() { // Returns the output socket connected to this input socket
        if (isOutput()) {
            return null; // Inputs don't have connected inputs
        }
        for (ConnectionLine conn : YourNodeManager.getAllConnections()) {
            if (conn.getInput() == this) {
                return conn.getOutput();
            }
        }
        return null;
    }


    public List<Socket> getConnectedInputs() { // Returns a list of input sockets connected to this output socket
        List<Socket> result = new ArrayList<>();
        if (isInput()) {
            return null; // Outputs don't have connected outputs
        }
        for (ConnectionLine conn : YourNodeManager.getAllConnections()) {
            if (conn.getOutput() == this) {
                result.add(conn.getInput());
            }
        }
        return result;
    }

    public List<ConnectionLine> getConnectionLines() {
        List<ConnectionLine> result = new ArrayList<>();
        for (ConnectionLine conn : YourNodeManager.getAllConnections()) {
            if (conn.getInput() == this || conn.getOutput() == this) {
                result.add(conn);
            }
        }
        
        return result;
    }

    public List<ConnectionLine> getInputLine() { // Returns a list of lines connected to this input socket
        if (!isInput()) {
            return null; // Outputs don't have input lines
        }
        List<ConnectionLine> result = new ArrayList<>();
        for (ConnectionLine conn : YourNodeManager.getAllConnections()) {
            if (conn.getInput() == this) {
                result.add(conn);
            }
        }
        return result;
    }


    public List<ConnectionLine> getOutputLines() {
        if (!isOutput()) {
            return null; // Inputs don't have output lines
        }
        List<ConnectionLine> result = new ArrayList<>();
        for (ConnectionLine conn : YourNodeManager.getAllConnections()) {
            if (conn.getInput() == this) {
                result.add(conn);
            }
        }
        return result;
    }

    public boolean isCompatibleWith(Socket other) {
        for (SocketType type : this.acceptedTypes) {
            for (SocketType oType : other.acceptedTypes) {
                if (type == oType) {
                    return true;
                }
            }
        }
        return false;
    }



    public String getName() {
        return name;
    }

    public SocketType getType() {
        return type;
    }

    public SocketDirection getDirection() {
        return direction;
    }

    public BaseNode getParentNode() {
        return parentNode;
    }

    public void setRenderRef(Circle circle) {
        this.renderRef = circle;
    }

    public Circle getRenderRef() {
        return renderRef;
    }

    public String getFullId() {
        String direction = isInput() ? "I" : "O";
        return parentNode.getId() + "-" + direction + "-" + index;
    }

    @Override
    public String toString() {
        return getFullId();
    }

}

enum SocketType {
    DOUBLE,
    VECTOR,
    VECTOR_ARRAY,
}

enum SocketDirection {
    INPUT,
    OUTPUT
}
