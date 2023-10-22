package com.server;

import java.util.ArrayList;
import java.util.Hashtable;

public class Locations {
    private Hashtable<String, String> virtualHosts = new Hashtable<String, String>();
    private String defaultLocation;

    // each array is in format [url, documentroot, servername]
    public Locations(ArrayList<String[]> virtualHostsArrayList) {
        defaultLocation = virtualHostsArrayList.get(0)[1];
        for (String[] virtualHostArray : virtualHostsArrayList) {
            virtualHosts.put(virtualHostArray[2], virtualHostArray[1]);
        }
    }

    public String getLocation(String serverName) {
        if (virtualHosts.get(serverName) != null) {
            return virtualHosts.get(serverName);
        }
        else {
            return this.getDefaultLocation();
        }
    }

    public String getDefaultLocation() {
        return defaultLocation;
    }

}
