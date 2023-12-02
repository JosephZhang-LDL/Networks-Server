package com.server;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
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
            ControlThreadHandler controlThreadHandler) {
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
        try {
            String request = "";
            while (true) {
                int readyCount = selector.select(3000);
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

                        request = "";
                    }

                    if (key.isReadable()) {
                        SocketChannel client = (SocketChannel) key.channel();

                        int BUFFER_SIZE = 1024;
                        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

                        try {
                            client.read(buffer);
                            buffer.flip();
                        } catch (IOException e) {
                            e.printStackTrace();
                            continue;
                        }
                        // Read into buffer
                        String t_str = new String(buffer.array(), "UTF-8");
                        for (int i=0; i<BUFFER_SIZE; i++) {
                            if ((int) t_str.charAt(i) != 0 ) {
                                request += t_str.charAt(i);
                            } else {
                                break;
                            }
                        }

                        // Create new RequestHandler and read request
                        if (request.contains("\r\n\r\n")) {
                            RequestHandler currRequest = new RequestHandler(locations, authorizationCache);
                            currRequest.readRequestNew(request, client);

                            key.attach(currRequest);

                            buffer.clear();

                            key.interestOps(SelectionKey.OP_WRITE);
                        }

                    } else if (key.isWritable()) {
                        SocketChannel client = (SocketChannel) key.channel();
                        RequestHandler res = (RequestHandler) key.attachment();

                        res.writeResponseNew(client, key);

                        if (res.getFields().containsKey("Connection") &&
                            res.getFields().get("Connection").equals("Keep Alive")) {
                                System.out.println("Keep alive");
                                key.interestOps(SelectionKey.OP_READ);
                                request = "";
                                // client.close();
                        } else {
                            key.cancel();
                            client.close();
                        }
                        //else {

                        // System.out.println("Closing connection");
                        // Sleep 500ms to allow client to receive response
                        /*
                         * try {
                         * Thread.sleep(3);
                         * } catch (InterruptedException e) {
                         * e.printStackTrace();
                         * }
                         */
                        // client.register(selector, SelectionKey.OP_CONNECT);
                        // }

                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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