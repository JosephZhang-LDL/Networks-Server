package com.server;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Hashtable;
import java.util.List;

public class ReadRunnable implements Runnable{
    Hashtable<String, String> fields;
    List<Byte> responseBuffer;
    SocketChannel client;
    ByteBuffer buffer;
    RequestHandler handler;

    public ReadRunnable(Hashtable<String, String> fields,
    List<Byte> responseBuffer,
    SocketChannel client,
    ByteBuffer buffer,
    RequestHandler handler) {
        this.fields = fields;
        this.responseBuffer = responseBuffer;
        this.client = client;
        this.buffer = buffer;
        this.handler = handler;
    }

    public void run() {
        try {
            handler.readRequest(fields, new String(buffer.array(), "UTF-8"),
                                    responseBuffer, client);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }
}
