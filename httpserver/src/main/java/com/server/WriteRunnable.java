package com.server;

import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Hashtable;

public class WriteRunnable implements Runnable {
    Hashtable<String, String> fields;
    List<Byte> responseBuffer;
    SocketChannel client;
    RequestHandler handler;
    private final Object lock;

    public WriteRunnable(Hashtable<String, String> fields,
            List<Byte> responseBuffer,
            SocketChannel client,
            RequestHandler handler,
            Object lock) {
        this.fields = fields;
        this.responseBuffer = responseBuffer;
        this.client = client;
        this.handler = handler;
        this.lock = lock;
    }

    public void run() {
        synchronized (lock) {
            System.out.println(fields.toString());
            handler.writeResponse(fields, fields.get("Method"), responseBuffer, client);
        }

    }

}
