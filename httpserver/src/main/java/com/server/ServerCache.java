package com.server;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

class ServerCache {
    private int size;
    private final int capacity;
    private final ConcurrentHashMap<String, Byte[]> cache;
    private final ConcurrentLinkedDeque<String> lruList;

    public ServerCache(int capacity) {
        this.size = 0;
        this.capacity = capacity;
        this.cache = new ConcurrentHashMap<String, Byte[]>();
        this.lruList = new ConcurrentLinkedDeque<String>();
    }

    public Byte[] get(String key) {
        synchronized (this) {
            if (cache.containsKey(key)) {
                // Move the accessed element to the end of the list
                lruList.remove(key);
                lruList.addLast(key);
                return cache.get(key);
            }
        }
        return null;
    }

    public void put(String key, Byte[] value) {
        synchronized (this) {
            if (cache.size() >= capacity) {
                // Remove the least recently used element from the cache and list
                String lruKey = lruList.removeFirst();
                cache.remove(lruKey);
                size--;
            }
            // Add the new element to the end of the list and the cache
            size++;
            lruList.addLast(key);
            cache.put(key, value);
        }
    }

    public int size() {
        synchronized (this) {
            return size;
        }
    }
}
