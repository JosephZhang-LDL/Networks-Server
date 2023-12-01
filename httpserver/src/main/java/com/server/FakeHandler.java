package com.server;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class FakeHandler implements Runnable {
    private Selector selector;

	public FakeHandler() {
		// create selector
		try {
			selector = Selector.open();
		} catch (IOException ex) {
			System.out.println("Cannot create selector.");
			ex.printStackTrace();
			System.exit(1);
		} // end of catch
	} // end of Dispatcher

	public Selector selector() {
		return selector;
	}

	public void run() {
        while (true) {
            try {
                // check to see if any events
				selector.select();
                System.out.println("Hello");
			} catch (IOException ex) {
				ex.printStackTrace();
				break;
			}

			// readKeys is a set of ready events
			Set<SelectionKey> readyKeys = selector.selectedKeys();

			// create an iterator for the set
			Iterator<SelectionKey> iterator = readyKeys.iterator();

			// iterate over all events
			while (iterator.hasNext()) {

                SelectionKey key = (SelectionKey) iterator.next();
				iterator.remove();

				try {
					if (key.isAcceptable()) { // a new connection is ready to be
												// accepted
                        ServerSocketChannel server = (ServerSocketChannel) key.channel();

                        SocketChannel client = server.accept();
                        client.configureBlocking(false);
                        client.register(selector, SelectionKey.OP_READ);
					} // end of isAcceptable

					if (key.isReadable() || key.isWritable()) {
						if (key.isReadable()) {
                            System.out.println("READABLE");
                            SocketChannel client = (SocketChannel) key.channel();
                            String hello = new String("Hello World!");
							key.attach(hello);
                            client.register(selector, SelectionKey.OP_WRITE);
						} // end of if isReadable

						if (key.isWritable()) {
                            SocketChannel client = (SocketChannel) key.channel();
                            System.out.println("WRITABLE");
                            try {
                                String hello = (String) key.attachment();
                                System.out.println("HELLO IS: " + hello);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            client.close();
						} // end of if isWritable
					} // end of readwrite
				} catch (IOException ex) {
					key.cancel();
					try {
						key.channel().close();
						// in a more general design, call have a handleException
					} catch (IOException cex) {
					}
				} // end of catch

			} // end of while (iterator.hasNext()) {

		} // end of while (true)
	} // end of run
}
