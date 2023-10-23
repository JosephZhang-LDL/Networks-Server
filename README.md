# Networks-Server
Java HTTP Server for CPSC 434/534

# How to Run
Navigate to the right directory. Assuming you're in ```/Networks-Server```:

Run
```
source activate.sh
```
to activate the server. Some of the current configurations use relative pathing from the `/Networks-Server` folder.


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

## Async I/O Using Select Structure

## Performance and Benchmarking
To benchmark our performance, we have used Apache Benchmark, as suggested. Our server performs with a transfer rate of [TRANSFER_RATE] after we remove the print to console we use for debugging.
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



## Best Throughput

## Compliance