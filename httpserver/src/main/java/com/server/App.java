package com.server;

/**
 * Hello world!
 */
public final class App {
    private App() {
    }

    public static void main(String[] args) {
        String sampleHeader = "GET / HTTP/1.1\r\n" +
        "Host: www.example.com\r\n" +
        "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36\r\n" +
        "Accept: text/html,application/xhtml+xml\r\n" +
        "Connection: keep-alive\r\n" +
        "If-Modified-Since: Mon, 26 Jul 1997 05:00:00 GMT\r\n" +
        "Authorization: Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==\r\n" +
        "Content-Length: 13\r\n" +
        "Content-Type: application/x-www-form-urlencoded\r\n";

        RequestHandler handler = new RequestHandler(sampleHeader);
    }
}
