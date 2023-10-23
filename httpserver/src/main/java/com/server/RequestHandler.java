package com.server;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map;
import java.util.Stack;

import javax.imageio.ImageIO;

public class RequestHandler {
    // Class that takes in an http header and splits it up by carriage return lines

    // Initiate a hash table to store the header fields

    private Hashtable<String, String> fields = new Hashtable<String, String>();
    private AuthorizationCache authCache = new AuthorizationCache();
    private Locations locations = null;
    private Socket clientSocket;
    private OutputStream out;

    public RequestHandler(String request, Locations locations, AuthorizationCache authCache, Socket clientSocket, OutputStream out) {
        this.authCache = authCache;
        this.locations = locations;
        this.clientSocket = clientSocket;
        this.out = out;

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

    // Normal Error Response
    public byte[] constructErrorResponse(int errorCode, String description) {
        return ("HTTP/1.1 " + errorCode + " " + description + "\r\n" +
                "Date: " + new Date() + "\r\n" +
                "Server: JZAS Server\r\n" +
                "\r\n\r\n").getBytes();
    }

    // Error Response with children
    public byte[] constructErrorResponse(int errorCode, String description, String children) {
        return ("HTTP/1.1 " + errorCode + " " + description + "\r\n" +
                "Date: " + new Date() + "\r\n" +
                "Server: JZAS Server\r\n" +
                children + "\r\n" +
                "\r\n\r\n").getBytes();
    }

    public byte[] handleGet() {
        String hostPath = this.locations.getDefaultLocation();
        if (this.fields.get("Host") != null) {
            hostPath = this.locations.getLocation(this.fields.get("Host"));
        }

        // Construct the file path and match against Config
        Boolean found = false;
        String filePath = hostPath + this.fields.get("Path");

        // BAD FILE CHECK: if path tries to go out of bounds
        if (filePath.contains("..")) {
            String resolvedRelativePath = this.createPath(this.fields.get("Path"));
            if (resolvedRelativePath == null) {
                System.out.println("Error: Invalid file path");
                return constructErrorResponse(400, "Bad Request");
            }
            filePath = hostPath + resolvedRelativePath;
            System.out.println("=====NEW PATH IS:" + filePath);
        }

        // if empty path
        if (filePath.charAt(filePath.length() - 1) == '/') {
            // check for mobile
            if (this.fields.get("User-Agent").contains("iPhone") || this.fields.get("User-Agent").contains("android")) {
                File f = new File(filePath + "index_m.html");
                if (f.exists() && !f.isDirectory()) {
                    filePath = filePath + "index_m.html";
                    found = true;
                }
            }
            // mobile exclusive wasn't found / user isn't mobile
            if (!found) {
                File f = new File(filePath + "index.html");
                if (f.exists() && !f.isDirectory()) {
                    filePath = filePath + "index.html";
                } else if (f.exists() && f.isDirectory()) {
                    filePath = filePath + "/index.html";
                } else {
                    return constructErrorResponse(404, "Not Found");
                }
            }
        }

        File f = new File(filePath);
        String responseBody = "";
        String contentType = "";
        String responseLength = "";
        Date lastModified = new Date();
        String[] fileParts = filePath.split("\\.");
        String extension = fileParts[fileParts.length - 1];
        // do file execution
        if (f.canExecute() || extension.equals("cgi|pl")) {
            String queryString = "";
            if (this.fields.get("Content-Type").equals("application/x-www-form-urlencoded")) {
                queryString = this.fields.get("Body");
            } else if (this.fields.get("Content-Type").equals("application/json")) {
                String[] parameters = this.fields.get("Body").replace("{", "").replace("}", "").replace("\"", "").split(",");
                for (String parameterPair : parameters) {
                    try {
                        String key = parameterPair.split(":")[0].trim();
                        String value = parameterPair.split(":")[1].trim();
                        if (queryString.equals("")) {
                            queryString = key + "=" + value;
                        } else {
                            queryString += "&" + key + "=" + value;
                        }
                    } catch (ArrayIndexOutOfBoundsException e) {
                        e.printStackTrace();
                        return constructErrorResponse(400, "Bad Request");
                    }
                }
            } else {
                System.out.println("Content-Type not recognized.");
                return constructErrorResponse(415, "Unsupported Media Type");
            }

            try {
                BufferedReader in = new BufferedReader(new FileReader(filePath));
                String line = in.readLine();
                in.close();
                String[] lineParts = line.split(" ");
                lineParts[0] = lineParts[0].replace("#!", "");
                String[] inputParts = Arrays.copyOf(lineParts, lineParts.length + 1);
                inputParts[inputParts.length - 1] = filePath;

                ProcessBuilder pb = new ProcessBuilder(inputParts);
                pb.redirectErrorStream(true);
                Map<String, String> env = pb.environment();
                env.clear();
                env.put("QUERY_STRING", queryString);
                env.put("REMOTE_ADDR", this.clientSocket.getInetAddress().toString().replace("/", ""));
                env.put("REMOTE_HOST", this.clientSocket.getInetAddress().getCanonicalHostName());
                env.put("REQUEST_METHOD", this.fields.get("Method"));
                env.put("SERVER_NAME", this.fields.get("Host").split(":")[0]);
                env.put("SERVER_PORT", this.fields.get("Host").split(":")[1]);
                env.put("SERVER_PROTOCOL", this.fields.get("Version"));
                env.put("SERVER_SOFTWARE", "Java/" + System.getProperty("java.version"));
                env.put("CONTENT_LENGTH", Integer.toString(queryString.length()));
                System.out.println(env.toString());

                Process process = pb.start();
                BufferedReader process_in = new BufferedReader(new InputStreamReader(process.getInputStream()));
                int process_char;
                int count = 0;
                boolean flag = false;
                while ((process_char = process_in.read()) != -1) {
                    char character = (char) process_char;
                    if (character == '\n') {
                    }
                    if (!flag) {
                        if (character == '\n') {
                            responseBody += character;
                            process_char = process_in.read();
                            if (process_char != -1) {
                                character = (char) process_char;
                                if (character == '\r'){
                                    flag = true;
                                }
                            }
                        } else {
                            count++;
                        }
                    }
                    responseBody += character;
                }

                System.out.println(responseBody);
                try {
                    if (process.waitFor() != 0) {
                        return constructErrorResponse(400, "Bad Request");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return constructErrorResponse(400, "Bad Request");
                }

                responseLength = Integer.toString(responseBody.length() - count - 3);

                return ("HTTP/1.1 200 OK\r\n" +
                            "Date: " + new Date() + "\r\n" +
                            "Server: JZAS Server\r\n" +
                            "Last-Modified: " + lastModified + "\r\n" +
                            "Content-Length: " + responseLength + "\r\n" +
                            responseBody).getBytes();
            } catch (IOException e) {
                e.printStackTrace();
                return constructErrorResponse(404, "Unable to Read the File");
            }

        } else if (f.exists()) {
            try {
                // CHECK 1: LAST MODIFIED CHECK
                lastModified = new Date(f.lastModified());
                if (this.fields.get("If-Modified-Since") != null) {
                    try {
                        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
                        Date ifModifiedSince = dateFormat.parse(this.fields.get("If-Modified-Since"));
                        if (ifModifiedSince.after(lastModified)) {
                            return constructErrorResponse(304, "Not Modified");
                        }
                    } catch (Exception e) {
                        return constructErrorResponse(409, "Conflict");
                    }
                }

                // CHECK 2 : TYPE CHECK
                if (this.fields.get("Accept") != null && !this.fields.get("Accept").equals("*/*")) {
                    String[] acceptTypes = this.fields.get("Accept").split(",");
                    Boolean foundType = false;
                    for (String type : acceptTypes) {
                        if (type.contains(extension)) {
                            foundType = true;
                            break;
                        }
                    }
                    if (!foundType) {
                        return constructErrorResponse(406, "Not Acceptable");
                    }
                }

                // CHECK 3: AUTHORIZATION CHECK
                String authResults = this.handleAuthorization(filePath.split("/"));

                if (!authResults.equals("1")) {
                    return constructErrorResponse(401, "Unauthorized",
                            "WWW-Authenticate: Basic realm=\"" + authResults + "\"");
                }

                // HANDLING IMAGES
                if (extension.matches("jpg|jpeg|png")) {
                    contentType = "image/" + extension;

                    BufferedImage image = ImageIO.read(new FileInputStream(f));
                    if (image == null) {
                        return constructErrorResponse(404, "Not Found");
                    }

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

                    byte[] combinedResponse = new byte[headers.length + imageReturn.length];
                    System.arraycopy(headers, 0, combinedResponse, 0, headers.length);
                    System.arraycopy(imageReturn, 0, combinedResponse, headers.length, imageReturn.length);

                    return combinedResponse;
                }
                // HANDLING TEXT AND HTML
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
                } else {
                    return constructErrorResponse(404, "Unable to read the file");
                }
            } catch (IOException e) {
                e.printStackTrace();
                return constructErrorResponse(404, "Not Found");
            }

        } else {
            return constructErrorResponse(404, "Not Found");
        }
    }

    public byte[] handlePost() {
        String hostPath = this.locations.getDefaultLocation();
        if (this.fields.get("Host") != null) {
            hostPath = this.locations.getLocation(this.fields.get("Host"));
        }

        // Construct the file path and match against Config
        String filePath = hostPath + this.fields.get("Path");

        // BAD FILE CHECK: if path tries to go out of bounds
        if (filePath.contains("..")) {
            String resolvedRelativePath = this.createPath(filePath);
            if (resolvedRelativePath == null) {
                System.out.println("Error: Invalid file path");
                return constructErrorResponse(400, "Bad Request");
            }
            filePath = hostPath + resolvedRelativePath;
        }

        // if empty path or HTML
        if (filePath.charAt(filePath.length() - 1) == '/') {
            System.out.println("Method not allowed.");
            return constructErrorResponse(405, "Method Not Allowed");
        }

        File f = new File(filePath);
        String responseBody = "";
        String responseLength = "";
        Date lastModified = new Date();
        if (!f.exists()) {
            return constructErrorResponse(404, "Not Found");
        }

        try {
            String[] fileParts = filePath.split("\\.");
            String extension = fileParts[fileParts.length - 1];
            if (!extension.matches("cgi|pl")) {
                System.out.println("Extension not recognized.");
                return constructErrorResponse(403, "Forbidden");
            }
            BufferedReader in = new BufferedReader(new FileReader(filePath));
            String line = in.readLine();
            in.close();
            String[] lineParts = line.split(" ");
            lineParts[0] = lineParts[0].replace("#!", "");
            String[] inputParts = Arrays.copyOf(lineParts, lineParts.length + 1);
            inputParts[inputParts.length - 1] = filePath;

            ProcessBuilder pb = new ProcessBuilder(inputParts);
            pb.redirectErrorStream(true);

            Map<String, String> env = pb.environment();
            env.clear();
            env.put("QUERY_STRING", "");
            env.put("REMOTE_ADDR", this.clientSocket.getInetAddress().toString().replace("/", ""));
            env.put("REMOTE_HOST", this.clientSocket.getInetAddress().getCanonicalHostName());
            env.put("REQUEST_METHOD", this.fields.get("Method"));
            env.put("SERVER_NAME", this.fields.get("Host").split(":")[0]);
            env.put("SERVER_PORT", this.fields.get("Host").split(":")[1]);
            env.put("SERVER_PROTOCOL", this.fields.get("Version"));
            env.put("SERVER_SOFTWARE", "Java/" + System.getProperty("java.version"));

            Process process = pb.start();
            OutputStream stdin = process.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stdin));
            writer.write(this.fields.get("Body"));
            writer.flush();
            writer.close();

            BufferedReader process_in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            if (this.fields.get("Transfer-Encoding").equals("chunked")){
                String proc_line;
                String content_type = "";
                while ((proc_line = process_in.readLine()) != null){
                    if (proc_line.split(":")[0].equals("Content-Type")) {
                        content_type = proc_line + "\r\n\r\n";
                        break;
                    }
                }
                String chunked_response = "HTTP/1.1 200 OK\r\n" +
                        "Date: " + new Date() + "\r\n" +
                        "Server: JZAS Server\r\n" +
                        "Last-Modified: " + lastModified + "\r\n" +
                        "Transfer-Encoding: chunked\r\n" +
                        content_type;
                clientSocket.getOutputStream().write((chunked_response).getBytes());
                while ((proc_line = process_in.readLine()) != null) {
                    if (proc_line.length() == 0) {
                        continue;
                    }
                    clientSocket.getOutputStream().write(
                            (Integer.toHexString((proc_line.length())) + "\r\n" +
                            proc_line + "\r\n").getBytes());
                }

                return ("0\r\n\r\n").getBytes();
            }

            int process_char;
            int count = 0;
            boolean flag = false;
            while ((process_char = process_in.read()) != -1) {
                char character = (char) process_char;
                if (character == '\n') {
                }
                if (!flag) {
                    if (character == '\n') {
                        responseBody += character;
                        process_char = process_in.read();
                        if (process_char != -1) {
                            character = (char) process_char;
                            if (character == '\r'){
                                flag = true;
                            }
                        }
                    } else {
                        count++;
                    }
                }
                responseBody += character;
            }

            try {
                if (process.waitFor() != 0) {
                    return constructErrorResponse(400, "Bad Request");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                return constructErrorResponse(400, "Bad Request");
            }

            responseLength = Integer.toString(responseBody.length() - count - 3);

            return ("HTTP/1.1 200 OK\r\n" +
                        "Date: " + new Date() + "\r\n" +
                        "Server: JZAS Server\r\n" +
                        "Last-Modified: " + lastModified + "\r\n" +
                        "Content-Length: " + responseLength + "\r\n" +
                        responseBody).getBytes();
        } catch (IOException e) {
            e.printStackTrace();
            return constructErrorResponse(404, "Unable to Read the File");
        }
    }

