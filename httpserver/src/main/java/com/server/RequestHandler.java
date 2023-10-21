package com.server;

import java.util.Date;
import java.util.Hashtable;

public class RequestHandler {
    // Class that takes in an http header and splits it up by carriage return lines

    // Initiate a hash table to store the header fields

    private Hashtable<String, String> headerFields = new Hashtable<String, String>();

    public RequestHandler(String header) {
        // Reach each header field line into a hash table
        String[] headerLines = header.split("\r\n");

        // Initial parsing of data
        for (int i = 0; i < headerLines.length; i++) {
            String line = headerLines[i];
            String[] lineSplit = line.split(":", 2);
            // If it's the method header
            if (lineSplit.length < 2) {
                lineSplit = line.split(" ");
                this.headerFields.put("Method", lineSplit[0]);
                this.headerFields.put("Path", lineSplit[1]);
                this.headerFields.put("Version", lineSplit[2]);
            }
            // Otherwise use header's field name
            else {
                this.headerFields.put(lineSplit[0], lineSplit[1]);
            }
        }

        // Parse Individual elements
        // if accept header

        // Print out the hash table
        for (String key : this.headerFields.keySet()) {
            System.out.println(key + ": " + this.headerFields.get(key));
        }
    }

    public String getMethod() {
        return this.headerFields.get("Method");
    }

    public String getResponse() {
        if (this.getMethod().equals("GET")) {
            return this.handleGet();
        } else if (this.getMethod().equals("POST")) {
            return this.handlePost();
        }
        return "";
    }

    public String handleGet() {

        
        // Parse: Accept, If-Modified-Since, Authorization
        String[] acceptTypes = this.headerFields.get("Accept").split(",");
        /* try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
            Date ifModifiedSince = dateFormat.parse(this.headerFields.get("If-Modified-Since"));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        String[] auth = this.handleAuthorization(); */
        

        // On Success
        String responseBody = "This is the response body.";
        Date lastModified = new Date();
        String contentType = "text/plain";
        String response = "HTTP/1.1 200 OK\r\n" +
                "Date: " + new Date() + "\r\n" +
                "Server: JZAS Server\r\n" +
                "Last-Modified: " + lastModified + "\r\n" +
                "Content-Type: " + contentType + "\r\n" +
                "Content-Length: " + responseBody.length() + "\r\n" +
                "\r\n" +
                responseBody;


        // Returns resource or 404 "NOT FOUND"
        System.out.println(response);
        return response;
    }

    public String handlePost() {

        // Returns success only on
        return "";
    }

    private String[] handleAuthorization() {
        // Parse the authorization header

        return new String[]{"", ""};
    }
}
