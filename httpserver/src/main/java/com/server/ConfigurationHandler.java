package com.server;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class ConfigurationHandler {
    private String fileString;
    BufferedReader in = null;
    private int port = -1;
    private String mainServerName = "";
    private String mainDocumentRoot = "";
    private ArrayList<String[]> virtualHosts = new ArrayList<String[]>();

    public int getPort() {
        return this.port;
    }

    public String getMainServerName() {
        return this.mainServerName;
    }

    public String getMainDocumentRoot() {
        return this.mainDocumentRoot;
    }

    public ArrayList<String[]> getVirtualHosts() {
        return this.virtualHosts;
    }

    public void parseConfigFile() {
        try {
            in = new BufferedReader(new FileReader(fileString));

            String line;
            while((line = in.readLine()) != null){
                String[] words = line.split(" ");
                if (words.length == 0) {
                    continue;
                }
                if (words[0].equals("Listen")){
                    try {
                        this.port = Integer.parseInt(words[1]);
                    }
                    catch (NumberFormatException e){
                        // do nothing
                    }
                }
                else if (words[0].equals("ServerName")) {
                    this.mainServerName = words[1];
                }
                else if (words[0].equals("DocumentRoot")) {
                    this.mainDocumentRoot = words[1];
                }
                else if(words[0].equals("<VirtualHost")) {
                    this.virtualHosts.add(new String[3]);
                    String[] currHost = this.virtualHosts.get(virtualHosts.size()-1);
                    currHost[0] = words[1].replace(">", "");
                    while((line = in.readLine()) != null) {
                        line = line.trim();
                        if (line.equals("</VirtualHost>")) {
                            break;
                        }
                        words = line.split(" ");
                        if (words[0].equals("DocumentRoot")) {
                            assert currHost[1] == null;
                            currHost[1] = words[1];
                        }
                        else if (words[0].equals("ServerName")){
                            assert currHost[1] != null;
                            assert currHost[2] == null;
                            currHost[2] = words[1];
                        }
                    }
                }
            }
        } catch (IOException e) {
            // do nothing
            System.out.println("File IO Error with " + fileString);
            e.printStackTrace();
        }
    }

    public ConfigurationHandler(String fileString) {
        this.fileString = fileString;
    }

}
