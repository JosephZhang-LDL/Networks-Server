package com.server;

import java.util.ArrayList;
import java.util.HashMap;

public class Locations {
    private HashMap<String, String> virtualHosts = new HashMap<String, String>();

    // each array is in format [url, documentroot, servername]
    public Locations(ArrayList<String[]> virtualHostsArrayList) {
        for (String[] virtualHostArray : virtualHostsArrayList) {
            virtualHosts.put(virtualHostArray[2], virtualHostArray[1]);
        }
    }

    public String getLocation(String serverName) {
        return virtualHosts.get(serverName);
    }

}
