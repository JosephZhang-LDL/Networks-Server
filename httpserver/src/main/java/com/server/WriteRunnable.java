package com.server;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Hashtable;

public class WriteRunnable implements Runnable {
    Hashtable<String, String> fields;
    List<Byte> responseBuffer;
    SocketChannel client;
    RequestHandler handler;
    SelectionKey key;

    public WriteRunnable(Hashtable<String, String> fields,
            List<Byte> responseBuffer,
            SocketChannel client,
            RequestHandler handler,
            SelectionKey key) {
        this.fields = fields;
        this.responseBuffer = responseBuffer;
        this.client = client;
        this.handler = handler;
        this.key = key;
    }

    public void run() {
        synchronized (fields) {
            synchronized (responseBuffer) {
                handler.writeResponse(fields, fields.get("Method"), responseBuffer, client, key);

                responseBuffer.clear();
                fields.clear();
            }
        }

    }

}
