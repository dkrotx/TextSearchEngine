package org.bytesoft.tsengine;

import java.util.LinkedHashMap;
import java.util.Map;

public class LemmatizerCache {
    static class CacheHashMap extends LinkedHashMap<String, String> {
        int capacity;

        CacheHashMap(int capacity) {
            super(capacity, 0.75f, true);
            this.capacity = capacity;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > capacity;
        }
    }

    CacheHashMap cache;
    int hits = 0;
    int attempts = 0;

    public String GetFirstForm(String word) {
        String lem = cache.get(word);

        if (lem != null) {
            hits++;
            return lem;
        }

        lem = WordUtils.GetWordFirstForm(word);
        cache.put(word, lem);
        return lem;
    }

    public LemmatizerCache(int capacity) {
        cache = new CacheHashMap(capacity);
    }
}
