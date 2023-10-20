package com.server;

import java.net.*;

import org.omg.CORBA.Request;

import java.io.*;

/**
 * Hello world!
 */
public final class App {
    private App() {
    }


    public static void main(String[] args) {
        ServerSocket serverSocket;
        Socket clientSocket;
        BufferedReader in;
        OutputStream out;
        int port = 8080;

        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Listening for connection on port " + Integer.toString(port) + "...");
            while (true) {
                clientSocket = serverSocket.accept();
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = clientSocket.getOutputStream();
                String header = "";
                String line = in.readLine();
                while(!line.isEmpty()) {
                    header = header + line + "\r\n";
                    // clientSocket.getOutputStream().write(line.getBytes("UTF-8"));
                    line = in.readLine();
                }
                RequestHandler handler = new RequestHandler(header);
                String responseString = handler.getResponse();
                System.out.println(responseString);
                out.write(responseString.getBytes());

                in.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}


// String sampleHeader = "GET / HTTP/1.1\r\n" +
//         "Host: www.example.com\r\n" +
//         "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36\r\n" +
//         "Accept: text/html,application/xhtml+xml\r\n" +
//         "Connection: keep-alive\r\n" +
//         "If-Modified-Since: Mon, 26 Jul 1997 05:00:00 GMT\r\n" +
//         "Authorization: Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==\r\n" +
//         "Content-Length: 13\r\n" +
//         "Content-Type: application/x-www-form-urlencoded\r\n";
