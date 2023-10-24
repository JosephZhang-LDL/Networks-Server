package com.server;

import java.nio.channels.SocketChannel;
import java.util.Hashtable;
import java.util.List;

public class WriteRunnable implements Runnable{
    Hashtable<String, String> fields;
    List<Byte> responseBuffer;
    SocketChannel client;
    RequestHandler handler;

    public WriteRunnable(Hashtable<String, String> fields,
    List<Byte> responseBuffer,
    SocketChannel client,
    RequestHandler handler){
        this.fields = fields;
        this.responseBuffer = responseBuffer;
        this.client = client;
        this.handler = handler;
    }

    public void run() {
        handler.writeResponse(fields, fields.get("Method"), responseBuffer, client);
    }

}
