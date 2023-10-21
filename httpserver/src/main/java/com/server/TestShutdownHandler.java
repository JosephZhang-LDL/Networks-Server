package com.server;

public class TestShutdownHandler implements Runnable{
    public void run() {
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            // TODO: handle exception
        }
    }
}
