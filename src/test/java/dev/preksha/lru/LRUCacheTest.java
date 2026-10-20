package dev.preksha.lru;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class LRUCacheTest {

    @Test
    void capacityOneEvictsPreviousEntry() {
        LRUCache<Integer, String> cache = new LRUCache<>(1);

        cache.put(1, "A");
        cache.put(2, "B");

        assertNull(cache.get(1));
        assertEquals("B", cache.get(2));
        assertEquals(1, cache.size());
    }

    @Test
    void leastRecentlyUsedEntryIsEvicted() {
        LRUCache<Integer, String> cache = new LRUCache<>(2);

        cache.put(1, "A");
        cache.put(2, "B");
        cache.put(3, "C");

        assertNull(cache.get(1));
        assertEquals("B", cache.get(2));
        assertEquals("C", cache.get(3));
    }

    @Test
    void getMakesEntryMostRecentlyUsed() {
        LRUCache<Integer, String> cache = new LRUCache<>(2);

        cache.put(1, "A");
        cache.put(2, "B");

        assertEquals("A", cache.get(1));

        cache.put(3, "C");

        assertEquals("A", cache.get(1));
        assertNull(cache.get(2));
        assertEquals("C", cache.get(3));
    }

    @Test
    void updatingExistingKeyDoesNotIncreaseSize() {
        LRUCache<Integer, String> cache = new LRUCache<>(2);

        cache.put(1, "A");
        cache.put(2, "B");

        cache.put(1, "Updated");

        assertEquals(2, cache.size());
        assertEquals("Updated", cache.get(1));
    }

    @Test
    void removeDeletesEntry() {
        LRUCache<Integer, String> cache = new LRUCache<>(2);

        cache.put(1, "A");
        cache.put(2, "B");

        assertEquals("A", cache.remove(1));
        assertNull(cache.get(1));
        assertEquals(1, cache.size());
    }

    @Test
    void removeMissingKeyReturnsNull() {
        LRUCache<Integer, String> cache = new LRUCache<>(2);

        assertNull(cache.remove(99));
    }

    @Test
    void removedKeyCanBeAddedAgain() {
        LRUCache<Integer, String> cache = new LRUCache<>(2);

        cache.put(1, "A");
        cache.remove(1);
        cache.put(1, "New A");

        assertEquals("New A", cache.get(1));
        assertEquals(1, cache.size());
    }

    @Test
    void nullKeyIsRejected() {
        LRUCache<Integer, String> cache = new LRUCache<>(2);

        assertThrows(
                NullPointerException.class,
                () -> cache.get(null)
        );

        assertThrows(
                NullPointerException.class,
                () -> cache.put(null, "A")
        );
    }

    @Test
    void nullValueIsRejected() {
        LRUCache<Integer, String> cache = new LRUCache<>(2);

        assertThrows(
                NullPointerException.class,
                () -> cache.put(1, null)
        );
    }

    @Test
    void invalidCapacityIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new LRUCache<Integer, String>(0)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new LRUCache<Integer, String>(-1)
        );
    }

    @Test
    void statisticsAreTracked() {
        LRUCache<Integer, String> cache = new LRUCache<>(2);

        cache.put(1, "A");
        cache.put(2, "B");

        assertEquals("A", cache.get(1)); // hit
        assertNull(cache.get(3));       // miss
        cache.put(3, "C");              // eviction

        CacheStats stats = cache.stats();

        assertEquals(1, stats.hits());
        assertEquals(1, stats.misses());
        assertEquals(1, stats.evictions());
        assertEquals(2, stats.size());
        assertEquals(2, stats.capacity());
        assertEquals(0.5, stats.hitRate());
    }

    @Test
    void snapshotIsInMostRecentlyUsedOrder() {
        LRUCache<Integer, String> cache = new LRUCache<>(3);

        cache.put(1, "A");
        cache.put(2, "B");
        cache.put(3, "C");

        cache.get(1);

        List<Map.Entry<Integer, String>> snapshot = cache.snapshot();

        assertEquals(
                List.of(1, 3, 2),
                snapshot.stream()
                        .map(Map.Entry::getKey)
                        .toList()
        );
    }

    @Test
    void clearRemovesEverythingAndResetsStatistics() {
        LRUCache<Integer, String> cache = new LRUCache<>(2);

        cache.put(1, "A");
        cache.put(2, "B");
        cache.get(1);
        cache.get(99);

        cache.clear();

        assertEquals(0, cache.size());
        assertTrue(cache.snapshot().isEmpty());

        CacheStats stats = cache.stats();

        assertEquals(0, stats.hits());
        assertEquals(0, stats.misses());
        assertEquals(0, stats.evictions());
    }

    @Test
    void evictionReturnsEvictedKey() {
        LRUCache<Integer, String> cache = new LRUCache<>(2);

        assertNull(cache.put(1, "A"));
        assertNull(cache.put(2, "B"));

        Integer evicted = cache.put(3, "C");

        assertEquals(1, evicted);
    }

    @Test
    void invariantsRemainValid() {
        LRUCache<Integer, String> cache = new LRUCache<>(3);

        cache.checkInvariants();

        cache.put(1, "A");
        cache.checkInvariants();

        cache.put(2, "B");
        cache.checkInvariants();

        cache.get(1);
        cache.checkInvariants();

        cache.put(3, "C");
        cache.checkInvariants();

        cache.put(4, "D");
        cache.checkInvariants();

        cache.remove(1);
        cache.checkInvariants();

        cache.clear();
        cache.checkInvariants();
    }

    @Test
    void randomizedDifferentialTestAgainstLinkedHashMap() {
        int capacity = 50;

        LRUCache<Integer, Integer> actual = new LRUCache<>(capacity);

        LinkedHashMap<Integer, Integer> expected =
                new LinkedHashMap<>(capacity, 0.75f, true);

        Random random = new Random(42);

        for (int i = 0; i < 100_000; i++) {

            int key = random.nextInt(100);
            int operation = random.nextInt(3);

            switch (operation) {

                case 0 -> {
                    int value = random.nextInt(1000);

                    Integer actualEvicted = actual.put(key, value);

                    Integer expectedEvicted = null;

                    if (!expected.containsKey(key)
                            && expected.size() == capacity) {

                        Iterator<Integer> iterator =
                                expected.keySet().iterator();

                        expectedEvicted = iterator.next();
                        iterator.remove();
                    }

                    expected.put(key, value);

                    assertEquals(
                            expectedEvicted,
                            actualEvicted
                    );
                }

                case 1 -> {
                    Integer actualValue = actual.get(key);
                    Integer expectedValue = expected.get(key);

                    assertEquals(expectedValue, actualValue);
                }

                case 2 -> {
                    Integer actualValue = actual.remove(key);
                    Integer expectedValue = expected.remove(key);

                    assertEquals(expectedValue, actualValue);
                }
            }

            assertEquals(expected.size(), actual.size());

            List<Integer> actualKeys =
                    actual.snapshot()
                            .stream()
                            .map(Map.Entry::getKey)
                            .toList();

            List<Integer> expectedKeys =
                    new ArrayList<>(expected.keySet());

            Collections.reverse(expectedKeys);

            assertEquals(expectedKeys, actualKeys);

            actual.checkInvariants();
        }
    }
}