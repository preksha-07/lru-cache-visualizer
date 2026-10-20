package dev.preksha.lru;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class ShardedLRUCacheTest {

    @Test
    void survivesConcurrentOperations() throws Exception {

        ShardedLRUCache<Integer, Integer> cache =
                new ShardedLRUCache<>(100, 16);

        int threadCount = 8;
        int operationsPerThread = 10_000;

        ExecutorService executor =
                Executors.newFixedThreadPool(threadCount);

        List<Future<?>> futures = new ArrayList<>();

        for (int thread = 0; thread < threadCount; thread++) {

            futures.add(executor.submit(() -> {

                for (int i = 0; i < operationsPerThread; i++) {

                    int key = ThreadLocalRandom.current()
                            .nextInt(200);

                    cache.put(key, i);

                    if (i % 3 == 0) {
                        cache.get(key);
                    }

                    if (i % 10 == 0) {
                        cache.remove(key);
                    }
                }
            }));
        }

        for (Future<?> future : futures) {
            future.get();
        }

        executor.shutdown();

        assertTrue(
                executor.awaitTermination(10, TimeUnit.SECONDS)
        );

        assertTrue(cache.size() <= cache.capacity());
        assertTrue(cache.size() >= 0);
    }
}