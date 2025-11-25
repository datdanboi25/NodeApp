package com.example.connections;

import com.example.nodes.Socket;

public class Connection {
    private Socket a;
    private Socket b;

    public Connection(Socket a, Socket b) {
        this.a = a;
        this.b = b;
    }

    public Socket getOther(Socket s) {
        return s == a ? b : a;
    }

    public Socket getA() { return a; }
    public Socket getB() { return b; }
}
