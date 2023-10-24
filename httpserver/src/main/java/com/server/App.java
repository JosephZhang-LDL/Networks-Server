package com.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Set;
import java.util.Iterator;

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

        Selector selector = Selector.open();

        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);

        InetSocketAddress hostAddress = new InetSocketAddress(port);
        serverChannel.bind(hostAddress);

        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("Listening for connection on port " + Integer.toString(port) + "...");



        // master control thread: will throw SocketException
        ControlThreadHandler controlThreadHandler = new ControlThreadHandler(serverChannel);
        Thread controlThread = new Thread(controlThreadHandler);
        controlThread.start();

        RequestHandler requestHandler = new RequestHandler(locations, authorizationCache);

        SocketHandler handler = new SocketHandler(selector, locations, authorizationCache, requestHandler, controlThreadHandler);

        controlThreadHandler.submit(handler);

        // SocketHandler[] socketHandlerArray = new SocketHandler[nSelectLoops];
        // for (int i=0; i < nSelectLoops; i++){
        //     SocketHandler handler = new SocketHandler(selector, locations, authorizationCache, requestHandler);
        //     socketHandlerArray[i] = handler;
        //     controlThread.submit(handler);
        // }


        // controlThreadHandler.submit(handler);







        // while (true) {
        //     int readyCount = selector.select();
        //     if (readyCount == 0) {
        //         continue;
        //     }

        //     Set<SelectionKey> readyKeys = selector.selectedKeys();
        //     Iterator<SelectionKey> iterator = readyKeys.iterator();

        //     while(iterator.hasNext()) {
        //         SelectionKey key = iterator.next();

        //         iterator.remove();


        //     }
        // }


        // ServerSocket serverSocket;
        // Socket clientSocket;

        // // sanity check: configuration handler should be fully populated
        // assert configurationHandler != null;
        // port = configurationHandler.getPort();
        // assert port > 0;

        // // set up server
        // serverSocket = new ServerSocket(port);
        // System.out.println("Listening for connection on port " + Integer.toString(port) + "...");

        // // set up virtual host locations
        // Locations locations = new Locations(configurationHandler.getVirtualHosts());

        // // Set up authorization cache
        // AuthorizationCache authorizationCache = new AuthorizationCache();

        // // master control thread: will throw SocketException
        // ControlThreadHandler controlThreadHandler = new ControlThreadHandler(serverSocket);
        // Thread controlThread = new Thread(controlThreadHandler);
        // controlThread.start();

        // try {
        //     while ((clientSocket = serverSocket.accept()) != null) {
        //         System.out.println("Received connection from " + clientSocket.getRemoteSocketAddress().toString());
        //         SocketHandler handler = new SocketHandler(clientSocket, locations, authorizationCache);
        //         // Submit the handler to the thread pool
        //         controlThreadHandler.submit(handler);
        //     }
        // } catch (SocketException e) {
        //     System.out.println("Shutting Down");
        // } finally {
        //     if (serverSocket != null) {
        //         controlThreadHandler.shutdown();
        //     }
        // }
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
