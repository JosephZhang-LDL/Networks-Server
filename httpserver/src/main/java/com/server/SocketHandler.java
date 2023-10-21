package com.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

public class SocketHandler implements Runnable {
    private Socket clientSocket;

    public SocketHandler(Socket socket) {
        this.clientSocket = socket;
    }

    public String errorResponse(Exception e) {
        return "HTTP/1.1 500 " + e.toString() + "\r\n";
    }

    public void run() {
        BufferedReader in = null;
        OutputStream out = null;

        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = this.clientSocket.getOutputStream();
            String header = "";
            String line = in.readLine();
            while(!line.isEmpty()) {
                header = header + line + "\r\n";
                line = in.readLine();
            }
            RequestHandler handler = new RequestHandler(header);
            // We should add: if unable to parse, we should return
            String responseString = handler.getResponse();
            out.write(responseString.getBytes());
        } catch (IOException e) {
            try {
                e.printStackTrace();
                if (out != null) {
                    out.write(this.errorResponse(e).getBytes());
                }
            }
            catch (IOException e2) {
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
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
