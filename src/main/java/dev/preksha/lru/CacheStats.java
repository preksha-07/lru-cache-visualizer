package dev.preksha.lru;

public record CacheStats(
        long hits,
        long misses,
        long evictions,
        int size,
        int capacity
) {
    public double hitRate() {
        long total = hits + misses;
        return total == 0 ? 0.0 : (double) hits / total;
    }
}