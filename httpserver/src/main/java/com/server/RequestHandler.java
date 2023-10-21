package com.server;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;

import javax.imageio.ImageIO;

public class RequestHandler {
    // Class that takes in an http header and splits it up by carriage return lines

    // Initiate a hash table to store the header fields

    private Hashtable<String, String> fields = new Hashtable<String, String>();
    private Locations locations = null;

    public RequestHandler(String request, Locations locations) {
        this.locations = locations;

        String[] sections = request.split("\r\n\r\n");

        // Read each header field line into a hash table
        String[] headerLines = sections[0].split("\r\n");

        // Initial parsing of data
        for (int i = 0; i < headerLines.length; i++) {
            String line = headerLines[i];
            String[] lineSplit = line.split(":", 2);
            // If it's the method header
            if (lineSplit.length < 2) {
                lineSplit = line.split(" ");
                this.fields.put("Method", lineSplit[0]);
                this.fields.put("Path", lineSplit[1]);
                this.fields.put("Version", lineSplit[2]);
            }
            // Otherwise use header's field name
            else {
                this.fields.put(lineSplit[0].trim(), lineSplit[1].trim());
            }
        }

        // Read body into hash table
        if (sections.length > 1) {
            this.fields.put("Body", sections[1]);
        }

        // Print out the hash table
        for (String key : this.fields.keySet()) {
            System.out.println(key + ": " + this.fields.get(key));
        }
        System.out.println();
    }

    public String getMethod() {
        return this.fields.get("Method");
    }

    public byte[] getResponse() {
        if (this.getMethod().equals("GET")) {
            return this.handleGet();
        } else if (this.getMethod().equals("POST")) {
            return this.handlePost();
        }
        return "".getBytes();
    }

    public byte[] constructErrorResponse(int errorCode, String description) {
         return ("HTTP/1.1 " + errorCode + " " + description + "\r\n" +
                        "Date: " + new Date() + "\r\n" +
                        "Server: JZAS Server\r\n" +
                        "\r\n\r\n").getBytes();
    }

    public byte[] handleGet() {
        // Parse: Accept, If-Modified-Since, Authorization
        try {
            if (this.fields.get("Accept") != null) {
                String[] acceptTypes = this.fields.get("Accept").split(",");
            }
            if (this.fields.get("If-Modified-Since") != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
                Date ifModifiedSince = dateFormat.parse(this.fields.get("If-Modified-Since"));
            }
            if (this.fields.get("Authorization") != null) {
                String[] auth = this.handleAuthorization();
            }
        } catch (Exception e) {
            System.out.println("Error parsing header fields");
        }

        String hostPath = this.locations.getDefaultLocation();
        if (this.fields.get("Host") != null) {
            hostPath = this.locations.getLocation(this.fields.get("Host"));
        }

        // Construct the file path and match against Config
        Boolean found = false;
        String filePath = hostPath + this.fields.get("Path");

        System.out.println(filePath);

        // if empty path
        if (filePath.charAt(filePath.length() - 1) == '/') {
            // check for mobile
            if (this.fields.get("User-Agent").contains("iPhone") || this.fields.get("User-Agent").contains("android")) {
                File f = new File(filePath + "index_m.html");
                if(f.exists() && !f.isDirectory()) {
                    filePath = filePath + "index_m.html";
                    found = true;
                }
            }
            // mobile exclusive wasn't found / user isn't mobile
            if (!found) {
                File f = new File(filePath + "index.html");
                if (f.exists() && !f.isDirectory()) {
                    filePath = filePath + "index.html";
                }
                else {
                    return constructErrorResponse(404, "Not Found");
                }
            }
        }

        // if path tries to go out of bounds
        if (filePath.contains("..")) {
            System.out.println("Error: Invalid file path");
            return constructErrorResponse(400, "Bad Request");
        }

        File f = new File(filePath);
        String responseBody = "";
        String contentType = "";
        String responseLength = "";
        Date lastModified = new Date();
        // do file execution
        if (f.canExecute()) {
            System.out.println("what the fucks");
        }
        else if (f.exists()) {
            try {
                String[] fileParts = filePath.split("\\.");
                String extension = fileParts[fileParts.length - 1];
                if (extension.matches("jpg|jpeg|png")) {
                    contentType = "image/" + extension;

                    BufferedImage image = ImageIO.read(new FileInputStream(f));
                    System.out.println(filePath);
                    System.out.println(image == null);
                    System.out.println(f.exists());

                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    ImageIO.write(image, extension, byteArrayOutputStream);
                    byte[] imageReturn = byteArrayOutputStream.toByteArray();
                    responseLength = String.valueOf(imageReturn.length);

                    byte[] headers = ("HTTP/1.1 200 OK\r\n" +
                        "Date: " + new Date() + "\r\n" +
                        "Server: JZAS Server\r\n" +
                        "Last-Modified: " + lastModified + "\r\n" +
                        "Content-Type: " + contentType + "\r\n" +
                        "Content-Length: " + responseLength + "\r\n" +
                        "\r\n").getBytes();
                    return headers;
                    // byte[] returnArray = Arrays.copyOf(headers, headers.length + imageReturn.length);
                    // for (int i = 0; i < imageReturn.length; i++){
                    //     returnArray[i + headers.length] = imageReturn[i];
                    // }
                    // return returnArray;
                }
                else if (extension.matches("html|txt")) {
                    if (extension.equals("html")) {
                        contentType = "text/html";
                    } else {
                        contentType = "text/plain";
                    }

                    BufferedReader in = new BufferedReader(new FileReader(filePath));
                    String line;
                    while ((line = in.readLine()) != null) {
                        responseBody += line + "\r\n";
                    }
                    in.close();
                    responseLength = String.valueOf(responseBody.length());

                    return ("HTTP/1.1 200 OK\r\n" +
                        "Date: " + new Date() + "\r\n" +
                        "Server: JZAS Server\r\n" +
                        "Last-Modified: " + lastModified + "\r\n" +
                        "Content-Type: " + contentType + "\r\n" +
                        "Content-Length: " + responseLength + "\r\n" +
                        "\r\n" +
                        responseBody).getBytes();
                }
                else {
                    return constructErrorResponse(404, "Unable to read the file");
                }
            } catch (IOException e) {
                e.printStackTrace();
                return constructErrorResponse(404, "Not Found");
            }

        }
        else {
            return constructErrorResponse(404, "Not Found");
        }


        return "".getBytes();
    }

    public byte[] handlePost() {

        // Returns success only on
        return "".getBytes();
    }

    private String[] handleAuthorization() {
        // Parse the authorization header

        return new String[]{"", ""};
    }
}