    /**
     *
     * @param filepath
     * @return 1 if authorized, "AuthName" if authorization is needed
     */
    private String handleAuthorization(String[] filepath) {
        // Check the directory for an htaccess file
        String user = "";
        String password = "";
        String authName = "";

        String directoryPath = String.join("/", Arrays.copyOfRange(filepath, 0, filepath.length - 1));
        File htaccess = new File(directoryPath + "/.htaccess");

        // If no need to authenticate, then return success
        if (!htaccess.exists()) {
            return "1";
        }

        // Check for cached data
        String[] cachedData = this.authCache.get(directoryPath, new Date(htaccess.lastModified()));
        if (cachedData != null) {
            user = cachedData[0];
            password = cachedData[1];
            authName = cachedData[2];
        } else {
            try {
                // Read in file contents
                BufferedReader in = new BufferedReader(new FileReader(htaccess));
                String line;
                user = "";
                password = "";
                while ((line = in.readLine()) != null) {
                    String[] lineSplit = line.split(" ", 2);
                    if (lineSplit[0].equals("AuthName")) {
                        authName = lineSplit[1];
                    } else if (lineSplit[0].equals("User")) {
                        user = lineSplit[1];
                    } else if (lineSplit[0].equals("Password")) {
                        password = lineSplit[1];
                    }
                }
                in.close();

                authCache.add(directoryPath, new String[] { user, password, authName }, new Date(htaccess.lastModified()));
            } catch (IOException e) {
                e.printStackTrace();
                return "Forbidden";
            }
        }

        // Check for authorization
        if (this.fields.get("Authorization") == null) {
            return authName;
        }

        // Get the credentials
        String[] auth = this.fields.get("Authorization").split(" ");
        String[] credentials = new String(Base64.getDecoder().decode(auth[1])).split(":");

        // Check if the credentials match
        if (auth[0].equals("Basic") &&
                user.equals(credentials[0]) &&
                password.equals(credentials[1])) {
            return "1";
        }

        return authName;
    }
    
    /**
     * Tests to see if the path tries to go out of bounds
     * @param filepath
     * @return null if the path tries to go out of bounds, or the normalized path if the path is viable
     */
    private String createPath(String filepath) {
        Stack<String> pathStack = new Stack<>();
        String[] pathParts = filepath.split("/");

        for (int i = 1; i < pathParts.length; i++) {
            String part = pathParts[i];

            if (part.equals("..") && pathStack.empty()) {
                return null;
            } else if (part.equals("..")) {
                pathStack.pop();
            } else if (!part.equals(".")) {
                pathStack.push(part);
            }
        }

        return "/" + String.join("/", pathStack);
    }
}
