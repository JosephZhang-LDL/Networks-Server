package com.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;

/**
 * Hello world!
 */
public final class App {
    private int port = -1;
    private static ConfigurationHandler configurationHandler = null;
    // empty constructor
    public App() {

    }

    public void start() throws IOException {
        assert configurationHandler != null;
        port = configurationHandler.getPort();
        assert port > 0;

        // set up virtual host locations
        Locations locations = new Locations(configurationHandler.getVirtualHosts());

        // Set up authorization cache
        AuthorizationCache authorizationCache = new AuthorizationCache();


        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);

        InetSocketAddress hostAddress = new InetSocketAddress(port);
        serverChannel.bind(hostAddress);


        System.out.println("Listening for connection on port " + Integer.toString(port) + "...");

        // master control thread: will throw SocketException
        ControlThreadHandler controlThreadHandler = new ControlThreadHandler(serverChannel);
        Thread controlThread = new Thread(controlThreadHandler);
        controlThread.start();

        for (int i=0; i < configurationHandler.getNSelectLoops(); i++) {
            Selector selector = Selector.open();
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            SocketHandler handler = new SocketHandler(selector, locations, authorizationCache, controlThreadHandler);
            controlThreadHandler.submit(handler);
        }
    }

    public static void parseArgs(String[] args) {
        if (args.length > 0) {
            if (args[0].equals("-config") && args.length > 1){
                configurationHandler = new ConfigurationHandler(args[1]);
                configurationHandler.parseConfigFile();
            }
        }
    }

    public static void main(String[] args) throws IOException{
        parseArgs(args);

        App server = new App();
        server.start();
    }
}