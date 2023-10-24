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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.imageio.ImageIO;

public class RequestHandler {
    // Class that takes in an http header and splits it up by carriage return lines

    // Initiate a hash table to store the header fields

    private Hashtable<String, String> fields = new Hashtable<String, String>();
    private AuthorizationCache authCache = new AuthorizationCache();
    private Locations locations = null;

    public RequestHandler(Locations locations, AuthorizationCache authCache) {
        this.authCache = authCache;
        this.locations = locations;
    }

    /**
     * For STDIN
     *
     * @param fields
     * @param request
     * @return the error response or empty string "" if the request is valid
     */
    public String readRequest(Hashtable<String, String> fields, String request, List<Byte> buffer, SocketChannel clientSocket) {
        this.parseHeaders(fields, request);
        String valid = this.isValid(fields);

        if (valid.length() != 0) {
            return valid;
        }

        return this.readFile(fields, buffer, clientSocket);
    }

    public void writeResponse(Hashtable<String, String> fields, String method, List<Byte> response, SocketChannel client) {
        if (method.equals("GET")) {
            byte[] responseBytes = new byte[response.size()];
            for (int i = 0; i < response.size(); i++) {
                responseBytes[i] = response.get(i);
            }
            try {
                client.write(ByteBuffer.wrap(responseBytes));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (method.equals("POST")) {
            this.writePostResponse(fields, client);
        }

        byte[] defaultResponse = constructErrorResponse(400, "Bad Request").getBytes();
        try {
            client.write(ByteBuffer.wrap(defaultResponse));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Parses the headers and inserts them into the provided hashtable pointer
     *
     * @param fields
     * @param request
     */
    private void parseHeaders(Hashtable<String, String> fields, String request) {
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
                fields.put("Method", lineSplit[0]);
                fields.put("Path", lineSplit[1]);
                fields.put("Version", lineSplit[2]);
            }
            // Otherwise use header's field name
            else {
                fields.put(lineSplit[0].trim(), lineSplit[1].trim());
            }
        }

        // Read body into hash table
        if (sections.length > 1) {
            fields.put("Body", sections[1]);
        }

        // Print out the hash table
        // for (String key : fields.keySet()) {
        //     System.out.println(key + ": " + fields.get(key));
        // }
        // System.out.println();
    }

    /**
     * Parses for viable fields/valid file path
     * if the file path is valid
     * add a key:value pair to the request hash table
     *
     * @return the error response or empty string "" if the request is valid
     */
    private String isValid(Hashtable<String, String> fields) {
        // Check for required fields
        if (!fields.containsKey("Method") ||
                !fields.containsKey("Path") ||
                !fields.containsKey("Version")) {
            return constructErrorResponse(400, "Bad Request");
        }

        // Check for valid method
        if (!fields.get("Method").equals("GET") &&
                !fields.get("Method").equals("POST")) {
            return constructErrorResponse(400, "Bad Request");
        }

        // Check for valid version
        if (!fields.get("Version").equals("HTTP/1.1")) {
            return constructErrorResponse(400, "Bad Request");
        }

        // Bad host check
        if (fields.get("Host") != null && this.locations.getLocation(fields.get("Host")) == null) {
            return constructErrorResponse(400, "Bad Request");
        }
        String hostPath = fields.get("Host") == null ? this.locations.getDefaultLocation()
                : this.locations.getLocation(fields.get("Host"));

        // Construct the file path and match against Config
        Boolean found = false;
        String filePath = hostPath + fields.get("Path").split("\\?")[0];

        // BAD FILE CHECK: if path tries to go out of bounds
        if (filePath.contains("..")) {
            String resolvedRelativePath = this.createPath(fields.get("Path"));
            if (resolvedRelativePath == null) {
                return constructErrorResponse(400, "Bad Request");
            }
            filePath = hostPath + resolvedRelativePath;
        }

        // EMPTY PATH CHECK
        if (filePath.charAt(filePath.length() - 1) == '/') {
            // check for mobile
            if (fields.get("User-Agent").matches(".*iPhone.*|.*android.*")) {
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

        fields.put("filePath", filePath);

        return "";
    }

    private String readFile(Hashtable<String, String> fields, List<Byte> buffer, SocketChannel client) {
        String filePath = fields.get("filePath");
        File f = new File(filePath);
        String contentType = "";
        String responseLength = "";
        Date lastModified = new Date();
        String responseBody = "";

        String[] fileParts = filePath.split("\\.");
        String extension = fileParts[fileParts.length - 1].split("\\?")[0];

        // CHECK 0: FILE EXISTS CHECK
        if (!f.exists()) {
            return constructErrorResponse(404, "Not Found");
        }

        // CHECK 1: LAST MODIFIED CHECK
        lastModified = new Date(f.lastModified());
        if (fields.get("If-Modified-Since") != null) {
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
                Date ifModifiedSince = dateFormat.parse(fields.get("If-Modified-Since"));
                if (ifModifiedSince.after(lastModified)) {
                    return constructErrorResponse(304, "Not Modified");
                }
            } catch (Exception e) {
                return constructErrorResponse(409, "Conflict");
            }
        }

        // CHECK 2 : TYPE CHECK
        if (fields.get("Accept") != null && !fields.get("Accept").equals("*/*")) {
            String[] acceptTypes = fields.get("Accept").split(",");
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

        if (authResults.length() != 0) {
            return constructErrorResponse(401, "Unauthorized",
                    "WWW-Authenticate: Basic realm=\"" + authResults + "\"");
        }

        // HANDLING FILE EXECUTION
        // HANDLE POST SEPERATELY
        if (fields.get("Method").equals("POST")) {
            return "";
        }

        if (f.canExecute() || extension.matches("cgi|pl")) {
            String queryString = "";
            queryString = fields.get("Path").split("\\?")[1];
            if (!fields.containsKey("Content-Type")
                    || !fields.get("Content-Type").equals("application/x-www-urlencoded")) {
                System.out.println("Missing Header");
                String err = constructErrorResponse(400, "Bad Request");
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
                env.put("REMOTE_ADDR", ((InetSocketAddress) (client.getRemoteAddress())).toString().replace("/", ""));
                env.put("REMOTE_HOST", ((InetSocketAddress) (client.getRemoteAddress())).getHostName());
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
                                if (character == '\r') {
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

                byte[] response = ("HTTP/1.1 200 OK\r\n" +
                        "Date: " + new Date() + "\r\n" +
                        "Server: JZAS Server\r\n" +
                        "Last-Modified: " + lastModified + "\r\n" +
                        "Content-Length: " + responseLength + "\r\n" +
                        responseBody).getBytes();

                for (byte b : response) {
                    buffer.add(b);
                }

                return "";
            } catch (IOException e) {
                e.printStackTrace();
                return constructErrorResponse(404, "Unable to Read the File");
            }

        }

        try {
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

                for (byte b : headers) {
                    buffer.add(b);
                }
                for (byte b : imageReturn) {
                    buffer.add(b);
                }

                return "";
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

                byte[] response = ("HTTP/1.1 200 OK\r\n" +
                        "Date: " + new Date() + "\r\n" +
                        "Server: JZAS Server\r\n" +
                        "Last-Modified: " + lastModified + "\r\n" +
                        "Content-Type: " + contentType + "\r\n" +
                        "Content-Length: " + responseLength + "\r\n" +
                        "\r\n" +
                        responseBody).getBytes();

                for (byte b : response) {
                    buffer.add(b);
                }

                return "";
            } else {
                return constructErrorResponse(404, "Unable to read the file");
            }
        } catch (IOException e) {
            e.printStackTrace();
            return constructErrorResponse(404, "Not Found");
        }
    }

    private void writePostResponse(Hashtable<String, String> fields, SocketChannel client) {
        String filePath = fields.get("filePath");
        File f = new File(filePath);
        String responseBody = "";
        String responseLength = "";
        Date lastModified = new Date();

        try {
            String[] fileParts = filePath.split("\\.");
            String extension = fileParts[fileParts.length - 1];
            if (!extension.matches("cgi|pl")) {
                System.out.println("Extension not recognized.");
                client.write(ByteBuffer.wrap(constructErrorResponse(403, "Forbidden").getBytes()));
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
            env.put("REMOTE_ADDR", ((InetSocketAddress) (client.getRemoteAddress())).toString().replace("/", ""));
            env.put("REMOTE_HOST", ((InetSocketAddress) (client.getRemoteAddress())).getHostName());
            env.put("REQUEST_METHOD", fields.get("Method"));
            env.put("SERVER_NAME", fields.get("Host").split(":")[0]);
            env.put("SERVER_PORT", fields.get("Host").split(":")[1]);
            env.put("SERVER_PROTOCOL", fields.get("Version"));
            env.put("SERVER_SOFTWARE", "Java/" + System.getProperty("java.version"));

            Process process = pb.start();
            OutputStream stdin = process.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stdin));
            writer.write(fields.get("Body"));
            writer.flush();
            writer.close();

            BufferedReader process_in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            if (fields.containsKey("Transfer-Encoding")
                    && fields.get("Transfer-Encoding").equals("chunked")) {
                String proc_line;
                String content_type = "";
                while ((proc_line = process_in.readLine()) != null) {
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

                client.write(ByteBuffer.wrap((chunked_response).getBytes()));

                while ((proc_line = process_in.readLine()) != null) {
                    if (proc_line.length() == 0) {
                        continue;
                    }
                    client.write(
                            ByteBuffer.wrap((Integer.toHexString((proc_line.length())) + "\r\n" +
                                    proc_line + "\r\n").getBytes()));
                }

                client.write(ByteBuffer.wrap(("0\r\n\r\n").getBytes()));
            } else {
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
                                if (character == '\r') {
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
                        client.write(ByteBuffer.wrap(constructErrorResponse(400, "Bad Request").getBytes()));
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    client.write(ByteBuffer.wrap(constructErrorResponse(400, "Bad Request").getBytes()));
                }

                responseLength = Integer.toString(responseBody.length() - count - 3);

                client.write(ByteBuffer.wrap(("HTTP/1.1 200 OK\r\n" +
                        "Date: " + new Date() + "\r\n" +
                        "Server: JZAS Server\r\n" +
                        "Last-Modified: " + lastModified + "\r\n" +
                        "Content-Length: " + responseLength + "\r\n" +
                        responseBody).getBytes()));

            }

        } catch (IOException e) {
            e.printStackTrace();
            //socketHandler.write(constructErrorResponse(404, "Unable to Read the File").getBytes());
        }
    }

    public String getMethod() {
        return this.fields.get("Method");
    }

    // Normal Error Response
    public String constructErrorResponse(int errorCode, String description) {
        return ("HTTP/1.1 " + errorCode + " " + description + "\r\n" +
                "Date: " + new Date() + "\r\n" +
                "Server: JZAS Server\r\n" +
                "\r\n\r\n");
    }

    // Error Response with children
    public String constructErrorResponse(int errorCode, String description, String children) {
        return ("HTTP/1.1 " + errorCode + " " + description + "\r\n" +
                "Date: " + new Date() + "\r\n" +
                "Server: JZAS Server\r\n" +
                children + "\r\n" +
                "\r\n\r\n");
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
            return "";
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

                authCache.add(directoryPath, new String[] { user, password, authName },
                        new Date(htaccess.lastModified()));
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
            return "";
        }

        return authName;
    }

    /**
     * Tests to see if the path tries to go out of bounds
     *
     * @param filepath
     * @return null if the path tries to go out of bounds, or the normalized path if
     *         the path is viable
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
