package com.server;

import java.util.Date;
import java.util.Hashtable;

public class AuthorizationCache {
    private Hashtable<String, String[]> authCache = new Hashtable<String, String[]>();
    private Hashtable<String, Date> authDates = new Hashtable<String, Date>();

    public void add(String key, String[] value, Date lastModified) {
        authCache.put(key, value);
        authDates.put(key, lastModified);
    }

    public String[] get(String key, Date lastModified) {
        String[] res = authCache.get(key);
        if (res != null && lastModified.after(authDates.get(key))) {
            authCache.remove(key);
            authDates.remove(key);
            return null;
        }
        
        return authCache.get(key);
    }
}