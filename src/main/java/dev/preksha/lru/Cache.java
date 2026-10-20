package dev.preksha.lru;

import java.util.List;
import java.util.Map;

public interface Cache<K, V> {

    V get(K key);

    K put(K key, V value);

    V remove(K key);

    int size();

    int capacity();

    void clear();

    CacheStats stats();

    List<Map.Entry<K, V>> snapshot();
}