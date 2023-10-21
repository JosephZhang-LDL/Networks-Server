package com.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;

public class ControlThreadHandler implements Runnable{
    private volatile boolean running;
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    private String line;
    private ServerSocket serverSocket;

    public ControlThreadHandler(ServerSocket serverSocket){
        this.serverSocket = serverSocket;
    }

    public void doWork() throws IOException{
        line = in.readLine();
        if (line.equals("shutdown")) {
            running = false;
        }
    }

    public void run() {

        try {
            running = true;
            while (running) {
                doWork();
            }
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
