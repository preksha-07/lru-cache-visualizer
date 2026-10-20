package dev.preksha.lru;

import org.openjdk.jmh.annotations.*;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class LRUCacheBenchmark {

    @Param({"UNIFORM", "SKEWED"})
    public String distribution;

    private LRUCache<Integer, Integer> plainCache;
    private SynchronizedLRUCache<Integer, Integer> synchronizedCache;
    private ShardedLRUCache<Integer, Integer> shardedCache;
    private Map<Integer, Integer> synchronizedMap;

    @Setup(Level.Trial)
    public void setup() {
        int capacity = 10_000;

        plainCache = new LRUCache<>(capacity);

        synchronizedCache =
                new SynchronizedLRUCache<>(capacity);

        shardedCache =
                new ShardedLRUCache<>(capacity, 16);

        synchronizedMap =
                Collections.synchronizedMap(
                        new LinkedHashMap<>(
                                capacity,
                                0.75f,
                                true
                        )
                );

        for (int i = 0; i < capacity; i++) {
            plainCache.put(i, i);
            synchronizedCache.put(i, i);
            shardedCache.put(i, i);
            synchronizedMap.put(i, i);
        }
    }

    private int nextKey() {
        ThreadLocalRandom random =
                ThreadLocalRandom.current();

        if ("SKEWED".equals(distribution)) {
            if (random.nextInt(100) < 80) {
                return random.nextInt(1_000);
            }

            return random.nextInt(10_000);
        }

        return random.nextInt(10_000);
    }

    /*
     * Plain LRU:
     * This implementation is intentionally benchmarked
     * only with one thread because it is not thread-safe.
     */
    @Benchmark
    @Threads(1)
    public Integer plainGet() {
        return plainCache.get(nextKey());
    }

    /*
     * Coarse-grained synchronization:
     * multiple threads can safely access the cache,
     * but operations are protected by the cache-level lock.
     */
    @Benchmark
    @Threads(1)
    public Integer synchronizedGet1() {
        return synchronizedCache.get(nextKey());
    }

    @Benchmark
    @Threads(2)
    public Integer synchronizedGet2() {
        return synchronizedCache.get(nextKey());
    }

    @Benchmark
    @Threads(4)
    public Integer synchronizedGet4() {
        return synchronizedCache.get(nextKey());
    }

    @Benchmark
    @Threads(8)
    public Integer synchronizedGet8() {
        return synchronizedCache.get(nextKey());
    }

    /*
     * Sharded cache:
     * different keys can be handled by different shards.
     */
    @Benchmark
    @Threads(1)
    public Integer shardedGet1() {
        return shardedCache.get(nextKey());
    }

    @Benchmark
    @Threads(2)
    public Integer shardedGet2() {
        return shardedCache.get(nextKey());
    }

    @Benchmark
    @Threads(4)
    public Integer shardedGet4() {
        return shardedCache.get(nextKey());
    }

    @Benchmark
    @Threads(8)
    public Integer shardedGet8() {
        return shardedCache.get(nextKey());
    }

    /*
     * Java's access-order LinkedHashMap is used as
     * a standard-library baseline.
     */
    @Benchmark
    @Threads(1)
    public Integer synchronizedLinkedHashMapGet1() {
        int key = nextKey();

        synchronized (synchronizedMap) {
            return synchronizedMap.get(key);
        }
    }

    @Benchmark
    @Threads(2)
    public Integer synchronizedLinkedHashMapGet2() {
        int key = nextKey();

        synchronized (synchronizedMap) {
            return synchronizedMap.get(key);
        }
    }

    @Benchmark
    @Threads(4)
    public Integer synchronizedLinkedHashMapGet4() {
        int key = nextKey();

        synchronized (synchronizedMap) {
            return synchronizedMap.get(key);
        }
    }

    @Benchmark
    @Threads(8)
    public Integer synchronizedLinkedHashMapGet8() {
        int key = nextKey();

        synchronized (synchronizedMap) {
            return synchronizedMap.get(key);
        }
    }
}