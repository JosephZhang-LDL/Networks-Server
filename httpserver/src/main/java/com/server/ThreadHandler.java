package com.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadHandler implements Runnable {
    private volatile boolean running;
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    private String line;
    private ServerSocket serverSocket;
    private static final int THREAD_POOL_SIZE = 10;
    private final ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

    public ThreadHandler(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    public void submit(Runnable task) {
        threadPool.submit(task);
    }

    public void shutdown() {
        running = false;
    }

    public void doWork() throws IOException {
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
            threadPool.shutdown();
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
