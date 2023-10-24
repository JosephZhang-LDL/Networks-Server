package com.server;

import java.util.List;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

public class SocketHandler implements Runnable {
    private Socket clientSocket;
    private Locations locations;
    private AuthorizationCache authorizationCache;
    private Selector selector;
    private RequestHandler handler;
    private ControlThreadHandler threadpool;

    public SocketHandler(Selector selector, Locations locations, AuthorizationCache authorizationCache,
            RequestHandler handler, ControlThreadHandler controlThreadHandler) {
        this.selector = selector;
        this.locations = locations;
        this.authorizationCache = authorizationCache;
        this.handler = handler;
        this.threadpool = controlThreadHandler;
    }

    public String errorResponse(Exception e) {
        return "HTTP/1.1 500 " + e.toString() + "\r\n";
    }

    public void run() {
        Hashtable<String, String> fields = new Hashtable<String, String>();
        List<Byte> responseBuffer = new ArrayList<Byte>();
        final Object lock = new Object();

        try {
            while (true) {
                int readyCount = selector.select();
                if (readyCount == 0) {
                    continue;
                }

                Set<SelectionKey> readyKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = readyKeys.iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();

                    iterator.remove();

                    if (key.isAcceptable()) {
                        ServerSocketChannel server = (ServerSocketChannel) key.channel();
                        SocketChannel client = server.accept();
                        client.configureBlocking(false);
                        client.register(selector, SelectionKey.OP_READ);
                        // client.register(selector, SelectionKey.OP_WRITE);
                    }

                    if (key.isReadable()) {

                        SocketChannel client = (SocketChannel) key.channel();

                        int BUFFER_SIZE = 1024;
                        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

                        try {
                            client.read(buffer);

                            // handler.readRequest(fields, new String(buffer.array(), "UTF-8"),
                            // responseBuffer, client);
                            ReadRunnable readRunnable = new ReadRunnable(fields, responseBuffer, client, buffer,
                                    handler, lock);
                            threadpool.submit(readRunnable);

                            buffer.clear();

                        } catch (IOException e) {
                            e.printStackTrace();
                            continue;
                        }
                        client.register(selector, SelectionKey.OP_WRITE);
                    }

                    if (key.isWritable()) {
                        SocketChannel client = (SocketChannel) key.channel();

                        // handler.writeResponse(fields, fields.get("Method"), responseBuffer, client);
                        WriteRunnable writeRunnable = new WriteRunnable(fields, responseBuffer, client, handler, lock);
                        threadpool.submit(writeRunnable);

                        // if (fields.containsKey("Connection") && fields.get("Connection").equals("close")) {
                            // client.close();
                        // } else {
                            // client.register(selector, SelectionKey.OP_CONNECT);
                        // }
                        client.close();

                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // InputStream in = null;
        // OutputStream out = null;
        // System.out.println(Thread.currentThread().getName());

        // try {
        // clientSocket.setSoTimeout(3000);
        // in = clientSocket.getInputStream();
        // out = clientSocket.getOutputStream();

        // // Write input stream into a byte buffer array
        // ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        // byte[] byteBuffer = new byte[1024];
        // int bytesRead;
        // boolean headerComplete = false;
        // while ((bytesRead = in.read(byteBuffer)) != -1) {
        // buffer.write(byteBuffer, 0, bytesRead);
        // if (containsEndOfHeader(buffer.toByteArray())) {
        // headerComplete = true;
        // break;
        // }
        // }

        // // If header is complete, parse it and send a response
        // if (headerComplete) {
        // byte[] rawHeader = buffer.toByteArray();
        // String header = new String(rawHeader, "UTF-8");
        // RequestHandler handler = new RequestHandler(header, locations,
        // authorizationCache, clientSocket, out);
        // byte[] responseString = handler.getResponse();
        // out.write(responseString);
        // } else {
        // out.write(this.errorResponse(new Exception("Incomplete header")).getBytes());
        // }

        // } catch (SocketTimeoutException ste) {
        // System.out.println("Connection timed out, closing socket.");
        // try {
        // clientSocket.close();
        // } catch (IOException e) {
        // e.printStackTrace();
        // }
        // } catch (IOException e) {
        // try {
        // e.printStackTrace();
        // if (out != null) {
        // out.write(this.errorResponse(e).getBytes());
        // }
        // } catch (IOException e2) {
        // e.printStackTrace();
        // throw new RuntimeException(e2);
        // }
        // } finally {
        // try {
        // if (out != null) {
        // out.close();
        // }
        // if (in != null) {
        // in.close();
        // }
        // this.clientSocket.close();
        // return;
        // } catch (IOException e) {
        // e.printStackTrace();
        // }
        // }
    }

    private boolean containsEndOfHeader(byte[] data) {
        byte[] CRLFCRLF = { 0x0D, 0x0A, 0x0D, 0x0A }; // represents '\r\n\r\n'
        byte[] LFLF = { 0x0A, 0x0A }; // represents '\n\n'

        return containsBytes(data, CRLFCRLF) || containsBytes(data, LFLF);
    }

    private boolean containsBytes(byte[] array, byte[] target) {
        for (int i = 0; i < array.length - target.length + 1; i++) {
            boolean found = true;
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    found = false;
                    break;
                }
            }
            if (found)
                return true;
        }
        return false;
    }

    public void write(byte[] response) throws IOException {
        OutputStream out = clientSocket.getOutputStream();
        out.write(response);
    }

    public String getClientAddress() {
        return clientSocket.getInetAddress().toString().replace("/", "");
    }

    public String getClientHost() {
        return clientSocket.getInetAddress().getCanonicalHostName();
    }
}
