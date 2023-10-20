package com.server;

import java.util.Hashtable;

public class RequestHandler {
    // Class that takes in an http header and splits it up by carriage return lines

    // Initiate a hash table to store the header fields

    private Hashtable<String, String> headerFields = new Hashtable<String, String>();

    public RequestHandler(String header) {
        // Reach each header field line into a hash table
        String[] headerLines = header.split("\r\n");
        for (String line : headerLines) {
            String[] lineSplit = line.split(":", 2);
            // If it's the method header
            if (lineSplit.length < 2) {
                lineSplit = line.split(" ");
                this.headerFields.put("METHOD", lineSplit[0]);
                this.headerFields.put("PATH", lineSplit[1]);
                this.headerFields.put("VERSION", lineSplit[2]);
            }
            // Otherwise use header's field name 
            else {
                this.headerFields.put(lineSplit[0], lineSplit[1]);
            }
        }

        // Print out the hash table
        for (String key : this.headerFields.keySet()) {
            System.out.println(key + ": " + this.headerFields.get(key));
        }
    }
}
