# Networks-Server
Java HTTP Server for CPSC 434/534

# How to Run
Navigate to the right directory. Assuming you're in ```/Networks-Server```:

Run
```
source activate.sh
```
to activate the server. Some of the current configurations use relative pathing from the `/Networks-Server` folder.

# Directory Structure

## Files
```
.
├── httpserver/
│   └── src/main/java/com/server/
│       ├── App.java
│       ├── AuthorizationCache.java
│       ├── ConfigurationHandler.java
│       ├── ControlThreadHandler.java
│       ├── Locations.java
│       ├── ReadRunnable.java
│       ├── ServerCache.java
│       ├── SocketHandler.java
│       ├── WriteRunnable.java
│       └── config.txt
└── SampleSite/
    ├── books/
    │   ├── .htaccess
    │   ├── index.html
    │   ├── index_m.html
    │   └── the-stand.html
    ├── cgi/
    │   ├── cgi_test.pl
    │   └── priceraw.cgi
    ├── goof.txt
    ├── image.png
    ├── index.html
    └── index2.txt
```
## File breakdowns
* `App` : We initialize most handlers and run the server from App
* `AuthorizationCache` : Stores the credentials and last modified time for `.htaccess` files for specific directories. A cache miss happens when either the htaccess credentials weren't saved or an `.htaccess` was modified after its associated cache entry.
* `ConfigurationHandler` : Reads in data about the config file and processes it
* `ControlThreadHandler` : Handles the main control thread with graceful shutdown, as well as an associated threadpool that handle read and write requests.
* `Locations` : Hashes config data for virtual hosts
* `ReadRunnable` : Runnable task for inputs (reading headers, processing files, creating a response) from client sockets
* `WriteRunnable` : Writable task for outputs (handles content-length responses and chunked encoding responses) to client sockets
* `ServerCache` : LRU Cache for matching requests against responses
* `SocketHandler` : Handles select loops to listen for client sockets that are ready to read and write, and then multiplexes i/o to the thread pool

# Netty Comparison

## a)
Netty uses two EventLoopGroups - the boss and the worker. The boss accepts new connections and hands them off to the worker. The worker handles all the IO for those connections - the actual reading and writing of data.

Generally you only need a small number of threads for the boss since accepting connections isn't CPU intensive. But you need more threads for the worker to handle the heavier IO load. However, what makes Netty interesting is that it achieves synchronization by having each channel handled by a single thread. This avoids needing locks or synchronization for per-channel state.

But you still need to handle shared state and resources carefully, since they could be accessed across different threads. And any tasks offloaded from the event loop also require synchronization.

## b)
The ioRatio in Netty controls how the EventLoop divides its time between IO tasks and non-IO tasks. It's a value from 1 to 100 that represents a percentage.

For example, an ioRatio of 70 means the EventLoop will try to spend 70% of its time doing IO, like reading or writing to channels. The remaining 30% will be spent on non-IO tasks like running scheduled jobs.

When the EventLoop runs, it first processes IO tasks up to the ioRatio limit. So if there are 100 tasks queued and ioRatio is 70, it will do 70 IO tasks then move to non-IO. This ensures a fair balance between the two task types.

The default is 50 - meaning it tries to divide time equally. But you can tune ioRatio up or down to favor one task type if needed. Like lowering it if you have heavy processing after reading data from a channel.

## c)
Netty's ChannelPipeline chains ChannelHandlers to process data flowing through a connection, which represents the data workflow for a channel. Each Channel has its own pipeline of handlers that can transform inbound or outbound data. Events propagate from start to end inbound, and vice versa outbound. Handlers use their ChannelHandlerContext to interact with the pipeline, like writing data or modifying handlers.

A key feature about the pipeline is that it can be dynamically modifiable at runtime - handlers can be added, removed, replaced on the fly. Handlers also control event propagation, choosing whether to pass events down the pipeline. This is important because it makes the pipeline very flexible at processing data flow.

For example, the provided HTTP HelloWorld server has one handler to read requests and write "Hello World" responses. On the other hand, the other provided example for HTTP Snoop Server has two handlers that process for both the client and server. The server handler inspect and analyze request details like headers to process and build a response, while the client handler will print out status, version, and content for the response.

