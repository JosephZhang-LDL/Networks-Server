package com.server;

import java.util.ArrayList;
import java.util.HashMap;

public class Locations {
    private HashMap<String, String> virtualHosts = new HashMap<String, String>();

    public Locations(ArrayList<String[]> virtualHostsArrayList) {
        for (String[] virtualHostArray : virtualHostsArrayList) {
            virtualHosts.put(virtualHostArray[2], virtualHostArray[1]);
        }
        System.out.println(virtualHosts);
    }

    public String getLocation(String serverName) {
        return virtualHosts.get(serverName);
    }

}
