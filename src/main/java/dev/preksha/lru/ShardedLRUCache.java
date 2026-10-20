package dev.preksha.lru;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ShardedLRUCache<K, V> implements Cache<K, V> {

    private final List<SynchronizedLRUCache<K, V>> shards;
    private final int capacity;

    public ShardedLRUCache(int capacity, int shardCount) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be > 0");
        }

        if (shardCount <= 0) {
            throw new IllegalArgumentException("shardCount must be > 0");
        }

        this.capacity = capacity;

        int actualShardCount = Math.min(capacity, shardCount);

        this.shards = new ArrayList<>(actualShardCount);

        int baseCapacity = capacity / actualShardCount;
        int remainder = capacity % actualShardCount;

        for (int i = 0; i < actualShardCount; i++) {
            int shardCapacity =
                    baseCapacity + (i < remainder ? 1 : 0);

            shards.add(new SynchronizedLRUCache<>(shardCapacity));
        }
    }

    private SynchronizedLRUCache<K, V> shardFor(K key) {
        int hash = key.hashCode();

        hash ^= (hash >>> 16);

        int index = Math.floorMod(hash, shards.size());

        return shards.get(index);
    }

    @Override
    public V get(K key) {
        return shardFor(key).get(key);
    }

    @Override
    public K put(K key, V value) {
        return shardFor(key).put(key, value);
    }

    @Override
    public V remove(K key) {
        return shardFor(key).remove(key);
    }

    @Override
    public int size() {
        int total = 0;

        for (SynchronizedLRUCache<K, V> shard : shards) {
            total += shard.size();
        }

        return total;
    }

    @Override
    public int capacity() {
        return capacity;
    }

    @Override
    public void clear() {
        for (SynchronizedLRUCache<K, V> shard : shards) {
            shard.clear();
        }
    }

    @Override
    public CacheStats stats() {
        long hits = 0;
        long misses = 0;
        long evictions = 0;

        for (SynchronizedLRUCache<K, V> shard : shards) {
            CacheStats stats = shard.stats();

            hits += stats.hits();
            misses += stats.misses();
            evictions += stats.evictions();
        }

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

        for (SynchronizedLRUCache<K, V> shard : shards) {
            result.addAll(shard.snapshot());
        }

        return result;
    }
}