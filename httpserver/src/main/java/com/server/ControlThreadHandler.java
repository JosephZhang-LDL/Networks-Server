package com.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ControlThreadHandler implements Runnable {
    private volatile boolean running;
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    private String line;
    private ServerSocket serverSocket;
    private static final int THREAD_POOL_SIZE = 10;
    private final ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

    public ControlThreadHandler(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    public void submit(Runnable task) {
        threadPool.submit(task);
    }

    public void shutdown() {
        running = false;
        // Shutdown the thread pool
        threadPool.shutdown();
        try {
            // Wait for all tasks to complete or for timeout to occur
            if (!threadPool.awaitTermination(10, TimeUnit.SECONDS)) {
                // Timeout occurred before all tasks completed
                threadPool.shutdownNow();
                System.err.println("Thread pool did not terminate");
            }
            serverSocket.close();
        } catch (InterruptedException e) {
            // Cancel all running tasks
            threadPool.shutdownNow(); 
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
            this.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
