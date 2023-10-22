package com.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class SocketHandler implements Runnable {
    private Socket clientSocket;
    private Locations locations;
    private AuthorizationCache authorizationCache;

    public SocketHandler(Socket socket, Locations locations, AuthorizationCache authorizationCache) {
        this.clientSocket = socket;
        this.locations = locations;
        this.authorizationCache = authorizationCache;
    }

    public String errorResponse(Exception e) {
        return "HTTP/1.1 500 " + e.toString() + "\r\n";
    }

    public void run() {
        InputStream in = null;
        OutputStream out = null;

        try {
            clientSocket.setSoTimeout(3000);
            in = clientSocket.getInputStream();
            out = clientSocket.getOutputStream();

            // Write input stream into a byte buffer array
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] byteBuffer = new byte[1024];
            int bytesRead;
            boolean headerComplete = false;
            while ((bytesRead = in.read(byteBuffer)) != -1) {
                buffer.write(byteBuffer, 0, bytesRead);
                if (containsEndOfHeader(buffer.toByteArray())) {
                    headerComplete = true;
                    break;
                }
            }

            // If header is complete, parse it and send a response
            if (headerComplete) {
                byte[] rawHeader = buffer.toByteArray();
                String header = new String(rawHeader, "UTF-8");
                RequestHandler handler = new RequestHandler(header, locations, authorizationCache);
                byte[] responseString = handler.getResponse();
                // System.out.println(responseString);
                out.write(responseString);
            } else {
                out.write(this.errorResponse(new Exception("Incomplete header")).getBytes());
            }

        } catch (SocketTimeoutException ste) {
            System.out.println("Connection timed out, closing socket.");
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            try {
                e.printStackTrace();
                if (out != null) {
                    out.write(this.errorResponse(e).getBytes());
                }
            } catch (IOException e2) {
                e.printStackTrace();
                throw new RuntimeException(e2);
            }
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
                this.clientSocket.close();
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
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
}
