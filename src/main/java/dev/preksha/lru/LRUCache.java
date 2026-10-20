package dev.preksha.lru;

import java.util.*;

public class LRUCache<K, V> implements Cache<K, V> {

    private static class Node<K, V> {
        K key;
        V value;
        Node<K, V> prev;
        Node<K, V> next;

        Node(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    private final int capacity;

    private final Map<K, Node<K, V>> map = new HashMap<>();

    private final Node<K, V> head = new Node<>(null, null);
    private final Node<K, V> tail = new Node<>(null, null);

    private long hits = 0;
    private long misses = 0;
    private long evictions = 0;

    public LRUCache(int capacity) {

        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be > 0");
        }

        this.capacity = capacity;

        head.next = tail;
        tail.prev = head;
    }

    @Override
    public V get(K key) {

        Objects.requireNonNull(key);

        Node<K, V> node = map.get(key);

        if (node == null) {
            misses++;
            return null;
        }

        hits++;

        moveToFront(node);

        return node.value;
    }

    @Override
    public K put(K key, V value) {

        Objects.requireNonNull(key);
        Objects.requireNonNull(value);

        Node<K, V> node = map.get(key);

        // Key already exists
        if (node != null) {
            node.value = value;
            moveToFront(node);
            return null;
        }

        K evictedKey = null;

        // Cache is full
        if (map.size() == capacity) {
            evictedKey = evictLRU();
        }

        // Create new node
        node = new Node<>(key, value);

        map.put(key, node);

        addAfterHead(node);

        return evictedKey;
    }

    @Override
    public V remove(K key) {

        Node<K, V> node = map.remove(key);

        if (node == null) {
            return null;
        }

        unlink(node);

        return node.value;
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public int capacity() {
        return capacity;
    }

    @Override
    public void clear() {

        map.clear();

        head.next = tail;
        tail.prev = head;

        hits = 0;
        misses = 0;
        evictions = 0;
    }

    @Override
    public CacheStats stats() {

        return new CacheStats(
                hits,
                misses,
                evictions,
                size(),
                capacity
        );
    }

    @Override
    public List<Map.Entry<K, V>> snapshot() {

        List<Map.Entry<K, V>> result = new ArrayList<>();

        Node<K, V> current = head.next;

        while (current != tail) {

            result.add(
                    new AbstractMap.SimpleEntry<>(
                            current.key,
                            current.value
                    )
            );

            current = current.next;
        }

        return result;
    }

    private void addAfterHead(Node<K, V> node) {

        node.next = head.next;
        node.prev = head;

        head.next.prev = node;
        head.next = node;
    }

    private void unlink(Node<K, V> node) {

        node.prev.next = node.next;
        node.next.prev = node.prev;

        node.prev = null;
        node.next = null;
    }

    private void moveToFront(Node<K, V> node) {

        if (head.next == node) {
            return;
        }

        unlink(node);
        addAfterHead(node);
    }

    private K evictLRU() {

        Node<K, V> lru = tail.prev;

        K key = lru.key;

        unlink(lru);

        map.remove(key);

        evictions++;

        return key;
    }

    void checkInvariants() {
        if (head.prev != null) {
            throw new IllegalStateException("Head.prev must be null");
        }

        if (tail.next != null) {
            throw new IllegalStateException("Tail.next must be null");
        }

        int listSize = 0;
        Node<K, V> current = head.next;
        Node<K, V> previous = head;

        while (current != tail) {
            if (current.prev != previous) {
                throw new IllegalStateException("Broken prev pointer");
            }

            if (current.next == null) {
                throw new IllegalStateException("Node.next must not be null");
            }

            if (current.next.prev != current) {
                throw new IllegalStateException("Broken next pointer");
            }

            if (!map.containsKey(current.key)) {
                throw new IllegalStateException("List node missing from map");
            }

            if (map.get(current.key) != current) {
                throw new IllegalStateException("Map points to wrong node");
            }

            listSize++;
            previous = current;
            current = current.next;
        }

        if (tail.prev != previous) {
            throw new IllegalStateException("Tail.prev is incorrect");
        }

        if (listSize != map.size()) {
            throw new IllegalStateException("List size and map size differ");
        }

        if (map.size() > capacity) {
            throw new IllegalStateException("Cache exceeds capacity");
        }
    }
}