## d)

Netty's ChannelFuture.sync() blocks until the async operation it represents completes. It does this by:
1) Checking if the operation already finished when sync() is called. If so, return immediately.
2) Otherwise, block the thread until notified of completion.
3) Handle interrupts properly while waiting.

One might implement this in a sync method of a future by using a lock object for mutual exclusion or synchronized blocks to control access. So sync() synchronizes on a lock, waiting if needed, then returns once operationComplete becomes true.This would allow Netty methods to return a ChannelFuture immediately, while sync() provides a blocking wait on it. The synchronization makes async I/O feel blocking to the caller.

## e)

A key difference between ByteBuffer and ByteBuf is how they handle read/write operations. ByteBuffer uses a single position marker, requiring flip() to switch between reading and writing, which can be very inconvenient. ByteBuf simplifies this by having separate readerIndex and writerIndex markers. This enables reading and writing independently without flipping.

# Design Discussion

## Cache Design
We implemented caching for our GET requests. To do so, we built a LRU cache with an initial capacity set to [INITIAL_CAPACITY]. When new requests come in, we hash them against the requests in the LRU cache to see if we already have a response string. If found, we check to see if the file has been modified since when the request was added into the LRU cache, and if it has not been modified, we return the cached response. We cache by [header?] in order to ensure that the response respects the appropriate headers, such as Accept. We wrap the logic within a synchronized this block to avoid issues with multiple threads in our thread pool attempting to change the cache at the same time.

## Chunked Response
To create a chunked response, we first start the ```ProcessBuilder``` and ```Process``` normally as if the request did not have ```"Transfer-Encoding: chunked"```.  Then, instead of waiting for the program to completely finish, we send each read line of output generated from the ```Process``` as it is processed via chunking, with the length of the message being the length of the current output line. Finally, after all output has been read, we send the 0 length chunk to show that the transmission is complete. We have tested this functionality using the appropriate headers, and the transmission is read by curl with no issues.

## Heartbeat
To use our heartbeat functionality, please navigate to:
```
http://localhost:8080/heartbeat
```
Any client can ping this URI which will return a 200 signal if all server functionality is operational, or a 500 if one or more of the services is down. To do so, the heartbeat function tests our two main supported services: GET and POST. The heartbeat function generates new requests using default URIs (GET index.html and POST cgi/default.cgi?param1=value1), and if both requests return 200s, the service returns 200.

## Thread Pool Structure

We use ExecutorService to maintain a threadpool for I/O tasks. We maintain the threadpool inside our `ControlThreadHandler`, where we can submit read or write tasks. In our current design, we maintain a pool of 10 threads that are picked based on Java's ExectuorService class to run our runnables.

## Async I/O Using Select Structure
We currently have a select structure set up where the Selector will poll for keys. If a key is acceptable, it will connect to the server and be marked as readable. Then, if a key is readable, a new thread from the threadpool would take on the task of reading it before marking the key as writable. In this time, the async runnable task will be locked using synchronized over the fields the runnable is writing into. Finally, if a key is readable, a new thread from the threadpool would output in the same way before checking the Connection header to see if the socket should be closed or returned to a connect state.

## Performance and Benchmarking
To benchmark our performance, we have used Apache Benchmark, as suggested. Our server performs with a transfer rate of 1366 Kbytes/sec after we remove the print to console we use for debugging.
To test our performance, we tested primarily using POSTMAN, but also a suite of curl requests:
We test our GET method in ways including testing:
    ../ outside of the stack.
    Headers indicating mobile/desktop using iPhone, Android, Chrome etc.
    the benchmarking with and without cache.
    different last modified dates.
    an executable .pl and .cgi.
    an invalid combination of headers indicating "Content-Type" and data.
    missing "Content-Type."
    missing files.
...


## Best Throughput
On a randomly chosen test, our server's 1366 Kbytes/sec surpasses the 10Mbps benchmark.

## Compliance
We comply with HTTP 1.1 by using HTTP 1.1's standards. For instance, connections are assume to be closed unless keep-alive is clearly indicated.
In addition, we ensured that our responses could be parsed using curl's 1.1 setting instead of the higher level HTTPs.