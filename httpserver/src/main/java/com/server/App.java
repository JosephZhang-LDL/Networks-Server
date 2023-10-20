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
        "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8\r\n" +
        "Accept-Encoding: gzip, deflate, br\r\n" +
        "Accept-Language: en-US,en;q=0.5\r\n" +
        "Cache-Control: no-cache\r\n" +
        "Connection: keep-alive\r\n" +
        "Cookie: JSESSIONID=abc123\r\n" +
        "Pragma: no-cache\r\n" +
        "Referer: http://www.example.com/index.html\r\n" +
        "Upgrade-Insecure-Requests: 1\r\n" +
        "User-Agent: curl/7.64.1\r\n" +
        "Authorization: Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==\r\n";
        RequestHandler handler = new RequestHandler(sampleHeader);
        
        System.out.println("Hello World!");
    }
}
