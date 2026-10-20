package dev.preksha.lru;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CacheService {

    private final Cache<String, String> cache;

    public CacheService() {
        this.cache = new LRUCache<>(100);
    }

    public String get(String key) {
        return cache.get(key);
    }

    public String put(String key, String value) {
        cache.put(key, value);
        return value;
    }

    public String remove(String key) {
        return cache.remove(key);
    }

    public void clear() {
        cache.clear();
    }

    public int size() {
        return cache.size();
    }

    public int capacity() {
        return cache.capacity();
    }

    public CacheStats stats() {
        return cache.stats();
    }

    public List<Map.Entry<String, String>> snapshot() {
        return cache.snapshot();
    }
}