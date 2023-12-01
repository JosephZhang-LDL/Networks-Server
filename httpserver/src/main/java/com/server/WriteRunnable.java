package com.server;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Hashtable;
import java.util.List;

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
                try {
                    client.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

}
