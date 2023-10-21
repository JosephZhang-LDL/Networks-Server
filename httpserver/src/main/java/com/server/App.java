package com.server;

import java.net.*;

import java.io.*;

/**
 * Hello world!
 */
public final class App {
    private int port;

    public App(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        ServerSocket serverSocket;
        Socket clientSocket;
        serverSocket = new ServerSocket(port);
        System.out.println("Listening for connection on port " + Integer.toString(port) + "...");

        ControlThreadHandler controlThreadHandler = new ControlThreadHandler(serverSocket);
        Thread controlThread = new Thread(controlThreadHandler);
        controlThread.start();

        try {
            while ((clientSocket = serverSocket.accept()) != null) {
                System.out.println("Received connection from " + clientSocket.getRemoteSocketAddress().toString());
                SocketHandler handler = new SocketHandler(clientSocket);
                Thread t = new Thread(handler);
                t.start();
            }
        } catch (SocketException e) {
            System.out.println("Shutting Down");
        } finally {
            if (serverSocket != null) {
                serverSocket.close();
            }
        }
    }

    public static void main(String[] args) throws IOException{
        App server = new App(8080);
        server.start();
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



        // testing whether or not running threads would terminate before they finish
        /*
        TestShutdownHandler testThreadShutdownHandler = new TestShutdownHandler();
        Thread testShutdown= new Thread(testThreadShutdownHandler);
        testShutdown.start();

        System.out.println(testShutdown.isAlive());

        */
