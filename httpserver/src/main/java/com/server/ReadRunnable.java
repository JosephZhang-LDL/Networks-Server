package com.server;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Hashtable;

public class ReadRunnable implements Runnable {
    Hashtable<String, String> fields;
    List<Byte> responseBuffer;
    SocketChannel client;
    ByteBuffer buffer;
    RequestHandler handler;
    private final Object lock;

    public ReadRunnable(Hashtable<String, String> fields,
            List<Byte> responseBuffer,
            SocketChannel client,
            ByteBuffer buffer,
            RequestHandler handler,
            Object lock) {
        this.fields = fields;
        this.responseBuffer = responseBuffer;
        this.client = client;
        this.buffer = buffer;
        this.handler = handler;
        this.lock = lock;
    }

    public void run() {
        synchronized (fields) {
            synchronized (responseBuffer) {
                try {
                    handler.readRequest(this.fields, new String(buffer.array(), "UTF-8"),
                            responseBuffer, client);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}
