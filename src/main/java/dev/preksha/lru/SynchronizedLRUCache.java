package dev.preksha.lru;

import java.util.List;
import java.util.Map;

public class SynchronizedLRUCache<K, V> implements Cache<K, V> {

    private final LRUCache<K, V> delegate;

    public SynchronizedLRUCache(int capacity) {
        this.delegate = new LRUCache<>(capacity);
    }

    @Override
    public synchronized V get(K key) {
        return delegate.get(key);
    }

    @Override
    public synchronized K put(K key, V value) {
        return delegate.put(key, value);
    }

    @Override
    public synchronized V remove(K key) {
        return delegate.remove(key);
    }

    @Override
    public synchronized int size() {
        return delegate.size();
    }

    @Override
    public synchronized int capacity() {
        return delegate.capacity();
    }

    @Override
    public synchronized void clear() {
        delegate.clear();
    }

    @Override
    public synchronized CacheStats stats() {
        return delegate.stats();
    }

    @Override
    public synchronized List<Map.Entry<K, V>> snapshot() {
        return delegate.snapshot();
    }
}